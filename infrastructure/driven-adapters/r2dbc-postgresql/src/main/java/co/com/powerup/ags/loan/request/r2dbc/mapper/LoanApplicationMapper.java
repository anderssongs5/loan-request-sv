package co.com.powerup.ags.loan.request.r2dbc.mapper;

import co.com.powerup.ags.loan.request.model.loanapplication.LoanApplication;
import co.com.powerup.ags.loan.request.r2dbc.entity.LoanRequestEntity;
import co.com.powerup.ags.loan.request.r2dbc.entity.LoanRequestWithDetailsEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface LoanApplicationMapper {
    
    LoanApplicationMapper INSTANCE = Mappers.getMapper(LoanApplicationMapper.class);
    
    @Mapping(target = "requestId", source = "id")
    @Mapping(target = "statusId", source = "status.id")
    @Mapping(target = "loanTypeId", source = "loanType.id")
    LoanRequestEntity toEntity(LoanApplication loanApplication);
    
    @Mapping(target = "id", source = "requestId")
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "loanType", ignore = true)
    LoanApplication toDomain(LoanRequestEntity entity);
    
    @Mapping(target = "id", source = "requestId")
    @Mapping(target = "status.id", source = "statusId")
    @Mapping(target = "status.name", source = "statusName")
    @Mapping(target = "status.description", source = "statusDescription")
    @Mapping(target = "loanType.id", source = "loanTypeId")
    @Mapping(target = "loanType.name", source = "typeName")
    @Mapping(target = "loanType.minAmount", source = "minimumAmount")
    @Mapping(target = "loanType.maxAmount", source = "maximumAmount")
    @Mapping(target = "loanType.minTerm", source = "minimumTerm")
    @Mapping(target = "loanType.maxTerm", source = "maximumTerm")
    @Mapping(target = "loanType.interestRate", source = "interestRate")
    @Mapping(target = "loanType.automaticValidation", source = "automaticValidation")
    LoanApplication toDomain(LoanRequestWithDetailsEntity detailsEntity);
}