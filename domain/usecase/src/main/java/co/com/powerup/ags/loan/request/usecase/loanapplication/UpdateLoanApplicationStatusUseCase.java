package co.com.powerup.ags.loan.request.usecase.loanapplication;

import co.com.powerup.ags.loan.request.model.loanapplication.LoanApplication;
import co.com.powerup.ags.loan.request.model.loanapplication.gateways.LoanApplicationRepository;
import co.com.powerup.ags.loan.request.model.loanapplicationstatus.LoanApplicationStatus;
import co.com.powerup.ags.loan.request.model.loanapplicationstatus.LoanApplicationStatusEnum;
import co.com.powerup.ags.loan.request.model.loanapplicationstatus.gateways.LoanApplicationStatusRepository;
import co.com.powerup.ags.loan.request.model.loantype.gateways.LoanTypeRepository;
import co.com.powerup.ags.loan.request.model.notification.LoanApplicationNotification;
import co.com.powerup.ags.loan.request.model.notification.gateway.NotificationGateway;
import co.com.powerup.ags.loan.request.model.user.gateways.UserGateway;
import co.com.powerup.ags.loan.request.usecase.loanapplication.dto.UpdateLoanApplicationCommand;
import co.com.powerup.ags.loan.request.usecase.common.exception.LoanApplicationNotFoundException;
import co.com.powerup.ags.loan.request.usecase.common.exception.LoanApplicationStatusNotFoundException;
import co.com.powerup.ags.loan.request.usecase.common.exception.UpdateLoanApplicationException;
import co.com.powerup.ags.loan.request.usecase.loanapplication.dto.UpdateLoanAutomaticValidationCommand;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Set;

import static co.com.powerup.ags.loan.request.model.loanapplicationstatus.LoanApplicationStatusEnum.*;

@RequiredArgsConstructor
public class UpdateLoanApplicationStatusUseCase {
    
    public static final String LOAN_APPLICATION_NOT_FOUND = "Loan application not found";
    public static final String NEW_STATUS_IS_INVALID = "The new status is the same as the current status of the loan request.";
    private final LoanApplicationRepository loanApplicationRepository;
    private final LoanApplicationStatusRepository loanApplicationStatusRepository;
    private final LoanTypeRepository loanTypeRepository;
    private final NotificationGateway notificationGateway;
    private final UserGateway userGateway;
    private final PaymentPlanUseCase paymentPlanUseCase;
    
    public Mono<LoanApplication> updateLoanApplicationStatus(UpdateLoanApplicationCommand command) {
        return loanApplicationRepository.getById(command.id())
                .switchIfEmpty(Mono.error(new LoanApplicationNotFoundException(LOAN_APPLICATION_NOT_FOUND)))
                .filter(la -> !la.getStatus().getId().equals(command.status()))
                .switchIfEmpty(Mono.error(new UpdateLoanApplicationException(NEW_STATUS_IS_INVALID)))
                .flatMap(loanApplication -> updateStatus(loanApplication, command.status()))
                .flatMap(updatedLoanApplication -> 
                    notifyUser(updatedLoanApplication, null, null)
                        .then(Mono.just(updatedLoanApplication)));
    }
    
    private Mono<LoanApplication> updateStatus(LoanApplication loanApplication, Integer newStatus) {
        return loanApplicationStatusRepository.getById(newStatus)
                .switchIfEmpty(Mono.error(new LoanApplicationStatusNotFoundException("Loan application status with id " + newStatus + " was not found")))
                .flatMap(las -> {
                    var toSave = loanApplication.toBuilder().status(new LoanApplicationStatus(newStatus)).build();
                    return loanApplicationRepository.saveLoanApplication(toSave);
                });
    }
    
    private Mono<Void> notifyUser(LoanApplication updatedLoanApplication, BigDecimal monthlyPayment, String rejectReason) {
        Flux<LoanApplicationStatus> approvedAndRejected = loanApplicationStatusRepository
                .getByNames(Set.of(APPROVED.name(), REJECTED.name(), UNDER_REVIEW.name()));
        
        return approvedAndRejected
                .filter(loanApplicationStatus -> updatedLoanApplication.getStatus().getId().equals(loanApplicationStatus.getId()))
                .switchIfEmpty(Mono.empty())
                .flatMap(loanApplicationStatus ->
                        loanTypeRepository.getById(updatedLoanApplication.getLoanType().getId())
                                .map(loanType ->
                                        updatedLoanApplication.toBuilder()
                                                .status(loanApplicationStatus)
                                                .loanType(loanType)
                                                .build()))
                .flatMap(loanApplication ->
                        userGateway.getUserByIdNumberOrEmail(null, loanApplication.getEmail())
                                .map(user -> LoanApplicationNotification.builder()
                                        .loanRequest(loanApplication)
                                        .user(user)
                                        .rejectReason(rejectReason)
                                        .build()))
                .flatMap(loanApplicationNotification ->
                        paymentPlanUseCase.calculatePaymentSchedule(loanApplicationNotification.getLoanRequest().getAmount(), monthlyPayment, loanApplicationNotification.getLoanRequest().getLoanType().getInterestRate(), loanApplicationNotification.getLoanRequest().getTerm())
                                .collectList()
                                .map(paymentPlan -> loanApplicationNotification.toBuilder()
                                        .paymentPlan(paymentPlan)
                                        .build()))
                .flatMap(notificationGateway::notify)
                .then();
    }
    
    public Mono<Void> updateLoanApplicationStatusFromAutoValidation(UpdateLoanAutomaticValidationCommand command) {
        return validateDecision(command.validationResponse().analysis().decision())
                .flatMap(statusEnum ->
                        fetchLoanApplicationAndStatus(command.validationResponse().loanApplicationId(), statusEnum))
                .filter(tuple -> !tuple.getT1().getStatus().getId().equals(tuple.getT2().getId()))
                .switchIfEmpty(Mono.error(new UpdateLoanApplicationException(NEW_STATUS_IS_INVALID)))
                .flatMap(tuple -> updateStatus(tuple.getT1(), tuple.getT2().getId()))
                .flatMap(updatedLoan -> notifyUser(updatedLoan,
                        command.validationResponse().analysis().newLoanMonthlyPayment(), 
                        command.validationResponse().rejectionReason()))
                .then();
    }
    
    private Mono<LoanApplicationStatusEnum> validateDecision(String decision) {
        try {
            return Mono.just(LoanApplicationStatusEnum.valueOf(decision));
        } catch (IllegalArgumentException ex) {
            return Mono.error(new UpdateLoanApplicationException("Invalid decision: " + decision));
        }
    }
    
    private Mono<reactor.util.function.Tuple2<LoanApplication, LoanApplicationStatus>> fetchLoanApplicationAndStatus(
            String loanApplicationId, LoanApplicationStatusEnum statusEnum) {
        
        Mono<LoanApplication> loanApplicationMono = loanApplicationRepository.getById(loanApplicationId)
                .switchIfEmpty(Mono.error(new LoanApplicationNotFoundException(LOAN_APPLICATION_NOT_FOUND)));
        
        Mono<LoanApplicationStatus> statusMono = loanApplicationStatusRepository.getByName(statusEnum.name())
                .switchIfEmpty(Mono.error(new LoanApplicationStatusNotFoundException(
                        "Loan application status '" + statusEnum.name() + "' not found")));
        
        return Mono.zip(loanApplicationMono, statusMono);
    }
}
