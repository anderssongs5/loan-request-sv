package co.com.powerup.ags.loan.request.usecase.loanapplication.command;

import java.math.BigDecimal;

public class CreateLoanRequestCommand {
    
    private final String userIdNumber;
    private final Integer loanTypeId;
    private final BigDecimal amount;
    private final Integer term;
    
    private CreateLoanRequestCommand(Builder builder) {
        this.userIdNumber = builder.userIdNumber;
        this.loanTypeId = builder.loanTypeId;
        this.amount = builder.amount;
        this.term = builder.term;
    }
    
    public String getUserIdNumber() {
        return userIdNumber;
    }
    
    public Integer getLoanTypeId() {
        return loanTypeId;
    }
    
    public BigDecimal getAmount() {
        return amount;
    }
    
    public Integer getTerm() {
        return term;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String userIdNumber;
        private Integer loanTypeId;
        private BigDecimal amount;
        private Integer term;
        
        private Builder() {}
        
        public Builder userIdNumber(String userIdNumber) {
            this.userIdNumber = userIdNumber;
            return this;
        }
        
        public Builder loanTypeId(Integer loanTypeId) {
            this.loanTypeId = loanTypeId;
            return this;
        }
        
        public Builder amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }
        
        public Builder term(Integer term) {
            this.term = term;
            return this;
        }
        
        public CreateLoanRequestCommand build() {
            return new CreateLoanRequestCommand(this);
        }
    }
}