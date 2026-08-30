package com.parakore.application.dto;

import com.parakore.application.entity.ApplicationStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateApplicationResponse(

        ResponseInfo ResponseInfo,

        Application Application
) {

    public record ResponseInfo(
            String msgId,
            String status
    ) {
    }

    public record Application(
            Long id,
            String applicationNumber,
            String tenantId,
            String applicantUuid,
            String applicantMobile,
            String roadType,
            BigDecimal lengthInMeters,
            BigDecimal widthInMeters,
            Integer durationInDays,
            String applicantType,
            LocalDate proposedStartDate,
            BigDecimal areaInSqm,
            BigDecimal restorationCharge,
            BigDecimal permissionFee,
            BigDecimal urgencySurcharge,
            BigDecimal securityDeposit,
            BigDecimal totalAmount,
            ApplicationStatus status
    ) {
    }
}

