package co.com.fcv.training.citas.adapter.events;

import co.com.fcv.training.citas.application.Ports;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.net.URI;
import java.time.Clock;

/** Selects the WF-002 webhook adapter when app.n8n.status-webhook.url is set; otherwise the no-op adapter. */
@Configuration
class AppointmentEventsConfig {
    @Bean Ports.AppointmentEvents appointmentEvents(@Value("${app.n8n.status-webhook.url:}") String url,
                                                    @Value("${app.n8n.status-webhook.bearer-token:}") String token,
                                                    ObjectMapper json, Clock clock) {
        if (url == null || url.isBlank()) return new NoOpAppointmentEventsAdapter();
        if (token == null || token.isBlank())
            throw new IllegalStateException("N8N_STATUS_WEBHOOK_BEARER_TOKEN is required when N8N_STATUS_WEBHOOK_URL is set");
        return new N8nWebhookAppointmentEvents(URI.create(url.trim()), token.trim(), json, clock.getZone());
    }
}
