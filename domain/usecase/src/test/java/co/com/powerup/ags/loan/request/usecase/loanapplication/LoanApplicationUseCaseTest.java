package co.com.powerup.ags.loan.request.usecase.loanapplication;

import co.com.powerup.ags.loan.request.model.exception.UserServiceException;
import co.com.powerup.ags.loan.request.model.exception.UserValidationException;
import co.com.powerup.ags.loan.request.model.loanapplication.LoanApplication;
import co.com.powerup.ags.loan.request.model.loanapplication.gateways.LoanApplicationRepository;
import co.com.powerup.ags.loan.request.model.loanapplicationstatus.LoanApplicationStatus;
import co.com.powerup.ags.loan.request.model.loanapplicationstatus.gateways.LoanApplicationStatusRepository;
import co.com.powerup.ags.loan.request.model.loantype.LoanType;
import co.com.powerup.ags.loan.request.model.loantype.gateways.LoanTypeRepository;
import co.com.powerup.ags.loan.request.model.user.User;
import co.com.powerup.ags.loan.request.model.user.gateways.UserGateway;
import co.com.powerup.ags.loan.request.usecase.loanapplication.command.CreateLoanRequestCommand;
import co.com.powerup.ags.loan.request.usecase.loanapplication.exception.FieldValidationException;
import co.com.powerup.ags.loan.request.usecase.loanapplication.exception.LoanApplicationStatusNotFoundException;
import co.com.powerup.ags.loan.request.usecase.loanapplication.exception.LoanTypeNotFoundException;
import co.com.powerup.ags.loan.request.usecase.loanapplication.exception.UserNotFoundException;
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
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoanApplicationUseCaseTest {

    private static final String CARLOS_EMAIL = "carlos.rodriguez@ejemplo.com";
    private static final String MARIA_EMAIL = "maria.fernandez@ejemplo.com";
    private static final BigDecimal AMOUNT_50000 = new BigDecimal("50000.00");
    private static final BigDecimal AMOUNT_75000 = new BigDecimal("75000.00");
    private static final BigDecimal AMOUNT_500 = new BigDecimal("500.00");
    private static final BigDecimal AMOUNT_150000 = new BigDecimal("150000.00");
    private static final Integer TERM_24 = 24;
    private static final Integer TERM_36 = 36;
    private static final Integer TERM_3 = 3;
    private static final Integer TERM_80 = 80;
    private static final String VALID_USER_ID = "12345678";
    private static final String INVALID_USER_ID = "99999999";
    private static final String CARLOS_NAME = "Carlos";
    private static final String CARLOS_LASTNAME = "Rodriguez";
    private static final String MARIA_NAME = "Maria";
    private static final String MARIA_LASTNAME = "Fernandez";
    private static final Integer LOAN_TYPE_ID_1 = 1;
    private static final Integer INVALID_LOAN_TYPE_ID = 999;
    private static final Integer STATUS_ID_1 = 1;
    private static final String PENDING_STATUS = "PENDING";
    private static final String PERSONAL_LOAN = "Personal Loan";
    private static final String PENDING_DESCRIPTION = "Pending review";
    private static final BigDecimal MIN_AMOUNT = new BigDecimal("1000.00");
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("100000.00");
    private static final Integer MIN_TERM = 6;
    private static final Integer MAX_TERM = 60;
    private static final BigDecimal INTEREST_RATE = new BigDecimal("12.5");
    private static final BigDecimal BASE_SALARY = new BigDecimal("5000.00");
    private static final String ADDRESS = "Calle 123, Bogota";
    private static final String PHONE_NUMBER = "3001234567";

    @Mock
    private LoanApplicationRepository loanApplicationRepository;

    @Mock
    private LoanTypeRepository loanTypeRepository;

    @Mock
    private LoanApplicationStatusRepository loanApplicationStatusRepository;

    @Mock
    private UserGateway userGateway;

    private LoanApplicationUseCase useCase;

    private User sampleUser;
    private LoanType sampleLoanType;
    private LoanApplicationStatus pendingStatus;
    private LoanApplication sampleLoanApplication;
    private CreateLoanRequestCommand validCommand;

    @BeforeEach
    void setUp() {
        useCase = new LoanApplicationUseCase(
                loanApplicationRepository,
                loanTypeRepository,
                loanApplicationStatusRepository,
                userGateway
        );

        sampleUser = User.builder()
                .id(UUID.randomUUID().toString())
                .name(CARLOS_NAME)
                .lastName(CARLOS_LASTNAME)
                .email(CARLOS_EMAIL)
                .idNumber(VALID_USER_ID)
                .baseSalary(BASE_SALARY)
                .address(ADDRESS)
                .phoneNumber(PHONE_NUMBER)
                .birthDate(LocalDate.of(1990, 1, 1))
                .build();

        sampleLoanType = LoanType.builder()
                .id(LOAN_TYPE_ID_1)
                .name(PERSONAL_LOAN)
                .minAmount(MIN_AMOUNT)
                .maxAmount(MAX_AMOUNT)
                .minTerm(MIN_TERM)
                .maxTerm(MAX_TERM)
                .interestRate(INTEREST_RATE)
                .automaticValidation(true)
                .build();

        pendingStatus = LoanApplicationStatus.builder()
                .id(STATUS_ID_1)
                .name(PENDING_STATUS)
                .description(PENDING_DESCRIPTION)
                .build();

        sampleLoanApplication = LoanApplication.builder()
                .id(UUID.randomUUID().toString())
                .email(CARLOS_EMAIL)
                .amount(AMOUNT_50000)
                .term(TERM_24)
                .status(pendingStatus)
                .loanType(sampleLoanType)
                .build();

        validCommand = CreateLoanRequestCommand.builder()
                .userIdNumber(VALID_USER_ID)
                .loanTypeId(LOAN_TYPE_ID_1)
                .amount(AMOUNT_50000)
                .term(TERM_24)
                .build();
    }

    @Test
    void shouldGetAllLoanRequestsSuccessfully() {
        LoanApplication secondLoanApplication = LoanApplication.builder()
                .id(UUID.randomUUID().toString())
                .email(MARIA_EMAIL)
                .amount(AMOUNT_75000)
                .term(TERM_36)
                .status(pendingStatus)
                .loanType(sampleLoanType)
                .build();

        when(loanApplicationRepository.getAllLoanRequests())
                .thenReturn(Flux.just(sampleLoanApplication, secondLoanApplication));

        Flux<LoanApplication> result = useCase.getAllLoanRequests();

        StepVerifier.create(result)
                .expectNextMatches(loanApp -> 
                    loanApp.getEmail().equals(CARLOS_EMAIL) &&
                    loanApp.getAmount().equals(AMOUNT_50000)
                )
                .expectNextMatches(loanApp -> 
                    loanApp.getEmail().equals(MARIA_EMAIL) &&
                    loanApp.getAmount().equals(AMOUNT_75000)
                )
                .verifyComplete();
    }

    @Test
    void shouldReturnEmptyFluxWhenNoLoanRequestsExist() {
        when(loanApplicationRepository.getAllLoanRequests())
                .thenReturn(Flux.empty());

        Flux<LoanApplication> result = useCase.getAllLoanRequests();

        StepVerifier.create(result)
                .verifyComplete();
    }

    @Test
    void shouldCreateLoanRequestSuccessfully() {
        when(loanTypeRepository.getById(LOAN_TYPE_ID_1))
                .thenReturn(Mono.just(sampleLoanType));
        when(userGateway.getUserByIdNumber(VALID_USER_ID))
                .thenReturn(Mono.just(sampleUser));
        when(loanApplicationStatusRepository.getByName(PENDING_STATUS))
                .thenReturn(Mono.just(pendingStatus));
        when(loanApplicationRepository.createLoanRequest(any(LoanApplication.class)))
                .thenReturn(Mono.just(sampleLoanApplication));

        Mono<LoanApplication> result = useCase.createLoanRequest(validCommand);

        StepVerifier.create(result)
                .expectNextMatches(loanApp -> 
                    loanApp.getEmail().equals(CARLOS_EMAIL) &&
                    loanApp.getAmount().equals(AMOUNT_50000) &&
                    loanApp.getTerm().equals(TERM_24)
                )
                .verifyComplete();
    }

    @Test
    void shouldThrowLoanTypeNotFoundExceptionWhenLoanTypeDoesNotExist() {
        CreateLoanRequestCommand command = CreateLoanRequestCommand.builder()
                .userIdNumber(VALID_USER_ID)
                .loanTypeId(INVALID_LOAN_TYPE_ID)
                .amount(AMOUNT_50000)
                .term(TERM_24)
                .build();

        when(loanTypeRepository.getById(INVALID_LOAN_TYPE_ID))
                .thenReturn(Mono.empty());

        Mono<LoanApplication> result = useCase.createLoanRequest(command);

        StepVerifier.create(result)
                .expectError(LoanTypeNotFoundException.class)
                .verify();
    }

    @Test
    void shouldThrowFieldValidationExceptionWhenAmountIsBelowMinimum() {
        CreateLoanRequestCommand command = CreateLoanRequestCommand.builder()
                .userIdNumber(VALID_USER_ID)
                .loanTypeId(LOAN_TYPE_ID_1)
                .amount(AMOUNT_500)
                .term(TERM_24)
                .build();

        when(loanTypeRepository.getById(LOAN_TYPE_ID_1))
                .thenReturn(Mono.just(sampleLoanType));
        when(userGateway.getUserByIdNumber(VALID_USER_ID))
                .thenReturn(Mono.just(sampleUser));
        when(loanApplicationStatusRepository.getByName(PENDING_STATUS))
                .thenReturn(Mono.just(pendingStatus));

        Mono<LoanApplication> result = useCase.createLoanRequest(command);

        StepVerifier.create(result)
                .expectError(FieldValidationException.class)
                .verify();
    }

    @Test
    void shouldThrowFieldValidationExceptionWhenAmountIsAboveMaximum() {
        CreateLoanRequestCommand command = CreateLoanRequestCommand.builder()
                .userIdNumber(VALID_USER_ID)
                .loanTypeId(LOAN_TYPE_ID_1)
                .amount(AMOUNT_150000)
                .term(TERM_24)
                .build();

        when(loanTypeRepository.getById(LOAN_TYPE_ID_1))
                .thenReturn(Mono.just(sampleLoanType));
        when(userGateway.getUserByIdNumber(VALID_USER_ID))
                .thenReturn(Mono.just(sampleUser));
        when(loanApplicationStatusRepository.getByName(PENDING_STATUS))
                .thenReturn(Mono.just(pendingStatus));

        Mono<LoanApplication> result = useCase.createLoanRequest(command);

        StepVerifier.create(result)
                .expectError(FieldValidationException.class)
                .verify();
    }

    @Test
    void shouldThrowFieldValidationExceptionWhenTermIsBelowMinimum() {
        CreateLoanRequestCommand command = CreateLoanRequestCommand.builder()
                .userIdNumber(VALID_USER_ID)
                .loanTypeId(LOAN_TYPE_ID_1)
                .amount(AMOUNT_50000)
                .term(TERM_3)
                .build();

        when(loanTypeRepository.getById(LOAN_TYPE_ID_1))
                .thenReturn(Mono.just(sampleLoanType));
        when(userGateway.getUserByIdNumber(VALID_USER_ID))
                .thenReturn(Mono.just(sampleUser));
        when(loanApplicationStatusRepository.getByName(PENDING_STATUS))
                .thenReturn(Mono.just(pendingStatus));

        Mono<LoanApplication> result = useCase.createLoanRequest(command);

        StepVerifier.create(result)
                .expectError(FieldValidationException.class)
                .verify();
    }

    @Test
    void shouldThrowFieldValidationExceptionWhenTermIsAboveMaximum() {
        CreateLoanRequestCommand command = CreateLoanRequestCommand.builder()
                .userIdNumber(VALID_USER_ID)
                .loanTypeId(LOAN_TYPE_ID_1)
                .amount(AMOUNT_50000)
                .term(TERM_80)
                .build();

        when(loanTypeRepository.getById(LOAN_TYPE_ID_1))
                .thenReturn(Mono.just(sampleLoanType));
        when(userGateway.getUserByIdNumber(VALID_USER_ID))
                .thenReturn(Mono.just(sampleUser));
        when(loanApplicationStatusRepository.getByName(PENDING_STATUS))
                .thenReturn(Mono.just(pendingStatus));

        Mono<LoanApplication> result = useCase.createLoanRequest(command);

        StepVerifier.create(result)
                .expectError(FieldValidationException.class)
                .verify();
    }

    @Test
    void shouldThrowUserNotFoundExceptionWhenUserDoesNotExist() {
        CreateLoanRequestCommand command = CreateLoanRequestCommand.builder()
                .userIdNumber(INVALID_USER_ID)
                .loanTypeId(LOAN_TYPE_ID_1)
                .amount(AMOUNT_50000)
                .term(TERM_24)
                .build();

        when(loanTypeRepository.getById(LOAN_TYPE_ID_1))
                .thenReturn(Mono.just(sampleLoanType));
        when(userGateway.getUserByIdNumber(INVALID_USER_ID))
                .thenReturn(Mono.empty());
        when(loanApplicationStatusRepository.getByName(PENDING_STATUS))
                .thenReturn(Mono.just(pendingStatus));

        Mono<LoanApplication> result = useCase.createLoanRequest(command);

        StepVerifier.create(result)
                .expectError(UserNotFoundException.class)
                .verify();
    }

    @Test
    void shouldThrowUserServiceExceptionWhenUserGatewayFails() {
        when(loanTypeRepository.getById(LOAN_TYPE_ID_1))
                .thenReturn(Mono.just(sampleLoanType));
        when(userGateway.getUserByIdNumber(VALID_USER_ID))
                .thenReturn(Mono.error(new RuntimeException("Connection timeout")));
        when(loanApplicationStatusRepository.getByName(PENDING_STATUS))
                .thenReturn(Mono.just(pendingStatus));

        Mono<LoanApplication> result = useCase.createLoanRequest(validCommand);

        StepVerifier.create(result)
                .expectError(UserServiceException.class)
                .verify();
    }

    @Test
    void shouldPropagateUserValidationExceptionFromUserGateway() {
        when(loanTypeRepository.getById(LOAN_TYPE_ID_1))
                .thenReturn(Mono.just(sampleLoanType));
        when(userGateway.getUserByIdNumber(VALID_USER_ID))
                .thenReturn(Mono.error(new UserValidationException("User does not meet eligibility criteria")));
        when(loanApplicationStatusRepository.getByName(PENDING_STATUS))
                .thenReturn(Mono.just(pendingStatus));

        Mono<LoanApplication> result = useCase.createLoanRequest(validCommand);

        StepVerifier.create(result)
                .expectError(UserValidationException.class)
                .verify();
    }

    @Test
    void shouldThrowLoanApplicationStatusNotFoundExceptionWhenStatusDoesNotExist() {
        when(loanTypeRepository.getById(LOAN_TYPE_ID_1))
                .thenReturn(Mono.just(sampleLoanType));
        when(userGateway.getUserByIdNumber(VALID_USER_ID))
                .thenReturn(Mono.just(sampleUser));
        when(loanApplicationStatusRepository.getByName(PENDING_STATUS))
                .thenReturn(Mono.empty());

        Mono<LoanApplication> result = useCase.createLoanRequest(validCommand);

        StepVerifier.create(result)
                .expectError(LoanApplicationStatusNotFoundException.class)
                .verify();
    }

    @Test
    void shouldHandleRepositoryErrorWhenCreatingLoanRequest() {
        when(loanTypeRepository.getById(LOAN_TYPE_ID_1))
                .thenReturn(Mono.just(sampleLoanType));
        when(userGateway.getUserByIdNumber(VALID_USER_ID))
                .thenReturn(Mono.just(sampleUser));
        when(loanApplicationStatusRepository.getByName(PENDING_STATUS))
                .thenReturn(Mono.just(pendingStatus));
        when(loanApplicationRepository.createLoanRequest(any(LoanApplication.class)))
                .thenReturn(Mono.error(new RuntimeException("Database error")));

        Mono<LoanApplication> result = useCase.createLoanRequest(validCommand);

        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();
    }

    @Test
    void shouldCreateLoanRequestWithDifferentUserData() {
        User mariaUser = User.builder()
                .id(UUID.randomUUID().toString())
                .name(MARIA_NAME)
                .lastName(MARIA_LASTNAME)
                .email(MARIA_EMAIL)
                .idNumber("87654321")
                .baseSalary(new BigDecimal("6000.00"))
                .address("Carrera 456, Medellin")
                .phoneNumber("3009876543")
                .birthDate(LocalDate.of(1985, 5, 15))
                .build();

        LoanApplication mariaLoanApplication = LoanApplication.builder()
                .id(UUID.randomUUID().toString())
                .email(MARIA_EMAIL)
                .amount(AMOUNT_75000)
                .term(TERM_36)
                .status(pendingStatus)
                .loanType(sampleLoanType)
                .build();

        CreateLoanRequestCommand mariaCommand = CreateLoanRequestCommand.builder()
                .userIdNumber("87654321")
                .loanTypeId(LOAN_TYPE_ID_1)
                .amount(AMOUNT_75000)
                .term(TERM_36)
                .build();

        when(loanTypeRepository.getById(LOAN_TYPE_ID_1))
                .thenReturn(Mono.just(sampleLoanType));
        when(userGateway.getUserByIdNumber("87654321"))
                .thenReturn(Mono.just(mariaUser));
        when(loanApplicationStatusRepository.getByName(PENDING_STATUS))
                .thenReturn(Mono.just(pendingStatus));
        when(loanApplicationRepository.createLoanRequest(any(LoanApplication.class)))
                .thenReturn(Mono.just(mariaLoanApplication));

        Mono<LoanApplication> result = useCase.createLoanRequest(mariaCommand);

        StepVerifier.create(result)
                .expectNextMatches(loanApp -> 
                    loanApp.getEmail().equals(MARIA_EMAIL) &&
                    loanApp.getAmount().equals(AMOUNT_75000) &&
                    loanApp.getTerm().equals(TERM_36)
                )
                .verifyComplete();
    }

    @Test
    void shouldHandleGetAllLoanRequestsError() {
        when(loanApplicationRepository.getAllLoanRequests())
                .thenReturn(Flux.error(new RuntimeException("Database connection failed")));

        Flux<LoanApplication> result = useCase.getAllLoanRequests();

        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();
    }

    @Test
    void shouldValidateAmountAtExactMinimumBoundary() {
        CreateLoanRequestCommand command = CreateLoanRequestCommand.builder()
                .userIdNumber(VALID_USER_ID)
                .loanTypeId(LOAN_TYPE_ID_1)
                .amount(MIN_AMOUNT)
                .term(TERM_24)
                .build();

        when(loanTypeRepository.getById(LOAN_TYPE_ID_1))
                .thenReturn(Mono.just(sampleLoanType));
        when(userGateway.getUserByIdNumber(VALID_USER_ID))
                .thenReturn(Mono.just(sampleUser));
        when(loanApplicationStatusRepository.getByName(PENDING_STATUS))
                .thenReturn(Mono.just(pendingStatus));
        when(loanApplicationRepository.createLoanRequest(any(LoanApplication.class)))
                .thenReturn(Mono.just(sampleLoanApplication));

        Mono<LoanApplication> result = useCase.createLoanRequest(command);

        StepVerifier.create(result)
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    void shouldValidateAmountAtExactMaximumBoundary() {
        CreateLoanRequestCommand command = CreateLoanRequestCommand.builder()
                .userIdNumber(VALID_USER_ID)
                .loanTypeId(LOAN_TYPE_ID_1)
                .amount(MAX_AMOUNT)
                .term(TERM_24)
                .build();

        when(loanTypeRepository.getById(LOAN_TYPE_ID_1))
                .thenReturn(Mono.just(sampleLoanType));
        when(userGateway.getUserByIdNumber(VALID_USER_ID))
                .thenReturn(Mono.just(sampleUser));
        when(loanApplicationStatusRepository.getByName(PENDING_STATUS))
                .thenReturn(Mono.just(pendingStatus));
        when(loanApplicationRepository.createLoanRequest(any(LoanApplication.class)))
                .thenReturn(Mono.just(sampleLoanApplication));

        Mono<LoanApplication> result = useCase.createLoanRequest(command);

        StepVerifier.create(result)
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    void shouldValidateTermAtExactMinimumBoundary() {
        CreateLoanRequestCommand command = CreateLoanRequestCommand.builder()
                .userIdNumber(VALID_USER_ID)
                .loanTypeId(LOAN_TYPE_ID_1)
                .amount(AMOUNT_50000)
                .term(MIN_TERM)
                .build();

        when(loanTypeRepository.getById(LOAN_TYPE_ID_1))
                .thenReturn(Mono.just(sampleLoanType));
        when(userGateway.getUserByIdNumber(VALID_USER_ID))
                .thenReturn(Mono.just(sampleUser));
        when(loanApplicationStatusRepository.getByName(PENDING_STATUS))
                .thenReturn(Mono.just(pendingStatus));
        when(loanApplicationRepository.createLoanRequest(any(LoanApplication.class)))
                .thenReturn(Mono.just(sampleLoanApplication));

        Mono<LoanApplication> result = useCase.createLoanRequest(command);

        StepVerifier.create(result)
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    void shouldValidateTermAtExactMaximumBoundary() {
        CreateLoanRequestCommand command = CreateLoanRequestCommand.builder()
                .userIdNumber(VALID_USER_ID)
                .loanTypeId(LOAN_TYPE_ID_1)
                .amount(AMOUNT_50000)
                .term(MAX_TERM)
                .build();

        when(loanTypeRepository.getById(LOAN_TYPE_ID_1))
                .thenReturn(Mono.just(sampleLoanType));
        when(userGateway.getUserByIdNumber(VALID_USER_ID))
                .thenReturn(Mono.just(sampleUser));
        when(loanApplicationStatusRepository.getByName(PENDING_STATUS))
                .thenReturn(Mono.just(pendingStatus));
        when(loanApplicationRepository.createLoanRequest(any(LoanApplication.class)))
                .thenReturn(Mono.just(sampleLoanApplication));

        Mono<LoanApplication> result = useCase.createLoanRequest(command);

        StepVerifier.create(result)
                .expectNextCount(1)
                .verifyComplete();
    }
}