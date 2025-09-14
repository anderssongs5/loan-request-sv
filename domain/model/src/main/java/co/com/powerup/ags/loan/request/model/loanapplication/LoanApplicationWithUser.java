package co.com.powerup.ags.loan.request.model.loanapplication;

import co.com.powerup.ags.loan.request.model.user.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class LoanApplicationWithUser {
    
    private LoanApplication loanRequest;
    private User user;
    
}
