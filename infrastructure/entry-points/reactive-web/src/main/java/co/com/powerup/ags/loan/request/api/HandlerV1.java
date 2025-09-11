package co.com.powerup.ags.loan.request.api;

import co.com.powerup.ags.loan.request.api.dto.CreateLoanRequestDto;
import co.com.powerup.ags.loan.request.api.dto.LoanApplicationSummaryResponse;
import co.com.powerup.ags.loan.request.api.dto.SuccessResponse;
import co.com.powerup.ags.loan.request.api.mapper.LoanRequestMapper;
import co.com.powerup.ags.loan.request.usecase.loanapplication.LoanApplicationUseCase;
import co.com.powerup.ags.loan.request.usecase.loanapplication.command.GetLoanApplicationsByStatusesCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class HandlerV1 {
    
    public static final String ANONYMOUS = "anonymous";
    public static final String PAGE_QUERY_PARAM = "page";
    public static final String SIZE_QUERY_PARAM = "size";
    public static final String SORT_BY_QUERY_PARAM = "sortBy";
    public static final String SORT_DIRECTION_QUERY_PARAM = "sortDirection";
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
                .zipWith(ReactiveSecurityContextHolder.getContext()
                        .map(ctx -> ctx.getAuthentication().getName())
                        .switchIfEmpty(Mono.just(ANONYMOUS)))
                .map(tuple -> LoanRequestMapper.INSTANCE.toCommand(tuple.getT1(), tuple.getT2()))
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

    public Mono<ServerResponse> getLoanRequestsByStatuses(ServerRequest serverRequest) {
        return Mono.fromCallable(() -> {
                    Set<String> statuses = serverRequest.queryParam("statuses")
                            .map(s -> Arrays.stream(s.split(","))
                                    .map(String::trim)
                                    .collect(Collectors.toSet()))
                            .orElse(null);
                    
                    Integer page = validatePage(serverRequest.queryParam(PAGE_QUERY_PARAM).orElse(null));
                    Integer size = validateSize(serverRequest.queryParam(SIZE_QUERY_PARAM).orElse(null));
                    String sortBy = validateSortBy(serverRequest.queryParam(SORT_BY_QUERY_PARAM).orElse(null));
                    String sortDirection = validateSortDirection(serverRequest.queryParam(SORT_DIRECTION_QUERY_PARAM).orElse(null));
                    
                    return GetLoanApplicationsByStatusesCommand.builder()
                            .statuses(statuses)
                            .page(page)
                            .size(size)
                            .sortBy(sortBy)
                            .sortDirection(sortDirection)
                            .build();
                })
                .flatMap(command -> loanApplicationUseCase.getLoanRequestsRequiringReview(command)
                        .map(pagedResponse -> {
                            List<LoanApplicationSummaryResponse> summaryList = pagedResponse.getContent()
                                    .stream()
                                    .map(LoanRequestMapper.INSTANCE::toSummaryResponse)
                                    .toList();
                            
                            return SuccessResponse.builder()
                                    .timestamp(LocalDateTime.now())
                                    .path(serverRequest.path())
                                    .data(Map.of(
                                            "content", summaryList,
                                            "pagination", pagedResponse.getPagination()
                                    ))
                                    .message("Loan requests requiring review retrieved successfully")
                                    .build();
                        })
                        .flatMap(successResponse -> ServerResponse.ok()
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(successResponse))
                )
                .onErrorResume(IllegalArgumentException.class, ex -> 
                        ServerResponse.badRequest()
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(SuccessResponse.builder()
                                        .timestamp(LocalDateTime.now())
                                        .path(serverRequest.path())
                                        .data(null)
                                        .message("Validation error: " + ex.getMessage())
                                        .build())
                );
    }
    
    private Integer validatePage(String pageParam) {
        if (pageParam == null || pageParam.trim().isEmpty()) {
            return null;
        }
        
        try {
            int page = Integer.parseInt(pageParam.trim());
            if (page < 1) {
                throw new IllegalArgumentException("Page number must be >= 1, provided: " + page);
            }
            return page;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Page must be a valid integer, provided: " + pageParam);
        }
    }
    
    private Integer validateSize(String sizeParam) {
        if (sizeParam == null || sizeParam.trim().isEmpty()) {
            return null;
        }
        
        try {
            int size = Integer.parseInt(sizeParam.trim());
            if (size < 1) {
                throw new IllegalArgumentException("Size must be >= 1, provided: " + size);
            }
            if (size > 100) {
                throw new IllegalArgumentException("Size must be <= 100, provided: " + size);
            }
            return size;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Size must be a valid integer, provided: " + sizeParam);
        }
    }
    
    private String validateSortBy(String sortByParam) {
        if (sortByParam == null || sortByParam.trim().isEmpty()) {
            return null;
        }
        
        String sortBy = sortByParam.trim();
        Set<String> allowedSortFields = Set.of("amount", "term", "email", "status");
        
        if (!allowedSortFields.contains(sortBy)) {
            throw new IllegalArgumentException("Invalid sortBy field: " + sortBy + 
                ". Allowed values: " + allowedSortFields);
        }
        
        return sortBy;
    }
    
    private String validateSortDirection(String sortDirectionParam) {
        if (sortDirectionParam == null || sortDirectionParam.trim().isEmpty()) {
            return null;
        }
        
        String sortDirection = sortDirectionParam.trim().toLowerCase();
        
        if (!sortDirection.equals("asc") && !sortDirection.equals("desc")) {
            throw new IllegalArgumentException("Invalid sortDirection: " + sortDirectionParam + 
                ". Allowed values: asc, desc");
        }
        
        return sortDirection;
    }
}
