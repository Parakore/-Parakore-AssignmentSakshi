package com.parakore.application.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "applications",
        indexes = {
                @Index(name = "idx_applications_tenant", columnList = "tenant_id"),
                @Index(name = "idx_applications_tenant_status", columnList = "tenant_id, status"),
                @Index(name = "idx_applications_tenant_mobile", columnList = "tenant_id, applicant_mobile"),
                @Index(name = "idx_applications_tenant_applicant", columnList = "tenant_id, applicant_uuid")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_application_number",
                        columnNames = "application_number"
                )
        }
)
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "application_number", nullable = false, length = 30)
    private String applicationNumber;

    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    @Column(name = "applicant_uuid", nullable = false, length = 100)
    private String applicantUuid;

    @Column(name = "applicant_mobile", nullable = false, length = 20)
    private String applicantMobile;

    @Column(name = "road_type", nullable = false, length = 20)
    private String roadType;

    @Column(name = "length_in_meters", nullable = false, precision = 12, scale = 3)
    private BigDecimal lengthInMeters;

    @Column(name = "width_in_meters", nullable = false, precision = 12, scale = 3)
    private BigDecimal widthInMeters;

    @Column(name = "duration_in_days", nullable = false)
    private Integer durationInDays;

    @Column(name = "applicant_type", nullable = false, length = 30)
    private String applicantType;

    @Column(name = "proposed_start_date", nullable = false)
    private LocalDate proposedStartDate;

    @Column(name = "area_in_sqm", nullable = false, precision = 12, scale = 2)
    private BigDecimal areaInSqm;

    @Column(name = "restoration_charge", nullable = false, precision = 15, scale = 2)
    private BigDecimal restorationCharge;

    @Column(name = "permission_fee", nullable = false, precision = 15, scale = 2)
    private BigDecimal permissionFee;

    @Column(name = "urgency_surcharge", nullable = false, precision = 15, scale = 2)
    private BigDecimal urgencySurcharge;

    @Column(name = "security_deposit", nullable = false, precision = 15, scale = 2)
    private BigDecimal securityDeposit;

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ApplicationStatus status;

    @Column(name = "created_by", nullable = false, length = 100)
    private String createdBy;

    @Column(name = "created_time", nullable = false)
    private LocalDateTime createdTime;

    @Column(name = "last_modified_by", nullable = false, length = 100)
    private String lastModifiedBy;

    @Column(name = "last_modified_time", nullable = false)
    private LocalDateTime lastModifiedTime;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

}