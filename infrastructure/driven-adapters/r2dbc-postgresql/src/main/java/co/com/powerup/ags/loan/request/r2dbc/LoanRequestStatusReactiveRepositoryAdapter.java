package co.com.powerup.ags.loan.request.r2dbc;

import co.com.powerup.ags.loan.request.model.loanapplicationstatus.LoanApplicationStatus;
import co.com.powerup.ags.loan.request.model.loanapplicationstatus.gateways.LoanApplicationStatusRepository;
import co.com.powerup.ags.loan.request.r2dbc.entity.LoanRequestStatusEntity;
import co.com.powerup.ags.loan.request.r2dbc.helper.ReactiveAdapterOperations;
import co.com.powerup.ags.loan.request.r2dbc.mapper.LoanApplicationStatusMapper;
import org.reactivecommons.utils.ObjectMapper;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Set;

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
    
    @Override
    public Flux<LoanApplicationStatus> getAll() {
        return super.findAll();
    }
    
    @Override
    public Flux<LoanApplicationStatus> getByNames(Set<String> names) {
        return this.repository.findByNameIn(names)
                .map(LoanApplicationStatusMapper.INSTANCE::toDomain);
    }
}
