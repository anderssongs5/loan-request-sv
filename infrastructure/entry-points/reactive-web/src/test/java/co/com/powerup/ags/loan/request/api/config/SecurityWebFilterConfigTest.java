package co.com.powerup.ags.loan.request.api.config;

import co.com.powerup.ags.loan.request.api.exception.AccessDeniedException;
import co.com.powerup.ags.loan.request.api.security.BearerTokenConverter;
import co.com.powerup.ags.loan.request.api.security.ExternalJwtAuthenticationManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class SecurityWebFilterConfigTest {

    @Mock
    private ExternalJwtAuthenticationManager authManager;

    @Mock
    private BearerTokenConverter tokenConverter;

    @Mock
    private CustomAuthenticationEntryPoint authenticationEntryPoint;

    private SecurityWebFilterConfig securityWebFilterConfig;

    @BeforeEach
    void setUp() {
        securityWebFilterConfig = new SecurityWebFilterConfig();
    }

    @Test
    void shouldCreateAccessDeniedHandler() {
        ServerAccessDeniedHandler handler = securityWebFilterConfig.accessDeniedHandler();

        assertThat(handler).isNotNull();
    }

    @Test
    void accessDeniedHandlerShouldReturnAccessDeniedException() {
        ServerAccessDeniedHandler handler = securityWebFilterConfig.accessDeniedHandler();

        Mono<Void> result = handler.handle(null, null);

        StepVerifier.create(result)
                .expectError(AccessDeniedException.class)
                .verify();
    }

    @Test
    void shouldCreateSecurityWebFilterChain() {
        ServerHttpSecurity http = ServerHttpSecurity.http();

        SecurityWebFilterChain filterChain = securityWebFilterConfig.securityWebFilterChain(
                http, authManager, tokenConverter, authenticationEntryPoint);

        assertThat(filterChain).isNotNull();
    }

    @Test
    void accessDeniedHandlerShouldHaveCorrectMessage() {
        ServerAccessDeniedHandler handler = securityWebFilterConfig.accessDeniedHandler();

        StepVerifier.create(handler.handle(null, null))
                .expectErrorMatches(throwable -> 
                    throwable instanceof AccessDeniedException &&
                    throwable.getMessage().equals("Access denied. Insufficient permissions."))
                .verify();
    }
}