package co.com.powerup.ags.loan.request.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request data for updating an existing loan request")
public class UpdateLoanRequestDto {
    
    @Schema(
        description = "Loan new status",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "Status is required")
    @Min(value = 1, message = "Status must be greater or equal than 1")
    private Integer status;
}