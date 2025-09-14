package co.com.powerup.ags.loan.request.sqs.sender.notification.exception;

public class NotificationErrorException extends RuntimeException {
    
    public NotificationErrorException(String message, Throwable ex) {
        super(message, ex);
    }
}
