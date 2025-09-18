package co.com.powerup.ags.loan.request.model.notification;

import co.com.powerup.ags.loan.request.model.loanapplication.LoanApplication;
import co.com.powerup.ags.loan.request.model.user.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class LoanApplicationNotification {
    
    private LoanApplication loanRequest;
    private User user;
    private String rejectReason;
    private List<PaymentPlanItem> paymentPlan;
}
