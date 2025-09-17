package co.com.powerup.ags.loan.request.usecase.loanapplication.dto;

public record ValidationResponse(String loanApplicationId, ValidationAnalysis analysis, boolean approved,
                                 String rejectionReason) {
}
