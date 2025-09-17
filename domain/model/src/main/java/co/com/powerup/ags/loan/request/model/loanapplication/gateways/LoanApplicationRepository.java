package co.com.powerup.ags.loan.request.model.loanapplication.gateways;

import co.com.powerup.ags.loan.request.model.loanapplication.LoanApplication;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Set;

public interface LoanApplicationRepository {
    
    Mono<LoanApplication> saveLoanApplication(LoanApplication loanApplication);
    
    Flux<LoanApplication> getAllLoanRequests();
    
    Flux<LoanApplication> getLoanApplicationsPageableByStatuses(Set<String> statuses, Integer page, Integer size,
                                                                String sortBy, String sortDirection);
    
    Flux<LoanApplication> getLoanApplicationsPageableByStatuses2(Set<Integer> statuses, Integer page, Integer size,
                                                                String sortBy, String sortDirection);
    
    Mono<Long> countLoanApplicationsByStatuses(Set<String> statuses);
    
    Mono<Long> countLoanApplicationsByStatuses2(Set<Integer> statuses);
    
    Mono<LoanApplication> getById(String id);
    
    Mono<Long> countLoanApplicationsByStatusesAndEmail(Set<String> statuses, String email);
    
    Flux<LoanApplication> getLoanApplicationsPageableByStatusesAndEmail(Set<String> statuses, String email, 
                                                                        Integer page, Integer size, 
                                                                        String sortBy, String sortDirection);
}
