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
}