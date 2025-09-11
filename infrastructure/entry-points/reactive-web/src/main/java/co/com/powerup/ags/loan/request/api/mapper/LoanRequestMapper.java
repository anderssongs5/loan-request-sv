package co.com.powerup.ags.loan.request.api.mapper;

import co.com.powerup.ags.loan.request.api.dto.CreateLoanRequestDto;
import co.com.powerup.ags.loan.request.api.dto.LoanApplicationSummaryResponse;
import co.com.powerup.ags.loan.request.api.dto.LoanRequestResponseDto;
import co.com.powerup.ags.loan.request.model.loanapplication.LoanApplication;
import co.com.powerup.ags.loan.request.model.loanapplication.LoanRequestRequiringReview;
import co.com.powerup.ags.loan.request.usecase.loanapplication.command.CreateLoanRequestCommand;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface LoanRequestMapper {
    
    LoanRequestMapper INSTANCE = Mappers.getMapper(LoanRequestMapper.class);
    
    @Mapping(target = "createdBy", source = "createdBy")
    CreateLoanRequestCommand toCommand(CreateLoanRequestDto dto, String createdBy);
    
    @Mapping(target = "loanStatusId", source = "status.id")
    @Mapping(target = "loanTypeId", source = "loanType.id")
    LoanRequestResponseDto toResponseDto(LoanApplication loanApplication);
    
    @Mapping(target = "name",
            expression = "java(loanRequestRequiringReview.getUserName().concat(\" \").concat(loanRequestRequiringReview.getUserLastName()))")
    @Mapping(target = "requestStatus", source = "status")
    @Mapping(target = "loanType", source = "loanType")
    LoanApplicationSummaryResponse toSummaryResponse(LoanRequestRequiringReview loanRequestRequiringReview);
}