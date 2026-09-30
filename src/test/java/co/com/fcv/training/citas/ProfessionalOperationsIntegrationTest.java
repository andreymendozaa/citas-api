package co.com.fcv.training.citas;

import co.com.fcv.training.citas.application.SchedulingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

/** HU-029 (agenda profesional) and HU-030 (cierre de atención). */
@SpringBootTest
class ProfessionalOperationsIntegrationTest extends DatabaseIntegrationSupport {
    @DynamicPropertySource static void properties(DynamicPropertyRegistry r) {
        r.add("app.jwt.access-secret", () -> "a".repeat(40)); r.add("app.jwt.refresh-secret", () -> "b".repeat(40)); r.add("app.cookie.secure", () -> true); r.add("app.cookie.same-site", () -> "None");
    }
    @Autowired SchedulingService scheduling;
    @Autowired JdbcTemplate jdbc;

    @Test void agendaListsOnlyOwnApprovedAppointmentsWithFilters() {
        String suffix = UUID.randomUUID().toString();
        Long professionalA = scheduling.createProfessional("Agenda","A","CC","PA"+suffix,"agenda-a-"+suffix+"@example.test","300","hash","PCA"+suffix,"LICA"+suffix);
        Long ownerA = jdbc.queryForObject("select user_id from professionals where id=?",Long.class,professionalA);
        Long professionalB = scheduling.createProfessional("Agenda","B","CC","PB"+suffix,"agenda-b-"+suffix+"@example.test","300","hash","PCB"+suffix,"LICB"+suffix);
        Long ownerB = jdbc.queryForObject("select user_id from professionals where id=?",Long.class,professionalB);
        Long generalSpecialty = scheduling.createSpecialty("AGG"+suffix,"Agenda General "+suffix,30,true).id();
        Long specializedSpecialty = scheduling.createSpecialty("AGE"+suffix,"Agenda Especial "+suffix,30,false).id();
        List<Long> locationIds = jdbc.query("select id from locations where active=true order by id limit 2",(r,n)->r.getLong(1));
        Long location1 = locationIds.get(0); Long location2 = locationIds.get(1);
        scheduling.setProfessionalSpecialties(professionalA,List.of(generalSpecialty,specializedSpecialty),generalSpecialty);
        scheduling.setProfessionalLocations(professionalA,List.of(location1,location2));
        scheduling.setProfessionalSpecialties(professionalB,List.of(generalSpecialty),generalSpecialty);
        scheduling.setProfessionalLocations(professionalB,List.of(location1));

        LocalDate day1 = LocalDate.now().plusDays(1); LocalDate day2 = LocalDate.now().plusDays(2);
        scheduling.createBlock(ownerA,location1,day1,LocalTime.of(8,0),LocalTime.of(9,0));
        scheduling.createBlock(ownerA,location2,day2,LocalTime.of(8,0),LocalTime.of(9,0));
        scheduling.createBlock(ownerB,location1,day1,LocalTime.of(8,0),LocalTime.of(9,0));

        Long patient = user("agenda-patient-"+suffix+"@example.test","U"+suffix);
        SchedulingService.Appointment approvedDay1 = scheduling.reserve(patient,professionalA,location1,generalSpecialty,LocalDateTime.of(day1,LocalTime.of(8,0)),"General día 1");
        SchedulingService.Appointment approvedDay2 = scheduling.reserve(patient,professionalA,location2,generalSpecialty,LocalDateTime.of(day2,LocalTime.of(8,0)),"General día 2");
        SchedulingService.Appointment requested = scheduling.reserve(patient,professionalA,location1,specializedSpecialty,LocalDateTime.of(day1,LocalTime.of(8,30)),"Especializada pendiente");
        assertThat(requested.status()).isEqualTo("REQUESTED");
        scheduling.reserve(patient,professionalB,location1,generalSpecialty,LocalDateTime.of(day1,LocalTime.of(8,0)),"De otro profesional");

        assertThat(scheduling.professionalAppointments(ownerA,null,null,null))
                .extracting(SchedulingService.ProfessionalAppointment::id)
                .containsExactlyInAnyOrder(approvedDay1.id(),approvedDay2.id());
        assertThat(scheduling.professionalAppointments(ownerA,day1,day1,null))
                .extracting(SchedulingService.ProfessionalAppointment::id).containsExactly(approvedDay1.id());
        assertThat(scheduling.professionalAppointments(ownerA,null,null,location2))
                .extracting(SchedulingService.ProfessionalAppointment::id).containsExactly(approvedDay2.id());
        assertThat(scheduling.professionalAppointments(ownerB,null,null,null))
                .extracting(SchedulingService.ProfessionalAppointment::id)
                .doesNotContain(approvedDay1.id(),approvedDay2.id());

        // Resolve the specialized request so it does not linger in the global pending pool for other tests.
        Long admin = user("agenda-admin-"+suffix+"@example.test","A"+suffix);
        scheduling.decide(admin,requested.id(),"REJECT","Cleanup");
    }

    @Test void closesAppointmentsWithGuardsAndHistory() {
        String suffix = UUID.randomUUID().toString();
        Long professional = scheduling.createProfessional("Close","Case","CC","P"+suffix,"close-"+suffix+"@example.test","300","hash","PC"+suffix,"LIC"+suffix);
        Long owner = jdbc.queryForObject("select user_id from professionals where id=?",Long.class,professional);
        Long specialty = scheduling.createSpecialty("CLS"+suffix,"Cierre "+suffix,30,true).id();
        Long location = jdbc.queryForObject("select id from locations where active=true limit 1",Long.class);
        scheduling.setProfessionalSpecialties(professional,List.of(specialty),specialty);
        scheduling.setProfessionalLocations(professional,List.of(location));
        LocalDate date = LocalDate.now().plusDays(1);
        scheduling.createBlock(owner,location,date,LocalTime.of(8,0),LocalTime.of(9,0));
        Long patient = user("close-patient-"+suffix+"@example.test","U"+suffix);
        SchedulingService.Appointment appointment = scheduling.reserve(patient,professional,location,specialty,LocalDateTime.of(date,LocalTime.of(8,0)),"Cierre");
        assertThat(appointment.status()).isEqualTo("APPROVED");

        assertThatThrownBy(() -> scheduling.closeAppointment(owner,appointment.id(),"COMPLETED",null)).hasMessageContaining("finalizado");

        // Simulate an elapsed appointment: only the schedule moves to the past, business rules stay untouched.
        LocalDateTime bogotaNow = LocalDateTime.now(ZoneId.of("America/Bogota"));
        jdbc.update("update appointments set scheduled_start_at=?,scheduled_end_at=? where id=?",
                bogotaNow.minusHours(2),bogotaNow.minusHours(1),appointment.id());

        Long otherProfessional = scheduling.createProfessional("Other","Pro","CC","P2"+suffix,"other-"+suffix+"@example.test","300","hash","PC2"+suffix,"LIC2"+suffix);
        Long otherOwner = jdbc.queryForObject("select user_id from professionals where id=?",Long.class,otherProfessional);
        assertThatThrownBy(() -> scheduling.closeAppointment(otherOwner,appointment.id(),"COMPLETED",null)).hasMessageContaining("no encontrado");
        assertThatThrownBy(() -> scheduling.closeAppointment(owner,appointment.id(),"APPROVED",null)).hasMessageContaining("inválido");

        SchedulingService.Appointment closed = scheduling.closeAppointment(owner,appointment.id(),"COMPLETED",null);
        assertThat(closed.status()).isEqualTo("COMPLETED");
        assertThat(jdbc.queryForObject(
                "select count(*) from appointment_status_history h join appointment_statuses s on s.id=h.status_id where h.appointment_id=? and s.code='COMPLETED' and h.change_source='PROFESSIONAL' and h.changed_by_user_id=?",
                Integer.class,appointment.id(),owner)).isEqualTo(1);

        assertThatThrownBy(() -> scheduling.closeAppointment(owner,appointment.id(),"NO_SHOW",null)).hasMessageContaining("aprobada");
    }

    @Test void closesAppointmentAsNoShow() {
        String suffix = UUID.randomUUID().toString();
        Long professional = scheduling.createProfessional("NoShow","Case","CC","P"+suffix,"noshow-"+suffix+"@example.test","300","hash","PC"+suffix,"LIC"+suffix);
        Long owner = jdbc.queryForObject("select user_id from professionals where id=?",Long.class,professional);
        Long specialty = scheduling.createSpecialty("NSW"+suffix,"No Show "+suffix,30,true).id();
        Long location = jdbc.queryForObject("select id from locations where active=true limit 1",Long.class);
        scheduling.setProfessionalSpecialties(professional,List.of(specialty),specialty);
        scheduling.setProfessionalLocations(professional,List.of(location));
        LocalDate date = LocalDate.now().plusDays(1);
        scheduling.createBlock(owner,location,date,LocalTime.of(8,0),LocalTime.of(9,0));
        Long patient = user("noshow-patient-"+suffix+"@example.test","U"+suffix);
        SchedulingService.Appointment appointment = scheduling.reserve(patient,professional,location,specialty,LocalDateTime.of(date,LocalTime.of(8,0)),"No show");
        LocalDateTime bogotaNow = LocalDateTime.now(ZoneId.of("America/Bogota"));
        jdbc.update("update appointments set scheduled_start_at=?,scheduled_end_at=? where id=?",
                bogotaNow.minusHours(2),bogotaNow.minusHours(1),appointment.id());
        assertThat(scheduling.closeAppointment(owner,appointment.id(),"NO_SHOW",null).status()).isEqualTo("NO_SHOW");
    }

    private Long user(String email,String document) { jdbc.update("insert into users(first_name,last_name,document_type,document_number,email,phone,password_hash,active,email_verified) values ('Test','User','CC',?,?,?,'hash',true,false)",document,email,"300"); return jdbc.queryForObject("select id from users where email=?",Long.class,email); }
}
