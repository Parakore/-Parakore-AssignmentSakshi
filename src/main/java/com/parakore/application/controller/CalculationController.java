package com.parakore.application.controller;

import com.parakore.fee.dto.CalculationRequest;
import com.parakore.fee.dto.CalculationResponse;
import com.parakore.fee.service.FeeCalculationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/rcp/v1")
@RequiredArgsConstructor
public class CalculationController {

    private final FeeCalculationService feeCalculationService;

    @PostMapping("/_calculate")
    public ResponseEntity<CalculationResponse> calculate(
            @Valid @RequestBody CalculationRequest request) {

        CalculationResponse response =
                feeCalculationService.calculate(request);

        return ResponseEntity.ok(response);
    }
}