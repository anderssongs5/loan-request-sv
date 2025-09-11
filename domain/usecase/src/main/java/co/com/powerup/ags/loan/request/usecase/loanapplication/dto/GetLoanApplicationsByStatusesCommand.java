package co.com.powerup.ags.loan.request.usecase.loanapplication.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetLoanApplicationsByStatusesCommand {

    private Set<String> statuses;
    private Integer page;
    private Integer size;
    private String sortBy;
    private String sortDirection;
}