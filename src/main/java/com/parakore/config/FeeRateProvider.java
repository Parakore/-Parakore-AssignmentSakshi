package com.parakore.config;

import java.util.Optional;

public interface FeeRateProvider {

    Optional<FeeConfiguration.RoadTypeRate> getRoadTypeRate(
            String tenantId,
            String roadType
    );

    FeeConfiguration.Defaults getDefaults();
}
