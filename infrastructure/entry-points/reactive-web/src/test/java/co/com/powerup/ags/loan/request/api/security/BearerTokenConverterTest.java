package co.com.powerup.ags.loan.request.api.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BearerTokenConverterTest {

    @Mock
    private ServerWebExchange exchange;

    @Mock
    private ServerHttpRequest request;

    @Mock
    private HttpHeaders headers;

    private BearerTokenConverter bearerTokenConverter;

    @BeforeEach
    void setUp() {
        bearerTokenConverter = new BearerTokenConverter();
    }

    @Test
    void shouldConvertBearerTokenToAuthentication() {
        String token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...";
        String authHeader = "Bearer " + token;

        when(exchange.getRequest()).thenReturn(request);
        when(request.getHeaders()).thenReturn(headers);
        when(headers.getFirst(HttpHeaders.AUTHORIZATION)).thenReturn(authHeader);

        Mono<Authentication> result = bearerTokenConverter.convert(exchange);

        StepVerifier.create(result)
                .expectNextMatches(auth -> 
                    auth instanceof UsernamePasswordAuthenticationToken &&
                    auth.getCredentials().equals(token) &&
                    auth.getPrincipal() == null
                )
                .verifyComplete();
    }

    @Test
    void shouldReturnEmptyWhenNoAuthorizationHeader() {
        when(exchange.getRequest()).thenReturn(request);
        when(request.getHeaders()).thenReturn(headers);
        when(headers.getFirst(HttpHeaders.AUTHORIZATION)).thenReturn(null);

        Mono<Authentication> result = bearerTokenConverter.convert(exchange);

        StepVerifier.create(result)
                .verifyComplete();
    }

    @Test
    void shouldReturnEmptyWhenAuthorizationHeaderDoesNotStartWithBearer() {
        String authHeader = "Basic dXNlcjpwYXNzd29yZA==";

        when(exchange.getRequest()).thenReturn(request);
        when(request.getHeaders()).thenReturn(headers);
        when(headers.getFirst(HttpHeaders.AUTHORIZATION)).thenReturn(authHeader);

        Mono<Authentication> result = bearerTokenConverter.convert(exchange);

        StepVerifier.create(result)
                .verifyComplete();
    }

    @Test
    void shouldReturnEmptyWhenAuthorizationHeaderIsOnlyBearer() {
        String authHeader = "Bearer";

        when(exchange.getRequest()).thenReturn(request);
        when(request.getHeaders()).thenReturn(headers);
        when(headers.getFirst(HttpHeaders.AUTHORIZATION)).thenReturn(authHeader);

        Mono<Authentication> result = bearerTokenConverter.convert(exchange);

        StepVerifier.create(result)
                .expectComplete();
    }

    @Test
    void shouldHandleBearerWithSpaces() {
        String token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...";
        String authHeader = "Bearer " + token;

        when(exchange.getRequest()).thenReturn(request);
        when(request.getHeaders()).thenReturn(headers);
        when(headers.getFirst(HttpHeaders.AUTHORIZATION)).thenReturn(authHeader);

        Mono<Authentication> result = bearerTokenConverter.convert(exchange);

        StepVerifier.create(result)
                .expectNextMatches(auth -> 
                    auth.getCredentials().equals(token)
                )
                .verifyComplete();
    }
}