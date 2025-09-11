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

import java.util.List;
import java.util.Optional;
import java.util.Set;

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
    
    @Override
    public Flux<LoanApplication> getLoanApplicationsPageableByStatuses(Set<String> statuses, Integer page, Integer size, String sortBy, String sortDirection) {
        log.info("Retrieving loan applications by statuses: {}, page: {}, size: {}, sortBy: {}, sortDirection: {}", 
                statuses, page, size, sortBy, sortDirection);
        
        page = Optional.ofNullable(page).orElse(0);
        size = Optional.ofNullable(size).orElse(10);
        sortDirection = Optional.ofNullable(sortDirection).orElse("asc").toUpperCase();
        Integer offset = page * size;

        List<String> allowedSortFields = List.of("email", "term", "amount", "status");
        if (sortBy == null || !allowedSortFields.contains(sortBy)) {
            sortBy = "request_id";
        }
        
        return repository.findByStatusesPageable(statuses, sortBy, sortDirection, size, offset)
                .doOnSubscribe(subscription -> log.debug("Starting to fetch loan requests with pagination from database"))
                .doOnComplete(() -> log.info("Successfully retrieved paginated loan requests"))
                .doOnError(error -> log.error("Error retrieving paginated loan requests", error))
                .map(LoanApplicationMapper.INSTANCE::toDomain);
    }
    
    @Override
    public Mono<Long> countLoanApplicationsByStatuses(Set<String> statuses) {
        log.info("Counting loan applications by statuses: {}", statuses);
        
        return repository.countByStatuses(statuses)
                .doOnSubscribe(subscription -> log.debug("Starting to count loan requests by statuses"))
                .doOnNext(count -> log.info("Found {} loan applications matching statuses: {}", count, statuses))
                .doOnError(error -> log.error("Error counting loan applications by statuses", error));
    }
}
