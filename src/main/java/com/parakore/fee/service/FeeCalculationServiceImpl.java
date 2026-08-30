package com.parakore.fee.service;


import com.parakore.application.entity.*;
import com.parakore.config.FeeConfiguration;
import com.parakore.config.FeeRateProvider;
import com.parakore.fee.dto.CalculationRequest;
import com.parakore.fee.dto.CalculationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class FeeCalculationServiceImpl implements FeeCalculationService {

    private static final String GOVERNMENT_AGENCY = "GOVERNMENT_AGENCY";
    private static final String REVIEW_REF = "K7Q2";

    private final FeeRateProvider feeRateProvider;

    @Override
    public CalculationResponse calculate(CalculationRequest request) {

        CalculationRequest.Calculation calculation =
                request.Calculation();

        String tenantId = calculation.tenantId();
        String roadTypeCode = calculation.roadType();

        FeeConfiguration.RoadTypeRate roadType =
                feeRateProvider
                        .getRoadTypeRate(tenantId, roadTypeCode)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Road type " + roadTypeCode +
                                        " is not active or does not exist for tenant " +
                                        tenantId
                        ));

        /*
         * 1. Area
         *
         * area = ceil(length × width)
         *
         * Important:
         * We multiply first and then apply CEILING.
         */
        BigDecimal area = calculation.lengthInMeters()
                .multiply(calculation.widthInMeters())
                .setScale(0, RoundingMode.CEILING);

        /*
         * 2. Restoration charge
         */
        BigDecimal restorationCharge = area
                .multiply(roadType.getRestorationRatePerSqm());

        /*
         * 3. Permission fee
         */
        BigDecimal permissionFee;

        if (GOVERNMENT_AGENCY.equalsIgnoreCase(
                calculation.applicantType())) {

            permissionFee = BigDecimal.ZERO;

        } else {

            permissionFee = area
                    .multiply(roadType.getPermissionRatePerSqmPerDay())
                    .multiply(BigDecimal.valueOf(
                            calculation.durationInDays()
                    ));
        }

        /*
         * 4. Urgency surcharge
         *
         * surcharge applies when:
         *
         * proposedStartDate - applicationDate
         * < urgencyThresholdDays
         *
         * Exactly 3 days away => NO surcharge.
         */
        LocalDate applicationDate = calculation.applicationDate();

        long daysUntilStart =
                calculation.proposedStartDate()
                        .toEpochDay()
                        - applicationDate.toEpochDay();

        BigDecimal urgencySurcharge = BigDecimal.ZERO;

        int urgencyThreshold =
                feeRateProvider
                        .getDefaults()
                        .getUrgencyThresholdDays();

        if (daysUntilStart < urgencyThreshold) {

            BigDecimal surchargePercent =
                    feeRateProvider
                            .getDefaults()
                            .getUrgencySurchargePercent();

            urgencySurcharge = permissionFee
                    .multiply(surchargePercent)
                    .divide(
                            BigDecimal.valueOf(100),
                            2,
                            RoundingMode.HALF_UP
                    );
        }

        /*
         * 5. Security deposit
         *
         * max(
         *     minimum deposit,
         *     restorationCharge × deposit percentage
         * )
         */
        BigDecimal securityDepositPercentage =
                feeRateProvider
                        .getDefaults()
                        .getSecurityDepositPercent();

        BigDecimal percentageDeposit =
                restorationCharge
                        .multiply(securityDepositPercentage)
                        .divide(
                                BigDecimal.valueOf(100),
                                2,
                                RoundingMode.HALF_UP
                        );

        BigDecimal securityDeposit =
                percentageDeposit.max(
                        roadType.getMinSecurityDeposit()
                );

        /*
         * 6. Total
         *
         * total =
         * restoration
         * + permission
         * + surcharge
         * + security deposit
         *
         * Round HALF_UP.
         */
        BigDecimal totalAmount =
                restorationCharge
                        .add(permissionFee)
                        .add(urgencySurcharge)
                        .add(securityDeposit)
                        .setScale(0, RoundingMode.HALF_UP);

        return new CalculationResponse(

                new CalculationResponse.ResponseInfo(
                        request.RequestInfo().msgId(),
                        "successful"
                ),

                new CalculationResponse.Calculation(
                        area,
                        restorationCharge,
                        permissionFee,
                        urgencySurcharge,
                        securityDeposit,
                        totalAmount,
                        REVIEW_REF
                )
        );
    }


}