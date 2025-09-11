package co.com.powerup.ags.loan.request.usecase.loanapplication.exception;

public class LoanCreationForbiddenException extends BusinessException {
    
    public LoanCreationForbiddenException(String message) {
        super(message);
    }
}
