package co.com.powerup.ags.loan.request.api;

import co.com.powerup.ags.loan.request.api.dto.CreateLoanRequestDto;
import co.com.powerup.ags.loan.request.api.helper.GlobalErrorAttributes;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.validation.Validator;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@Configuration
class RouterRestTestConfig {
    
    @Bean
    public Validator validator() {
        return new LocalValidatorFactoryBean();
    }
}

@WebFluxTest
@ContextConfiguration(classes = {RouterRest.class, HandlerV1.class, RouterRestTestConfig.class,
            GlobalExceptionHandler.class, GlobalErrorAttributes.class})
class RouterRestTest {

    private static final String LOAN_REQUESTS_URI = "/api/v1/loan-requests";
    private static final String LOAN_REQUESTS_SUCCESS_MESSAGE = "Loan requests retrieved successfully";
    private static final String CARLOS_EMAIL = "carlos.rodriguez@example.com";
    private static final String MARIA_EMAIL = "maria.fernandez@example.com";
    private static final BigDecimal AMOUNT_50000 = new BigDecimal("50000.00");
    private static final BigDecimal AMOUNT_75000 = new BigDecimal("75000.00");
    private static final Integer TERM_24 = 24;
    private static final Integer TERM_36 = 36;
    private static final Integer LOAN_TYPE_ID_1 = 1;

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private LoanApplicationUseCase loanApplicationUseCase;

    private LoanApplication sampleLoanApplication;
    private LoanApplicationStatus pendingStatus;
    private LoanType personalLoanType;

    @BeforeEach
    void setUp() {
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
    void shouldGetAllLoanRequests() {
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

        webTestClient.get()
                .uri(LOAN_REQUESTS_URI)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.timestamp").isNotEmpty()
                .jsonPath("$.path").isEqualTo(LOAN_REQUESTS_URI)
                .jsonPath("$.message").isEqualTo(LOAN_REQUESTS_SUCCESS_MESSAGE)
                .jsonPath("$.data").isArray()
                .jsonPath("$.data[0].id").isNotEmpty()
                .jsonPath("$.data[0].email").isEqualTo(CARLOS_EMAIL)
                .jsonPath("$.data[0].term").isEqualTo(TERM_24)
                .jsonPath("$.data[0].amount").isEqualTo(new BigDecimal("50000.0"))
                .jsonPath("$.data[0].loanStatusId").isEqualTo(LOAN_TYPE_ID_1)
                .jsonPath("$.data[0].loanTypeId").isEqualTo("1")
                .jsonPath("$.data[1].email").isEqualTo(MARIA_EMAIL)
                .jsonPath("$.data[1].term").isEqualTo(TERM_36);
    }

    @Test
    void shouldGetEmptyLoanRequestsList() {
        when(loanApplicationUseCase.getAllLoanRequests())
                .thenReturn(Flux.empty());

        webTestClient.get()
                .uri(LOAN_REQUESTS_URI)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.data").isArray()
                .jsonPath("$.data").isEmpty()
                .jsonPath("$.message").isEqualTo(LOAN_REQUESTS_SUCCESS_MESSAGE);
    }

    @Test
    void shouldCreateLoanRequestSuccessfully() {
        CreateLoanRequestDto requestDto = CreateLoanRequestDto.builder()
                .userIdNumber("12345678")
                .loanTypeId(1)
                .amount(new BigDecimal("50000.00"))
                .term(24)
                .build();

        when(loanApplicationUseCase.createLoanRequest(any(CreateLoanRequestCommand.class)))
                .thenReturn(Mono.just(sampleLoanApplication));

        webTestClient.post()
                .uri("/api/v1/loan-requests")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestDto)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.timestamp").isNotEmpty()
                .jsonPath("$.path").isEqualTo("/api/v1/loan-requests")
                .jsonPath("$.message").isEqualTo("Loan request created successfully")
                .jsonPath("$.data.id").isNotEmpty()
                .jsonPath("$.data.email").isEqualTo("carlos.rodriguez@example.com")
                .jsonPath("$.data.term").isEqualTo(24)
                .jsonPath("$.data.amount").isEqualTo(50000.00)
                .jsonPath("$.data.loanStatusId").isEqualTo(1)
                .jsonPath("$.data.loanTypeId").isEqualTo("1");
    }

    @Test
    void shouldReturnBadRequestWhenUserIdNumberIsBlank() {
        CreateLoanRequestDto requestDto = CreateLoanRequestDto.builder()
                .userIdNumber("")
                .loanTypeId(1)
                .amount(new BigDecimal("50000.00"))
                .term(24)
                .build();

        webTestClient.post()
                .uri("/api/v1/loan-requests")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestDto)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.code").isEqualTo("INVALID_INPUT")
                .jsonPath("$.error").isEqualTo("Bad Request")
                .jsonPath("$.message").value(org.hamcrest.Matchers.containsString("User ID number is required"));
    }

    @Test
    void shouldReturnBadRequestWhenUserIdNumberIsNull() {
        CreateLoanRequestDto requestDto = CreateLoanRequestDto.builder()
                .userIdNumber(null)
                .loanTypeId(1)
                .amount(new BigDecimal("50000.00"))
                .term(24)
                .build();

        webTestClient.post()
                .uri("/api/v1/loan-requests")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestDto)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.message").value(org.hamcrest.Matchers.containsString("User ID number is required"));
    }

    @Test
    void shouldReturnBadRequestWhenLoanTypeIdIsNull() {
        CreateLoanRequestDto requestDto = CreateLoanRequestDto.builder()
                .userIdNumber("12345678")
                .loanTypeId(null)
                .amount(new BigDecimal("50000.00"))
                .term(24)
                .build();

        webTestClient.post()
                .uri("/api/v1/loan-requests")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestDto)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Loan type ID is required"));
    }

    @Test
    void shouldReturnBadRequestWhenLoanTypeIdIsZero() {
        CreateLoanRequestDto requestDto = CreateLoanRequestDto.builder()
                .userIdNumber("12345678")
                .loanTypeId(0)
                .amount(new BigDecimal("50000.00"))
                .term(24)
                .build();

        webTestClient.post()
                .uri("/api/v1/loan-requests")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestDto)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Loan type ID must be greater than 0"));
    }

    @Test
    void shouldReturnBadRequestWhenAmountIsNull() {
        CreateLoanRequestDto requestDto = CreateLoanRequestDto.builder()
                .userIdNumber("12345678")
                .loanTypeId(1)
                .amount(null)
                .term(24)
                .build();

        webTestClient.post()
                .uri("/api/v1/loan-requests")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestDto)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Amount is required"));
    }

    @Test
    void shouldReturnBadRequestWhenAmountIsZero() {
        CreateLoanRequestDto requestDto = CreateLoanRequestDto.builder()
                .userIdNumber("12345678")
                .loanTypeId(1)
                .amount(BigDecimal.ZERO)
                .term(24)
                .build();

        webTestClient.post()
                .uri("/api/v1/loan-requests")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestDto)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Amount must be greater than 0"));
    }

    @Test
    void shouldReturnBadRequestWhenTermIsNull() {
        CreateLoanRequestDto requestDto = CreateLoanRequestDto.builder()
                .userIdNumber("12345678")
                .loanTypeId(1)
                .amount(new BigDecimal("50000.00"))
                .term(null)
                .build();

        webTestClient.post()
                .uri("/api/v1/loan-requests")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestDto)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Term is required"));
    }

    @Test
    void shouldReturnBadRequestWhenTermIsZero() {
        CreateLoanRequestDto requestDto = CreateLoanRequestDto.builder()
                .userIdNumber("12345678")
                .loanTypeId(1)
                .amount(new BigDecimal("50000.00"))
                .term(0)
                .build();

        webTestClient.post()
                .uri("/api/v1/loan-requests")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestDto)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Term must be at least 1 month"));
    }

    @Test
    void shouldReturnBadRequestWhenMultipleValidationsFail() {
        CreateLoanRequestDto requestDto = CreateLoanRequestDto.builder()
                .userIdNumber("")
                .loanTypeId(0)
                .amount(BigDecimal.ZERO)
                .term(0)
                .build();

        webTestClient.post()
                .uri("/api/v1/loan-requests")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestDto)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.code").isEqualTo("INVALID_INPUT");
    }

    @Test
    void shouldReturnBadRequestWhenUserDoesNotExist() {
        CreateLoanRequestDto requestDto = CreateLoanRequestDto.builder()
                .userIdNumber("99999999")
                .loanTypeId(1)
                .amount(new BigDecimal("50000.00"))
                .term(24)
                .build();

        when(loanApplicationUseCase.createLoanRequest(any(CreateLoanRequestCommand.class)))
                .thenReturn(Mono.error(new UserNotFoundException("User with ID number 99999999 does not exist")));

        webTestClient.post()
                .uri("/api/v1/loan-requests")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestDto)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.code").isEqualTo("INVALID_INPUT")
                .jsonPath("$.message").isEqualTo("User with ID number 99999999 does not exist");
    }

    @Test
    void shouldReturnServiceUnavailableWhenUserServiceIsDown() {
        CreateLoanRequestDto requestDto = CreateLoanRequestDto.builder()
                .userIdNumber("12345678")
                .loanTypeId(1)
                .amount(new BigDecimal("50000.00"))
                .term(24)
                .build();

        when(loanApplicationUseCase.createLoanRequest(any(CreateLoanRequestCommand.class)))
                .thenReturn(Mono.error(new UserServiceException("User service unavailable")));

        webTestClient.post()
                .uri("/api/v1/loan-requests")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestDto)
                .exchange()
                .expectStatus().isEqualTo(503)
                .expectBody()
                .jsonPath("$.status").isEqualTo(503)
                .jsonPath("$.code").isEqualTo("USER_SERVICE_UNAVAILABLE")
                .jsonPath("$.error").isEqualTo("Service Unavailable")
                .jsonPath("$.message").value(org.hamcrest.Matchers.containsString("user validation service is temporarily unavailable"));
    }

    @Test
    void shouldReturnBadRequestWhenUserValidationFails() {
        CreateLoanRequestDto requestDto = CreateLoanRequestDto.builder()
                .userIdNumber("12345678")
                .loanTypeId(1)
                .amount(new BigDecimal("50000.00"))
                .term(24)
                .build();

        when(loanApplicationUseCase.createLoanRequest(any(CreateLoanRequestCommand.class)))
                .thenReturn(Mono.error(new UserValidationException("User does not meet eligibility criteria")));

        webTestClient.post()
                .uri("/api/v1/loan-requests")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestDto)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.code").isEqualTo("INVALID_INPUT")
                .jsonPath("$.error").isEqualTo("Bad Request")
                .jsonPath("$.message").isEqualTo("User does not meet eligibility criteria");
    }

    @Test
    void shouldReturnInternalServerErrorForUnexpectedError() {
        CreateLoanRequestDto requestDto = CreateLoanRequestDto.builder()
                .userIdNumber("12345678")
                .loanTypeId(1)
                .amount(new BigDecimal("50000.00"))
                .term(24)
                .build();

        when(loanApplicationUseCase.createLoanRequest(any(CreateLoanRequestCommand.class)))
                .thenReturn(Mono.error(new RuntimeException("Unexpected error")));

        webTestClient.post()
                .uri("/api/v1/loan-requests")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestDto)
                .exchange()
                .expectStatus().is5xxServerError()
                .expectBody()
                .jsonPath("$.status").isEqualTo(500)
                .jsonPath("$.code").isEqualTo("UNEXPECTED_ERROR")
                .jsonPath("$.error").isEqualTo("Internal Server Error")
                .jsonPath("$.message").value(org.hamcrest.Matchers.containsString("unexpected error occurred"));
    }

    @Test
    void shouldHandleMalformedJsonRequest() {
        webTestClient.post()
                .uri("/api/v1/loan-requests")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{invalid json}")
                .exchange()
                .expectStatus().is4xxClientError();
    }
}
