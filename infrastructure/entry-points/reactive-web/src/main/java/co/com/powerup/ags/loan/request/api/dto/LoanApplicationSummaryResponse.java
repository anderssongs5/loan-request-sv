package co.com.powerup.ags.loan.request.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanApplicationSummaryResponse {
    
    private String id;
    private BigDecimal amount;
    private Integer term;
    private String email;
    private String name;
    private String loanType;
    private BigDecimal interestRate;
    private String requestStatus;
    private BigDecimal baseSalary;
    private BigDecimal monthlyPaymentAmount;
}