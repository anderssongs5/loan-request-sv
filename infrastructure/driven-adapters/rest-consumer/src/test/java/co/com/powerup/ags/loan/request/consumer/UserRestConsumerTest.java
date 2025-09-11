package co.com.powerup.ags.loan.request.consumer;

import co.com.powerup.ags.loan.request.consumer.api.UsersApi;
import co.com.powerup.ags.loan.request.consumer.api.model.SuccessResponse;
import co.com.powerup.ags.loan.request.consumer.api.model.User;
import co.com.powerup.ags.loan.request.consumer.exception.UserServiceException;
import co.com.powerup.ags.loan.request.consumer.exception.UserValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserRestConsumerTest {
    
    @Mock
    private UsersApi usersApi;
    
    private UserRestConsumer userRestConsumer;

    private static final String FALLBACK_TOKEN = "fallback-token";
    
    @BeforeEach
    void setUp() {
        userRestConsumer = new UserRestConsumer(usersApi);
    }
    
    @Test
    void shouldReturnUserSuccessfullyByIdNumber() {
        String idNumber = "12345678";
        User apiUser = User.builder()
                .id("1")
                .name("Carlos")
                .email("carlos@example.com")
                .idNumber(idNumber)
                .build();
        
        SuccessResponse<User> response = new SuccessResponse<>();
        response.setData(apiUser);
        
        when(usersApi.getUserByIdNumberOrEmailRequest(idNumber, null, FALLBACK_TOKEN)).thenReturn(Mono.just(response));
        
        StepVerifier.create(userRestConsumer.getUserByIdNumberOrEmail(idNumber, null))
                .expectNextMatches(user -> user.getId().equals("1") && user.getEmail().equals("carlos@example.com"))
                .verifyComplete();
    }
    
    @Test
    void shouldReturnEmptyWhenUserNotFound() {
        String idNumber = "99999999";
        when(usersApi.getUserByIdNumberOrEmailRequest(idNumber, null, FALLBACK_TOKEN)).thenReturn(Mono.empty());
        
        StepVerifier.create(userRestConsumer.getUserByIdNumberOrEmail(idNumber, null))
                .expectNextCount(0)
                .verifyComplete();
    }
    
    @Test
    void shouldMapWebClientResponseExceptionToUserServiceException() {
        String idNumber = "12345678";
        WebClientResponseException ex = mock(WebClientResponseException.class);
        when(ex.getMessage()).thenReturn("Timeout");
        when(usersApi.getUserByIdNumberOrEmailRequest(idNumber, null, FALLBACK_TOKEN)).thenReturn(Mono.error(ex));
        
        StepVerifier.create(userRestConsumer.getUserByIdNumberOrEmail(idNumber, null))
                .expectErrorMatches(error -> error instanceof UserServiceException &&
                        error.getMessage().contains("External user service error"))
                .verify();
    }
    
    @Test
    void shouldPropagateUserServiceException() {
        String idNumber = "12345678";
        UserServiceException ex = new UserServiceException("Service down");
        when(usersApi.getUserByIdNumberOrEmailRequest(idNumber, null, FALLBACK_TOKEN)).thenReturn(Mono.error(ex));
        
        StepVerifier.create(userRestConsumer.getUserByIdNumberOrEmail(idNumber, null))
                .expectError(UserServiceException.class)
                .verify();
    }
    
    @Test
    void shouldMapUserValidationExceptionToDomainException() {
        String idNumber = "12345678";
        UserValidationException ex = new UserValidationException("Invalid ID");
        when(usersApi.getUserByIdNumberOrEmailRequest(idNumber, null, FALLBACK_TOKEN)).thenReturn(Mono.error(ex));
        
        StepVerifier.create(userRestConsumer.getUserByIdNumberOrEmail(idNumber, null))
                .expectError(co.com.powerup.ags.loan.request.model.exception.UserValidationException.class)
                .verify();
    }
    
    @Test
    void shouldMapGenericExceptionToUserServiceException() {
        String idNumber = "12345678";
        when(usersApi.getUserByIdNumberOrEmailRequest(idNumber, null, FALLBACK_TOKEN))
                .thenReturn(Mono.error(new RuntimeException("Unexpected")));
        
        StepVerifier.create(userRestConsumer.getUserByIdNumberOrEmail(idNumber, null))
                .expectErrorMatches(error -> error instanceof UserServiceException &&
                        error.getMessage().contains("Unexpected error retrieving user"))
                .verify();
    }
    
    @Test
    void shouldReturnUserSuccessfullyByEmail() {
        String email = "jose.garcia@example.com";
        User apiUser = User.builder()
                .id("2")
                .name("José")
                .lastName("García")
                .email(email)
                .idNumber("87654321")
                .build();
        
        SuccessResponse<User> response = new SuccessResponse<>();
        response.setData(apiUser);
        
        when(usersApi.getUserByIdNumberOrEmailRequest(null, email, FALLBACK_TOKEN)).thenReturn(Mono.just(response));
        
        StepVerifier.create(userRestConsumer.getUserByIdNumberOrEmail(null, email))
                .expectNextMatches(user -> user.getId().equals("2") && user.getEmail().equals(email))
                .verifyComplete();
    }
    
    @Test
    void shouldReturnEmptyWhenUserNotFoundByEmail() {
        String email = "notfound@example.com";
        when(usersApi.getUserByIdNumberOrEmailRequest(null, email, FALLBACK_TOKEN)).thenReturn(Mono.empty());
        
        StepVerifier.create(userRestConsumer.getUserByIdNumberOrEmail(null, email))
                .expectNextCount(0)
                .verifyComplete();
    }
    
    @Test
    void shouldMapWebClientResponseExceptionToUserServiceExceptionForEmail() {
        String email = "jose.garcia@example.com";
        WebClientResponseException ex = mock(WebClientResponseException.class);
        when(ex.getMessage()).thenReturn("Connection timeout");
        when(usersApi.getUserByIdNumberOrEmailRequest(null, email, FALLBACK_TOKEN)).thenReturn(Mono.error(ex));
        
        StepVerifier.create(userRestConsumer.getUserByIdNumberOrEmail(null, email))
                .expectErrorMatches(error -> error instanceof UserServiceException &&
                        error.getMessage().contains("External user service error"))
                .verify();
    }
    
    @Test
    void shouldPropagateUserServiceExceptionForEmail() {
        String email = "jose.garcia@example.com";
        UserServiceException ex = new UserServiceException("Service down");
        when(usersApi.getUserByIdNumberOrEmailRequest(null, email, FALLBACK_TOKEN)).thenReturn(Mono.error(ex));
        
        StepVerifier.create(userRestConsumer.getUserByIdNumberOrEmail(null, email))
                .expectError(UserServiceException.class)
                .verify();
    }
    
    @Test
    void shouldMapUserValidationExceptionToDomainExceptionForEmail() {
        String email = "invalid.email@example.com";
        UserValidationException ex = new UserValidationException("Invalid email format");
        when(usersApi.getUserByIdNumberOrEmailRequest(null, email, FALLBACK_TOKEN)).thenReturn(Mono.error(ex));
        
        StepVerifier.create(userRestConsumer.getUserByIdNumberOrEmail(null, email))
                .expectError(co.com.powerup.ags.loan.request.model.exception.UserValidationException.class)
                .verify();
    }
    
    @Test
    void shouldMapGenericExceptionToUserServiceExceptionForEmail() {
        String email = "jose.garcia@example.com";
        when(usersApi.getUserByIdNumberOrEmailRequest(null, email, FALLBACK_TOKEN))
                .thenReturn(Mono.error(new RuntimeException("Unexpected error")));
        
        StepVerifier.create(userRestConsumer.getUserByIdNumberOrEmail(null, email))
                .expectErrorMatches(error -> error instanceof UserServiceException &&
                        error.getMessage().contains("Unexpected error retrieving user"))
                .verify();
    }
    
    @Test
    void shouldReturnUserSuccessfullyByBothIdNumberAndEmail() {
        String idNumber = "12345678";
        String email = "maria.rodriguez@example.com";
        User apiUser = User.builder()
                .id("3")
                .name("María")
                .lastName("Rodríguez")
                .email(email)
                .idNumber(idNumber)
                .build();
        
        SuccessResponse<User> response = new SuccessResponse<>();
        response.setData(apiUser);
        
        when(usersApi.getUserByIdNumberOrEmailRequest(idNumber, email, FALLBACK_TOKEN)).thenReturn(Mono.just(response));
        
        StepVerifier.create(userRestConsumer.getUserByIdNumberOrEmail(idNumber, email))
                .expectNextMatches(user -> user.getId().equals("3") && 
                                          user.getEmail().equals(email) &&
                                          user.getIdNumber().equals(idNumber))
                .verifyComplete();
    }
}