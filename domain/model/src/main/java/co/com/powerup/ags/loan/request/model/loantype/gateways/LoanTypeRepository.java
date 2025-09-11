package co.com.powerup.ags.loan.request.model.loantype.gateways;

import co.com.powerup.ags.loan.request.model.loantype.LoanType;
import reactor.core.publisher.Mono;

public interface LoanTypeRepository {
    
    Mono<LoanType> getById(Integer id);
}
