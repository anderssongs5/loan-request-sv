package co.com.powerup.ags.loan.request.r2dbc;

import co.com.powerup.ags.loan.request.model.loanapplication.LoanApplication;
import co.com.powerup.ags.loan.request.model.loanapplicationstatus.LoanApplicationStatus;
import co.com.powerup.ags.loan.request.model.loantype.LoanType;
import co.com.powerup.ags.loan.request.r2dbc.entity.LoanRequestEntity;
import co.com.powerup.ags.loan.request.r2dbc.entity.LoanRequestWithDetailsEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.reactivecommons.utils.ObjectMapper;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoanRequestReactiveRepositoryAdapterTest {
    
    private static final String JOSE_EMAIL = "jose.garcia@example.com";
    private static final String MARIA_EMAIL = "maria.rodriguez@example.com";
    private static final String CARLOS_EMAIL = "carlos.lopez@example.com";
    private static final BigDecimal AMOUNT_50000 = new BigDecimal("50000.00");
    private static final BigDecimal AMOUNT_75000 = new BigDecimal("75000.00");
    private static final Integer TERM_24 = 24;
    private static final Integer TERM_36 = 36;
    private static final Integer STATUS_ID_1 = 1;
    private static final Integer LOAN_TYPE_ID_1 = 1;
    private static final String PENDING_STATUS = "PENDING";
    private static final String PERSONAL_LOAN = "Personal Loan";
    private static final String PENDING_DESCRIPTION = "Pending review";
    private static final BigDecimal MIN_AMOUNT = new BigDecimal("1000.00");
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("100000.00");
    private static final Integer MIN_TERM = 6;
    private static final Integer MAX_TERM = 60;
    private static final BigDecimal INTEREST_RATE = new BigDecimal("12.5");
    public static final String MANUAL_REVIEW = "MANUAL_REVIEW";
    public static final String AMOUNT = "amount";
    public static final String DESC = "DESC";
    public static final String ASC = "ASC";
    public static final String EMAIL = "email";
    
    @Mock
    private LoanRequestReactiveRepository repository;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private TransactionalOperator transactionalOperator;

    private LoanRequestReactiveRepositoryAdapter adapter;

    private LoanApplication sampleLoanApplication;
    private LoanApplicationStatus pendingStatus;
    private LoanType personalLoanType;

    @BeforeEach
    void setUp() {
        adapter = new LoanRequestReactiveRepositoryAdapter(repository, objectMapper, transactionalOperator);

        pendingStatus = LoanApplicationStatus.builder()
                .id(STATUS_ID_1)
                .name(PENDING_STATUS)
                .description(PENDING_DESCRIPTION)
                .build();

        personalLoanType = LoanType.builder()
                .id(LOAN_TYPE_ID_1)
                .name(PERSONAL_LOAN)
                .minAmount(MIN_AMOUNT)
                .maxAmount(MAX_AMOUNT)
                .minTerm(MIN_TERM)
                .maxTerm(MAX_TERM)
                .interestRate(INTEREST_RATE)
                .automaticValidation(true)
                .build();

        sampleLoanApplication = LoanApplication.builder()
                .id(UUID.randomUUID().toString())
                .email(JOSE_EMAIL)
                .amount(AMOUNT_50000)
                .term(TERM_24)
                .status(pendingStatus)
                .loanType(personalLoanType)
                .build();
    }

    @Test
    void shouldCreateLoanRequestSuccessfully() {
        String loanId = UUID.randomUUID().toString();
        LoanRequestEntity savedEntity = LoanRequestEntity.builder()
                .requestId(loanId)
                .email(JOSE_EMAIL)
                .amount(AMOUNT_50000)
                .term(TERM_24)
                .statusId(STATUS_ID_1)
                .loanTypeId(LOAN_TYPE_ID_1)
                .build();

        when(repository.save(any(LoanRequestEntity.class)))
                .thenReturn(Mono.just(savedEntity));
        when(transactionalOperator.transactional(any(Mono.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Mono<LoanApplication> result = adapter.createLoanRequest(sampleLoanApplication);

        StepVerifier.create(result)
                .expectNextMatches(loanApplication -> {
                    return loanApplication.getId().equals(loanId) &&
                           loanApplication.getEmail().equals(JOSE_EMAIL) &&
                           loanApplication.getAmount().equals(AMOUNT_50000) &&
                           loanApplication.getTerm().equals(TERM_24) &&
                           loanApplication.getStatus().equals(pendingStatus) &&
                           loanApplication.getLoanType().equals(personalLoanType);
                })
                .verifyComplete();
    }

    @Test
    void shouldHandleCreateLoanRequestError() {
        RuntimeException error = new RuntimeException("Database error");

        when(repository.save(any(LoanRequestEntity.class)))
                .thenReturn(Mono.error(error));
        when(transactionalOperator.transactional(any(Mono.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Mono<LoanApplication> result = adapter.createLoanRequest(sampleLoanApplication);

        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();
    }

    @Test
    void shouldGetAllLoanRequestsSuccessfully() {
        String firstLoanId = UUID.randomUUID().toString();
        String secondLoanId = UUID.randomUUID().toString();

        LoanRequestWithDetailsEntity firstEntity = LoanRequestWithDetailsEntity.builder()
                .requestId(firstLoanId)
                .email(JOSE_EMAIL)
                .amount(AMOUNT_50000)
                .term(TERM_24)
                .statusId(STATUS_ID_1)
                .statusName(PENDING_STATUS)
                .statusDescription(PENDING_DESCRIPTION)
                .loanTypeId(LOAN_TYPE_ID_1)
                .typeName(PERSONAL_LOAN)
                .minimumAmount(MIN_AMOUNT)
                .maximumAmount(MAX_AMOUNT)
                .minimumTerm(MIN_TERM)
                .maximumTerm(MAX_TERM)
                .interestRate(INTEREST_RATE)
                .automaticValidation(true)
                .build();

        LoanRequestWithDetailsEntity secondEntity = LoanRequestWithDetailsEntity.builder()
                .requestId(secondLoanId)
                .email(MARIA_EMAIL)
                .amount(AMOUNT_75000)
                .term(TERM_36)
                .statusId(STATUS_ID_1)
                .statusName(PENDING_STATUS)
                .statusDescription(PENDING_DESCRIPTION)
                .loanTypeId(LOAN_TYPE_ID_1)
                .typeName(PERSONAL_LOAN)
                .minimumAmount(MIN_AMOUNT)
                .maximumAmount(MAX_AMOUNT)
                .minimumTerm(MIN_TERM)
                .maximumTerm(MAX_TERM)
                .interestRate(INTEREST_RATE)
                .automaticValidation(true)
                .build();

        when(repository.findAllWithDetails())
                .thenReturn(Flux.just(firstEntity, secondEntity));

        Flux<LoanApplication> result = adapter.getAllLoanRequests();

        StepVerifier.create(result)
                .expectNextMatches(loanApp -> 
                    loanApp.getId().equals(firstLoanId) &&
                    loanApp.getEmail().equals(JOSE_EMAIL) &&
                    loanApp.getAmount().equals(AMOUNT_50000)
                )
                .expectNextMatches(loanApp -> 
                    loanApp.getId().equals(secondLoanId) &&
                    loanApp.getEmail().equals(MARIA_EMAIL) &&
                    loanApp.getAmount().equals(AMOUNT_75000)
                )
                .verifyComplete();
    }

    @Test
    void shouldReturnEmptyFluxWhenNoLoanRequestsExist() {
        when(repository.findAllWithDetails())
                .thenReturn(Flux.empty());

        Flux<LoanApplication> result = adapter.getAllLoanRequests();

        StepVerifier.create(result)
                .verifyComplete();
    }

    @Test
    void shouldHandleGetAllLoanRequestsError() {
        RuntimeException error = new RuntimeException("Database connection error");

        when(repository.findAllWithDetails())
                .thenReturn(Flux.error(error));

        Flux<LoanApplication> result = adapter.getAllLoanRequests();

        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();
    }

    @Test
    void shouldCountLoanApplicationsByStatusesSuccessfully() {
        Set<String> statuses = Set.of(PENDING_STATUS, MANUAL_REVIEW);
        Long expectedCount = 5L;
        
        when(repository.countByStatuses(statuses))
                .thenReturn(Mono.just(expectedCount));
        
        Mono<Long> result = adapter.countLoanApplicationsByStatuses(statuses);
        
        StepVerifier.create(result)
                .expectNext(expectedCount)
                .verifyComplete();
    }
    
    @Test
    void shouldCountLoanApplicationsByStatusesWithEmptyStatuses() {
        Set<String> emptyStatuses = Set.of();
        Long expectedCount = 0L;
        
        when(repository.countByStatuses(emptyStatuses))
                .thenReturn(Mono.just(expectedCount));
        
        Mono<Long> result = adapter.countLoanApplicationsByStatuses(emptyStatuses);
        
        StepVerifier.create(result)
                .expectNext(expectedCount)
                .verifyComplete();
    }
    
    @Test
    void shouldHandleCountLoanApplicationsByStatusesError() {
        Set<String> statuses = Set.of(PENDING_STATUS);
        RuntimeException error = new RuntimeException("Database connection error");
        
        when(repository.countByStatuses(statuses))
                .thenReturn(Mono.error(error));
        
        Mono<Long> result = adapter.countLoanApplicationsByStatuses(statuses);
        
        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();
    }
    
    @Test
    void shouldGetLoanApplicationsPageableByStatusesSuccessfully() {
        Set<String> statuses = Set.of(PENDING_STATUS, MANUAL_REVIEW);
        int page = 0;
        int size = 10;
        String sortBy = AMOUNT;
        String sortDirection = DESC;
        
        String firstLoanId = UUID.randomUUID().toString();
        String secondLoanId = UUID.randomUUID().toString();
        
        LoanRequestWithDetailsEntity firstEntity = LoanRequestWithDetailsEntity.builder()
                .requestId(firstLoanId)
                .email(JOSE_EMAIL)
                .amount(AMOUNT_75000)
                .term(TERM_36)
                .statusId(STATUS_ID_1)
                .statusName(PENDING_STATUS)
                .statusDescription(PENDING_DESCRIPTION)
                .loanTypeId(LOAN_TYPE_ID_1)
                .typeName(PERSONAL_LOAN)
                .minimumAmount(MIN_AMOUNT)
                .maximumAmount(MAX_AMOUNT)
                .minimumTerm(MIN_TERM)
                .maximumTerm(MAX_TERM)
                .interestRate(INTEREST_RATE)
                .automaticValidation(true)
                .build();
        
        LoanRequestWithDetailsEntity secondEntity = LoanRequestWithDetailsEntity.builder()
                .requestId(secondLoanId)
                .email(MARIA_EMAIL)
                .amount(AMOUNT_50000)
                .term(TERM_24)
                .statusId(STATUS_ID_1)
                .statusName(PENDING_STATUS)
                .statusDescription(PENDING_DESCRIPTION)
                .loanTypeId(LOAN_TYPE_ID_1)
                .typeName(PERSONAL_LOAN)
                .minimumAmount(MIN_AMOUNT)
                .maximumAmount(MAX_AMOUNT)
                .minimumTerm(MIN_TERM)
                .maximumTerm(MAX_TERM)
                .interestRate(INTEREST_RATE)
                .automaticValidation(true)
                .build();
        
        when(repository.findByStatusesPageable(statuses, sortBy, sortDirection, size, page))
                .thenReturn(Flux.just(firstEntity, secondEntity));
        
        Flux<LoanApplication> result = adapter.getLoanApplicationsPageableByStatuses(statuses, page, size, sortBy, sortDirection);
        
        StepVerifier.create(result)
                .expectNextMatches(loanApp -> 
                    loanApp.getId().equals(firstLoanId) &&
                    loanApp.getEmail().equals(JOSE_EMAIL) &&
                    loanApp.getAmount().equals(AMOUNT_75000)
                )
                .expectNextMatches(loanApp -> 
                    loanApp.getId().equals(secondLoanId) &&
                    loanApp.getEmail().equals(MARIA_EMAIL) &&
                    loanApp.getAmount().equals(AMOUNT_50000)
                )
                .verifyComplete();
    }
    
    @Test
    void shouldGetLoanApplicationsPageableByStatusesWithNullParameters() {
        Set<String> statuses = Set.of(PENDING_STATUS);
        int page = 1;
        int size = 5;
        
        String loanId = UUID.randomUUID().toString();
        
        LoanRequestWithDetailsEntity entity = LoanRequestWithDetailsEntity.builder()
                .requestId(loanId)
                .email(CARLOS_EMAIL)
                .amount(new BigDecimal("100000.00"))
                .term(48)
                .statusId(STATUS_ID_1)
                .statusName(PENDING_STATUS)
                .statusDescription(PENDING_DESCRIPTION)
                .loanTypeId(LOAN_TYPE_ID_1)
                .typeName(PERSONAL_LOAN)
                .minimumAmount(MIN_AMOUNT)
                .maximumAmount(MAX_AMOUNT)
                .minimumTerm(MIN_TERM)
                .maximumTerm(MAX_TERM)
                .interestRate(INTEREST_RATE)
                .automaticValidation(true)
                .build();
        
        when(repository.findByStatusesPageable(statuses, "request_id", ASC, 10, 0))
                .thenReturn(Flux.just(entity));
        
        Flux<LoanApplication> result = adapter.getLoanApplicationsPageableByStatuses(statuses, null, null, null, null);
        
        StepVerifier.create(result)
                .expectNextMatches(loanApp -> 
                    loanApp.getId().equals(loanId) &&
                    loanApp.getEmail().equals(CARLOS_EMAIL) &&
                    loanApp.getAmount().equals(new BigDecimal("100000.00"))
                )
                .verifyComplete();
    }
    
    @Test
    void shouldReturnEmptyFluxWhenNoLoanApplicationsMatchStatusesPageable() {
        Set<String> statuses = Set.of("COMPLETED");
        int page = 0;
        int size = 10;
        String sortBy = EMAIL;
        String sortDirection = ASC;
        
        when(repository.findByStatusesPageable(statuses, sortBy, sortDirection, size, page))
                .thenReturn(Flux.empty());
        
        Flux<LoanApplication> result = adapter.getLoanApplicationsPageableByStatuses(statuses, page, size, sortBy, sortDirection);
        
        StepVerifier.create(result)
                .verifyComplete();
    }
    
    @Test
    void shouldHandleGetLoanApplicationsPageableByStatusesError() {
        Set<String> statuses = Set.of(PENDING_STATUS);
        int page = 0;
        int size = 10;
        String sortBy = AMOUNT;
        String sortDirection = DESC;
        RuntimeException error = new RuntimeException("Database query error");
        
        when(repository.findByStatusesPageable(statuses, sortBy, sortDirection, size, page))
                .thenReturn(Flux.error(error));
        
        Flux<LoanApplication> result = adapter.getLoanApplicationsPageableByStatuses(statuses, page, size, sortBy, sortDirection);
        
        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();
    }
}