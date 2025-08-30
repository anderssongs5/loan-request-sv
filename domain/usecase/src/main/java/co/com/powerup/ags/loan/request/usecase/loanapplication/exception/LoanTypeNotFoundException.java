package co.com.powerup.ags.loan.request.usecase.loanapplication.exception;

public class LoanTypeNotFoundException extends BusinessException {
    
    public LoanTypeNotFoundException(String message) {
        super(message);
    }
}
