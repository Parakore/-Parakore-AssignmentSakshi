package com.parakore.config;

import com.parakore.application.entity.ApplicationAction;
import com.parakore.application.entity.ApplicationStatus;
import com.parakore.application.entity.Role;

import java.util.Optional;

public interface WorkflowRuleProvider {

    Optional<WorkflowConfiguration.Transition> findTransition(
            ApplicationStatus fromStatus,
            ApplicationAction action,
            Role role
    );
}