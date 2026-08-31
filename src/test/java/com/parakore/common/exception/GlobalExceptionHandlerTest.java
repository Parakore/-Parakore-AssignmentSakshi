package com.parakore.common.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler =
            new GlobalExceptionHandler();

    @Test
    void shouldHandleIllegalArgumentException() {

        IllegalArgumentException exception =
                new IllegalArgumentException(
                        "Invalid application action"
                );

        var response =
                handler.handleIllegalArgumentException(exception);

        assertEquals(
                HttpStatus.BAD_REQUEST,
                response.getStatusCode()
        );

        assertNotNull(response.getBody());

        assertEquals(
                "failed",
                response.getBody().ResponseInfo().status()
        );

        assertEquals(
                1,
                response.getBody().Errors().size()
        );

        assertEquals(
                "INVALID_REQUEST",
                response.getBody().Errors().get(0).code()
        );

        assertEquals(
                "Invalid application action",
                response.getBody().Errors().get(0).message()
        );
    }

    @Test
    void shouldReturnBadRequestForDifferentExceptionMessage() {

        IllegalArgumentException exception =
                new IllegalArgumentException(
                        "Invalid workflow transition"
                );

        var response =
                handler.handleIllegalArgumentException(exception);

        assertEquals(
                HttpStatus.BAD_REQUEST,
                response.getStatusCode()
        );

        assertNotNull(response.getBody());

        assertEquals(
                "INVALID_REQUEST",
                response.getBody().Errors().get(0).code()
        );

        assertEquals(
                "Invalid workflow transition",
                response.getBody().Errors().get(0).message()
        );
    }
}