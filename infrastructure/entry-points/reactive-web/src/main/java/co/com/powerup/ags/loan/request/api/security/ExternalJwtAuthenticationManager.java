package co.com.powerup.ags.loan.request.api.security;

import co.com.powerup.ags.loan.request.api.exception.UnauthorizedException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Stream;

import static co.com.powerup.ags.loan.request.api.constants.SecurityConstants.*;

@Component
public class ExternalJwtAuthenticationManager implements ReactiveAuthenticationManager {
    
    private static final Logger log = LoggerFactory.getLogger(ExternalJwtAuthenticationManager.class);
    public static final String INVALID_TOKEN = "Invalid token";
    
    private final WebClient webClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    public ExternalJwtAuthenticationManager(
            @Value("${security.introspectionBaseUrl:http://localhost:8080}") String introspectionBaseUrl) {
        this.webClient = WebClient.create(introspectionBaseUrl);
    }

    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        String token = authentication.getCredentials().toString();
        
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add(TOKEN, token);
        
        return webClient.post()
                .uri("/api/v1/introspect")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(formData)
                .retrieve()
                .bodyToMono(String.class)
                .map(this::mapResponse)
                .filter(introspection -> introspection.at(ACTIVE_NODE_PATH).asBoolean(false))
                .switchIfEmpty(Mono.error(new UnauthorizedException(INVALID_TOKEN)))
                .map(introspection -> introspection.at("/data"))
                .map(response -> {
                    var subject = response.get(SUBJECT_CLAIM).asText();
                    var roles = Stream.of(response.get(ROLE_CLAIM).asText())
                            .map(List::of)
                            .flatMap(rs -> rs.stream()
                                    .map(role -> new SimpleGrantedAuthority(ROLE_PREFIX + role)))
                            .toList();
                    
                    return new UsernamePasswordAuthenticationToken(subject, token, roles);
                })
                .cast(Authentication.class)
                .onErrorMap(Exception.class, ex -> {
                    log.error("Exception during token introspection", ex);
                    return new UnauthorizedException(INVALID_TOKEN);
                });
    }
    
    private JsonNode mapResponse(String introspectionResponse) {
        try {
            return objectMapper.readTree(introspectionResponse);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}

