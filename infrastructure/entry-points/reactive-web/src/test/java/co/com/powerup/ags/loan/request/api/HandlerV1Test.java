package co.com.powerup.ags.loan.request.api;

import co.com.powerup.ags.loan.request.api.dto.CreateLoanRequestDto;
import co.com.powerup.ags.loan.request.api.dto.UpdateLoanRequestDto;
import co.com.powerup.ags.loan.request.model.common.PagedResponse;
import co.com.powerup.ags.loan.request.model.loanapplication.LoanApplication;
import co.com.powerup.ags.loan.request.usecase.loanapplication.dto.LoanRequestRequiringReview;
import co.com.powerup.ags.loan.request.model.loanapplicationstatus.LoanApplicationStatus;
import co.com.powerup.ags.loan.request.model.loantype.LoanType;
import co.com.powerup.ags.loan.request.model.user.User;
import co.com.powerup.ags.loan.request.model.exception.UserServiceException;
import co.com.powerup.ags.loan.request.model.exception.UserValidationException;
import co.com.powerup.ags.loan.request.usecase.loanapplication.LoanApplicationUseCase;
import co.com.powerup.ags.loan.request.usecase.loanapplication.UpdateLoanApplicationStatusUseCase;
import co.com.powerup.ags.loan.request.usecase.loanapplication.dto.CreateLoanRequestCommand;
import co.com.powerup.ags.loan.request.usecase.loanapplication.dto.GetLoanApplicationsByStatusesCommand;
import co.com.powerup.ags.loan.request.usecase.loanapplication.dto.UpdateLoanApplicationCommand;
import co.com.powerup.ags.loan.request.usecase.common.exception.LoanApplicationNotFoundException;
import co.com.powerup.ags.loan.request.usecase.common.exception.LoanApplicationStatusNotFoundException;
import co.com.powerup.ags.loan.request.usecase.common.exception.UpdateLoanApplicationException;
import co.com.powerup.ags.loan.request.usecase.common.exception.UserNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.reactive.function.server.MockServerRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.util.context.Context;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HandlerV1Test {
    
    private static final String JOSE_EMAIL = "jose.garcia@example.com";
    private static final String MARIA_EMAIL = "maria.rodriguez@example.com";
    private static final String CARLOS_EMAIL = "carlos.lopez@example.com";
    private static final BigDecimal AMOUNT_50000 = new BigDecimal("50000.00");
    private static final BigDecimal AMOUNT_75000 = new BigDecimal("75000.00");
    private static final BigDecimal SALARY_5000 = new BigDecimal("5000.00");
    private static final BigDecimal SALARY_7500 = new BigDecimal("7500.00");
    private static final Integer TERM_24 = 24;
    private static final Integer TERM_36 = 36;
    private static final String VALID_USER_ID = "12345678";
    private static final String INVALID_USER_ID = "99999999";
    private static final Integer LOAN_TYPE_ID_1 = 1;
    public static final String API_PATH = "/api/v1/loan-requests";
    private static final String CONTENT_TYPE_HEADER = "Content-Type";
    private static final String APPLICATION_JSON_VALUE = "application/json";
    private static final String TEST_USERNAME = "testuser@example.com";
    
    @Mock
    private LoanApplicationUseCase loanApplicationUseCase;

    @Mock
    private UpdateLoanApplicationStatusUseCase updateLoanApplicationStatusUseCase;

    private HandlerV1 handlerV1;
    
    private LoanApplication sampleLoanApplication;
    private LoanApplicationStatus pendingStatus;
    private LoanType personalLoanType;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validatorFactory = new LocalValidatorFactoryBean();
        validatorFactory.afterPropertiesSet();
        handlerV1 = new HandlerV1(loanApplicationUseCase, updateLoanApplicationStatusUseCase, validatorFactory);

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
    
    private Context getSecurityContext() {
        SecurityContext securityContext = new SecurityContextImpl();
        UsernamePasswordAuthenticationToken authentication = 
            new UsernamePasswordAuthenticationToken(TEST_USERNAME, null, null);
        securityContext.setAuthentication(authentication);
        return ReactiveSecurityContextHolder.withSecurityContext(Mono.just(securityContext));
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

        StepVerifier.create(response.contextWrite(getSecurityContext()))
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

        StepVerifier.create(response.contextWrite(getSecurityContext()))
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

        StepVerifier.create(response.contextWrite(getSecurityContext()))
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

        StepVerifier.create(response.contextWrite(getSecurityContext()))
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

        StepVerifier.create(response.contextWrite(getSecurityContext()))
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

        StepVerifier.create(response.contextWrite(getSecurityContext()))
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

        StepVerifier.create(response.contextWrite(getSecurityContext()))
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

        StepVerifier.create(response.contextWrite(getSecurityContext()))
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

        StepVerifier.create(response.contextWrite(getSecurityContext()))
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

        StepVerifier.create(response.contextWrite(getSecurityContext()))
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

        StepVerifier.create(response.contextWrite(getSecurityContext()))
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

        StepVerifier.create(response.contextWrite(getSecurityContext()))
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

        StepVerifier.create(response.contextWrite(getSecurityContext()))
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

        StepVerifier.create(response.contextWrite(getSecurityContext()))
                .expectError(RuntimeException.class)
                .verify();
    }
    
    @Test
    void shouldCreateLoanRequestWithCreatedByFromAuthentication() {
        CreateLoanRequestDto requestDto = CreateLoanRequestDto.builder()
                .userIdNumber(VALID_USER_ID)
                .loanTypeId(LOAN_TYPE_ID_1)
                .amount(AMOUNT_50000)
                .term(TERM_24)
                .build();

        when(loanApplicationUseCase.createLoanRequest(argThat(command -> 
                command.getCreatedBy() != null && command.getCreatedBy().equals(TEST_USERNAME))))
                .thenReturn(Mono.just(sampleLoanApplication));

        ServerRequest request = MockServerRequest.builder()
                .uri(URI.create(API_PATH))
                .header(CONTENT_TYPE_HEADER, APPLICATION_JSON_VALUE)
                .body(Mono.just(requestDto));

        Mono<ServerResponse> response = handlerV1.createLoanRequest(request);

        StepVerifier.create(response.contextWrite(getSecurityContext()))
                .expectNextMatches(serverResponse -> {
                    assertEquals(200, serverResponse.statusCode().value());
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void shouldGetLoanRequestsByStatusesWithDefaultParameters() {
        LoanRequestRequiringReview loanRequest1 = createSampleLoanRequestRequiringReview(
                JOSE_EMAIL, "José", "García", AMOUNT_50000, TERM_24, SALARY_5000);
        LoanRequestRequiringReview loanRequest2 = createSampleLoanRequestRequiringReview(
                MARIA_EMAIL, "María", "Rodríguez", AMOUNT_75000, TERM_36, SALARY_7500);
        
        PagedResponse<LoanRequestRequiringReview> pagedResponse = PagedResponse.of(
                List.of(loanRequest1, loanRequest2), 0, 10, 2L);
        
        when(loanApplicationUseCase.getLoanRequestsRequiringReview(any(GetLoanApplicationsByStatusesCommand.class)))
                .thenReturn(Mono.just(pagedResponse));
        
        ServerRequest request = MockServerRequest.builder()
                .uri(URI.create(API_PATH))
                .build();
        
        Mono<ServerResponse> response = handlerV1.getLoanRequestsByStatuses(request);
        
        StepVerifier.create(response)
                .expectNextMatches(serverResponse -> {
                    assertEquals(200, serverResponse.statusCode().value());
                    return true;
                })
                .verifyComplete();
    }
    
    @Test
    void shouldGetLoanRequestsByStatusesWithCustomParameters() {
        LoanRequestRequiringReview loanRequest = createSampleLoanRequestRequiringReview(
                CARLOS_EMAIL, "Carlos", "López", AMOUNT_75000, TERM_36, SALARY_7500);
        
        PagedResponse<LoanRequestRequiringReview> pagedResponse = PagedResponse.of(
                List.of(loanRequest), 1, 5, 8L);
        
        when(loanApplicationUseCase.getLoanRequestsRequiringReview(any(GetLoanApplicationsByStatusesCommand.class)))
                .thenReturn(Mono.just(pagedResponse));
        
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("statuses", "PENDING,MANUAL_REVIEW");
        queryParams.put("page", "2");
        queryParams.put("size", "5");
        queryParams.put("sortBy", "amount");
        queryParams.put("sortDirection", "desc");
        
        ServerRequest request = MockServerRequest.builder()
                .uri(URI.create(API_PATH + "?statuses=PENDING,MANUAL_REVIEW&page=2&size=5&sortBy=amount&sortDirection=desc"))
                .build();
        
        Mono<ServerResponse> response = handlerV1.getLoanRequestsByStatuses(request);
        
        StepVerifier.create(response)
                .expectNextMatches(serverResponse -> {
                    assertEquals(200, serverResponse.statusCode().value());
                    return true;
                })
                .verifyComplete();
    }
    
    @Test
    void shouldReturnBadRequestWhenPageValidationFails() {
        ServerRequest request = mock(ServerRequest.class);
        when(request.queryParam("statuses")).thenReturn(Optional.empty());
        when(request.queryParam("page")).thenReturn(Optional.of("0"));
        when(request.path()).thenReturn(API_PATH);
        
        Mono<ServerResponse> response = handlerV1.getLoanRequestsByStatuses(request);
        
        StepVerifier.create(response)
                .expectNextMatches(serverResponse -> {
                    assertEquals(400, serverResponse.statusCode().value());
                    return true;
                })
                .verifyComplete();
    }
    
    @Test
    void shouldReturnBadRequestWhenSizeValidationFails() {
        ServerRequest request = mock(ServerRequest.class);
        when(request.queryParam("statuses")).thenReturn(Optional.empty());
        when(request.queryParam("page")).thenReturn(Optional.empty());
        when(request.queryParam("size")).thenReturn(Optional.of("101"));
        when(request.path()).thenReturn(API_PATH);
        
        Mono<ServerResponse> response = handlerV1.getLoanRequestsByStatuses(request);
        
        StepVerifier.create(response)
                .expectNextMatches(serverResponse -> {
                    assertEquals(400, serverResponse.statusCode().value());
                    return true;
                })
                .verifyComplete();
    }
    
    @Test
    void shouldReturnBadRequestWhenSortByValidationFails() {
        ServerRequest request = mock(ServerRequest.class);
        when(request.queryParam("statuses")).thenReturn(Optional.empty());
        when(request.queryParam("page")).thenReturn(Optional.empty());
        when(request.queryParam("size")).thenReturn(Optional.empty());
        when(request.queryParam("sortBy")).thenReturn(Optional.of("invalidField"));
        when(request.path()).thenReturn(API_PATH);
        
        Mono<ServerResponse> response = handlerV1.getLoanRequestsByStatuses(request);
        
        StepVerifier.create(response)
                .expectNextMatches(serverResponse -> {
                    assertEquals(400, serverResponse.statusCode().value());
                    return true;
                })
                .verifyComplete();
    }
    
    @Test
    void shouldReturnBadRequestWhenSortDirectionValidationFails() {
        ServerRequest request = mock(ServerRequest.class);
        when(request.queryParam("statuses")).thenReturn(Optional.empty());
        when(request.queryParam("page")).thenReturn(Optional.empty());
        when(request.queryParam("size")).thenReturn(Optional.empty());
        when(request.queryParam("sortBy")).thenReturn(Optional.empty());
        when(request.queryParam("sortDirection")).thenReturn(Optional.of("invalid"));
        when(request.path()).thenReturn(API_PATH);
        
        Mono<ServerResponse> response = handlerV1.getLoanRequestsByStatuses(request);
        
        StepVerifier.create(response)
                .expectNextMatches(serverResponse -> {
                    assertEquals(400, serverResponse.statusCode().value());
                    return true;
                })
                .verifyComplete();
    }
    
    @Test
    void shouldReturnBadRequestWhenStatusValidationFails() {
        when(loanApplicationUseCase.getLoanRequestsRequiringReview(any(GetLoanApplicationsByStatusesCommand.class)))
                .thenReturn(Mono.error(new IllegalArgumentException("Invalid status: INVALID")));
        
        ServerRequest request = mock(ServerRequest.class);
        when(request.queryParam("statuses")).thenReturn(Optional.of("INVALID,PENDING"));
        when(request.queryParam("page")).thenReturn(Optional.empty());
        when(request.queryParam("size")).thenReturn(Optional.empty());
        when(request.queryParam("sortBy")).thenReturn(Optional.empty());
        when(request.queryParam("sortDirection")).thenReturn(Optional.empty());
        when(request.path()).thenReturn(API_PATH);
        
        Mono<ServerResponse> response = handlerV1.getLoanRequestsByStatuses(request);
        
        StepVerifier.create(response)
                .expectNextMatches(serverResponse -> {
                    assertEquals(400, serverResponse.statusCode().value());
                    return true;
                })
                .verifyComplete();
    }
    
    @Test
    void shouldGetEmptyLoanRequestsByStatuses() {
        PagedResponse<LoanRequestRequiringReview> emptyPagedResponse = PagedResponse.of(
                List.of(), 0, 10, 0L);
        
        when(loanApplicationUseCase.getLoanRequestsRequiringReview(any(GetLoanApplicationsByStatusesCommand.class)))
                .thenReturn(Mono.just(emptyPagedResponse));
        
        ServerRequest request = MockServerRequest.builder()
                .uri(URI.create(API_PATH))
                .build();
        
        Mono<ServerResponse> response = handlerV1.getLoanRequestsByStatuses(request);
        
        StepVerifier.create(response)
                .expectNextMatches(serverResponse -> {
                    assertEquals(200, serverResponse.statusCode().value());
                    return true;
                })
                .verifyComplete();
    }
    
    @Test
    void shouldGetLoanRequestsByStatuses2WithDefaultParameters() {
        LoanRequestRequiringReview loanRequest1 = createSampleLoanRequestRequiringReview(
                JOSE_EMAIL, "José", "García", AMOUNT_50000, TERM_24, SALARY_5000);
        LoanRequestRequiringReview loanRequest2 = createSampleLoanRequestRequiringReview(
                MARIA_EMAIL, "María", "Rodríguez", AMOUNT_75000, TERM_36, SALARY_7500);
        
        PagedResponse<LoanRequestRequiringReview> pagedResponse = PagedResponse.of(
                List.of(loanRequest1, loanRequest2), 0, 10, 2L);
        
        when(loanApplicationUseCase.getLoanRequestsRequiringReview2(any(GetLoanApplicationsByStatusesCommand.class)))
                .thenReturn(Mono.just(pagedResponse));
        
        ServerRequest request = MockServerRequest.builder()
                .uri(URI.create(API_PATH))
                .build();
        
        Mono<ServerResponse> response = handlerV1.getLoanRequestsByStatuses2(request);
        
        StepVerifier.create(response)
                .expectNextMatches(serverResponse -> {
                    assertEquals(200, serverResponse.statusCode().value());
                    return true;
                })
                .verifyComplete();
    }
    
    @Test
    void shouldGetLoanRequestsByStatuses2WithCustomParameters() {
        LoanRequestRequiringReview loanRequest = createSampleLoanRequestRequiringReview(
                CARLOS_EMAIL, "Carlos", "López", AMOUNT_75000, TERM_36, SALARY_7500);
        
        PagedResponse<LoanRequestRequiringReview> pagedResponse = PagedResponse.of(
                List.of(loanRequest), 1, 5, 8L);
        
        when(loanApplicationUseCase.getLoanRequestsRequiringReview2(any(GetLoanApplicationsByStatusesCommand.class)))
                .thenReturn(Mono.just(pagedResponse));
        
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("statuses", "PENDING,MANUAL_REVIEW");
        queryParams.put("page", "2");
        queryParams.put("size", "5");
        queryParams.put("sortBy", "amount");
        queryParams.put("sortDirection", "desc");
        
        ServerRequest request = MockServerRequest.builder()
                .uri(URI.create(API_PATH + "?statuses=PENDING,MANUAL_REVIEW&page=2&size=5&sortBy=amount&sortDirection=desc"))
                .build();
        
        Mono<ServerResponse> response = handlerV1.getLoanRequestsByStatuses2(request);
        
        StepVerifier.create(response)
                .expectNextMatches(serverResponse -> {
                    assertEquals(200, serverResponse.statusCode().value());
                    return true;
                })
                .verifyComplete();
    }
    
    @Test
    void shouldReturnBadRequestWhenPageValidationFailsForStatuses2() {
        ServerRequest request = mock(ServerRequest.class);
        when(request.queryParam("statuses")).thenReturn(Optional.empty());
        when(request.queryParam("page")).thenReturn(Optional.of("0"));
        when(request.path()).thenReturn(API_PATH);
        
        Mono<ServerResponse> response = handlerV1.getLoanRequestsByStatuses2(request);
        
        StepVerifier.create(response)
                .expectNextMatches(serverResponse -> {
                    assertEquals(400, serverResponse.statusCode().value());
                    return true;
                })
                .verifyComplete();
    }
    
    @Test
    void shouldReturnBadRequestWhenSizeValidationFailsForStatuses2() {
        ServerRequest request = mock(ServerRequest.class);
        when(request.queryParam("statuses")).thenReturn(Optional.empty());
        when(request.queryParam("page")).thenReturn(Optional.empty());
        when(request.queryParam("size")).thenReturn(Optional.of("101"));
        when(request.path()).thenReturn(API_PATH);
        
        Mono<ServerResponse> response = handlerV1.getLoanRequestsByStatuses2(request);
        
        StepVerifier.create(response)
                .expectNextMatches(serverResponse -> {
                    assertEquals(400, serverResponse.statusCode().value());
                    return true;
                })
                .verifyComplete();
    }
    
    @Test
    void shouldReturnBadRequestWhenSortByValidationFailsForStatuses2() {
        ServerRequest request = mock(ServerRequest.class);
        when(request.queryParam("statuses")).thenReturn(Optional.empty());
        when(request.queryParam("page")).thenReturn(Optional.empty());
        when(request.queryParam("size")).thenReturn(Optional.empty());
        when(request.queryParam("sortBy")).thenReturn(Optional.of("invalidField"));
        when(request.path()).thenReturn(API_PATH);
        
        Mono<ServerResponse> response = handlerV1.getLoanRequestsByStatuses2(request);
        
        StepVerifier.create(response)
                .expectNextMatches(serverResponse -> {
                    assertEquals(400, serverResponse.statusCode().value());
                    return true;
                })
                .verifyComplete();
    }
    
    @Test
    void shouldReturnBadRequestWhenSortDirectionValidationFailsForStatuses2() {
        ServerRequest request = mock(ServerRequest.class);
        when(request.queryParam("statuses")).thenReturn(Optional.empty());
        when(request.queryParam("page")).thenReturn(Optional.empty());
        when(request.queryParam("size")).thenReturn(Optional.empty());
        when(request.queryParam("sortBy")).thenReturn(Optional.empty());
        when(request.queryParam("sortDirection")).thenReturn(Optional.of("invalid"));
        when(request.path()).thenReturn(API_PATH);
        
        Mono<ServerResponse> response = handlerV1.getLoanRequestsByStatuses2(request);
        
        StepVerifier.create(response)
                .expectNextMatches(serverResponse -> {
                    assertEquals(400, serverResponse.statusCode().value());
                    return true;
                })
                .verifyComplete();
    }
    
    @Test
    void shouldReturnBadRequestWhenStatusValidationFailsForStatuses2() {
        when(loanApplicationUseCase.getLoanRequestsRequiringReview2(any(GetLoanApplicationsByStatusesCommand.class)))
                .thenReturn(Mono.error(new IllegalArgumentException("Invalid status: INVALID")));
        
        ServerRequest request = mock(ServerRequest.class);
        when(request.queryParam("statuses")).thenReturn(Optional.of("INVALID,PENDING"));
        when(request.queryParam("page")).thenReturn(Optional.empty());
        when(request.queryParam("size")).thenReturn(Optional.empty());
        when(request.queryParam("sortBy")).thenReturn(Optional.empty());
        when(request.queryParam("sortDirection")).thenReturn(Optional.empty());
        when(request.path()).thenReturn(API_PATH);
        
        Mono<ServerResponse> response = handlerV1.getLoanRequestsByStatuses2(request);
        
        StepVerifier.create(response)
                .expectNextMatches(serverResponse -> {
                    assertEquals(400, serverResponse.statusCode().value());
                    return true;
                })
                .verifyComplete();
    }
    
    @Test
    void shouldGetEmptyLoanRequestsByStatuses2() {
        PagedResponse<LoanRequestRequiringReview> emptyPagedResponse = PagedResponse.of(
                List.of(), 0, 10, 0L);
        
        when(loanApplicationUseCase.getLoanRequestsRequiringReview2(any(GetLoanApplicationsByStatusesCommand.class)))
                .thenReturn(Mono.just(emptyPagedResponse));
        
        ServerRequest request = MockServerRequest.builder()
                .uri(URI.create(API_PATH))
                .build();
        
        Mono<ServerResponse> response = handlerV1.getLoanRequestsByStatuses2(request);
        
        StepVerifier.create(response)
                .expectNextMatches(serverResponse -> {
                    assertEquals(200, serverResponse.statusCode().value());
                    return true;
                })
                .verifyComplete();
    }
    
    private LoanRequestRequiringReview createSampleLoanRequestRequiringReview(
            String email, String firstName, String lastName, 
            BigDecimal amount, Integer term, BigDecimal baseSalary) {
        
        LoanApplication loanApplication = LoanApplication.builder()
                .id(UUID.randomUUID().toString())
                .email(email)
                .amount(amount)
                .term(term)
                .status(pendingStatus)
                .loanType(personalLoanType)
                .build();
        
        User user = User.builder()
                .id(UUID.randomUUID().toString())
                .email(email)
                .name(firstName)
                .lastName(lastName)
                .baseSalary(baseSalary)
                .phoneNumber("+57300123456")
                .address("Calle 123 # 45-67, Bogotá")
                .birthDate(LocalDate.of(1985, 6, 15))
                .idNumber("12345678")
                .build();
        
        return new LoanRequestRequiringReview(loanApplication, user);
    }

    // Update Loan Request Tests

    @Test
    void shouldUpdateLoanRequestStatusSuccessfully() {
        String loanId = UUID.randomUUID().toString();
        Integer newStatus = 3; // APPROVED
        
        UpdateLoanRequestDto requestDto = UpdateLoanRequestDto.builder()
                .status(newStatus)
                .build();

        LoanApplicationStatus approvedStatus = LoanApplicationStatus.builder()
                .id(newStatus)
                .name("APPROVED")
                .description("Approved")
                .build();

        LoanApplication updatedLoanApplication = sampleLoanApplication.toBuilder()
                .status(approvedStatus)
                .build();

        when(updateLoanApplicationStatusUseCase.updateLoanApplicationStatus(any(UpdateLoanApplicationCommand.class)))
                .thenReturn(Mono.just(updatedLoanApplication));

        ServerRequest request = MockServerRequest.builder()
                .uri(URI.create(API_PATH + "/" + loanId))
                .pathVariable("id", loanId)
                .header(CONTENT_TYPE_HEADER, APPLICATION_JSON_VALUE)
                .body(Mono.just(requestDto));

        Mono<ServerResponse> response = handlerV1.updateLoanRequest(request);

        StepVerifier.create(response)
                .expectNextMatches(serverResponse -> {
                    assertEquals(200, serverResponse.statusCode().value());
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void shouldHandleValidationErrorWhenStatusIsNull() {
        String loanId = UUID.randomUUID().toString();
        
        UpdateLoanRequestDto requestDto = UpdateLoanRequestDto.builder()
                .status(null)
                .build();

        ServerRequest request = MockServerRequest.builder()
                .uri(URI.create(API_PATH + "/" + loanId))
                .pathVariable("id", loanId)
                .header(CONTENT_TYPE_HEADER, APPLICATION_JSON_VALUE)
                .body(Mono.just(requestDto));

        Mono<ServerResponse> response = handlerV1.updateLoanRequest(request);

        StepVerifier.create(response)
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    void shouldHandleValidationErrorWhenStatusIsZero() {
        String loanId = UUID.randomUUID().toString();
        
        UpdateLoanRequestDto requestDto = UpdateLoanRequestDto.builder()
                .status(0)
                .build();

        ServerRequest request = MockServerRequest.builder()
                .uri(URI.create(API_PATH + "/" + loanId))
                .pathVariable("id", loanId)
                .header(CONTENT_TYPE_HEADER, APPLICATION_JSON_VALUE)
                .body(Mono.just(requestDto));

        Mono<ServerResponse> response = handlerV1.updateLoanRequest(request);

        StepVerifier.create(response)
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    void shouldHandleLoanApplicationNotFoundException() {
        String loanId = UUID.randomUUID().toString();
        Integer newStatus = 3;
        
        UpdateLoanRequestDto requestDto = UpdateLoanRequestDto.builder()
                .status(newStatus)
                .build();

        when(updateLoanApplicationStatusUseCase.updateLoanApplicationStatus(any(UpdateLoanApplicationCommand.class)))
                .thenReturn(Mono.error(new LoanApplicationNotFoundException("Loan application not found")));

        ServerRequest request = MockServerRequest.builder()
                .uri(URI.create(API_PATH + "/" + loanId))
                .pathVariable("id", loanId)
                .header(CONTENT_TYPE_HEADER, APPLICATION_JSON_VALUE)
                .body(Mono.just(requestDto));

        Mono<ServerResponse> response = handlerV1.updateLoanRequest(request);

        StepVerifier.create(response)
                .expectError(LoanApplicationNotFoundException.class)
                .verify();
    }

    @Test
    void shouldHandleLoanApplicationStatusNotFoundException() {
        String loanId = UUID.randomUUID().toString();
        Integer invalidStatusId = 999;
        
        UpdateLoanRequestDto requestDto = UpdateLoanRequestDto.builder()
                .status(invalidStatusId)
                .build();

        when(updateLoanApplicationStatusUseCase.updateLoanApplicationStatus(any(UpdateLoanApplicationCommand.class)))
                .thenReturn(Mono.error(new LoanApplicationStatusNotFoundException("Loan application status with id " + invalidStatusId + " was not found")));

        ServerRequest request = MockServerRequest.builder()
                .uri(URI.create(API_PATH + "/" + loanId))
                .pathVariable("id", loanId)
                .header(CONTENT_TYPE_HEADER, APPLICATION_JSON_VALUE)
                .body(Mono.just(requestDto));

        Mono<ServerResponse> response = handlerV1.updateLoanRequest(request);

        StepVerifier.create(response)
                .expectError(LoanApplicationStatusNotFoundException.class)
                .verify();
    }

    @Test
    void shouldHandleUpdateLoanApplicationException() {
        String loanId = UUID.randomUUID().toString();
        Integer sameStatus = 1; // Same as current status
        
        UpdateLoanRequestDto requestDto = UpdateLoanRequestDto.builder()
                .status(sameStatus)
                .build();

        when(updateLoanApplicationStatusUseCase.updateLoanApplicationStatus(any(UpdateLoanApplicationCommand.class)))
                .thenReturn(Mono.error(new UpdateLoanApplicationException("The new status is the same as the current status of the loan request.")));

        ServerRequest request = MockServerRequest.builder()
                .uri(URI.create(API_PATH + "/" + loanId))
                .pathVariable("id", loanId)
                .header(CONTENT_TYPE_HEADER, APPLICATION_JSON_VALUE)
                .body(Mono.just(requestDto));

        Mono<ServerResponse> response = handlerV1.updateLoanRequest(request);

        StepVerifier.create(response)
                .expectError(UpdateLoanApplicationException.class)
                .verify();
    }

    @Test
    void shouldHandleUnexpectedExceptionInUpdateLoanRequest() {
        String loanId = UUID.randomUUID().toString();
        Integer newStatus = 3;
        
        UpdateLoanRequestDto requestDto = UpdateLoanRequestDto.builder()
                .status(newStatus)
                .build();

        when(updateLoanApplicationStatusUseCase.updateLoanApplicationStatus(any(UpdateLoanApplicationCommand.class)))
                .thenReturn(Mono.error(new RuntimeException("Unexpected error")));

        ServerRequest request = MockServerRequest.builder()
                .uri(URI.create(API_PATH + "/" + loanId))
                .pathVariable("id", loanId)
                .header(CONTENT_TYPE_HEADER, APPLICATION_JSON_VALUE)
                .body(Mono.just(requestDto));

        Mono<ServerResponse> response = handlerV1.updateLoanRequest(request);

        StepVerifier.create(response)
                .expectError(RuntimeException.class)
                .verify();
    }

    @Test
    void shouldPassCorrectParametersToUpdateUseCase() {
        String loanId = "29a4639d-b328-453a-a408-d2eff0bcae84";
        Integer newStatus = 4; // REJECTED
        
        UpdateLoanRequestDto requestDto = UpdateLoanRequestDto.builder()
                .status(newStatus)
                .build();

        LoanApplicationStatus rejectedStatus = LoanApplicationStatus.builder()
                .id(newStatus)
                .name("REJECTED")
                .description("Rejected")
                .build();

        LoanApplication updatedLoanApplication = sampleLoanApplication.toBuilder()
                .id(loanId)
                .status(rejectedStatus)
                .build();

        when(updateLoanApplicationStatusUseCase.updateLoanApplicationStatus(argThat(command -> 
                command.id().equals(loanId) && command.status().equals(newStatus))))
                .thenReturn(Mono.just(updatedLoanApplication));

        ServerRequest request = MockServerRequest.builder()
                .uri(URI.create(API_PATH + "/" + loanId))
                .pathVariable("id", loanId)
                .header(CONTENT_TYPE_HEADER, APPLICATION_JSON_VALUE)
                .body(Mono.just(requestDto));

        Mono<ServerResponse> response = handlerV1.updateLoanRequest(request);

        StepVerifier.create(response)
                .expectNextMatches(serverResponse -> {
                    assertEquals(200, serverResponse.statusCode().value());
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void shouldHandleValidationErrorWhenRequestIdIsBlank() {
        UpdateLoanRequestDto requestDto = UpdateLoanRequestDto.builder()
                .status(3)
                .build();

        ServerRequest request = MockServerRequest.builder()
                .uri(URI.create(API_PATH + "/"))
                .pathVariable("id", "")
                .header(CONTENT_TYPE_HEADER, APPLICATION_JSON_VALUE)
                .body(Mono.just(requestDto));

        Mono<ServerResponse> response = handlerV1.updateLoanRequest(request);

        StepVerifier.create(response)
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    void shouldHandleValidationErrorWhenRequestIdIsNotValidUuid() {
        UpdateLoanRequestDto requestDto = UpdateLoanRequestDto.builder()
                .status(3)
                .build();

        ServerRequest request = MockServerRequest.builder()
                .uri(URI.create(API_PATH + "/invalid-uuid"))
                .pathVariable("id", "invalid-uuid")
                .header(CONTENT_TYPE_HEADER, APPLICATION_JSON_VALUE)
                .body(Mono.just(requestDto));

        Mono<ServerResponse> response = handlerV1.updateLoanRequest(request);

        StepVerifier.create(response)
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    void shouldHandleValidationErrorWhenRequestIdIsNumeric() {
        UpdateLoanRequestDto requestDto = UpdateLoanRequestDto.builder()
                .status(3)
                .build();

        ServerRequest request = MockServerRequest.builder()
                .uri(URI.create(API_PATH + "/12345"))
                .pathVariable("id", "12345")
                .header(CONTENT_TYPE_HEADER, APPLICATION_JSON_VALUE)
                .body(Mono.just(requestDto));

        Mono<ServerResponse> response = handlerV1.updateLoanRequest(request);

        StepVerifier.create(response)
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    void shouldHandleValidationErrorWhenRequestIdHasInvalidFormat() {
        UpdateLoanRequestDto requestDto = UpdateLoanRequestDto.builder()
                .status(3)
                .build();

        ServerRequest request = MockServerRequest.builder()
                .uri(URI.create(API_PATH + "/not-a-uuid-format"))
                .pathVariable("id", "not-a-uuid-format")
                .header(CONTENT_TYPE_HEADER, APPLICATION_JSON_VALUE)
                .body(Mono.just(requestDto));

        Mono<ServerResponse> response = handlerV1.updateLoanRequest(request);

        StepVerifier.create(response)
                .expectError(IllegalArgumentException.class)
                .verify();
    }
}