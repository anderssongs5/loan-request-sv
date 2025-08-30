package co.com.powerup.ags.loan.request.api.mapper;

import co.com.powerup.ags.loan.request.api.dto.CreateLoanRequestDto;
import co.com.powerup.ags.loan.request.api.dto.LoanRequestResponseDto;
import co.com.powerup.ags.loan.request.model.loanapplication.LoanApplication;
import co.com.powerup.ags.loan.request.model.loanapplicationstatus.LoanApplicationStatus;
import co.com.powerup.ags.loan.request.model.loantype.LoanType;
import co.com.powerup.ags.loan.request.usecase.loanapplication.command.CreateLoanRequestCommand;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class LoanRequestMapperTest {

    @Test
    void shouldMapCreateLoanRequestDtoToCommand() {
        CreateLoanRequestDto dto = CreateLoanRequestDto.builder()
                .userIdNumber("12345678")
                .loanTypeId(1)
                .amount(new BigDecimal("50000.00"))
                .term(24)
                .build();

        CreateLoanRequestCommand command = LoanRequestMapper.INSTANCE.toCommand(dto);

        assertNotNull(command);
        assertEquals("12345678", command.getUserIdNumber());
        assertEquals(Integer.valueOf(1), command.getLoanTypeId());
        assertEquals(new BigDecimal("50000.00"), command.getAmount());
        assertEquals(Integer.valueOf(24), command.getTerm());
    }

    @Test
    void shouldMapCreateLoanRequestDtoToCommandWithNullValues() {
        CreateLoanRequestDto dto = CreateLoanRequestDto.builder()
                .userIdNumber(null)
                .loanTypeId(null)
                .amount(null)
                .term(null)
                .build();

        CreateLoanRequestCommand command = LoanRequestMapper.INSTANCE.toCommand(dto);

        assertNotNull(command);
        assertNull(command.getUserIdNumber());
        assertNull(command.getLoanTypeId());
        assertNull(command.getAmount());
        assertNull(command.getTerm());
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
        assertEquals("2", responseDto.getLoanTypeId());
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
        assertEquals("1", responseDto.getLoanTypeId());
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
        CreateLoanRequestCommand command = LoanRequestMapper.INSTANCE.toCommand(null);

        assertNull(command);
    }
}