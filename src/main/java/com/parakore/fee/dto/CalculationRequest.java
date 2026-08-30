package com.parakore.fee.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CalculationRequest(
        @NotNull @Valid
        RequestInfo RequestInfo,

        @NotNull @Valid
        Calculation Calculation
) {

    public record RequestInfo(
            @NotBlank
            String apiId,

            @NotBlank
            String msgId,

            @NotNull @Valid
            UserInfo userInfo
    ) {
    }

    public record UserInfo(
            @NotBlank
            String uuid,

            @NotBlank
            String userName,

            @NotBlank
            String tenantId,

            @NotNull
            List<Role> roles
    ) {
    }

    public record Role(
            @NotBlank
            String code
    ) {
    }

    public record Calculation(
            @NotBlank
            String tenantId,

            @NotBlank
            String roadType,

            @NotNull
            @Positive
            BigDecimal lengthInMeters,

            @NotNull
            @Positive
            BigDecimal widthInMeters,

            @NotNull
            @Positive
            Integer durationInDays,

            @NotBlank
            String applicantType,

            @NotNull
            LocalDate proposedStartDate,


            LocalDate applicationDate
    ) {
    }
}