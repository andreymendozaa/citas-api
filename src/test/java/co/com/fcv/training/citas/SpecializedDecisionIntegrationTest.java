package co.com.fcv.training.citas;

import co.com.fcv.training.citas.adapter.security.JwtTokens;
import co.com.fcv.training.citas.application.SchedulingService;
import org.junit.jupiter.api.AfterEach;
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
import org.springframework.test.web.servlet.RequestBuilder;
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

/** HU-023 initial audit of a specialized request and HU-024 guards of the ADMIN decision. */
@SpringBootTest
@AutoConfigureMockMvc
class SpecializedDecisionIntegrationTest extends DatabaseIntegrationSupport {
    @DynamicPropertySource static void properties(DynamicPropertyRegistry r) {
        r.add("app.jwt.access-secret", () -> "a".repeat(40)); r.add("app.jwt.refresh-secret", () -> "b".repeat(40)); r.add("app.cookie.secure", () -> true); r.add("app.cookie.same-site", () -> "None");
    }
    @Autowired MockMvc mvc;
    @Autowired SchedulingService scheduling;
    @Autowired JdbcTemplate jdbc;
    @Autowired JwtTokens jwt;

    Long professional, owner, general, specialized, location, patient, admin;
    LocalDate date;
    String patientToken, adminToken, professionalToken;

    @BeforeEach void fixture() {
        String suffix = UUID.randomUUID().toString();
        professional = scheduling.createProfessional("Decision","Admin","CC","P"+suffix,"decision-"+suffix+"@example.test","300","hash","PC"+suffix,"LIC"+suffix);
        owner = jdbc.queryForObject("select user_id from professionals where id=?",Long.class,professional);
        general = scheduling.createSpecialty("DCG"+suffix,"Decisión General "+suffix,30,true).id();
        specialized = scheduling.createSpecialty("DCE"+suffix,"Decisión Especial "+suffix,60,false).id();
        location = jdbc.queryForObject("select id from locations where active=true order by id limit 1",Long.class);
        scheduling.setProfessionalSpecialties(professional,List.of(general,specialized),specialized);
        scheduling.setProfessionalLocations(professional,List.of(location));
        date = LocalDate.now(ZoneId.of("America/Bogota")).plusDays(4);
        scheduling.createBlock(owner,location,date,LocalTime.of(8,0),LocalTime.of(12,0));
        patient = user("decision-patient-"+suffix+"@example.test","U"+suffix);
        admin = user("decision-admin-"+suffix+"@example.test","A"+suffix);
        patientToken = jwt.access(patient,Set.of("USER"));
        adminToken = jwt.access(admin,Set.of("ADMIN"));
        professionalToken = jwt.access(owner,Set.of("PROFESSIONAL"));
    }

    /** The test schema is persistent and other suites assume they own the only REQUESTED appointment: resolve ours even if a test failed. */
    @AfterEach void resolveLeftovers() {
        jdbc.queryForList("select a.id from appointments a join appointment_statuses s on s.id=a.status_id where a.professional_id=? and s.code='REQUESTED'",Long.class,professional)
                .forEach(id -> scheduling.decide(admin,id,"REJECT","Limpieza de prueba"));
    }

    // HU-023 CA-03: the specialized request starts its audit trail as REQUESTED, source USER, actor = patient.
    @Test void specializedRequestRecordsInitialHistoryAsUser() throws Exception {
        Long id = requestedAt(8,0);

        assertThat(historyOf(id)).containsExactly("REQUESTED|USER|" + patient);
        mvc.perform(get("/api/v1/appointments/{id}/history",id).header("Authorization","Bearer "+patientToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].status").value("REQUESTED")).andExpect(jsonPath("$[0].changeSource").value("USER"));

    }

    // HU-024 CA-03: non-admin actors, missing reason and invalid decisions never transition the request.
    @Test void invalidActorsReasonsAndDecisionsDoNotTransition() throws Exception {
        Long id = requestedAt(8,0);

        mvc.perform(decision(id,patientToken,"APPROVE",null)).andExpect(status().isForbidden());
        mvc.perform(decision(id,professionalToken,"APPROVE",null)).andExpect(status().isForbidden());
        mvc.perform(decision(id,adminToken,"REJECT",null)).andExpect(status().isBadRequest());
        mvc.perform(decision(id,adminToken,"REJECT","   ")).andExpect(status().isBadRequest());
        mvc.perform(decision(id,adminToken,"MAYBE",null)).andExpect(status().isBadRequest());

        assertThat(statusOf(id)).isEqualTo("REQUESTED");
        assertThat(slotsOf(id)).containsExactly(at(8,0),at(8,30));
        assertThat(historyOf(id)).hasSize(1);

    }

    // HU-024 CA-03: only REQUESTED specialized appointments can be decided; anything else is 404/409 without changes.
    @Test void onlyRequestedSpecializedAppointmentsCanBeDecided() throws Exception {
        Long id = requestedAt(8,0);
        Long generalApproved = scheduling.reserve(patient,professional,location,general,LocalDateTime.of(date,LocalTime.of(10,0)),"General").id();

        mvc.perform(decision(id + 999999,adminToken,"APPROVE",null)).andExpect(status().isNotFound());
        mvc.perform(decision(generalApproved,adminToken,"REJECT","No aplica")).andExpect(status().isConflict());
        assertThat(statusOf(generalApproved)).isEqualTo("APPROVED");
        assertThat(slotsOf(generalApproved)).containsExactly(at(10,0));

        mvc.perform(decision(id,adminToken,"APPROVE",null)).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("APPROVED"));
        mvc.perform(decision(id,adminToken,"REJECT","Tarde")).andExpect(status().isConflict()); // already decided
        assertThat(statusOf(id)).isEqualTo("APPROVED");
        assertThat(slotsOf(id)).containsExactly(at(8,0),at(8,30));
        assertThat(historyOf(id)).hasSize(2);

        Long cancelled = requestedAt(11,0);
        scheduling.cancel(patient,cancelled);
        mvc.perform(decision(cancelled,adminToken,"APPROVE",null)).andExpect(status().isConflict());
        assertThat(statusOf(cancelled)).isEqualTo("CANCELLED");
        assertThat(slotsOf(cancelled)).isEmpty();
    }

    private Long requestedAt(int hour,int minute) {
        SchedulingService.Appointment appointment = scheduling.reserve(patient,professional,location,specialized,at(hour,minute),"Especializada");
        assertThat(appointment.status()).isEqualTo("REQUESTED");
        return appointment.id();
    }
    private RequestBuilder decision(Long appointment,String token,String decision,String reason) {
        String body = reason == null ? "{\"decision\":\"%s\"}".formatted(decision) : "{\"decision\":\"%s\",\"reason\":\"%s\"}".formatted(decision,reason);
        return post("/api/v1/admin/appointments/{id}/decision",appointment).header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON).content(body);
    }
    private LocalDateTime at(int hour,int minute) { return LocalDateTime.of(date,LocalTime.of(hour,minute)); }
    private List<LocalDateTime> slotsOf(Long appointment) { return jdbc.queryForList("select start_at from professional_slots where appointment_id=? order by start_at",LocalDateTime.class,appointment); }
    private String statusOf(Long appointment) { return jdbc.queryForObject("select s.code from appointments a join appointment_statuses s on s.id=a.status_id where a.id=?",String.class,appointment); }
    private List<String> historyOf(Long appointment) { return jdbc.queryForList("select concat(s.code,'|',h.change_source,'|',h.changed_by_user_id) from appointment_status_history h join appointment_statuses s on s.id=h.status_id where h.appointment_id=? order by h.id",String.class,appointment); }
    private Long user(String email,String document) { jdbc.update("insert into users(first_name,last_name,document_type,document_number,email,phone,password_hash,active,email_verified) values ('Test','User','CC',?,?,?,'hash',true,false)",document,email,"300"); return jdbc.queryForObject("select id from users where email=?",Long.class,email); }
}
