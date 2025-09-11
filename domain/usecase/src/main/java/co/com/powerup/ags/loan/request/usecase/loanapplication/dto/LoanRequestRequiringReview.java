package co.com.powerup.ags.loan.request.usecase.loanapplication.dto;

import co.com.powerup.ags.loan.request.model.loanapplication.LoanApplication;
import co.com.powerup.ags.loan.request.model.user.User;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Getter
@Setter
@NoArgsConstructor
public class LoanRequestRequiringReview {
    
    private String id;
    private BigDecimal amount;
    private Integer term;
    private String email;
    private String userName;
    private String userLastName;
    private String status;
    private String loanType;
    private BigDecimal interestRate;
    private BigDecimal baseSalary;
    private BigDecimal monthlyPaymentAmount;
    
    public LoanRequestRequiringReview(LoanApplication loanApplication, User user) {
        this.id = loanApplication.getId();
        this.amount = loanApplication.getAmount();
        this.term = loanApplication.getTerm();
        this.email = user.getEmail();
        this.userName = user.getName();
        this.userLastName = user.getLastName();
        this.status = loanApplication.getStatus().getName();
        this.loanType = loanApplication.getLoanType().getName();
        this.interestRate = loanApplication.getLoanType().getInterestRate();
        this.baseSalary = user.getBaseSalary();
        // (amount + (amount*interestRate))/term
        this.monthlyPaymentAmount = (amount.add(amount.multiply(this.interestRate)))
                .divide(BigDecimal.valueOf(this.term), 2, RoundingMode.HALF_EVEN);
    }
}
