package co.com.powerup.ags.loan.request.api.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Component
public class CustomAuthenticationEntryPoint implements ServerAuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Mono<Void> commence(ServerWebExchange exchange, AuthenticationException ex) {
        return exchange.getResponse().writeWith(
                Mono.fromCallable(() -> {
                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    exchange.getResponse().getHeaders().add("Content-Type", MediaType.APPLICATION_JSON_VALUE);

                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("timestamp", LocalDateTime.now().toString());
                    errorResponse.put("status", HttpStatus.UNAUTHORIZED.value());
                    errorResponse.put("error", "Unauthorized");
                    errorResponse.put("message", "Authentication required. Please provide a valid token.");
                    errorResponse.put("path", exchange.getRequest().getPath().value());

                    try {
                        String json = objectMapper.writeValueAsString(errorResponse);
                        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(json.getBytes(StandardCharsets.UTF_8));
                        return buffer;
                    } catch (JsonProcessingException e) {
                        String fallbackResponse = "{\"error\":\"Unauthorized\",\"message\":\"Authentication required\"}";
                        return exchange.getResponse().bufferFactory().wrap(fallbackResponse.getBytes(StandardCharsets.UTF_8));
                    }
                })
        );
    }
}