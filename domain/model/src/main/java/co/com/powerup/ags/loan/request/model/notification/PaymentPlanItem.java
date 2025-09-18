package co.com.powerup.ags.loan.request.model.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class PaymentPlanItem {

    private Integer paymentNumber;
    private LocalDate dueDate;
    private BigDecimal principalPayment;
    private BigDecimal interestPayment;
    private BigDecimal totalPayment;
    private BigDecimal remainingBalance;
}
