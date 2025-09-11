package co.com.powerup.ags.loan.request.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Builder
@Data
@Schema(description = "Standard error response format")
public class ErrorResponse {
    
    @Schema(description = "Timestamp when the error occurred", example = "2025-08-26T08:48:10.161513")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
    private LocalDateTime timestamp;
    
    @Schema(description = "Request path where error occurred", example = "/api/v1/users")
    private String path;
    
    @Schema(description = "HTTP status code", example = "409")
    private int status;
    
    @Schema(description = "HTTP status description", example = "Conflict")
    private String error;
    
    @Schema(description = "Unique request identifier", example = "7bf1f546-2")
    private String requestId;
    
    @Schema(description = "Application-specific error code", example = "DATA_ALREADY_EXISTS")
    private String code;
    
    @Schema(description = "Human-readable error message", example = "User already exists with email: test@example.com")
    private String message;
}
