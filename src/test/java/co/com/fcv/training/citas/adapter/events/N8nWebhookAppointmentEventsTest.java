package co.com.fcv.training.citas.adapter.events;

import co.com.fcv.training.citas.application.Ports;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;

/** HU-035 / WF-002: the webhook adapter sends an authenticated, PII-free, versioned payload and retries once on 5xx. */
class N8nWebhookAppointmentEventsTest {
    record Received(String authorization, String contentType, String body) {}

    final ObjectMapper json = new ObjectMapper();
    final List<Received> received = new CopyOnWriteArrayList<>();
    final AtomicInteger failuresLeft = new AtomicInteger();
    HttpServer server;
    N8nWebhookAppointmentEvents adapter;

    @BeforeEach void start() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/webhook/test", exchange -> {
            received.add(new Received(exchange.getRequestHeaders().getFirst("Authorization"),
                    exchange.getRequestHeaders().getFirst("Content-Type"),
                    new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)));
            int status = failuresLeft.getAndUpdate(n -> Math.max(0, n - 1)) > 0 ? 503 : 202;
            exchange.sendResponseHeaders(status, -1);
            exchange.close();
        });
        server.start();
        URI url = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/webhook/test");
        adapter = new N8nWebhookAppointmentEvents(url, "test-token", json, ZoneId.of("America/Bogota"));
    }

    @AfterEach void stop() { server.stop(0); }

    @Test void statusChangeIsPostedWithBearerTokenAndMinimalPayload() throws Exception {
        adapter.publish(new Ports.AppointmentStatusChanged(41L, "REQUESTED", "APPROVED", "ADMIN", 7L, LocalDateTime.of(2026, 10, 5, 9, 30)));
        await().atMost(5, TimeUnit.SECONDS).until(() -> received.size() == 1);

        Received request = received.get(0);
        assertThat(request.authorization()).isEqualTo("Bearer test-token");
        assertThat(request.contentType()).isEqualTo("application/json");
        JsonNode body = json.readTree(request.body());
        assertThat(body.get("schemaVersion").asText()).isEqualTo("1.0");
        assertThat(body.get("eventType").asText()).isEqualTo("appointment.status.changed");
        assertThat(body.get("eventId").asText()).matches("[0-9a-f-]{36}");
        assertThat(body.get("appointmentId").asLong()).isEqualTo(41L);
        assertThat(body.get("previousStatus").asText()).isEqualTo("REQUESTED");
        assertThat(body.get("status").asText()).isEqualTo("APPROVED");
        assertThat(body.get("source").asText()).isEqualTo("ADMIN");
        assertThat(body.get("occurredAt").asText()).isEqualTo("2026-10-05T14:30:00Z");
        assertThat(body.has("actorUserId")).isFalse();
        assertThat(body.size()).isEqualTo(8);
    }

    @Test void rescheduleDecisionIsPostedAsItsOwnEventType() throws Exception {
        adapter.publish(new Ports.RescheduleDecided(41L, 3L, "REJECTED", 7L, LocalDateTime.of(2026, 10, 5, 9, 30)));
        await().atMost(5, TimeUnit.SECONDS).until(() -> received.size() == 1);

        JsonNode body = json.readTree(received.get(0).body());
        assertThat(body.get("eventType").asText()).isEqualTo("appointment.reschedule.decided");
        assertThat(body.get("appointmentId").asLong()).isEqualTo(41L);
        assertThat(body.get("rescheduleRequestId").asLong()).isEqualTo(3L);
        assertThat(body.get("status").asText()).isEqualTo("REJECTED");
        assertThat(body.get("source").asText()).isEqualTo("ADMIN");
        assertThat(body.has("actorUserId")).isFalse();
    }

    @Test void serverErrorIsRetriedOnceWithTheSameEventId() throws Exception {
        failuresLeft.set(1);
        adapter.publish(new Ports.AppointmentStatusChanged(42L, "APPROVED", "CANCELLED", "USER", 9L, LocalDateTime.of(2026, 10, 5, 9, 30)));
        await().atMost(6, TimeUnit.SECONDS).until(() -> received.size() == 2);

        assertThat(json.readTree(received.get(1).body()).get("eventId").asText())
                .isEqualTo(json.readTree(received.get(0).body()).get("eventId").asText());
        TimeUnit.MILLISECONDS.sleep(1500);
        assertThat(received).hasSize(2);
    }

    @Test void unreachableWebhookNeverThrowsToTheCaller() {
        server.stop(0);
        assertThatCode(() -> adapter.publish(new Ports.AppointmentStatusChanged(43L, "APPROVED", "CANCELLED", "USER", 9L, LocalDateTime.now())))
                .doesNotThrowAnyException();
    }
}
