package co.com.powerup.ags.loan.request.usecase.loanapplication;

import co.com.powerup.ags.loan.request.model.loanapplication.LoanApplication;
import co.com.powerup.ags.loan.request.model.loanapplication.LoanApplicationWithUser;
import co.com.powerup.ags.loan.request.model.loanapplication.gateways.LoanApplicationRepository;
import co.com.powerup.ags.loan.request.model.loanapplicationstatus.LoanApplicationStatus;
import co.com.powerup.ags.loan.request.model.loanapplicationstatus.LoanApplicationStatusEnum;
import co.com.powerup.ags.loan.request.model.loanapplicationstatus.gateways.LoanApplicationStatusRepository;
import co.com.powerup.ags.loan.request.model.loantype.gateways.LoanTypeRepository;
import co.com.powerup.ags.loan.request.model.notification.gateway.NotificationGateway;
import co.com.powerup.ags.loan.request.model.user.gateways.UserGateway;
import co.com.powerup.ags.loan.request.usecase.loanapplication.dto.UpdateLoanApplicationCommand;
import co.com.powerup.ags.loan.request.usecase.loanapplication.exception.LoanApplicationNotFoundException;
import co.com.powerup.ags.loan.request.usecase.loanapplication.exception.LoanApplicationStatusNotFoundException;
import co.com.powerup.ags.loan.request.usecase.loanapplication.exception.UpdateLoanApplicationException;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Set;

@RequiredArgsConstructor
public class UpdateLoanApplicationStatusUseCase {
    
    private final LoanApplicationRepository loanApplicationRepository;
    private final LoanApplicationStatusRepository loanApplicationStatusRepository;
    private final LoanTypeRepository loanTypeRepository;
    private final NotificationGateway notificationGateway;
    private final UserGateway userGateway;
    
    public Mono<LoanApplication> updateLoanApplicationStatus(UpdateLoanApplicationCommand command) {
        return loanApplicationRepository.getById(command.id())
                .switchIfEmpty(Mono.error(new LoanApplicationNotFoundException("Loan application not found")))
                .filter(la -> !la.getStatus().getId().equals(command.status()))
                .switchIfEmpty(Mono.error(new UpdateLoanApplicationException("The new status is the same as the current status of the loan request.")))
                .flatMap(loanApplication -> updateStatus(loanApplication, command.status()))
                .flatMap(updatedLoanApplication -> 
                    notifyUser(updatedLoanApplication)
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
    
    private Mono<Void> notifyUser(LoanApplication updatedLoanApplication) {
        Flux<LoanApplicationStatus> approvedAndRejected = loanApplicationStatusRepository
                .getByNames(Set.of(LoanApplicationStatusEnum.APPROVED.name(), LoanApplicationStatusEnum.REJECTED.name()));
        
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
                                .map(user -> LoanApplicationWithUser.builder()
                                        .loanRequest(loanApplication)
                                        .user(user)
                                        .build()))
                .flatMap(notificationGateway::notify)
                .then();
    }
}
