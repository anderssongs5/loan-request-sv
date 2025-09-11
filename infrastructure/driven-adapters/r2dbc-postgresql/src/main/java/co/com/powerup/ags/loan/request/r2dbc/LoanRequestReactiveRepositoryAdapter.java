package co.com.powerup.ags.loan.request.r2dbc;

import co.com.powerup.ags.loan.request.model.loanapplication.LoanApplication;
import co.com.powerup.ags.loan.request.model.loanapplication.gateways.LoanApplicationRepository;
import co.com.powerup.ags.loan.request.r2dbc.entity.LoanRequestEntity;
import co.com.powerup.ags.loan.request.r2dbc.helper.ReactiveAdapterOperations;
import co.com.powerup.ags.loan.request.r2dbc.mapper.LoanApplicationMapper;
import org.reactivecommons.utils.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public class LoanRequestReactiveRepositoryAdapter extends ReactiveAdapterOperations<
        LoanApplication,
        LoanRequestEntity,
        String,
        LoanRequestReactiveRepository
> implements LoanApplicationRepository {

    private static final Logger log = LoggerFactory.getLogger(LoanRequestReactiveRepositoryAdapter.class);
    private final TransactionalOperator transactionalOperator;

    public LoanRequestReactiveRepositoryAdapter(LoanRequestReactiveRepository repository, ObjectMapper mapper,
                                                TransactionalOperator transactionalOperator) {
        super(repository, mapper, d -> mapper.map(d, LoanApplication.class));
        this.transactionalOperator = transactionalOperator;
    }
    
    @Override
    public Mono<LoanApplication> createLoanRequest(LoanApplication loanApplication) {
        log.info("Creating loan request for email: {}, amount: {}, term: {}", 
                loanApplication.getEmail(), loanApplication.getAmount(), loanApplication.getTerm());
        
        LoanRequestEntity entity = LoanApplicationMapper.INSTANCE.toEntity(loanApplication);
        
        return repository.save(entity)
                .doOnSuccess(savedEntity -> log.info("Loan request created successfully with ID: {}", savedEntity.getRequestId()))
                .doOnError(error -> log.error("Error creating loan request for email: {}", loanApplication.getEmail(), error))
                .map(savedEntity -> {
                    LoanApplication mapped = LoanApplicationMapper.INSTANCE.toDomain(savedEntity);
                    return mapped.toBuilder()
                            .status(loanApplication.getStatus())
                            .loanType(loanApplication.getLoanType())
                            .build();
                }).as(transactionalOperator::transactional);
    }
    
    @Override
    public Flux<LoanApplication> getAllLoanRequests() {
        log.info("Retrieving all loan requests");
        
        return repository.findAllWithDetails()
                .doOnSubscribe(subscription -> log.debug("Starting to fetch loan requests from database"))
                .doOnComplete(() -> log.info("Successfully retrieved all loan requests"))
                .doOnError(error -> log.error("Error retrieving loan requests", error))
                .map(LoanApplicationMapper.INSTANCE::toDomain);
    }
}
