package com.parakore.application.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.parakore.application.dto.*;
import com.parakore.application.entity.ApplicationAction;
import com.parakore.application.entity.ApplicationStatus;
import com.parakore.application.service.ApplicationService;
import com.parakore.fee.dto.CalculationRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ApplicationController.class)
class ApplicationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ApplicationService applicationService;

    @Test
    void create_shouldReturn201() throws Exception {

        CreateApplicationRequest request =
                new CreateApplicationRequest(
                        requestInfo(),
                        calculation()
                );

        CreateApplicationResponse response =
                new CreateApplicationResponse(
                        new CreateApplicationResponse.ResponseInfo(
                                "create-001",
                                "successful"
                        ),
                        new CreateApplicationResponse.Application(
                                1L,
                                "DEH-RCP-000001-2026-27",
                                "dehradun",
                                "u-1",
                                "9990000001",
                                "BT",
                                new BigDecimal("10"),
                                new BigDecimal("5"),
                                2,
                                "PRIVATE",
                                LocalDate.of(2026, 9, 2),
                                new BigDecimal("50"),
                                new BigDecimal("60000"),
                                new BigDecimal("1500"),
                                new BigDecimal("150"),
                                new BigDecimal("15000"),
                                new BigDecimal("76650"),
                                ApplicationStatus.APPLIED
                        )
                );

        when(applicationService.create(any()))
                .thenReturn(response);

        mockMvc.perform(
                        post("/rcp/v1/_create")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ResponseInfo.status")
                        .value("successful"))
                .andExpect(jsonPath("$.Application.applicationNumber")
                        .value("DEH-RCP-000001-2026-27"))
                .andExpect(jsonPath("$.Application.status")
                        .value("APPLIED"));
    }

    @Test
    void action_shouldReturn200() throws Exception {

        ActionApplicationRequest request =
                new ActionApplicationRequest(
                        new ActionApplicationRequest.RequestInfo(
                                "portal",
                                "action-001",
                                new ActionApplicationRequest.UserInfo(
                                        "u-2",
                                        "9990000002",
                                        "dehradun",
                                        List.of(
                                                new ActionApplicationRequest.Role(
                                                        "VERIFIER"
                                                )
                                        )
                                )
                        ),
                        "DEH-RCP-000001-2026-27",
                        ApplicationAction.VERIFY,
                        "Verified"
                );

        ActionApplicationResponse response =
                new ActionApplicationResponse(
                        new ActionApplicationResponse.ResponseInfo(
                                "action-001",
                                "successful"
                        ),
                        new ActionApplicationResponse.Application(
                                1L,
                                "DEH-RCP-000001-2026-27",
                                "dehradun",
                                ApplicationStatus.PENDING_APPROVAL
                        )
                );

        when(applicationService.action(any()))
                .thenReturn(response);

        mockMvc.perform(
                        post("/rcp/v1/_action")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ResponseInfo.status")
                        .value("successful"))
                .andExpect(jsonPath("$.Application.status")
                        .value("PENDING_APPROVAL"));
    }

    @Test
    void search_shouldReturn200() throws Exception {

        SearchApplicationRequest request =
                new SearchApplicationRequest(
                        requestInfo(),
                        null,
                        ApplicationStatus.APPLIED,
                        null,
                         null,
                        0,
                        20
                );

        SearchApplicationResponse response =
                new SearchApplicationResponse(
                        new SearchApplicationResponse.ResponseInfo(
                                "search-001",
                                "successful"
                        ),
                        List.of(),
                        0,
                        0,
                        20
                );

        when(applicationService.search(any()))
                .thenReturn(response);

        mockMvc.perform(
                        post("/rcp/v1/_search")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ResponseInfo.status")
                        .value("successful"))
                .andExpect(jsonPath("$.totalCount")
                        .value(0));
    }

    @Test
    void create_shouldReturn400_whenRequestInfoMissing() throws Exception {

        String invalidRequest = """
                {
                  "Calculation": {
                    "tenantId": "dehradun",
                    "roadType": "BT",
                    "lengthInMeters": 10,
                    "widthInMeters": 5,
                    "durationInDays": 2,
                    "applicantType": "PRIVATE",
                    "proposedStartDate": "2026-09-02"
                  }
                }
                """;

        mockMvc.perform(
                        post("/rcp/v1/_create")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(invalidRequest)
                )
                .andExpect(status().isBadRequest());
    }

    private CalculationRequest.RequestInfo requestInfo() {

        return new CalculationRequest.RequestInfo(
                "portal",
                "test-001",
                new CalculationRequest.UserInfo(
                        "u-1",
                        "9990000001",
                        "dehradun",
                        List.of(
                                new CalculationRequest.Role("APPLICANT")
                        )
                )
        );
    }

    private CalculationRequest.Calculation calculation() {

        return new CalculationRequest.Calculation(
                "dehradun",
                "BT",
                new BigDecimal("10"),
                new BigDecimal("5"),
                2,
                "PRIVATE",
                LocalDate.of(2026, 9, 2),
                LocalDate.of(2026, 8, 31)
        );
    }
}