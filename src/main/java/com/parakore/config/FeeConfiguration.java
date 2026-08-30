package com.parakore.config;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class FeeConfiguration {

    private Defaults defaults;
    private Map<String, TenantOverride> tenants;

    @Data
    public static class Defaults {

        private List<RoadTypeRate> roadTypes;

        private int urgencyThresholdDays;

        private BigDecimal urgencySurchargePercent;

        private BigDecimal securityDepositPercent;
    }

    @Data
    public static class RoadTypeRate {

        private String code;

        private String name;

        private BigDecimal restorationRatePerSqm;

        private BigDecimal permissionRatePerSqmPerDay;

        private BigDecimal minSecurityDeposit;

        private Boolean active;
    }

    @Data
    public static class TenantOverride {

        private List<RoadTypeRate> roadTypes;
    }
}