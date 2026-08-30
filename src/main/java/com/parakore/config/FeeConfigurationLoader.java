package com.parakore.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class FeeConfigurationLoader {

    private static final String CONFIG_FILE = "fee-rates.json";

    @Getter
    private final FeeConfiguration feeConfiguration;

    public FeeConfigurationLoader(ObjectMapper objectMapper) {
        try {
            ClassPathResource resource =
                    new ClassPathResource(CONFIG_FILE);

            this.feeConfiguration =
                    objectMapper.readValue(
                            resource.getInputStream(),
                            FeeConfiguration.class
                    );

        } catch (IOException e) {
            throw new IllegalStateException(
                    "Failed to load fee configuration from " + CONFIG_FILE,
                    e
            );
        }
    }
}
