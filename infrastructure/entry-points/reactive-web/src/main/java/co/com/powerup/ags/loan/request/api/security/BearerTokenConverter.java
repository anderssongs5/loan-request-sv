package co.com.powerup.ags.loan.request.api.security;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static co.com.powerup.ags.loan.request.api.constants.SecurityConstants.BEARER_PREFIX;

@Component
public class BearerTokenConverter implements ServerAuthenticationConverter {
    
    @Override
    public Mono<Authentication> convert(ServerWebExchange exchange) {
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        
        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            String token = authHeader.substring(BEARER_PREFIX.length());
            return Mono.just(new UsernamePasswordAuthenticationToken(null, token));
        }
        
        return Mono.empty();
    }
}

