package co.com.powerup.ags.loan.request.usecase.common.exception;

public class LoanApplicationNotFoundException extends BusinessException {
    
    public LoanApplicationNotFoundException(String message) {
        super(message);
    }
}
