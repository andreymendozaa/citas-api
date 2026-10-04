package co.com.fcv.training.citas;

import co.com.fcv.training.citas.adapter.security.JwtTokens;
import co.com.fcv.training.citas.application.SchedulingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** HU-031: unified admin inbox combining specialized REQUESTED appointments and PENDING reschedule requests. */
@SpringBootTest
@AutoConfigureMockMvc
class InboxIntegrationTest extends DatabaseIntegrationSupport {
    @DynamicPropertySource static void properties(DynamicPropertyRegistry r) {
        r.add("app.jwt.access-secret", () -> "a".repeat(40)); r.add("app.jwt.refresh-secret", () -> "b".repeat(40)); r.add("app.cookie.secure", () -> true); r.add("app.cookie.same-site", () -> "None");
    }
    @Autowired MockMvc mvc;
    @Autowired SchedulingService scheduling;
    @Autowired JdbcTemplate jdbc;
    @Autowired JwtTokens jwt;

    @Test void inboxCombinesSpecializedAndRescheduleAndAppliesFiltersAdminOnly() throws Exception {
        String suffix = UUID.randomUUID().toString();
        Long professional = scheduling.createProfessional("Inbox","Case","CC","P"+suffix,"inbox-"+suffix+"@example.test","300","hash","PC"+suffix,"LIC"+suffix);
        Long owner = jdbc.queryForObject("select user_id from professionals where id=?",Long.class,professional);
        Long general = scheduling.createSpecialty("INBG"+suffix,"Inbox General "+suffix,30,true).id();
        Long specialized = scheduling.createSpecialty("INBE"+suffix,"Inbox Especial "+suffix,30,false).id();
        Long location = jdbc.queryForObject("select id from locations where active=true limit 1",Long.class);
        scheduling.setProfessionalSpecialties(professional,List.of(general,specialized),general);
        scheduling.setProfessionalLocations(professional,List.of(location));
        LocalDate date = LocalDate.now().plusDays(1);
        scheduling.createBlock(owner,location,date,LocalTime.of(8,0),LocalTime.of(10,0));
        Long patient = user("inbox-patient-"+suffix+"@example.test","U"+suffix);
        Long admin = user("inbox-admin-"+suffix+"@example.test","A"+suffix);

        SchedulingService.Appointment approvedGeneral = scheduling.reserve(patient,professional,location,general,LocalDateTime.of(date,LocalTime.of(8,0)),"General");
        SchedulingService.Appointment pendingSpecialized = scheduling.reserve(patient,professional,location,specialized,LocalDateTime.of(date,LocalTime.of(8,30)),"Especializada pendiente");
        assertThat(pendingSpecialized.status()).isEqualTo("REQUESTED");
        SchedulingService.RescheduleRequest reschedule = scheduling.requestReschedule(patient,approvedGeneral.id(),location,LocalDateTime.of(date,LocalTime.of(9,0)));

        String user = jwt.access(patient, Set.of("USER"));
        String professionalToken = jwt.access(owner, Set.of("PROFESSIONAL"));
        String adminToken = jwt.access(admin, Set.of("ADMIN"));

        mvc.perform(get("/api/v1/admin/inbox").header("Authorization","Bearer "+user)).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/admin/inbox").header("Authorization","Bearer "+professionalToken)).andExpect(status().isForbidden());

        mvc.perform(get("/api/v1/admin/inbox").header("Authorization","Bearer "+adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.type=='SPECIALIZED' && @.id=="+pendingSpecialized.id()+")]").isNotEmpty())
                .andExpect(jsonPath("$[?(@.type=='RESCHEDULE' && @.id=="+reschedule.id()+")]").isNotEmpty());

        mvc.perform(get("/api/v1/admin/inbox").param("specialtyId",specialized.toString()).header("Authorization","Bearer "+adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.type=='SPECIALIZED')]").isNotEmpty())
                .andExpect(jsonPath("$[?(@.type=='RESCHEDULE')]").isEmpty());

        scheduling.decide(admin,pendingSpecialized.id(),"APPROVE",null);
        mvc.perform(get("/api/v1/admin/inbox").header("Authorization","Bearer "+adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.type=='SPECIALIZED' && @.id=="+pendingSpecialized.id()+")]").isEmpty());
    }

    private Long user(String email,String document) { jdbc.update("insert into users(first_name,last_name,document_type,document_number,email,phone,password_hash,active,email_verified) values ('Test','User','CC',?,?,?,'hash',true,false)",document,email,"300"); return jdbc.queryForObject("select id from users where email=?",Long.class,email); }
}
