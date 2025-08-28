package co.com.powerup.ags.loan.request.consumer.api.model;

import java.util.Objects;
import java.math.BigDecimal;
import org.threeten.bp.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
* Request model for creating a new user
*/

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserRequest {
    private String name = null;
    private String lastName = null;
    private String address = null;
    private String phoneNumber = null;
    private LocalDate birthDate = null;
    private String email = null;
    private BigDecimal baseSalary = null;
}