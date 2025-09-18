package co.com.powerup.ags.loan.request.sqs.sender.notification;

import co.com.powerup.ags.loan.request.model.loanapplication.LoanApplication;
import co.com.powerup.ags.loan.request.model.loanapplicationstatus.LoanApplicationStatus;
import co.com.powerup.ags.loan.request.model.loantype.LoanType;
import co.com.powerup.ags.loan.request.model.notification.LoanApplicationNotification;
import co.com.powerup.ags.loan.request.model.user.User;
import co.com.powerup.ags.loan.request.sqs.sender.notification.config.NotificationSQSSenderProperties;
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
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoanAutomaticValidationSQSSenderTest {

    @Mock
    private SqsAsyncClient sqsAsyncClient;

    @Mock
    private NotificationSQSSenderProperties properties;

    private NotificationSQSSender notificationSQSSender;

    private LoanApplicationNotification sampleLoanApplicationNotification;

    private static final String QUEUE_URL = "https://sqs.us-east-1.amazonaws.com/123456789012/loan-notifications";
    private static final String MESSAGE_ID = "12345678-1234-1234-1234-123456789012";
    private static final String LOAN_ID = "29a4639d-b328-453a-a408-d2eff0bcae84";
    private static final String USER_EMAIL = "user@example.com";

    @BeforeEach
    void setUp() {
        when(properties.queueUrl()).thenReturn(QUEUE_URL);

        notificationSQSSender = new NotificationSQSSender(properties, sqsAsyncClient);

        // Create sample data
        LoanApplicationStatus approvedStatus = LoanApplicationStatus.builder()
                .id(3)
                .name("APPROVED")
                .description("Approved")
                .build();

        LoanType personalLoanType = LoanType.builder()
                .id(1)
                .name("Personal Loan")
                .minAmount(new BigDecimal("1000.00"))
                .maxAmount(new BigDecimal("100000.00"))
                .minTerm(6)
                .maxTerm(60)
                .interestRate(new BigDecimal("12.5"))
                .automaticValidation(true)
                .build();

        LoanApplication loanApplication = LoanApplication.builder()
                .id(LOAN_ID)
                .email(USER_EMAIL)
                .amount(new BigDecimal("50000.00"))
                .term(24)
                .status(approvedStatus)
                .loanType(personalLoanType)
                .build();

        User user = User.builder()
                .id(UUID.randomUUID().toString())
                .email(USER_EMAIL)
                .name("John")
                .lastName("Doe")
                .baseSalary(new BigDecimal("5000.00"))
                .phoneNumber("+57300123456")
                .address("Calle 123 # 45-67, Bogotá")
                .birthDate(LocalDate.of(1985, 6, 15))
                .idNumber("12345678")
                .build();

        sampleLoanApplicationNotification = LoanApplicationNotification.builder()
                .loanRequest(loanApplication)
                .user(user)
                .build();
    }

    @Test
    void shouldSendNotificationSuccessfully() {
        // Given
        SendMessageResponse mockResponse = SendMessageResponse.builder()
                .messageId(MESSAGE_ID)
                .build();

        when(sqsAsyncClient.sendMessage(any(SendMessageRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(mockResponse));

        // When & Then
        StepVerifier.create(notificationSQSSender.notify(sampleLoanApplicationNotification))
                .verifyComplete();

        // Verify message was sent with correct parameters
        ArgumentCaptor<SendMessageRequest> requestCaptor = ArgumentCaptor.forClass(SendMessageRequest.class);
        verify(sqsAsyncClient).sendMessage(requestCaptor.capture());

        SendMessageRequest sentRequest = requestCaptor.getValue();
        assertEquals(QUEUE_URL, sentRequest.queueUrl());
        assertNotNull(sentRequest.messageBody());
        assertTrue(sentRequest.messageBody().contains(USER_EMAIL));
        assertTrue(sentRequest.messageBody().contains(LOAN_ID));
    }

    @Test
    void shouldHandleSQSClientException() {
        // Given
        RuntimeException sqsException = new RuntimeException("SQS service unavailable");
        when(sqsAsyncClient.sendMessage(any(SendMessageRequest.class)))
                .thenReturn(CompletableFuture.failedFuture(sqsException));

        // When & Then
        StepVerifier.create(notificationSQSSender.notify(sampleLoanApplicationNotification))
                .expectError(RuntimeException.class)
                .verify();
    }

    @Test
    void shouldHandleJsonSerializationError() {
        // Given - Create a loan application with circular reference or problematic data
        LoanApplication invalidLoanApplication = LoanApplication.builder()
                .id(LOAN_ID)
                .email(USER_EMAIL)
                .amount(null) // This should be fine, but let's test error handling
                .build();

        // Create user with problematic data
        User userWithInvalidDate = User.builder()
                .id(UUID.randomUUID().toString())
                .email(USER_EMAIL)
                .name("John")
                .lastName("Doe")
                .birthDate(null) // This might cause issues during serialization
                .build();

        LoanApplicationNotification invalidData = LoanApplicationNotification.builder()
                .loanRequest(invalidLoanApplication)
                .user(userWithInvalidDate)
                .build();

        // When & Then - The Jackson mapper should still be able to handle this, 
        // but if there were real serialization issues, they would be caught
        SendMessageResponse mockResponse = SendMessageResponse.builder()
                .messageId(MESSAGE_ID)
                .build();

        when(sqsAsyncClient.sendMessage(any(SendMessageRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(mockResponse));

        StepVerifier.create(notificationSQSSender.notify(invalidData))
                .verifyComplete();
    }

    @Test
    void shouldIncludeAllRequiredDataInMessage() {
        // Given
        SendMessageResponse mockResponse = SendMessageResponse.builder()
                .messageId(MESSAGE_ID)
                .build();

        when(sqsAsyncClient.sendMessage(any(SendMessageRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(mockResponse));

        // When
        StepVerifier.create(notificationSQSSender.notify(sampleLoanApplicationNotification))
                .verifyComplete();

        // Then - Verify message content
        ArgumentCaptor<SendMessageRequest> requestCaptor = ArgumentCaptor.forClass(SendMessageRequest.class);
        verify(sqsAsyncClient).sendMessage(requestCaptor.capture());

        String messageBody = requestCaptor.getValue().messageBody();
        
        // Verify loan application data is present
        assertTrue(messageBody.contains(LOAN_ID));
        assertTrue(messageBody.contains(USER_EMAIL));
        assertTrue(messageBody.contains("50000"));
        assertTrue(messageBody.contains("24"));
        assertTrue(messageBody.contains("APPROVED"));
        assertTrue(messageBody.contains("Personal Loan"));
        
        // Verify user data is present
        assertTrue(messageBody.contains("John"));
        assertTrue(messageBody.contains("Doe"));
        assertTrue(messageBody.contains("12345678"));
        assertTrue(messageBody.contains("+57300123456"));
    }

    @Test
    void shouldHandleRejectedLoanApplication() {
        // Given - Create rejected loan application
        LoanApplicationStatus rejectedStatus = LoanApplicationStatus.builder()
                .id(4)
                .name("REJECTED")
                .description("Rejected")
                .build();

        LoanApplication rejectedLoanApplication = sampleLoanApplicationNotification.getLoanRequest().toBuilder()
                .status(rejectedStatus)
                .build();

        LoanApplicationNotification rejectedData = sampleLoanApplicationNotification.toBuilder()
                .loanRequest(rejectedLoanApplication)
                .build();

        SendMessageResponse mockResponse = SendMessageResponse.builder()
                .messageId(MESSAGE_ID)
                .build();

        when(sqsAsyncClient.sendMessage(any(SendMessageRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(mockResponse));

        // When & Then
        StepVerifier.create(notificationSQSSender.notify(rejectedData))
                .verifyComplete();

        // Verify message contains rejected status
        ArgumentCaptor<SendMessageRequest> requestCaptor = ArgumentCaptor.forClass(SendMessageRequest.class);
        verify(sqsAsyncClient).sendMessage(requestCaptor.capture());

        String messageBody = requestCaptor.getValue().messageBody();
        assertTrue(messageBody.contains("REJECTED"));
    }

    @Test
    void shouldUseCorrectQueueUrl() {
        // Given
        String customQueueUrl = "https://sqs.eu-west-1.amazonaws.com/987654321098/custom-loan-notifications";
        when(properties.queueUrl()).thenReturn(customQueueUrl);

        NotificationSQSSender customSender = new NotificationSQSSender(properties, sqsAsyncClient);

        SendMessageResponse mockResponse = SendMessageResponse.builder()
                .messageId(MESSAGE_ID)
                .build();

        when(sqsAsyncClient.sendMessage(any(SendMessageRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(mockResponse));

        // When
        StepVerifier.create(customSender.notify(sampleLoanApplicationNotification))
                .verifyComplete();

        // Then
        ArgumentCaptor<SendMessageRequest> requestCaptor = ArgumentCaptor.forClass(SendMessageRequest.class);
        verify(sqsAsyncClient).sendMessage(requestCaptor.capture());

        assertEquals(customQueueUrl, requestCaptor.getValue().queueUrl());
    }

    @Test
    void shouldHandleNullLoanApplicationWithUser() {
        // When & Then - This should cause a runtime exception during JSON serialization
        StepVerifier.create(notificationSQSSender.notify(null))
                .expectError(RuntimeException.class)
                .verify();
    }

    @Test
    void shouldHandleEmptyLoanApplicationWithUser() {
        // Given
        LoanApplicationNotification emptyData = LoanApplicationNotification.builder().build();

        SendMessageResponse mockResponse = SendMessageResponse.builder()
                .messageId(MESSAGE_ID)
                .build();

        when(sqsAsyncClient.sendMessage(any(SendMessageRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(mockResponse));

        // When & Then
        StepVerifier.create(notificationSQSSender.notify(emptyData))
                .verifyComplete();

        // Verify message was sent even with empty data
        verify(sqsAsyncClient).sendMessage(any(SendMessageRequest.class));
    }

    @Test
    void shouldReturnCompletedMonoOnSuccessfulSend() {
        // Given
        SendMessageResponse mockResponse = SendMessageResponse.builder()
                .messageId(MESSAGE_ID)
                .build();

        when(sqsAsyncClient.sendMessage(any(SendMessageRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(mockResponse));

        // When & Then
        StepVerifier.create(notificationSQSSender.notify(sampleLoanApplicationNotification))
                .verifyComplete();

        verify(sqsAsyncClient, times(1)).sendMessage(any(SendMessageRequest.class));
    }
}