package co.com.powerup.ags.loan.request.usecase.loanapplication;

import co.com.powerup.ags.loan.request.model.exception.UserValidationException;
import co.com.powerup.ags.loan.request.model.loanapplication.LoanApplication;
import co.com.powerup.ags.loan.request.model.loanapplication.gateways.LoanApplicationRepository;
import co.com.powerup.ags.loan.request.model.loanapplicationstatus.LoanApplicationStatus;
import co.com.powerup.ags.loan.request.model.loanapplicationstatus.gateways.LoanApplicationStatusRepository;
import co.com.powerup.ags.loan.request.model.loantype.LoanType;
import co.com.powerup.ags.loan.request.model.loantype.gateways.LoanTypeRepository;
import co.com.powerup.ags.loan.request.model.user.gateways.UserGateway;
import co.com.powerup.ags.loan.request.usecase.loanapplication.command.CreateLoanRequestCommand;
import co.com.powerup.ags.loan.request.usecase.loanapplication.exception.FieldValidationException;
import co.com.powerup.ags.loan.request.usecase.loanapplication.exception.LoanApplicationStatusNotFoundException;
import co.com.powerup.ags.loan.request.usecase.loanapplication.exception.LoanTypeNotFoundException;
import co.com.powerup.ags.loan.request.usecase.loanapplication.exception.UserNotFoundException;
import co.com.powerup.ags.loan.request.model.exception.UserServiceException;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@RequiredArgsConstructor
public class LoanApplicationUseCase {
    
    public static final String PENDING = "PENDING";
    private final LoanApplicationRepository loanApplicationRepository;
    private final LoanTypeRepository loanTypeRepository;
    private final LoanApplicationStatusRepository loanApplicationStatusRepository;
    private final UserGateway userGateway;
    
    public Flux<LoanApplication> getAllLoanRequests() {
        return loanApplicationRepository.getAllLoanRequests();
    }
    
    public Mono<LoanApplication> createLoanRequest(CreateLoanRequestCommand command) {
        return validateLoanType(command.getLoanTypeId())
            .flatMap(loanType -> validateAmountAndTerm(loanType, command.getAmount(), command.getTerm())
                .then(userGateway.getUserByIdNumber(command.getUserIdNumber())
                    .onErrorMap(throwable -> {
                        if (throwable instanceof UserValidationException) {
                            return throwable;
                        }
                        
                        return new UserServiceException("User service unavailable", throwable);
                    })
                    .switchIfEmpty(Mono.error(new UserNotFoundException("User with ID number " + command.getUserIdNumber() + " does not exist")))
                )
                .zipWith(validateLoanApplicationStatusByName(PENDING))
                .map(tuple -> {
                    String email = tuple.getT1().getEmail();
                    LoanApplicationStatus pendingStatus = tuple.getT2();
                    return LoanApplication.builder()
                        .amount(command.getAmount())
                        .term(command.getTerm())
                        .email(email)
                        .loanType(loanType)
                        .status(pendingStatus)
                        .build();
                })
            )
            .flatMap(loanApplicationRepository::createLoanRequest);
    }
    
    private Mono<LoanType> validateLoanType(Integer loanTypeId) {
        return loanTypeRepository.getById(loanTypeId)
            .switchIfEmpty(Mono.error(new LoanTypeNotFoundException("Invalid loan type ID: " + loanTypeId)));
    }
    
    private Mono<Void> validateAmountAndTerm(LoanType loanType, BigDecimal amount, Integer term) {
        if (amount.compareTo(loanType.getMinAmount()) < 0 || amount.compareTo(loanType.getMaxAmount()) > 0) {
            return Mono.error(new FieldValidationException(
                String.format("Amount %s is outside allowed range [%s - %s] for loan type %s", 
                    amount, loanType.getMinAmount(), loanType.getMaxAmount(), loanType.getName())));
        }
        
        if (term < loanType.getMinTerm() || term > loanType.getMaxTerm()) {
            return Mono.error(new FieldValidationException(
                String.format("Term %d is outside allowed range [%d - %d] for loan type %s", 
                    term, loanType.getMinTerm(), loanType.getMaxTerm(), loanType.getName())));
        }
        
        return Mono.empty();
    }
    
    private Mono<LoanApplicationStatus> validateLoanApplicationStatusByName(String status) {
        return loanApplicationStatusRepository.getByName(status)
                .switchIfEmpty(Mono.error(new LoanApplicationStatusNotFoundException("Loan request status not found")));
    }
}
