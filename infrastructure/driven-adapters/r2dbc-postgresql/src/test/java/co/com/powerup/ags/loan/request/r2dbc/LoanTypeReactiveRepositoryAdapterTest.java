package co.com.powerup.ags.loan.request.r2dbc;

import co.com.powerup.ags.loan.request.model.loantype.LoanType;
import co.com.powerup.ags.loan.request.r2dbc.entity.LoanTypeEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.reactivecommons.utils.ObjectMapper;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoanTypeReactiveRepositoryAdapterTest {

    private static final Integer LOAN_TYPE_ID_1 = 1;
    private static final Integer LOAN_TYPE_ID_2 = 2;
    private static final String PERSONAL_LOAN = "Personal Loan";
    private static final String MORTGAGE_LOAN = "Mortgage Loan";
    private static final BigDecimal MIN_AMOUNT = new BigDecimal("1000.00");
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("100000.00");
    private static final BigDecimal MORTGAGE_MIN_AMOUNT = new BigDecimal("50000.00");
    private static final BigDecimal MORTGAGE_MAX_AMOUNT = new BigDecimal("500000.00");
    private static final Integer MIN_TERM = 6;
    private static final Integer MAX_TERM = 60;
    private static final Integer MORTGAGE_MIN_TERM = 12;
    private static final Integer MORTGAGE_MAX_TERM = 360;
    private static final BigDecimal INTEREST_RATE = new BigDecimal("12.5");
    private static final BigDecimal MORTGAGE_INTEREST_RATE = new BigDecimal("8.5");

    @Mock
    private LoanTypeReactiveRepository repository;

    @Mock
    private ObjectMapper objectMapper;

    private LoanTypeReactiveRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new LoanTypeReactiveRepositoryAdapter(repository, objectMapper);
    }

    @Test
    void shouldGetLoanTypeByIdSuccessfully() {
        LoanTypeEntity loanTypeEntity = LoanTypeEntity.builder()
                .id(LOAN_TYPE_ID_1)
                .name(PERSONAL_LOAN)
                .minAmount(MIN_AMOUNT)
                .maxAmount(MAX_AMOUNT)
                .minTerm(MIN_TERM)
                .maxTerm(MAX_TERM)
                .interestRate(INTEREST_RATE)
                .automaticValidation(true)
                .build();

        LoanType expectedLoanType = LoanType.builder()
                .id(LOAN_TYPE_ID_1)
                .name(PERSONAL_LOAN)
                .minAmount(MIN_AMOUNT)
                .maxAmount(MAX_AMOUNT)
                .minTerm(MIN_TERM)
                .maxTerm(MAX_TERM)
                .interestRate(INTEREST_RATE)
                .automaticValidation(true)
                .build();

        when(repository.findById(LOAN_TYPE_ID_1))
                .thenReturn(Mono.just(loanTypeEntity));
        when(objectMapper.map(loanTypeEntity, LoanType.class))
                .thenReturn(expectedLoanType);

        Mono<LoanType> result = adapter.getById(LOAN_TYPE_ID_1);

        StepVerifier.create(result)
                .expectNextMatches(loanType -> 
                    loanType.getId().equals(LOAN_TYPE_ID_1) &&
                    loanType.getName().equals(PERSONAL_LOAN) &&
                    loanType.getMinAmount().equals(MIN_AMOUNT) &&
                    loanType.getMaxAmount().equals(MAX_AMOUNT) &&
                    loanType.getMinTerm().equals(MIN_TERM) &&
                    loanType.getMaxTerm().equals(MAX_TERM) &&
                    loanType.getInterestRate().equals(INTEREST_RATE) &&
                    loanType.getAutomaticValidation().equals(true)
                )
                .verifyComplete();
    }

    @Test
    void shouldReturnEmptyWhenLoanTypeNotFoundById() {
        when(repository.findById(LOAN_TYPE_ID_1))
                .thenReturn(Mono.empty());

        Mono<LoanType> result = adapter.getById(LOAN_TYPE_ID_1);

        StepVerifier.create(result)
                .verifyComplete();
    }

    @Test
    void shouldHandleErrorWhenGettingLoanTypeById() {
        RuntimeException error = new RuntimeException("Database connection error");

        when(repository.findById(LOAN_TYPE_ID_1))
                .thenReturn(Mono.error(error));

        Mono<LoanType> result = adapter.getById(LOAN_TYPE_ID_1);

        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();
    }

    @Test
    void shouldGetMortgageLoanTypeById() {
        LoanTypeEntity mortgageLoanEntity = LoanTypeEntity.builder()
                .id(LOAN_TYPE_ID_2)
                .name(MORTGAGE_LOAN)
                .minAmount(MORTGAGE_MIN_AMOUNT)
                .maxAmount(MORTGAGE_MAX_AMOUNT)
                .minTerm(MORTGAGE_MIN_TERM)
                .maxTerm(MORTGAGE_MAX_TERM)
                .interestRate(MORTGAGE_INTEREST_RATE)
                .automaticValidation(false)
                .build();

        LoanType expectedMortgageLoanType = LoanType.builder()
                .id(LOAN_TYPE_ID_2)
                .name(MORTGAGE_LOAN)
                .minAmount(MORTGAGE_MIN_AMOUNT)
                .maxAmount(MORTGAGE_MAX_AMOUNT)
                .minTerm(MORTGAGE_MIN_TERM)
                .maxTerm(MORTGAGE_MAX_TERM)
                .interestRate(MORTGAGE_INTEREST_RATE)
                .automaticValidation(false)
                .build();

        when(repository.findById(LOAN_TYPE_ID_2))
                .thenReturn(Mono.just(mortgageLoanEntity));
        when(objectMapper.map(mortgageLoanEntity, LoanType.class))
                .thenReturn(expectedMortgageLoanType);

        Mono<LoanType> result = adapter.getById(LOAN_TYPE_ID_2);

        StepVerifier.create(result)
                .expectNextMatches(loanType -> 
                    loanType.getId().equals(LOAN_TYPE_ID_2) &&
                    loanType.getName().equals(MORTGAGE_LOAN) &&
                    loanType.getMinAmount().equals(MORTGAGE_MIN_AMOUNT) &&
                    loanType.getMaxAmount().equals(MORTGAGE_MAX_AMOUNT) &&
                    loanType.getMinTerm().equals(MORTGAGE_MIN_TERM) &&
                    loanType.getMaxTerm().equals(MORTGAGE_MAX_TERM) &&
                    loanType.getInterestRate().equals(MORTGAGE_INTEREST_RATE) &&
                    loanType.getAutomaticValidation().equals(false)
                )
                .verifyComplete();
    }
}