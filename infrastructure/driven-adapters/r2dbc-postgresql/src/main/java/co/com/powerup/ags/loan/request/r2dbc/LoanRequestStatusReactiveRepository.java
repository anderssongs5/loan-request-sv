package co.com.powerup.ags.loan.request.r2dbc;

import co.com.powerup.ags.loan.request.r2dbc.entity.LoanRequestStatusEntity;
import org.springframework.data.repository.query.ReactiveQueryByExampleExecutor;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

import java.util.Set;

public interface LoanRequestStatusReactiveRepository extends
        ReactiveCrudRepository<LoanRequestStatusEntity, Integer>, ReactiveQueryByExampleExecutor<LoanRequestStatusEntity> {

    Flux<LoanRequestStatusEntity> findByNameIn(Set<String> names);
}
