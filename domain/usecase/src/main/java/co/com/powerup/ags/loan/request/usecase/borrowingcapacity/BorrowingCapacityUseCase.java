package co.com.powerup.ags.loan.request.usecase.borrowingcapacity;

import co.com.powerup.ags.loan.request.model.exception.UserServiceException;
import co.com.powerup.ags.loan.request.model.exception.UserValidationException;
import co.com.powerup.ags.loan.request.model.user.gateways.UserGateway;
import co.com.powerup.ags.loan.request.usecase.common.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;

@RequiredArgsConstructor
public class BorrowingCapacityUseCase {
    
    private final UserGateway userGateway;
    private static final BigDecimal MAX_BORROWING_CAPACITY_PERCENTAGE = BigDecimal.valueOf(0.35);
    
    public Mono<BorrowingCapacity> calculateBorrowingCapacity(String idNumber) {
        return userGateway.getUserByIdNumberOrEmail(idNumber, null)
                .onErrorMap(throwable -> {
                    if (throwable instanceof UserValidationException) {
                        return throwable;
                    }
                    
                    return new UserServiceException("User service unavailable", throwable);
                })
                .switchIfEmpty(Mono.error(new UserNotFoundException("User with id number " + idNumber + " does not exist")))
                .map(user -> {
                    BigDecimal borrowingCapacity = user.getBaseSalary().multiply(MAX_BORROWING_CAPACITY_PERCENTAGE)
                            .setScale(2, RoundingMode.HALF_UP);
                    
                    return new BorrowingCapacity(borrowingCapacity);
                });
    }
}
