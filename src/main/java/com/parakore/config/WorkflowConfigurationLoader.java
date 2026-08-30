package com.parakore.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class WorkflowConfigurationLoader {

    private final WorkflowConfiguration workflowConfiguration;

    public WorkflowConfigurationLoader(ObjectMapper objectMapper) {

        try {
            ClassPathResource resource =
                    new ClassPathResource("workflow-rules.json");

            this.workflowConfiguration =
                    objectMapper.readValue(
                            resource.getInputStream(),
                            WorkflowConfiguration.class
                    );

        } catch (IOException e) {
            throw new IllegalStateException(
                    "Failed to load workflow-rules.json",
                    e
            );
        }
    }

    public WorkflowConfiguration getWorkflowConfiguration() {
        return workflowConfiguration;
    }
}