package co.com.powerup.ags.loan.request.model.loanapplication.gateways;

import co.com.powerup.ags.loan.request.model.loanapplication.LoanApplication;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface LoanApplicationRepository {
    
    Mono<LoanApplication> createLoanRequest(LoanApplication loanApplication);
    
    Flux<LoanApplication> getAllLoanRequests();
}
