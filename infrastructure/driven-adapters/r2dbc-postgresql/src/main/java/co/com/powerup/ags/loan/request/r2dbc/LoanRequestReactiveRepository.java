package co.com.powerup.ags.loan.request.r2dbc;

import co.com.powerup.ags.loan.request.r2dbc.entity.LoanRequestEntity;
import co.com.powerup.ags.loan.request.r2dbc.entity.LoanRequestWithDetailsEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.query.ReactiveQueryByExampleExecutor;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Set;

public interface LoanRequestReactiveRepository extends
        ReactiveCrudRepository<LoanRequestEntity, String>, ReactiveQueryByExampleExecutor<LoanRequestEntity> {

    @Query("""
        SELECT lr.request_id, lr.amount, lr.term, lr.email,
               ls.status_id, ls.name as status_name, ls.description as status_description,
               lt.loan_type_id, lt.name as type_name,
               lt.minimum_amount, lt.maximum_amount, lt.minimum_term, lt.maximum_term,
               lt.interest_rate, lt.automatic_validation
        FROM loan_requests lr
        JOIN loan_request_statuses ls ON lr.status_id = ls.status_id
        JOIN loan_types lt ON lr.loan_type_id = lt.loan_type_id
        """)
    Flux<LoanRequestWithDetailsEntity> findAllWithDetails();
    
    @Query("""
        SELECT * FROM (
            SELECT lr.request_id, lr.amount, lr.term, lr.email,
                   ls.status_id, ls.name as status_name, ls.description as status_description,
                   lt.loan_type_id, lt.name as type_name,
                   lt.minimum_amount, lt.maximum_amount, lt.minimum_term, lt.maximum_term,
                   lt.interest_rate, lt.automatic_validation
            FROM loan_requests lr
            JOIN loan_request_statuses ls ON lr.status_id = ls.status_id
            JOIN loan_types lt ON lr.loan_type_id = lt.loan_type_id
            WHERE ls.name IN (:statuses)
            ORDER BY
                CASE WHEN :sortBy = 'request_id' AND :sortDirection = 'ASC' THEN lr.request_id END ASC,
                CASE WHEN :sortBy = 'request_id' AND :sortDirection = 'DESC' THEN lr.request_id END DESC,
                CASE WHEN :sortBy = 'email' AND :sortDirection = 'ASC' THEN lr.email END ASC,
                CASE WHEN :sortBy = 'email' AND :sortDirection = 'DESC' THEN lr.email END DESC,
                CASE WHEN :sortBy = 'term' AND :sortDirection = 'ASC' THEN lr.term END ASC,
                CASE WHEN :sortBy = 'term' AND :sortDirection = 'DESC' THEN lr.term END DESC,
                CASE WHEN :sortBy = 'amount' AND :sortDirection = 'ASC' THEN lr.amount END ASC,
                CASE WHEN :sortBy = 'amount' AND :sortDirection = 'DESC' THEN lr.amount END DESC,
                CASE WHEN :sortBy = 'status_name' AND :sortDirection = 'ASC' THEN ls.name END ASC,
                CASE WHEN :sortBy = 'status_name' AND :sortDirection = 'DESC' THEN ls.name END DESC,
                CASE WHEN :sortBy NOT IN ('request_id', 'email', 'term', 'amount', 'status_name') AND :sortDirection = 'ASC' THEN lr.request_id END ASC,
                CASE WHEN :sortBy NOT IN ('request_id', 'email', 'term', 'amount', 'status_name') AND :sortDirection = 'DESC' THEN lr.request_id END DESC
        )
        LIMIT :size OFFSET :offset
        """)
    Flux<LoanRequestWithDetailsEntity> findByStatusesPageable(@Param("statuses") Set<String> statuses,
                                                              @Param("sortBy") String sortBy, 
                                                              @Param("sortDirection") String sortDirection,
                                                              @Param("size") Integer size, 
                                                              @Param("offset") Integer offset);
    
    @Query("""
        SELECT COUNT(lr.request_id)
        FROM loan_requests lr
        JOIN loan_request_statuses ls ON lr.status_id = ls.status_id
        WHERE ls.name IN (:statuses)
        """)
    Mono<Long> countByStatuses(@Param("statuses") Set<String> statuses);

    Flux<LoanRequestEntity> getAllByStatusIdIn(Set<Integer> statuses, Pageable pageable);
    
    Mono<Long> countByStatusIdIn(Set<Integer> statuses);
}
