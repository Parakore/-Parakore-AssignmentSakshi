package com.parakore.fee.service;

import com.parakore.config.FeeConfiguration;
import com.parakore.config.FeeRateProvider;
import com.parakore.fee.dto.CalculationRequest;
import com.parakore.fee.dto.CalculationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class FeeCalculationServiceImplTest {

    private FeeCalculationServiceImpl service;

    private FeeRateProvider feeRateProvider;

    @BeforeEach
    void setUp() {

        feeRateProvider = new TestFeeRateProvider();

        service = new FeeCalculationServiceImpl(feeRateProvider);
    }

    @Test
    void shouldCalculateFeeForBituminousRoad() {

        CalculationRequest request =
                createRequest(
                        "BT",
                        new BigDecimal("10"),
                        new BigDecimal("5"),
                        2,
                        "PRIVATE",
                        LocalDate.of(2026, 9, 5),
                        LocalDate.of(2026, 8, 31)
                );

        CalculationResponse response =
                service.calculate(request);

        assertNotNull(response);

        assertEquals(
                BigDecimal.valueOf(50),
                response.Calculation().areaInSqm()
        );

        assertEquals(
                new BigDecimal("60000"),
                response.Calculation().restorationCharge()
        );

        assertEquals(
                new BigDecimal("1500"),
                response.Calculation().permissionFee()
        );

        assertEquals(
                BigDecimal.ZERO,
                response.Calculation().urgencySurcharge()
        );

        assertEquals(
                new BigDecimal("15000.00"),
                response.Calculation().securityDeposit()
        );

        assertEquals(
                new BigDecimal("76500"),
                response.Calculation().totalAmount()
        );
    }

    @Test
    void shouldCalculateUrgencySurchargeWhenStartDateIsLessThanThreshold() {

        CalculationRequest request =
                createRequest(
                        "BT",
                        new BigDecimal("10"),
                        new BigDecimal("5"),
                        2,
                        "PRIVATE",
                        LocalDate.of(2026, 9, 2),
                        LocalDate.of(2026, 8, 31)
                );

        CalculationResponse response =
                service.calculate(request);

        assertEquals(
                new BigDecimal("150.00"),
                response.Calculation().urgencySurcharge()
        );
    }

    @Test
    void shouldNotApplyUrgencySurchargeWhenStartDateIsExactlyThreshold() {

        CalculationRequest request =
                createRequest(
                        "BT",
                        new BigDecimal("10"),
                        new BigDecimal("5"),
                        2,
                        "PRIVATE",
                        LocalDate.of(2026, 9, 3),
                        LocalDate.of(2026, 8, 31)
                );

        CalculationResponse response =
                service.calculate(request);

        assertEquals(
                BigDecimal.ZERO,
                response.Calculation().urgencySurcharge()
        );
    }

    @Test
    void shouldNotChargePermissionFeeForGovernmentAgency() {

        CalculationRequest request =
                createRequest(
                        "BT",
                        new BigDecimal("10"),
                        new BigDecimal("5"),
                        2,
                        "GOVERNMENT_AGENCY",
                        LocalDate.of(2026, 9, 10),
                        LocalDate.of(2026, 8, 31)
                );

        CalculationResponse response =
                service.calculate(request);

        assertEquals(
                BigDecimal.ZERO,
                response.Calculation().permissionFee()
        );

        assertEquals(
                BigDecimal.ZERO,
                response.Calculation().urgencySurcharge()
        );
    }

    @Test
    void shouldApplyMinimumSecurityDeposit() {

        CalculationRequest request =
                createRequest(
                        "WBM",
                        new BigDecimal("1"),
                        new BigDecimal("1"),
                        1,
                        "PRIVATE",
                        LocalDate.of(2026, 9, 10),
                        LocalDate.of(2026, 8, 31)
                );

        CalculationResponse response =
                service.calculate(request);

        assertEquals(
                new BigDecimal("2000"),
                response.Calculation().securityDeposit()
        );
    }

    @Test
    void shouldCeilAreaWhenDecimalResult() {

        CalculationRequest request =
                createRequest(
                        "BT",
                        new BigDecimal("2.1"),
                        new BigDecimal("2.1"),
                        1,
                        "PRIVATE",
                        LocalDate.of(2026, 9, 10),
                        LocalDate.of(2026, 8, 31)
                );

        CalculationResponse response =
                service.calculate(request);

        assertEquals(
                BigDecimal.valueOf(5),
                response.Calculation().areaInSqm()
        );
    }

    @Test
    void shouldRejectInactiveRoadType() {

        CalculationRequest request =
                createRequest(
                        "KUTCHA",
                        new BigDecimal("10"),
                        new BigDecimal("5"),
                        1,
                        "PRIVATE",
                        LocalDate.of(2026, 9, 10),
                        LocalDate.of(2026, 8, 31)
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> service.calculate(request)
        );
    }

    @Test
    void shouldRejectUnknownRoadType() {

        CalculationRequest request =
                createRequest(
                        "UNKNOWN",
                        new BigDecimal("10"),
                        new BigDecimal("5"),
                        1,
                        "PRIVATE",
                        LocalDate.of(2026, 9, 10),
                        LocalDate.of(2026, 8, 31)
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> service.calculate(request)
        );
    }

    private CalculationRequest createRequest(
            String roadType,
            BigDecimal length,
            BigDecimal width,
            int duration,
            String applicantType,
            LocalDate proposedStartDate,
            LocalDate applicationDate) {

        CalculationRequest.RequestInfo requestInfo =
                new CalculationRequest.RequestInfo(
                        "test-api",
                        "test-msg",
                        new CalculationRequest.UserInfo(
                                "test-user",
                                "test-user",
                                "dehradun",
                                List.of()
                        )
                );

        CalculationRequest.Calculation calculation =
                new CalculationRequest.Calculation(
                        "dehradun",
                        roadType,
                        length,
                        width,
                        duration,
                        applicantType,
                        proposedStartDate,
                        applicationDate
                );

        return new CalculationRequest(
                requestInfo,
                calculation
        );
    }

    private static class TestFeeRateProvider
            implements FeeRateProvider {

        private final FeeConfiguration.Defaults defaults;

        private final List<FeeConfiguration.RoadTypeRate> roadTypes;

        TestFeeRateProvider() {

            defaults = new FeeConfiguration.Defaults();

            defaults.setUrgencyThresholdDays(3);
            defaults.setUrgencySurchargePercent(
                    new BigDecimal("10")
            );
            defaults.setSecurityDepositPercent(
                    new BigDecimal("25")
            );

            FeeConfiguration.RoadTypeRate bt =
                    new FeeConfiguration.RoadTypeRate();

            bt.setCode("BT");
            bt.setName("Bituminous");
            bt.setRestorationRatePerSqm(
                    new BigDecimal("1200")
            );
            bt.setPermissionRatePerSqmPerDay(
                    new BigDecimal("15")
            );
            bt.setMinSecurityDeposit(
                    new BigDecimal("5000")
            );
            bt.setActive(true);

            FeeConfiguration.RoadTypeRate wbm =
                    new FeeConfiguration.RoadTypeRate();

            wbm.setCode("WBM");
            wbm.setName("Water Bound Macadam");
            wbm.setRestorationRatePerSqm(
                    new BigDecimal("650")
            );
            wbm.setPermissionRatePerSqmPerDay(
                    new BigDecimal("8")
            );
            wbm.setMinSecurityDeposit(
                    new BigDecimal("2000")
            );
            wbm.setActive(true);

            FeeConfiguration.RoadTypeRate kutcha =
                    new FeeConfiguration.RoadTypeRate();

            kutcha.setCode("KUTCHA");
            kutcha.setName("Kutcha");
            kutcha.setRestorationRatePerSqm(
                    new BigDecimal("150")
            );
            kutcha.setPermissionRatePerSqmPerDay(
                    new BigDecimal("3")
            );
            kutcha.setMinSecurityDeposit(
                    new BigDecimal("500")
            );
            kutcha.setActive(false);

            roadTypes = List.of(bt, wbm, kutcha);

            defaults.setRoadTypes(roadTypes);
        }

        @Override
        public Optional<FeeConfiguration.RoadTypeRate> getRoadTypeRate(
                String tenantId,
                String roadType) {

            return roadTypes.stream()
                    .filter(rate ->
                            rate.getCode()
                                    .equalsIgnoreCase(roadType))
                    .filter(rate ->
                            Boolean.TRUE.equals(rate.getActive()))
                    .findFirst();
        }

        @Override
        public FeeConfiguration.Defaults getDefaults() {
            return defaults;
        }
    }
}