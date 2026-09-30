package co.com.fcv.training.citas.adapter.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.Optional;

interface UsersJpa extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByDocumentTypeAndDocumentNumber(String type, String number);
}

interface RolesJpa extends JpaRepository<RoleEntity, Short> {
    Optional<RoleEntity> findByCode(String code);
}

interface SessionsJpa extends JpaRepository<SessionEntity, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from SessionEntity s where s.tokenHash = :hash")
    Optional<SessionEntity> lockByTokenHash(@Param("hash") String hash);

    @Modifying
    @Query("update SessionEntity s set s.revokedAt = :when where s.userId = :userId and s.revokedAt is null")
    void revokeAllByUserId(@Param("userId") Long userId, @Param("when") Instant when);
}

interface PasswordResetTokensJpa extends JpaRepository<PasswordResetTokenEntity, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from PasswordResetTokenEntity t where t.tokenHash = :hash")
    Optional<PasswordResetTokenEntity> lockByTokenHash(@Param("hash") String hash);
}
