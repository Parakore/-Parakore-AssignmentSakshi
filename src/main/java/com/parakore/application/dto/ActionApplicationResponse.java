package com.parakore.application.dto;

import com.parakore.application.entity.ApplicationStatus;

public record ActionApplicationResponse(

        ResponseInfo ResponseInfo,

        Application Application
) {

    public record ResponseInfo(
            String msgId,
            String status
    ) {
    }

    public record Application(
            Long id,
            String applicationNumber,
            String tenantId,
            ApplicationStatus status
    ) {
    }
}