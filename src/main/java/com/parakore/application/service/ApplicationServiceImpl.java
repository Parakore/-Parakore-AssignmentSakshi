package com.parakore.application.service;

import com.parakore.application.dto.SearchApplicationRequest;
import com.parakore.application.dto.SearchApplicationResponse;
import com.parakore.application.dto.ActionApplicationRequest;
import com.parakore.application.dto.ActionApplicationResponse;
import com.parakore.application.dto.CreateApplicationRequest;
import com.parakore.application.dto.CreateApplicationResponse;
import com.parakore.application.entity.*;
import com.parakore.application.repository.ApplicationRepository;
import com.parakore.application.repository.ApplicationSequenceRepository;
import com.parakore.application.repository.ApplicationTransitionRepository;
import com.parakore.config.WorkflowConfiguration;
import com.parakore.config.WorkflowRuleProvider;
import com.parakore.fee.dto.CalculationRequest;
import com.parakore.fee.dto.CalculationResponse;
import com.parakore.fee.service.FeeCalculationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ApplicationServiceImpl implements ApplicationService {

    private static final String MODULE_CODE = "RCP";
    private final ApplicationTransitionRepository transitionRepository;
    private final ApplicationRepository applicationRepository;
    private final ApplicationSequenceRepository sequenceRepository;
    private final FeeCalculationService feeCalculationService;
    private final WorkflowRuleProvider workflowRuleProvider;

    @Override
    @Transactional
    public CreateApplicationResponse create(
            CreateApplicationRequest request) {

        CalculationRequest.RequestInfo requestInfo =
                request.RequestInfo();

        CalculationRequest.Calculation calculation =
                request.Calculation();

        String tenantId = requestInfo.userInfo().tenantId();

        // Server-side tenant validation
        if (!tenantId.equals(calculation.tenantId())) {
            throw new IllegalArgumentException(
                    "Request tenant does not match user tenant"
            );
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDate applicationDate = now.toLocalDate();

        /*
         * Rebuild the calculation request with the actual
         * server-side application date.
         *
         * We never trust fee values from the client.
         */
        CalculationRequest feeRequest =
                new CalculationRequest(
                        requestInfo,
                        new CalculationRequest.Calculation(
                                calculation.tenantId(),
                                calculation.roadType(),
                                calculation.lengthInMeters(),
                                calculation.widthInMeters(),
                                calculation.durationInDays(),
                                calculation.applicantType(),
                                calculation.proposedStartDate(),
                                applicationDate
                        )
                );

        CalculationResponse calculationResponse =
                feeCalculationService.calculate(feeRequest);

        CalculationResponse.Calculation fee =
                calculationResponse.Calculation();

        String financialYear =
                getFinancialYear(applicationDate);

        ApplicationSequence sequence =
                sequenceRepository
                        .findForUpdate(
                                tenantId,
                                financialYear,
                                MODULE_CODE
                        )
                        .orElseGet(() -> createInitialSequence(
                                tenantId,
                                financialYear
                        ));

        long sequenceNumber = sequence.getNextValue();

        sequence.setNextValue(sequenceNumber + 1);

        sequenceRepository.save(sequence);

        String applicationNumber =
                generateApplicationNumber(
                        tenantId,
                        sequenceNumber,
                        financialYear
                );

        Application application = new Application();

        application.setApplicationNumber(applicationNumber);
        application.setTenantId(tenantId);
        application.setApplicantUuid(
                requestInfo.userInfo().uuid()
        );
        application.setApplicantMobile(
                requestInfo.userInfo().userName()
        );

        application.setRoadType(calculation.roadType());
        application.setLengthInMeters(
                calculation.lengthInMeters()
        );
        application.setWidthInMeters(
                calculation.widthInMeters()
        );
        application.setDurationInDays(
                calculation.durationInDays()
        );
        application.setApplicantType(
                calculation.applicantType()
        );
        application.setProposedStartDate(
                calculation.proposedStartDate()
        );

        application.setAreaInSqm(fee.areaInSqm());
        application.setRestorationCharge(
                fee.restorationCharge()
        );
        application.setPermissionFee(
                fee.permissionFee()
        );
        application.setUrgencySurcharge(
                fee.urgencySurcharge()
        );
        application.setSecurityDeposit(
                fee.securityDeposit()
        );
        application.setTotalAmount(
                fee.totalAmount()
        );

        application.setStatus(ApplicationStatus.APPLIED);

        application.setCreatedBy(
                requestInfo.userInfo().uuid()
        );
        application.setCreatedTime(now);
        application.setLastModifiedBy(
                requestInfo.userInfo().uuid()
        );
        application.setLastModifiedTime(now);
        application.setVersion(0L);

        Application saved =
                applicationRepository.save(application);

        ApplicationTransition transition = new ApplicationTransition();

        transition.setApplication(saved);
        transition.setFromStatus(null);
        transition.setAction("CREATE");
        transition.setToStatus(ApplicationStatus.APPLIED.name());
        transition.setActorUuid(
                requestInfo.userInfo().uuid()
        );
        transition.setActorUsername(
                requestInfo.userInfo().userName()
        );
        transition.setActorRole("APPLICANT");
        transition.setComment("Application created");
        transition.setCreatedTime(now);

        transitionRepository.save(transition);

        return new CreateApplicationResponse(

                new CreateApplicationResponse.ResponseInfo(
                        requestInfo.msgId(),
                        "successful"
                ),

                new CreateApplicationResponse.Application(
                        saved.getId(),
                        saved.getApplicationNumber(),
                        saved.getTenantId(),
                        saved.getApplicantUuid(),
                        saved.getApplicantMobile(),
                        saved.getRoadType(),
                        saved.getLengthInMeters(),
                        saved.getWidthInMeters(),
                        saved.getDurationInDays(),
                        saved.getApplicantType(),
                        saved.getProposedStartDate(),
                        saved.getAreaInSqm(),
                        saved.getRestorationCharge(),
                        saved.getPermissionFee(),
                        saved.getUrgencySurcharge(),
                        saved.getSecurityDeposit(),
                        saved.getTotalAmount(),
                        saved.getStatus()
                )
        );
    }

    private ApplicationSequence createInitialSequence(
            String tenantId,
            String financialYear) {

        ApplicationSequence sequence =
                new ApplicationSequence();

        sequence.setTenantId(tenantId);
        sequence.setFinancialYear(financialYear);
        sequence.setModuleCode(MODULE_CODE);
        sequence.setNextValue(1L);

        return sequenceRepository.save(sequence);
    }

    private String generateApplicationNumber(
            String tenantId,
            long sequenceNumber,
            String financialYear) {

        String cityPrefix =
                tenantId.length() >= 3
                        ? tenantId.substring(0, 3).toUpperCase()
                        : tenantId.toUpperCase();

        return String.format(
                "%s-%s-%06d-%s",
                cityPrefix,
                MODULE_CODE,
                sequenceNumber,
                financialYear
        );
    }

    private String getFinancialYear(LocalDate date) {

        int year = date.getYear();

        if (date.getMonthValue() >= 4) {
            return year + "-" + String.valueOf(year + 1).substring(2);
        }

        return (year - 1) + "-"
                + String.valueOf(year).substring(2);
    }

    @Override
    @Transactional
    public ActionApplicationResponse action(
            ActionApplicationRequest request) {

        String tenantId =
                request.RequestInfo().userInfo().tenantId();

        String applicationNumber =
                request.applicationNumber();

        Application application =
                applicationRepository
                        .findByTenantIdAndApplicationNumber(
                                tenantId,
                                applicationNumber
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Application " +
                                                applicationNumber +
                                                " not found for tenant " +
                                                tenantId
                                )
                        );

        ApplicationStatus currentStatus =
                application.getStatus();

        /*
         * Find the actor role that is allowed to perform
         * the requested action from workflow configuration.
         */
        Role actorRole =
                request.RequestInfo()
                        .userInfo()
                        .roles()
                        .stream()
                        .map(role -> Role.valueOf(
                                role.code().toUpperCase()
                        ))
                        .filter(role ->
                                workflowRuleProvider
                                        .findTransition(
                                                currentStatus,
                                                request.action(),
                                                role
                                        )
                                        .isPresent()
                        )
                        .findFirst()
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Role is not allowed to perform action "
                                                + request.action()
                                )
                        );

        /*
         * Find the configured transition.
         */
        WorkflowConfiguration.Transition transition =
                workflowRuleProvider
                        .findTransition(
                                currentStatus,
                                request.action(),
                                actorRole
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Action " +
                                                request.action() +
                                                " is not allowed from status " +
                                                currentStatus
                                )
                        );

        ApplicationStatus targetStatus;

        try {

            targetStatus =
                    ApplicationStatus.valueOf(
                            transition.getTo().toUpperCase()
                    );

        } catch (IllegalArgumentException e) {

            throw new IllegalArgumentException(
                    "Invalid target status configured for action "
                            + request.action()
            );
        }

        LocalDateTime now =
                LocalDateTime.now();

        /*
         * Record transition history.
         */
        ApplicationTransition applicationTransition =
                new ApplicationTransition();

        applicationTransition.setApplication(application);

        applicationTransition.setFromStatus(
                currentStatus.name()
        );

        applicationTransition.setAction(
                request.action().name()
        );

        applicationTransition.setToStatus(
                targetStatus.name()
        );

        applicationTransition.setActorUuid(
                request.RequestInfo().userInfo().uuid()
        );

        applicationTransition.setActorUsername(
                request.RequestInfo().userInfo().userName()
        );

        applicationTransition.setActorRole(
                actorRole.name()
        );

        applicationTransition.setComment(
                request.comment()
        );

        applicationTransition.setCreatedTime(now);

        transitionRepository.save(applicationTransition);

        /*
         * Update application status.
         */
        application.setStatus(targetStatus);

        application.setLastModifiedBy(
                request.RequestInfo().userInfo().uuid()
        );

        application.setLastModifiedTime(now);

        Application saved =
                applicationRepository.save(application);

        return new ActionApplicationResponse(

                new ActionApplicationResponse.ResponseInfo(
                        request.RequestInfo().msgId(),
                        "successful"
                ),

                new ActionApplicationResponse.Application(
                        saved.getId(),
                        saved.getApplicationNumber(),
                        saved.getTenantId(),
                        saved.getStatus()
                )
        );
    }

    @Override
    @Transactional(readOnly = true)
    public SearchApplicationResponse search(
            SearchApplicationRequest request) {

        String tenantId = request.RequestInfo()
                .userInfo()
                .tenantId();

        String applicationNumber = request.applicationNumber();
        ApplicationStatus status = request.status();
        String mobileNumber = request.mobileNumber();

        int offset = request.offset() == null
                ? 0
                : request.offset();

        int limit = request.limit() == null
                ? 20
                : Math.min(request.limit(), 100);

        Pageable pageable = PageRequest.of(
                offset / limit,
                limit
        );

        Page<Application> page;

        if (applicationNumber != null &&
                !applicationNumber.isBlank()) {

            Optional<Application> application =
                    applicationRepository
                            .findByTenantIdAndApplicationNumber(
                                    tenantId,
                                    applicationNumber
                            );

            List<Application> applications =
                    application
                            .map(List::of)
                            .orElse(List.of());

            return new SearchApplicationResponse(
                    new SearchApplicationResponse.ResponseInfo(
                            request.RequestInfo().msgId(),
                            "successful"
                    ),
                    applications.stream()
                            .map(this::toSearchApplication)
                            .toList(),
                    applications.size(),
                    offset,
                    limit
            );
        }

        if (status != null && mobileNumber != null &&
                !mobileNumber.isBlank()) {

            page = applicationRepository
                    .findByTenantIdAndStatusAndApplicantMobile(
                            tenantId,
                            status,
                            mobileNumber,
                            pageable
                    );

        } else if (status != null) {

            page = applicationRepository
                    .findByTenantIdAndStatus(
                            tenantId,
                            status,
                            pageable
                    );

        } else if (mobileNumber != null &&
                !mobileNumber.isBlank()) {

            page = applicationRepository
                    .findByTenantIdAndApplicantMobile(
                            tenantId,
                            mobileNumber,
                            pageable
                    );

        } else {

            page = applicationRepository
                    .findByTenantId(
                            tenantId,
                            pageable
                    );
        }

        return new SearchApplicationResponse(
                new SearchApplicationResponse.ResponseInfo(
                        request.RequestInfo().msgId(),
                        "successful"
                ),
                page.getContent()
                        .stream()
                        .map(this::toSearchApplication)
                        .toList(),
                (int) page.getTotalElements(),
                offset,
                limit
        );
    }

    private SearchApplicationResponse.Application toSearchApplication(
            Application application) {

        return new SearchApplicationResponse.Application(
                application.getId(),
                application.getApplicationNumber(),
                application.getTenantId(),
                application.getApplicantUuid(),
                application.getApplicantMobile(),
                application.getRoadType(),
                application.getLengthInMeters(),
                application.getWidthInMeters(),
                application.getDurationInDays(),
                application.getApplicantType(),
                application.getProposedStartDate(),
                application.getAreaInSqm(),
                application.getRestorationCharge(),
                application.getPermissionFee(),
                application.getUrgencySurcharge(),
                application.getSecurityDeposit(),
                application.getTotalAmount(),
                application.getStatus()
        );
    }
}

