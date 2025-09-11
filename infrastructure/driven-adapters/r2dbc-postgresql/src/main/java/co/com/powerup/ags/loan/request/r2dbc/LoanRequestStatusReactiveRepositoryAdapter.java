package co.com.powerup.ags.loan.request.r2dbc;

import co.com.powerup.ags.loan.request.model.loanapplicationstatus.LoanApplicationStatus;
import co.com.powerup.ags.loan.request.model.loanapplicationstatus.gateways.LoanApplicationStatusRepository;
import co.com.powerup.ags.loan.request.r2dbc.entity.LoanRequestStatusEntity;
import co.com.powerup.ags.loan.request.r2dbc.helper.ReactiveAdapterOperations;
import org.reactivecommons.utils.ObjectMapper;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public class LoanRequestStatusReactiveRepositoryAdapter extends ReactiveAdapterOperations<
        LoanApplicationStatus,
        LoanRequestStatusEntity,
        Integer,
        LoanRequestStatusReactiveRepository
> implements LoanApplicationStatusRepository {
    public LoanRequestStatusReactiveRepositoryAdapter(LoanRequestStatusReactiveRepository repository, ObjectMapper mapper) {

        super(repository, mapper, d -> mapper.map(d, LoanApplicationStatus.class));
    }
    
    @Override
    public Mono<LoanApplicationStatus> getById(Integer id) {
        return super.findById(id);
    }
    
    @Override
    public Mono<LoanApplicationStatus> getByName(String name) {
        return super.findByExample(LoanApplicationStatus.builder().name(name).build())
                .next();
    }
}
