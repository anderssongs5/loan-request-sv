package co.com.powerup.ags.loan.request.sqs.sender.approvedloanreport;

import co.com.powerup.ags.loan.request.model.loanapplication.ApprovedLoanSummary;
import co.com.powerup.ags.loan.request.model.loanapplication.gateways.ApprovedLoanReportGateway;
import co.com.powerup.ags.loan.request.sqs.sender.approvedloanreport.config.ApprovedLoanReportSQSSenderProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import java.time.Instant;

@Service
@Log4j2
@RequiredArgsConstructor
public class ApprovedLoanReportSQSSender implements ApprovedLoanReportGateway {
    
    private final ApprovedLoanReportSQSSenderProperties properties;
    private final SqsAsyncClient client;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private Mono<String> send(String message) {
        return Mono.fromCallable(() -> buildRequest(message))
                .flatMap(request -> Mono.fromFuture(client.sendMessage(request)))
                .doOnNext(response -> log.info("Message sent {}", response.messageId()))
                .doOnError(Exception.class, ex -> log.error("Error sending message {}", message, ex))
                .onErrorMap(Exception.class, ex -> new RuntimeException("Error sending approved loan report to SQS", ex))
                .map(SendMessageResponse::messageId);
    }

    private SendMessageRequest buildRequest(String message) {
        return SendMessageRequest.builder()
                .queueUrl(properties.queueUrl())
                .messageBody(message)
                .build();
    }
    
    private String getMessage(ApprovedLoanSummary approvedLoanSummary) {
        try {
            return objectMapper.writeValueAsString(approvedLoanSummary);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
    
    @Override
    public Mono<Void> reportApprovedLoan(ApprovedLoanSummary approvedLoanSummary) {
        String message = getMessage(approvedLoanSummary);
        
        return send(message).then();
    }
}
