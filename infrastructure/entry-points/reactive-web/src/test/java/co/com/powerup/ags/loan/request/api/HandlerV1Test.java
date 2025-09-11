package co.com.powerup.ags.loan.request.api;

import co.com.powerup.ags.loan.request.api.dto.CreateLoanRequestDto;
import co.com.powerup.ags.loan.request.model.loanapplication.LoanApplication;
import co.com.powerup.ags.loan.request.model.loanapplicationstatus.LoanApplicationStatus;
import co.com.powerup.ags.loan.request.model.loantype.LoanType;
import co.com.powerup.ags.loan.request.model.exception.UserServiceException;
import co.com.powerup.ags.loan.request.model.exception.UserValidationException;
import co.com.powerup.ags.loan.request.usecase.loanapplication.LoanApplicationUseCase;
import co.com.powerup.ags.loan.request.usecase.loanapplication.command.CreateLoanRequestCommand;
import co.com.powerup.ags.loan.request.usecase.loanapplication.exception.UserNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.reactive.function.server.MockServerRequest;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.net.URI;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HandlerV1Test {
    
    private static final String CARLOS_EMAIL = "carlos.rodriguez@example.com";
    private static final String MARIA_EMAIL = "maria.fernandez@example.com";
    private static final BigDecimal AMOUNT_50000 = new BigDecimal("50000.00");
    private static final BigDecimal AMOUNT_75000 = new BigDecimal("75000.00");
    private static final Integer TERM_24 = 24;
    private static final Integer TERM_36 = 36;
    private static final String VALID_USER_ID = "12345678";
    private static final String INVALID_USER_ID = "99999999";
    private static final Integer LOAN_TYPE_ID_1 = 1;
    public static final String API_PATH = "/api/v1/loan-requests";
    private static final String CONTENT_TYPE_HEADER = "Content-Type";
    private static final String APPLICATION_JSON_VALUE = "application/json";
    
    @Mock
    private LoanApplicationUseCase loanApplicationUseCase;

    private HandlerV1 handlerV1;
    
    private LoanApplication sampleLoanApplication;
    private LoanApplicationStatus pendingStatus;
    private LoanType personalLoanType;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validatorFactory = new LocalValidatorFactoryBean();
        validatorFactory.afterPropertiesSet();
        handlerV1 = new HandlerV1(loanApplicationUseCase, validatorFactory);

        pendingStatus = LoanApplicationStatus.builder()
                .id(1)
                .name("PENDING")
                .description("Pending review")
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
                .id(UUID.randomUUID().toString())
                .email(CARLOS_EMAIL)
                .amount(AMOUNT_50000)
                .term(TERM_24)
                .status(pendingStatus)
                .loanType(personalLoanType)
                .build();
    }

    @Test
    void shouldListLoanRequestsSuccessfully() {
        LoanApplication secondLoanApplication = LoanApplication.builder()
                .id(UUID.randomUUID().toString())
                .email(MARIA_EMAIL)
                .amount(AMOUNT_75000)
                .term(TERM_36)
                .status(pendingStatus)
                .loanType(personalLoanType)
                .build();

        when(loanApplicationUseCase.getAllLoanRequests())
                .thenReturn(Flux.just(sampleLoanApplication, secondLoanApplication));

        ServerRequest request = MockServerRequest.builder()
                .uri(URI.create(API_PATH))
                .build();

        Mono<ServerResponse> response = handlerV1.listLoanRequests(request);

        StepVerifier.create(response)
                .expectNextMatches(serverResponse -> {
                    assertEquals(200, serverResponse.statusCode().value());
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void shouldReturnEmptyLoanRequestsList() {
        when(loanApplicationUseCase.getAllLoanRequests())
                .thenReturn(Flux.empty());

        ServerRequest request = MockServerRequest.builder()
                .uri(URI.create(API_PATH))
                .build();

        Mono<ServerResponse> response = handlerV1.listLoanRequests(request);

        StepVerifier.create(response)
                .expectNextMatches(serverResponse -> {
                    assertEquals(200, serverResponse.statusCode().value());
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void shouldCreateLoanRequestSuccessfully() {
        CreateLoanRequestDto requestDto = CreateLoanRequestDto.builder()
                .userIdNumber(VALID_USER_ID)
                .loanTypeId(LOAN_TYPE_ID_1)
                .amount(AMOUNT_50000)
                .term(TERM_24)
                .build();

        when(loanApplicationUseCase.createLoanRequest(any(CreateLoanRequestCommand.class)))
                .thenReturn(Mono.just(sampleLoanApplication));

        ServerRequest request = MockServerRequest.builder()
                .uri(URI.create(API_PATH))
                .header(CONTENT_TYPE_HEADER, APPLICATION_JSON_VALUE)
                .body(Mono.just(requestDto));

        Mono<ServerResponse> response = handlerV1.createLoanRequest(request);

        StepVerifier.create(response)
                .expectNextMatches(serverResponse -> {
                    assertEquals(200, serverResponse.statusCode().value());
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void shouldHandleValidationErrorWhenUserIdNumberIsBlank() {
        CreateLoanRequestDto requestDto = CreateLoanRequestDto.builder()
                .userIdNumber("")
                .loanTypeId(LOAN_TYPE_ID_1)
                .amount(AMOUNT_50000)
                .term(TERM_24)
                .build();

        ServerRequest request = MockServerRequest.builder()
                .uri(URI.create(API_PATH))
                .header(CONTENT_TYPE_HEADER, APPLICATION_JSON_VALUE)
                .body(Mono.just(requestDto));

        Mono<ServerResponse> response = handlerV1.createLoanRequest(request);

        StepVerifier.create(response)
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    void shouldHandleValidationErrorWhenUserIdNumberIsNull() {
        CreateLoanRequestDto requestDto = CreateLoanRequestDto.builder()
                .userIdNumber(null)
                .loanTypeId(LOAN_TYPE_ID_1)
                .amount(AMOUNT_50000)
                .term(TERM_24)
                .build();

        ServerRequest request = MockServerRequest.builder()
                .uri(URI.create(API_PATH))
                .header(CONTENT_TYPE_HEADER, APPLICATION_JSON_VALUE)
                .body(Mono.just(requestDto));

        Mono<ServerResponse> response = handlerV1.createLoanRequest(request);

        StepVerifier.create(response)
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    void shouldHandleValidationErrorWhenLoanTypeIdIsNull() {
        CreateLoanRequestDto requestDto = CreateLoanRequestDto.builder()
                .userIdNumber(VALID_USER_ID)
                .loanTypeId(null)
                .amount(AMOUNT_50000)
                .term(TERM_24)
                .build();

        ServerRequest request = MockServerRequest.builder()
                .uri(URI.create(API_PATH))
                .header(CONTENT_TYPE_HEADER, APPLICATION_JSON_VALUE)
                .body(Mono.just(requestDto));

        Mono<ServerResponse> response = handlerV1.createLoanRequest(request);

        StepVerifier.create(response)
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    void shouldHandleValidationErrorWhenLoanTypeIdIsZero() {
        CreateLoanRequestDto requestDto = CreateLoanRequestDto.builder()
                .userIdNumber(VALID_USER_ID)
                .loanTypeId(0)
                .amount(AMOUNT_50000)
                .term(TERM_24)
                .build();

        ServerRequest request = MockServerRequest.builder()
                .uri(URI.create(API_PATH))
                .header(CONTENT_TYPE_HEADER, APPLICATION_JSON_VALUE)
                .body(Mono.just(requestDto));

        Mono<ServerResponse> response = handlerV1.createLoanRequest(request);

        StepVerifier.create(response)
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    void shouldHandleValidationErrorWhenAmountIsNull() {
        CreateLoanRequestDto requestDto = CreateLoanRequestDto.builder()
                .userIdNumber(VALID_USER_ID)
                .loanTypeId(LOAN_TYPE_ID_1)
                .amount(null)
                .term(TERM_24)
                .build();

        ServerRequest request = MockServerRequest.builder()
                .uri(URI.create(API_PATH))
                .header(CONTENT_TYPE_HEADER, APPLICATION_JSON_VALUE)
                .body(Mono.just(requestDto));

        Mono<ServerResponse> response = handlerV1.createLoanRequest(request);

        StepVerifier.create(response)
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    void shouldHandleValidationErrorWhenAmountIsZero() {
        CreateLoanRequestDto requestDto = CreateLoanRequestDto.builder()
                .userIdNumber(VALID_USER_ID)
                .loanTypeId(LOAN_TYPE_ID_1)
                .amount(BigDecimal.ZERO)
                .term(TERM_24)
                .build();

        ServerRequest request = MockServerRequest.builder()
                .uri(URI.create(API_PATH))
                .header(CONTENT_TYPE_HEADER, APPLICATION_JSON_VALUE)
                .body(Mono.just(requestDto));

        Mono<ServerResponse> response = handlerV1.createLoanRequest(request);

        StepVerifier.create(response)
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    void shouldHandleValidationErrorWhenTermIsNull() {
        CreateLoanRequestDto requestDto = CreateLoanRequestDto.builder()
                .userIdNumber(VALID_USER_ID)
                .loanTypeId(LOAN_TYPE_ID_1)
                .amount(AMOUNT_50000)
                .term(null)
                .build();

        ServerRequest request = MockServerRequest.builder()
                .uri(URI.create(API_PATH))
                .header(CONTENT_TYPE_HEADER, APPLICATION_JSON_VALUE)
                .body(Mono.just(requestDto));

        Mono<ServerResponse> response = handlerV1.createLoanRequest(request);

        StepVerifier.create(response)
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    void shouldHandleValidationErrorWhenTermIsZero() {
        CreateLoanRequestDto requestDto = CreateLoanRequestDto.builder()
                .userIdNumber(VALID_USER_ID)
                .loanTypeId(LOAN_TYPE_ID_1)
                .amount(AMOUNT_50000)
                .term(0)
                .build();

        ServerRequest request = MockServerRequest.builder()
                .uri(URI.create(API_PATH))
                .header(CONTENT_TYPE_HEADER, APPLICATION_JSON_VALUE)
                .body(Mono.just(requestDto));

        Mono<ServerResponse> response = handlerV1.createLoanRequest(request);

        StepVerifier.create(response)
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    void shouldHandleMultipleValidationErrors() {
        CreateLoanRequestDto requestDto = CreateLoanRequestDto.builder()
                .userIdNumber("")
                .loanTypeId(0)
                .amount(BigDecimal.ZERO)
                .term(0)
                .build();

        ServerRequest request = MockServerRequest.builder()
                .uri(URI.create(API_PATH))
                .header(CONTENT_TYPE_HEADER, APPLICATION_JSON_VALUE)
                .body(Mono.just(requestDto));

        Mono<ServerResponse> response = handlerV1.createLoanRequest(request);

        StepVerifier.create(response)
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    void shouldPropagateUserNotFoundExceptionFromUseCase() {
        CreateLoanRequestDto requestDto = CreateLoanRequestDto.builder()
                .userIdNumber(INVALID_USER_ID)
                .loanTypeId(LOAN_TYPE_ID_1)
                .amount(AMOUNT_50000)
                .term(TERM_24)
                .build();

        when(loanApplicationUseCase.createLoanRequest(any(CreateLoanRequestCommand.class)))
                .thenReturn(Mono.error(new UserNotFoundException("User with ID number " + INVALID_USER_ID + " does not exist")));

        ServerRequest request = MockServerRequest.builder()
                .uri(URI.create(API_PATH))
                .header(CONTENT_TYPE_HEADER, APPLICATION_JSON_VALUE)
                .body(Mono.just(requestDto));

        Mono<ServerResponse> response = handlerV1.createLoanRequest(request);

        StepVerifier.create(response)
                .expectError(UserNotFoundException.class)
                .verify();
    }

    @Test
    void shouldPropagateUserServiceExceptionFromUseCase() {
        CreateLoanRequestDto requestDto = CreateLoanRequestDto.builder()
                .userIdNumber(VALID_USER_ID)
                .loanTypeId(LOAN_TYPE_ID_1)
                .amount(AMOUNT_50000)
                .term(TERM_24)
                .build();

        when(loanApplicationUseCase.createLoanRequest(any(CreateLoanRequestCommand.class)))
                .thenReturn(Mono.error(new UserServiceException("User service unavailable")));

        ServerRequest request = MockServerRequest.builder()
                .uri(URI.create(API_PATH))
                .header(CONTENT_TYPE_HEADER, APPLICATION_JSON_VALUE)
                .body(Mono.just(requestDto));

        Mono<ServerResponse> response = handlerV1.createLoanRequest(request);

        StepVerifier.create(response)
                .expectError(UserServiceException.class)
                .verify();
    }

    @Test
    void shouldPropagateUserValidationExceptionFromUseCase() {
        CreateLoanRequestDto requestDto = CreateLoanRequestDto.builder()
                .userIdNumber(VALID_USER_ID)
                .loanTypeId(LOAN_TYPE_ID_1)
                .amount(AMOUNT_50000)
                .term(TERM_24)
                .build();

        when(loanApplicationUseCase.createLoanRequest(any(CreateLoanRequestCommand.class)))
                .thenReturn(Mono.error(new UserValidationException("User does not meet eligibility criteria")));

        ServerRequest request = MockServerRequest.builder()
                .uri(URI.create(API_PATH))
                .header(CONTENT_TYPE_HEADER, APPLICATION_JSON_VALUE)
                .body(Mono.just(requestDto));

        Mono<ServerResponse> response = handlerV1.createLoanRequest(request);

        StepVerifier.create(response)
                .expectError(UserValidationException.class)
                .verify();
    }

    @Test
    void shouldPropagateUnexpectedExceptionFromUseCase() {
        CreateLoanRequestDto requestDto = CreateLoanRequestDto.builder()
                .userIdNumber(VALID_USER_ID)
                .loanTypeId(LOAN_TYPE_ID_1)
                .amount(AMOUNT_50000)
                .term(TERM_24)
                .build();

        when(loanApplicationUseCase.createLoanRequest(any(CreateLoanRequestCommand.class)))
                .thenReturn(Mono.error(new RuntimeException("Unexpected error")));

        ServerRequest request = MockServerRequest.builder()
                .uri(URI.create(API_PATH))
                .header(CONTENT_TYPE_HEADER, APPLICATION_JSON_VALUE)
                .body(Mono.just(requestDto));

        Mono<ServerResponse> response = handlerV1.createLoanRequest(request);

        StepVerifier.create(response)
                .expectError(RuntimeException.class)
                .verify();
    }
}