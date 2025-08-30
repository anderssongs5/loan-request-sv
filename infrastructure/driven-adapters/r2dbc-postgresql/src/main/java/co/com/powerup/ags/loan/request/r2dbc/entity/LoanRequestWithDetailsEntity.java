package co.com.powerup.ags.loan.request.r2dbc.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanRequestWithDetailsEntity {
    
    // Loan Request fields
    private String requestId;
    private BigDecimal amount;
    private Integer term;
    private String email;
    
    // Status fields
    private Integer statusId;
    private String statusName;
    private String statusDescription;
    
    // Loan Type fields
    private Integer loanTypeId;
    private String typeName;
    private BigDecimal minimumAmount;
    private BigDecimal maximumAmount;
    private Integer minimumTerm;
    private Integer maximumTerm;
    private BigDecimal interestRate;
    private Boolean automaticValidation;
}