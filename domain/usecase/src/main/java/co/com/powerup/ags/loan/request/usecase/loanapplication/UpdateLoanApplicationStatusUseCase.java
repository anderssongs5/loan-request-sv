package co.com.powerup.ags.loan.request.usecase.loanapplication;

import co.com.powerup.ags.loan.request.model.loanapplication.LoanApplication;
import co.com.powerup.ags.loan.request.model.loanapplication.LoanApplicationNotification;
import co.com.powerup.ags.loan.request.model.loanapplication.gateways.LoanApplicationRepository;
import co.com.powerup.ags.loan.request.model.loanapplicationstatus.LoanApplicationStatus;
import co.com.powerup.ags.loan.request.model.loanapplicationstatus.LoanApplicationStatusEnum;
import co.com.powerup.ags.loan.request.model.loanapplicationstatus.gateways.LoanApplicationStatusRepository;
import co.com.powerup.ags.loan.request.model.loantype.gateways.LoanTypeRepository;
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

import java.util.Set;

import static co.com.powerup.ags.loan.request.model.loanapplicationstatus.LoanApplicationStatusEnum.*;

@RequiredArgsConstructor
public class UpdateLoanApplicationStatusUseCase {
    
    public static final String LOAN_APPLICATION_NOT_FOUND = "Loan application not found";
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
                .switchIfEmpty(Mono.error(new UpdateLoanApplicationException("The new status is the same as the current status of the loan request.")))
                .flatMap(loanApplication -> updateStatus(loanApplication, command.status()))
                .flatMap(updatedLoanApplication -> 
                    notifyUser(updatedLoanApplication, null)
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
    
    private Mono<Void> notifyUser(LoanApplication updatedLoanApplication, String rejectReason) {
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
                .flatMap(notificationGateway::notify)
                .then();
    }
    
    public Mono<Void> updateLoanApplicationStatusFromAutoValidation(UpdateLoanAutomaticValidationCommand command) {
        LoanApplicationStatusEnum status = LoanApplicationStatusEnum.valueOf(command.validationResponse().analysis().decision());
        
        return loanApplicationRepository.getById(command.validationResponse().loanApplicationId())
                .switchIfEmpty(Mono.error(new LoanApplicationNotFoundException(LOAN_APPLICATION_NOT_FOUND)))
                .flatMap(loanApplication -> 
                    loanApplicationStatusRepository.getByName(status.name())
                        .map(newStatus -> {
                            if (loanApplication.getStatus().getId().equals(newStatus.getId())) {
                                throw new UpdateLoanApplicationException("The new status is the same as the current status of the loan request.");
                            }
                            return newStatus;
                        })
                        .flatMap(newStatus -> updateStatus(loanApplication, newStatus.getId()))
                )
                .flatMap(updatedLoanApplication ->
                        notifyUser(updatedLoanApplication, command.validationResponse().rejectionReason())
                                .then());
    }
}
