package co.com.fcv.training.citas.domain;

import java.time.Instant;

public record PasswordResetToken(Long id, Long userId, String tokenHash, Instant expiresAt, Instant usedAt) {
    public boolean activeAt(Instant now) {
        return usedAt == null && expiresAt.isAfter(now);
    }
}
