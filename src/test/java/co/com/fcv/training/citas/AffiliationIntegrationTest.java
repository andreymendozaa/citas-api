package co.com.fcv.training.citas;

import co.com.fcv.training.citas.adapter.security.JwtTokens;
import co.com.fcv.training.citas.application.SchedulingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import java.util.Set;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** HU-011: a USER consults and changes their own insurance affiliation without duplicating plans (RF-04). */
@SpringBootTest
@AutoConfigureMockMvc
class AffiliationIntegrationTest extends DatabaseIntegrationSupport {
    @DynamicPropertySource static void properties(DynamicPropertyRegistry r) {
        r.add("app.jwt.access-secret", () -> "a".repeat(40)); r.add("app.jwt.refresh-secret", () -> "b".repeat(40)); r.add("app.cookie.secure", () -> true); r.add("app.cookie.same-site", () -> "None");
    }
    @Autowired MockMvc mvc;
    @Autowired SchedulingService scheduling;
    @Autowired JdbcTemplate jdbc;
    @Autowired JwtTokens jwt;

    Long contributive, subsidized, inactivePlan, planOfInactiveEps, userA, userB;
    String tokenA, tokenB, epsName;

    @BeforeEach void fixture() {
        String suffix = UUID.randomUUID().toString().substring(0, 12);
        Long contributivo = regime("CONTRIBUTIVO"), subsidiado = regime("SUBSIDIADO");
        epsName = "EPS Afiliación " + suffix;
        Long eps = scheduling.createEps("AF" + suffix, epsName).id();
        contributive = scheduling.createEpsPlan(eps, contributivo, "PC" + suffix, "Plan Contributivo " + suffix).id();
        subsidized = scheduling.createEpsPlan(eps, subsidiado, "PS" + suffix, "Plan Subsidiado " + suffix).id();
        inactivePlan = scheduling.createEpsPlan(eps, contributivo, "PI" + suffix, "Plan Inactivo " + suffix).id();
        scheduling.updateEpsPlan(inactivePlan, null, false);
        Long closedEps = scheduling.createEps("AX" + suffix, "EPS Cerrada " + suffix).id();
        planOfInactiveEps = scheduling.createEpsPlan(closedEps, contributivo, "PX" + suffix, "Plan EPS cerrada " + suffix).id();
        scheduling.updateEps(closedEps, null, false);
        userA = user("aff-a-" + suffix + "@example.test", "AA" + suffix);
        userB = user("aff-b-" + suffix + "@example.test", "AB" + suffix);
        tokenA = jwt.access(userA, Set.of("USER"));
        tokenB = jwt.access(userB, Set.of("USER"));
    }

    // V5 seed: the regime catalog holds the five values of the reference model.
    @Test void regimeCatalogHoldsTheReferenceValues() throws Exception {
        mvc.perform(get("/api/v1/catalogs/regimes").header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].code", hasItems("CONTRIBUTIVO", "SUBSIDIADO", "ESPECIAL", "EXCEPCION", "PARTICULAR")));
    }

    // CA-01: with active catalogs and a valid plan, the affiliation is associated to the caller's profile.
    @Test void userAssociatesAndConsultsTheirAffiliation() throws Exception {
        mvc.perform(get("/api/v1/users/me/affiliation").header("Authorization", "Bearer " + tokenA)).andExpect(status().isNoContent());

        mvc.perform(change(tokenA, contributive)).andExpect(status().isOk())
                .andExpect(jsonPath("$.planId").value(contributive)).andExpect(jsonPath("$.epsName").value(epsName))
                .andExpect(jsonPath("$.regimeName").value("Contributivo")).andExpect(jsonPath("$.membershipNumber").isNotEmpty());

        mvc.perform(get("/api/v1/users/me/affiliation").header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk()).andExpect(jsonPath("$.planId").value(contributive));
    }

    // CA-02: repeating the current plan is rejected; returning to an old plan reuses its row; one current at most.
    @Test void plansAreNeverDuplicatedWithinTheAffiliation() throws Exception {
        mvc.perform(change(tokenA, contributive)).andExpect(status().isOk());
        mvc.perform(change(tokenA, contributive)).andExpect(status().isConflict());
        assertThat(rows(userA)).isEqualTo(1);

        mvc.perform(change(tokenA, subsidized)).andExpect(status().isOk()).andExpect(jsonPath("$.regimeName").value("Subsidiado"));
        mvc.perform(change(tokenA, contributive)).andExpect(status().isOk()).andExpect(jsonPath("$.planId").value(contributive));

        assertThat(rows(userA)).isEqualTo(2);
        assertThat(jdbc.queryForObject("select count(*) from user_insurance_affiliations where user_id=? and is_current=true", Integer.class, userA)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from (select plan_id from user_insurance_affiliations where user_id=? group by plan_id having count(*)>1) d", Integer.class, userA)).isZero();
    }

    // CA-01 (catálogos activos): inactive plans, plans of an inactive EPS, unknown or missing plans are rejected without changes.
    @Test void onlySelectablePlansAreAccepted() throws Exception {
        mvc.perform(change(tokenA, contributive)).andExpect(status().isOk());

        mvc.perform(change(tokenA, inactivePlan)).andExpect(status().isBadRequest());
        mvc.perform(change(tokenA, planOfInactiveEps)).andExpect(status().isBadRequest());
        mvc.perform(change(tokenA, 99999999L)).andExpect(status().isBadRequest());
        mvc.perform(put("/api/v1/users/me/affiliation").header("Authorization", "Bearer " + tokenA).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());

        assertThat(rows(userA)).isEqualTo(1);
        mvc.perform(get("/api/v1/users/me/affiliation").header("Authorization", "Bearer " + tokenA)).andExpect(jsonPath("$.planId").value(contributive));
        // the public plan catalog does not offer the plan of an inactive EPS either
        mvc.perform(get("/api/v1/catalogs/plans").header("Authorization", "Bearer " + tokenA))
                .andExpect(jsonPath("$[*].id", not(hasItem(planOfInactiveEps.intValue())))).andExpect(jsonPath("$[*].id", hasItem(contributive.intValue())));
    }

    // CA-03: each USER only sees and changes their own affiliation; other roles and anonymous callers are denied.
    @Test void affiliationIsIsolatedPerUserAndRestrictedToUsers() throws Exception {
        mvc.perform(change(tokenA, contributive)).andExpect(status().isOk());

        mvc.perform(get("/api/v1/users/me/affiliation").header("Authorization", "Bearer " + tokenB)).andExpect(status().isNoContent());
        mvc.perform(change(tokenB, subsidized)).andExpect(status().isOk());
        mvc.perform(get("/api/v1/users/me/affiliation").header("Authorization", "Bearer " + tokenA)).andExpect(jsonPath("$.planId").value(contributive));

        mvc.perform(get("/api/v1/users/me/affiliation").header("Authorization", "Bearer " + jwt.access(userA, Set.of("ADMIN")))).andExpect(status().isForbidden());
        mvc.perform(change(jwt.access(userA, Set.of("PROFESSIONAL")), subsidized)).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/users/me/affiliation")).andExpect(status().isUnauthorized());
        assertThat(rows(userA)).isEqualTo(1);
    }

    private RequestBuilder change(String token, Long planId) {
        return put("/api/v1/users/me/affiliation").header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON).content("{\"planId\":" + planId + "}");
    }
    private Long regime(String code) { return jdbc.queryForObject("select id from insurance_regimes where code=?", Long.class, code); }
    private int rows(Long user) { return jdbc.queryForObject("select count(*) from user_insurance_affiliations where user_id=?", Integer.class, user); }
    private Long user(String email, String document) { jdbc.update("insert into users(first_name,last_name,document_type,document_number,email,phone,password_hash,active,email_verified) values ('Test','User','CC',?,?,?,'hash',true,false)", document, email, "300"); return jdbc.queryForObject("select id from users where email=?", Long.class, email); }
}
