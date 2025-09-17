package co.com.powerup.ags.loan.request.model.loanapplication.gateways;

import co.com.powerup.ags.loan.request.model.loanapplication.AutomaticValidationRequest;
import reactor.core.publisher.Mono;

public interface AutomaticValidationGateway {
    
    Mono<Void> validateLoanApplicationDecision(AutomaticValidationRequest request);
}