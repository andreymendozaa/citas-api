package co.com.fcv.training.citas;

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
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ProfileIntegrationTest extends DatabaseIntegrationSupport {
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

    private String uniqueEmail() { return "profile-" + UUID.randomUUID() + "@example.test"; }
    private String registerAndLogin(String email) throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                {"firstName":"Ana","lastName":"Perfil","documentType":"CC","documentNumber":"%s","email":"%s","phone":"3000000000","password":"SyntheticPass123!"}
                """.formatted(UUID.randomUUID(), email))).andExpect(status().isCreated());
        String body = mvc.perform(post("/api/v1/auth/login").header("X-Requested-With", "XMLHttpRequest")
                .contentType(MediaType.APPLICATION_JSON).content("""
                    {"email":"%s","password":"SyntheticPass123!"}
                    """.formatted(email))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return mapper.readTree(body).get("accessToken").asText();
    }

    @Test void meRequiresAuthenticationAndReturnsOwnDataWithoutSecrets() throws Exception {
        mvc.perform(get("/api/v1/users/me")).andExpect(status().isUnauthorized());
        String email = uniqueEmail();
        String access = registerAndLogin(email);
        mvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + access))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.firstName").value("Ana"))
                .andExpect(jsonPath("$.phone").value("3000000000"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.roles[0]").value("USER"));
    }

    @Test void patchMeUpdatesOnlyPhoneAndPersists() throws Exception {
        String email = uniqueEmail();
        String access = registerAndLogin(email);
        mvc.perform(patch("/api/v1/users/me").header("Authorization", "Bearer " + access)
                .contentType(MediaType.APPLICATION_JSON).content("{\"phone\":\"3009999999\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phone").value("3009999999"));
        assertThat(jdbc.queryForObject("select phone from users where email=?", String.class, email)).isEqualTo("3009999999");
        assertThat(jdbc.queryForObject("select first_name from users where email=?", String.class, email)).isEqualTo("Ana");
    }

    @Test void patchMeRejectsPayloadsThatTouchImmutableFields() throws Exception {
        String email = uniqueEmail();
        String access = registerAndLogin(email);
        mvc.perform(patch("/api/v1/users/me").header("Authorization", "Bearer " + access)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"phone\":\"3009999999\",\"firstName\":\"Hackeado\"}"))
                .andExpect(status().isBadRequest());
        assertThat(jdbc.queryForObject("select first_name from users where email=?", String.class, email)).isEqualTo("Ana");
        assertThat(jdbc.queryForObject("select phone from users where email=?", String.class, email)).isEqualTo("3000000000");
    }

    @Test void patchMeOnlyAffectsTheAuthenticatedUser() throws Exception {
        String emailA = uniqueEmail(); String accessA = registerAndLogin(emailA);
        String emailB = uniqueEmail(); registerAndLogin(emailB);
        mvc.perform(patch("/api/v1/users/me").header("Authorization", "Bearer " + accessA)
                .contentType(MediaType.APPLICATION_JSON).content("{\"phone\":\"3001111111\"}"))
                .andExpect(status().isOk());
        assertThat(jdbc.queryForObject("select phone from users where email=?", String.class, emailA)).isEqualTo("3001111111");
        assertThat(jdbc.queryForObject("select phone from users where email=?", String.class, emailB)).isEqualTo("3000000000");
    }
}
