package co.com.powerup.ags.loan.request.consumer.api.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class User {
    
    private String id;
    private String name;
    private String lastName;
    private String address;
    private String phoneNumber;
    private LocalDate birthDate;
    private String email;
    private BigDecimal baseSalary;
    private String idNumber;
}
