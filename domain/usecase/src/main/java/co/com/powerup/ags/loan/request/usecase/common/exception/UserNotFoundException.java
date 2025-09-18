package co.com.powerup.ags.loan.request.usecase.common.exception;

public class UserNotFoundException extends BusinessException {
    
    public UserNotFoundException(String message) {
        super(message);
    }
}
