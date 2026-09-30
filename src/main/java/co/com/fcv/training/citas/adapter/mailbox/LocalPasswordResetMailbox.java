package co.com.fcv.training.citas.adapter.mailbox;

import co.com.fcv.training.citas.application.Ports;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Development-only, in-memory delivery of password recovery tokens (profile "local").
 * Never persisted, never logged; consulted exclusively by an ADMIN-only endpoint.
 */
public class LocalPasswordResetMailbox implements Ports.PasswordResetNotifications {
    public record Entry(String token, Instant issuedAt, Instant expiresAt) {}

    private final Map<String, Entry> byEmail = new ConcurrentHashMap<>();

    @Override
    public void publish(Long userId, String email, String rawToken, Instant expiresAt) {
        byEmail.put(email, new Entry(rawToken, Instant.now(), expiresAt));
    }

    public Optional<Entry> find(String email) {
        return Optional.ofNullable(byEmail.get(email));
    }

    public Map<String, Entry> all() {
        return Map.copyOf(byEmail);
    }
}
