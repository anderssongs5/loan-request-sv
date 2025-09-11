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
@Table("loan_requests")
public class LoanRequestEntity {

    @Id
    @Column("request_id")
    private String requestId;

    @Column("amount")
    private BigDecimal amount;

    @Column("term")
    private Integer term;

    @Column("email")
    private String email;

    @Column("status_id")
    private Integer statusId;

    @Column("loan_type_id")
    private Integer loanTypeId;
}