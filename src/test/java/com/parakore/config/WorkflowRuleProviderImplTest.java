package com.parakore.config;

import com.parakore.application.entity.ApplicationAction;
import com.parakore.application.entity.ApplicationStatus;
import com.parakore.application.entity.Role;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WorkflowRuleProviderImplTest {

    private WorkflowRuleProvider provider;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();

        WorkflowConfigurationLoader loader =
                new WorkflowConfigurationLoader(objectMapper);

        provider = new WorkflowRuleProviderImpl(loader);
    }

    @Test
    void shouldFindVerifyTransitionForVerifier() {

        var result = provider.findTransition(
                ApplicationStatus.APPLIED,
                ApplicationAction.VERIFY,
                Role.VERIFIER
        );

        assertTrue(result.isPresent());
        assertEquals("APPLIED", result.get().getFrom());
        assertEquals("VERIFY", result.get().getAction());
        assertEquals("PENDING_APPROVAL", result.get().getTo());
        assertTrue(
                result.get().getRoles().contains("VERIFIER")
        );
    }

    @Test
    void shouldFindApproveTransitionForApprover() {

        var result = provider.findTransition(
                ApplicationStatus.PENDING_APPROVAL,
                ApplicationAction.APPROVE,
                Role.APPROVER
        );

        assertTrue(result.isPresent());
        assertEquals("APPROVED", result.get().getTo());
    }

    @Test
    void shouldFindRejectTransitionForApprover() {

        var result = provider.findTransition(
                ApplicationStatus.PENDING_APPROVAL,
                ApplicationAction.REJECT,
                Role.APPROVER
        );

        assertTrue(result.isPresent());
        assertEquals("REJECTED", result.get().getTo());
    }

    @Test
    void shouldFindSendBackTransitionForVerifier() {

        var result = provider.findTransition(
                ApplicationStatus.PENDING_APPROVAL,
                ApplicationAction.SEND_BACK,
                Role.VERIFIER
        );

        assertTrue(result.isPresent());
        assertEquals("APPLIED", result.get().getTo());
    }

    @Test
    void shouldFindCancelTransitionForApplicant() {

        var result = provider.findTransition(
                ApplicationStatus.APPLIED,
                ApplicationAction.CANCEL,
                Role.APPLICANT
        );

        assertTrue(result.isPresent());
        assertEquals("CANCELLED", result.get().getTo());
    }

    @Test
    void shouldReturnEmptyWhenRoleIsNotAllowed() {

        var result = provider.findTransition(
                ApplicationStatus.APPLIED,
                ApplicationAction.VERIFY,
                Role.APPROVER
        );

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldReturnEmptyWhenStatusIsInvalid() {

        var result = provider.findTransition(
                ApplicationStatus.APPROVED,
                ApplicationAction.VERIFY,
                Role.VERIFIER
        );

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldReturnEmptyWhenActionIsInvalid() {

        var result = provider.findTransition(
                ApplicationStatus.APPLIED,
                ApplicationAction.APPROVE,
                Role.APPROVER
        );

        assertTrue(result.isEmpty());
    }
}