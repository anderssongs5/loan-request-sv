package co.com.powerup.ags.loan.request.r2dbc.mapper;

import co.com.powerup.ags.loan.request.model.loanapplicationstatus.LoanApplicationStatus;
import co.com.powerup.ags.loan.request.r2dbc.entity.LoanRequestStatusEntity;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface LoanApplicationStatusMapper {
    
    LoanApplicationStatusMapper INSTANCE = Mappers.getMapper(LoanApplicationStatusMapper.class);
    
    LoanApplicationStatus toDomain(LoanRequestStatusEntity entity);
}
