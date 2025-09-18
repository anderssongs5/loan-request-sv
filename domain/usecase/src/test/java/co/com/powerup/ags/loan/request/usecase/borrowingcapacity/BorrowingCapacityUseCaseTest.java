package co.com.powerup.ags.loan.request.usecase.borrowingcapacity;

import co.com.powerup.ags.loan.request.model.exception.UserServiceException;
import co.com.powerup.ags.loan.request.model.exception.UserValidationException;
import co.com.powerup.ags.loan.request.model.user.User;
import co.com.powerup.ags.loan.request.model.user.gateways.UserGateway;
import co.com.powerup.ags.loan.request.usecase.common.exception.UserNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BorrowingCapacityUseCaseTest {

    private static final String VALID_ID_NUMBER = "1234567890";
    private static final String INVALID_ID_NUMBER = "9999999999";
    private static final String USER_EMAIL = "carlos.rodriguez@ejemplo.com";
    private static final String USER_NAME = "Carlos Rodriguez";
    private static final BigDecimal BASE_SALARY_100000 = new BigDecimal("100000.00");
    private static final BigDecimal BASE_SALARY_50000 = new BigDecimal("50000.00");
    private static final BigDecimal BASE_SALARY_200000 = new BigDecimal("200000.00");
    private static final BigDecimal EXPECTED_CAPACITY_35000 = new BigDecimal("35000.00");
    private static final BigDecimal EXPECTED_CAPACITY_17500 = new BigDecimal("17500.00");
    private static final BigDecimal EXPECTED_CAPACITY_70000 = new BigDecimal("70000.00");

    @Mock
    private UserGateway userGateway;

    private BorrowingCapacityUseCase borrowingCapacityUseCase;

    @BeforeEach
    void setUp() {
        borrowingCapacityUseCase = new BorrowingCapacityUseCase(userGateway);
    }

    @Test
    void calculateBorrowingCapacity_WhenUserExists_ShouldReturnCorrectCapacity() {
        User user = createUser(USER_NAME, USER_EMAIL, VALID_ID_NUMBER, BASE_SALARY_100000);
        when(userGateway.getUserByIdNumberOrEmail(eq(VALID_ID_NUMBER), eq(null)))
                .thenReturn(Mono.just(user));

        StepVerifier.create(borrowingCapacityUseCase.calculateBorrowingCapacity(VALID_ID_NUMBER))
                .expectNextMatches(capacity -> 
                    capacity.borrowingCapacity().compareTo(EXPECTED_CAPACITY_35000) == 0)
                .verifyComplete();
    }

    @Test
    void calculateBorrowingCapacity_WhenUserHasLowerSalary_ShouldReturnCorrectCapacity() {
        User user = createUser(USER_NAME, USER_EMAIL, VALID_ID_NUMBER, BASE_SALARY_50000);
        when(userGateway.getUserByIdNumberOrEmail(eq(VALID_ID_NUMBER), eq(null)))
                .thenReturn(Mono.just(user));

        StepVerifier.create(borrowingCapacityUseCase.calculateBorrowingCapacity(VALID_ID_NUMBER))
                .expectNextMatches(capacity -> 
                    capacity.borrowingCapacity().compareTo(EXPECTED_CAPACITY_17500) == 0)
                .verifyComplete();
    }

    @Test
    void calculateBorrowingCapacity_WhenUserHasHigherSalary_ShouldReturnCorrectCapacity() {
        User user = createUser(USER_NAME, USER_EMAIL, VALID_ID_NUMBER, BASE_SALARY_200000);
        when(userGateway.getUserByIdNumberOrEmail(eq(VALID_ID_NUMBER), eq(null)))
                .thenReturn(Mono.just(user));

        StepVerifier.create(borrowingCapacityUseCase.calculateBorrowingCapacity(VALID_ID_NUMBER))
                .expectNextMatches(capacity -> 
                    capacity.borrowingCapacity().compareTo(EXPECTED_CAPACITY_70000) == 0)
                .verifyComplete();
    }

    @Test
    void calculateBorrowingCapacity_WhenUserNotFound_ShouldThrowUserNotFoundException() {
        when(userGateway.getUserByIdNumberOrEmail(eq(INVALID_ID_NUMBER), eq(null)))
                .thenReturn(Mono.empty());

        StepVerifier.create(borrowingCapacityUseCase.calculateBorrowingCapacity(INVALID_ID_NUMBER))
                .expectErrorMatches(throwable -> 
                    throwable instanceof UserNotFoundException &&
                    throwable.getMessage().contains("User with id number " + INVALID_ID_NUMBER + " does not exist"))
                .verify();
    }

    @Test
    void calculateBorrowingCapacity_WhenUserValidationException_ShouldPropagateException() {
        UserValidationException validationException = new UserValidationException("Invalid user data");
        when(userGateway.getUserByIdNumberOrEmail(eq(VALID_ID_NUMBER), eq(null)))
                .thenReturn(Mono.error(validationException));

        StepVerifier.create(borrowingCapacityUseCase.calculateBorrowingCapacity(VALID_ID_NUMBER))
                .expectErrorMatches(throwable -> 
                    throwable instanceof UserValidationException &&
                    throwable.getMessage().equals("Invalid user data"))
                .verify();
    }

    @Test
    void calculateBorrowingCapacity_WhenUserServiceException_ShouldWrapInUserServiceException() {
        RuntimeException originalException = new RuntimeException("Database connection failed");
        when(userGateway.getUserByIdNumberOrEmail(eq(VALID_ID_NUMBER), eq(null)))
                .thenReturn(Mono.error(originalException));

        StepVerifier.create(borrowingCapacityUseCase.calculateBorrowingCapacity(VALID_ID_NUMBER))
                .expectErrorMatches(throwable -> 
                    throwable instanceof UserServiceException &&
                    throwable.getMessage().equals("User service unavailable") &&
                    throwable.getCause() == originalException)
                .verify();
    }

    @Test
    void calculateBorrowingCapacity_WhenUserHasZeroSalary_ShouldReturnZeroCapacity() {
        User user = createUser(USER_NAME, USER_EMAIL, VALID_ID_NUMBER, BigDecimal.ZERO);
        when(userGateway.getUserByIdNumberOrEmail(eq(VALID_ID_NUMBER), eq(null)))
                .thenReturn(Mono.just(user));

        StepVerifier.create(borrowingCapacityUseCase.calculateBorrowingCapacity(VALID_ID_NUMBER))
                .expectNextMatches(capacity -> 
                    capacity.borrowingCapacity().compareTo(BigDecimal.ZERO) == 0)
                .verifyComplete();
    }

    @Test
    void calculateBorrowingCapacity_WhenUserHasVerySmallSalary_ShouldReturnCorrectlyRoundedCapacity() {
        BigDecimal smallSalary = new BigDecimal("100.33");
        BigDecimal expectedCapacity = new BigDecimal("35.12");
        User user = createUser(USER_NAME, USER_EMAIL, VALID_ID_NUMBER, smallSalary);
        when(userGateway.getUserByIdNumberOrEmail(eq(VALID_ID_NUMBER), eq(null)))
                .thenReturn(Mono.just(user));

        StepVerifier.create(borrowingCapacityUseCase.calculateBorrowingCapacity(VALID_ID_NUMBER))
                .expectNextMatches(capacity -> 
                    capacity.borrowingCapacity().compareTo(expectedCapacity) == 0)
                .verifyComplete();
    }

    @Test
    void calculateBorrowingCapacity_WhenUserHasSalaryRequiringRounding_ShouldUseHalfUpRounding() {
        BigDecimal salary = new BigDecimal("100.14");
        BigDecimal expectedCapacity = new BigDecimal("35.05");
        User user = createUser(USER_NAME, USER_EMAIL, VALID_ID_NUMBER, salary);
        when(userGateway.getUserByIdNumberOrEmail(eq(VALID_ID_NUMBER), eq(null)))
                .thenReturn(Mono.just(user));

        StepVerifier.create(borrowingCapacityUseCase.calculateBorrowingCapacity(VALID_ID_NUMBER))
                .expectNextMatches(capacity -> 
                    capacity.borrowingCapacity().compareTo(expectedCapacity) == 0)
                .verifyComplete();
    }

    @Test
    void calculateBorrowingCapacity_WhenCalledMultipleTimes_ShouldReturnConsistentResults() {
        User user = createUser(USER_NAME, USER_EMAIL, VALID_ID_NUMBER, BASE_SALARY_100000);
        when(userGateway.getUserByIdNumberOrEmail(eq(VALID_ID_NUMBER), eq(null)))
                .thenReturn(Mono.just(user));

        StepVerifier.create(borrowingCapacityUseCase.calculateBorrowingCapacity(VALID_ID_NUMBER))
                .expectNextMatches(capacity -> 
                    capacity.borrowingCapacity().compareTo(EXPECTED_CAPACITY_35000) == 0)
                .verifyComplete();

        StepVerifier.create(borrowingCapacityUseCase.calculateBorrowingCapacity(VALID_ID_NUMBER))
                .expectNextMatches(capacity -> 
                    capacity.borrowingCapacity().compareTo(EXPECTED_CAPACITY_35000) == 0)
                .verifyComplete();
    }

    private User createUser(String name, String email, String idNumber, BigDecimal baseSalary) {
        return User.builder()
                .name(name)
                .email(email)
                .idNumber(idNumber)
                .baseSalary(baseSalary)
                .build();
    }
}