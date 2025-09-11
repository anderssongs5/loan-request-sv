package co.com.powerup.ags.loan.request.api.helper;

import co.com.powerup.ags.loan.request.api.exception.UnauthorizedException;
import co.com.powerup.ags.loan.request.api.exception.AccessDeniedException;
import co.com.powerup.ags.loan.request.model.exception.UserValidationException;
import co.com.powerup.ags.loan.request.usecase.loanapplication.exception.BusinessException;
import co.com.powerup.ags.loan.request.model.exception.UserServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.reactive.error.DefaultErrorAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;

import java.time.LocalDateTime;
import java.util.Map;

@Component
public class GlobalErrorAttributes extends DefaultErrorAttributes {
    
    private static final Logger log = LoggerFactory.getLogger(GlobalErrorAttributes.class);
    private static final String INTERNAL_SERVER_ERROR = "Internal Server Error";
    private static final String INVALID_INPUT = "INVALID_INPUT";
    private static final String BAD_REQUEST = "Bad Request";
    
    @Override
    public Map<String, Object> getErrorAttributes(ServerRequest serverRequest, ErrorAttributeOptions options) {
        Map<String, Object> errorAttributes = super.getErrorAttributes(serverRequest, options);
        Throwable error = getError(serverRequest);

        errorAttributes.remove("trace");
        errorAttributes.remove("exception");
        
        String path = getPath(serverRequest);
        
        switch (error) {
            case BusinessException businessException ->
                    setErrorAttributes(errorAttributes, HttpStatus.BAD_REQUEST, INVALID_INPUT,
                            BAD_REQUEST, businessException.getMessage(), path);
            case IllegalArgumentException illegalArgumentException ->
                    setErrorAttributes(errorAttributes, HttpStatus.BAD_REQUEST, INVALID_INPUT,
                            BAD_REQUEST, error.getMessage(), path);
            case UserValidationException userValidationException ->
                    setErrorAttributes(errorAttributes, HttpStatus.BAD_REQUEST, INVALID_INPUT,
                            BAD_REQUEST, error.getMessage(), path);
            case UnauthorizedException unauthorizedException -> {
                log.warn("Authorization failed", unauthorizedException);
                setErrorAttributes(errorAttributes, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED",
                        "Unauthorized", "Authorization is invalid", path);
            }
            case AccessDeniedException accessDeniedException -> {
                log.warn("Access denied", accessDeniedException);
                setErrorAttributes(errorAttributes, HttpStatus.FORBIDDEN, "ACCESS_DENIED",
                        "Forbidden", "Access denied. You don't have sufficient permissions to access this resource.", path);
            }
            case UserServiceException userServiceException -> {
                log.error("Error calling the user service", userServiceException);
                setErrorAttributes(errorAttributes, HttpStatus.SERVICE_UNAVAILABLE, "USER_SERVICE_UNAVAILABLE",
                        "Service Unavailable", "The user validation service is temporarily unavailable." +
                                " Please try again in a few moments or reach out administrators.",
                        path);
            }
            case null, default -> {
                log.error("Unexpected error", error);
                
                setErrorAttributes(errorAttributes, HttpStatus.INTERNAL_SERVER_ERROR, "UNEXPECTED_ERROR",
                        INTERNAL_SERVER_ERROR, "An unexpected error occurred, please contact administrators.",
                        getPath(serverRequest));
            }
        }
        
        errorAttributes.put("timestamp", LocalDateTime.now());
        errorAttributes.put("requestId", serverRequest.exchange().getRequest().getId());
        return errorAttributes;
    }
    
    private static String getPath(ServerRequest serverRequest) {
        return serverRequest.path() + (serverRequest.uri().getQuery() != null ? "?" + serverRequest.uri().getQuery() : "");
    }
    
    private void setErrorAttributes(Map<String, Object> errorAttributes, HttpStatus status,
                                    String code, String error, String message, String path) {
        errorAttributes.put("status", status.value());
        errorAttributes.put("code", code);
        errorAttributes.put("error", error);
        errorAttributes.put("message", message);
        errorAttributes.put("path", path);
    }
}
