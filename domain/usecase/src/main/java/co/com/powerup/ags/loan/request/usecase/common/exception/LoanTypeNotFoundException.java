package co.com.powerup.ags.loan.request.usecase.common.exception;

public class LoanTypeNotFoundException extends BusinessException {
    
    public LoanTypeNotFoundException(String message) {
        super(message);
    }
}
