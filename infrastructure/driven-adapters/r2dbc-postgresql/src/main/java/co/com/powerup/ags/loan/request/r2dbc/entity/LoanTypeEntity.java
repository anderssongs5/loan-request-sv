package co.com.powerup.ags.loan.request.r2dbc.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table("loan_types")
public class LoanTypeEntity {

    @Id
    @Column("loan_type_id")
    private Integer id;

    @Column("name")
    private String name;

    @Column("minimum_amount")
    private BigDecimal minAmount;

    @Column("maximum_amount")
    private BigDecimal maxAmount;
    
    @Column("minimum_term")
    private Integer minTerm;
    
    @Column("maximum_term")
    private Integer maxTerm;

    @Column("interest_rate")
    private BigDecimal interestRate;

    @Column("automatic_validation")
    private Boolean automaticValidation;
}