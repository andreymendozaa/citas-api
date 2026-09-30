package co.com.fcv.training.citas.adapter.persistence;

import co.com.fcv.training.citas.application.Ports;
import co.com.fcv.training.citas.domain.PasswordResetToken;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.util.Optional;

@Repository
class PasswordResetJpaAdapter implements Ports.PasswordResets {
    private final PasswordResetTokensJpa tokens;
    PasswordResetJpaAdapter(PasswordResetTokensJpa tokens) { this.tokens = tokens; }

    public void save(PasswordResetToken token) {
        PasswordResetTokenEntity e = new PasswordResetTokenEntity();
        e.userId = token.userId();
        e.tokenHash = token.tokenHash();
        e.expiresAt = token.expiresAt();
        e.usedAt = token.usedAt();
        tokens.saveAndFlush(e);
    }

    public Optional<PasswordResetToken> lockByTokenHash(String hash) {
        return tokens.lockByTokenHash(hash).map(e -> new PasswordResetToken(e.id, e.userId, e.tokenHash, e.expiresAt, e.usedAt));
    }

    public void markUsed(Long id, Instant when) {
        PasswordResetTokenEntity e = tokens.findById(id).orElseThrow();
        e.usedAt = when;
        tokens.saveAndFlush(e);
    }
}
