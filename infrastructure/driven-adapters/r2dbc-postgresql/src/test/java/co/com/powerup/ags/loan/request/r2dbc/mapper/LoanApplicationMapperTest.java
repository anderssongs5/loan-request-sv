package co.com.powerup.ags.loan.request.r2dbc.mapper;

import co.com.powerup.ags.loan.request.model.loanapplication.LoanApplication;
import co.com.powerup.ags.loan.request.model.loanapplicationstatus.LoanApplicationStatus;
import co.com.powerup.ags.loan.request.model.loantype.LoanType;
import co.com.powerup.ags.loan.request.r2dbc.entity.LoanRequestEntity;
import co.com.powerup.ags.loan.request.r2dbc.entity.LoanRequestWithDetailsEntity;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class LoanApplicationMapperTest {

    private static final String CARLOS_EMAIL = "carlos.rodriguez@example.com";
    private static final String MARIA_EMAIL = "maria.fernandez@example.com";
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

    @Test
    void shouldMapLoanApplicationToEntity() {
        LoanApplicationStatus status = LoanApplicationStatus.builder()
                .id(STATUS_ID_1)
                .name(PENDING_STATUS)
                .description(PENDING_DESCRIPTION)
                .build();

        LoanType loanType = LoanType.builder()
                .id(LOAN_TYPE_ID_1)
                .name(PERSONAL_LOAN)
                .minAmount(MIN_AMOUNT)
                .maxAmount(MAX_AMOUNT)
                .minTerm(MIN_TERM)
                .maxTerm(MAX_TERM)
                .interestRate(INTEREST_RATE)
                .automaticValidation(true)
                .build();

        String loanId = UUID.randomUUID().toString();
        LoanApplication loanApplication = LoanApplication.builder()
                .id(loanId)
                .email(CARLOS_EMAIL)
                .amount(AMOUNT_50000)
                .term(TERM_24)
                .status(status)
                .loanType(loanType)
                .build();

        LoanRequestEntity entity = LoanApplicationMapper.INSTANCE.toEntity(loanApplication);

        assertNotNull(entity);
        assertEquals(loanId, entity.getRequestId());
        assertEquals(CARLOS_EMAIL, entity.getEmail());
        assertEquals(AMOUNT_50000, entity.getAmount());
        assertEquals(TERM_24, entity.getTerm());
        assertEquals(STATUS_ID_1, entity.getStatusId());
        assertEquals(LOAN_TYPE_ID_1, entity.getLoanTypeId());
    }

    @Test
    void shouldMapEntityToLoanApplication() {
        String loanId = UUID.randomUUID().toString();
        LoanRequestEntity entity = LoanRequestEntity.builder()
                .requestId(loanId)
                .email(MARIA_EMAIL)
                .amount(AMOUNT_75000)
                .term(TERM_36)
                .statusId(STATUS_ID_1)
                .loanTypeId(LOAN_TYPE_ID_1)
                .build();

        LoanApplication loanApplication = LoanApplicationMapper.INSTANCE.toDomain(entity);

        assertNotNull(loanApplication);
        assertEquals(loanId, loanApplication.getId());
        assertEquals(MARIA_EMAIL, loanApplication.getEmail());
        assertEquals(AMOUNT_75000, loanApplication.getAmount());
        assertEquals(TERM_36, loanApplication.getTerm());
        assertNull(loanApplication.getStatus());
        assertNull(loanApplication.getLoanType());
    }

    @Test
    void shouldMapDetailsEntityToLoanApplication() {
        String loanId = UUID.randomUUID().toString();
        LoanRequestWithDetailsEntity detailsEntity = LoanRequestWithDetailsEntity.builder()
                .requestId(loanId)
                .email(CARLOS_EMAIL)
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

        LoanApplication loanApplication = LoanApplicationMapper.INSTANCE.toDomain(detailsEntity);

        assertNotNull(loanApplication);
        assertEquals(loanId, loanApplication.getId());
        assertEquals(CARLOS_EMAIL, loanApplication.getEmail());
        assertEquals(AMOUNT_50000, loanApplication.getAmount());
        assertEquals(TERM_24, loanApplication.getTerm());
        
        assertNotNull(loanApplication.getStatus());
        assertEquals(STATUS_ID_1, loanApplication.getStatus().getId());
        assertEquals(PENDING_STATUS, loanApplication.getStatus().getName());
        assertEquals(PENDING_DESCRIPTION, loanApplication.getStatus().getDescription());
        
        assertNotNull(loanApplication.getLoanType());
        assertEquals(LOAN_TYPE_ID_1, loanApplication.getLoanType().getId());
        assertEquals(PERSONAL_LOAN, loanApplication.getLoanType().getName());
        assertEquals(MIN_AMOUNT, loanApplication.getLoanType().getMinAmount());
        assertEquals(MAX_AMOUNT, loanApplication.getLoanType().getMaxAmount());
        assertEquals(MIN_TERM, loanApplication.getLoanType().getMinTerm());
        assertEquals(MAX_TERM, loanApplication.getLoanType().getMaxTerm());
        assertEquals(INTEREST_RATE, loanApplication.getLoanType().getInterestRate());
        assertTrue(loanApplication.getLoanType().getAutomaticValidation());
    }

    @Test
    void shouldMapNullLoanApplicationToNull() {
        LoanRequestEntity entity = LoanApplicationMapper.INSTANCE.toEntity(null);
        assertNull(entity);
    }

    @Test
    void shouldMapNullEntityToNull() {
        LoanApplication loanApplication = LoanApplicationMapper.INSTANCE.toDomain((LoanRequestEntity) null);
        assertNull(loanApplication);
    }

    @Test
    void shouldMapNullDetailsEntityToNull() {
        LoanApplication loanApplication = LoanApplicationMapper.INSTANCE.toDomain((LoanRequestWithDetailsEntity) null);
        assertNull(loanApplication);
    }

    @Test
    void shouldMapLoanApplicationWithNullStatusAndLoanType() {
        String loanId = UUID.randomUUID().toString();
        LoanApplication loanApplication = LoanApplication.builder()
                .id(loanId)
                .email(CARLOS_EMAIL)
                .amount(AMOUNT_50000)
                .term(TERM_24)
                .status(null)
                .loanType(null)
                .build();

        LoanRequestEntity entity = LoanApplicationMapper.INSTANCE.toEntity(loanApplication);

        assertNotNull(entity);
        assertEquals(loanId, entity.getRequestId());
        assertEquals(CARLOS_EMAIL, entity.getEmail());
        assertEquals(AMOUNT_50000, entity.getAmount());
        assertEquals(TERM_24, entity.getTerm());
        assertNull(entity.getStatusId());
        assertNull(entity.getLoanTypeId());
    }
}