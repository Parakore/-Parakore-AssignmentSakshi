package com.parakore.application.dto;

import com.parakore.application.entity.ApplicationAction;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ActionApplicationRequest(

        @NotNull
        @Valid
        RequestInfo RequestInfo,

        @NotBlank
        String applicationNumber,

        @NotNull
        ApplicationAction action,

        String comment
) {

    public record RequestInfo(

            @NotBlank
            String apiId,

            @NotBlank
            String msgId,

            @NotNull
            @Valid
            UserInfo userInfo
    ) {
    }

    public record UserInfo(

            @NotBlank
            String uuid,

            @NotBlank
            String userName,

            @NotBlank
            String tenantId,

            @NotNull
            java.util.List<Role> roles
    ) {
    }

    public record Role(

            @NotBlank
            String code
    ) {
    }
}