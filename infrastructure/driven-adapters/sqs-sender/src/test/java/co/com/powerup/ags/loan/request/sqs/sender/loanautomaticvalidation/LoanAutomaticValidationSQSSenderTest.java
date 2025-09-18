package co.com.powerup.ags.loan.request.sqs.sender.loanautomaticvalidation;

import co.com.powerup.ags.loan.request.model.loanapplication.ApprovedLoan;
import co.com.powerup.ags.loan.request.model.loanapplication.AutomaticValidationRequest;
import co.com.powerup.ags.loan.request.sqs.sender.loanautomaticvalidation.config.LoanAutomaticValidationSQSSenderProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoanAutomaticValidationSQSSenderTest {

    @Mock
    private SqsAsyncClient sqsAsyncClient;

    @Mock
    private LoanAutomaticValidationSQSSenderProperties properties;

    private LoanAutomaticValidationSQSSender loanAutomaticValidationSQSSender;

    private AutomaticValidationRequest sampleValidationRequest;

    private static final String QUEUE_URL = "https://sqs.us-east-1.amazonaws.com/123456789012/loan-automatic-validation";
    private static final String MESSAGE_ID = "87654321-4321-4321-4321-210987654321";
    private static final String LOAN_ID = "f47ac10b-58cc-4372-a567-0e02b2c3d479";
    private static final String USER_EMAIL = "ana.garcia@example.com";
    private static final String USER_ID_NUMBER = "87654321";
    private static final BigDecimal LOAN_AMOUNT = new BigDecimal("75000.00");
    private static final Integer TERM_MONTHS = 36;
    private static final BigDecimal INTEREST_RATE = new BigDecimal("15.5");
    private static final BigDecimal USER_SALARY = new BigDecimal("6000000.00");

    @BeforeEach
    void setUp() {
        when(properties.queueUrl()).thenReturn(QUEUE_URL);

        loanAutomaticValidationSQSSender = new LoanAutomaticValidationSQSSender(properties, sqsAsyncClient);

        ApprovedLoan previousLoan1 = ApprovedLoan.builder()
                .amount(new BigDecimal("30000.00"))
                .term(24)
                .interestRate(new BigDecimal("12.0"))
                .build();

        ApprovedLoan previousLoan2 = ApprovedLoan.builder()
                .amount(new BigDecimal("20000.00"))
                .term(18)
                .interestRate(new BigDecimal("14.5"))
                .build();

        sampleValidationRequest = AutomaticValidationRequest.builder()
                .loanApplicationId(LOAN_ID)
                .userEmail(USER_EMAIL)
                .userIdNumber(USER_ID_NUMBER)
                .loanAmount(LOAN_AMOUNT)
                .termInMonths(TERM_MONTHS)
                .interestRate(INTEREST_RATE)
                .userBaseSalary(USER_SALARY)
                .approvedLoans(Arrays.asList(previousLoan1, previousLoan2))
                .build();
    }

    @Test
    void shouldSendValidationRequestSuccessfully() {
        SendMessageResponse mockResponse = SendMessageResponse.builder()
                .messageId(MESSAGE_ID)
                .build();

        when(sqsAsyncClient.sendMessage(any(SendMessageRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(mockResponse));

        StepVerifier.create(loanAutomaticValidationSQSSender.validateLoanApplicationDecision(sampleValidationRequest))
                .verifyComplete();

        ArgumentCaptor<SendMessageRequest> requestCaptor = ArgumentCaptor.forClass(SendMessageRequest.class);
        verify(sqsAsyncClient).sendMessage(requestCaptor.capture());

        SendMessageRequest sentRequest = requestCaptor.getValue();
        assertEquals(QUEUE_URL, sentRequest.queueUrl());
        assertNotNull(sentRequest.messageBody());
        assertTrue(sentRequest.messageBody().contains(LOAN_ID));
        assertTrue(sentRequest.messageBody().contains(USER_EMAIL));
        assertTrue(sentRequest.messageBody().contains(USER_ID_NUMBER));
    }

    @Test
    void shouldHandleSQSClientException() {
        RuntimeException sqsException = new RuntimeException("SQS service unavailable");
        when(sqsAsyncClient.sendMessage(any(SendMessageRequest.class)))
                .thenReturn(CompletableFuture.failedFuture(sqsException));

        StepVerifier.create(loanAutomaticValidationSQSSender.validateLoanApplicationDecision(sampleValidationRequest))
                .expectErrorMatches(throwable -> 
                    throwable instanceof RuntimeException &&
                    throwable.getMessage().contains("Error sending automatic loan validation to SQS"))
                .verify();
    }

    @Test
    void shouldIncludeAllRequiredDataInMessage() {
        SendMessageResponse mockResponse = SendMessageResponse.builder()
                .messageId(MESSAGE_ID)
                .build();

        when(sqsAsyncClient.sendMessage(any(SendMessageRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(mockResponse));

        StepVerifier.create(loanAutomaticValidationSQSSender.validateLoanApplicationDecision(sampleValidationRequest))
                .verifyComplete();

        ArgumentCaptor<SendMessageRequest> requestCaptor = ArgumentCaptor.forClass(SendMessageRequest.class);
        verify(sqsAsyncClient).sendMessage(requestCaptor.capture());

        String messageBody = requestCaptor.getValue().messageBody();
        
        assertTrue(messageBody.contains(LOAN_ID));
        assertTrue(messageBody.contains(USER_EMAIL));
        assertTrue(messageBody.contains(USER_ID_NUMBER));
        assertTrue(messageBody.contains("75000"));
        assertTrue(messageBody.contains("36"));
        assertTrue(messageBody.contains("15.5"));
        assertTrue(messageBody.contains("6000000"));
        assertTrue(messageBody.contains("30000"));
        assertTrue(messageBody.contains("20000"));
        assertTrue(messageBody.contains("approvedLoans"));
    }

    @Test
    void shouldHandleRequestWithNoApprovedLoans() {
        AutomaticValidationRequest requestWithoutLoans = sampleValidationRequest.toBuilder()
                .approvedLoans(Collections.emptyList())
                .build();

        SendMessageResponse mockResponse = SendMessageResponse.builder()
                .messageId(MESSAGE_ID)
                .build();

        when(sqsAsyncClient.sendMessage(any(SendMessageRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(mockResponse));

        StepVerifier.create(loanAutomaticValidationSQSSender.validateLoanApplicationDecision(requestWithoutLoans))
                .verifyComplete();

        ArgumentCaptor<SendMessageRequest> requestCaptor = ArgumentCaptor.forClass(SendMessageRequest.class);
        verify(sqsAsyncClient).sendMessage(requestCaptor.capture());

        String messageBody = requestCaptor.getValue().messageBody();
        assertTrue(messageBody.contains("approvedLoans"));
        assertTrue(messageBody.contains("[]")); // Empty array
    }

    @Test
    void shouldHandleRequestWithNullApprovedLoans() {
        AutomaticValidationRequest requestWithNullLoans = sampleValidationRequest.toBuilder()
                .approvedLoans(null)
                .build();

        SendMessageResponse mockResponse = SendMessageResponse.builder()
                .messageId(MESSAGE_ID)
                .build();

        when(sqsAsyncClient.sendMessage(any(SendMessageRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(mockResponse));

        StepVerifier.create(loanAutomaticValidationSQSSender.validateLoanApplicationDecision(requestWithNullLoans))
                .verifyComplete();

        ArgumentCaptor<SendMessageRequest> requestCaptor = ArgumentCaptor.forClass(SendMessageRequest.class);
        verify(sqsAsyncClient).sendMessage(requestCaptor.capture());

        String messageBody = requestCaptor.getValue().messageBody();
        assertTrue(messageBody.contains("approvedLoans"));
        assertTrue(messageBody.contains("null") || !messageBody.contains("approvedLoans\":["));
    }

    @Test
    void shouldHandleMinimalValidationRequest() {
        AutomaticValidationRequest minimalRequest = AutomaticValidationRequest.builder()
                .loanApplicationId(LOAN_ID)
                .userEmail(USER_EMAIL)
                .loanAmount(new BigDecimal("10000.00"))
                .termInMonths(12)
                .build();

        SendMessageResponse mockResponse = SendMessageResponse.builder()
                .messageId(MESSAGE_ID)
                .build();

        when(sqsAsyncClient.sendMessage(any(SendMessageRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(mockResponse));

        StepVerifier.create(loanAutomaticValidationSQSSender.validateLoanApplicationDecision(minimalRequest))
                .verifyComplete();

        ArgumentCaptor<SendMessageRequest> requestCaptor = ArgumentCaptor.forClass(SendMessageRequest.class);
        verify(sqsAsyncClient).sendMessage(requestCaptor.capture());

        String messageBody = requestCaptor.getValue().messageBody();
        assertTrue(messageBody.contains(LOAN_ID));
        assertTrue(messageBody.contains(USER_EMAIL));
        assertTrue(messageBody.contains("10000"));
        assertTrue(messageBody.contains("12"));
    }

    @Test
    void shouldUseCorrectQueueUrl() {
        String customQueueUrl = "https://sqs.eu-west-1.amazonaws.com/987654321098/custom-loan-validation";
        when(properties.queueUrl()).thenReturn(customQueueUrl);

        LoanAutomaticValidationSQSSender customSender = new LoanAutomaticValidationSQSSender(properties, sqsAsyncClient);

        SendMessageResponse mockResponse = SendMessageResponse.builder()
                .messageId(MESSAGE_ID)
                .build();

        when(sqsAsyncClient.sendMessage(any(SendMessageRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(mockResponse));

        StepVerifier.create(customSender.validateLoanApplicationDecision(sampleValidationRequest))
                .verifyComplete();

        ArgumentCaptor<SendMessageRequest> requestCaptor = ArgumentCaptor.forClass(SendMessageRequest.class);
        verify(sqsAsyncClient).sendMessage(requestCaptor.capture());

        assertEquals(customQueueUrl, requestCaptor.getValue().queueUrl());
    }

    @Test
    void shouldHandleNullValidationRequest() {
        StepVerifier.create(loanAutomaticValidationSQSSender.validateLoanApplicationDecision(null))
                .expectError(RuntimeException.class)
                .verify();
    }

    @Test
    void shouldHandleJsonSerializationErrors() {
        AutomaticValidationRequest invalidRequest = AutomaticValidationRequest.builder()
                .loanApplicationId(LOAN_ID)
                .userEmail(USER_EMAIL)
                .loanAmount(LOAN_AMOUNT)
                .termInMonths(TERM_MONTHS)
                .build();

        SendMessageResponse mockResponse = SendMessageResponse.builder()
                .messageId(MESSAGE_ID)
                .build();

        when(sqsAsyncClient.sendMessage(any(SendMessageRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(mockResponse));

        StepVerifier.create(loanAutomaticValidationSQSSender.validateLoanApplicationDecision(invalidRequest))
                .verifyComplete();

        verify(sqsAsyncClient).sendMessage(any(SendMessageRequest.class));
    }

    @Test
    void shouldHandleLargeAmountValidation() {
        AutomaticValidationRequest largeAmountRequest = sampleValidationRequest.toBuilder()
                .loanAmount(new BigDecimal("999999999.99"))
                .userBaseSalary(new BigDecimal("50000000.00"))
                .build();

        SendMessageResponse mockResponse = SendMessageResponse.builder()
                .messageId(MESSAGE_ID)
                .build();

        when(sqsAsyncClient.sendMessage(any(SendMessageRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(mockResponse));

        StepVerifier.create(loanAutomaticValidationSQSSender.validateLoanApplicationDecision(largeAmountRequest))
                .verifyComplete();

        ArgumentCaptor<SendMessageRequest> requestCaptor = ArgumentCaptor.forClass(SendMessageRequest.class);
        verify(sqsAsyncClient).sendMessage(requestCaptor.capture());

        String messageBody = requestCaptor.getValue().messageBody();
        assertTrue(messageBody.contains("999999999.99"));
        assertTrue(messageBody.contains("50000000"));
    }

    @Test
    void shouldHandleMultipleApprovedLoans() {
        ApprovedLoan loan1 = ApprovedLoan.builder()
                .amount(new BigDecimal("15000.00"))
                .term(12)
                .interestRate(new BigDecimal("10.5"))
                .build();

        ApprovedLoan loan2 = ApprovedLoan.builder()
                .amount(new BigDecimal("25000.00"))
                .term(24)
                .interestRate(new BigDecimal("13.0"))
                .build();

        ApprovedLoan loan3 = ApprovedLoan.builder()
                .amount(new BigDecimal("35000.00"))
                .term(36)
                .interestRate(new BigDecimal("16.5"))
                .build();

        AutomaticValidationRequest multipleLoansRequest = sampleValidationRequest.toBuilder()
                .approvedLoans(Arrays.asList(loan1, loan2, loan3))
                .build();

        SendMessageResponse mockResponse = SendMessageResponse.builder()
                .messageId(MESSAGE_ID)
                .build();

        when(sqsAsyncClient.sendMessage(any(SendMessageRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(mockResponse));

        StepVerifier.create(loanAutomaticValidationSQSSender.validateLoanApplicationDecision(multipleLoansRequest))
                .verifyComplete();

        ArgumentCaptor<SendMessageRequest> requestCaptor = ArgumentCaptor.forClass(SendMessageRequest.class);
        verify(sqsAsyncClient).sendMessage(requestCaptor.capture());

        String messageBody = requestCaptor.getValue().messageBody();
        assertTrue(messageBody.contains("15000"));
        assertTrue(messageBody.contains("25000"));
        assertTrue(messageBody.contains("35000"));
        assertTrue(messageBody.contains("10.5"));
        assertTrue(messageBody.contains("13.0"));
        assertTrue(messageBody.contains("16.5"));
    }

    @Test
    void shouldReturnCompletedMonoOnSuccessfulSend() {
        SendMessageResponse mockResponse = SendMessageResponse.builder()
                .messageId(MESSAGE_ID)
                .build();

        when(sqsAsyncClient.sendMessage(any(SendMessageRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(mockResponse));

        StepVerifier.create(loanAutomaticValidationSQSSender.validateLoanApplicationDecision(sampleValidationRequest))
                .verifyComplete();

        verify(sqsAsyncClient, times(1)).sendMessage(any(SendMessageRequest.class));
    }

    @Test
    void shouldLogErrorOnSQSFailure() {
        RuntimeException sqsException = new RuntimeException("AWS SQS connection timeout");
        when(sqsAsyncClient.sendMessage(any(SendMessageRequest.class)))
                .thenReturn(CompletableFuture.failedFuture(sqsException));

        StepVerifier.create(loanAutomaticValidationSQSSender.validateLoanApplicationDecision(sampleValidationRequest))
                .expectErrorMatches(throwable -> 
                    throwable instanceof RuntimeException &&
                    throwable.getMessage().contains("Error sending automatic loan validation to SQS") &&
                    throwable.getCause() == sqsException)
                .verify();

        verify(sqsAsyncClient, times(1)).sendMessage(any(SendMessageRequest.class));
    }

    @Test
    void shouldHandleSpecialCharactersInUserData() {
        AutomaticValidationRequest specialCharsRequest = sampleValidationRequest.toBuilder()
                .userEmail("maría.josé@domínio.com")
                .userIdNumber("123.456.789-0")
                .build();

        SendMessageResponse mockResponse = SendMessageResponse.builder()
                .messageId(MESSAGE_ID)
                .build();

        when(sqsAsyncClient.sendMessage(any(SendMessageRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(mockResponse));

        StepVerifier.create(loanAutomaticValidationSQSSender.validateLoanApplicationDecision(specialCharsRequest))
                .verifyComplete();

        ArgumentCaptor<SendMessageRequest> requestCaptor = ArgumentCaptor.forClass(SendMessageRequest.class);
        verify(sqsAsyncClient).sendMessage(requestCaptor.capture());

        String messageBody = requestCaptor.getValue().messageBody();
        assertTrue(messageBody.contains("maría.josé@domínio.com") || messageBody.contains("mar\\u00eda"));
        assertTrue(messageBody.contains("123.456.789-0"));
    }
}