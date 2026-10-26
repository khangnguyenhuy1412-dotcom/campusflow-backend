package com.campusflow.verification.repository;

import com.campusflow.verification.entity.VerificationCode;
import com.campusflow.verification.entity.VerificationPurpose;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VerificationCodeRepository extends JpaRepository<VerificationCode, Long> {

    Optional<VerificationCode> findFirstByUserIdAndPurposeAndUsedAtIsNullOrderByIdDesc(
            Long userId, VerificationPurpose purpose);

    Optional<VerificationCode> findFirstByUserIdAndPurposeOrderByIdDesc(Long userId, VerificationPurpose purpose);

    /** Deletes codes that were never used, so only the newest code is valid. */
    @Modifying
    @Query("delete from VerificationCode c where c.user.id = :userId and c.purpose = :purpose and c.usedAt is null")
    void deleteUnused(@Param("userId") Long userId, @Param("purpose") VerificationPurpose purpose);
}
