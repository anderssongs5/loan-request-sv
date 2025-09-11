package co.com.powerup.ags.loan.request.r2dbc;

import co.com.powerup.ags.loan.request.r2dbc.entity.LoanRequestEntity;
import co.com.powerup.ags.loan.request.r2dbc.entity.LoanRequestWithDetailsEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.query.ReactiveQueryByExampleExecutor;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
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
            CASE WHEN :sortDirection = 'ASC' THEN
                CASE :sortBy
                    WHEN 'amount' THEN lr.amount::text
                    WHEN 'term' THEN lr.term::text
                    WHEN 'email' THEN lr.email
                    WHEN 'status' THEN ls.name
                    ELSE lr.request_id::text
                END
            END ASC,
            CASE WHEN :sortDirection = 'DESC' THEN
                CASE :sortBy
                    WHEN 'amount' THEN lr.amount::text
                    WHEN 'term' THEN lr.term::text
                    WHEN 'email' THEN lr.email
                    WHEN 'status' THEN ls.name
                    ELSE lr.request_id::text
                END
            END DESC
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
}
