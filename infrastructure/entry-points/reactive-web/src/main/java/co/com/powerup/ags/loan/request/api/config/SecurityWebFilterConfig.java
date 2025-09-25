package co.com.powerup.ags.loan.request.api.config;

import co.com.powerup.ags.loan.request.api.constants.SecurityConstants;
import co.com.powerup.ags.loan.request.api.security.BearerTokenConverter;
import co.com.powerup.ags.loan.request.api.security.ExternalJwtAuthenticationManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import co.com.powerup.ags.loan.request.api.exception.AccessDeniedException;
import reactor.core.publisher.Mono;

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
public class SecurityWebFilterConfig {
    
    public static final String API_V1_LOAN_REQUESTS_PATH = "/api/v1/loan-requests";
    public static final String CLIENT_ROLE = "CLIENT";
    public static final String ADVISOR_ROLE = "ADVISOR";
    public static final String ADMIN_ROLE = "ADMIN";
    
    @Bean
    public ServerAccessDeniedHandler accessDeniedHandler() {
        return (exchange, denied) ->
                Mono.error(new AccessDeniedException("Access denied. Insufficient permissions."));
    }
    
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http,
                                                         ExternalJwtAuthenticationManager authManager,
                                                         BearerTokenConverter tokenConverter,
                                                         CustomAuthenticationEntryPoint authenticationEntryPoint) {
        
        AuthenticationWebFilter authFilter = new AuthenticationWebFilter(authManager);
        authFilter.setServerAuthenticationConverter(tokenConverter);
        
        http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .addFilterAfter(authFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .exceptionHandling(exceptions -> exceptions
                        .accessDeniedHandler(accessDeniedHandler())
                        .authenticationEntryPoint(authenticationEntryPoint)
                )
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers(SecurityConstants.EXCLUDED_PATTERNS.toArray(new String[0]))
                        .permitAll()
                        .pathMatchers(HttpMethod.GET, API_V1_LOAN_REQUESTS_PATH).hasRole(ADVISOR_ROLE)
                        .pathMatchers(HttpMethod.POST, API_V1_LOAN_REQUESTS_PATH).hasRole(CLIENT_ROLE)
                        .pathMatchers(HttpMethod.PUT, API_V1_LOAN_REQUESTS_PATH + "/**").hasRole(ADVISOR_ROLE)
                        .pathMatchers(HttpMethod.POST, "/api/v1/borrowing-capacity").hasAnyRole(ADVISOR_ROLE,
                                CLIENT_ROLE, ADMIN_ROLE)
                        .anyExchange()
                        .authenticated()
                );

        return http.build();
    }
}
