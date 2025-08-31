package co.com.powerup.ags.loan.request.consumer.api;

import co.com.powerup.ags.loan.request.consumer.api.model.ErrorResponse;
import co.com.powerup.ags.loan.request.consumer.api.model.SuccessResponse;
import co.com.powerup.ags.loan.request.consumer.api.model.User;
import co.com.powerup.ags.loan.request.consumer.exception.UserServiceException;
import co.com.powerup.ags.loan.request.consumer.exception.UserValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriBuilder;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.function.Function;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsersApiTest {

    private static final String CARLOS_EMAIL = "carlos.rodriguez@ejemplo.com";
    private static final String MARIA_EMAIL = "maria.fernandez@ejemplo.com";
    private static final String CARLOS_NAME = "Carlos";
    private static final String CARLOS_LASTNAME = "Rodriguez";
    private static final String MARIA_NAME = "Maria";
    private static final String MARIA_LASTNAME = "Fernandez";
    private static final String VALID_USER_ID_1 = "12345678";
    private static final String VALID_USER_ID_2 = "87654321";
    private static final String INVALID_USER_ID = "99999999";
    private static final BigDecimal BASE_SALARY_5000 = new BigDecimal("5000.00");
    private static final BigDecimal BASE_SALARY_6000 = new BigDecimal("6000.00");
    private static final String ADDRESS_BOGOTA = "Calle 123, Bogota";
    private static final String ADDRESS_MEDELLIN = "Carrera 456, Medellin";
    private static final String PHONE_NUMBER_1 = "3001234567";
    private static final String PHONE_NUMBER_2 = "3009876543";
    private static final LocalDate BIRTH_DATE_1990 = LocalDate.of(1990, 1, 1);
    private static final LocalDate BIRTH_DATE_1985 = LocalDate.of(1985, 5, 15);
    private static final String SUCCESS_MESSAGE = "User retrieved successfully";
    private static final String API_PATH = "/api/v1/users/search";

    @Mock
    private WebClient webClient;
    
    @Mock
    private WebClient.RequestBodyUriSpec mockRequestBodyUriSpec;
    
    @Mock
    private WebClient.RequestBodyUriSpec mockRequestBodyUriSpec2;
    
    @Mock
    private WebClient.RequestBodySpec mockRequestBodySpec;
    
    @Mock
    private WebClient.ResponseSpec mockResponseSpec;

    @Mock
    private ClientResponse clientResponse;

    private UsersApi usersApi;

    @BeforeEach
    void setUp() {
        usersApi = new UsersApi(webClient);
    }

    @Test
    void shouldGetUserByIdNumberSuccessfully() {
        String userId = UUID.randomUUID().toString();
        User expectedUser = User.builder()
                .id(userId)
                .name(CARLOS_NAME)
                .lastName(CARLOS_LASTNAME)
                .email(CARLOS_EMAIL)
                .idNumber(VALID_USER_ID_1)
                .baseSalary(BASE_SALARY_5000)
                .address(ADDRESS_BOGOTA)
                .phoneNumber(PHONE_NUMBER_1)
                .birthDate(BIRTH_DATE_1990)
                .build();

        SuccessResponse<User> successResponse = new SuccessResponse<>();
        successResponse.setData(expectedUser);
        successResponse.setMessage(SUCCESS_MESSAGE);
        successResponse.setTimestamp(LocalDateTime.now());
        successResponse.setPath(API_PATH);

        when(webClient.method(HttpMethod.GET)).thenReturn(mockRequestBodyUriSpec);
        
        when(mockRequestBodyUriSpec.uri(any(Function.class))).thenAnswer(invocation -> {
            Function<UriBuilder, URI> uriFunction = invocation.getArgument(0);
            URI uri = uriFunction.apply(new DefaultUriBuilderFactory().builder());
            return mockRequestBodySpec;
        });
        
        
        when(mockRequestBodySpec.accept(any())).thenReturn(mockRequestBodyUriSpec2);
        
        when(mockRequestBodyUriSpec2.exchangeToMono(any())).thenAnswer(invocation -> {
            Function<ClientResponse, Mono<SuccessResponse<User>>> handler = invocation.getArgument(0);
            ClientResponse clientResponse = mock(ClientResponse.class);
            when(clientResponse.statusCode()).thenReturn(HttpStatus.OK);
            when(clientResponse.bodyToMono(any(ParameterizedTypeReference.class))).thenReturn(Mono.just(successResponse));
            return handler.apply(clientResponse);
        });

        Mono<SuccessResponse<User>> result = usersApi.getUserByIdNumberRequest(VALID_USER_ID_1);

        StepVerifier.create(result)
                .expectNextMatches(response ->
                    response.getData().getId().equals(userId) &&
                    response.getData().getName().equals(CARLOS_NAME) &&
                    response.getData().getEmail().equals(CARLOS_EMAIL) &&
                    response.getData().getIdNumber().equals(VALID_USER_ID_1)
                )
                .verifyComplete();
    }
    
    @Test
    void shouldReturnEmptyWhenUserNotFound() {
        when(webClient.method(HttpMethod.GET)).thenReturn(mockRequestBodyUriSpec);
        when(mockRequestBodyUriSpec.uri(any(Function.class))).thenReturn(mockRequestBodySpec);
        when(mockRequestBodySpec.accept(any())).thenReturn(mockRequestBodyUriSpec2);
        when(mockRequestBodyUriSpec2.exchangeToMono(any(Function.class))).thenAnswer(invocation -> {
            Function<ClientResponse, Mono<?>> handler = invocation.getArgument(0);
            ClientResponse clientResponse = mock(ClientResponse.class);
            when(clientResponse.statusCode()).thenReturn(HttpStatus.NOT_FOUND);
            return handler.apply(clientResponse);
        });
        
        Mono<SuccessResponse<User>> result = usersApi.getUserByIdNumberRequest(INVALID_USER_ID);
        
        StepVerifier.create(result)
                .expectNextCount(0)
                .verifyComplete();
    }
    
    @Test
    void shouldThrowUserValidationExceptionOn4xxClientError() {
        ErrorResponse errorResponse = ErrorResponse.builder().message("Invalid user id number.").build();
        
        when(webClient.method(HttpMethod.GET)).thenReturn(mockRequestBodyUriSpec);
        when(mockRequestBodyUriSpec.uri(any(Function.class))).thenReturn(mockRequestBodySpec);
        when(mockRequestBodySpec.accept(any())).thenReturn(mockRequestBodyUriSpec2);
        when(mockRequestBodyUriSpec2.exchangeToMono(any(Function.class))).thenAnswer(invocation -> {
            Function<ClientResponse, Mono<?>> handler = invocation.getArgument(0);
            ClientResponse clientResponse = mock(ClientResponse.class);
            when(clientResponse.statusCode()).thenReturn(HttpStatus.BAD_REQUEST);
            when(clientResponse.bodyToMono(ErrorResponse.class)).thenReturn(Mono.just(errorResponse));
            return handler.apply(clientResponse);
        });
        
        Mono<SuccessResponse<User>> result = usersApi.getUserByIdNumberRequest(INVALID_USER_ID);
        
        StepVerifier.create(result)
                .expectError(UserValidationException.class)
                .verify();
    }

    @Test
    void shouldThrowUserServiceExceptionOn5xxServerError() {
        when(webClient.method(HttpMethod.GET)).thenReturn(mockRequestBodyUriSpec);
        when(mockRequestBodyUriSpec.uri(any(Function.class))).thenReturn(mockRequestBodySpec);
        when(mockRequestBodySpec.accept(any())).thenReturn(mockRequestBodyUriSpec2);
        
        when(mockRequestBodyUriSpec2.exchangeToMono(any(Function.class))).thenAnswer(invocation -> {
            Function<ClientResponse, Mono<?>> handler = invocation.getArgument(0);
            ClientResponse clientResponse = mock(ClientResponse.class);
            when(clientResponse.statusCode()).thenReturn(HttpStatus.SERVICE_UNAVAILABLE);
            return handler.apply(clientResponse);
        });

        Mono<SuccessResponse<User>> result = usersApi.getUserByIdNumberRequest(VALID_USER_ID_1);

        StepVerifier.create(result)
                .expectError(UserServiceException.class)
                .verify();
    }

    @Test
    void shouldGetUserByIdSuccessfully() {
        String userId = UUID.randomUUID().toString();
        User expectedUser = User.builder()
                .id(userId)
                .name(MARIA_NAME)
                .lastName(MARIA_LASTNAME)
                .email(MARIA_EMAIL)
                .idNumber(VALID_USER_ID_2)
                .baseSalary(BASE_SALARY_6000)
                .address(ADDRESS_MEDELLIN)
                .phoneNumber(PHONE_NUMBER_2)
                .birthDate(BIRTH_DATE_1985)
                .build();

        SuccessResponse<User> successResponse = new SuccessResponse<>();
        successResponse.setData(expectedUser);
        successResponse.setMessage(SUCCESS_MESSAGE);
        
        when(webClient.method(HttpMethod.GET)).thenReturn(mockRequestBodyUriSpec);
        when(mockRequestBodyUriSpec.uri(eq("/api/v1/users/{id}"), eq(userId))).thenReturn(mockRequestBodySpec);
        when(mockRequestBodySpec.accept(any())).thenReturn(mockRequestBodyUriSpec2);
        when(mockRequestBodyUriSpec2.retrieve()).thenReturn(mockResponseSpec);
        when(mockResponseSpec.bodyToMono(any(ParameterizedTypeReference.class))).thenReturn(Mono.just(successResponse));

        Mono<SuccessResponse<User>> result = usersApi.getUserById(userId);

        StepVerifier.create(result)
                .expectNextMatches(response ->
                    response.getData().getId().equals(userId) &&
                    response.getData().getName().equals(MARIA_NAME) &&
                    response.getData().getEmail().equals(MARIA_EMAIL) &&
                    response.getData().getIdNumber().equals(VALID_USER_ID_2)
                )
                .verifyComplete();
    }

    @Test
    void shouldHandleWebClientGeneralError() {
        when(webClient.method(HttpMethod.GET)).thenReturn(mockRequestBodyUriSpec);
        when(mockRequestBodyUriSpec.uri(any(Function.class))).thenReturn(mockRequestBodySpec);
        when(mockRequestBodySpec.accept(any())).thenReturn(mockRequestBodyUriSpec2);
        when(mockRequestBodyUriSpec2.exchangeToMono(any(Function.class)))
                .thenReturn(Mono.error(new RuntimeException("Connection timeout")));

        Mono<SuccessResponse<User>> result = usersApi.getUserByIdNumberRequest(VALID_USER_ID_1);

        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();
    }
}