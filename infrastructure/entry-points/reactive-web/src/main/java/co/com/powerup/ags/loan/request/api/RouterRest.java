package co.com.powerup.ags.loan.request.api;

import co.com.powerup.ags.loan.request.api.dto.CreateLoanRequestDto;
import co.com.powerup.ags.loan.request.api.dto.ErrorResponse;
import co.com.powerup.ags.loan.request.api.dto.LoanRequestResponseDto;
import co.com.powerup.ags.loan.request.api.dto.SuccessResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springdoc.core.annotations.RouterOperation;
import org.springdoc.core.annotations.RouterOperations;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@Configuration
public class RouterRest {

    @Bean
    @RouterOperations({
        @RouterOperation(
            path = "/api/v1/loan-requests",
            method = RequestMethod.GET,
            operation = @Operation(
                summary = "Get all loan requests",
                description = "Retrieve a list of all loan requests",
                operationId = "listLoanRequests",
                responses = {
                    @ApiResponse(
                        responseCode = "200",
                        description = "Loan requests retrieved successfully",
                        content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = SuccessResponse.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                name = "Success Response",
                                value = """
                                {
                                  "timestamp": "2025-08-30T14:30:00.123456",
                                  "path": "/api/v1/loan-requests",
                                  "data": [
                                    {
                                      "id": "123e4567-e89b-12d3-a456-426614174000",
                                      "email": "andersson.garcia@example.com",
                                      "term": 24,
                                      "amount": 50000.00,
                                      "loanStatusId": 1,
                                      "loanTypeId": "1"
                                    },
                                    {
                                      "id": "456e7890-e89b-12d3-a456-426614174001",
                                      "email": "carlos.rodriguez@example.com",
                                      "term": 12,
                                      "amount": 25000.00,
                                      "loanStatusId": 1,
                                      "loanTypeId": "2"
                                    }
                                  ],
                                  "message": "Loan requests retrieved successfully"
                                }
                                """
                            )
                        )
                    ),
                    @ApiResponse(
                        responseCode = "500",
                        description = "Internal server error",
                        content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                        )
                    )
                }
            )
        ),
        @RouterOperation(
            path = "/api/v1/loan-requests",
            method = RequestMethod.POST,
            operation = @Operation(
                summary = "Create a new loan request",
                description = "Create a new loan request for a user",
                operationId = "createLoanRequest",
                requestBody = @RequestBody(
                    description = "Loan request creation data",
                    required = true,
                    content = @Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = @Schema(implementation = CreateLoanRequestDto.class)
                    )
                ),
                responses = {
                    @ApiResponse(
                        responseCode = "200",
                        description = "Loan request created successfully",
                        content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = SuccessResponse.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                name = "Success Response",
                                value = """
                                {
                                  "timestamp": "2025-08-30T14:30:00.123456",
                                  "path": "/api/v1/loan-requests",
                                  "data": {
                                    "id": "123e4567-e89b-12d3-a456-426614174000",
                                    "email": "john.doe@example.com",
                                    "term": 24,
                                    "amount": 50000.00,
                                    "loanStatusId": 1,
                                    "loanTypeId": "1"
                                  },
                                  "message": "Loan request created successfully"
                                }
                                """
                            )
                        )
                    ),
                    @ApiResponse(
                        responseCode = "400",
                        description = "Invalid input data",
                        content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                        )
                    ),
                    @ApiResponse(
                        responseCode = "404",
                        description = "User not found",
                        content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                        )
                    ),
                    @ApiResponse(
                        responseCode = "503",
                        description = "User service unavailable",
                        content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                        )
                    )
                }
            )
        )
    })
    public RouterFunction<ServerResponse> routerFunction(HandlerV1 handlerV1) {
        return route(GET("/api/v1/loan-requests"), handlerV1::listLoanRequests)
                .andRoute(POST("/api/v1/loan-requests"), handlerV1::createLoanRequest);
    }
}
