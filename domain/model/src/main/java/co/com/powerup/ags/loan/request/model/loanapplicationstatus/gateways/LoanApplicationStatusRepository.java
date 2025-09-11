package co.com.powerup.ags.loan.request.model.loanapplicationstatus.gateways;

import co.com.powerup.ags.loan.request.model.loanapplicationstatus.LoanApplicationStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface LoanApplicationStatusRepository {
    
    Mono<LoanApplicationStatus> getById(Integer id);
    
    Mono<LoanApplicationStatus> getByName(String name);
    
    Flux<LoanApplicationStatus> getAll();
}
