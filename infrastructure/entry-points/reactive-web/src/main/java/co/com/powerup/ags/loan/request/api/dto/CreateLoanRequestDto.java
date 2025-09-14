package co.com.powerup.ags.loan.request.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request data for creating a new loan request")
public class CreateLoanRequestDto {
    
    @Schema(
        description = "User's identification number. Must not be null, empty, or contain only whitespace characters.",
        example = "12345678",
        requiredMode = Schema.RequiredMode.REQUIRED,
        minLength = 1
    )
    @NotBlank(message = "User ID number is required")
    private String userIdNumber;
    
    @Schema(
        description = "Type of loan being requested. Must be a positive integer greater than 0.",
        example = "1",
        requiredMode = Schema.RequiredMode.REQUIRED,
        minimum = "1"
    )
    @NotNull(message = "Loan type ID is required")
    @Min(value = 1, message = "Loan type ID must be greater than 0")
    private Integer loanTypeId;
    
    @Schema(
        description = "Loan amount requested in currency units. Must be a positive decimal value greater than 0.01.",
        example = "50000.00",
        requiredMode = Schema.RequiredMode.REQUIRED,
        minimum = "0.01",
        type = "number",
        format = "decimal"
    )
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;
    
    @Schema(
        description = "Loan payment term in months. Must be a positive integer of at least 1 month.",
        example = "24",
        requiredMode = Schema.RequiredMode.REQUIRED,
        minimum = "1"
    )
    @NotNull(message = "Term is required")
    @Min(value = 1, message = "Term must be at least 1 month")
    private Integer term;
}