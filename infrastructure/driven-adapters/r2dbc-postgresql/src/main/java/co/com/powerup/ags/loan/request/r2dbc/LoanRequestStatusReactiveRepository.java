package co.com.powerup.ags.loan.request.r2dbc;

import co.com.powerup.ags.loan.request.r2dbc.entity.LoanRequestStatusEntity;
import org.springframework.data.repository.query.ReactiveQueryByExampleExecutor;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface LoanRequestStatusReactiveRepository extends
        ReactiveCrudRepository<LoanRequestStatusEntity, Integer>, ReactiveQueryByExampleExecutor<LoanRequestStatusEntity> {

}
