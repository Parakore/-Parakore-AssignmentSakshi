package com.parakore.application.controller;

import com.parakore.application.dto.*;
import com.parakore.application.service.ApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/rcp/v1")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;

    @PostMapping("/_create")
    public ResponseEntity<CreateApplicationResponse> create(
            @Valid @RequestBody CreateApplicationRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(applicationService.create(request));
    }

    @PostMapping("/_action")
    public ResponseEntity<ActionApplicationResponse> action(
            @Valid @RequestBody ActionApplicationRequest request) {

        return ResponseEntity.ok(
                applicationService.action(request)
        );
    }

    @PostMapping("/_search")
    public ResponseEntity<SearchApplicationResponse> search(
            @Valid @RequestBody SearchApplicationRequest request) {

        return ResponseEntity.ok(
                applicationService.search(request)
        );
    }
}