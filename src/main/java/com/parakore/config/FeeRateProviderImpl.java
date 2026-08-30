package com.parakore.config;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class FeeRateProviderImpl implements FeeRateProvider {

    private final FeeConfiguration configuration;

    public FeeRateProviderImpl(FeeConfigurationLoader loader) {
        this.configuration = loader.getFeeConfiguration();
    }

    @Override
    public Optional<FeeConfiguration.RoadTypeRate> getRoadTypeRate(
            String tenantId,
            String roadType
    ) {
        FeeConfiguration.RoadTypeRate defaultRate =
                findRoadType(
                        configuration.getDefaults().getRoadTypes(),
                        roadType
                );

        // Unknown road type
        if (defaultRate == null) {
            return Optional.empty();
        }

        // Inactive road type
        if (!Boolean.TRUE.equals(defaultRate.getActive())) {
            return Optional.empty();
        }

        FeeConfiguration.TenantOverride tenantOverride =
                configuration.getTenants() != null
                        ? configuration.getTenants().get(tenantId)
                        : null;

        // No tenant override
        if (tenantOverride == null ||
                tenantOverride.getRoadTypes() == null) {
            return Optional.of(defaultRate);
        }

        FeeConfiguration.RoadTypeRate override =
                findRoadType(
                        tenantOverride.getRoadTypes(),
                        roadType
                );

        // No override for this road type
        if (override == null) {
            return Optional.of(defaultRate);
        }

        return Optional.of(merge(defaultRate, override));
    }

    @Override
    public FeeConfiguration.Defaults getDefaults() {
        return configuration.getDefaults();
    }

    private FeeConfiguration.RoadTypeRate findRoadType(
            List<FeeConfiguration.RoadTypeRate> roadTypes,
            String roadType
    ) {
        if (roadTypes == null || roadType == null) {
            return null;
        }

        return roadTypes.stream()
                .filter(rate ->
                        rate.getCode() != null &&
                                rate.getCode().equalsIgnoreCase(roadType)
                )
                .findFirst()
                .orElse(null);
    }

    private FeeConfiguration.RoadTypeRate merge(
            FeeConfiguration.RoadTypeRate defaultRate,
            FeeConfiguration.RoadTypeRate override
    ) {
        FeeConfiguration.RoadTypeRate effective =
                new FeeConfiguration.RoadTypeRate();

        effective.setCode(defaultRate.getCode());

        effective.setName(
                override.getName() != null
                        ? override.getName()
                        : defaultRate.getName()
        );

        effective.setRestorationRatePerSqm(
                override.getRestorationRatePerSqm() != null
                        ? override.getRestorationRatePerSqm()
                        : defaultRate.getRestorationRatePerSqm()
        );

        effective.setPermissionRatePerSqmPerDay(
                override.getPermissionRatePerSqmPerDay() != null
                        ? override.getPermissionRatePerSqmPerDay()
                        : defaultRate.getPermissionRatePerSqmPerDay()
        );

        effective.setMinSecurityDeposit(
                override.getMinSecurityDeposit() != null
                        ? override.getMinSecurityDeposit()
                        : defaultRate.getMinSecurityDeposit()
        );

        effective.setActive(
                override.getActive() != null
                        ? override.getActive()
                        : defaultRate.getActive()
        );

        return effective;
    }
}
