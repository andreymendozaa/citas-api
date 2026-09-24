package co.com.fcv.training.citas.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

/** Application use cases for scheduling. Persistence is supplied through Ports.Scheduling. */
@Service
public class SchedulingService {
    public record Specialty(Long id, String code, String name, int durationMinutes, boolean general, boolean active) {}
    public record Block(Long id, Long locationId, LocalDate date, LocalTime start, LocalTime end) {}
    public record Available(Long professionalId, String professionalName, LocalDateTime startAt, LocalDateTime endAt) {}
    public record Appointment(Long id, String status, LocalDateTime startAt, LocalDateTime endAt) {}
    public record PendingAppointment(Long id, String patientName, String professionalName, String specialtyName, String locationName, LocalDateTime startAt, LocalDateTime endAt, int durationMinutes) {}

    private final Ports.Scheduling scheduling;

    public SchedulingService(Ports.Scheduling scheduling) {
        this.scheduling = scheduling;
    }

    public List<Map<String, Object>> catalog(String name) { return scheduling.catalog(name); }
    public List<Specialty> specialties(boolean activeOnly) { return scheduling.specialties(activeOnly); }
    public List<Map<String, Object>> professionals() { return scheduling.professionals(); }

    @Transactional public Specialty createSpecialty(String code, String name, int duration, boolean general) { return scheduling.createSpecialty(code, name, duration, general); }
    @Transactional public Specialty updateSpecialty(Long id, String name, Integer duration, Boolean active) { return scheduling.updateSpecialty(id, name, duration, active); }
    @Transactional public Long createProfessional(String first, String last, String docType, String document, String email, String phone, String passwordHash, String code, String license) { return scheduling.createProfessional(first, last, docType, document, email, phone, passwordHash, code, license); }
    @Transactional public void setProfessionalSpecialties(Long professionalId, List<Long> ids, Long primary) { scheduling.setProfessionalSpecialties(professionalId, ids, primary); }
    @Transactional public void setProfessionalLocations(Long professionalId, List<Long> ids) { scheduling.setProfessionalLocations(professionalId, ids); }
    @Transactional public void setProfessionalActive(Long id, boolean active) { scheduling.setProfessionalActive(id, active); }
    @Transactional public Block createBlock(Long userId, Long locationId, LocalDate date, LocalTime start, LocalTime end) { return scheduling.createBlock(userId, locationId, date, start, end); }
    public List<Block> blocks(Long userId, LocalDate date, Long locationId) { return scheduling.blocks(userId, date, locationId); }
    @Transactional public Block updateBlock(Long userId, Long id, Long locationId, LocalDate date, LocalTime start, LocalTime end) { return scheduling.updateBlock(userId, id, locationId, date, start, end); }
    @Transactional public void deleteBlock(Long userId, Long id) { scheduling.deleteBlock(userId, id); }
    public List<Available> availability(Long locationId, Long specialtyId, Long professionalId, LocalDate date) { return scheduling.availability(locationId, specialtyId, professionalId, date); }
    @Transactional public Appointment reserve(Long userId, Long professionalId, Long locationId, Long specialtyId, LocalDateTime start, String reason) { return scheduling.reserve(userId, professionalId, locationId, specialtyId, start, reason); }
    public List<PendingAppointment> pending() { return scheduling.pending(); }
    @Transactional public Appointment decide(Long adminId, Long appointmentId, String decision, String reason) { return scheduling.decide(adminId, appointmentId, decision, reason); }
}
