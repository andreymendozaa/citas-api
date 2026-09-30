package co.com.fcv.training.citas;

import co.com.fcv.training.citas.adapter.security.JwtTokens;
import co.com.fcv.training.citas.application.Ports;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(PasswordResetIntegrationTest.CapturingNotifications.class)
class PasswordResetIntegrationTest extends DatabaseIntegrationSupport {
    private static final String ACCESS_KEY = UUID.randomUUID() + UUID.randomUUID().toString();
    private static final String REFRESH_KEY = UUID.randomUUID() + UUID.randomUUID().toString();

    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        registry.add("app.jwt.access-secret", () -> ACCESS_KEY);
        registry.add("app.jwt.refresh-secret", () -> REFRESH_KEY);
        registry.add("app.cookie.secure", () -> true);
        registry.add("app.cookie.same-site", () -> "None");
    }

    /** Captures issued recovery tokens in memory, standing in for real delivery so tests can read the raw value. */
    static class CapturingMailbox implements Ports.PasswordResetNotifications {
        final ConcurrentHashMap<String, String> tokens = new ConcurrentHashMap<>();
        public void publish(Long userId, String email, String rawToken, Instant expiresAt) { tokens.put(email, rawToken); }
    }
    @TestConfiguration
    static class CapturingNotifications {
        @Bean @Primary CapturingMailbox capturingMailbox() { return new CapturingMailbox(); }
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired JdbcTemplate jdbc;
    @Autowired JwtTokens jwt;
    @Autowired CapturingMailbox mailbox;

    private String uniqueEmail() { return "reset-" + UUID.randomUUID() + "@example.test"; }
    private String register(String email) throws Exception {
        String doc = UUID.randomUUID().toString();
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                {"firstName":"Ana","lastName":"Reset","documentType":"CC","documentNumber":"%s","email":"%s","phone":"3000000000","password":"SyntheticPass123!"}
                """.formatted(doc, email))).andExpect(status().isCreated());
        return doc;
    }
    private String login(String email) throws Exception {
        return mvc.perform(post("/api/v1/auth/login").header("X-Requested-With", "XMLHttpRequest")
                .contentType(MediaType.APPLICATION_JSON).content("""
                    {"email":"%s","password":"SyntheticPass123!"}
                    """.formatted(email)))
                .andExpect(status().isOk()).andReturn().getResponse().getCookie("refresh_token").getValue();
    }
    private void recover(String email) throws Exception {
        mvc.perform(post("/api/v1/auth/password-recovery").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\"}".formatted(email))).andExpect(status().isAccepted());
    }
    private static String sha256Hex(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) { throw new IllegalStateException(e); }
    }

    @Test void recoveryAlwaysAcceptsAndNeverLeaksAccountExistence() throws Exception {
        String email = uniqueEmail();
        register(email);
        recover(email);
        recover(uniqueEmail());
        assertThat(mailbox.tokens).containsKey(email);
        assertThat(jdbc.queryForObject(
                "select count(*) from password_reset_tokens t join users u on u.id=t.user_id where u.email=?",
                Integer.class, email)).isEqualTo(1);
    }

    @Test void resetPasswordHashesUpdatesAndRevokesAllSessions() throws Exception {
        String email = uniqueEmail();
        register(email);
        String firstRefresh = login(email);
        String secondRefresh = login(email);
        recover(email);
        String token = mailbox.tokens.get(email);
        assertThat(token).isNotBlank();

        mvc.perform(post("/api/v1/auth/password-reset").contentType(MediaType.APPLICATION_JSON).content("""
                {"token":"%s","newPassword":"NewSyntheticPass456!","confirmation":"NewSyntheticPass456!"}
                """.formatted(token))).andExpect(status().isNoContent());

        String hash = jdbc.queryForObject("select password_hash from users where email=?", String.class, email);
        assertThat(hash).startsWith("$2").isNotEqualTo("SyntheticPass123!");
        assertThat(jdbc.queryForObject("select used_at is not null from password_reset_tokens where token_hash=?",
                Boolean.class, sha256Hex(token))).isTrue();

        mvc.perform(post("/api/v1/auth/refresh").header("X-Requested-With", "XMLHttpRequest")
                .cookie(new Cookie("refresh_token", firstRefresh))).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/refresh").header("X-Requested-With", "XMLHttpRequest")
                .cookie(new Cookie("refresh_token", secondRefresh))).andExpect(status().isUnauthorized());

        // Old password no longer works; new one does.
        mvc.perform(post("/api/v1/auth/login").header("X-Requested-With", "XMLHttpRequest")
                .contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"%s\",\"password\":\"SyntheticPass123!\"}".formatted(email)))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/login").header("X-Requested-With", "XMLHttpRequest")
                .contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"%s\",\"password\":\"NewSyntheticPass456!\"}".formatted(email)))
                .andExpect(status().isOk());

        // A used token can never be replayed.
        mvc.perform(post("/api/v1/auth/password-reset").contentType(MediaType.APPLICATION_JSON).content("""
                {"token":"%s","newPassword":"AnotherPass789!","confirmation":"AnotherPass789!"}
                """.formatted(token))).andExpect(status().isBadRequest());
    }

    @Test void resetPasswordRejectsExpiredToken() throws Exception {
        String email = uniqueEmail();
        register(email);
        recover(email);
        String token = mailbox.tokens.get(email);
        jdbc.update("update password_reset_tokens set expires_at=? where token_hash=?",
                java.sql.Timestamp.from(Instant.now().minus(1, ChronoUnit.MINUTES)), sha256Hex(token));

        mvc.perform(post("/api/v1/auth/password-reset").contentType(MediaType.APPLICATION_JSON).content("""
                {"token":"%s","newPassword":"NewSyntheticPass456!","confirmation":"NewSyntheticPass456!"}
                """.formatted(token))).andExpect(status().isBadRequest());
    }

    @Test void resetPasswordRejectsUnknownTokenAndMismatchedConfirmation() throws Exception {
        mvc.perform(post("/api/v1/auth/password-reset").contentType(MediaType.APPLICATION_JSON).content("""
                {"token":"does-not-exist","newPassword":"NewSyntheticPass456!","confirmation":"NewSyntheticPass456!"}
                """)).andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/auth/password-reset").contentType(MediaType.APPLICATION_JSON).content("""
                {"token":"anything","newPassword":"NewSyntheticPass456!","confirmation":"Different123!"}
                """)).andExpect(status().isBadRequest());
    }

    @Test void localMailboxRouteDoesNotExistOutsideLocalProfile() throws Exception {
        String admin = jwt.access(1L, Set.of("ADMIN"));
        mvc.perform(get("/api/v1/admin/local-mailbox/password-resets").header("Authorization", "Bearer " + admin))
                .andExpect(status().isNotFound());
    }
}
