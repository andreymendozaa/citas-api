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
    public record Professional(Long id, String name, String professionalCode, String licenseNumber, boolean active, List<Long> specialtyIds, List<Long> locationIds, Long primarySpecialtyId) {}
    public record Block(Long id, Long locationId, LocalDate date, LocalTime start, LocalTime end) {}
    public record Available(Long professionalId, String professionalName, LocalDateTime startAt, LocalDateTime endAt) {}
    public record Appointment(Long id, String status, LocalDateTime startAt, LocalDateTime endAt) {}
    public record MyAppointment(Long id, Long professionalId, Long specialtyId, Long locationId, String professionalName, String specialtyName, String locationName, LocalDateTime startAt, LocalDateTime endAt, int durationMinutes, String status, String rejectionReason, Long rescheduleRequestId, String rescheduleStatus, LocalDateTime rescheduleRequestedStartAt, String rescheduleDecisionReason) {}
    public record RescheduleRequest(Long id, String status) {}
    public record PendingReschedule(Long id, Long appointmentId, String patientName, String professionalName, String specialtyName, String locationName, LocalDateTime previousStartAt, LocalDateTime requestedStartAt, LocalDateTime requestedEndAt) {}
    public record PendingAppointment(Long id, String patientName, String professionalName, String specialtyName, String locationName, LocalDateTime startAt, LocalDateTime endAt, int durationMinutes) {}
    public record Eps(Long id, String code, String name, boolean active) {}
    public record EpsPlan(Long id, Long epsId, Long regimeId, String code, String name, boolean active) {}
    public record ProfessionalAppointment(Long id, String patientName, Long specialtyId, String specialtyName, Long locationId, String locationName, LocalDateTime startAt, LocalDateTime endAt, int durationMinutes, String reason) {}
    public record AppointmentHistoryEntry(Long id, String status, Long changedByUserId, String changeSource, String reason, LocalDateTime changedAt) {}
    public record UpcomingAppointment(Long id, String patientName, String patientPhone, String professionalName, String specialtyName, Long locationId, String locationName, LocalDateTime startAt, LocalDateTime endAt) {}
    public record InboxItem(String type, Long id, Long appointmentId, String patientName, String professionalName, String specialtyName, Long locationId, String locationName, LocalDateTime startAt, LocalDateTime endAt) {}

    private final Ports.Scheduling scheduling;

    public SchedulingService(Ports.Scheduling scheduling) {
        this.scheduling = scheduling;
    }

    public List<Map<String, Object>> catalog(String name) { return scheduling.catalog(name); }
    public List<Specialty> specialties(boolean activeOnly) { return scheduling.specialties(activeOnly); }
    public List<Professional> professionals() { return scheduling.professionals(); }

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
    public List<MyAppointment> appointments(Long userId, String status, LocalDate date) { return scheduling.appointments(userId, status, date); }
    @Transactional public Appointment cancel(Long userId, Long appointmentId) { return scheduling.cancel(userId, appointmentId); }
    @Transactional public RescheduleRequest requestReschedule(Long userId, Long appointmentId, Long locationId, LocalDateTime start) { return scheduling.requestReschedule(userId, appointmentId, locationId, start); }
    public List<PendingReschedule> pendingReschedules() { return scheduling.pendingReschedules(); }
    @Transactional public RescheduleRequest decideReschedule(Long adminId, Long requestId, String decision, String reason) { return scheduling.decideReschedule(adminId, requestId, decision, reason); }
    public List<PendingAppointment> pending() { return scheduling.pending(); }
    @Transactional public Appointment decide(Long adminId, Long appointmentId, String decision, String reason) { return scheduling.decide(adminId, appointmentId, decision, reason); }

    public List<Eps> epsList(boolean activeOnly) { return scheduling.epsList(activeOnly); }
    @Transactional public Eps createEps(String code, String name) { return scheduling.createEps(code, name); }
    @Transactional public Eps updateEps(Long id, String name, Boolean active) { return scheduling.updateEps(id, name, active); }
    public List<EpsPlan> epsPlans(Long epsId) { return scheduling.epsPlans(epsId); }
    @Transactional public EpsPlan createEpsPlan(Long epsId, Long regimeId, String code, String name) { return scheduling.createEpsPlan(epsId, regimeId, code, name); }
    @Transactional public EpsPlan updateEpsPlan(Long id, String name, Boolean active) { return scheduling.updateEpsPlan(id, name, active); }

    public List<ProfessionalAppointment> professionalAppointments(Long userId, LocalDate from, LocalDate to, Long locationId) { return scheduling.professionalAppointments(userId, from, to, locationId); }
    @Transactional public Appointment closeAppointment(Long userId, Long appointmentId, String result, String reason) { return scheduling.closeAppointment(userId, appointmentId, result, reason); }
    public List<AppointmentHistoryEntry> history(Long callerId, boolean admin, Long appointmentId) { return scheduling.history(callerId, admin, appointmentId); }
    public List<UpcomingAppointment> upcoming(LocalDate from, LocalDate to, Long locationId) { return scheduling.upcoming(from, to, locationId); }
    public List<InboxItem> inbox(Long locationId, Long professionalId, Long specialtyId, LocalDate date) { return scheduling.inbox(locationId, professionalId, specialtyId, date); }
}
