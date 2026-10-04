package co.com.fcv.training.citas.adapter.events;

import co.com.fcv.training.citas.application.Ports;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * WF-002 output adapter: POSTs a PII-free JSON event to the n8n webhook with a bearer token.
 * Delivery is post-commit, asynchronous (never blocks or fails the HTTP request that caused it) and best-effort:
 * one retry on network errors or 5xx; n8n deduplicates by eventId. Logs carry only eventId and outcome.
 */
class N8nWebhookAppointmentEvents implements Ports.AppointmentEvents {
    static final String SCHEMA_VERSION = "1.0";
    private static final Logger log = LoggerFactory.getLogger(N8nWebhookAppointmentEvents.class);
    private static final int MAX_ATTEMPTS = 2;

    private final URI url; private final String token; private final ObjectMapper json; private final ZoneId zone;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    private final ExecutorService sender = Executors.newVirtualThreadPerTaskExecutor();

    N8nWebhookAppointmentEvents(URI url, String token, ObjectMapper json, ZoneId zone) {
        this.url = url; this.token = token; this.json = json; this.zone = zone;
    }

    @Override public void publish(Ports.AppointmentStatusChanged event) {
        Map<String,Object> body = envelope("appointment.status.changed", event.appointmentId(), event.occurredAt());
        body.put("previousStatus", event.previousStatus());
        body.put("status", event.newStatus());
        body.put("source", event.source());
        send(body);
    }

    @Override public void publish(Ports.RescheduleDecided event) {
        Map<String,Object> body = envelope("appointment.reschedule.decided", event.appointmentId(), event.occurredAt());
        body.put("rescheduleRequestId", event.rescheduleRequestId());
        body.put("status", event.decision());
        body.put("source", "ADMIN");
        send(body);
    }

    private Map<String,Object> envelope(String type, Long appointmentId, LocalDateTime occurredAt) {
        Map<String,Object> body = new LinkedHashMap<>();
        body.put("schemaVersion", SCHEMA_VERSION);
        body.put("eventId", UUID.randomUUID().toString());
        body.put("eventType", type);
        body.put("appointmentId", appointmentId);
        body.put("occurredAt", occurredAt.atZone(zone).toInstant().toString());
        return body;
    }

    private void send(Map<String,Object> body) {
        String payload;
        try { payload = json.writeValueAsString(body); }
        catch (JsonProcessingException e) { log.warn("n8n event {} not serialized", body.get("eventId")); return; }
        HttpRequest request = HttpRequest.newBuilder(url).timeout(Duration.ofSeconds(5))
                .header("Content-Type", "application/json").header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.ofString(payload)).build();
        Object eventId = body.get("eventId");
        sender.submit(() -> deliver(request, eventId));
    }

    private void deliver(HttpRequest request, Object eventId) {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                int status = http.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
                if (status < 500) {
                    if (status >= 300) log.warn("n8n event {} rejected with HTTP {}", eventId, status);
                    else log.info("n8n event {} delivered (HTTP {})", eventId, status);
                    return;
                }
                log.warn("n8n event {} attempt {} failed with HTTP {}", eventId, attempt, status);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); return;
            } catch (Exception e) {
                log.warn("n8n event {} attempt {} failed: {}", eventId, attempt, e.getClass().getSimpleName());
            }
            if (attempt < MAX_ATTEMPTS) {
                try { Thread.sleep(1000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }
            }
        }
    }
}
