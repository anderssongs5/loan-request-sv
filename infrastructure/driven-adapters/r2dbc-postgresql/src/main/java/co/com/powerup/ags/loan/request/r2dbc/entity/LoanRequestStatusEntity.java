package co.com.powerup.ags.loan.request.r2dbc.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table("loan_request_statuses")
public class LoanRequestStatusEntity {

    @Id
    @Column("status_id")
    private Integer id;

    @Column("name")
    private String name;

    @Column("description")
    private String description;
}