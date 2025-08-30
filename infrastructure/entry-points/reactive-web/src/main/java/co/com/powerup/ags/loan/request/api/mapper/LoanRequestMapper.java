package co.com.powerup.ags.loan.request.api.mapper;

import co.com.powerup.ags.loan.request.api.dto.CreateLoanRequestDto;
import co.com.powerup.ags.loan.request.api.dto.LoanRequestResponseDto;
import co.com.powerup.ags.loan.request.model.loanapplication.LoanApplication;
import co.com.powerup.ags.loan.request.usecase.loanapplication.command.CreateLoanRequestCommand;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface LoanRequestMapper {
    
    LoanRequestMapper INSTANCE = Mappers.getMapper(LoanRequestMapper.class);
    
    CreateLoanRequestCommand toCommand(CreateLoanRequestDto dto);
    
    @Mapping(target = "loanStatusId", source = "status.id")
    @Mapping(target = "loanTypeId", source = "loanType.id")
    LoanRequestResponseDto toResponseDto(LoanApplication loanApplication);
}