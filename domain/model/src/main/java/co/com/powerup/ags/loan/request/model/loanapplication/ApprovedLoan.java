package co.com.powerup.ags.loan.request.model.loanapplication;

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
@Builder(toBuilder = true)
public class ApprovedLoan {
    
    private BigDecimal amount;
    private Integer term;
    private BigDecimal interestRate;
}
