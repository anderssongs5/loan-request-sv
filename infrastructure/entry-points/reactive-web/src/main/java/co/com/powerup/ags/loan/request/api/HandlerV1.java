package co.com.powerup.ags.loan.request.api;

import co.com.powerup.ags.loan.request.api.dto.CreateLoanRequestDto;
import co.com.powerup.ags.loan.request.api.dto.SuccessResponse;
import co.com.powerup.ags.loan.request.api.mapper.LoanRequestMapper;
import co.com.powerup.ags.loan.request.usecase.loanapplication.LoanApplicationUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class HandlerV1 {

    private final LoanApplicationUseCase loanApplicationUseCase;
    private final Validator validator;

    public Mono<ServerResponse> listLoanRequests(ServerRequest serverRequest) {
        return loanApplicationUseCase.getAllLoanRequests()
                .map(LoanRequestMapper.INSTANCE::toResponseDto)
                .collectList()
                .flatMap(responseDtos -> {
                    SuccessResponse<Object> successResponse = SuccessResponse.builder()
                            .timestamp(LocalDateTime.now())
                            .path(serverRequest.path())
                            .data(responseDtos)
                            .message("Loan requests retrieved successfully")
                            .build();
                    return ServerResponse.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(successResponse);
                });
    }

    public Mono<ServerResponse> createLoanRequest(ServerRequest serverRequest) {
        return serverRequest.bodyToMono(CreateLoanRequestDto.class)
                .flatMap(this::validateRequest)
                .map(LoanRequestMapper.INSTANCE::toCommand)
                .flatMap(loanApplicationUseCase::createLoanRequest)
                .map(LoanRequestMapper.INSTANCE::toResponseDto)
                .flatMap(responseDto -> {
                    SuccessResponse<Object> successResponse = SuccessResponse.builder()
                            .timestamp(LocalDateTime.now())
                            .path(serverRequest.path())
                            .data(responseDto)
                            .message("Loan request created successfully")
                            .build();
                    return ServerResponse.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(successResponse);
                });
    }
    
    private <T> Mono<T> validateRequest(T request) {
        return Mono.fromCallable(() -> {
                    Errors errors = new BeanPropertyBindingResult(request, request.getClass().getSimpleName());
                    validator.validate(request, errors);
                    return errors;
                })
                .flatMap(errors -> {
                    if (errors.hasErrors()) {
                        String errorMessage = errors.getAllErrors().stream()
                                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                                .reduce((msg1, msg2) -> msg1 + "; " + msg2)
                                .orElse("Validation failed");
                        return Mono.error(new IllegalArgumentException(errorMessage));
                    }
                    return Mono.just(request);
                });
    }
}
