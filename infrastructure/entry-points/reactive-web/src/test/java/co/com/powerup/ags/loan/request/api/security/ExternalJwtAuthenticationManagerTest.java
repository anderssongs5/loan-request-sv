package co.com.powerup.ags.loan.request.api.security;

import co.com.powerup.ags.loan.request.api.exception.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExternalJwtAuthenticationManagerTest {

    @Mock
    private WebClient webClient;
    
    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;
    
    @Mock
    private WebClient.RequestBodySpec requestBodySpec;
    
    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;
    
    @Mock
    private WebClient.ResponseSpec responseSpec;

    private ExternalJwtAuthenticationManager authenticationManager;

    private static final String TEST_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...";
    private static final String JOSE_EMAIL = "jose.garcia@ejemplo.com";
    private static final String MARIA_EMAIL = "maria.rodriguez@ejemplo.com";
    private static final String ADMIN_ROLE = "ADMIN";
    private static final String USER_ROLE = "USER";

    @BeforeEach
    void setUp() {
        authenticationManager = new ExternalJwtAuthenticationManager("http://localhost:8080");
        ReflectionTestUtils.setField(authenticationManager, "webClient", webClient);
        
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
    }

    @Test
    void shouldExtractSubjectAndAdminRoleFromValidToken() {
        String introspectionResponse = """
            {
                "data": {
                    "active": true,
                    "sub": "%s",
                    "role": "%s"
                }
            }
            """.formatted(JOSE_EMAIL, ADMIN_ROLE);

        when(responseSpec.bodyToMono(String.class))
                .thenReturn(Mono.just(introspectionResponse));

        Authentication inputAuth = new UsernamePasswordAuthenticationToken(null, TEST_TOKEN);
        Mono<Authentication> result = authenticationManager.authenticate(inputAuth);

        StepVerifier.create(result)
                .expectNextMatches(auth -> {
                    assertThat(auth.getPrincipal()).isEqualTo(JOSE_EMAIL);
                    assertThat(auth.getCredentials()).isEqualTo(TEST_TOKEN);
                    assertThat(auth.getAuthorities()).hasSize(1);
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void shouldExtractSubjectAndUserRoleFromValidToken() {
        String introspectionResponse = """
            {
                "data": {
                    "active": true,
                    "sub": "%s",
                    "role": "%s"
                }
            }
            """.formatted(MARIA_EMAIL, USER_ROLE);

        when(responseSpec.bodyToMono(String.class))
                .thenReturn(Mono.just(introspectionResponse));

        Authentication inputAuth = new UsernamePasswordAuthenticationToken(null, TEST_TOKEN);
        Mono<Authentication> result = authenticationManager.authenticate(inputAuth);

        StepVerifier.create(result)
                .expectNextMatches(auth -> {
                    assertThat(auth.getPrincipal()).isEqualTo(MARIA_EMAIL);
                    assertThat(auth.getCredentials()).isEqualTo(TEST_TOKEN);
                    assertThat(auth.getAuthorities()).hasSize(1);
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void shouldExtractSubjectWithEmptyRole() {
        String introspectionResponse = """
            {
                "data": {
                    "active": true,
                    "sub": "%s",
                    "role": ""
                }
            }
            """.formatted(JOSE_EMAIL);

        when(responseSpec.bodyToMono(String.class))
                .thenReturn(Mono.just(introspectionResponse));

        Authentication inputAuth = new UsernamePasswordAuthenticationToken(null, TEST_TOKEN);
        Mono<Authentication> result = authenticationManager.authenticate(inputAuth);

        StepVerifier.create(result)
                .expectNextMatches(auth -> {
                    assertThat(auth.getPrincipal()).isEqualTo(JOSE_EMAIL);
                    assertThat(auth.getCredentials()).isEqualTo(TEST_TOKEN);
                    assertThat(auth.getAuthorities()).hasSize(1);
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void shouldHandleNullSubjectClaim() {
        String introspectionResponse = """
            {
                "data": {
                    "active": true,
                    "sub": null,
                    "role": "%s"
                }
            }
            """.formatted(USER_ROLE);

        when(responseSpec.bodyToMono(String.class))
                .thenReturn(Mono.just(introspectionResponse));

        Authentication inputAuth = new UsernamePasswordAuthenticationToken(null, TEST_TOKEN);
        Mono<Authentication> result = authenticationManager.authenticate(inputAuth);

        StepVerifier.create(result)
                .expectNextMatches(auth -> {
                    assertThat(auth.getPrincipal()).isEqualTo("null");
                    assertThat(auth.getCredentials()).isEqualTo(TEST_TOKEN);
                    assertThat(auth.getAuthorities()).hasSize(1);
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void shouldHandleNullRoleClaim() {
        String introspectionResponse = """
            {
                "data": {
                    "active": true,
                    "sub": "%s",
                    "role": null
                }
            }
            """.formatted(MARIA_EMAIL);

        when(responseSpec.bodyToMono(String.class))
                .thenReturn(Mono.just(introspectionResponse));

        Authentication inputAuth = new UsernamePasswordAuthenticationToken(null, TEST_TOKEN);
        Mono<Authentication> result = authenticationManager.authenticate(inputAuth);

        StepVerifier.create(result)
                .expectNextMatches(auth -> {
                    assertThat(auth.getPrincipal()).isEqualTo(MARIA_EMAIL);
                    assertThat(auth.getCredentials()).isEqualTo(TEST_TOKEN);
                    assertThat(auth.getAuthorities()).hasSize(1);
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void shouldHandleMissingSubjectClaim() {
        String introspectionResponse = """
            {
                "data": {
                    "active": true,
                    "role": "%s"
                }
            }
            """.formatted(ADMIN_ROLE);

        when(responseSpec.bodyToMono(String.class))
                .thenReturn(Mono.just(introspectionResponse));

        Authentication inputAuth = new UsernamePasswordAuthenticationToken(null, TEST_TOKEN);
        Mono<Authentication> result = authenticationManager.authenticate(inputAuth);

        StepVerifier.create(result)
                .expectError(UnauthorizedException.class)
                .verify();
    }

    @Test
    void shouldHandleMissingRoleClaim() {
        String introspectionResponse = """
            {
                "data": {
                    "active": true,
                    "sub": "%s"
                }
            }
            """.formatted(JOSE_EMAIL);

        when(responseSpec.bodyToMono(String.class))
                .thenReturn(Mono.just(introspectionResponse));

        Authentication inputAuth = new UsernamePasswordAuthenticationToken(null, TEST_TOKEN);
        Mono<Authentication> result = authenticationManager.authenticate(inputAuth);

        StepVerifier.create(result)
                .expectError(UnauthorizedException.class)
                .verify();
    }

    @Test
    void shouldHandleInactiveToken() {
        String introspectionResponse = """
            {
                "data": {
                    "active": false
                }
            }
            """;

        when(responseSpec.bodyToMono(String.class))
                .thenReturn(Mono.just(introspectionResponse));

        Authentication inputAuth = new UsernamePasswordAuthenticationToken(null, TEST_TOKEN);
        Mono<Authentication> result = authenticationManager.authenticate(inputAuth);

        StepVerifier.create(result)
                .expectError(UnauthorizedException.class)
                .verify();
    }

    @Test
    void shouldHandleInvalidJsonResponse() {
        String invalidJsonResponse = "{ invalid json }";

        when(responseSpec.bodyToMono(String.class))
                .thenReturn(Mono.just(invalidJsonResponse));

        Authentication inputAuth = new UsernamePasswordAuthenticationToken(null, TEST_TOKEN);
        Mono<Authentication> result = authenticationManager.authenticate(inputAuth);

        StepVerifier.create(result)
                .expectError(UnauthorizedException.class)
                .verify();
    }

    @Test
    void shouldHandleServerError() {
        when(responseSpec.bodyToMono(String.class))
                .thenReturn(Mono.error(new RuntimeException("Server error")));

        Authentication inputAuth = new UsernamePasswordAuthenticationToken(null, TEST_TOKEN);
        Mono<Authentication> result = authenticationManager.authenticate(inputAuth);

        StepVerifier.create(result)
                .expectError(UnauthorizedException.class)
                .verify();
    }
}