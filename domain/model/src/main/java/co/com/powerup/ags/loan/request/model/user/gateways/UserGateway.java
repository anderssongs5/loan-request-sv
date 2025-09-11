package co.com.powerup.ags.loan.request.model.user.gateways;

import co.com.powerup.ags.loan.request.model.user.User;
import reactor.core.publisher.Mono;

public interface UserGateway {
    
    Mono<User> getUserByIdNumber(String userId);
}