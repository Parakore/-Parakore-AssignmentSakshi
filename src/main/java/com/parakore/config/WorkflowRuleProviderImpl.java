package com.parakore.config;

import com.parakore.application.entity.ApplicationAction;
import com.parakore.application.entity.ApplicationStatus;
import com.parakore.application.entity.Role;
import org.springframework.stereotype.Service;

@Service
public class WorkflowRuleProviderImpl implements WorkflowRuleProvider {

    private final WorkflowConfiguration configuration;

    public WorkflowRuleProviderImpl(
            WorkflowConfigurationLoader loader) {

        this.configuration =
                loader.getWorkflowConfiguration();
    }

    @Override
    public java.util.Optional<WorkflowConfiguration.Transition> findTransition(
            ApplicationStatus fromStatus,
            ApplicationAction action,
            Role role) {

        return configuration.getTransitions()
                .stream()
                .filter(transition ->
                        transition.getFrom().equalsIgnoreCase(
                                fromStatus.name()
                        )
                )
                .filter(transition ->
                        transition.getAction().equalsIgnoreCase(
                                action.name()
                        )
                )
                .filter(transition ->
                        transition.getRoles()
                                .stream()
                                .anyMatch(configuredRole ->
                                        configuredRole.equalsIgnoreCase(
                                                role.name()
                                        )
                                )
                )
                .findFirst();
    }
}