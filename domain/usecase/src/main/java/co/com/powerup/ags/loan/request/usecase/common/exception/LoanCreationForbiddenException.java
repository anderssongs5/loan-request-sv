package co.com.powerup.ags.loan.request.usecase.common.exception;

public class LoanCreationForbiddenException extends BusinessException {
    
    public LoanCreationForbiddenException(String message) {
        super(message);
    }
}
