package co.com.powerup.ags.loan.request.api;

import co.com.powerup.ags.loan.request.api.dto.CreateLoanRequestDto;
import co.com.powerup.ags.loan.request.api.dto.LoanApplicationSummaryResponse;
import co.com.powerup.ags.loan.request.api.dto.SuccessResponse;
import co.com.powerup.ags.loan.request.api.dto.UpdateLoanRequestDto;
import co.com.powerup.ags.loan.request.api.mapper.LoanRequestMapper;
import co.com.powerup.ags.loan.request.model.common.PagedResponse;
import co.com.powerup.ags.loan.request.usecase.loanapplication.LoanApplicationUseCase;
import co.com.powerup.ags.loan.request.usecase.loanapplication.UpdateLoanApplicationStatusUseCase;
import co.com.powerup.ags.loan.request.usecase.loanapplication.dto.GetLoanApplicationsByStatusesCommand;
import co.com.powerup.ags.loan.request.usecase.loanapplication.dto.LoanRequestRequiringReview;
import co.com.powerup.ags.loan.request.usecase.loanapplication.dto.UpdateLoanApplicationCommand;
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
import java.util.UUID;
import java.util.function.Function;
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
    private final UpdateLoanApplicationStatusUseCase loanApplicationStatusUseCase;
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
        return handleGetLoanRequestsByStatuses(serverRequest, loanApplicationUseCase::getLoanRequestsRequiringReview);
    }
    
    public Mono<ServerResponse> getLoanRequestsByStatuses2(ServerRequest serverRequest) {
        return handleGetLoanRequestsByStatuses(serverRequest, loanApplicationUseCase::getLoanRequestsRequiringReview2);
    }
    
    private Mono<ServerResponse> handleGetLoanRequestsByStatuses(
            ServerRequest serverRequest, 
            Function<GetLoanApplicationsByStatusesCommand, Mono<PagedResponse<LoanRequestRequiringReview>>> useCaseHandler) {
        return parseQueryParameters(serverRequest)
                .flatMap(command -> useCaseHandler.apply(command)
                        .map(pagedResponse -> buildGetByStatusesSuccessResponse(serverRequest, pagedResponse))
                        .flatMap(successResponse -> ServerResponse.ok()
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(successResponse))
                )
                .onErrorResume(IllegalArgumentException.class, ex -> 
                        buildErrorResponse(serverRequest, ex)
                );
    }
    
    private Mono<GetLoanApplicationsByStatusesCommand> parseQueryParameters(ServerRequest serverRequest) {
        return Mono.fromCallable(() -> {
            Set<String> statuses = serverRequest.queryParam("statuses")
                    .map(s -> Arrays.stream(s.split(","))
                            .map(String::trim)
                            .collect(Collectors.toSet()))
                    .orElse(null);
            
            String pageParam = serverRequest.queryParam(PAGE_QUERY_PARAM).orElse(null);
            String sizeParam = serverRequest.queryParam(SIZE_QUERY_PARAM).orElse(null);
            String sortByParam = serverRequest.queryParam(SORT_BY_QUERY_PARAM).orElse(null);
            String sortDirectionParam = serverRequest.queryParam(SORT_DIRECTION_QUERY_PARAM).orElse(null);
            
            Integer page = validatePage(pageParam);
            Integer size = validateSize(sizeParam);
            String sortBy = validateSortBy(sortByParam);
            String sortDirection = validateSortDirection(sortDirectionParam);
            
            return GetLoanApplicationsByStatusesCommand.builder()
                    .statuses(statuses)
                    .page(page)
                    .size(size)
                    .sortBy(sortBy)
                    .sortDirection(sortDirection)
                    .build();
        });
    }
    
    private SuccessResponse<Object> buildGetByStatusesSuccessResponse(ServerRequest serverRequest,
                                                                      PagedResponse<LoanRequestRequiringReview> pagedResponse) {
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
    }
    
    private Mono<ServerResponse> buildErrorResponse(ServerRequest serverRequest, IllegalArgumentException ex) {
        return ServerResponse.badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(SuccessResponse.builder()
                        .timestamp(LocalDateTime.now())
                        .path(serverRequest.path())
                        .data(null)
                        .message("Validation error: " + ex.getMessage())
                        .build());
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
    
    public Mono<ServerResponse> updateLoanRequest(ServerRequest serverRequest) {
        String id = serverRequest.pathVariable("id");
        
        return serverRequest.bodyToMono(UpdateLoanRequestDto.class)
                .flatMap(this::validateRequest)
                .map(r -> LoanRequestMapper.INSTANCE.toCommand(id, r))
                .flatMap(this::validateId)
                .flatMap(loanApplicationStatusUseCase::updateLoanApplicationStatus)
                .map(LoanRequestMapper.INSTANCE::toResponseDto)
                .flatMap(responseDto -> {
                    SuccessResponse<Object> successResponse = SuccessResponse.builder()
                            .timestamp(LocalDateTime.now())
                            .path(serverRequest.path())
                            .data(responseDto)
                            .message("Loan request updated successfully")
                            .build();
                    return ServerResponse.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(successResponse);
                });
    }
    
    private Mono<UpdateLoanApplicationCommand> validateId(UpdateLoanApplicationCommand request) {
        return Mono.fromCallable(() -> {
            if (request.id() == null || request.id().isBlank()) {
                throw new IllegalArgumentException("Id cannot be empty or null");
            }
            
            try {
                UUID.fromString(request.id());
            } catch (Exception e) {
                throw new IllegalArgumentException("Id is invalid");
            }
            
            return request;
        });
    }
}
