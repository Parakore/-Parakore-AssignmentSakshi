package com.parakore.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex) {

        ErrorResponse response = new ErrorResponse(
                new ResponseInfo("failed"),
                List.of(
                        new ErrorDetail(
                                "INVALID_REQUEST",
                                ex.getMessage()
                        )
                )
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    public record ErrorResponse(
            ResponseInfo ResponseInfo,
            List<ErrorDetail> Errors
    ) {
    }

    public record ResponseInfo(
            String status
    ) {
    }

    public record ErrorDetail(
            String code,
            String message
    ) {
    }
}