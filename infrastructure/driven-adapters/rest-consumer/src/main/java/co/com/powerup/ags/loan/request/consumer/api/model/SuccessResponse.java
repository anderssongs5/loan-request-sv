package co.com.powerup.ags.loan.request.consumer.api.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

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
public class SuccessResponse<T> {
    private LocalDateTime timestamp = null;
    private String path = null;
    private T data = null;
    private String message = null;
}