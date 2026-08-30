package com.parakore.fee.dto;

import java.math.BigDecimal;

public record CalculationResponse(
        ResponseInfo ResponseInfo,
        Calculation Calculation
) {

    public record ResponseInfo(
            String msgId,
            String status
    ) {
    }

    public record Calculation(
            BigDecimal areaInSqm,
            BigDecimal restorationCharge,
            BigDecimal permissionFee,
            BigDecimal urgencySurcharge,
            BigDecimal securityDeposit,
            BigDecimal totalAmount,
            String reviewRef
    ) {
    }
}