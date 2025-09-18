package co.com.powerup.ags.loan.request.api;

import co.com.powerup.ags.loan.request.api.dto.CreateLoanRequestDto;
import co.com.powerup.ags.loan.request.api.exception.AccessDeniedException;
import co.com.powerup.ags.loan.request.api.exception.UnauthorizedException;
import co.com.powerup.ags.loan.request.api.helper.GlobalErrorAttributes;
import co.com.powerup.ags.loan.request.model.common.PagedResponse;
import co.com.powerup.ags.loan.request.model.loanapplication.LoanApplication;
import co.com.powerup.ags.loan.request.usecase.loanapplication.dto.LoanRequestRequiringReview;
import co.com.powerup.ags.loan.request.model.loanapplicationstatus.LoanApplicationStatus;
import co.com.powerup.ags.loan.request.model.loantype.LoanType;
import co.com.powerup.ags.loan.request.model.user.User;
import co.com.powerup.ags.loan.request.model.exception.UserServiceException;
import co.com.powerup.ags.loan.request.model.exception.UserValidationException;
import co.com.powerup.ags.loan.request.usecase.borrowingcapacity.BorrowingCapacityUseCase;
import co.com.powerup.ags.loan.request.usecase.loanapplication.LoanApplicationUseCase;
import co.com.powerup.ags.loan.request.usecase.loanapplication.UpdateLoanApplicationStatusUseCase;
import co.com.powerup.ags.loan.request.usecase.loanapplication.dto.CreateLoanRequestCommand;
import co.com.powerup.ags.loan.request.usecase.loanapplication.dto.GetLoanApplicationsByStatusesCommand;
import co.com.powerup.ags.loan.request.usecase.common.exception.UserNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.validation.Validator;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
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

@WebFluxTest(excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.reactive.ReactiveUserDetailsServiceAutoConfiguration.class
})
@ContextConfiguration(classes = {RouterRest.class, HandlerV1.class, RouterRestTestConfig.class,
            GlobalExceptionHandler.class, GlobalErrorAttributes.class})
class RouterRestTest {

    private static final String LOAN_REQUESTS_URI = "/api/v1/loan-requests";
    private static final String LOAN_REQUESTS_REQUIRING_REVIEW_SUCCESS_MESSAGE = "Loan requests requiring review retrieved successfully";
    private static final String JOSE_EMAIL = "jose.garcia@example.com";
    private static final String MARIA_EMAIL = "maria.rodriguez@example.com";
    private static final String CARLOS_EMAIL = "carlos.lopez@example.com";
    private static final BigDecimal AMOUNT_50000 = new BigDecimal("50000.00");
    private static final BigDecimal AMOUNT_75000 = new BigDecimal("75000.00");
    private static final BigDecimal AMOUNT_100000 = new BigDecimal("100000.00");
    private static final BigDecimal SALARY_5000 = new BigDecimal("5000.00");
    private static final BigDecimal SALARY_7500 = new BigDecimal("7500.00");
    private static final BigDecimal SALARY_10000 = new BigDecimal("10000.00");
    private static final Integer TERM_24 = 24;
    private static final Integer TERM_36 = 36;
    private static final Integer TERM_48 = 48;

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private LoanApplicationUseCase loanApplicationUseCase;

    @MockitoBean
    private UpdateLoanApplicationStatusUseCase updateLoanApplicationStatusUseCase;

    @MockitoBean
    private BorrowingCapacityUseCase borrowingCapacityUseCase;

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
    void shouldCreateLoanRequestSuccessfully() {
        CreateLoanRequestDto requestDto = CreateLoanRequestDto.builder()
                .userIdNumber("12345678")
                .loanTypeId(1)
                .amount(new BigDecimal("50000.00"))
                .term(24)
                .build();

        when(loanApplicationUseCase.createLoanRequest(any(CreateLoanRequestCommand.class)))
                .thenReturn(Mono.just(sampleLoanApplication));
        
        webTestClient
                .post()
                .uri(LOAN_REQUESTS_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestDto)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.timestamp").isNotEmpty()
                .jsonPath("$.path").isEqualTo(LOAN_REQUESTS_URI)
                .jsonPath("$.message").isEqualTo("Loan request created successfully")
                .jsonPath("$.data.id").isNotEmpty()
                .jsonPath("$.data.email").isEqualTo(CARLOS_EMAIL)
                .jsonPath("$.data.term").isEqualTo(TERM_24)
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
        
        webTestClient
                .post()
                .uri(LOAN_REQUESTS_URI)
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
        
        webTestClient
                .post()
                .uri(LOAN_REQUESTS_URI)
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

        webTestClient
                .post()
                .uri(LOAN_REQUESTS_URI)
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

        webTestClient
                .post()
                .uri(LOAN_REQUESTS_URI)
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

        webTestClient
                .post()
                .uri(LOAN_REQUESTS_URI)
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

        webTestClient
                .post()
                .uri(LOAN_REQUESTS_URI)
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

        webTestClient
                .post()
                .uri(LOAN_REQUESTS_URI)
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

        webTestClient
                .post()
                .uri(LOAN_REQUESTS_URI)
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

        webTestClient
                .post()
                .uri(LOAN_REQUESTS_URI)
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

        webTestClient
                .post()
                .uri(LOAN_REQUESTS_URI)
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

        webTestClient
                .post()
                .uri(LOAN_REQUESTS_URI)
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

        webTestClient
                .post()
                .uri(LOAN_REQUESTS_URI)
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

        webTestClient
                .post()
                .uri(LOAN_REQUESTS_URI)
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
        UsernamePasswordAuthenticationToken authentication = 
            new UsernamePasswordAuthenticationToken(CARLOS_EMAIL, "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...", null);
        
        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockAuthentication(authentication))
                .post()
                .uri(LOAN_REQUESTS_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{invalid json}")
                .exchange()
                .expectStatus().is4xxClientError();
    }

    @Test
    void shouldReturnUnauthorizedWhenUnauthorizedExceptionIsThrown() {
        CreateLoanRequestDto requestDto = CreateLoanRequestDto.builder()
                .userIdNumber("12345678")
                .loanTypeId(1)
                .amount(new BigDecimal("50000.00"))
                .term(24)
                .build();

        when(loanApplicationUseCase.createLoanRequest(any(CreateLoanRequestCommand.class)))
                .thenReturn(Mono.error(new UnauthorizedException("Invalid token")));

        UsernamePasswordAuthenticationToken authentication = 
            new UsernamePasswordAuthenticationToken(CARLOS_EMAIL, "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...", null);

        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockAuthentication(authentication))
                .post()
                .uri(LOAN_REQUESTS_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestDto)
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.status").isEqualTo(401)
                .jsonPath("$.code").isEqualTo("UNAUTHORIZED")
                .jsonPath("$.error").isEqualTo("Unauthorized");
    }

    @Test
    void shouldReturnForbiddenWhenAccessDeniedExceptionIsThrown() {
        CreateLoanRequestDto requestDto = CreateLoanRequestDto.builder()
                .userIdNumber("12345678")
                .loanTypeId(1)
                .amount(new BigDecimal("50000.00"))
                .term(24)
                .build();

        when(loanApplicationUseCase.createLoanRequest(any(CreateLoanRequestCommand.class)))
                .thenReturn(Mono.error(new AccessDeniedException("Access denied. Insufficient permissions.")));

        UsernamePasswordAuthenticationToken authentication = 
            new UsernamePasswordAuthenticationToken(CARLOS_EMAIL, "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...", null);

        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockAuthentication(authentication))
                .post()
                .uri(LOAN_REQUESTS_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestDto)
                .exchange()
                .expectStatus().isForbidden()
                .expectBody()
                .jsonPath("$.status").isEqualTo(403)
                .jsonPath("$.code").isEqualTo("ACCESS_DENIED")
                .jsonPath("$.error").isEqualTo("Forbidden");
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
        
        webTestClient.get()
                .uri(LOAN_REQUESTS_URI)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.timestamp").isNotEmpty()
                .jsonPath("$.path").isEqualTo(LOAN_REQUESTS_URI)
                .jsonPath("$.message").isEqualTo(LOAN_REQUESTS_REQUIRING_REVIEW_SUCCESS_MESSAGE)
                .jsonPath("$.data.content").isArray()
                .jsonPath("$.data.content.length()").isEqualTo(2)
                .jsonPath("$.data.content[0].name").isEqualTo("José García")
                .jsonPath("$.data.content[0].email").isEqualTo(JOSE_EMAIL)
                .jsonPath("$.data.content[0].amount").isEqualTo(50000.0)
                .jsonPath("$.data.content[0].term").isEqualTo(24)
                .jsonPath("$.data.content[0].baseSalary").isEqualTo(5000.0)
                .jsonPath("$.data.content[1].name").isEqualTo("María Rodríguez")
                .jsonPath("$.data.content[1].email").isEqualTo(MARIA_EMAIL)
                .jsonPath("$.data.pagination.currentPage").isEqualTo(0)
                .jsonPath("$.data.pagination.pageSize").isEqualTo(10)
                .jsonPath("$.data.pagination.totalElements").isEqualTo(2)
                .jsonPath("$.data.pagination.totalPages").isEqualTo(1)
                .jsonPath("$.data.pagination.numberOfElements").isEqualTo(2)
                .jsonPath("$.data.pagination.hasNext").isEqualTo(false)
                .jsonPath("$.data.pagination.hasPrevious").isEqualTo(false)
                .jsonPath("$.data.pagination.first").isEqualTo(true)
                .jsonPath("$.data.pagination.last").isEqualTo(true);
    }
    
    @Test
    void shouldGetLoanRequestsByStatusesWithCustomParameters() {
        LoanRequestRequiringReview loanRequest = createSampleLoanRequestRequiringReview(
                CARLOS_EMAIL, "Carlos", "López", AMOUNT_100000, TERM_48, SALARY_10000);
        
        PagedResponse<LoanRequestRequiringReview> pagedResponse = PagedResponse.of(
                List.of(loanRequest), 1, 5, 8L);
        
        when(loanApplicationUseCase.getLoanRequestsRequiringReview(any(GetLoanApplicationsByStatusesCommand.class)))
                .thenReturn(Mono.just(pagedResponse));
        
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(LOAN_REQUESTS_URI)
                        .queryParam("statuses", "PENDING,MANUAL_REVIEW")
                        .queryParam("page", "2")
                        .queryParam("size", "5")
                        .queryParam("sortBy", "amount")
                        .queryParam("sortDirection", "desc")
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.message").isEqualTo(LOAN_REQUESTS_REQUIRING_REVIEW_SUCCESS_MESSAGE)
                .jsonPath("$.data.content.length()").isEqualTo(1)
                .jsonPath("$.data.content[0].name").isEqualTo("Carlos López")
                .jsonPath("$.data.content[0].amount").isEqualTo(100000.0)
                .jsonPath("$.data.content[0].baseSalary").isEqualTo(10000.0)
                .jsonPath("$.data.pagination.currentPage").isEqualTo(1)
                .jsonPath("$.data.pagination.pageSize").isEqualTo(5)
                .jsonPath("$.data.pagination.totalElements").isEqualTo(8)
                .jsonPath("$.data.pagination.totalPages").isEqualTo(2)
                .jsonPath("$.data.pagination.hasNext").isEqualTo(false)
                .jsonPath("$.data.pagination.hasPrevious").isEqualTo(true)
                .jsonPath("$.data.pagination.first").isEqualTo(false)
                .jsonPath("$.data.pagination.last").isEqualTo(true);
    }
    
    @Test
    void shouldGetEmptyLoanRequestsByStatuses() {
        PagedResponse<LoanRequestRequiringReview> emptyPagedResponse = PagedResponse.of(
                List.of(), 0, 10, 0L);
        
        when(loanApplicationUseCase.getLoanRequestsRequiringReview(any(GetLoanApplicationsByStatusesCommand.class)))
                .thenReturn(Mono.just(emptyPagedResponse));
        
        webTestClient.get()
                .uri(LOAN_REQUESTS_URI)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.data.content").isArray()
                .jsonPath("$.data.content").isEmpty()
                .jsonPath("$.data.pagination.totalElements").isEqualTo(0)
                .jsonPath("$.data.pagination.totalPages").isEqualTo(0)
                .jsonPath("$.message").isEqualTo(LOAN_REQUESTS_REQUIRING_REVIEW_SUCCESS_MESSAGE);
    }
    
    @Test
    void shouldReturnBadRequestWhenPageIsLessThanOne() {
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(LOAN_REQUESTS_URI)
                        .queryParam("page", "0")
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Page number must be >= 1"));
    }
    
    @Test
    void shouldReturnBadRequestWhenPageIsNotAnInteger() {
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(LOAN_REQUESTS_URI)
                        .queryParam("page", "invalid")
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Page must be a valid integer"));
    }
    
    @Test
    void shouldReturnBadRequestWhenSizeIsLessThanOne() {
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(LOAN_REQUESTS_URI)
                        .queryParam("size", "0")
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Size must be >= 1"));
    }
    
    @Test
    void shouldReturnBadRequestWhenSizeIsGreaterThanMax() {
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(LOAN_REQUESTS_URI)
                        .queryParam("size", "101")
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Size must be <= 100"));
    }
    
    @Test
    void shouldReturnBadRequestWhenSizeIsNotAnInteger() {
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(LOAN_REQUESTS_URI)
                        .queryParam("size", "invalid")
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Size must be a valid integer"));
    }
    
    @Test
    void shouldReturnBadRequestWhenSortByIsInvalid() {
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(LOAN_REQUESTS_URI)
                        .queryParam("sortBy", "invalidField")
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Invalid sortBy field"));
    }
    
    @Test
    void shouldReturnBadRequestWhenSortDirectionIsInvalid() {
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(LOAN_REQUESTS_URI)
                        .queryParam("sortDirection", "invalid")
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Invalid sortDirection"));
    }
    
    @Test
    void shouldReturnBadRequestWhenStatusesAreInvalid() {
        when(loanApplicationUseCase.getLoanRequestsRequiringReview(any(GetLoanApplicationsByStatusesCommand.class)))
                .thenReturn(Mono.error(new IllegalArgumentException("Invalid status: INVALID")));
        
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(LOAN_REQUESTS_URI)
                        .queryParam("statuses", "INVALID,PENDING")
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Invalid status"));
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
}
