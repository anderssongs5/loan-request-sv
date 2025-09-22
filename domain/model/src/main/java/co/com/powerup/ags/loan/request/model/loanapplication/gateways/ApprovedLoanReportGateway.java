package co.com.powerup.ags.loan.request.model.loanapplication.gateways;

import co.com.powerup.ags.loan.request.model.loanapplication.ApprovedLoanSummary;
import reactor.core.publisher.Mono;

public interface ApprovedLoanReportGateway {
    
    Mono<Void> reportApprovedLoan(ApprovedLoanSummary approvedLoanSummary);
}
