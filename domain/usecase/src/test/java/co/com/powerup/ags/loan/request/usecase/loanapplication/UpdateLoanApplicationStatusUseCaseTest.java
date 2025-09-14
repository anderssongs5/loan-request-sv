package co.com.powerup.ags.loan.request.usecase.loanapplication;

import co.com.powerup.ags.loan.request.model.loanapplication.LoanApplication;
import co.com.powerup.ags.loan.request.model.loanapplication.LoanApplicationWithUser;
import co.com.powerup.ags.loan.request.model.loanapplication.gateways.LoanApplicationRepository;
import co.com.powerup.ags.loan.request.model.loanapplicationstatus.LoanApplicationStatus;
import co.com.powerup.ags.loan.request.model.loanapplicationstatus.LoanApplicationStatusEnum;
import co.com.powerup.ags.loan.request.model.loanapplicationstatus.gateways.LoanApplicationStatusRepository;
import co.com.powerup.ags.loan.request.model.loantype.LoanType;
import co.com.powerup.ags.loan.request.model.loantype.gateways.LoanTypeRepository;
import co.com.powerup.ags.loan.request.model.notification.gateway.NotificationGateway;
import co.com.powerup.ags.loan.request.model.user.User;
import co.com.powerup.ags.loan.request.model.user.gateways.UserGateway;
import co.com.powerup.ags.loan.request.usecase.loanapplication.dto.UpdateLoanApplicationCommand;
import co.com.powerup.ags.loan.request.usecase.loanapplication.exception.LoanApplicationNotFoundException;
import co.com.powerup.ags.loan.request.usecase.loanapplication.exception.LoanApplicationStatusNotFoundException;
import co.com.powerup.ags.loan.request.usecase.loanapplication.exception.UpdateLoanApplicationException;
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

    private UpdateLoanApplicationStatusUseCase updateLoanApplicationStatusUseCase;

    private LoanApplication sampleLoanApplication;
    private LoanApplicationStatus pendingStatus;
    private LoanApplicationStatus approvedStatus;
    private LoanApplicationStatus rejectedStatus;
    private LoanType personalLoanType;
    private User sampleUser;

    private static final String LOAN_ID = "29a4639d-b328-453a-a408-d2eff0bcae84";
    private static final String USER_EMAIL = "user@example.com";
    private static final BigDecimal LOAN_AMOUNT = new BigDecimal("50000.00");
    private static final Integer LOAN_TERM = 24;

    @BeforeEach
    void setUp() {
        updateLoanApplicationStatusUseCase = new UpdateLoanApplicationStatusUseCase(
                loanApplicationRepository,
                loanApplicationStatusRepository,
                loanTypeRepository,
                notificationGateway,
                userGateway
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
                .name("John")
                .lastName("Doe")
                .baseSalary(new BigDecimal("5000.00"))
                .phoneNumber("+57300123456")
                .address("Calle 123 # 45-67, Bogotá")
                .birthDate(LocalDate.of(1985, 6, 15))
                .idNumber("12345678")
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
                LoanApplicationStatusEnum.REJECTED.name())))
                .thenReturn(Flux.just(approvedStatus, rejectedStatus));
        when(loanTypeRepository.getById(personalLoanType.getId()))
                .thenReturn(Mono.just(personalLoanType));
        when(userGateway.getUserByIdNumberOrEmail(null, USER_EMAIL))
                .thenReturn(Mono.just(sampleUser));
        when(notificationGateway.notify(any(LoanApplicationWithUser.class)))
                .thenReturn(Mono.empty());

        StepVerifier.create(updateLoanApplicationStatusUseCase.updateLoanApplicationStatus(command))
                .expectNextMatches(result -> {
                    assertEquals(LOAN_ID, result.getId());
                    assertEquals(3, result.getStatus().getId());
                    return true;
                })
                .verifyComplete();

        verify(notificationGateway).notify(any(LoanApplicationWithUser.class));
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
                LoanApplicationStatusEnum.REJECTED.name())))
                .thenReturn(Flux.just(approvedStatus, rejectedStatus));
        when(loanTypeRepository.getById(personalLoanType.getId()))
                .thenReturn(Mono.just(personalLoanType));
        when(userGateway.getUserByIdNumberOrEmail(null, USER_EMAIL))
                .thenReturn(Mono.just(sampleUser));
        when(notificationGateway.notify(any(LoanApplicationWithUser.class)))
                .thenReturn(Mono.empty());

        StepVerifier.create(updateLoanApplicationStatusUseCase.updateLoanApplicationStatus(command))
                .expectNextMatches(result -> {
                    assertEquals(LOAN_ID, result.getId());
                    assertEquals(4, result.getStatus().getId());
                    return true;
                })
                .verifyComplete();

        verify(notificationGateway).notify(any(LoanApplicationWithUser.class));
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
                LoanApplicationStatusEnum.REJECTED.name())))
                .thenReturn(Flux.just(approvedStatus, rejectedStatus));

        StepVerifier.create(updateLoanApplicationStatusUseCase.updateLoanApplicationStatus(command))
                .expectNextMatches(result -> {
                    assertEquals(LOAN_ID, result.getId());
                    assertEquals(2, result.getStatus().getId());
                    return true;
                })
                .verifyComplete();

        verify(notificationGateway, never()).notify(any(LoanApplicationWithUser.class));
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
        UpdateLoanApplicationCommand command = new UpdateLoanApplicationCommand(LOAN_ID, 1); // Same as current status

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
                LoanApplicationStatusEnum.REJECTED.name())))
                .thenReturn(Flux.just(approvedStatus, rejectedStatus));
        when(loanTypeRepository.getById(personalLoanType.getId()))
                .thenReturn(Mono.just(personalLoanType));
        when(userGateway.getUserByIdNumberOrEmail(null, USER_EMAIL))
                .thenReturn(Mono.just(sampleUser));
        when(notificationGateway.notify(any(LoanApplicationWithUser.class)))
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
                LoanApplicationStatusEnum.REJECTED.name())))
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

        verify(notificationGateway, never()).notify(any(LoanApplicationWithUser.class));
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
                LoanApplicationStatusEnum.REJECTED.name())))
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

        verify(notificationGateway, never()).notify(any(LoanApplicationWithUser.class));
    }
}