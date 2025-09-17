package co.com.powerup.ags.loan.request.usecase.loanapplication;

import co.com.powerup.ags.loan.request.model.loanapplication.ApprovedLoan;
import co.com.powerup.ags.loan.request.model.loanapplication.AutomaticValidationRequest;
import co.com.powerup.ags.loan.request.model.loanapplication.gateways.AutomaticValidationGateway;
import co.com.powerup.ags.loan.request.model.common.PagedResponse;
import co.com.powerup.ags.loan.request.model.exception.UserValidationException;
import co.com.powerup.ags.loan.request.model.loanapplication.LoanApplication;
import co.com.powerup.ags.loan.request.model.loanapplicationstatus.LoanApplicationStatusEnum;
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
import co.com.powerup.ags.loan.request.usecase.common.exception.FieldValidationException;
import co.com.powerup.ags.loan.request.usecase.common.exception.LoanCreationForbiddenException;
import co.com.powerup.ags.loan.request.usecase.common.exception.LoanApplicationStatusNotFoundException;
import co.com.powerup.ags.loan.request.usecase.common.exception.LoanTypeNotFoundException;
import co.com.powerup.ags.loan.request.usecase.common.exception.UserNotFoundException;
import co.com.powerup.ags.loan.request.model.exception.UserServiceException;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

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
    private final AutomaticValidationGateway automaticValidationGateway;
    
    public Flux<LoanApplication> getAllLoanRequests() {
        return loanApplicationRepository.getAllLoanRequests();
    }
    
    public Mono<PagedResponse<LoanRequestRequiringReview>> getLoanRequestsRequiringReview(GetLoanApplicationsByStatusesCommand command) {
        return handleGetLoanRequestsRequiringReview(
                command,
                this::validateAndGetStatuses,
                loanApplicationRepository::getLoanApplicationsPageableByStatuses,
                loanApplicationRepository::countLoanApplicationsByStatuses,
                false
        );
    }
    
    public Mono<PagedResponse<LoanRequestRequiringReview>> getLoanRequestsRequiringReview2(GetLoanApplicationsByStatusesCommand command) {
        return handleGetLoanRequestsRequiringReview(
                command,
                this::validateAndGetStatuses2,
                loanApplicationRepository::getLoanApplicationsPageableByStatuses2,
                loanApplicationRepository::countLoanApplicationsByStatuses2,
                true
        );
    }
    
    private <T> Mono<PagedResponse<LoanRequestRequiringReview>> handleGetLoanRequestsRequiringReview(
            GetLoanApplicationsByStatusesCommand command,
            Function<Set<String>, Mono<T>> statusValidator,
            PaginatedQueryFunction<T> repositoryQueryFunction,
            Function<T, Mono<Long>> countFunction,
            boolean enrichWithFullData) {
        
        int page = Optional.ofNullable(command.getPage()).map(p -> p - 1).orElse(0);
        int size = Optional.ofNullable(command.getSize()).orElse(10);
        String sortBy = command.getSortBy();
        String sortDirection = Optional.ofNullable(command.getSortDirection()).orElse("asc");
        
        return statusValidator.apply(command.getStatuses())
                .flatMap(validatedStatuses -> {
                    Mono<Long> totalCountMono = countFunction.apply(validatedStatuses);
                    
                    Flux<LoanApplication> loanApplications = repositoryQueryFunction.apply(
                            validatedStatuses, page, size, sortBy, sortDirection);
                    
                    Mono<Map<String, User>> usersMapMono = buildUsersMap(loanApplications);
                    
                    if (enrichWithFullData) {
                        return enrichWithFullDataAndBuildResponse(loanApplications, totalCountMono, usersMapMono, page, size);
                    } else {
                        return buildSimpleResponse(loanApplications, totalCountMono, usersMapMono, page, size);
                    }
                });
    }
    
    @FunctionalInterface
    private interface PaginatedQueryFunction<T> {
        Flux<LoanApplication> apply(T statuses, int page, int size, String sortBy, String sortDirection);
    }
    
    private Mono<Map<String, User>> buildUsersMap(Flux<LoanApplication> loanApplications) {
        return loanApplications
                .map(LoanApplication::getEmail)
                .distinct()
                .flatMap(email -> userGateway.getUserByIdNumberOrEmail(null, email)
                        .map(user -> Map.entry(email, user)))
                .collectMap(Map.Entry::getKey, Map.Entry::getValue);
    }
    
    private Mono<PagedResponse<LoanRequestRequiringReview>> buildSimpleResponse(
            Flux<LoanApplication> loanApplications,
            Mono<Long> totalCountMono,
            Mono<Map<String, User>> usersMapMono,
            int page, int size) {
        
        return Mono.zip(totalCountMono, loanApplications.collectList(), usersMapMono)
                .flatMap(tuple3 -> {
                    Long totalElements = tuple3.getT1();
                    List<LoanApplication> loanRequests = tuple3.getT2();
                    Map<String, User> usersMap = tuple3.getT3();
                    
                    return Flux.fromIterable(loanRequests)
                            .map(lr -> new LoanRequestRequiringReview(lr, usersMap.get(lr.getEmail())))
                            .collectList()
                            .map(requiringReview -> PagedResponse.of(requiringReview, page, size, totalElements));
                });
    }
    
    private Mono<PagedResponse<LoanRequestRequiringReview>> enrichWithFullDataAndBuildResponse(
            Flux<LoanApplication> loanApplications,
            Mono<Long> totalCountMono,
            Mono<Map<String, User>> usersMapMono,
            int page, int size) {
        
        Flux<LoanType> loanTypeFlux = loanTypeRepository.getAll();
        Flux<LoanApplicationStatus> loanApplicationStatusFlux = loanApplicationStatusRepository.getAll();
        
        return Mono.zip(totalCountMono, usersMapMono, 
                       loanTypeFlux.collectMap(LoanType::getId), 
                       loanApplicationStatusFlux.collectMap(LoanApplicationStatus::getId))
                .flatMap(tuple4 -> {
                    Long totalElements = tuple4.getT1();
                    Map<String, User> usersMap = tuple4.getT2();
                    Map<Integer, LoanType> loanTypesMap = tuple4.getT3();
                    Map<Integer, LoanApplicationStatus> loanStatusMap = tuple4.getT4();
                    
                    return loanApplications
                            .map(la -> {
                                LoanType loanType = loanTypesMap.get(la.getLoanType().getId());
                                LoanApplicationStatus loanStatus = loanStatusMap.get(la.getStatus().getId());
                                
                                return la.toBuilder()
                                        .loanType(loanType)
                                        .status(loanStatus)
                                        .build();
                            })
                            .map(lr -> new LoanRequestRequiringReview(lr, usersMap.get(lr.getEmail())))
                            .collectList()
                            .map(requiringReview -> PagedResponse.of(requiringReview, page, size, totalElements));
                });
    }
    
    private Mono<Set<Integer>> validateAndGetStatuses2(Set<String> requestedStatuses) {
        if (requestedStatuses == null || requestedStatuses.isEmpty()) {
            requestedStatuses = ALLOWED_REQUIRED_REVIEW_LOAN_APPLICATION_STATUSES;
        }
        
        for (String status : requestedStatuses) {
            if (!ALLOWED_REQUIRED_REVIEW_LOAN_APPLICATION_STATUSES.contains(status)) {
                return Mono.error(new IllegalArgumentException("Invalid status: " + status +
                        ". Allowed statuses are: " + ALLOWED_REQUIRED_REVIEW_LOAN_APPLICATION_STATUSES));
            }
        }
        
        Set<String> finalRequestedStatuses = requestedStatuses;
        return loanApplicationStatusRepository.getAll()
                .filter(s -> finalRequestedStatuses.contains(s.getName()))
                .map(LoanApplicationStatus::getId)
                .collect(Collectors.toSet());
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
                .flatMap(tuple -> {
                    User user = tuple.getT1();
                    LoanApplicationStatus pendingStatus = tuple.getT2();
                    LoanApplication loanApplication = LoanApplication.builder()
                        .amount(command.getAmount())
                        .term(command.getTerm())
                        .email(user.getEmail())
                        .loanType(loanType)
                        .status(pendingStatus)
                        .build();
                    
                    return loanApplicationRepository.saveLoanApplication(loanApplication)
                        .flatMap(savedLoan -> processAutomaticValidationIfEnabled(savedLoan, user));
                })
            );
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
    
    private Mono<LoanApplication> processAutomaticValidationIfEnabled(LoanApplication loanApplication, User user) {
        if (loanApplication.getLoanType().getAutomaticValidation() != null && 
            loanApplication.getLoanType().getAutomaticValidation()) {

            performAutomaticValidation(loanApplication, user)
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe();

            return Mono.just(loanApplication);
        }
        
        return Mono.just(loanApplication);
    }
    
    @SuppressWarnings("unchecked")
    private Mono<Void> performAutomaticValidation(LoanApplication loanApplication, User user) {
        Set<String> approvedStatuses = Set.of(LoanApplicationStatusEnum.APPROVED.name());
        
        return loanApplicationRepository.countLoanApplicationsByStatusesAndEmail(approvedStatuses, user.getEmail())
                .flatMap(totalCount -> {
                    if (totalCount == 0) {
                        return Mono.<List<ApprovedLoan>>just(List.of());
                    }

                    int pageSize = 100;
                    int totalPages = (int) Math.ceil((double) totalCount / pageSize);
                    
                    return Flux.range(0, totalPages)
                            .flatMap(page -> loanApplicationRepository.getLoanApplicationsPageableByStatusesAndEmail(
                                    approvedStatuses, user.getEmail(), page, pageSize, null, "asc"))
                            .map(this::getApprovedLoan)
                            .collectList();
                })
                .cast(List.class)
                .flatMap(approvedLoans -> {
                    
                    AutomaticValidationRequest validationRequest = AutomaticValidationRequest.builder()
                        .loanApplicationId(loanApplication.getId())
                        .userEmail(loanApplication.getEmail())
                        .userIdNumber(user.getIdNumber())
                        .loanAmount(loanApplication.getAmount())
                        .termInMonths(loanApplication.getTerm())
                        .interestRate(loanApplication.getLoanType().getInterestRate())
                        .userBaseSalary(user.getBaseSalary())
                        .approvedLoans((List<ApprovedLoan>) approvedLoans)
                        .build();

                    return automaticValidationGateway.validateLoanApplicationDecision(validationRequest);
                })
                .then();
    }
    
    private ApprovedLoan getApprovedLoan(LoanApplication loanApplication) {
        return ApprovedLoan.builder()
                .amount(loanApplication.getAmount())
                .interestRate(loanApplication.getLoanType().getInterestRate())
                .term(loanApplication.getTerm())
                .build();
    }
}
