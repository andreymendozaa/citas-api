package co.com.fcv.training.citas;

import co.com.fcv.training.citas.adapter.security.JwtTokens;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import java.util.Set;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Verifies the development-only local mailbox (profile "local") used to deliver HU-008 recovery tokens without SMTP. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class LocalMailboxIntegrationTest extends DatabaseIntegrationSupport {
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
    @Autowired JwtTokens jwt;

    private String uniqueEmail() { return "mailbox-" + UUID.randomUUID() + "@example.test"; }

    @Test void adminReadsTokenUserIsForbiddenAndTokenResetsThePassword() throws Exception {
        String email = uniqueEmail();
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                {"firstName":"Ana","lastName":"Buzon","documentType":"CC","documentNumber":"%s","email":"%s","phone":"3000000000","password":"SyntheticPass123!"}
                """.formatted(UUID.randomUUID(), email))).andExpect(status().isCreated());
        mvc.perform(post("/api/v1/auth/password-recovery").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\"}".formatted(email))).andExpect(status().isAccepted());

        String admin = jwt.access(1L, Set.of("ADMIN"));
        String user = jwt.access(1L, Set.of("USER"));

        mvc.perform(get("/api/v1/admin/local-mailbox/password-resets").param("email", email)
                .header("Authorization", "Bearer " + user)).andExpect(status().isForbidden());

        String body = mvc.perform(get("/api/v1/admin/local-mailbox/password-resets").param("email", email)
                .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String token = mapper.readTree(body).get("token").asText();
        assertThat(token).isNotBlank();

        mvc.perform(get("/api/v1/admin/local-mailbox/password-resets").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$['" + email + "'].token").value(token));

        mvc.perform(post("/api/v1/auth/password-reset").contentType(MediaType.APPLICATION_JSON).content("""
                {"token":"%s","newPassword":"NewSyntheticPass456!","confirmation":"NewSyntheticPass456!"}
                """.formatted(token))).andExpect(status().isNoContent());

        mvc.perform(get("/api/v1/admin/local-mailbox/password-resets").param("email", uniqueEmail())
                .header("Authorization", "Bearer " + admin)).andExpect(status().isNotFound());
    }
}
