package co.com.fcv.training.citas;

import co.com.fcv.training.citas.adapter.security.JwtTokens;
import co.com.fcv.training.citas.application.SchedulingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** HU-026 cancellation, HU-027 reschedule request and HU-028 reschedule decision: slots, ownership, roles and audit. */
@SpringBootTest
@AutoConfigureMockMvc
class AppointmentLifecycleIntegrationTest extends DatabaseIntegrationSupport {
    @DynamicPropertySource static void properties(DynamicPropertyRegistry r) {
        r.add("app.jwt.access-secret", () -> "a".repeat(40)); r.add("app.jwt.refresh-secret", () -> "b".repeat(40)); r.add("app.cookie.secure", () -> true); r.add("app.cookie.same-site", () -> "None");
    }
    @Autowired MockMvc mvc;
    @Autowired SchedulingService scheduling;
    @Autowired JdbcTemplate jdbc;
    @Autowired JwtTokens jwt;

    Long professional, owner, general, specialized, location, otherLocation, patient, stranger, admin;
    LocalDate date;
    String patientToken, strangerToken, adminToken, professionalToken;

    @BeforeEach void fixture() {
        String suffix = UUID.randomUUID().toString();
        professional = scheduling.createProfessional("Ciclo","Vida","CC","P"+suffix,"life-"+suffix+"@example.test","300","hash","PC"+suffix,"LIC"+suffix);
        owner = jdbc.queryForObject("select user_id from professionals where id=?",Long.class,professional);
        general = scheduling.createSpecialty("LCG"+suffix,"Ciclo General "+suffix,60,true).id();
        specialized = scheduling.createSpecialty("LCE"+suffix,"Ciclo Especial "+suffix,30,false).id();
        location = jdbc.queryForObject("select id from locations where active=true order by id limit 1",Long.class);
        otherLocation = jdbc.queryForObject("select id from locations where active=true and id<>? order by id limit 1",Long.class,location);
        scheduling.setProfessionalSpecialties(professional,List.of(general,specialized),general);
        scheduling.setProfessionalLocations(professional,List.of(location));
        date = LocalDate.now(ZoneId.of("America/Bogota")).plusDays(3);
        scheduling.createBlock(owner,location,date,LocalTime.of(8,0),LocalTime.of(12,0));
        patient = user("life-patient-"+suffix+"@example.test","U"+suffix);
        stranger = user("life-stranger-"+suffix+"@example.test","S"+suffix);
        admin = user("life-admin-"+suffix+"@example.test","A"+suffix);
        patientToken = jwt.access(patient,Set.of("USER"));
        strangerToken = jwt.access(stranger,Set.of("USER"));
        adminToken = jwt.access(admin,Set.of("ADMIN"));
        professionalToken = jwt.access(owner,Set.of("PROFESSIONAL"));
    }

    // ---------- HU-026 · cancelar cita ----------

    @Test void cancelReleasesSlotsRecordsHistoryAndCannotBeRepeated() throws Exception {
        Long id = approvedAt(8,0);
        assertThat(slotsOf(id)).containsExactly(at(8,0),at(8,30));

        mvc.perform(post("/api/v1/appointments/{id}/cancel",id).header("Authorization","Bearer "+patientToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELLED"));

        assertThat(statusOf(id)).isEqualTo("CANCELLED");
        assertThat(slotsOf(id)).isEmpty();
        assertThat(availableStarts(general)).contains(at(8,0));
        assertThat(jdbc.queryForObject("select count(*) from appointment_status_history h join appointment_statuses s on s.id=h.status_id where h.appointment_id=? and s.code='CANCELLED' and h.change_source='USER' and h.changed_by_user_id=?",Integer.class,id,patient)).isEqualTo(1);

        // CA-03: a cancelled appointment cannot be cancelled/reactivated again and history keeps a single cancellation
        mvc.perform(post("/api/v1/appointments/{id}/cancel",id).header("Authorization","Bearer "+patientToken)).andExpect(status().isConflict());
        assertThat(statusOf(id)).isEqualTo("CANCELLED");
        assertThat(jdbc.queryForObject("select count(*) from appointment_status_history h join appointment_statuses s on s.id=h.status_id where h.appointment_id=? and s.code='CANCELLED'",Integer.class,id)).isEqualTo(1);
    }

    @Test void cancelRejectsForeignMissingPastTerminalAndNonUserRequestsWithoutChanges() throws Exception {
        Long id = approvedAt(8,0);

        mvc.perform(post("/api/v1/appointments/{id}/cancel",id).header("Authorization","Bearer "+strangerToken)).andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/appointments/{id}/cancel",id + 999999).header("Authorization","Bearer "+patientToken)).andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/appointments/{id}/cancel",id).header("Authorization","Bearer "+adminToken)).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/appointments/{id}/cancel",id).header("Authorization","Bearer "+professionalToken)).andExpect(status().isForbidden());
        assertThat(statusOf(id)).isEqualTo("APPROVED");
        assertThat(slotsOf(id)).containsExactly(at(8,0),at(8,30));

        Long rejected = scheduling.reserve(patient,professional,location,specialized,LocalDateTime.of(date,LocalTime.of(11,0)),"Especializada").id();
        scheduling.decide(admin,rejected,"REJECT","Sin cupo");
        mvc.perform(post("/api/v1/appointments/{id}/cancel",rejected).header("Authorization","Bearer "+patientToken)).andExpect(status().isConflict());
        assertThat(statusOf(rejected)).isEqualTo("REJECTED");

        // An appointment whose start already passed is no longer cancellable
        jdbc.update("update appointments set scheduled_start_at=?,scheduled_end_at=? where id=?",LocalDateTime.now().minusDays(1),LocalDateTime.now().minusDays(1).plusHours(1),id);
        mvc.perform(post("/api/v1/appointments/{id}/cancel",id).header("Authorization","Bearer "+patientToken)).andExpect(status().isConflict());
        assertThat(statusOf(id)).isEqualTo("APPROVED");
    }

    @Test void cancellingWithPendingRescheduleCancelsTheRequestAndReleasesBothRanges() throws Exception {
        Long id = approvedAt(8,0);
        Long request = requestRescheduleAt(id,10,0);
        assertThat(slotsOf(id)).containsExactly(at(8,0),at(8,30),at(10,0),at(10,30));

        mvc.perform(post("/api/v1/appointments/{id}/cancel",id).header("Authorization","Bearer "+patientToken)).andExpect(status().isOk());

        assertThat(slotsOf(id)).isEmpty();
        assertThat(rescheduleStatusOf(request)).isEqualTo("CANCELLED");
        assertThat(availableStarts(general)).contains(at(8,0),at(10,0));
    }

    // ---------- HU-027 · solicitar reprogramación ----------

    @Test void rescheduleRetainsTheNewRangeAndKeepsTheOriginalUntilDecision() throws Exception {
        Long id = approvedAt(8,0);

        Long request = requestRescheduleAt(id,10,0);

        assertThat(rescheduleStatusOf(request)).isEqualTo("PENDING");
        assertThat(statusOf(id)).isEqualTo("APPROVED");
        assertThat(jdbc.queryForObject("select scheduled_start_at from appointments where id=?",LocalDateTime.class,id)).isEqualTo(at(8,0));
        assertThat(slotsOf(id)).containsExactly(at(8,0),at(8,30),at(10,0),at(10,30));
        assertThat(availableStarts(general)).doesNotContain(at(8,0),at(10,0));
    }

    @Test void rescheduleRejectsIneligibleRequestsWithoutSideEffects() throws Exception {
        Long id = approvedAt(8,0);
        Long blocker = scheduling.reserve(stranger,professional,location,general,LocalDateTime.of(date,LocalTime.of(10,0)),"Ocupa 10:00").id();
        Long requested = scheduling.reserve(patient,professional,location,specialized,LocalDateTime.of(date,LocalTime.of(11,30)),"Pendiente").id();

        mvc.perform(reschedule(id,strangerToken,location,date,"09:00")).andExpect(status().isNotFound());
        mvc.perform(reschedule(id + 999999,patientToken,location,date,"09:00")).andExpect(status().isNotFound());
        mvc.perform(reschedule(id,adminToken,location,date,"09:00")).andExpect(status().isForbidden());
        mvc.perform(reschedule(id,patientToken,location,date,"10:00")).andExpect(status().isConflict()); // range already taken
        mvc.perform(reschedule(id,patientToken,otherLocation,date,"09:00")).andExpect(status().isConflict()); // location not assigned
        mvc.perform(reschedule(id,patientToken,location,LocalDate.now(ZoneId.of("America/Bogota")).minusDays(1),"09:00")).andExpect(status().isConflict()); // past
        mvc.perform(reschedule(requested,patientToken,location,date,"09:00")).andExpect(status().isConflict()); // not APPROVED

        assertThat(rescheduleCountOf(id)).isZero();
        assertThat(slotsOf(id)).containsExactly(at(8,0),at(8,30));
        assertThat(slotsOf(blocker)).containsExactly(at(10,0),at(10,30));

        requestRescheduleAt(id,9,0);
        mvc.perform(reschedule(id,patientToken,location,date,"11:00")).andExpect(status().isConflict()); // already pending
        assertThat(rescheduleCountOf(id)).isEqualTo(1);

        // The test schema is persistent and other suites assume they own the only REQUESTED appointment: resolve ours.
        scheduling.decide(admin,requested,"REJECT","Limpieza de prueba");
    }

    // ---------- HU-028 · resolver reprogramación ----------

    @Test void approvalSwapsRangesUpdatesTheAppointmentAndAudits() throws Exception {
        Long id = approvedAt(8,0);
        Long request = requestRescheduleAt(id,10,0);

        mvc.perform(decision(request,adminToken,"APPROVE",null)).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("APPROVED"));

        assertThat(rescheduleStatusOf(request)).isEqualTo("APPROVED");
        assertThat(slotsOf(id)).containsExactly(at(10,0),at(10,30));
        assertThat(jdbc.queryForObject("select scheduled_start_at from appointments where id=?",LocalDateTime.class,id)).isEqualTo(at(10,0));
        assertThat(statusOf(id)).isEqualTo("APPROVED");
        assertThat(availableStarts(general)).contains(at(8,0));
        assertThat(jdbc.queryForObject("select count(*) from appointment_status_history where appointment_id=? and change_source='ADMIN' and changed_by_user_id=?",Integer.class,id,admin)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select decided_by_user_id from reschedule_requests where id=?",Long.class,request)).isEqualTo(admin);
    }

    @Test void rejectionRequiresReasonReleasesTheProvisionalRangeAndKeepsTheOriginal() throws Exception {
        Long id = approvedAt(8,0);
        Long request = requestRescheduleAt(id,10,0);

        mvc.perform(decision(request,adminToken,"REJECT",null)).andExpect(status().isBadRequest());
        assertThat(rescheduleStatusOf(request)).isEqualTo("PENDING");
        assertThat(slotsOf(id)).hasSize(4);

        mvc.perform(decision(request,adminToken,"REJECT","Sin cupo en la franja")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("REJECTED"));

        assertThat(rescheduleStatusOf(request)).isEqualTo("REJECTED");
        assertThat(jdbc.queryForObject("select decision_reason from reschedule_requests where id=?",String.class,request)).isEqualTo("Sin cupo en la franja");
        assertThat(slotsOf(id)).containsExactly(at(8,0),at(8,30));
        assertThat(jdbc.queryForObject("select scheduled_start_at from appointments where id=?",LocalDateTime.class,id)).isEqualTo(at(8,0));
        assertThat(availableStarts(general)).contains(at(10,0));

        // RF-15: after a rejection the user may still cancel the original appointment
        mvc.perform(post("/api/v1/appointments/{id}/cancel",id).header("Authorization","Bearer "+patientToken)).andExpect(status().isOk());
    }

    @Test void decisionIsRestrictedToAdminAndToPendingRequests() throws Exception {
        Long id = approvedAt(8,0);
        Long request = requestRescheduleAt(id,10,0);

        mvc.perform(decision(request,patientToken,"APPROVE",null)).andExpect(status().isForbidden());
        mvc.perform(decision(request,professionalToken,"APPROVE",null)).andExpect(status().isForbidden());
        mvc.perform(decision(request,adminToken,"MAYBE",null)).andExpect(status().isBadRequest());
        mvc.perform(decision(request + 999999,adminToken,"APPROVE",null)).andExpect(status().isNotFound());
        assertThat(rescheduleStatusOf(request)).isEqualTo("PENDING");
        assertThat(slotsOf(id)).hasSize(4);

        mvc.perform(decision(request,adminToken,"APPROVE",null)).andExpect(status().isOk());
        mvc.perform(decision(request,adminToken,"REJECT","Tarde")).andExpect(status().isConflict()); // already resolved
        assertThat(rescheduleStatusOf(request)).isEqualTo("APPROVED");
        assertThat(slotsOf(id)).containsExactly(at(10,0),at(10,30));
    }

    // ---------- helpers ----------

    private Long approvedAt(int hour,int minute) {
        SchedulingService.Appointment appointment = scheduling.reserve(patient,professional,location,general,LocalDateTime.of(date,LocalTime.of(hour,minute)),"Ciclo de vida");
        assertThat(appointment.status()).isEqualTo("APPROVED");
        return appointment.id();
    }
    private Long requestRescheduleAt(Long appointment,int hour,int minute) throws Exception {
        String body = mvc.perform(reschedule(appointment,patientToken,location,date,"%02d:%02d".formatted(hour,minute)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("PENDING")).andReturn().getResponse().getContentAsString();
        return Long.valueOf(body.replaceAll(".*\"id\":(\\d+).*","$1"));
    }
    private org.springframework.test.web.servlet.RequestBuilder reschedule(Long appointment,String token,Long loc,LocalDate day,String time) {
        return post("/api/v1/appointments/{id}/reschedule",appointment).header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"locationId\":%d,\"date\":\"%s\",\"startTime\":\"%s\"}".formatted(loc,day,time));
    }
    private org.springframework.test.web.servlet.RequestBuilder decision(Long request,String token,String decision,String reason) {
        String body = reason == null ? "{\"decision\":\"%s\"}".formatted(decision) : "{\"decision\":\"%s\",\"reason\":\"%s\"}".formatted(decision,reason);
        return post("/api/v1/admin/reschedule-requests/{id}/decision",request).header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON).content(body);
    }
    private LocalDateTime at(int hour,int minute) { return LocalDateTime.of(date,LocalTime.of(hour,minute)); }
    private List<LocalDateTime> slotsOf(Long appointment) { return jdbc.queryForList("select start_at from professional_slots where appointment_id=? order by start_at",LocalDateTime.class,appointment); }
    private List<LocalDateTime> availableStarts(Long specialty) { return scheduling.availability(location,specialty,professional,date).stream().map(SchedulingService.Available::startAt).toList(); }
    private String statusOf(Long appointment) { return jdbc.queryForObject("select s.code from appointments a join appointment_statuses s on s.id=a.status_id where a.id=?",String.class,appointment); }
    private String rescheduleStatusOf(Long request) { return jdbc.queryForObject("select s.code from reschedule_requests r join reschedule_request_statuses s on s.id=r.status_id where r.id=?",String.class,request); }
    private int rescheduleCountOf(Long appointment) { return jdbc.queryForObject("select count(*) from reschedule_requests where appointment_id=?",Integer.class,appointment); }
    private Long user(String email,String document) { jdbc.update("insert into users(first_name,last_name,document_type,document_number,email,phone,password_hash,active,email_verified) values ('Test','User','CC',?,?,?,'hash',true,false)",document,email,"300"); return jdbc.queryForObject("select id from users where email=?",Long.class,email); }
}
