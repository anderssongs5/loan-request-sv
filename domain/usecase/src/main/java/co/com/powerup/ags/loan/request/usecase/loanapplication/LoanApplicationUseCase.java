package co.com.powerup.ags.loan.request.usecase.loanapplication;

import co.com.powerup.ags.loan.request.model.common.PagedResponse;
import co.com.powerup.ags.loan.request.model.exception.UserValidationException;
import co.com.powerup.ags.loan.request.model.loanapplication.LoanApplication;
import co.com.powerup.ags.loan.request.usecase.loanapplication.dto.LoanRequestRequiringReview;
import co.com.powerup.ags.loan.request.model.loanapplication.gateways.LoanApplicationRepository;
import co.com.powerup.ags.loan.request.model.loanapplicationstatus.LoanApplicationStatus;
import co.com.powerup.ags.loan.request.model.loanapplicationstatus.gateways.LoanApplicationStatusRepository;
import co.com.powerup.ags.loan.request.model.loantype.LoanType;
import co.com.powerup.ags.loan.request.model.loantype.gateways.LoanTypeRepository;
import co.com.powerup.ags.loan.request.model.user.User;
import co.com.powerup.ags.loan.request.model.user.gateways.UserGateway;
import co.com.powerup.ags.loan.request.usecase.loanapplication.dto.CreateLoanRequestCommand;
import co.com.powerup.ags.loan.request.usecase.loanapplication.dto.GetLoanApplicationsByStatusesCommand;
import co.com.powerup.ags.loan.request.usecase.loanapplication.exception.FieldValidationException;
import co.com.powerup.ags.loan.request.usecase.loanapplication.exception.LoanCreationForbiddenException;
import co.com.powerup.ags.loan.request.usecase.loanapplication.exception.LoanApplicationStatusNotFoundException;
import co.com.powerup.ags.loan.request.usecase.loanapplication.exception.LoanTypeNotFoundException;
import co.com.powerup.ags.loan.request.usecase.loanapplication.exception.UserNotFoundException;
import co.com.powerup.ags.loan.request.model.exception.UserServiceException;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static co.com.powerup.ags.loan.request.model.loanapplicationstatus.LoanApplicationStatusEnum.*;

@RequiredArgsConstructor
public class LoanApplicationUseCase {

    private static final Set<String> ALLOWED_REQUIRED_REVIEW_LOAN_APPLICATION_STATUSES = Set.of(
            PENDING.name(), REJECTED.name(), MANUAL_REVIEW.name()
    );

    private final LoanApplicationRepository loanApplicationRepository;
    private final LoanTypeRepository loanTypeRepository;
    private final LoanApplicationStatusRepository loanApplicationStatusRepository;
    private final UserGateway userGateway;
    
    public Flux<LoanApplication> getAllLoanRequests() {
        return loanApplicationRepository.getAllLoanRequests();
    }
    
    public Mono<PagedResponse<LoanRequestRequiringReview>> getLoanRequestsRequiringReview(GetLoanApplicationsByStatusesCommand command) {
        int page = Optional.ofNullable(command.getPage()).map(p -> p - 1).orElse(0);
        int size = Optional.ofNullable(command.getSize()).orElse(10);
        String sortBy = command.getSortBy();
        String sortDirection = Optional.ofNullable(command.getSortDirection()).orElse("asc");
        
        return validateAndGetStatuses(command.getStatuses())
                .flatMap(validatedStatuses -> {
                    Mono<Long> totalCountMono = loanApplicationRepository.countLoanApplicationsByStatuses(validatedStatuses);

                    Flux<LoanApplication> loanApplications = loanApplicationRepository.getLoanApplicationsPageableByStatuses(
                            validatedStatuses, page, size, sortBy, sortDirection);
                    
                    Mono<Map<String, User>> usersMapMono = loanApplications
                            .map(LoanApplication::getEmail)
                            .distinct()
                            .flatMap(email -> userGateway.getUserByIdNumberOrEmail(null, email)
                                    .map(user -> Map.entry(email, user)))
                            .collectMap(Map.Entry::getKey, Map.Entry::getValue);
                    
                    return Mono.zip(totalCountMono, loanApplications.collectList(), usersMapMono)
                            .flatMap(tuple3 -> {
                                Long totalElements = tuple3.getT1();
                                List<LoanApplication> loanRequests = tuple3.getT2();
                                Map<String, User> usersMap = tuple3.getT3();
                                
                                return Flux.fromIterable(loanRequests)
                                        .map(lr -> new LoanRequestRequiringReview(lr, usersMap.get(lr.getEmail())))
                                        .collectList()
                                        .map(requiringReview ->
                                                PagedResponse.of(requiringReview, page, size, totalElements));
                            });
                });
    }
    
    private Mono<Set<String>> validateAndGetStatuses(Set<String> requestedStatuses) {
        if (requestedStatuses == null || requestedStatuses.isEmpty()) {
            return Mono.just(ALLOWED_REQUIRED_REVIEW_LOAN_APPLICATION_STATUSES);
        }
        
        for (String status : requestedStatuses) {
            if (!ALLOWED_REQUIRED_REVIEW_LOAN_APPLICATION_STATUSES.contains(status)) {
                return Mono.error(new IllegalArgumentException("Invalid status: " + status + 
                    ". Allowed statuses are: " + ALLOWED_REQUIRED_REVIEW_LOAN_APPLICATION_STATUSES));
            }
        }
        
        return Mono.just(requestedStatuses);
    }
    
    public Mono<LoanApplication> createLoanRequest(CreateLoanRequestCommand command) {
        return validateLoanType(command.getLoanTypeId())
            .flatMap(loanType -> validateAmountAndTerm(loanType, command.getAmount(), command.getTerm())
                .then(userGateway.getUserByIdNumberOrEmail(command.getUserIdNumber(), null)
                    .onErrorMap(throwable -> {
                        if (throwable instanceof UserValidationException) {
                            return throwable;
                        }
                        
                        return new UserServiceException("User service unavailable", throwable);
                    })
                    .switchIfEmpty(Mono.error(new UserNotFoundException("User with ID number " + command.getUserIdNumber() + " does not exist")))
                )
                .flatMap(user -> validateCreatedByMatchesUserEmail(command.getCreatedBy(), user.getEmail())
                        .thenReturn(user))
                .zipWith(validateLoanApplicationStatusByName(PENDING.name()))
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
    
    private Mono<Void> validateCreatedByMatchesUserEmail(String createdBy, String userEmail) {
        if (createdBy == null || !createdBy.equals(userEmail)) {
            return Mono.error(new LoanCreationForbiddenException("User is not permitted to create this loan request."));
        }
        return Mono.empty();
    }
}
