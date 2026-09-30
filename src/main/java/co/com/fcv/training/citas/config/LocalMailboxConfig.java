package co.com.fcv.training.citas.config;

import co.com.fcv.training.citas.adapter.mailbox.LocalPasswordResetMailbox;
import co.com.fcv.training.citas.application.Ports;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

/**
 * Wires the local-only password reset mailbox (profile "local") and a no-op elsewhere,
 * so the application core never knows whether a mailbox exists.
 */
@Configuration
class LocalMailboxConfig {
    @Bean
    @Profile("local")
    LocalPasswordResetMailbox localPasswordResetMailbox() {
        return new LocalPasswordResetMailbox();
    }

    @Bean
    @Primary
    @Profile("local")
    Ports.PasswordResetNotifications localNotifications(LocalPasswordResetMailbox mailbox) {
        return mailbox;
    }

    @Bean
    @Profile("!local")
    Ports.PasswordResetNotifications noOpNotifications() {
        return (userId, email, rawToken, expiresAt) -> { };
    }
}
