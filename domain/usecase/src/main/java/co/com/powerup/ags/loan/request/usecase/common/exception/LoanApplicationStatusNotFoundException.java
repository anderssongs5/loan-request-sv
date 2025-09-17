package co.com.powerup.ags.loan.request.usecase.common.exception;

public class LoanApplicationStatusNotFoundException extends BusinessException {
    
    public LoanApplicationStatusNotFoundException(String message) {
        super(message);
    }
}
