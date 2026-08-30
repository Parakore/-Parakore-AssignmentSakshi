package com.parakore.fee.service;

import com.parakore.fee.dto.CalculationRequest;
import com.parakore.fee.dto.CalculationResponse;

public interface FeeCalculationService {

    CalculationResponse calculate(CalculationRequest request);
}