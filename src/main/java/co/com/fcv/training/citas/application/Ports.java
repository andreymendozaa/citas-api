package co.com.fcv.training.citas.application;

import co.com.fcv.training.citas.domain.Account;
import co.com.fcv.training.citas.domain.PasswordResetToken;
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
        Account updatePhone(Long userId, String phone);
        void updatePasswordHash(Long userId, String passwordHash);
    }

    public interface Sessions {
        void save(RefreshSession session);
        Optional<RefreshSession> lockByJtiHash(String hash);
        void revoke(Long id, Instant when);
        void revokeAllByUserId(Long userId, Instant when);
    }

    public interface Passwords {
        String hash(String raw);
        boolean matches(String raw, String hash);
    }

    public interface Affiliations { void createCurrent(Long userId, Long planId); }

    /** Persistence for password recovery tokens (HU-008/HU-009). */
    public interface PasswordResets {
        void save(PasswordResetToken token);
        Optional<PasswordResetToken> lockByTokenHash(String hash);
        void markUsed(Long id, Instant when);
    }

    /** Optional notification of a freshly issued recovery token. Real delivery (email/SMTP) is out of scope;
     *  in development only the local mailbox (profile "local") implements this. */
    public interface PasswordResetNotifications {
        void publish(Long userId, String email, String rawToken, Instant expiresAt);
    }

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
        SchedulingService.Appointment cancel(Long userId, Long appointmentId);
        SchedulingService.RescheduleRequest requestReschedule(Long userId, Long appointmentId, Long locationId, LocalDateTime start);
        List<SchedulingService.PendingReschedule> pendingReschedules();
        SchedulingService.RescheduleRequest decideReschedule(Long adminId, Long requestId, String decision, String reason);
        List<SchedulingService.PendingAppointment> pending();
        SchedulingService.Appointment decide(Long adminId, Long appointmentId, String decision, String reason);
        List<SchedulingService.Eps> epsList(boolean activeOnly);
        SchedulingService.Eps createEps(String code, String name);
        SchedulingService.Eps updateEps(Long id, String name, Boolean active);
        List<SchedulingService.EpsPlan> epsPlans(Long epsId);
        SchedulingService.EpsPlan createEpsPlan(Long epsId, Long regimeId, String code, String name);
        SchedulingService.EpsPlan updateEpsPlan(Long id, String name, Boolean active);
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
