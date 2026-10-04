package co.com.fcv.training.citas.application;

import co.com.fcv.training.citas.domain.Account;
import co.com.fcv.training.citas.domain.Identity;
import co.com.fcv.training.citas.domain.PasswordResetToken;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

public class PasswordResetService {
    private final Ports.Accounts accounts;
    private final Ports.PasswordResets resets;
    private final Ports.Passwords passwords;
    private final Ports.Sessions sessions;
    private final Ports.Transactions transactions;
    private final Ports.PasswordResetNotifications notifications;
    private final Clock clock;
    private final Duration ttl;
    private final SecureRandom random = new SecureRandom();

    public PasswordResetService(Ports.Accounts accounts, Ports.PasswordResets resets, Ports.Passwords passwords,
                                Ports.Sessions sessions, Ports.Transactions transactions,
                                Ports.PasswordResetNotifications notifications, Clock clock, Duration ttl) {
        this.accounts = accounts;
        this.resets = resets;
        this.passwords = passwords;
        this.sessions = sessions;
        this.transactions = transactions;
        this.notifications = notifications;
        this.clock = clock;
        this.ttl = ttl;
    }

    /** Always completes the same way regardless of whether the email exists, to avoid user enumeration. */
    public void requestRecovery(String email) {
        String normalized = Identity.email(email);
        transactions.run(() -> {
            accounts.byEmail(normalized).ifPresent(this::issueToken);
            return null;
        });
    }

    private void issueToken(Account account) {
        String raw = randomToken();
        Instant expiresAt = clock.instant().plus(ttl);
        resets.save(new PasswordResetToken(null, account.id(), AuthService.hash(raw), expiresAt, null));
        notifications.publish(account.id(), account.email(), raw, expiresAt);
    }

    public void resetPassword(String rawToken, String newPassword, String confirmation) {
        transactions.run(() -> {
            if (rawToken == null || rawToken.isBlank()) throw new PasswordResetFailure();
            if (newPassword == null || newPassword.isBlank() || !newPassword.equals(confirmation)) throw new PasswordResetFailure();
            if (newPassword.getBytes(StandardCharsets.UTF_8).length > 72) throw new PasswordResetFailure();
            PasswordResetToken token = resets.lockByTokenHash(AuthService.hash(rawToken)).orElseThrow(PasswordResetFailure::new);
            if (!token.activeAt(clock.instant())) throw new PasswordResetFailure();
            accounts.updatePasswordHash(token.userId(), passwords.hash(newPassword));
            resets.markUsed(token.id(), clock.instant());
            sessions.revokeAllByUserId(token.userId(), clock.instant());
            return null;
        });
    }

    private String randomToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
