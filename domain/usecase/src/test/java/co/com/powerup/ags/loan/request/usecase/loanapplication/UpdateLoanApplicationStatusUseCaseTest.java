package co.com.powerup.ags.loan.request.usecase.loanapplication;

import co.com.powerup.ags.loan.request.model.loanapplication.LoanApplication;
import co.com.powerup.ags.loan.request.model.loanapplication.gateways.LoanApplicationRepository;
import co.com.powerup.ags.loan.request.model.loanapplicationstatus.LoanApplicationStatus;
import co.com.powerup.ags.loan.request.model.loanapplicationstatus.LoanApplicationStatusEnum;
import co.com.powerup.ags.loan.request.model.loanapplicationstatus.gateways.LoanApplicationStatusRepository;
import co.com.powerup.ags.loan.request.model.loantype.LoanType;
import co.com.powerup.ags.loan.request.model.loantype.gateways.LoanTypeRepository;
import co.com.powerup.ags.loan.request.model.notification.LoanApplicationNotification;
import co.com.powerup.ags.loan.request.model.notification.PaymentPlanItem;
import co.com.powerup.ags.loan.request.model.notification.gateway.NotificationGateway;
import co.com.powerup.ags.loan.request.model.user.User;
import co.com.powerup.ags.loan.request.model.user.gateways.UserGateway;
import co.com.powerup.ags.loan.request.usecase.loanapplication.dto.UpdateLoanApplicationCommand;
import co.com.powerup.ags.loan.request.usecase.loanapplication.dto.UpdateLoanAutomaticValidationCommand;
import co.com.powerup.ags.loan.request.usecase.loanapplication.dto.ValidationResponse;
import co.com.powerup.ags.loan.request.usecase.loanapplication.dto.ValidationAnalysis;
import co.com.powerup.ags.loan.request.usecase.common.exception.LoanApplicationNotFoundException;
import co.com.powerup.ags.loan.request.usecase.common.exception.LoanApplicationStatusNotFoundException;
import co.com.powerup.ags.loan.request.usecase.common.exception.UpdateLoanApplicationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateLoanApplicationStatusUseCaseTest {

    @Mock
    private LoanApplicationRepository loanApplicationRepository;

    @Mock
    private LoanApplicationStatusRepository loanApplicationStatusRepository;

    @Mock
    private LoanTypeRepository loanTypeRepository;

    @Mock
    private NotificationGateway notificationGateway;

    @Mock
    private UserGateway userGateway;

    @Mock
    private PaymentPlanUseCase paymentPlanUseCase;

    private UpdateLoanApplicationStatusUseCase updateLoanApplicationStatusUseCase;

    private LoanApplication sampleLoanApplication;
    private LoanApplicationStatus pendingStatus;
    private LoanApplicationStatus approvedStatus;
    private LoanApplicationStatus rejectedStatus;
    private LoanType personalLoanType;
    private User sampleUser;

    private static final String LOAN_ID = "29a4639d-b328-453a-a408-d2eff0bcae84";
    private static final String USER_EMAIL = "maria.rodriguez@ejemplo.com";
    private static final String USER_NAME = "Maria Rodriguez";
    private static final String USER_ID = "1234567890";
    private static final BigDecimal LOAN_AMOUNT = new BigDecimal("50000.00");
    private static final Integer LOAN_TERM = 24;
    private static final BigDecimal MONTHLY_PAYMENT = new BigDecimal("2500.00");
    private static final String REJECTION_REASON = "Ingresos insuficientes";

    @BeforeEach
    void setUp() {
        updateLoanApplicationStatusUseCase = new UpdateLoanApplicationStatusUseCase(
                loanApplicationRepository,
                loanApplicationStatusRepository,
                loanTypeRepository,
                notificationGateway,
                userGateway,
                paymentPlanUseCase
        );

        pendingStatus = LoanApplicationStatus.builder()
                .id(1)
                .name("PENDING")
                .description("Pending review")
                .build();

        approvedStatus = LoanApplicationStatus.builder()
                .id(3)
                .name("APPROVED")
                .description("Approved")
                .build();

        rejectedStatus = LoanApplicationStatus.builder()
                .id(4)
                .name("REJECTED")
                .description("Rejected")
                .build();

        personalLoanType = LoanType.builder()
                .id(1)
                .name("Personal Loan")
                .minAmount(new BigDecimal("1000.00"))
                .maxAmount(new BigDecimal("100000.00"))
                .minTerm(6)
                .maxTerm(60)
                .interestRate(new BigDecimal("12.5"))
                .automaticValidation(true)
                .build();

        sampleLoanApplication = LoanApplication.builder()
                .id(LOAN_ID)
                .email(USER_EMAIL)
                .amount(LOAN_AMOUNT)
                .term(LOAN_TERM)
                .status(pendingStatus)
                .loanType(personalLoanType)
                .build();

        sampleUser = User.builder()
                .id(UUID.randomUUID().toString())
                .email(USER_EMAIL)
                .name(USER_NAME)
                .lastName("Rodriguez")
                .baseSalary(new BigDecimal("5000000.00"))
                .phoneNumber("+57300123456")
                .address("Calle 123 # 45-67, Bogotá")
                .birthDate(LocalDate.of(1985, 6, 15))
                .idNumber(USER_ID)
                .build();
    }

    @Test
    void shouldUpdateLoanApplicationStatusSuccessfully() {
        UpdateLoanApplicationCommand command = new UpdateLoanApplicationCommand(LOAN_ID, 3);
        
        LoanApplication updatedLoanApplication = sampleLoanApplication.toBuilder()
                .status(approvedStatus)
                .build();

        when(loanApplicationRepository.getById(LOAN_ID))
                .thenReturn(Mono.just(sampleLoanApplication));
        when(loanApplicationStatusRepository.getById(3))
                .thenReturn(Mono.just(approvedStatus));
        when(loanApplicationRepository.saveLoanApplication(any(LoanApplication.class)))
                .thenReturn(Mono.just(updatedLoanApplication));
        when(loanApplicationStatusRepository.getByNames(Set.of(
                LoanApplicationStatusEnum.APPROVED.name(), 
                LoanApplicationStatusEnum.REJECTED.name(),
                LoanApplicationStatusEnum.UNDER_REVIEW.name())))
                .thenReturn(Flux.just(approvedStatus, rejectedStatus));
        when(loanTypeRepository.getById(personalLoanType.getId()))
                .thenReturn(Mono.just(personalLoanType));
        when(userGateway.getUserByIdNumberOrEmail(null, USER_EMAIL))
                .thenReturn(Mono.just(sampleUser));
        when(paymentPlanUseCase.calculatePaymentSchedule(any(), any(), any(), any()))
                .thenReturn(Flux.just(
                    PaymentPlanItem.builder()
                        .paymentNumber(1)
                        .principalPayment(new BigDecimal("2000.00"))
                        .interestPayment(new BigDecimal("500.00"))
                        .totalPayment(MONTHLY_PAYMENT)
                        .remainingBalance(new BigDecimal("48000.00"))
                        .build()
                ));
        when(notificationGateway.notify(any(LoanApplicationNotification.class)))
                .thenReturn(Mono.empty());

        StepVerifier.create(updateLoanApplicationStatusUseCase.updateLoanApplicationStatus(command))
                .expectNextMatches(result -> {
                    assertEquals(LOAN_ID, result.getId());
                    assertEquals(3, result.getStatus().getId());
                    return true;
                })
                .verifyComplete();

        verify(notificationGateway).notify(any(LoanApplicationNotification.class));
    }

    @Test
    void shouldUpdateStatusToRejectedAndSendNotification() {
        UpdateLoanApplicationCommand command = new UpdateLoanApplicationCommand(LOAN_ID, 4);
        
        LoanApplication updatedLoanApplication = sampleLoanApplication.toBuilder()
                .status(rejectedStatus)
                .build();

        when(loanApplicationRepository.getById(LOAN_ID))
                .thenReturn(Mono.just(sampleLoanApplication));
        when(loanApplicationStatusRepository.getById(4))
                .thenReturn(Mono.just(rejectedStatus));
        when(loanApplicationRepository.saveLoanApplication(any(LoanApplication.class)))
                .thenReturn(Mono.just(updatedLoanApplication));
        when(loanApplicationStatusRepository.getByNames(Set.of(
                LoanApplicationStatusEnum.APPROVED.name(), 
                LoanApplicationStatusEnum.REJECTED.name(),
                LoanApplicationStatusEnum.UNDER_REVIEW.name())))
                .thenReturn(Flux.just(approvedStatus, rejectedStatus));
        when(loanTypeRepository.getById(personalLoanType.getId()))
                .thenReturn(Mono.just(personalLoanType));
        when(userGateway.getUserByIdNumberOrEmail(null, USER_EMAIL))
                .thenReturn(Mono.just(sampleUser));
        when(notificationGateway.notify(any(LoanApplicationNotification.class)))
                .thenReturn(Mono.empty());

        StepVerifier.create(updateLoanApplicationStatusUseCase.updateLoanApplicationStatus(command))
                .expectNextMatches(result -> {
                    assertEquals(LOAN_ID, result.getId());
                    assertEquals(4, result.getStatus().getId());
                    return true;
                })
                .verifyComplete();

        verify(notificationGateway).notify(any(LoanApplicationNotification.class));
    }

    @Test
    void shouldUpdateStatusToUnderReviewAndNotSendNotification() {
        LoanApplicationStatus underReviewStatus = LoanApplicationStatus.builder()
                .id(2)
                .name("UNDER_REVIEW")
                .description("Under review")
                .build();

        UpdateLoanApplicationCommand command = new UpdateLoanApplicationCommand(LOAN_ID, 2);
        
        LoanApplication updatedLoanApplication = sampleLoanApplication.toBuilder()
                .status(underReviewStatus)
                .build();

        when(loanApplicationRepository.getById(LOAN_ID))
                .thenReturn(Mono.just(sampleLoanApplication));
        when(loanApplicationStatusRepository.getById(2))
                .thenReturn(Mono.just(underReviewStatus));
        when(loanApplicationRepository.saveLoanApplication(any(LoanApplication.class)))
                .thenReturn(Mono.just(updatedLoanApplication));
        when(loanApplicationStatusRepository.getByNames(Set.of(
                LoanApplicationStatusEnum.APPROVED.name(), 
                LoanApplicationStatusEnum.REJECTED.name(),
                LoanApplicationStatusEnum.UNDER_REVIEW.name())))
                .thenReturn(Flux.just(approvedStatus, rejectedStatus));

        StepVerifier.create(updateLoanApplicationStatusUseCase.updateLoanApplicationStatus(command))
                .expectNextMatches(result -> {
                    assertEquals(LOAN_ID, result.getId());
                    assertEquals(2, result.getStatus().getId());
                    return true;
                })
                .verifyComplete();

        verify(notificationGateway, never()).notify(any(LoanApplicationNotification.class));
        verify(userGateway, never()).getUserByIdNumberOrEmail(anyString(), anyString());
    }

    @Test
    void shouldThrowLoanApplicationNotFoundExceptionWhenLoanDoesNotExist() {
        UpdateLoanApplicationCommand command = new UpdateLoanApplicationCommand(LOAN_ID, 3);

        when(loanApplicationRepository.getById(LOAN_ID))
                .thenReturn(Mono.empty());

        StepVerifier.create(updateLoanApplicationStatusUseCase.updateLoanApplicationStatus(command))
                .expectError(LoanApplicationNotFoundException.class)
                .verify();
    }

    @Test
    void shouldThrowUpdateLoanApplicationExceptionWhenStatusIsSame() {
        UpdateLoanApplicationCommand command = new UpdateLoanApplicationCommand(LOAN_ID, 1);

        when(loanApplicationRepository.getById(LOAN_ID))
                .thenReturn(Mono.just(sampleLoanApplication));

        StepVerifier.create(updateLoanApplicationStatusUseCase.updateLoanApplicationStatus(command))
                .expectError(UpdateLoanApplicationException.class)
                .verify();
    }

    @Test
    void shouldThrowLoanApplicationStatusNotFoundExceptionWhenStatusDoesNotExist() {
        UpdateLoanApplicationCommand command = new UpdateLoanApplicationCommand(LOAN_ID, 999);

        when(loanApplicationRepository.getById(LOAN_ID))
                .thenReturn(Mono.just(sampleLoanApplication));
        when(loanApplicationStatusRepository.getById(999))
                .thenReturn(Mono.empty());

        StepVerifier.create(updateLoanApplicationStatusUseCase.updateLoanApplicationStatus(command))
                .expectError(LoanApplicationStatusNotFoundException.class)
                .verify();
    }

    @Test
    void shouldHandleNotificationFailureGracefully() {
        UpdateLoanApplicationCommand command = new UpdateLoanApplicationCommand(LOAN_ID, 3);
        
        LoanApplication updatedLoanApplication = sampleLoanApplication.toBuilder()
                .status(approvedStatus)
                .build();

        when(loanApplicationRepository.getById(LOAN_ID))
                .thenReturn(Mono.just(sampleLoanApplication));
        when(loanApplicationStatusRepository.getById(3))
                .thenReturn(Mono.just(approvedStatus));
        when(loanApplicationRepository.saveLoanApplication(any(LoanApplication.class)))
                .thenReturn(Mono.just(updatedLoanApplication));
        when(loanApplicationStatusRepository.getByNames(Set.of(
                LoanApplicationStatusEnum.APPROVED.name(), 
                LoanApplicationStatusEnum.REJECTED.name(),
                LoanApplicationStatusEnum.UNDER_REVIEW.name())))
                .thenReturn(Flux.just(approvedStatus, rejectedStatus));
        when(loanTypeRepository.getById(personalLoanType.getId()))
                .thenReturn(Mono.just(personalLoanType));
        when(userGateway.getUserByIdNumberOrEmail(null, USER_EMAIL))
                .thenReturn(Mono.just(sampleUser));
        when(paymentPlanUseCase.calculatePaymentSchedule(any(), any(), any(), any()))
                .thenReturn(Flux.just(createSamplePaymentPlan()));
        when(notificationGateway.notify(any(LoanApplicationNotification.class)))
                .thenReturn(Mono.error(new RuntimeException("Notification failed")));

        StepVerifier.create(updateLoanApplicationStatusUseCase.updateLoanApplicationStatus(command))
                .expectError(RuntimeException.class)
                .verify();
    }

    @Test
    void shouldHandleUserNotFoundInNotification() {
        UpdateLoanApplicationCommand command = new UpdateLoanApplicationCommand(LOAN_ID, 3);
        
        LoanApplication updatedLoanApplication = sampleLoanApplication.toBuilder()
                .status(approvedStatus)
                .build();

        when(loanApplicationRepository.getById(LOAN_ID))
                .thenReturn(Mono.just(sampleLoanApplication));
        when(loanApplicationStatusRepository.getById(3))
                .thenReturn(Mono.just(approvedStatus));
        when(loanApplicationRepository.saveLoanApplication(any(LoanApplication.class)))
                .thenReturn(Mono.just(updatedLoanApplication));
        when(loanApplicationStatusRepository.getByNames(Set.of(
                LoanApplicationStatusEnum.APPROVED.name(), 
                LoanApplicationStatusEnum.REJECTED.name(),
                LoanApplicationStatusEnum.UNDER_REVIEW.name())))
                .thenReturn(Flux.just(approvedStatus, rejectedStatus));
        when(loanTypeRepository.getById(personalLoanType.getId()))
                .thenReturn(Mono.just(personalLoanType));
        when(userGateway.getUserByIdNumberOrEmail(null, USER_EMAIL))
                .thenReturn(Mono.empty());

        StepVerifier.create(updateLoanApplicationStatusUseCase.updateLoanApplicationStatus(command))
                .expectNextMatches(result -> {
                    assertEquals(LOAN_ID, result.getId());
                    assertEquals(3, result.getStatus().getId());
                    return true;
                })
                .verifyComplete();

        verify(notificationGateway, never()).notify(any(LoanApplicationNotification.class));
    }

    @Test
    void shouldHandleLoanTypeNotFoundInNotification() {
        UpdateLoanApplicationCommand command = new UpdateLoanApplicationCommand(LOAN_ID, 3);
        
        LoanApplication updatedLoanApplication = sampleLoanApplication.toBuilder()
                .status(approvedStatus)
                .build();

        when(loanApplicationRepository.getById(LOAN_ID))
                .thenReturn(Mono.just(sampleLoanApplication));
        when(loanApplicationStatusRepository.getById(3))
                .thenReturn(Mono.just(approvedStatus));
        when(loanApplicationRepository.saveLoanApplication(any(LoanApplication.class)))
                .thenReturn(Mono.just(updatedLoanApplication));
        when(loanApplicationStatusRepository.getByNames(Set.of(
                LoanApplicationStatusEnum.APPROVED.name(), 
                LoanApplicationStatusEnum.REJECTED.name(),
                LoanApplicationStatusEnum.UNDER_REVIEW.name())))
                .thenReturn(Flux.just(approvedStatus, rejectedStatus));
        when(loanTypeRepository.getById(personalLoanType.getId()))
                .thenReturn(Mono.empty());

        StepVerifier.create(updateLoanApplicationStatusUseCase.updateLoanApplicationStatus(command))
                .expectNextMatches(result -> {
                    assertEquals(LOAN_ID, result.getId());
                    assertEquals(3, result.getStatus().getId());
                    return true;
                })
                .verifyComplete();

        verify(notificationGateway, never()).notify(any(LoanApplicationNotification.class));
    }

    @Test
    void shouldUpdateAndNotifyWhenValidApprovedResponseFromAutoValidation() {
        ValidationResponse validationResponse = createValidationResponse("APPROVED", null, MONTHLY_PAYMENT);
        UpdateLoanAutomaticValidationCommand command = new UpdateLoanAutomaticValidationCommand(200, validationResponse);

        LoanApplication approvedLoanApplication = sampleLoanApplication.toBuilder()
                .status(approvedStatus)
                .build();

        when(loanApplicationRepository.getById(LOAN_ID))
                .thenReturn(Mono.just(sampleLoanApplication));
        when(loanApplicationStatusRepository.getByName("APPROVED"))
                .thenReturn(Mono.just(approvedStatus));
        when(loanApplicationStatusRepository.getById(approvedStatus.getId()))
                .thenReturn(Mono.just(approvedStatus));
        when(loanApplicationRepository.saveLoanApplication(any(LoanApplication.class)))
                .thenReturn(Mono.just(approvedLoanApplication));
        when(loanApplicationStatusRepository.getByNames(Set.of(
                LoanApplicationStatusEnum.APPROVED.name(), 
                LoanApplicationStatusEnum.REJECTED.name(),
                LoanApplicationStatusEnum.UNDER_REVIEW.name())))
                .thenReturn(Flux.just(approvedStatus, rejectedStatus));
        when(loanTypeRepository.getById(personalLoanType.getId()))
                .thenReturn(Mono.just(personalLoanType));
        when(userGateway.getUserByIdNumberOrEmail(null, USER_EMAIL))
                .thenReturn(Mono.just(sampleUser));
        when(paymentPlanUseCase.calculatePaymentSchedule(any(), any(), any(), any()))
                .thenReturn(Flux.just(createSamplePaymentPlan()));
        when(notificationGateway.notify(any(LoanApplicationNotification.class)))
                .thenReturn(Mono.empty());

        StepVerifier.create(updateLoanApplicationStatusUseCase.updateLoanApplicationStatusFromAutoValidation(command))
                .verifyComplete();

        verify(notificationGateway).notify(any(LoanApplicationNotification.class));
    }

    @Test
    void shouldUpdateAndNotifyWhenValidRejectedResponseFromAutoValidation() {
        ValidationResponse validationResponse = createValidationResponse("REJECTED", REJECTION_REASON, null);
        UpdateLoanAutomaticValidationCommand command = new UpdateLoanAutomaticValidationCommand(200, validationResponse);

        LoanApplication rejectedLoanApplication = sampleLoanApplication.toBuilder()
                .status(rejectedStatus)
                .build();

        when(loanApplicationRepository.getById(LOAN_ID))
                .thenReturn(Mono.just(sampleLoanApplication));
        when(loanApplicationStatusRepository.getByName("REJECTED"))
                .thenReturn(Mono.just(rejectedStatus));
        when(loanApplicationStatusRepository.getById(rejectedStatus.getId()))
                .thenReturn(Mono.just(rejectedStatus));
        when(loanApplicationRepository.saveLoanApplication(any(LoanApplication.class)))
                .thenReturn(Mono.just(rejectedLoanApplication));
        when(loanApplicationStatusRepository.getByNames(Set.of(
                LoanApplicationStatusEnum.APPROVED.name(), 
                LoanApplicationStatusEnum.REJECTED.name(),
                LoanApplicationStatusEnum.UNDER_REVIEW.name())))
                .thenReturn(Flux.just(approvedStatus, rejectedStatus));
        when(loanTypeRepository.getById(personalLoanType.getId()))
                .thenReturn(Mono.just(personalLoanType));
        when(userGateway.getUserByIdNumberOrEmail(null, USER_EMAIL))
                .thenReturn(Mono.just(sampleUser));
        when(notificationGateway.notify(any(LoanApplicationNotification.class)))
                .thenReturn(Mono.empty());

        StepVerifier.create(updateLoanApplicationStatusUseCase.updateLoanApplicationStatusFromAutoValidation(command))
                .verifyComplete();

        verify(notificationGateway).notify(any(LoanApplicationNotification.class));
    }

    @Test
    void shouldThrowExceptionWhenInvalidDecisionFromAutoValidation() {
        ValidationResponse validationResponse = createValidationResponse("INVALID_STATUS", null, null);
        UpdateLoanAutomaticValidationCommand command = new UpdateLoanAutomaticValidationCommand(200, validationResponse);

        StepVerifier.create(updateLoanApplicationStatusUseCase.updateLoanApplicationStatusFromAutoValidation(command))
                .expectErrorMatches(throwable -> 
                    throwable instanceof UpdateLoanApplicationException &&
                    throwable.getMessage().contains("Invalid decision"))
                .verify();
    }

    @Test
    void shouldThrowExceptionWhenLoanNotFoundFromAutoValidation() {
        ValidationResponse validationResponse = createValidationResponse("APPROVED", null, MONTHLY_PAYMENT);
        UpdateLoanAutomaticValidationCommand command = new UpdateLoanAutomaticValidationCommand(200, validationResponse);

        when(loanApplicationRepository.getById(LOAN_ID))
                .thenReturn(Mono.empty());
        when(loanApplicationStatusRepository.getByName("APPROVED"))
                .thenReturn(Mono.just(approvedStatus));

        StepVerifier.create(updateLoanApplicationStatusUseCase.updateLoanApplicationStatusFromAutoValidation(command))
                .expectError(LoanApplicationNotFoundException.class)
                .verify();
    }

    @Test
    void shouldThrowExceptionWhenStatusNotFoundFromAutoValidation() {
        ValidationResponse validationResponse = createValidationResponse("APPROVED", null, MONTHLY_PAYMENT);
        UpdateLoanAutomaticValidationCommand command = new UpdateLoanAutomaticValidationCommand(200, validationResponse);

        when(loanApplicationRepository.getById(LOAN_ID))
                .thenReturn(Mono.just(sampleLoanApplication));
        when(loanApplicationStatusRepository.getByName("APPROVED"))
                .thenReturn(Mono.empty());

        StepVerifier.create(updateLoanApplicationStatusUseCase.updateLoanApplicationStatusFromAutoValidation(command))
                .expectErrorMatches(throwable -> 
                    throwable instanceof LoanApplicationStatusNotFoundException &&
                    throwable.getMessage().contains("APPROVED"))
                .verify();
    }

    @Test
    void shouldThrowExceptionWhenSameStatusFromAutoValidation() {
        LoanApplication approvedLoanApplication = sampleLoanApplication.toBuilder()
                .status(approvedStatus)
                .build();

        ValidationResponse validationResponse = createValidationResponse("APPROVED", null, MONTHLY_PAYMENT);
        UpdateLoanAutomaticValidationCommand command = new UpdateLoanAutomaticValidationCommand(200, validationResponse);

        when(loanApplicationRepository.getById(LOAN_ID))
                .thenReturn(Mono.just(approvedLoanApplication));
        when(loanApplicationStatusRepository.getByName("APPROVED"))
                .thenReturn(Mono.just(approvedStatus));

        StepVerifier.create(updateLoanApplicationStatusUseCase.updateLoanApplicationStatusFromAutoValidation(command))
                .expectErrorMatches(throwable -> 
                    throwable instanceof UpdateLoanApplicationException &&
                    throwable.getMessage().contains("same as the current status"))
                .verify();
    }

    @Test
    void shouldProcessCorrectlyWhenManualReviewDecisionFromAutoValidation() {
        LoanApplicationStatus manualReviewStatus = LoanApplicationStatus.builder()
                .id(5)
                .name("MANUAL_REVIEW")
                .description("Manual review required")
                .build();

        ValidationResponse validationResponse = createValidationResponse("MANUAL_REVIEW", "Requiere revisi\u00f3n manual", null);
        UpdateLoanAutomaticValidationCommand command = new UpdateLoanAutomaticValidationCommand(200, validationResponse);

        LoanApplication manualReviewLoanApplication = sampleLoanApplication.toBuilder()
                .status(manualReviewStatus)
                .build();

        when(loanApplicationRepository.getById(LOAN_ID))
                .thenReturn(Mono.just(sampleLoanApplication));
        when(loanApplicationStatusRepository.getByName("MANUAL_REVIEW"))
                .thenReturn(Mono.just(manualReviewStatus));
        when(loanApplicationStatusRepository.getById(manualReviewStatus.getId()))
                .thenReturn(Mono.just(manualReviewStatus));
        when(loanApplicationRepository.saveLoanApplication(any(LoanApplication.class)))
                .thenReturn(Mono.just(manualReviewLoanApplication));
        when(loanApplicationStatusRepository.getByNames(Set.of(
                LoanApplicationStatusEnum.APPROVED.name(), 
                LoanApplicationStatusEnum.REJECTED.name(),
                LoanApplicationStatusEnum.UNDER_REVIEW.name())))
                .thenReturn(Flux.just(approvedStatus, rejectedStatus));

        StepVerifier.create(updateLoanApplicationStatusUseCase.updateLoanApplicationStatusFromAutoValidation(command))
                .verifyComplete();

        verify(notificationGateway, never()).notify(any(LoanApplicationNotification.class));
    }

    @Test
    void shouldPropagateErrorWhenNotificationFailsFromAutoValidation() {
        ValidationResponse validationResponse = createValidationResponse("APPROVED", null, MONTHLY_PAYMENT);
        UpdateLoanAutomaticValidationCommand command = new UpdateLoanAutomaticValidationCommand(200, validationResponse);

        LoanApplication approvedLoanApplication = sampleLoanApplication.toBuilder()
                .status(approvedStatus)
                .build();

        when(loanApplicationRepository.getById(LOAN_ID))
                .thenReturn(Mono.just(sampleLoanApplication));
        when(loanApplicationStatusRepository.getByName("APPROVED"))
                .thenReturn(Mono.just(approvedStatus));
        when(loanApplicationStatusRepository.getById(approvedStatus.getId()))
                .thenReturn(Mono.just(approvedStatus));
        when(loanApplicationRepository.saveLoanApplication(any(LoanApplication.class)))
                .thenReturn(Mono.just(approvedLoanApplication));
        when(loanApplicationStatusRepository.getByNames(Set.of(
                LoanApplicationStatusEnum.APPROVED.name(), 
                LoanApplicationStatusEnum.REJECTED.name(),
                LoanApplicationStatusEnum.UNDER_REVIEW.name())))
                .thenReturn(Flux.just(approvedStatus, rejectedStatus));
        when(loanTypeRepository.getById(personalLoanType.getId()))
                .thenReturn(Mono.just(personalLoanType));
        when(userGateway.getUserByIdNumberOrEmail(null, USER_EMAIL))
                .thenReturn(Mono.just(sampleUser));
        when(paymentPlanUseCase.calculatePaymentSchedule(any(), any(), any(), any()))
                .thenReturn(Flux.just(createSamplePaymentPlan()));
        when(notificationGateway.notify(any(LoanApplicationNotification.class)))
                .thenReturn(Mono.error(new RuntimeException("Notification service unavailable")));

        StepVerifier.create(updateLoanApplicationStatusUseCase.updateLoanApplicationStatusFromAutoValidation(command))
                .expectError(RuntimeException.class)
                .verify();
    }

    @Test
    void shouldProcessCorrectlyWhenValidationResponseWithoutMonthlyPaymentFromAutoValidation() {
        ValidationResponse validationResponse = createValidationResponse("REJECTED", REJECTION_REASON, null);
        UpdateLoanAutomaticValidationCommand command = new UpdateLoanAutomaticValidationCommand(200, validationResponse);

        LoanApplication rejectedLoanApplication = sampleLoanApplication.toBuilder()
                .status(rejectedStatus)
                .build();

        when(loanApplicationRepository.getById(LOAN_ID))
                .thenReturn(Mono.just(sampleLoanApplication));
        when(loanApplicationStatusRepository.getByName("REJECTED"))
                .thenReturn(Mono.just(rejectedStatus));
        when(loanApplicationStatusRepository.getById(rejectedStatus.getId()))
                .thenReturn(Mono.just(rejectedStatus));
        when(loanApplicationRepository.saveLoanApplication(any(LoanApplication.class)))
                .thenReturn(Mono.just(rejectedLoanApplication));
        when(loanApplicationStatusRepository.getByNames(Set.of(
                LoanApplicationStatusEnum.APPROVED.name(), 
                LoanApplicationStatusEnum.REJECTED.name(),
                LoanApplicationStatusEnum.UNDER_REVIEW.name())))
                .thenReturn(Flux.just(approvedStatus, rejectedStatus));
        when(loanTypeRepository.getById(personalLoanType.getId()))
                .thenReturn(Mono.just(personalLoanType));
        when(userGateway.getUserByIdNumberOrEmail(null, USER_EMAIL))
                .thenReturn(Mono.just(sampleUser));
        when(notificationGateway.notify(any(LoanApplicationNotification.class)))
                .thenReturn(Mono.empty());

        StepVerifier.create(updateLoanApplicationStatusUseCase.updateLoanApplicationStatusFromAutoValidation(command))
                .verifyComplete();

        verify(notificationGateway).notify(any(LoanApplicationNotification.class));
    }

    private ValidationResponse createValidationResponse(String decision, String rejectionReason, BigDecimal monthlyPayment) {
        ValidationAnalysis analysis = new ValidationAnalysis(
                new BigDecimal("5000000.00"),
                new BigDecimal("1750000.00"),
                new BigDecimal("500000.00"),
                new BigDecimal("1250000.00"),
                monthlyPayment,
                decision,
                "Automated analysis based on income and debt"
        );

        return new ValidationResponse(
                LOAN_ID,
                analysis,
                "APPROVED".equals(decision),
                rejectionReason
        );
    }

    private PaymentPlanItem createSamplePaymentPlan() {
        return PaymentPlanItem.builder()
                .paymentNumber(1)
                .dueDate(LocalDate.now().plusMonths(1))
                .principalPayment(new BigDecimal("2000.00"))
                .interestPayment(new BigDecimal("500.00"))
                .totalPayment(MONTHLY_PAYMENT)
                .remainingBalance(new BigDecimal("48000.00"))
                .build();
    }
}