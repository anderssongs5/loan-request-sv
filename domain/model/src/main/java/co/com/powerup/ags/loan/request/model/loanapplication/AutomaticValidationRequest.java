package co.com.powerup.ags.loan.request.model.loanapplication;

import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class AutomaticValidationRequest {
    
    private String loanApplicationId;
    private String userEmail;
    private String userIdNumber;
    private BigDecimal loanAmount;
    private Integer termInMonths;
    private BigDecimal interestRate;
    private BigDecimal userBaseSalary;
    private List<ApprovedLoan> approvedLoans;
}