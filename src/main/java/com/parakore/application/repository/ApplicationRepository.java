package com.parakore.application.repository;

import com.parakore.application.entity.Application;
import com.parakore.application.entity.ApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ApplicationRepository extends JpaRepository<Application, Long> {

    Optional<Application> findByTenantIdAndApplicationNumber(
            String tenantId,
            String applicationNumber
    );

    Page<Application> findByTenantId(
            String tenantId,
            Pageable pageable
    );

    Page<Application> findByTenantIdAndStatus(
            String tenantId,
            ApplicationStatus status,
            Pageable pageable
    );

    Page<Application> findByTenantIdAndApplicantMobile(
            String tenantId,
            String applicantMobile,
            Pageable pageable
    );

    Page<Application> findByTenantIdAndStatusAndApplicantMobile(
            String tenantId,
            ApplicationStatus status,
            String applicantMobile,
            Pageable pageable
    );

    Page<Application> findByTenantIdAndApplicantUuid(
            String tenantId,
            String applicantUuid,
            Pageable pageable
    );
}