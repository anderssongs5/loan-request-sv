package co.com.powerup.ags.loan.request.consumer;


import co.com.powerup.ags.loan.request.consumer.api.UsersApi;
import co.com.powerup.ags.loan.request.consumer.api.model.SuccessResponse;
import co.com.powerup.ags.loan.request.consumer.api.model.User;
import co.com.powerup.ags.loan.request.consumer.exception.UserServiceException;
import co.com.powerup.ags.loan.request.consumer.exception.UserValidationException;
import okhttp3.mockwebserver.MockWebServer;
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
    
    @BeforeEach
    void setUp() {
        userRestConsumer = new UserRestConsumer(usersApi);
    }
    
    @Test
    void shouldReturnUserSuccessfully() {
        String idNumber = "12345678";
        User apiUser = User.builder()
                .id("1")
                .name("Carlos")
                .email("carlos@example.com")
                .idNumber(idNumber)
                .build();
        
        SuccessResponse<User> response = new SuccessResponse<>();
        response.setData(apiUser);
        
        when(usersApi.getUserByIdNumberRequest(idNumber)).thenReturn(Mono.just(response));
        
        StepVerifier.create(userRestConsumer.getUserByIdNumber(idNumber))
                .expectNextMatches(user -> user.getId().equals("1") && user.getEmail().equals("carlos@example.com"))
                .verifyComplete();
    }
    
    @Test
    void shouldReturnEmptyWhenUserNotFound() {
        String idNumber = "99999999";
        when(usersApi.getUserByIdNumberRequest(idNumber)).thenReturn(Mono.empty());
        
        StepVerifier.create(userRestConsumer.getUserByIdNumber(idNumber))
                .expectNextCount(0)
                .verifyComplete();
    }
    
    @Test
    void shouldMapWebClientResponseExceptionToUserServiceException() {
        String idNumber = "12345678";
        WebClientResponseException ex = mock(WebClientResponseException.class);
        when(ex.getMessage()).thenReturn("Timeout");
        when(usersApi.getUserByIdNumberRequest(idNumber)).thenReturn(Mono.error(ex));
        
        StepVerifier.create(userRestConsumer.getUserByIdNumber(idNumber))
                .expectErrorMatches(error -> error instanceof UserServiceException &&
                        error.getMessage().contains("External user service error"))
                .verify();
    }
    
    @Test
    void shouldPropagateUserServiceException() {
        String idNumber = "12345678";
        UserServiceException ex = new UserServiceException("Service down");
        when(usersApi.getUserByIdNumberRequest(idNumber)).thenReturn(Mono.error(ex));
        
        StepVerifier.create(userRestConsumer.getUserByIdNumber(idNumber))
                .expectError(UserServiceException.class)
                .verify();
    }
    
    @Test
    void shouldMapUserValidationExceptionToDomainException() {
        String idNumber = "12345678";
        UserValidationException ex = new UserValidationException("Invalid ID");
        when(usersApi.getUserByIdNumberRequest(idNumber)).thenReturn(Mono.error(ex));
        
        StepVerifier.create(userRestConsumer.getUserByIdNumber(idNumber))
                .expectError(co.com.powerup.ags.loan.request.model.exception.UserValidationException.class)
                .verify();
    }
    
    @Test
    void shouldMapGenericExceptionToUserServiceException() {
        String idNumber = "12345678";
        when(usersApi.getUserByIdNumberRequest(idNumber)).thenReturn(Mono.error(new RuntimeException("Unexpected")));
        
        StepVerifier.create(userRestConsumer.getUserByIdNumber(idNumber))
                .expectErrorMatches(error -> error instanceof UserServiceException &&
                        error.getMessage().contains("Unexpected error retrieving user"))
                .verify();
    }
}