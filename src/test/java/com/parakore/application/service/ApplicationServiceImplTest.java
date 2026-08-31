package com.parakore.application.service;

import com.parakore.application.dto.CreateApplicationRequest;
import com.parakore.application.dto.CreateApplicationResponse;
import com.parakore.application.dto.SearchApplicationRequest;
import com.parakore.application.dto.SearchApplicationResponse;
import com.parakore.application.entity.Application;
import com.parakore.application.entity.ApplicationSequence;
import com.parakore.application.entity.ApplicationStatus;
import com.parakore.application.entity.ApplicationTransition;
import com.parakore.application.repository.ApplicationRepository;
import com.parakore.application.repository.ApplicationSequenceRepository;
import com.parakore.application.repository.ApplicationTransitionRepository;
import com.parakore.fee.dto.CalculationRequest;
import com.parakore.fee.dto.CalculationResponse;
import com.parakore.fee.service.FeeCalculationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceImplTest {

    @Mock
    private ApplicationTransitionRepository transitionRepository;

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private ApplicationSequenceRepository sequenceRepository;

    @Mock
    private FeeCalculationService feeCalculationService;

    @InjectMocks
    private ApplicationServiceImpl applicationService;

    private CalculationRequest.RequestInfo requestInfo;
    private CalculationRequest.Calculation calculation;

    @BeforeEach
    void setUp() {

        requestInfo = new CalculationRequest.RequestInfo(
                "portal",
                "msg-001",
                new CalculationRequest.UserInfo(
                        "user-001",
                        "9990000001",
                        "dehradun",
                        List.of(
                                new CalculationRequest.Role("APPLICANT")
                        )
                )
        );

        calculation = new CalculationRequest.Calculation(
                "dehradun",
                "BT",
                new BigDecimal("10"),
                new BigDecimal("5"),
                2,
                "PRIVATE",
                LocalDate.now().plusDays(5),
                LocalDate.now()
        );
    }

    @Test
    void shouldCreateApplicationSuccessfully() {

        CreateApplicationRequest request =
                new CreateApplicationRequest(
                        requestInfo,
                        calculation
                );

        CalculationResponse.Calculation fee =
                new CalculationResponse.Calculation(
                        new BigDecimal("50"),
                        new BigDecimal("60000"),
                        new BigDecimal("1500"),
                        BigDecimal.ZERO,
                        new BigDecimal("15000"),
                        new BigDecimal("76500"),
                        "K7Q2"
                );

        CalculationResponse calculationResponse =
                new CalculationResponse(
                        new CalculationResponse.ResponseInfo(
                                "msg-001",
                                "successful"
                        ),
                        fee
                );

        when(feeCalculationService.calculate(any()))
                .thenReturn(calculationResponse);

        ApplicationSequence sequence =
                new ApplicationSequence();

        sequence.setTenantId("dehradun");
        sequence.setFinancialYear(
                LocalDate.now().getYear() +
                        "-" +
                        String.valueOf(LocalDate.now().getYear() + 1)
                                .substring(2)
        );
        sequence.setModuleCode("RCP");
        sequence.setNextValue(1L);

        when(sequenceRepository.findForUpdate(
                anyString(),
                anyString(),
                eq("RCP")
        )).thenReturn(Optional.of(sequence));

        when(applicationRepository.save(any(Application.class)))
                .thenAnswer(invocation -> {

                    Application application =
                            invocation.getArgument(0);

                    application.setId(1L);

                    return application;
                });

        CreateApplicationResponse response =
                applicationService.create(request);

        assertNotNull(response);
        assertEquals(
                "successful",
                response.ResponseInfo().status()
        );

        assertNotNull(response.Application());
        assertEquals(
                "DEH-RCP-000001-" +
                        sequence.getFinancialYear(),
                response.Application().applicationNumber()
        );

        assertEquals(
                ApplicationStatus.APPLIED,
                response.Application().status()
        );

        verify(feeCalculationService).calculate(any());
        verify(sequenceRepository).save(sequence);
        verify(applicationRepository).save(any(Application.class));
        verify(transitionRepository).save(
                any(ApplicationTransition.class)
        );
    }

    @Test
    void shouldRejectCreateWhenTenantDoesNotMatch() {

        CalculationRequest.Calculation invalidCalculation =
                new CalculationRequest.Calculation(
                        "haridwar",
                        "BT",
                        new BigDecimal("10"),
                        new BigDecimal("5"),
                        2,
                        "PRIVATE",
                        LocalDate.now().plusDays(5),
                        LocalDate.now()
                );

        CreateApplicationRequest request =
                new CreateApplicationRequest(
                        requestInfo,
                        invalidCalculation
                );

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> applicationService.create(request)
                );

        assertEquals(
                "Request tenant does not match user tenant",
                exception.getMessage()
        );

        verifyNoInteractions(feeCalculationService);
        verifyNoInteractions(sequenceRepository);
        verifyNoInteractions(applicationRepository);
    }

    @Test
    void shouldSearchByApplicationNumber() {

        Application application = createApplication();

        when(applicationRepository
                .findByTenantIdAndApplicationNumber(
                        "dehradun",
                        "DEH-RCP-000001-2026-27"
                ))
                .thenReturn(Optional.of(application));

        SearchApplicationRequest request =
                new SearchApplicationRequest(
                        requestInfo,
                        "DEH-RCP-000001-2026-27",
                        null,
                        null,
                        null,
                        0,
                        20
                );

        SearchApplicationResponse response =
                applicationService.search(request);

        assertNotNull(response);
        assertEquals(
                "successful",
                response.ResponseInfo().status()
        );

        assertEquals(
                1,
                response.Applications().size()
        );

        assertEquals(
                "DEH-RCP-000001-2026-27",
                response.Applications()
                        .get(0)
                        .applicationNumber()
        );

        verify(applicationRepository)
                .findByTenantIdAndApplicationNumber(
                        "dehradun",
                        "DEH-RCP-000001-2026-27"
                );
    }

    @Test
    void shouldSearchByStatus() {

        Application application = createApplication();

        Page<Application> page =
                new PageImpl<>(
                        List.of(application),
                        PageRequest.of(0, 20),
                        1
                );

        when(applicationRepository.findByTenantIdAndStatus(
                eq("dehradun"),
                eq(ApplicationStatus.APPLIED),
                any()
        )).thenReturn(page);

        SearchApplicationRequest request =
                new SearchApplicationRequest(
                        requestInfo,
                        null,
                        ApplicationStatus.APPLIED,
                        null,
                        null,
                        0,
                        20
                );

        SearchApplicationResponse response =
                applicationService.search(request);

        assertEquals(1, response.Applications().size());
        assertEquals(1, response.totalCount());

        verify(applicationRepository)
                .findByTenantIdAndStatus(
                        eq("dehradun"),
                        eq(ApplicationStatus.APPLIED),
                        any()
                );
    }

    @Test
    void shouldSearchByMobileNumber() {

        Application application = createApplication();

        Page<Application> page =
                new PageImpl<>(
                        List.of(application),
                        PageRequest.of(0, 20),
                        1
                );

        when(applicationRepository.findByTenantIdAndApplicantMobile(
                eq("dehradun"),
                eq("9990000001"),
                any()
        )).thenReturn(page);

        SearchApplicationRequest request =
                new SearchApplicationRequest(
                        requestInfo,
                        null,
                        null,
                        "9990000001",
                        null,
                        0,
                        20
                );

        SearchApplicationResponse response =
                applicationService.search(request);

        assertEquals(1, response.Applications().size());
        assertEquals(
                "9990000001",
                response.Applications()
                        .get(0)
                        .applicantMobile()
        );

        verify(applicationRepository)
                .findByTenantIdAndApplicantMobile(
                        eq("dehradun"),
                        eq("9990000001"),
                        any()
                );
    }
    @Test
    void shouldSearchAllApplicationsWhenNoFiltersProvided() {

        Application application = createApplication();

        Page<Application> page =
                new PageImpl<>(
                        List.of(application),
                        PageRequest.of(0, 20),
                        1
                );

        when(applicationRepository.findByTenantId(
                eq("dehradun"),
                any()
        )).thenReturn(page);

        SearchApplicationRequest request =
                new SearchApplicationRequest(
                        requestInfo,
                        null,
                        null,
                        null,
                        null,
                        0,
                        20
                );

        SearchApplicationResponse response =
                applicationService.search(request);

        assertNotNull(response);
        assertEquals(1, response.Applications().size());
        assertEquals(1, response.totalCount());

        assertEquals(
                "DEH-RCP-000001-2026-27",
                response.Applications()
                        .get(0)
                        .applicationNumber()
        );

        verify(applicationRepository)
                .findByTenantId(
                        eq("dehradun"),
                        any()
                );
    }
    private Application createApplication() {

        Application application = new Application();

        application.setId(1L);
        application.setApplicationNumber(
                "DEH-RCP-000001-2026-27"
        );
        application.setTenantId("dehradun");
        application.setApplicantUuid("user-001");
        application.setApplicantMobile("9990000001");
        application.setRoadType("BT");
        application.setLengthInMeters(
                new BigDecimal("10")
        );
        application.setWidthInMeters(
                new BigDecimal("5")
        );
        application.setDurationInDays(2);
        application.setApplicantType("PRIVATE");
        application.setProposedStartDate(
                LocalDate.now().plusDays(5)
        );
        application.setAreaInSqm(
                new BigDecimal("50")
        );
        application.setRestorationCharge(
                new BigDecimal("60000")
        );
        application.setPermissionFee(
                new BigDecimal("1500")
        );
        application.setUrgencySurcharge(
                BigDecimal.ZERO
        );
        application.setSecurityDeposit(
                new BigDecimal("15000")
        );
        application.setTotalAmount(
                new BigDecimal("76500")
        );
        application.setStatus(
                ApplicationStatus.APPLIED
        );
        application.setCreatedBy("user-001");
        application.setCreatedTime(LocalDateTime.now());
        application.setLastModifiedBy("user-001");
        application.setLastModifiedTime(LocalDateTime.now());
        application.setVersion(0L);

        return application;
    }
}