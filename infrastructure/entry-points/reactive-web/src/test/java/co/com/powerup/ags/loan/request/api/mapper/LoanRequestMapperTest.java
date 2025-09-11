package co.com.powerup.ags.loan.request.api.mapper;

import co.com.powerup.ags.loan.request.api.dto.CreateLoanRequestDto;
import co.com.powerup.ags.loan.request.api.dto.LoanApplicationSummaryResponse;
import co.com.powerup.ags.loan.request.api.dto.LoanRequestResponseDto;
import co.com.powerup.ags.loan.request.model.loanapplication.LoanApplication;
import co.com.powerup.ags.loan.request.usecase.loanapplication.dto.LoanRequestRequiringReview;
import co.com.powerup.ags.loan.request.model.loanapplicationstatus.LoanApplicationStatus;
import co.com.powerup.ags.loan.request.model.loantype.LoanType;
import co.com.powerup.ags.loan.request.model.user.User;
import co.com.powerup.ags.loan.request.usecase.loanapplication.dto.CreateLoanRequestCommand;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class LoanRequestMapperTest {
    
    private final String TEST_TOKEN = "ey.....";

    @Test
    void shouldMapCreateLoanRequestDtoToCommand() {
        CreateLoanRequestDto dto = CreateLoanRequestDto.builder()
                .userIdNumber("12345678")
                .loanTypeId(1)
                .amount(new BigDecimal("50000.00"))
                .term(24)
                .build();

        CreateLoanRequestCommand command = LoanRequestMapper.INSTANCE.toCommand(dto, TEST_TOKEN);

        assertNotNull(command);
        assertEquals("12345678", command.getUserIdNumber());
        assertEquals(Integer.valueOf(1), command.getLoanTypeId());
        assertEquals(new BigDecimal("50000.00"), command.getAmount());
        assertEquals(Integer.valueOf(24), command.getTerm());
        assertEquals(TEST_TOKEN, command.getCreatedBy());
    }

    @Test
    void shouldMapCreateLoanRequestDtoToCommandWithNullValues() {
        CreateLoanRequestDto dto = CreateLoanRequestDto.builder()
                .userIdNumber(null)
                .loanTypeId(null)
                .amount(null)
                .term(null)
                .build();

        CreateLoanRequestCommand command = LoanRequestMapper.INSTANCE.toCommand(dto, null);

        assertNotNull(command);
        assertNull(command.getUserIdNumber());
        assertNull(command.getLoanTypeId());
        assertNull(command.getAmount());
        assertNull(command.getTerm());
        assertNull(command.getCreatedBy());
    }

    @Test
    void shouldMapLoanApplicationToResponseDto() {
        String loanId = UUID.randomUUID().toString();
        
        LoanApplicationStatus status = LoanApplicationStatus.builder()
                .id(1)
                .name("PENDING")
                .description("Pending review")
                .build();

        LoanType loanType = LoanType.builder()
                .id(2)
                .name("Personal Loan")
                .minAmount(new BigDecimal("1000.00"))
                .maxAmount(new BigDecimal("100000.00"))
                .minTerm(6)
                .maxTerm(60)
                .interestRate(new BigDecimal("12.5"))
                .automaticValidation(true)
                .build();

        LoanApplication loanApplication = LoanApplication.builder()
                .id(loanId)
                .email("carlos.rodriguez@example.com")
                .amount(new BigDecimal("75000.00"))
                .term(36)
                .status(status)
                .loanType(loanType)
                .build();

        LoanRequestResponseDto responseDto = LoanRequestMapper.INSTANCE.toResponseDto(loanApplication);

        assertNotNull(responseDto);
        assertEquals(loanId, responseDto.getId());
        assertEquals("carlos.rodriguez@example.com", responseDto.getEmail());
        assertEquals(Integer.valueOf(36), responseDto.getTerm());
        assertEquals(new BigDecimal("75000.00"), responseDto.getAmount());
        assertEquals(Integer.valueOf(1), responseDto.getLoanStatusId());
        assertEquals(2, responseDto.getLoanTypeId());
    }

    @Test
    void shouldMapLoanApplicationToResponseDtoWithNullStatus() {
        String loanId = UUID.randomUUID().toString();
        
        LoanType loanType = LoanType.builder()
                .id(1)
                .name("Personal Loan")
                .build();

        LoanApplication loanApplication = LoanApplication.builder()
                .id(loanId)
                .email("maria.fernandez@example.com")
                .amount(new BigDecimal("50000.00"))
                .term(24)
                .status(null)
                .loanType(loanType)
                .build();

        LoanRequestResponseDto responseDto = LoanRequestMapper.INSTANCE.toResponseDto(loanApplication);

        assertNotNull(responseDto);
        assertEquals(loanId, responseDto.getId());
        assertEquals("maria.fernandez@example.com", responseDto.getEmail());
        assertEquals(Integer.valueOf(24), responseDto.getTerm());
        assertEquals(new BigDecimal("50000.00"), responseDto.getAmount());
        assertNull(responseDto.getLoanStatusId());
        assertEquals(1, responseDto.getLoanTypeId());
    }

    @Test
    void shouldMapLoanApplicationToResponseDtoWithNullLoanType() {
        String loanId = UUID.randomUUID().toString();
        
        LoanApplicationStatus status = LoanApplicationStatus.builder()
                .id(1)
                .name("PENDING")
                .build();

        LoanApplication loanApplication = LoanApplication.builder()
                .id(loanId)
                .email("antonio.garcia@example.com")
                .amount(new BigDecimal("50000.00"))
                .term(24)
                .status(status)
                .loanType(null)
                .build();

        LoanRequestResponseDto responseDto = LoanRequestMapper.INSTANCE.toResponseDto(loanApplication);

        assertNotNull(responseDto);
        assertEquals(loanId, responseDto.getId());
        assertEquals("antonio.garcia@example.com", responseDto.getEmail());
        assertEquals(Integer.valueOf(24), responseDto.getTerm());
        assertEquals(new BigDecimal("50000.00"), responseDto.getAmount());
        assertEquals(Integer.valueOf(1), responseDto.getLoanStatusId());
        assertNull(responseDto.getLoanTypeId());
    }

    @Test
    void shouldMapLoanApplicationToResponseDtoWithAllNullNestedObjects() {
        String loanId = UUID.randomUUID().toString();
        
        LoanApplication loanApplication = LoanApplication.builder()
                .id(loanId)
                .email("lucia.martinez@example.com")
                .amount(new BigDecimal("50000.00"))
                .term(24)
                .status(null)
                .loanType(null)
                .build();

        LoanRequestResponseDto responseDto = LoanRequestMapper.INSTANCE.toResponseDto(loanApplication);

        assertNotNull(responseDto);
        assertEquals(loanId, responseDto.getId());
        assertEquals("lucia.martinez@example.com", responseDto.getEmail());
        assertEquals(Integer.valueOf(24), responseDto.getTerm());
        assertEquals(new BigDecimal("50000.00"), responseDto.getAmount());
        assertNull(responseDto.getLoanStatusId());
        assertNull(responseDto.getLoanTypeId());
    }

    @Test
    void shouldHandleNullLoanApplicationGracefully() {
        LoanRequestResponseDto responseDto = LoanRequestMapper.INSTANCE.toResponseDto(null);

        assertNull(responseDto);
    }

    @Test
    void shouldHandleNullCreateLoanRequestDtoGracefully() {
        CreateLoanRequestCommand command = LoanRequestMapper.INSTANCE.toCommand(null, null);

        assertNull(command);
    }
    
    @Test
    void shouldMapCreateLoanRequestDtoToCommandWithCreatedBy() {
        String createdBy = "john.doe@example.com";
        CreateLoanRequestDto dto = CreateLoanRequestDto.builder()
                .userIdNumber("12345678")
                .loanTypeId(1)
                .amount(new BigDecimal("50000.00"))
                .term(24)
                .build();

        CreateLoanRequestCommand command = LoanRequestMapper.INSTANCE.toCommand(dto, createdBy);

        assertNotNull(command);
        assertEquals("12345678", command.getUserIdNumber());
        assertEquals(Integer.valueOf(1), command.getLoanTypeId());
        assertEquals(new BigDecimal("50000.00"), command.getAmount());
        assertEquals(Integer.valueOf(24), command.getTerm());
        assertEquals(createdBy, command.getCreatedBy());
    }
    
    @Test
    void shouldMapCreateLoanRequestDtoToCommandWithCreatedByAndNullValues() {
        String createdBy = "jane.smith@example.com";
        CreateLoanRequestDto dto = CreateLoanRequestDto.builder()
                .userIdNumber(null)
                .loanTypeId(null)
                .amount(null)
                .term(null)
                .build();

        CreateLoanRequestCommand command = LoanRequestMapper.INSTANCE.toCommand(dto, createdBy);

        assertNotNull(command);
        assertNull(command.getUserIdNumber());
        assertNull(command.getLoanTypeId());
        assertNull(command.getAmount());
        assertNull(command.getTerm());
        assertEquals(createdBy, command.getCreatedBy());
    }
    
    @Test
    void shouldMapCreateLoanRequestDtoToCommandWithNullCreatedBy() {
        CreateLoanRequestDto dto = CreateLoanRequestDto.builder()
                .userIdNumber("87654321")
                .loanTypeId(2)
                .amount(new BigDecimal("25000.00"))
                .term(12)
                .build();

        CreateLoanRequestCommand command = LoanRequestMapper.INSTANCE.toCommand(dto, null);

        assertNotNull(command);
        assertEquals("87654321", command.getUserIdNumber());
        assertEquals(Integer.valueOf(2), command.getLoanTypeId());
        assertEquals(new BigDecimal("25000.00"), command.getAmount());
        assertEquals(Integer.valueOf(12), command.getTerm());
        assertNull(command.getCreatedBy());
    }

    @Test
    void shouldMapLoanRequestRequiringReviewToSummaryResponse() {
        String loanId = UUID.randomUUID().toString();
        String userId = UUID.randomUUID().toString();
        
        LoanApplicationStatus status = LoanApplicationStatus.builder()
                .id(1)
                .name("PENDING")
                .description("Pending review")
                .build();

        LoanType loanType = LoanType.builder()
                .id(2)
                .name("Personal Loan")
                .minAmount(new BigDecimal("1000.00"))
                .maxAmount(new BigDecimal("100000.00"))
                .minTerm(6)
                .maxTerm(60)
                .interestRate(new BigDecimal("12.5"))
                .automaticValidation(true)
                .build();

        LoanApplication loanApplication = LoanApplication.builder()
                .id(loanId)
                .email("jose.garcia@example.com")
                .amount(new BigDecimal("75000.00"))
                .term(36)
                .status(status)
                .loanType(loanType)
                .build();
                
        User user = User.builder()
                .id(userId)
                .name("José")
                .lastName("García")
                .email("jose.garcia@example.com")
                .baseSalary(new BigDecimal("5000.00"))
                .phoneNumber("+57300123456")
                .address("Calle 123 # 45-67, Bogotá")
                .birthDate(LocalDate.of(1985, 6, 15))
                .idNumber("12345678")
                .build();

        LoanRequestRequiringReview loanRequestRequiringReview = new LoanRequestRequiringReview(loanApplication, user);

        LoanApplicationSummaryResponse summaryResponse = LoanRequestMapper.INSTANCE.toSummaryResponse(loanRequestRequiringReview);

        assertNotNull(summaryResponse);
        assertEquals(loanId, summaryResponse.getId());
        assertEquals("jose.garcia@example.com", summaryResponse.getEmail());
        assertEquals("José García", summaryResponse.getName());
        assertEquals(new BigDecimal("75000.00"), summaryResponse.getAmount());
        assertEquals(Integer.valueOf(36), summaryResponse.getTerm());
        assertEquals("PENDING", summaryResponse.getRequestStatus());
        assertEquals("Personal Loan", summaryResponse.getLoanType());
        assertEquals(new BigDecimal("12.5"), summaryResponse.getInterestRate());
        assertEquals(new BigDecimal("5000.00"), summaryResponse.getBaseSalary());
        assertNotNull(summaryResponse.getMonthlyPaymentAmount());
    }
    
    @Test
    void shouldMapLoanRequestRequiringReviewToSummaryResponseWithSpanishNames() {
        String loanId = UUID.randomUUID().toString();
        String userId = UUID.randomUUID().toString();
        
        LoanApplicationStatus status = LoanApplicationStatus.builder()
                .id(2)
                .name("MANUAL_REVIEW")
                .description("Manual review required")
                .build();

        LoanType loanType = LoanType.builder()
                .id(1)
                .name("Préstamo Personal")
                .minAmount(new BigDecimal("5000.00"))
                .maxAmount(new BigDecimal("200000.00"))
                .minTerm(12)
                .maxTerm(84)
                .interestRate(new BigDecimal("15.75"))
                .automaticValidation(false)
                .build();

        LoanApplication loanApplication = LoanApplication.builder()
                .id(loanId)
                .email("maria.rodriguez@example.com")
                .amount(new BigDecimal("120000.00"))
                .term(48)
                .status(status)
                .loanType(loanType)
                .build();
                
        User user = User.builder()
                .id(userId)
                .name("María")
                .lastName("Rodríguez")
                .email("maria.rodriguez@example.com")
                .baseSalary(new BigDecimal("8500.00"))
                .phoneNumber("+57301987654")
                .address("Carrera 45 # 12-34, Medellín")
                .birthDate(LocalDate.of(1988, 3, 20))
                .idNumber("87654321")
                .build();

        LoanRequestRequiringReview loanRequestRequiringReview = new LoanRequestRequiringReview(loanApplication, user);

        LoanApplicationSummaryResponse summaryResponse = LoanRequestMapper.INSTANCE.toSummaryResponse(loanRequestRequiringReview);

        assertNotNull(summaryResponse);
        assertEquals(loanId, summaryResponse.getId());
        assertEquals("maria.rodriguez@example.com", summaryResponse.getEmail());
        assertEquals("María Rodríguez", summaryResponse.getName());
        assertEquals(new BigDecimal("120000.00"), summaryResponse.getAmount());
        assertEquals(Integer.valueOf(48), summaryResponse.getTerm());
        assertEquals("MANUAL_REVIEW", summaryResponse.getRequestStatus());
        assertEquals("Préstamo Personal", summaryResponse.getLoanType());
        assertEquals(new BigDecimal("15.75"), summaryResponse.getInterestRate());
        assertEquals(new BigDecimal("8500.00"), summaryResponse.getBaseSalary());
        assertNotNull(summaryResponse.getMonthlyPaymentAmount());
    }
    
    @Test
    void shouldHandleNullLoanRequestRequiringReviewGracefully() {
        LoanApplicationSummaryResponse summaryResponse = LoanRequestMapper.INSTANCE.toSummaryResponse(null);

        assertNull(summaryResponse);
    }
}