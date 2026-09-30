package co.com.fcv.training.citas;

import co.com.fcv.training.citas.application.Ports;
import co.com.fcv.training.citas.application.SchedulingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import static org.assertj.core.api.Assertions.*;

/** n8n-prep: AppointmentStatusChanged fires only for admin decisions and user cancellations, after commit. */
@SpringBootTest
@Import(AppointmentEventsIntegrationTest.CapturingEvents.class)
class AppointmentEventsIntegrationTest extends DatabaseIntegrationSupport {
    @DynamicPropertySource static void properties(DynamicPropertyRegistry r) {
        r.add("app.jwt.access-secret", () -> "a".repeat(40)); r.add("app.jwt.refresh-secret", () -> "b".repeat(40)); r.add("app.cookie.secure", () -> true); r.add("app.cookie.same-site", () -> "None");
    }

    static class CapturingAppointmentEvents implements Ports.AppointmentEvents {
        final List<Ports.AppointmentStatusChanged> events = new CopyOnWriteArrayList<>();
        public void publish(Ports.AppointmentStatusChanged event) { events.add(event); }
    }
    @TestConfiguration
    static class CapturingEvents {
        @Bean @Primary CapturingAppointmentEvents capturingAppointmentEvents() { return new CapturingAppointmentEvents(); }
    }

    @Autowired SchedulingService scheduling;
    @Autowired JdbcTemplate jdbc;
    @Autowired CapturingAppointmentEvents events;

    @Test void publishesOnlyForAdminDecisionsAndUserCancellationNotForAutoApprovalOrReschedule() {
        String suffix = UUID.randomUUID().toString();
        Long professional = scheduling.createProfessional("Events","Case","CC","P"+suffix,"events-"+suffix+"@example.test","300","hash","PC"+suffix,"LIC"+suffix);
        Long owner = jdbc.queryForObject("select user_id from professionals where id=?",Long.class,professional);
        Long general = scheduling.createSpecialty("EVG"+suffix,"Eventos General "+suffix,30,true).id();
        Long specialized = scheduling.createSpecialty("EVE"+suffix,"Eventos Especial "+suffix,30,false).id();
        Long location = jdbc.queryForObject("select id from locations where active=true limit 1",Long.class);
        scheduling.setProfessionalSpecialties(professional,List.of(general,specialized),general);
        scheduling.setProfessionalLocations(professional,List.of(location));
        LocalDate date = LocalDate.now().plusDays(1);
        scheduling.createBlock(owner,location,date,LocalTime.of(8,0),LocalTime.of(10,0));
        Long patient = user("events-patient-"+suffix+"@example.test","U"+suffix);
        Long admin = user("events-admin-"+suffix+"@example.test","A"+suffix);

        SchedulingService.Appointment auto = scheduling.reserve(patient,professional,location,general,LocalDateTime.of(date,LocalTime.of(8,0)),"Auto");
        assertThat(auto.status()).isEqualTo("APPROVED");
        assertThat(events.events).isEmpty();

        SchedulingService.Appointment pending = scheduling.reserve(patient,professional,location,specialized,LocalDateTime.of(date,LocalTime.of(8,30)),"Especializada");
        assertThat(pending.status()).isEqualTo("REQUESTED");
        assertThat(events.events).isEmpty();

        scheduling.decide(admin,pending.id(),"APPROVE",null);
        assertThat(events.events).hasSize(1);
        assertThat(events.events.get(0).previousStatus()).isEqualTo("REQUESTED");
        assertThat(events.events.get(0).newStatus()).isEqualTo("APPROVED");
        assertThat(events.events.get(0).source()).isEqualTo("ADMIN");
        assertThat(events.events.get(0).actorUserId()).isEqualTo(admin);

        scheduling.cancel(patient,auto.id());
        assertThat(events.events).hasSize(2);
        assertThat(events.events.get(1).newStatus()).isEqualTo("CANCELLED");
        assertThat(events.events.get(1).source()).isEqualTo("USER");
        assertThat(events.events.get(1).actorUserId()).isEqualTo(patient);

        SchedulingService.RescheduleRequest reschedule = scheduling.requestReschedule(patient,pending.id(),location,LocalDateTime.of(date,LocalTime.of(9,0)));
        scheduling.decideReschedule(admin,reschedule.id(),"APPROVE",null);
        assertThat(events.events).hasSize(2);
    }

    private Long user(String email,String document) { jdbc.update("insert into users(first_name,last_name,document_type,document_number,email,phone,password_hash,active,email_verified) values ('Test','User','CC',?,?,?,'hash',true,false)",document,email,"300"); return jdbc.queryForObject("select id from users where email=?",Long.class,email); }
}
