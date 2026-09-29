package co.com.fcv.training.citas.application;

import co.com.fcv.training.citas.domain.Account;
import co.com.fcv.training.citas.domain.RefreshSession;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

public final class Ports {
    private Ports() {}

    public interface Accounts {
        boolean existsEmail(String email);
        boolean existsDocument(String type, String number);
        Optional<Account> byEmail(String email);
        Optional<Account> byId(Long id);
        Account save(Account account);
    }

    public interface Sessions {
        void save(RefreshSession session);
        Optional<RefreshSession> lockByJtiHash(String hash);
        void revoke(Long id, Instant when);
    }

    public interface Passwords {
        String hash(String raw);
        boolean matches(String raw, String hash);
    }

    public interface Affiliations { void createCurrent(Long userId, Long planId); }

    /** Persistence boundary for the scheduling use cases. */
    public interface Scheduling {
        List<Map<String,Object>> catalog(String name);
        List<SchedulingService.Specialty> specialties(boolean activeOnly);
        List<SchedulingService.Professional> professionals();
        SchedulingService.Specialty createSpecialty(String code, String name, int duration, boolean general);
        SchedulingService.Specialty updateSpecialty(Long id, String name, Integer duration, Boolean active);
        Long createProfessional(String first, String last, String docType, String document, String email, String phone, String passwordHash, String code, String license);
        void setProfessionalSpecialties(Long professionalId, List<Long> ids, Long primary);
        void setProfessionalLocations(Long professionalId, List<Long> ids);
        void setProfessionalActive(Long id, boolean active);
        SchedulingService.Block createBlock(Long userId, Long locationId, LocalDate date, LocalTime start, LocalTime end);
        List<SchedulingService.Block> blocks(Long userId, LocalDate date, Long locationId);
        SchedulingService.Block updateBlock(Long userId, Long id, Long locationId, LocalDate date, LocalTime start, LocalTime end);
        void deleteBlock(Long userId, Long id);
        List<SchedulingService.Available> availability(Long locationId, Long specialtyId, Long professionalId, LocalDate date);
        SchedulingService.Appointment reserve(Long userId, Long professionalId, Long locationId, Long specialtyId, LocalDateTime start, String reason);
        List<SchedulingService.MyAppointment> appointments(Long userId, String status, LocalDate date);
        List<SchedulingService.PendingAppointment> pending();
        SchedulingService.Appointment decide(Long adminId, Long appointmentId, String decision, String reason);
    }

    public record IssuedRefresh(String value, String jti, Instant expiresAt) {}
    public record RefreshIdentity(Long userId, String jti) {}

    public interface Tokens {
        String access(Long userId, Set<String> roles);
        IssuedRefresh refresh(Long userId);
        RefreshIdentity readRefresh(String token);
        long accessSeconds();
    }

    public interface Transactions {
        <T> T run(Supplier<T> work);
    }
}
