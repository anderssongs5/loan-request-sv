package co.com.powerup.ags.loan.request.model.notification.gateway;

import co.com.powerup.ags.loan.request.model.loanapplication.LoanApplicationWithUser;
import reactor.core.publisher.Mono;

public interface NotificationGateway {
    
    Mono<Void> notify(LoanApplicationWithUser loanApplication);
}
