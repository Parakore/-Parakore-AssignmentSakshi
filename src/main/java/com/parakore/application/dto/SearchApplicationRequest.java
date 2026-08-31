package com.parakore.application.dto;

import com.parakore.application.entity.ApplicationStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record SearchApplicationRequest(

        @NotNull
        @Valid
        com.parakore.fee.dto.CalculationRequest.RequestInfo RequestInfo,

        String applicationNumber,

        ApplicationStatus status,

        String mobileNumber,

        String applicantUuid,

        @Min(0)
        Integer offset,

        @Min(1)
        @Max(100)
        Integer limit
) {
}