package co.com.powerup.ags.loan.request.consumer.api;

import co.com.powerup.ags.loan.request.consumer.api.model.CreateUserRequest;
import co.com.powerup.ags.loan.request.consumer.api.model.ErrorResponse;
import co.com.powerup.ags.loan.request.consumer.api.model.ServerResponse;
import co.com.powerup.ags.loan.request.consumer.api.model.SuccessResponse;
import co.com.powerup.ags.loan.request.consumer.api.model.UpdateUserRequest;

import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.log4j.Log4j2;
import lombok.AllArgsConstructor;

@Log4j2
@AllArgsConstructor
@Service
public class UsersApi {
    private final WebClient client;

    /**
    * Build call for createUser
    * @param body User creation data (required)
    * @return Mono<SuccessResponse> response
    */
    public Mono<SuccessResponse> createUserRequest(CreateUserRequest body) {
        return client.method(HttpMethod.POST)
            .uri("/api/v1/users")
            .contentType(MediaType.parseMediaType("application/json"))
            .body(BodyInserters.fromValue(body))
            .accept(MediaType.parseMediaType("application/json"))
            .retrieve()
            .bodyToMono(SuccessResponse.class);
    }
    /**
    * Build call for deleteUser
    * @param id User unique identifier (required)
    * @return Mono<ServerResponse> response
    */
    public Mono<ServerResponse> deleteUserRequest(String id) {
        return client.method(HttpMethod.DELETE)
            .uri("/api/v1/users/{id}", id)
            .accept(MediaType.parseMediaType("*/*"))
            .retrieve()
            .bodyToMono(ServerResponse.class);
    }
    /**
    * Build call for getUserByEmail
    * @param email User email address (required)
    * @return Mono<SuccessResponse> response
    */
    public Mono<SuccessResponse> getUserByEmailRequest(String email) {
        return client.method(HttpMethod.GET)
            .uri("/api/v1/users/search")
            .accept(MediaType.parseMediaType("application/json"))
            .retrieve()
            .bodyToMono(SuccessResponse.class);
    }
    /**
    * Build call for getUserById
    * @param id User unique identifier (required)
    * @return Mono<SuccessResponse> response
    */
    public Mono<SuccessResponse> getUserByIdRequest(String id) {
        return client.method(HttpMethod.GET)
            .uri("/api/v1/users/{id}", id)
            .accept(MediaType.parseMediaType("application/json"))
            .retrieve()
            .bodyToMono(SuccessResponse.class);
    }
    /**
    * Build call for updateUser
    * @param body User update data (required)
    * @param id User unique identifier (required)
    * @return Mono<SuccessResponse> response
    */
    public Mono<SuccessResponse> updateUserRequest(UpdateUserRequest body, String id) {
        return client.method(HttpMethod.PUT)
            .uri("/api/v1/users/{id}", id)
            .contentType(MediaType.parseMediaType("application/json"))
            .body(BodyInserters.fromValue(body))
            .accept(MediaType.parseMediaType("application/json"))
            .retrieve()
            .bodyToMono(SuccessResponse.class);
    }
}
