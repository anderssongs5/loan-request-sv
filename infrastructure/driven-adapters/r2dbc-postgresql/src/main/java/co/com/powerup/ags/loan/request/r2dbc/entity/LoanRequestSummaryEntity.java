package co.com.powerup.ags.loan.request.r2dbc.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanRequestSummaryEntity {
    
    private String id;
    private String email;
    private String statusName;
    private BigDecimal amount;
    private Integer term;
    private BigDecimal interestRate;
}