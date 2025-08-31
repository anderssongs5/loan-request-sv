package co.com.powerup.ags.loan.request.consumer.api.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
* Standard error response format
*/

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class ErrorResponse {
    private LocalDateTime timestamp = null;
    private String path = null;
    private Integer status = null;
    private String error = null;
    private String requestId = null;
    private String code = null;
    private String message = null;
}