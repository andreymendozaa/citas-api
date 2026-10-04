package co.com.fcv.training.citas;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** HU-004 / RF-20: the OpenAPI contract is published and documents the JWT scheme; health is public without details. */
@SpringBootTest
@AutoConfigureMockMvc
class OpenApiContractIntegrationTest extends DatabaseIntegrationSupport {
    @DynamicPropertySource static void properties(DynamicPropertyRegistry r) {
        r.add("app.jwt.access-secret", () -> "a".repeat(40)); r.add("app.jwt.refresh-secret", () -> "b".repeat(40)); r.add("app.cookie.secure", () -> true); r.add("app.cookie.same-site", () -> "None");
    }
    @Autowired MockMvc mvc;

    // CA-01: every approved capability appears in the published contract with its security scheme.
    @Test void openApiDocumentListsTheV1RoutesAndTheBearerScheme() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("FCV Citas API"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$.paths", hasKey("/api/v1/auth/login")))
                .andExpect(jsonPath("$.paths", hasKey("/api/v1/appointments")))
                .andExpect(jsonPath("$.paths", hasKey("/api/v1/admin/appointments/upcoming")))
                .andExpect(jsonPath("$.paths", hasKey("/api/v1/admin/inbox")))
                .andExpect(jsonPath("$.paths", hasKey("/api/v1/appointments/{id}/history")))
                .andExpect(jsonPath("$.paths", not(hasKey("/actuator/health"))));
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
    }

    @Test void healthIsPublicAndRevealsNoDetails() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components").doesNotExist());
        mvc.perform(get("/actuator/env")).andExpect(status().is4xxClientError());
        mvc.perform(get("/api/v1/admin/inbox")).andExpect(status().isUnauthorized());
    }
}
