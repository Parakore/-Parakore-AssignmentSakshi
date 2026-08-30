package com.parakore.application.repository;

import com.parakore.application.entity.ApplicationSequence;
import com.parakore.application.entity.ApplicationSequenceId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;

import java.util.Optional;

public interface ApplicationSequenceRepository
        extends JpaRepository<ApplicationSequence, ApplicationSequenceId> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT s
            FROM ApplicationSequence s
            WHERE s.tenantId = :tenantId
              AND s.financialYear = :financialYear
              AND s.moduleCode = :moduleCode
            """)
    Optional<ApplicationSequence> findForUpdate(
            String tenantId,
            String financialYear,
            String moduleCode
    );
}