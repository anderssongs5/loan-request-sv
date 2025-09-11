package co.com.powerup.ags.loan.request.consumer.api;

import co.com.powerup.ags.loan.request.consumer.api.model.ErrorResponse;
import co.com.powerup.ags.loan.request.consumer.api.model.SuccessResponse;
import co.com.powerup.ags.loan.request.consumer.api.model.User;
import co.com.powerup.ags.loan.request.consumer.exception.UserServiceException;
import co.com.powerup.ags.loan.request.consumer.exception.UserValidationException;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import lombok.extern.log4j.Log4j2;
import lombok.AllArgsConstructor;

@Log4j2
@AllArgsConstructor
@Service
public class UsersApi {

    private final WebClient client;

    /**
    * Build call for getUserByIdNumber
    * @param idNumber User id number (required)
    * @return Mono<SuccessResponse> response
    */
    public Mono<SuccessResponse<User>> getUserByIdNumberRequest(String idNumber) {
        return client.method(HttpMethod.GET)
            .uri(uriBuilder -> uriBuilder
                    .path("/api/v1/users/search")
                    .queryParam("idNumber", idNumber)
                    .build())
            .accept(MediaType.APPLICATION_JSON)
            .exchangeToMono(response -> {
                if (response.statusCode().value() == 404) {
                    log.warn("User not found (404) for idNumber: {}", idNumber);
                    return Mono.empty();
                }
                if (response.statusCode().is4xxClientError()) {
                    return response.bodyToMono(ErrorResponse.class)
                            .doOnNext(errorBody ->
                                    log.error("Client error {} when fetching user with idNumber: {}. Error response: {}",
                                    response.statusCode(), idNumber, errorBody.getMessage()))
                            .map(errorBody -> new UserValidationException(errorBody.getMessage()))
                            .flatMap(Mono::error);
                }
                if (response.statusCode().is5xxServerError()) {
                    log.error("Server error {} when fetching user with idNumber: {}",
                            response.statusCode(), idNumber);
                    return Mono.error(new UserServiceException("User service unavailable"));
                }
                return response.bodyToMono(new ParameterizedTypeReference<SuccessResponse<User>>() {});
            });
    }

    /**
    * Build call for getUserById
    * @param id User unique identifier (required)
    * @return Mono<SuccessResponse> response
    */
    public Mono<SuccessResponse<User>> getUserById(String id) {
        return client.method(HttpMethod.GET)
            .uri("/api/v1/users/{id}", id)
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<SuccessResponse<User>>() {});
    }
}
