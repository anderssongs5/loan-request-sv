package co.com.powerup.ags.loan.request.usecase.loanapplication.dto;

public record UpdateLoanAutomaticValidationCommand(Integer statusCode, ValidationResponse validationResponse) {
}
