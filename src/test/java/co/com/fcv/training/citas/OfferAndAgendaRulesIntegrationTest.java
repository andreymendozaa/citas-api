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

/** Remaining CA of HU-014 (duration), HU-015 (professional creation), HU-016 (specialty assignment), HU-018/019/020 (availability blocks). */
@SpringBootTest
@AutoConfigureMockMvc
class OfferAndAgendaRulesIntegrationTest extends DatabaseIntegrationSupport {
    @DynamicPropertySource static void properties(DynamicPropertyRegistry r) {
        r.add("app.jwt.access-secret", () -> "a".repeat(40)); r.add("app.jwt.refresh-secret", () -> "b".repeat(40)); r.add("app.cookie.secure", () -> true); r.add("app.cookie.same-site", () -> "None");
    }
    @Autowired MockMvc mvc;
    @Autowired SchedulingService scheduling;
    @Autowired JdbcTemplate jdbc;
    @Autowired JwtTokens jwt;

    String suffix;
    Long professional, owner, thirty, sixty, location, otherLocation, patient, admin;
    LocalDate date;
    String adminToken, userToken, professionalToken;

    @BeforeEach void fixture() {
        suffix = UUID.randomUUID().toString();
        professional = scheduling.createProfessional("Reglas","Oferta","CC","P"+suffix,"offer-"+suffix+"@example.test","300","hash","PC"+suffix,"LIC"+suffix);
        owner = jdbc.queryForObject("select user_id from professionals where id=?",Long.class,professional);
        thirty = scheduling.createSpecialty("R30"+suffix,"Reglas 30 "+suffix,30,true).id();
        sixty = scheduling.createSpecialty("R60"+suffix,"Reglas 60 "+suffix,60,true).id();
        location = jdbc.queryForObject("select id from locations where active=true order by id limit 1",Long.class);
        otherLocation = jdbc.queryForObject("select id from locations where active=true and id<>? order by id limit 1",Long.class,location);
        scheduling.setProfessionalSpecialties(professional,List.of(thirty,sixty),thirty);
        scheduling.setProfessionalLocations(professional,List.of(location,otherLocation));
        date = LocalDate.now(ZoneId.of("America/Bogota")).plusDays(5);
        patient = user("offer-patient-"+suffix+"@example.test","U"+suffix);
        admin = user("offer-admin-"+suffix+"@example.test","A"+suffix);
        adminToken = jwt.access(admin,Set.of("ADMIN"));
        userToken = jwt.access(patient,Set.of("USER"));
        professionalToken = jwt.access(owner,Set.of("PROFESSIONAL"));
    }

    /** Persistent test schema: never leave REQUESTED rows behind, even when a test fails. */
    @AfterEach void resolveLeftovers() {
        jdbc.queryForList("select a.id from appointments a join appointment_statuses s on s.id=a.status_id where a.professional_id=? and s.code='REQUESTED'",Long.class,professional)
                .forEach(id -> scheduling.decide(admin,id,"REJECT","Limpieza de prueba"));
    }

    // ---------- HU-014 CA-01 · duración restringida a 30/60 ----------

    @Test void specialtyDurationOnlyAccepts30Or60Minutes() throws Exception {
        for (int minutes : new int[]{0, 15, 45, 90}) {
            mvc.perform(post("/api/v1/admin/specialties").header("Authorization","Bearer "+adminToken).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"code\":\"BAD%d%s\",\"name\":\"Inválida %d\",\"durationMinutes\":%d,\"general\":false}".formatted(minutes,suffix.substring(0,8),minutes,minutes)))
                    .andExpect(status().isBadRequest());
            assertThat(jdbc.queryForObject("select count(*) from specialties where code=?",Integer.class,"BAD"+minutes+suffix.substring(0,8))).isZero();
        }
        mvc.perform(patch("/api/v1/admin/specialties/{id}",thirty).header("Authorization","Bearer "+adminToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"durationMinutes\":45}")).andExpect(status().isBadRequest());
        assertThat(jdbc.queryForObject("select appointment_duration_minutes from specialties where id=?",Integer.class,thirty)).isEqualTo(30);

        mvc.perform(patch("/api/v1/admin/specialties/{id}",thirty).header("Authorization","Bearer "+adminToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"durationMinutes\":60}")).andExpect(status().isOk()).andExpect(jsonPath("$.durationMinutes").value(60));
    }

    // ---------- HU-015 CA-02/CA-03 · alta solo ADMIN e integridad de identidad ----------

    @Test void onlyAdminCreatesProfessionalsAndDuplicatesLeaveNoPartialRows() throws Exception {
        String email = "new-pro-"+suffix+"@example.test";
        mvc.perform(createProfessional(userToken,email,"D1"+suffix.substring(0,12),"NC"+suffix)).andExpect(status().isForbidden());
        mvc.perform(createProfessional(professionalToken,email,"D1"+suffix.substring(0,12),"NC"+suffix)).andExpect(status().isForbidden());
        assertThat(usersWithEmail(email)).isZero();

        mvc.perform(createProfessional(adminToken,email,"D1"+suffix.substring(0,12),"NC"+suffix)).andExpect(status().isCreated());
        assertThat(jdbc.queryForObject("select count(*) from users u join user_roles ur on ur.user_id=u.id join roles r on r.id=ur.role_id join professionals p on p.user_id=u.id where u.email=? and r.code='PROFESSIONAL'",Integer.class,email)).isEqualTo(1);

        // duplicate email: rejected before anything is written
        mvc.perform(createProfessional(adminToken,email,"D2"+suffix.substring(0,12),"NC2"+suffix)).andExpect(status().isConflict());
        assertThat(jdbc.queryForObject("select count(*) from users where document_number=?",Integer.class,"D2"+suffix.substring(0,12))).isZero();

        // duplicate professional code: the user row inserted first must be rolled back with the failed professional row
        String otherEmail = "dup-code-"+suffix+"@example.test";
        mvc.perform(createProfessional(adminToken,otherEmail,"D3"+suffix.substring(0,12),"NC"+suffix)).andExpect(status().isConflict());
        assertThat(usersWithEmail(otherEmail)).isZero();
    }

    // ---------- HU-016 CA-02/CA-03 · especialidad primaria y especialidad no asociada ----------

    @Test void primarySpecialtyIsSingleAndConsistent() throws Exception {
        mvc.perform(assignSpecialties(List.of(thirty,sixty),sixty)).andExpect(status().isNoContent());
        assertThat(primaryOf(professional)).containsExactly(sixty);

        mvc.perform(assignSpecialties(List.of(thirty,sixty),thirty)).andExpect(status().isNoContent());
        assertThat(primaryOf(professional)).containsExactly(thirty);
        // The admin listing exposes the primary so the client can edit assignments without losing it.
        mvc.perform(get("/api/v1/admin/professionals").header("Authorization","Bearer "+adminToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$[?(@.id=="+professional+")].primarySpecialtyId").value(org.hamcrest.Matchers.contains(thirty.intValue())));

        Long unrelated = scheduling.createSpecialty("RX"+suffix,"Ajena "+suffix,30,true).id();
        mvc.perform(assignSpecialties(List.of(thirty,sixty),unrelated)).andExpect(status().isBadRequest()); // primary must belong to the list
        assertThat(primaryOf(professional)).containsExactly(thirty);
        assertThat(jdbc.queryForObject("select count(*) from professional_specialties where professional_id=?",Integer.class,professional)).isEqualTo(2);
    }

    @Test void specialtyNotAssociatedWithTheProfessionalIsNeitherOfferedNorBookable() {
        scheduling.createBlock(owner,location,date,LocalTime.of(8,0),LocalTime.of(10,0));
        Long unrelated = scheduling.createSpecialty("RN"+suffix,"No asociada "+suffix,30,true).id();

        assertThat(scheduling.availability(location,unrelated,professional,date)).isEmpty();
        assertThatThrownBy(() -> scheduling.reserve(patient,professional,location,unrelated,LocalDateTime.of(date,LocalTime.of(8,0)),"No asociada"));
        assertThat(jdbc.queryForObject("select count(*) from appointments where professional_id=? and specialty_id=?",Integer.class,professional,unrelated)).isZero();
    }

    // ---------- HU-018 CA-03 · dos franjas el mismo día sin el intervalo intermedio ----------

    @Test void twoBlocksTheSameDayDoNotExposeTheGapBetweenThem() {
        scheduling.createBlock(owner,location,date,LocalTime.of(8,0),LocalTime.of(10,0));
        scheduling.createBlock(owner,location,date,LocalTime.of(14,0),LocalTime.of(16,0));

        assertThat(starts(thirty)).containsExactly(at(8,0),at(8,30),at(9,0),at(9,30),at(14,0),at(14,30),at(15,0),at(15,30));
        // 60-minute appointments need two consecutive slots: 09:30 + 14:00 are not consecutive
        assertThat(starts(sixty)).containsExactly(at(8,0),at(8,30),at(9,0),at(14,0),at(14,30),at(15,0));
    }

    // ---------- HU-019 CA-03 · bloques ajenos, comprometidos o pasados ----------

    @Test void foreignCommittedAndPastBlocksCannotBeEditedOrDeleted() throws Exception {
        Long committed = scheduling.createBlock(owner,location,date,LocalTime.of(8,0),LocalTime.of(10,0)).id();
        Long appointment = scheduling.reserve(patient,professional,location,thirty,at(8,0),"Compromiso").id();

        Long strangerPro = scheduling.createProfessional("Otro","Pro","CC","Q"+suffix,"other-pro-"+suffix+"@example.test","300","hash","QC"+suffix,"QL"+suffix);
        String strangerToken = jwt.access(jdbc.queryForObject("select user_id from professionals where id=?",Long.class,strangerPro),Set.of("PROFESSIONAL"));
        mvc.perform(editBlock(committed,strangerToken,"{\"endTime\":\"11:00\"}")).andExpect(status().isNotFound());
        mvc.perform(delete("/api/v1/professional/availability-blocks/{id}",committed).header("Authorization","Bearer "+strangerToken)).andExpect(status().isNotFound());

        mvc.perform(editBlock(committed,professionalToken,"{\"endTime\":\"11:00\"}")).andExpect(status().isConflict());
        mvc.perform(delete("/api/v1/professional/availability-blocks/{id}",committed).header("Authorization","Bearer "+professionalToken)).andExpect(status().isConflict());
        assertThat(jdbc.queryForObject("select end_time from availability_blocks where id=? and active=true",LocalTime.class,committed)).isEqualTo(LocalTime.of(10,0));
        assertThat(jdbc.queryForObject("select count(*) from professional_slots where appointment_id=?",Integer.class,appointment)).isEqualTo(1);

        // a block whose day already passed is history: it cannot be moved to the future nor deleted
        Long past = scheduling.createBlock(owner,location,date.plusDays(1),LocalTime.of(8,0),LocalTime.of(9,0)).id();
        LocalDate yesterday = LocalDate.now(ZoneId.of("America/Bogota")).minusDays(1);
        jdbc.update("update availability_blocks set available_date=? where id=?",yesterday,past);
        mvc.perform(editBlock(past,professionalToken,"{\"date\":\"%s\"}".formatted(date.plusDays(2)))).andExpect(status().isConflict());
        mvc.perform(delete("/api/v1/professional/availability-blocks/{id}",past).header("Authorization","Bearer "+professionalToken)).andExpect(status().isConflict());
        assertThat(jdbc.queryForObject("select available_date from availability_blocks where id=? and active=true",LocalDate.class,past)).isEqualTo(yesterday);
    }

    // ---------- HU-020 CA-02 · filtros del calendario ----------

    @Test void calendarFiltersByDateAndLocation() throws Exception {
        Long a = scheduling.createBlock(owner,location,date,LocalTime.of(8,0),LocalTime.of(9,0)).id();
        Long b = scheduling.createBlock(owner,otherLocation,date,LocalTime.of(14,0),LocalTime.of(15,0)).id();
        Long c = scheduling.createBlock(owner,location,date.plusDays(1),LocalTime.of(8,0),LocalTime.of(9,0)).id();

        mvc.perform(get("/api/v1/professional/availability-blocks").header("Authorization","Bearer "+professionalToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(3));
        mvc.perform(get("/api/v1/professional/availability-blocks").param("date",date.toString()).header("Authorization","Bearer "+professionalToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2)).andExpect(jsonPath("$[0].id").value(a)).andExpect(jsonPath("$[1].id").value(b));
        mvc.perform(get("/api/v1/professional/availability-blocks").param("locationId",location.toString()).header("Authorization","Bearer "+professionalToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2)).andExpect(jsonPath("$[0].id").value(a)).andExpect(jsonPath("$[1].id").value(c));
        mvc.perform(get("/api/v1/professional/availability-blocks").param("date",date.toString()).param("locationId",otherLocation.toString()).header("Authorization","Bearer "+professionalToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].id").value(b));
    }

    // ---------- helpers ----------

    private org.springframework.test.web.servlet.RequestBuilder createProfessional(String token,String email,String document,String code) {
        return post("/api/v1/admin/professionals").header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON).content("""
                {"firstName":"Nuevo","lastName":"Profesional","documentType":"CC","documentNumber":"%s","email":"%s","phone":"3000000000","temporaryPassword":"Temporal123*","professionalCode":"%s","licenseNumber":"LIC-%s"}
                """.formatted(document,email,code,document));
    }
    private org.springframework.test.web.servlet.RequestBuilder assignSpecialties(List<Long> ids,Long primary) {
        return put("/api/v1/admin/professionals/{id}/specialties",professional).header("Authorization","Bearer "+adminToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"specialtyIds\":%s,\"primarySpecialtyId\":%d}".formatted(ids,primary));
    }
    private org.springframework.test.web.servlet.RequestBuilder editBlock(Long id,String token,String body) {
        return patch("/api/v1/professional/availability-blocks/{id}",id).header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON).content(body);
    }
    private List<Long> primaryOf(Long pro) { return jdbc.queryForList("select specialty_id from professional_specialties where professional_id=? and is_primary=true",Long.class,pro); }
    private int usersWithEmail(String email) { return jdbc.queryForObject("select count(*) from users where email=?",Integer.class,email); }
    private List<LocalDateTime> starts(Long specialty) { return scheduling.availability(location,specialty,professional,date).stream().map(SchedulingService.Available::startAt).toList(); }
    private LocalDateTime at(int hour,int minute) { return LocalDateTime.of(date,LocalTime.of(hour,minute)); }
    private Long user(String email,String document) { jdbc.update("insert into users(first_name,last_name,document_type,document_number,email,phone,password_hash,active,email_verified) values ('Test','User','CC',?,?,?,'hash',true,false)",document,email,"300"); return jdbc.queryForObject("select id from users where email=?",Long.class,email); }
}
