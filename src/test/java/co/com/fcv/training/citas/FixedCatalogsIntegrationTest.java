package co.com.fcv.training.citas;

import co.com.fcv.training.citas.adapter.security.JwtTokens;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import java.util.Set;
import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** HU-003: fixed catalogs required by RF-05 are seeded, readable and not writable through the contract. */
@SpringBootTest
@AutoConfigureMockMvc
class FixedCatalogsIntegrationTest extends DatabaseIntegrationSupport {
    @DynamicPropertySource static void properties(DynamicPropertyRegistry r) {
        r.add("app.jwt.access-secret", () -> "a".repeat(40)); r.add("app.jwt.refresh-secret", () -> "b".repeat(40)); r.add("app.cookie.secure", () -> true); r.add("app.cookie.same-site", () -> "None");
    }
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired JwtTokens jwt;

    // CA-01: roles, appointment statuses, reschedule statuses, regimes and locations are available.
    @Test void everyFixedCatalogIsSeededAndReadable() throws Exception {
        String token = "Bearer " + jwt.access(1L, Set.of("USER"));
        mvc.perform(get("/api/v1/catalogs/roles").header("Authorization",token))
                .andExpect(status().isOk()).andExpect(jsonPath("$[*].code", containsInAnyOrder("USER","PROFESSIONAL","ADMIN")));
        mvc.perform(get("/api/v1/catalogs/appointment-statuses").header("Authorization",token))
                .andExpect(status().isOk()).andExpect(jsonPath("$[*].code", containsInAnyOrder("REQUESTED","APPROVED","REJECTED","CANCELLED","COMPLETED","NO_SHOW")))
                .andExpect(jsonPath("$[?(@.code=='REQUESTED')].terminal", contains(false)))
                .andExpect(jsonPath("$[?(@.code=='CANCELLED')].terminal", contains(true)));
        mvc.perform(get("/api/v1/catalogs/reschedule-statuses").header("Authorization",token))
                .andExpect(status().isOk()).andExpect(jsonPath("$[*].code", containsInAnyOrder("PENDING","APPROVED","REJECTED","CANCELLED")))
                .andExpect(jsonPath("$[?(@.code=='PENDING')].terminal", contains(false)));
        mvc.perform(get("/api/v1/catalogs/regimes").header("Authorization",token))
                .andExpect(status().isOk()).andExpect(jsonPath("$", not(empty())));
        mvc.perform(get("/api/v1/catalogs/unknown").header("Authorization",token)).andExpect(status().isBadRequest());
    }

    // CA-02: the location catalog holds exactly the two PRD sites.
    @Test void locationsAreTheTwoPrdSites() throws Exception {
        mvc.perform(get("/api/v1/catalogs/locations").header("Authorization","Bearer " + jwt.access(1L, Set.of("USER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].code", containsInAnyOrder("HIC","ICV")))
                .andExpect(jsonPath("$[?(@.code=='HIC')].name", contains(containsString("Hospital Internacional de Colombia"))))
                .andExpect(jsonPath("$[?(@.code=='ICV')].name", contains(containsString("Instituto Cardiovascular"))));
    }

    // CA-03: the contract exposes no write operation over fixed catalogs, not even for ADMIN.
    @Test void fixedCatalogsAreReadOnlyThroughTheContract() throws Exception {
        String admin = "Bearer " + jwt.access(1L, Set.of("ADMIN"));
        int before = jdbc.queryForObject("select count(*) from locations",Integer.class);
        mvc.perform(post("/api/v1/catalogs/locations").header("Authorization",admin).contentType(MediaType.APPLICATION_JSON).content("{\"code\":\"X\",\"name\":\"Externa\"}"))
                .andExpect(status().is4xxClientError());
        mvc.perform(delete("/api/v1/catalogs/locations").header("Authorization",admin)).andExpect(status().is4xxClientError());
        assertThat(jdbc.queryForObject("select count(*) from locations",Integer.class)).isEqualTo(before);
    }
}
