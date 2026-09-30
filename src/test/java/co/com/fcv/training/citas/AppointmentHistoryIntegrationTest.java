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

/** HU-032: appointment status history is readable only within ownership/role, never editable. */
@SpringBootTest
@AutoConfigureMockMvc
class AppointmentHistoryIntegrationTest extends DatabaseIntegrationSupport {
    @DynamicPropertySource static void properties(DynamicPropertyRegistry r) {
        r.add("app.jwt.access-secret", () -> "a".repeat(40)); r.add("app.jwt.refresh-secret", () -> "b".repeat(40)); r.add("app.cookie.secure", () -> true); r.add("app.cookie.same-site", () -> "None");
    }
    @Autowired MockMvc mvc;
    @Autowired SchedulingService scheduling;
    @Autowired JdbcTemplate jdbc;
    @Autowired JwtTokens jwt;

    @Test void historyRespectsOwnershipAcrossRolesAndIsInvisibleOutsideScope() throws Exception {
        String suffix = UUID.randomUUID().toString();
        Long professional = scheduling.createProfessional("History","Case","CC","P"+suffix,"history-"+suffix+"@example.test","300","hash","PC"+suffix,"LIC"+suffix);
        Long owner = jdbc.queryForObject("select user_id from professionals where id=?",Long.class,professional);
        Long otherProfessional = scheduling.createProfessional("Other","History","CC","P2"+suffix,"other-history-"+suffix+"@example.test","300","hash","PC2"+suffix,"LIC2"+suffix);
        Long otherOwner = jdbc.queryForObject("select user_id from professionals where id=?",Long.class,otherProfessional);
        Long specialty = scheduling.createSpecialty("HIS"+suffix,"Historial "+suffix,30,true).id();
        Long location = jdbc.queryForObject("select id from locations where active=true limit 1",Long.class);
        scheduling.setProfessionalSpecialties(professional,List.of(specialty),specialty);
        scheduling.setProfessionalLocations(professional,List.of(location));
        LocalDate date = LocalDate.now().plusDays(1);
        scheduling.createBlock(owner,location,date,LocalTime.of(8,0),LocalTime.of(9,0));
        Long patient = user("history-patient-"+suffix+"@example.test","U"+suffix);
        SchedulingService.Appointment appointment = scheduling.reserve(patient,professional,location,specialty,LocalDateTime.of(date,LocalTime.of(8,0)),"Historial");
        assertThat(appointment.status()).isEqualTo("APPROVED");

        String userToken = jwt.access(patient, Set.of("USER"));
        String professionalToken = jwt.access(owner, Set.of("PROFESSIONAL"));
        String adminToken = jwt.access(1L, Set.of("ADMIN"));
        String foreignUserToken = jwt.access(patient + 999999L, Set.of("USER"));
        String foreignProfessionalToken = jwt.access(otherOwner, Set.of("PROFESSIONAL"));

        mvc.perform(get("/api/v1/appointments/{id}/history", appointment.id()).header("Authorization","Bearer "+userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("APPROVED"))
                .andExpect(jsonPath("$[0].changeSource").value("SYSTEM"));
        mvc.perform(get("/api/v1/appointments/{id}/history", appointment.id()).header("Authorization","Bearer "+professionalToken))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/appointments/{id}/history", appointment.id()).header("Authorization","Bearer "+adminToken))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/appointments/{id}/history", appointment.id()).header("Authorization","Bearer "+foreignUserToken))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/appointments/{id}/history", appointment.id()).header("Authorization","Bearer "+foreignProfessionalToken))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/appointments/{id}/history", appointment.id()))
                .andExpect(status().isUnauthorized());
    }

    private Long user(String email,String document) { jdbc.update("insert into users(first_name,last_name,document_type,document_number,email,phone,password_hash,active,email_verified) values ('Test','User','CC',?,?,?,'hash',true,false)",document,email,"300"); return jdbc.queryForObject("select id from users where email=?",Long.class,email); }
}
