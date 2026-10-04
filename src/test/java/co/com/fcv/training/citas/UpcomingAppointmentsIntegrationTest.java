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

/** n8n-prep: read-only feed of upcoming APPROVED appointments for ADMIN/automation consumption. */
@SpringBootTest
@AutoConfigureMockMvc
class UpcomingAppointmentsIntegrationTest extends DatabaseIntegrationSupport {
    @DynamicPropertySource static void properties(DynamicPropertyRegistry r) {
        r.add("app.jwt.access-secret", () -> "a".repeat(40)); r.add("app.jwt.refresh-secret", () -> "b".repeat(40)); r.add("app.cookie.secure", () -> true); r.add("app.cookie.same-site", () -> "None");
    }
    @Autowired MockMvc mvc;
    @Autowired SchedulingService scheduling;
    @Autowired JdbcTemplate jdbc;
    @Autowired JwtTokens jwt;

    @Test void onlyAdminSeesUpcomingApprovedAppointmentsWithPhoneAndNoMutation() throws Exception {
        String suffix = UUID.randomUUID().toString();
        Long professional = scheduling.createProfessional("Upcoming","Case","CC","P"+suffix,"upcoming-"+suffix+"@example.test","300","hash","PC"+suffix,"LIC"+suffix);
        Long owner = jdbc.queryForObject("select user_id from professionals where id=?",Long.class,professional);
        Long specialty = scheduling.createSpecialty("UPC"+suffix,"Upcoming "+suffix,30,true).id();
        Long location = jdbc.queryForObject("select id from locations where active=true limit 1",Long.class);
        scheduling.setProfessionalSpecialties(professional,List.of(specialty),specialty);
        scheduling.setProfessionalLocations(professional,List.of(location));
        LocalDate date = LocalDate.now().plusDays(1);
        scheduling.createBlock(owner,location,date,LocalTime.of(8,0),LocalTime.of(9,0));
        Long patient = user("upcoming-patient-"+suffix+"@example.test","U"+suffix,"3009999999");
        SchedulingService.Appointment appointment = scheduling.reserve(patient,professional,location,specialty,LocalDateTime.of(date,LocalTime.of(8,0)),"Upcoming");
        assertThat(appointment.status()).isEqualTo("APPROVED");

        Integer appointmentsBefore = jdbc.queryForObject("select count(*) from appointments",Integer.class);
        Integer historyBefore = jdbc.queryForObject("select count(*) from appointment_status_history",Integer.class);

        String admin = jwt.access(1L, Set.of("ADMIN"));
        String user = jwt.access(patient, Set.of("USER"));
        String professionalToken = jwt.access(owner, Set.of("PROFESSIONAL"));

        mvc.perform(get("/api/v1/admin/appointments/upcoming").header("Authorization","Bearer "+user)).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/admin/appointments/upcoming").header("Authorization","Bearer "+professionalToken)).andExpect(status().isForbidden());

        String body = mvc.perform(get("/api/v1/admin/appointments/upcoming").param("locationId",location.toString())
                        .header("Authorization","Bearer "+admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=="+appointment.id()+")].patientPhone").value("3009999999"))
                .andReturn().getResponse().getContentAsString();
        assertThat(body).doesNotContain("upcoming-patient-"+suffix+"@example.test");

        mvc.perform(get("/api/v1/admin/appointments/upcoming").param("from",date.plusDays(1).toString())
                        .header("Authorization","Bearer "+admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=="+appointment.id()+")]").isEmpty());

        assertThat(jdbc.queryForObject("select count(*) from appointments",Integer.class)).isEqualTo(appointmentsBefore);
        assertThat(jdbc.queryForObject("select count(*) from appointment_status_history",Integer.class)).isEqualTo(historyBefore);
    }

    private Long user(String email,String document,String phone) { jdbc.update("insert into users(first_name,last_name,document_type,document_number,email,phone,password_hash,active,email_verified) values ('Test','User','CC',?,?,?,'hash',true,false)",document,email,phone); return jdbc.queryForObject("select id from users where email=?",Long.class,email); }
}
