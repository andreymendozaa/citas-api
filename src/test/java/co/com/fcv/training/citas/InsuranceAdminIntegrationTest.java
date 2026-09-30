package co.com.fcv.training.citas;

import co.com.fcv.training.citas.adapter.security.JwtTokens;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** HU-012 (EPS) and HU-013 (EPS plans): logical CRUD, cloned from the already-approved specialties pattern. */
@SpringBootTest
@AutoConfigureMockMvc
class InsuranceAdminIntegrationTest extends DatabaseIntegrationSupport {
    private static final String ACCESS_KEY = UUID.randomUUID() + UUID.randomUUID().toString();
    private static final String REFRESH_KEY = UUID.randomUUID() + UUID.randomUUID().toString();

    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        registry.add("app.jwt.access-secret", () -> ACCESS_KEY);
        registry.add("app.jwt.refresh-secret", () -> REFRESH_KEY);
        registry.add("app.cookie.secure", () -> true);
        registry.add("app.cookie.same-site", () -> "None");
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired JdbcTemplate jdbc;
    @Autowired JwtTokens jwt;

    private String admin() { return jwt.access(1L, Set.of("ADMIN")); }
    private String user() { return jwt.access(1L, Set.of("USER")); }
    private String code(String prefix) { return (prefix + UUID.randomUUID()).substring(0, 30); }

    @Test void nonAdminCannotManageEpsOrPlans() throws Exception {
        mvc.perform(post("/api/v1/admin/eps").header("Authorization", "Bearer " + user())
                .contentType(MediaType.APPLICATION_JSON).content("{\"code\":\"" + code("EPS") + "\",\"name\":\"Cualquiera\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/admin/eps").header("Authorization", "Bearer " + user())).andExpect(status().isForbidden());
    }

    @Test void adminCrudEpsAndLogicalDeactivationHidesItFromPublicList() throws Exception {
        String epsCode = code("EPS");
        String body = mvc.perform(post("/api/v1/admin/eps").header("Authorization", "Bearer " + admin())
                .contentType(MediaType.APPLICATION_JSON).content("{\"code\":\"" + epsCode + "\",\"name\":\"Salud Total Demo\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.active").value(true))
                .andReturn().getResponse().getContentAsString();
        Long id = mapper.readTree(body).get("id").asLong();

        mvc.perform(get("/api/v1/eps").header("Authorization", "Bearer " + user()))
                .andExpect(status().isOk()).andExpect(jsonPath("$[?(@.code=='" + epsCode + "')]").isNotEmpty());

        mvc.perform(patch("/api/v1/admin/eps/{id}", id).header("Authorization", "Bearer " + admin())
                .contentType(MediaType.APPLICATION_JSON).content("{\"active\":false}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.active").value(false));

        mvc.perform(get("/api/v1/eps").header("Authorization", "Bearer " + user()))
                .andExpect(status().isOk()).andExpect(jsonPath("$[?(@.code=='" + epsCode + "')]").isEmpty());
        mvc.perform(get("/api/v1/admin/eps").header("Authorization", "Bearer " + admin()))
                .andExpect(status().isOk()).andExpect(jsonPath("$[?(@.code=='" + epsCode + "')]").isNotEmpty());
    }

    @Test void duplicateEpsCodeConflicts() throws Exception {
        String epsCode = code("EPS");
        mvc.perform(post("/api/v1/admin/eps").header("Authorization", "Bearer " + admin())
                .contentType(MediaType.APPLICATION_JSON).content("{\"code\":\"" + epsCode + "\",\"name\":\"Uno\"}"))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/v1/admin/eps").header("Authorization", "Bearer " + admin())
                .contentType(MediaType.APPLICATION_JSON).content("{\"code\":\"" + epsCode + "\",\"name\":\"Dos\"}"))
                .andExpect(status().isConflict());
    }

    @Test void epsPlanValidatesExistingActiveEpsAndRegime() throws Exception {
        Long regimeId = jdbc.queryForObject("select id from insurance_regimes where code='PARTICULAR'", Long.class);
        String epsCode = code("EPS");
        String epsBody = mvc.perform(post("/api/v1/admin/eps").header("Authorization", "Bearer " + admin())
                .contentType(MediaType.APPLICATION_JSON).content("{\"code\":\"" + epsCode + "\",\"name\":\"Con Planes\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        Long epsId = mapper.readTree(epsBody).get("id").asLong();

        String planCode = code("PLAN");
        String planBody = mvc.perform(post("/api/v1/admin/eps-plans").header("Authorization", "Bearer " + admin())
                .contentType(MediaType.APPLICATION_JSON).content(
                        "{\"epsId\":%d,\"regimeId\":%d,\"code\":\"%s\",\"name\":\"Plan Base\"}".formatted(epsId, regimeId, planCode)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.active").value(true))
                .andReturn().getResponse().getContentAsString();
        Long planId = mapper.readTree(planBody).get("id").asLong();

        mvc.perform(get("/api/v1/catalogs/plans").header("Authorization", "Bearer " + user()))
                .andExpect(status().isOk()).andExpect(jsonPath("$[?(@.code=='" + planCode + "')]").isNotEmpty());

        // Non-existent EPS.
        mvc.perform(post("/api/v1/admin/eps-plans").header("Authorization", "Bearer " + admin())
                .contentType(MediaType.APPLICATION_JSON).content(
                        "{\"epsId\":999999999,\"regimeId\":%d,\"code\":\"%s\",\"name\":\"Plan\"}".formatted(regimeId, code("PLAN"))))
                .andExpect(status().isNotFound());
        // Invalid regime.
        mvc.perform(post("/api/v1/admin/eps-plans").header("Authorization", "Bearer " + admin())
                .contentType(MediaType.APPLICATION_JSON).content(
                        "{\"epsId\":%d,\"regimeId\":999999999,\"code\":\"%s\",\"name\":\"Plan\"}".formatted(epsId, code("PLAN"))))
                .andExpect(status().isNotFound());
        // Duplicate plan code within the same EPS.
        mvc.perform(post("/api/v1/admin/eps-plans").header("Authorization", "Bearer " + admin())
                .contentType(MediaType.APPLICATION_JSON).content(
                        "{\"epsId\":%d,\"regimeId\":%d,\"code\":\"%s\",\"name\":\"Otro\"}".formatted(epsId, regimeId, planCode)))
                .andExpect(status().isConflict());

        // Deactivating the plan removes it from the public catalog used by HU-011 registration.
        mvc.perform(patch("/api/v1/admin/eps-plans/{id}", planId).header("Authorization", "Bearer " + admin())
                .contentType(MediaType.APPLICATION_JSON).content("{\"active\":false}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.active").value(false));
        mvc.perform(get("/api/v1/catalogs/plans").header("Authorization", "Bearer " + user()))
                .andExpect(status().isOk()).andExpect(jsonPath("$[?(@.code=='" + planCode + "')]").isEmpty());

        // Deactivating the EPS blocks new plans against it.
        mvc.perform(patch("/api/v1/admin/eps/{id}", epsId).header("Authorization", "Bearer " + admin())
                .contentType(MediaType.APPLICATION_JSON).content("{\"active\":false}"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/admin/eps-plans").header("Authorization", "Bearer " + admin())
                .contentType(MediaType.APPLICATION_JSON).content(
                        "{\"epsId\":%d,\"regimeId\":%d,\"code\":\"%s\",\"name\":\"Plan\"}".formatted(epsId, regimeId, code("PLAN"))))
                .andExpect(status().isConflict());
    }
}
