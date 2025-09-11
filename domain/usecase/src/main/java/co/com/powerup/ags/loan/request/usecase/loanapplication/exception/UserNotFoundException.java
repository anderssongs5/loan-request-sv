package co.com.powerup.ags.loan.request.usecase.loanapplication.exception;

public class UserNotFoundException extends BusinessException {
    
    public UserNotFoundException(String message) {
        super(message);
    }
}
