package co.com.powerup.ags.loan.request.model.loanapplication;

import java.math.BigDecimal;
import java.time.Instant;

public record ApprovedLoanSummary(String loanId, BigDecimal amount, Instant approvedDate) {
}
