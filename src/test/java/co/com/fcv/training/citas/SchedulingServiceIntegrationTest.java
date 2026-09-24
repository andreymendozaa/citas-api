package co.com.fcv.training.citas;

import co.com.fcv.training.citas.application.SchedulingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import java.time.*;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
class SchedulingServiceIntegrationTest extends DatabaseIntegrationSupport {
    @DynamicPropertySource static void properties(DynamicPropertyRegistry r) {
        r.add("app.jwt.access-secret", () -> "a".repeat(40)); r.add("app.jwt.refresh-secret", () -> "b".repeat(40)); r.add("app.cookie.secure", () -> true); r.add("app.cookie.same-site", () -> "None");
    }
    @Autowired SchedulingService scheduling; @Autowired JdbcTemplate jdbc;
    @Test void retainsConsecutiveSlotsAndReleasesThemAfterAdministrativeRejection() {
        String suffix=UUID.randomUUID().toString();
        Long professionalUser=user("prof-"+suffix+"@example.test",suffix); Long professional=scheduling.createProfessional("Pro","Fes","CC","P"+suffix,"pro2-"+suffix+"@example.test","300", "hash", "PC"+suffix,"LIC"+suffix);
        // createProfessional creates a second user; use that owner for the professional block
        Long owner=jdbc.queryForObject("select user_id from professionals where id=?",Long.class,professional);
        Long specialty=scheduling.createSpecialty("ESP"+suffix,"Especialidad "+suffix,60,false).id(); Long location=jdbc.queryForObject("select id from locations where active=true limit 1",Long.class);
        scheduling.setProfessionalSpecialties(professional,List.of(specialty),specialty); scheduling.setProfessionalLocations(professional,List.of(location));
        LocalDate date=LocalDate.now().plusDays(2); scheduling.createBlock(owner,location,date,LocalTime.of(8,0),LocalTime.of(10,0));
        assertThat(scheduling.availability(location,specialty,professional,date)).anyMatch(a -> a.startAt().equals(LocalDateTime.of(date,LocalTime.of(8,0))) && a.endAt().equals(LocalDateTime.of(date,LocalTime.of(9,0))));
        Long patient=user("patient-"+suffix+"@example.test","U"+suffix); Long admin=user("admin-"+suffix+"@example.test","A"+suffix);
        SchedulingService.Appointment appointment=scheduling.reserve(patient,professional,location,specialty,LocalDateTime.of(date,LocalTime.of(8,0)),"Prueba");
        assertThat(appointment.status()).isEqualTo("REQUESTED");
        assertThat(scheduling.pending()).singleElement().satisfies(pending -> {
            assertThat(pending.id()).isEqualTo(appointment.id());
            assertThat(pending.patientName()).isEqualTo("Test User");
            assertThat(pending.professionalName()).isEqualTo("Pro Fes");
            assertThat(pending.specialtyName()).isEqualTo("Especialidad "+suffix);
            assertThat(pending.durationMinutes()).isEqualTo(60);
        });
        assertThatThrownBy(() -> scheduling.reserve(patient,professional,location,specialty,LocalDateTime.of(date,LocalTime.of(8,0)),"Duplicada")).hasMessageContaining("Franja");
        assertThat(scheduling.decide(admin,appointment.id(),"REJECT","Sin disponibilidad clínica").status()).isEqualTo("REJECTED");
        assertThat(jdbc.queryForObject("select count(*) from appointment_status_history where appointment_id=?",Integer.class,appointment.id())).isEqualTo(2);
        assertThat(jdbc.queryForObject("select count(*) from appointment_status_history h join appointment_statuses s on s.id=h.status_id where h.appointment_id=? and s.code='REJECTED' and h.change_source='ADMIN'",Integer.class,appointment.id())).isEqualTo(1);
        assertThat(scheduling.availability(location,specialty,professional,date)).anyMatch(a -> a.startAt().equals(LocalDateTime.of(date,LocalTime.of(8,0))));
    }
    @Test void concurrentReservationsProduceExactlyOneAppointmentAndOneConflict() throws Exception {
        String suffix=UUID.randomUUID().toString();
        Long professional=scheduling.createProfessional("Con","Current","CC","P"+suffix,"con-"+suffix+"@example.test","300", "hash", "PC"+suffix,"LIC"+suffix);
        Long owner=jdbc.queryForObject("select user_id from professionals where id=?",Long.class,professional);
        Long specialty=scheduling.createSpecialty("GEN"+suffix,"General "+suffix,30,true).id(); Long location=jdbc.queryForObject("select id from locations where active=true limit 1",Long.class);
        scheduling.setProfessionalSpecialties(professional,List.of(specialty),specialty); scheduling.setProfessionalLocations(professional,List.of(location));
        LocalDate date=LocalDate.now().plusDays(3); scheduling.createBlock(owner,location,date,LocalTime.of(8,0),LocalTime.of(9,0));
        Long first=user("first-"+suffix+"@example.test","F"+suffix); Long second=user("second-"+suffix+"@example.test","S"+suffix);
        ExecutorService pool=Executors.newFixedThreadPool(2); CountDownLatch start=new CountDownLatch(1);
        try {
            Future<String> one=pool.submit(() -> reserveStatus(start,first,professional,location,specialty,date));
            Future<String> two=pool.submit(() -> reserveStatus(start,second,professional,location,specialty,date));
            start.countDown();
            assertThat(List.of(one.get(),two.get())).containsExactlyInAnyOrder("APPROVED","CONFLICT");
            assertThat(jdbc.queryForObject("select count(*) from appointments a join appointment_statuses s on s.id=a.status_id where a.professional_id=? and a.scheduled_start_at=? and s.code='APPROVED'",Integer.class,professional,LocalDateTime.of(date,LocalTime.of(8,0)))).isEqualTo(1);
            assertThat(jdbc.queryForObject("select count(*) from appointment_status_history h join appointment_statuses s on s.id=h.status_id where s.code='APPROVED' and h.change_source='SYSTEM' and h.appointment_id in (select id from appointments where professional_id=? and scheduled_start_at=?)",Integer.class,professional,LocalDateTime.of(date,LocalTime.of(8,0)))).isEqualTo(1);
        } finally { pool.shutdownNow(); }
    }
    @Test void rejectsPastOverlappingAndUnavailableSchedulingConfiguration() {
        String suffix=UUID.randomUUID().toString();
        Long professional=scheduling.createProfessional("Rules","Case","CC","P"+suffix,"rules-"+suffix+"@example.test","300", "hash", "PC"+suffix,"LIC"+suffix);
        Long owner=jdbc.queryForObject("select user_id from professionals where id=?",Long.class,professional);
        Long location=jdbc.queryForObject("select id from locations where active=true limit 1",Long.class);
        LocalDate future=LocalDate.now(ZoneId.of("America/Bogota")).plusDays(2);
        assertThatThrownBy(() -> scheduling.createBlock(owner,location,future,LocalTime.of(8,0),LocalTime.of(9,0))).hasMessageContaining("habilitado");
        scheduling.setProfessionalLocations(professional,List.of(location));
        assertThatThrownBy(() -> scheduling.createBlock(owner,location,future.minusDays(2),LocalTime.of(8,0),LocalTime.of(9,0))).hasMessageContaining("futuro");
        scheduling.createBlock(owner,location,future,LocalTime.of(8,0),LocalTime.of(10,0));
        assertThatThrownBy(() -> scheduling.createBlock(owner,location,future,LocalTime.of(9,0),LocalTime.of(11,0))).hasMessageContaining("solapado");
        Long specialty=scheduling.createSpecialty("OFF"+suffix,"Inactiva "+suffix,30,false).id();
        scheduling.setProfessionalSpecialties(professional,List.of(specialty),specialty);
        scheduling.updateSpecialty(specialty,null,null,false);
        assertThatThrownBy(() -> scheduling.availability(location,specialty,professional,future)).hasMessageContaining("Especialidad activa");
        scheduling.setProfessionalActive(professional,false);
        assertThatThrownBy(() -> scheduling.createBlock(owner,location,future.plusDays(1),LocalTime.of(8,0),LocalTime.of(9,0))).hasMessageContaining("Profesional");
    }
    @Test void administrativeApprovalRetainsSlotsAndWritesHistory() {
        String suffix=UUID.randomUUID().toString();
        Long professional=scheduling.createProfessional("Approve","Case","CC","P"+suffix,"approve-"+suffix+"@example.test","300","hash","PC"+suffix,"LIC"+suffix);
        Long owner=jdbc.queryForObject("select user_id from professionals where id=?",Long.class,professional);
        Long specialty=scheduling.createSpecialty("APR"+suffix,"Aprobación "+suffix,30,false).id(); Long location=jdbc.queryForObject("select id from locations where active=true limit 1",Long.class);
        scheduling.setProfessionalSpecialties(professional,List.of(specialty),specialty); scheduling.setProfessionalLocations(professional,List.of(location));
        LocalDate date=LocalDate.now().plusDays(4); scheduling.createBlock(owner,location,date,LocalTime.of(8,0),LocalTime.of(9,0));
        Long patient=user("approve-patient-"+suffix+"@example.test","U"+suffix); Long admin=user("approve-admin-"+suffix+"@example.test","A"+suffix);
        SchedulingService.Appointment appointment=scheduling.reserve(patient,professional,location,specialty,LocalDateTime.of(date,LocalTime.of(8,0)),"Prueba de aprobación");
        assertThat(scheduling.decide(admin,appointment.id(),"APPROVE",null).status()).isEqualTo("APPROVED");
        assertThat(scheduling.availability(location,specialty,professional,date)).noneMatch(slot -> slot.startAt().equals(LocalDateTime.of(date,LocalTime.of(8,0))));
        assertThat(jdbc.queryForObject("select count(*) from professional_slots where appointment_id=?",Integer.class,appointment.id())).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from appointment_status_history h join appointment_statuses s on s.id=h.status_id where h.appointment_id=? and s.code='APPROVED' and h.change_source='ADMIN'",Integer.class,appointment.id())).isEqualTo(1);
    }
    @Test void updatesAndDeletesFutureUncommittedBlocks() {
        String suffix=UUID.randomUUID().toString();
        Long professional=scheduling.createProfessional("Blocks","Case","CC","P"+suffix,"blocks-"+suffix+"@example.test","300","hash","PC"+suffix,"LIC"+suffix);
        Long owner=jdbc.queryForObject("select user_id from professionals where id=?",Long.class,professional); Long location=jdbc.queryForObject("select id from locations where active=true limit 1",Long.class);
        scheduling.setProfessionalLocations(professional,List.of(location)); LocalDate date=LocalDate.now().plusDays(5);
        SchedulingService.Block block=scheduling.createBlock(owner,location,date,LocalTime.of(8,0),LocalTime.of(9,0));
        assertThat(scheduling.updateBlock(owner,block.id(),null,null,LocalTime.of(8,0),LocalTime.of(10,0)).end()).isEqualTo(LocalTime.of(10,0));
        scheduling.deleteBlock(owner,block.id());
        assertThat(scheduling.blocks(owner,null,null)).noneMatch(candidate -> candidate.id().equals(block.id()));
        assertThat(jdbc.queryForObject("select count(*) from professional_slots where availability_block_id=?",Integer.class,block.id())).isZero();
    }
    private String reserveStatus(CountDownLatch start,Long patient,Long professional,Long location,Long specialty,LocalDate date) throws Exception { start.await(); try { return scheduling.reserve(patient,professional,location,specialty,LocalDateTime.of(date,LocalTime.of(8,0)),"Concurrente").status(); } catch (RuntimeException error) { return "CONFLICT"; } }
    private Long user(String email,String document) { jdbc.update("insert into users(first_name,last_name,document_type,document_number,email,phone,password_hash,active,email_verified) values ('Test','User','CC',?,?,?,'hash',true,false)",document,email,"300"); return jdbc.queryForObject("select id from users where email=?",Long.class,email); }
}
