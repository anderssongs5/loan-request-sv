package co.com.powerup.ags.loan.request.r2dbc;

import co.com.powerup.ags.loan.request.model.loanapplicationstatus.LoanApplicationStatus;
import co.com.powerup.ags.loan.request.r2dbc.entity.LoanRequestStatusEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.reactivecommons.utils.ObjectMapper;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoanRequestStatusReactiveRepositoryAdapterTest {

    private static final Integer STATUS_ID_1 = 1;
    private static final String PENDING_STATUS = "PENDING";
    private static final String PENDING_DESCRIPTION = "Pending review";

    @Mock
    private LoanRequestStatusReactiveRepository repository;

    @Mock
    private ObjectMapper objectMapper;

    private LoanRequestStatusReactiveRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new LoanRequestStatusReactiveRepositoryAdapter(repository, objectMapper);
    }

    @Test
    void shouldGetStatusByIdSuccessfully() {
        LoanRequestStatusEntity statusEntity = LoanRequestStatusEntity.builder()
                .id(STATUS_ID_1)
                .name(PENDING_STATUS)
                .description(PENDING_DESCRIPTION)
                .build();

        LoanApplicationStatus expectedStatus = LoanApplicationStatus.builder()
                .id(STATUS_ID_1)
                .name(PENDING_STATUS)
                .description(PENDING_DESCRIPTION)
                .build();

        when(repository.findById(STATUS_ID_1))
                .thenReturn(Mono.just(statusEntity));
        when(objectMapper.map(statusEntity, LoanApplicationStatus.class))
                .thenReturn(expectedStatus);

        Mono<LoanApplicationStatus> result = adapter.getById(STATUS_ID_1);

        StepVerifier.create(result)
                .expectNextMatches(status -> 
                    status.getId().equals(STATUS_ID_1) &&
                    status.getName().equals(PENDING_STATUS) &&
                    status.getDescription().equals(PENDING_DESCRIPTION)
                )
                .verifyComplete();
    }

    @Test
    void shouldReturnEmptyWhenStatusNotFoundById() {
        when(repository.findById(STATUS_ID_1))
                .thenReturn(Mono.empty());

        Mono<LoanApplicationStatus> result = adapter.getById(STATUS_ID_1);

        StepVerifier.create(result)
                .verifyComplete();
    }

    @Test
    void shouldHandleErrorWhenGettingStatusById() {
        RuntimeException error = new RuntimeException("Database error");

        when(repository.findById(STATUS_ID_1))
                .thenReturn(Mono.error(error));

        Mono<LoanApplicationStatus> result = adapter.getById(STATUS_ID_1);

        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();
    }

    @Test
    void shouldGetStatusByNameSuccessfully() {
        LoanRequestStatusEntity statusEntity = LoanRequestStatusEntity.builder()
                .id(STATUS_ID_1)
                .name(PENDING_STATUS)
                .description(PENDING_DESCRIPTION)
                .build();

        LoanApplicationStatus expectedStatus = LoanApplicationStatus.builder()
                .id(STATUS_ID_1)
                .name(PENDING_STATUS)
                .description(PENDING_DESCRIPTION)
                .build();

        LoanRequestStatusEntity searchEntity = LoanRequestStatusEntity.builder()
                .name(PENDING_STATUS)
                .build();

        when(objectMapper.map(any(LoanApplicationStatus.class), eq(LoanRequestStatusEntity.class)))
                .thenReturn(searchEntity);
        when(repository.findAll(any()))
                .thenReturn(Flux.just(statusEntity));
        when(objectMapper.map(statusEntity, LoanApplicationStatus.class))
                .thenReturn(expectedStatus);

        Mono<LoanApplicationStatus> result = adapter.getByName(PENDING_STATUS);

        StepVerifier.create(result)
                .expectNextMatches(status -> 
                    status.getId().equals(STATUS_ID_1) &&
                    status.getName().equals(PENDING_STATUS) &&
                    status.getDescription().equals(PENDING_DESCRIPTION)
                )
                .verifyComplete();
    }

    @Test
    void shouldReturnEmptyWhenStatusNotFoundByName() {
        LoanRequestStatusEntity searchEntity = LoanRequestStatusEntity.builder()
                .name(PENDING_STATUS)
                .build();

        when(objectMapper.map(any(LoanApplicationStatus.class), eq(LoanRequestStatusEntity.class)))
                .thenReturn(searchEntity);
        when(repository.findAll(any()))
                .thenReturn(Flux.empty());

        Mono<LoanApplicationStatus> result = adapter.getByName(PENDING_STATUS);

        StepVerifier.create(result)
                .verifyComplete();
    }

    @Test
    void shouldHandleErrorWhenGettingStatusByName() {
        RuntimeException error = new RuntimeException("Database connection error");

        LoanRequestStatusEntity searchEntity = LoanRequestStatusEntity.builder()
                .name(PENDING_STATUS)
                .build();
        
        when(objectMapper.map(any(LoanApplicationStatus.class), eq(LoanRequestStatusEntity.class)))
                .thenReturn(searchEntity);
        when(repository.findAll(any()))
                .thenReturn(Flux.error(error));

        Mono<LoanApplicationStatus> result = adapter.getByName(PENDING_STATUS);

        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();
    }

    // Additional tests for getByNames method used in PUT flow

    @Test
    void shouldGetStatusesByNamesSuccessfully() {
        java.util.Set<String> statusNames = java.util.Set.of("APPROVED", "REJECTED");
        
        LoanRequestStatusEntity approvedEntity = LoanRequestStatusEntity.builder()
                .id(3)
                .name("APPROVED")
                .description("Approved")
                .build();
                
        LoanRequestStatusEntity rejectedEntity = LoanRequestStatusEntity.builder()
                .id(4)
                .name("REJECTED")
                .description("Rejected")
                .build();

        when(repository.findByNameIn(statusNames))
                .thenReturn(Flux.just(approvedEntity, rejectedEntity));

        Flux<LoanApplicationStatus> result = adapter.getByNames(statusNames);

        StepVerifier.create(result)
                .expectNextMatches(status -> 
                    status.getId().equals(3) &&
                    status.getName().equals("APPROVED")
                )
                .expectNextMatches(status -> 
                    status.getId().equals(4) &&
                    status.getName().equals("REJECTED")
                )
                .verifyComplete();
    }

    @Test
    void shouldReturnEmptyFluxWhenStatusesNotFoundByNames() {
        java.util.Set<String> nonExistentNames = java.util.Set.of("INVALID_STATUS_1", "INVALID_STATUS_2");

        when(repository.findByNameIn(nonExistentNames))
                .thenReturn(Flux.empty());

        Flux<LoanApplicationStatus> result = adapter.getByNames(nonExistentNames);

        StepVerifier.create(result)
                .verifyComplete();
    }

    @Test
    void shouldHandleErrorWhenGettingStatusesByNames() {
        java.util.Set<String> statusNames = java.util.Set.of("APPROVED", "REJECTED");
        RuntimeException error = new RuntimeException("Database query error");

        when(repository.findByNameIn(statusNames))
                .thenReturn(Flux.error(error));

        Flux<LoanApplicationStatus> result = adapter.getByNames(statusNames);

        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();
    }

    @Test
    void shouldGetStatusesByEmptyNamesSet() {
        java.util.Set<String> emptyNames = java.util.Set.of();

        when(repository.findByNameIn(emptyNames))
                .thenReturn(Flux.empty());

        Flux<LoanApplicationStatus> result = adapter.getByNames(emptyNames);

        StepVerifier.create(result)
                .verifyComplete();
    }

    @Test
    void shouldGetStatusesByNamesForNotificationScenario() {
        // Specific test for the PUT update flow notification scenario
        java.util.Set<String> notificationStatuses = java.util.Set.of("APPROVED", "REJECTED");
        
        LoanRequestStatusEntity approvedEntity = LoanRequestStatusEntity.builder()
                .id(3)
                .name("APPROVED")
                .description("Loan application approved")
                .build();
                
        LoanRequestStatusEntity rejectedEntity = LoanRequestStatusEntity.builder()
                .id(4)
                .name("REJECTED")  
                .description("Loan application rejected")
                .build();

        when(repository.findByNameIn(notificationStatuses))
                .thenReturn(Flux.just(approvedEntity, rejectedEntity));

        Flux<LoanApplicationStatus> result = adapter.getByNames(notificationStatuses);

        StepVerifier.create(result)
                .expectNextMatches(status -> {
                    return status.getId().equals(3) &&
                           status.getName().equals("APPROVED") &&
                           status.getDescription().equals("Loan application approved");
                })
                .expectNextMatches(status -> {
                    return status.getId().equals(4) &&
                           status.getName().equals("REJECTED") &&
                           status.getDescription().equals("Loan application rejected");
                })
                .verifyComplete();
    }

    @Test
    void shouldGetAllStatusesSuccessfully() {
        LoanRequestStatusEntity pendingEntity = LoanRequestStatusEntity.builder()
                .id(1)
                .name("PENDING")
                .description("Pending review")
                .build();
                
        LoanRequestStatusEntity approvedEntity = LoanRequestStatusEntity.builder()
                .id(3)
                .name("APPROVED")
                .description("Approved")
                .build();

        LoanApplicationStatus pendingStatus = LoanApplicationStatus.builder()
                .id(1)
                .name("PENDING")
                .description("Pending review")
                .build();
                
        LoanApplicationStatus approvedStatus = LoanApplicationStatus.builder()
                .id(3)
                .name("APPROVED")
                .description("Approved")
                .build();

        when(repository.findAll())
                .thenReturn(Flux.just(pendingEntity, approvedEntity));
        when(objectMapper.map(pendingEntity, LoanApplicationStatus.class))
                .thenReturn(pendingStatus);
        when(objectMapper.map(approvedEntity, LoanApplicationStatus.class))
                .thenReturn(approvedStatus);

        Flux<LoanApplicationStatus> result = adapter.getAll();

        StepVerifier.create(result)
                .expectNext(pendingStatus)
                .expectNext(approvedStatus)
                .verifyComplete();
    }

    @Test
    void shouldReturnEmptyFluxWhenNoStatusesExistForGetAll() {
        when(repository.findAll())
                .thenReturn(Flux.empty());

        Flux<LoanApplicationStatus> result = adapter.getAll();

        StepVerifier.create(result)
                .verifyComplete();
    }

    @Test
    void shouldHandleErrorWhenGettingAllStatuses() {
        RuntimeException error = new RuntimeException("Database connection lost");

        when(repository.findAll())
                .thenReturn(Flux.error(error));

        Flux<LoanApplicationStatus> result = adapter.getAll();

        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();
    }
}