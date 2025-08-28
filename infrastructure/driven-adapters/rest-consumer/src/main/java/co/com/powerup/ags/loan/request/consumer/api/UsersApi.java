package co.com.powerup.ags.loan.request.consumer.api;

import co.com.powerup.ags.loan.request.consumer.api.model.SuccessResponse;

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
    public Mono<SuccessResponse> getUserByIdNumberRequest(String idNumber) {
        return client.method(HttpMethod.GET)
            .uri(uriBuilder -> uriBuilder
                    .path("/api/v1/users/search")
                    .queryParam("idNumber", idNumber)
                    .build())
            .accept(MediaType.APPLICATION_JSON)
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
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .bodyToMono(SuccessResponse.class);
    }
}
