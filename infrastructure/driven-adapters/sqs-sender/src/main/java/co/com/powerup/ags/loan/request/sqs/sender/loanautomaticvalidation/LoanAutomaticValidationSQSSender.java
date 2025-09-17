package co.com.powerup.ags.loan.request.sqs.sender.loanautomaticvalidation;

import co.com.powerup.ags.loan.request.model.loanapplication.AutomaticValidationRequest;
import co.com.powerup.ags.loan.request.model.loanapplication.gateways.AutomaticValidationGateway;
import co.com.powerup.ags.loan.request.sqs.sender.loanautomaticvalidation.config.LoanAutomaticValidationSQSSenderProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

@Service
@Log4j2
@RequiredArgsConstructor
public class LoanAutomaticValidationSQSSender implements AutomaticValidationGateway {
    
    private final LoanAutomaticValidationSQSSenderProperties properties;
    private final SqsAsyncClient client;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private Mono<String> send(String message) {
        return Mono.fromCallable(() -> buildRequest(message))
                .flatMap(request -> Mono.fromFuture(client.sendMessage(request)))
                .doOnNext(response -> log.info("Message sent {}", response.messageId()))
                .doOnError(Exception.class, ex -> log.error("Error sending message {}", message, ex))
                .onErrorMap(Exception.class, ex -> new RuntimeException("Error sending automatic loan validation to SQS", ex))
                .map(SendMessageResponse::messageId);
    }

    private SendMessageRequest buildRequest(String message) {
        return SendMessageRequest.builder()
                .queueUrl(properties.queueUrl())
                .messageBody(message)
                .build();
    }
    
    private String getMessage(AutomaticValidationRequest loanApplication) {
        try {
            return objectMapper.writeValueAsString(loanApplication);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
    
    @Override
    public Mono<Void> validateLoanApplicationDecision(AutomaticValidationRequest request) {
        String message = getMessage(request);
        
        return send(message).then();
    }
}
