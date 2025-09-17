package co.com.powerup.ags.loan.request.usecase.loanapplication;

import co.com.powerup.ags.loan.request.usecase.loanapplication.dto.PaymentPlanItem;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class PaymentPlanUseCase {

    public List<PaymentPlanItem> calculatePaymentSchedule(BigDecimal amount, BigDecimal monthlyPayment,
                                                          BigDecimal annualRate, Integer termMonths) {
        List<PaymentPlanItem> paymentPlan = new ArrayList<>();
        BigDecimal remainingBalance = amount;
        
        if (monthlyPayment == null) {
            monthlyPayment = calculateMonthlyPayment(amount, annualRate, termMonths);
        }
        
        BigDecimal monthlyRate = annualRate.divide(java.math.BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP)
                .divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_UP);
        
        LocalDate currentDate = LocalDate.now().plusMonths(1);
        
        for (int i = 1; i <= termMonths; i++) {
            BigDecimal interestPayment = remainingBalance.multiply(monthlyRate)
                    .setScale(2, RoundingMode.HALF_UP);
            
            BigDecimal principalPayment = monthlyPayment.subtract(interestPayment);
            
            if (i == termMonths) {
                principalPayment = remainingBalance;
                monthlyPayment = principalPayment.add(interestPayment);
            }
            
            remainingBalance = remainingBalance.subtract(principalPayment);
            
            paymentPlan.add(PaymentPlanItem.builder()
                    .paymentNumber(i)
                    .dueDate(currentDate)
                    .principalPayment(principalPayment)
                    .interestPayment(interestPayment)
                    .totalPayment(monthlyPayment)
                    .remainingBalance(remainingBalance)
                    .build());
            
            currentDate = currentDate.plusMonths(1);
        }
        
        return paymentPlan;
    }
    
    // P × [r(1+r)^n] / [(1+r)^n - 1]
    private BigDecimal calculateMonthlyPayment(BigDecimal principal, BigDecimal annualRate, Integer termMonths) {
        if (annualRate.compareTo(BigDecimal.ZERO) == 0) {
            return principal.divide(BigDecimal.valueOf(termMonths), 2, RoundingMode.HALF_UP);
        }
        
        BigDecimal monthlyRate = annualRate.divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP)
                .divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_UP);
        
        BigDecimal onePlusRate = BigDecimal.ONE.add(monthlyRate);
        BigDecimal powerTerm = onePlusRate.pow(termMonths, MathContext.DECIMAL128);
        
        BigDecimal numerator = principal.multiply(monthlyRate).multiply(powerTerm);
        BigDecimal denominator = powerTerm.subtract(BigDecimal.ONE);
        
        return numerator.divide(denominator, 2, RoundingMode.HALF_UP);
    }
}
