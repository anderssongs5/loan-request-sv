package co.com.powerup.ags.loan.request.model.notification.gateway;

import co.com.powerup.ags.loan.request.model.loanapplication.LoanApplicationNotification;
import reactor.core.publisher.Mono;

public interface NotificationGateway {
    
    Mono<Void> notify(LoanApplicationNotification loanApplication);
}
