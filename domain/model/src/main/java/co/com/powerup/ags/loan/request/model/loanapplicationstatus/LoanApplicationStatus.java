package co.com.powerup.ags.loan.request.model.loanapplicationstatus;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class LoanApplicationStatus {
    
    private Integer id;
    private String name;
    private String description;
    
    public LoanApplicationStatus(Integer id) {
        this.id = id;
    }
}
