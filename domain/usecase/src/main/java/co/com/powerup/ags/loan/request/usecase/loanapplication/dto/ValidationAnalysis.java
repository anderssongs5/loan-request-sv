package co.com.powerup.ags.loan.request.usecase.loanapplication.dto;

import java.math.BigDecimal;

public record ValidationAnalysis(BigDecimal totalIncome, BigDecimal maxBorrowingCapacity, BigDecimal currentMonthlyDebt,
                                 BigDecimal availableCapacity, BigDecimal newLoanMonthlyPayment, String decision,
                                 String reasoning) {
}
