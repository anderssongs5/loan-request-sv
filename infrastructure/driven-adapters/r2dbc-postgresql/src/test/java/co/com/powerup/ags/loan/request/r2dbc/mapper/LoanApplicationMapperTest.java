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
        assertEquals(loanId, entity.getRequestId().toString());
        assertEquals(CARLOS_EMAIL, entity.getEmail());
        assertEquals(AMOUNT_50000, entity.getAmount());
        assertEquals(TERM_24, entity.getTerm());
        assertEquals(STATUS_ID_1, entity.getStatusId());
        assertEquals(LOAN_TYPE_ID_1, entity.getLoanTypeId());
    }

    @Test
    void shouldMapEntityToLoanApplication() {
        UUID loanId = UUID.randomUUID();
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
        assertEquals(loanId.toString(), loanApplication.getId());
        assertEquals(MARIA_EMAIL, loanApplication.getEmail());
        assertEquals(AMOUNT_75000, loanApplication.getAmount());
        assertEquals(TERM_36, loanApplication.getTerm());
        assertNotNull(loanApplication.getStatus());
        assertNotNull(loanApplication.getLoanType());
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
        assertEquals(loanId, entity.getRequestId().toString());
        assertEquals(CARLOS_EMAIL, entity.getEmail());
        assertEquals(AMOUNT_50000, entity.getAmount());
        assertEquals(TERM_24, entity.getTerm());
        assertNull(entity.getStatusId());
        assertNull(entity.getLoanTypeId());
    }

    // Additional tests for PUT update flow scenarios

    @Test
    void shouldMapEntityToLoanApplicationWithUUIDId() {
        UUID uuidId = UUID.fromString("29a4639d-b328-453a-a408-d2eff0bcae84");
        
        LoanRequestEntity entity = LoanRequestEntity.builder()
                .requestId(uuidId)
                .email(MARIA_EMAIL)
                .amount(AMOUNT_75000)
                .term(TERM_36)
                .statusId(3) // APPROVED
                .loanTypeId(LOAN_TYPE_ID_1)
                .build();

        LoanApplication loanApplication = LoanApplicationMapper.INSTANCE.toDomain(entity);

        assertNotNull(loanApplication);
        assertEquals(uuidId.toString(), loanApplication.getId());
        assertEquals(MARIA_EMAIL, loanApplication.getEmail());
        assertEquals(AMOUNT_75000, loanApplication.getAmount());
        assertEquals(TERM_36, loanApplication.getTerm());
        
        // Check that status and loan type have the correct IDs
        assertNotNull(loanApplication.getStatus());
        assertEquals(Integer.valueOf(3), loanApplication.getStatus().getId());
        
        assertNotNull(loanApplication.getLoanType());
        assertEquals(LOAN_TYPE_ID_1, loanApplication.getLoanType().getId());
    }

    @Test
    void shouldMapLoanApplicationToEntityForStatusUpdate() {
        String loanId = "f47ac10b-58cc-4372-a567-0e02b2c3d479";
        
        // Simulate updating from PENDING to APPROVED
        LoanApplicationStatus approvedStatus = LoanApplicationStatus.builder()
                .id(3)
                .name("APPROVED")
                .description("Approved")
                .build();

        LoanType loanType = LoanType.builder()
                .id(LOAN_TYPE_ID_1)
                .name(PERSONAL_LOAN)
                .build();

        LoanApplication loanApplication = LoanApplication.builder()
                .id(loanId)
                .email(CARLOS_EMAIL)
                .amount(AMOUNT_50000)
                .term(TERM_24)
                .status(approvedStatus)
                .loanType(loanType)
                .build();

        LoanRequestEntity entity = LoanApplicationMapper.INSTANCE.toEntity(loanApplication);

        assertNotNull(entity);
        assertEquals(loanId, entity.getRequestId().toString());
        assertEquals(CARLOS_EMAIL, entity.getEmail());
        assertEquals(AMOUNT_50000, entity.getAmount());
        assertEquals(TERM_24, entity.getTerm());
        assertEquals(Integer.valueOf(3), entity.getStatusId()); // APPROVED status
        assertEquals(LOAN_TYPE_ID_1, entity.getLoanTypeId());
    }

    @Test
    void shouldMapLoanApplicationToEntityForRejectedStatus() {
        String loanId = "550e8400-e29b-41d4-a716-446655440000";
        
        // Simulate updating to REJECTED
        LoanApplicationStatus rejectedStatus = LoanApplicationStatus.builder()
                .id(4)
                .name("REJECTED")
                .description("Rejected")
                .build();

        LoanType loanType = LoanType.builder()
                .id(2) // Different loan type
                .name("Mortgage")
                .build();

        LoanApplication loanApplication = LoanApplication.builder()
                .id(loanId)
                .email(MARIA_EMAIL)
                .amount(AMOUNT_75000)
                .term(TERM_36)
                .status(rejectedStatus)
                .loanType(loanType)
                .build();

        LoanRequestEntity entity = LoanApplicationMapper.INSTANCE.toEntity(loanApplication);

        assertNotNull(entity);
        assertEquals(loanId, entity.getRequestId().toString());
        assertEquals(MARIA_EMAIL, entity.getEmail());
        assertEquals(AMOUNT_75000, entity.getAmount());
        assertEquals(TERM_36, entity.getTerm());
        assertEquals(Integer.valueOf(4), entity.getStatusId()); // REJECTED status
        assertEquals(Integer.valueOf(2), entity.getLoanTypeId()); // Different loan type
    }

    @Test
    void shouldHandlePartialEntityMapping() {
        String loanId = "123e4567-e89b-12d3-a456-426614174000";
        
        // Entity with only basic fields (might happen in some DB scenarios)
        LoanRequestEntity entity = LoanRequestEntity.builder()
                .requestId(UUID.fromString(loanId))
                .email(CARLOS_EMAIL)
                .amount(null) // Missing amount
                .term(TERM_24)
                .statusId(null) // Missing status
                .loanTypeId(LOAN_TYPE_ID_1)
                .build();

        LoanApplication loanApplication = LoanApplicationMapper.INSTANCE.toDomain(entity);

        assertNotNull(loanApplication);
        assertEquals(loanId, loanApplication.getId());
        assertEquals(CARLOS_EMAIL, loanApplication.getEmail());
        assertNull(loanApplication.getAmount()); // Should be null
        assertEquals(TERM_24, loanApplication.getTerm());
        
        // Status should have null ID but not be null object
        assertNotNull(loanApplication.getStatus());
        assertNull(loanApplication.getStatus().getId());
        
        // LoanType should have the correct ID
        assertNotNull(loanApplication.getLoanType());
        assertEquals(LOAN_TYPE_ID_1, loanApplication.getLoanType().getId());
    }

    @Test
    void shouldMapBidirectionallyConsistently() {
        // Test that mapping to entity and back gives consistent results
        String loanId = "6ba7b810-9dad-11d1-80b4-00c04fd430c8";
        
        LoanApplicationStatus status = LoanApplicationStatus.builder()
                .id(3)
                .name("APPROVED")
                .description("Approved")
                .build();

        LoanType loanType = LoanType.builder()
                .id(LOAN_TYPE_ID_1)
                .name(PERSONAL_LOAN)
                .build();

        LoanApplication originalLoanApplication = LoanApplication.builder()
                .id(loanId)
                .email(CARLOS_EMAIL)
                .amount(AMOUNT_50000)
                .term(TERM_24)
                .status(status)
                .loanType(loanType)
                .build();

        // Map to entity and back
        LoanRequestEntity entity = LoanApplicationMapper.INSTANCE.toEntity(originalLoanApplication);
        LoanApplication mappedBackLoanApplication = LoanApplicationMapper.INSTANCE.toDomain(entity);

        // Verify consistency
        assertNotNull(mappedBackLoanApplication);
        assertEquals(originalLoanApplication.getId(), mappedBackLoanApplication.getId());
        assertEquals(originalLoanApplication.getEmail(), mappedBackLoanApplication.getEmail());
        assertEquals(originalLoanApplication.getAmount(), mappedBackLoanApplication.getAmount());
        assertEquals(originalLoanApplication.getTerm(), mappedBackLoanApplication.getTerm());
        assertEquals(originalLoanApplication.getStatus().getId(), mappedBackLoanApplication.getStatus().getId());
        assertEquals(originalLoanApplication.getLoanType().getId(), mappedBackLoanApplication.getLoanType().getId());
    }
}