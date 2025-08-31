package co.com.powerup.ags.loan.request.consumer;

import co.com.powerup.ags.loan.request.consumer.api.UsersApi;
import co.com.powerup.ags.loan.request.consumer.exception.UserServiceException;
import co.com.powerup.ags.loan.request.consumer.exception.UserValidationException;
import co.com.powerup.ags.loan.request.consumer.mapper.UserMapper;
import co.com.powerup.ags.loan.request.model.user.gateways.UserGateway;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class UserRestConsumer implements UserGateway {
    
    private static final Logger log = LoggerFactory.getLogger(UserRestConsumer.class);

    private final UsersApi usersApi;
    
    @Override
    @CircuitBreaker(name = "listenGetUserByIdentityNumber")
    public Mono<co.com.powerup.ags.loan.request.model.user.User> getUserByIdNumber(String identityNumber) {
        log.info("Getting user by identity number {}", identityNumber);
        return usersApi.getUserByIdNumberRequest(identityNumber)
                .switchIfEmpty(Mono.empty())
                .flatMap(successResponse -> Mono.just(UserMapper.INSTANCE.toDomain(successResponse.getData())))
                .doOnSuccess(user -> {
                    if (user != null) {
                        log.debug("Successfully retrieved user: {} for identity: {}", user.getEmail(), identityNumber);
                    } else {
                        log.debug("No user found for identity: {}", identityNumber);
                    }
                })
                .doOnError(error -> log.error("Failed to get user for identity number: {}",
                        identityNumber, error))
                .onErrorMap(this::mapError);
        
    }
    
    private Throwable mapError(Throwable error) {
        return switch (error) {
            case WebClientResponseException webClientResponseException ->
                    new UserServiceException("External user service error: " + error.getMessage(), error);
            case UserValidationException userValidationException ->
                    new co.com.powerup.ags.loan.request.model.exception.UserValidationException(error.getMessage());
            case UserServiceException userServiceException -> error;
            default ->
                    new UserServiceException("Unexpected error retrieving user: " + error.getMessage(), error);
        };
    }
}