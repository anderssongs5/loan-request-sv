package co.com.powerup.ags.loan.request.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Loan request response data with filtered information")
public class LoanRequestResponseDto {

    @Schema(description = "Unique loan request identifier", example = "123e4567-e89b-12d3-a456-426614174000")
    private String id;
    
    @Schema(description = "User's email address", example = "andersson.garcia@example.com")
    private String email;
    
    @Schema(description = "Loan term in months", example = "24")
    private Integer term;
    
    @Schema(description = "Loan amount requested in currency units.", example = "2000")
    private BigDecimal amount;
    
    @Schema(description = "Current loan request status", example = "1")
    private Integer loanStatusId;
    
    @Schema(description = "Type of loan being requested. Must be a positive integer greater than 0.")
    private String loanTypeId;
}