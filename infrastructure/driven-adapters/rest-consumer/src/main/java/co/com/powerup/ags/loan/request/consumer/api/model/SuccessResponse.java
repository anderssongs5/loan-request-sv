package co.com.powerup.ags.loan.request.consumer.api.model;

import java.util.Objects;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
* Standard success response wrapper
*/

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SuccessResponse {
    private OffsetDateTime timestamp = null;
    private String path = null;
    private Object data = null;
    private String message = null;
}