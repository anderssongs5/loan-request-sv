package co.com.powerup.ags.loan.request.usecase.loanapplication;

import co.com.powerup.ags.loan.request.model.notification.PaymentPlanItem;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public class PaymentPlanUseCase {

    public Flux<PaymentPlanItem> calculatePaymentSchedule(BigDecimal amount, BigDecimal monthlyPayment,
                                                          BigDecimal annualRate, Integer termMonths) {
        
        Mono<BigDecimal> monthlyPaymentMono = monthlyPayment != null ? 
                Mono.just(monthlyPayment) : calculateMonthlyPayment(amount, annualRate, termMonths);
        
        return monthlyPaymentMono
                .flatMapMany(finalMonthlyPayment -> {
                    BigDecimal monthlyRate = annualRate.divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP)
                            .divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_UP);
                    
                    LocalDate startDate = LocalDate.now().plusMonths(1);
                    
                    AtomicReference<BigDecimal> remainingBalance = new AtomicReference<>(amount);
                    AtomicReference<BigDecimal> currentMonthlyPayment = new AtomicReference<>(finalMonthlyPayment);
                    
                    return Flux.range(1, termMonths)
                            .map(paymentNumber -> {
                                BigDecimal currentBalance = remainingBalance.get();
                                BigDecimal currentPayment = currentMonthlyPayment.get();
                                
                                BigDecimal interestPayment = currentBalance.multiply(monthlyRate)
                                        .setScale(2, RoundingMode.HALF_UP);
                                
                                BigDecimal principalPayment = currentPayment.subtract(interestPayment);
                                
                                if (Objects.equals(paymentNumber, termMonths)) {
                                    principalPayment = currentBalance;
                                    currentPayment = principalPayment.add(interestPayment);
                                    currentMonthlyPayment.set(currentPayment);
                                }
                                
                                BigDecimal newBalance = currentBalance.subtract(principalPayment);
                                remainingBalance.set(newBalance);
                                
                                LocalDate dueDate = startDate.plusMonths(paymentNumber - 1);
                                
                                return PaymentPlanItem.builder()
                                        .paymentNumber(paymentNumber)
                                        .dueDate(dueDate)
                                        .principalPayment(principalPayment)
                                        .interestPayment(interestPayment)
                                        .totalPayment(currentPayment)
                                        .remainingBalance(newBalance)
                                        .build();
                            });
                });
    }
    
    // P × [r(1+r)^n] / [(1+r)^n - 1]
    private Mono<BigDecimal> calculateMonthlyPayment(BigDecimal principal, BigDecimal annualRate, Integer termMonths) {
        return Mono.fromCallable(() -> {
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
        });
    }
}
