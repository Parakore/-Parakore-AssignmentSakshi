package com.parakore.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record CreateApplicationRequest(

        @NotNull
        @Valid
        com.parakore.fee.dto.CalculationRequest.RequestInfo RequestInfo,

        @NotNull
        @Valid
        com.parakore.fee.dto.CalculationRequest.Calculation Calculation
) {
}

