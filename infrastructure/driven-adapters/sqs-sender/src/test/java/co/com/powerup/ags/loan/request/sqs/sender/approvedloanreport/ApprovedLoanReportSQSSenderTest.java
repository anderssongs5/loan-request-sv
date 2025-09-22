package co.com.powerup.ags.loan.request.sqs.sender.approvedloanreport;

import co.com.powerup.ags.loan.request.model.loanapplication.ApprovedLoanSummary;
import co.com.powerup.ags.loan.request.sqs.sender.approvedloanreport.config.ApprovedLoanReportSQSSenderProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;
import software.amazon.awssdk.services.sqs.model.SqsException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApprovedLoanReportSQSSenderTest {

    @Mock
    private SqsAsyncClient sqsAsyncClient;

    @Mock
    private ApprovedLoanReportSQSSenderProperties properties;

    private ApprovedLoanReportSQSSender approvedLoanReportSQSSender;
    private ObjectMapper objectMapper;

    private static final String QUEUE_URL = "https://sqs.us-east-1.amazonaws.com/123456789012/approved-loans-queue";
    private static final String MESSAGE_ID = "12345678-1234-1234-1234-123456789012";
    private static final String LOAN_ID = "loan-123";
    private static final BigDecimal AMOUNT = new BigDecimal("250000.00");
    private static final Instant APPROVED_DATE = Instant.parse("2024-01-15T10:30:00Z");

    @BeforeEach
    void setUp() {
        when(properties.queueUrl()).thenReturn(QUEUE_URL);
        
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        approvedLoanReportSQSSender = new ApprovedLoanReportSQSSender(properties, sqsAsyncClient);
    }

    @Test
    void shouldSendApprovedLoanReportSuccessfully() {
        ApprovedLoanSummary summary = new ApprovedLoanSummary(LOAN_ID, AMOUNT, APPROVED_DATE);
        
        SendMessageResponse response = SendMessageResponse.builder()
                .messageId(MESSAGE_ID)
                .build();
        
        when(sqsAsyncClient.sendMessage(any(SendMessageRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(response));
        
        StepVerifier.create(approvedLoanReportSQSSender.reportApprovedLoan(summary))
                .verifyComplete();

        ArgumentCaptor<SendMessageRequest> requestCaptor = ArgumentCaptor.forClass(SendMessageRequest.class);
        verify(sqsAsyncClient).sendMessage(requestCaptor.capture());
        
        SendMessageRequest capturedRequest = requestCaptor.getValue();
        assertEquals(QUEUE_URL, capturedRequest.queueUrl());
        
        String messageBody = capturedRequest.messageBody();
        assertNotNull(messageBody);
        assertTrue(messageBody.contains(LOAN_ID));
        assertTrue(messageBody.contains(AMOUNT.toString()));
        assertTrue(messageBody.contains("2024-01-15T10:30:00Z"));
    }

    @Test
    void shouldHandleSQSExceptionAndPropagateError() {
        ApprovedLoanSummary summary = new ApprovedLoanSummary(LOAN_ID, AMOUNT, APPROVED_DATE);
        
        AwsServiceException sqsException = SqsException.builder()
                .message("Queue not found")
                .build();
        
        when(sqsAsyncClient.sendMessage(any(SendMessageRequest.class)))
                .thenReturn(CompletableFuture.failedFuture(sqsException));
        
        StepVerifier.create(approvedLoanReportSQSSender.reportApprovedLoan(summary))
                .expectErrorMatches(throwable -> 
                    throwable instanceof RuntimeException &&
                    throwable.getMessage().contains("Error sending approved loan report to SQS") &&
                    throwable.getCause() == sqsException)
                .verify();

        verify(sqsAsyncClient).sendMessage(any(SendMessageRequest.class));
    }

    @Test
    void shouldValidateMessageBodyFormat() throws JsonProcessingException {
        ApprovedLoanSummary summary = new ApprovedLoanSummary(LOAN_ID, AMOUNT, APPROVED_DATE);
        
        SendMessageResponse response = SendMessageResponse.builder()
                .messageId(MESSAGE_ID)
                .build();
        
        when(sqsAsyncClient.sendMessage(any(SendMessageRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(response));
        
        StepVerifier.create(approvedLoanReportSQSSender.reportApprovedLoan(summary))
                .verifyComplete();
        
        ArgumentCaptor<SendMessageRequest> requestCaptor = ArgumentCaptor.forClass(SendMessageRequest.class);
        verify(sqsAsyncClient).sendMessage(requestCaptor.capture());
        
        String messageBody = requestCaptor.getValue().messageBody();
        
        ApprovedLoanSummary deserializedSummary = objectMapper.readValue(messageBody, ApprovedLoanSummary.class);
        
        assertEquals(LOAN_ID, deserializedSummary.loanId());
        assertEquals(AMOUNT, deserializedSummary.amount());
        assertEquals(APPROVED_DATE, deserializedSummary.approvedDate());
    }

    @Test
    void shouldHandleNullValues() {
        ApprovedLoanSummary summaryWithNulls = new ApprovedLoanSummary(null, null, null);
        
        SendMessageResponse response = SendMessageResponse.builder()
                .messageId(MESSAGE_ID)
                .build();
        
        when(sqsAsyncClient.sendMessage(any(SendMessageRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(response));
        
        StepVerifier.create(approvedLoanReportSQSSender.reportApprovedLoan(summaryWithNulls))
                .verifyComplete();

        verify(sqsAsyncClient).sendMessage(any(SendMessageRequest.class));
    }

    @Test
    void shouldHandleJsonProcessingException() {
        ApprovedLoanSummary summary = new ApprovedLoanSummary(LOAN_ID, AMOUNT, APPROVED_DATE);
        
        SendMessageResponse response = SendMessageResponse.builder()
                .messageId(MESSAGE_ID)
                .build();
        
        when(sqsAsyncClient.sendMessage(any(SendMessageRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(response));
        
        StepVerifier.create(approvedLoanReportSQSSender.reportApprovedLoan(summary))
                .verifyComplete();
    }

    @Test
    void shouldCreateCorrectSendMessageRequest() {
        ApprovedLoanSummary summary = new ApprovedLoanSummary(LOAN_ID, AMOUNT, APPROVED_DATE);
        
        SendMessageResponse response = SendMessageResponse.builder()
                .messageId(MESSAGE_ID)
                .build();
        
        when(sqsAsyncClient.sendMessage(any(SendMessageRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(response));
        
        StepVerifier.create(approvedLoanReportSQSSender.reportApprovedLoan(summary))
                .verifyComplete();
        
        ArgumentCaptor<SendMessageRequest> requestCaptor = ArgumentCaptor.forClass(SendMessageRequest.class);
        verify(sqsAsyncClient).sendMessage(requestCaptor.capture());
        
        SendMessageRequest request = requestCaptor.getValue();
        
        assertEquals(QUEUE_URL, request.queueUrl());
        assertNotNull(request.messageBody());
        assertFalse(request.messageBody().isEmpty());
    }

    @Test
    void shouldHandleCompletableFutureException() {
        ApprovedLoanSummary summary = new ApprovedLoanSummary(LOAN_ID, AMOUNT, APPROVED_DATE);
        
        RuntimeException runtimeException = new RuntimeException("Network error");
        CompletableFuture<SendMessageResponse> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(runtimeException);
        
        when(sqsAsyncClient.sendMessage(any(SendMessageRequest.class)))
                .thenReturn(failedFuture);
        
        StepVerifier.create(approvedLoanReportSQSSender.reportApprovedLoan(summary))
                .expectErrorMatches(throwable -> 
                    throwable instanceof RuntimeException &&
                    throwable.getMessage().contains("Error sending approved loan report to SQS") &&
                    throwable.getCause() == runtimeException)
                .verify();
    }

    @Test
    void shouldLogSuccessfulMessageSending() {
        ApprovedLoanSummary summary = new ApprovedLoanSummary(LOAN_ID, AMOUNT, APPROVED_DATE);
        
        SendMessageResponse response = SendMessageResponse.builder()
                .messageId(MESSAGE_ID)
                .build();
        
        when(sqsAsyncClient.sendMessage(any(SendMessageRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(response));
        
        StepVerifier.create(approvedLoanReportSQSSender.reportApprovedLoan(summary))
                .verifyComplete();
        
        verify(sqsAsyncClient).sendMessage(any(SendMessageRequest.class));
    }

    @Test
    void shouldUseCorrectObjectMapperConfiguration() {
        ApprovedLoanSummary summary = new ApprovedLoanSummary(LOAN_ID, AMOUNT, APPROVED_DATE);
        
        SendMessageResponse response = SendMessageResponse.builder()
                .messageId(MESSAGE_ID)
                .build();
        
        when(sqsAsyncClient.sendMessage(any(SendMessageRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(response));
        
        StepVerifier.create(approvedLoanReportSQSSender.reportApprovedLoan(summary))
                .verifyComplete();
        
        ArgumentCaptor<SendMessageRequest> requestCaptor = ArgumentCaptor.forClass(SendMessageRequest.class);
        verify(sqsAsyncClient).sendMessage(requestCaptor.capture());
        
        String messageBody = requestCaptor.getValue().messageBody();
        
        assertFalse(messageBody.contains("1705317000"));
        assertTrue(messageBody.contains("2024-01-15T10:30:00Z"));
    }
}