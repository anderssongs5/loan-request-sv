package co.com.powerup.ags.loan.request.api;

import co.com.powerup.ags.loan.request.api.dto.CreateLoanRequestDto;
import co.com.powerup.ags.loan.request.api.dto.ErrorResponse;
import co.com.powerup.ags.loan.request.api.dto.SuccessResponse;
import co.com.powerup.ags.loan.request.api.dto.UpdateLoanRequestDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springdoc.core.annotations.RouterOperation;
import org.springdoc.core.annotations.RouterOperations;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration
public class RouterRest {

    @Bean
    @RouterOperations({
        @RouterOperation(
            path = "/api/v1/loan-requests",
            method = RequestMethod.GET,
            operation = @Operation(
                tags = {"Loan Request"},
                summary = "Get loan requests requiring review",
                description = """
                    Retrieve paginated and filterable loan requests that require review.
                    
                    Returns a paginated response with detailed pagination metadata including total count, page information, and navigation flags.
                    Supports filtering by status, pagination, and sorting. Only ADVISOR role can access this endpoint.
                    
                    Response Structure:
                    - content: Array of loan application data
                    - pagination: Metadata object containing:
                      * currentPage: Current page number (0-based)
                      * pageSize: Number of items per page
                      * totalElements: Total number of matching records
                      * totalPages: Total number of pages
                      * numberOfElements: Number of items in current page
                      * hasNext/hasPrevious: Navigation flags
                      * first/last: Position flags
                    
                    Query Parameters:
                    - statuses: Comma-separated list of statuses to filter by (PENDING, REJECTED, MANUAL_REVIEW). Defaults to all allowed statuses if not provided.
                    - page: Page number (1-based). Default: 1
                    - size: Number of items per page (1-100). Default: 10
                    - sortBy: Field to sort by (amount, term, email, status). Default: request_id
                    - sortDirection: Sort direction (asc, desc). Default: asc
                    
                    Example: /api/v1/loan-requests?statuses=PENDING,REJECTED&page=1&size=20&sortBy=amount&sortDirection=desc
                    """,
                operationId = "getLoanRequestsByStatuses",
                parameters = {
                    @Parameter(
                        name = "statuses",
                        description = "Comma-separated list of loan application statuses to filter by. Valid values: PENDING, REJECTED, MANUAL_REVIEW",
                        example = "PENDING,REJECTED",
                        in = ParameterIn.QUERY,
                        required = false,
                        schema = @Schema(type = "string")
                    ),
                    @Parameter(
                        name = "page",
                        description = "Page number (0-based)",
                        example = "0",
                        in = ParameterIn.QUERY,
                        required = false,
                        schema = @Schema(type = "integer", minimum = "0")
                    ),
                    @Parameter(
                        name = "size",
                        description = "Number of items per page",
                        example = "10",
                        in = ParameterIn.QUERY,
                        required = false,
                        schema = @Schema(type = "integer", minimum = "1", maximum = "100")
                    ),
                    @Parameter(
                        name = "sortBy",
                        description = "Field to sort by",
                        example = "amount",
                        in = ParameterIn.QUERY,
                        required = false,
                        schema = @Schema(type = "string", allowableValues = {"amount", "term", "email", "status"})
                    ),
                    @Parameter(
                        name = "sortDirection",
                        description = "Sort direction",
                        example = "asc",
                        in = ParameterIn.QUERY,
                        required = false,
                        schema = @Schema(type = "string", allowableValues = {"asc", "desc"})
                    )
                },
                responses = {
                    @ApiResponse(
                        responseCode = "200",
                        description = "Loan requests requiring review retrieved successfully",
                        content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = SuccessResponse.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                name = "Success Response",
                                value = """
                                {
                                  "timestamp": "2025-09-10T14:30:00.123456",
                                  "path": "/api/v1/loan-requests?statuses=PENDING&page=1&size=10&sortBy=amount&sortDirection=asc",
                                  "data": {
                                    "content": [
                                      {
                                        "amount": 25000.00,
                                        "term": 12,
                                        "email": "user1@example.com",
                                        "name": "",
                                        "loanType": "Personal",
                                        "interestRate": 12.5,
                                        "applicationStatus": "PENDING",
                                        "baseSalary": null,
                                        "monthlyPaymentAmount": null
                                      },
                                      {
                                        "amount": 50000.00,
                                        "term": 24,
                                        "email": "user2@example.com",
                                        "name": "",
                                        "loanType": "Mortgage",
                                        "interestRate": 8.75,
                                        "applicationStatus": "PENDING",
                                        "baseSalary": null,
                                        "monthlyPaymentAmount": null
                                      }
                                    ],
                                    "pagination": {
                                      "currentPage": 0,
                                      "pageSize": 10,
                                      "totalElements": 25,
                                      "totalPages": 3,
                                      "numberOfElements": 2,
                                      "hasNext": true,
                                      "hasPrevious": false,
                                      "first": true,
                                      "last": false
                                    }
                                  },
                                  "message": "Loan requests requiring review retrieved successfully"
                                }
                                """
                            )
                        )
                    ),
                    @ApiResponse(
                        responseCode = "400",
                        description = "Bad Request - Invalid status values or query parameters",
                        content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = SuccessResponse.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                name = "Validation Error Response",
                                value = """
                                {
                                  "timestamp": "2025-09-10T14:30:00.123456",
                                  "path": "/api/v1/loan-requests",
                                  "data": null,
                                  "message": "Validation error: Invalid status: INVALID_STATUS. Allowed statuses are: [PENDING, REJECTED, MANUAL_REVIEW]"
                                }
                                """
                            )
                        )
                    ),
                    @ApiResponse(
                        responseCode = "403",
                        description = "Access denied - ADVISOR role required",
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
                tags = {"Loan Request"},
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
                                    "email": "andersson.garcia@example.com",
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
        ),
        @RouterOperation(
            path = "/api/v1/borrowing-capacity",
            method = RequestMethod.GET,
            operation = @Operation(
                tags = {"Borrowing Capacity"},
                summary = "Calculate borrowing capacity for a user",
                description = """
                    Calculate the borrowing capacity for a user based on their identification number.
                    
                    This endpoint calculates the maximum amount a user can borrow based on their financial profile,
                    specifically using the 35% income rule where the maximum borrowing capacity is calculated as
                    35% of the user's monthly income multiplied by the loan term.
                    
                    The calculation is performed by:
                    1. Retrieving user information from the user service using the provided ID number
                    2. Validating user eligibility and financial data
                    3. Applying the 35% income rule to calculate maximum borrowing capacity
                    4. Returning the calculated amount with appropriate precision
                    
                    Business Rules:
                    - Maximum borrowing capacity = (Monthly Income * 0.35) * Loan Term (months)
                    - User must exist in the system and have valid financial data
                    - ID number must be provided and cannot be empty or blank
                    
                    Access Control:
                    - This endpoint is accessible to authenticated users
                    - No specific role restrictions apply for borrowing capacity calculation
                    """,
                operationId = "getBorrowingCapacity",
                parameters = {
                    @Parameter(
                        name = "idNumber",
                        description = "User's identification number to calculate borrowing capacity for",
                        example = "1234567890",
                        in = ParameterIn.QUERY,
                        required = true,
                        schema = @Schema(type = "string", minLength = 1)
                    )
                },
                responses = {
                    @ApiResponse(
                        responseCode = "200",
                        description = "Borrowing capacity calculated successfully",
                        content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = SuccessResponse.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                name = "Success Response",
                                value = """
                                {
                                  "timestamp": "2025-09-18T14:30:00.123456",
                                  "path": "/api/v1/borrowing-capacity?idNumber=1234567890",
                                  "data": {
                                    "borrowingCapacity": "35000.00"
                                  },
                                  "message": "Borrowing capacity calculated successfully"
                                }
                                """
                            )
                        )
                    ),
                    @ApiResponse(
                        responseCode = "400",
                        description = "Bad Request - Missing or invalid ID number, or user validation failed",
                        content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                name = "Missing ID Number Error",
                                value = """
                                {
                                  "timestamp": "2025-09-18T14:30:00.123456",
                                  "path": "/api/v1/borrowing-capacity",
                                  "data": null,
                                  "message": "Id number must be provided"
                                }
                                """
                            )
                        )
                    ),
                    @ApiResponse(
                        responseCode = "404",
                        description = "User not found",
                        content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                name = "User Not Found Error",
                                value = """
                                {
                                  "timestamp": "2025-09-18T14:30:00.123456",
                                  "path": "/api/v1/borrowing-capacity?idNumber=9999999999",
                                  "data": null,
                                  "message": "User not found"
                                }
                                """
                            )
                        )
                    ),
                    @ApiResponse(
                        responseCode = "500",
                        description = "Internal Server Error - User service unavailable or unexpected error",
                        content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                name = "Service Unavailable Error",
                                value = """
                                {
                                  "timestamp": "2025-09-18T14:30:00.123456",
                                  "path": "/api/v1/borrowing-capacity?idNumber=1234567890",
                                  "data": null,
                                  "message": "User service unavailable"
                                }
                                """
                            )
                        )
                    )
                }
            )
        ),
        @RouterOperation(
            path = "/api/v1/loan-requests/{id}",
            method = RequestMethod.PUT,
            operation = @Operation(
                tags = {"Loan Request"},
                summary = "Update loan application status",
                description = """
                    Update the status of an existing loan application.
                    
                    This endpoint allows advisors to change the status of a loan application (e.g., approve, reject).
                    When the status is updated to APPROVED or REJECTED, the system will automatically:
                    - Send a notification to the user via SQS
                    - Include complete loan application details and user information in the notification
                    
                    Status validation:
                    - The new status must be different from the current status
                    - Only valid status IDs are accepted (must exist in the system)
                    
                    Access Control:
                    - Only users with ADVISOR role can update loan application statuses
                    - Authentication token must be provided in the Authorization header
                    """,
                operationId = "updateLoanApplicationStatus",
                parameters = {
                    @Parameter(
                        name = "id",
                        description = "Unique identifier of the loan application (UUID format)",
                        example = "29a4639d-b328-453a-a408-d2eff0bcae84",
                        in = ParameterIn.PATH,
                        required = true,
                        schema = @Schema(type = "string", format = "uuid")
                    )
                },
                requestBody = @RequestBody(
                    description = "Status update data",
                    required = true,
                    content = @Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = @Schema(implementation = UpdateLoanRequestDto.class)
                    )
                ),
                responses = {
                    @ApiResponse(
                        responseCode = "200",
                        description = "Loan application status updated successfully",
                        content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = SuccessResponse.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                name = "Success Response",
                                value = """
                                {
                                  "timestamp": "2025-09-14T14:30:00.123456",
                                  "path": "/api/v1/loan-requests/29a4639d-b328-453a-a408-d2eff0bcae84",
                                  "data": {
                                    "id": "29a4639d-b328-453a-a408-d2eff0bcae84",
                                    "email": "user@example.com",
                                    "term": 24,
                                    "amount": 50000.00,
                                    "loanStatusId": 3,
                                    "loanTypeId": 1
                                  },
                                  "message": "Loan application status updated successfully"
                                }
                                """
                            )
                        )
                    ),
                    @ApiResponse(
                        responseCode = "400",
                        description = "Bad Request - Invalid status ID or same status",
                        content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                name = "Same Status Error",
                                value = """
                                {
                                  "timestamp": "2025-09-14T14:30:00.123456",
                                  "path": "/api/v1/loan-requests/29a4639d-b328-453a-a408-d2eff0bcae84",
                                  "data": null,
                                  "message": "The new status is the same as the current status of the loan request."
                                }
                                """
                            )
                        )
                    ),
                    @ApiResponse(
                        responseCode = "404",
                        description = "Loan application not found or invalid status ID",
                        content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                name = "Not Found Error",
                                value = """
                                {
                                  "timestamp": "2025-09-14T14:30:00.123456",
                                  "path": "/api/v1/loan-requests/29a4639d-b328-453a-a408-d2eff0bcae84",
                                  "data": null,
                                  "message": "Loan application not found"
                                }
                                """
                            )
                        )
                    ),
                    @ApiResponse(
                        responseCode = "403",
                        description = "Access denied - ADVISOR role required",
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
        return RouterFunctions.route()
                .path("/api/v1/loan-requests", builder -> builder
                        .GET("", handlerV1::getLoanRequestsByStatuses)
                        .POST("", handlerV1::createLoanRequest)
                        .PUT("/{id}", handlerV1::updateLoanRequest))
                .path("/api/v1/loan-requests-2", builder -> builder
                        .GET("", handlerV1::getLoanRequestsByStatuses2))
                .path("/api/v1/borrowing-capacity", builder -> builder
                        .GET("", handlerV1::getBorrowingCapacity))
                .build();
    }
}
