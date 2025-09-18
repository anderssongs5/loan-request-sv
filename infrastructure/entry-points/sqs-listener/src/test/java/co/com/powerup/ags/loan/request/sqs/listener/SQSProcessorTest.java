package co.com.powerup.ags.loan.request.sqs.listener;

import co.com.powerup.ags.loan.request.usecase.loanapplication.UpdateLoanApplicationStatusUseCase;
import co.com.powerup.ags.loan.request.usecase.loanapplication.dto.UpdateLoanAutomaticValidationCommand;
import co.com.powerup.ags.loan.request.usecase.common.exception.LoanApplicationNotFoundException;
import co.com.powerup.ags.loan.request.usecase.common.exception.UpdateLoanApplicationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.services.sqs.model.Message;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SQSProcessorTest {

    private static final String MESSAGE_ID = "12345-67890-abcdef";
    private static final String LOAN_APPLICATION_ID = "loan-123";
    private static final String APPROVED_DECISION = "APPROVED";
    private static final String REJECTED_DECISION = "REJECTED";
    private static final String REJECTION_REASON = "Insufficient income";
    private static final String MONTHLY_PAYMENT = "500.00";

    @Mock
    private UpdateLoanApplicationStatusUseCase updateLoanApplicationStatusUseCase;

    private SQSProcessor sqsProcessor;

    @BeforeEach
    void setUp() {
        sqsProcessor = new SQSProcessor(updateLoanApplicationStatusUseCase);
    }

    @Test
    void shouldProcessValidApprovedMessageSuccessfully() {
        String messageBody = createValidApprovedMessageBody();
        Message message = createMessage(MESSAGE_ID, messageBody);

        when(updateLoanApplicationStatusUseCase.updateLoanApplicationStatusFromAutoValidation(any()))
                .thenReturn(Mono.empty());

        StepVerifier.create(sqsProcessor.apply(message))
                .verifyComplete();

        ArgumentCaptor<UpdateLoanAutomaticValidationCommand> commandCaptor = 
                ArgumentCaptor.forClass(UpdateLoanAutomaticValidationCommand.class);
        verify(updateLoanApplicationStatusUseCase).updateLoanApplicationStatusFromAutoValidation(commandCaptor.capture());

        UpdateLoanAutomaticValidationCommand capturedCommand = commandCaptor.getValue();
        assertNotNull(capturedCommand);
        assertEquals(200, capturedCommand.statusCode());
        assertEquals(LOAN_APPLICATION_ID, capturedCommand.validationResponse().loanApplicationId());
        assertEquals(APPROVED_DECISION, capturedCommand.validationResponse().analysis().decision());
    }

    @Test
    void shouldProcessValidRejectedMessageSuccessfully() {
        String messageBody = createValidRejectedMessageBody();
        Message message = createMessage(MESSAGE_ID, messageBody);

        when(updateLoanApplicationStatusUseCase.updateLoanApplicationStatusFromAutoValidation(any()))
                .thenReturn(Mono.empty());

        StepVerifier.create(sqsProcessor.apply(message))
                .verifyComplete();

        ArgumentCaptor<UpdateLoanAutomaticValidationCommand> commandCaptor = 
                ArgumentCaptor.forClass(UpdateLoanAutomaticValidationCommand.class);
        verify(updateLoanApplicationStatusUseCase).updateLoanApplicationStatusFromAutoValidation(commandCaptor.capture());

        UpdateLoanAutomaticValidationCommand capturedCommand = commandCaptor.getValue();
        assertNotNull(capturedCommand);
        assertEquals(200, capturedCommand.statusCode());
        assertEquals(LOAN_APPLICATION_ID, capturedCommand.validationResponse().loanApplicationId());
        assertEquals(REJECTED_DECISION, capturedCommand.validationResponse().analysis().decision());
        assertEquals(REJECTION_REASON, capturedCommand.validationResponse().rejectionReason());
    }

    @Test
    void shouldPropagateErrorWhenUseCaseReturnsError() {
        String messageBody = createValidApprovedMessageBody();
        Message message = createMessage(MESSAGE_ID, messageBody);

        when(updateLoanApplicationStatusUseCase.updateLoanApplicationStatusFromAutoValidation(any()))
                .thenReturn(Mono.error(new LoanApplicationNotFoundException("Loan not found")));

        StepVerifier.create(sqsProcessor.apply(message))
                .expectError(LoanApplicationNotFoundException.class)
                .verify();
    }

    @Test
    void shouldPropagateUpdateException() {
        String messageBody = createValidApprovedMessageBody();
        Message message = createMessage(MESSAGE_ID, messageBody);

        when(updateLoanApplicationStatusUseCase.updateLoanApplicationStatusFromAutoValidation(any()))
                .thenReturn(Mono.error(new UpdateLoanApplicationException("Update failed")));

        StepVerifier.create(sqsProcessor.apply(message))
                .expectError(UpdateLoanApplicationException.class)
                .verify();
    }

    @Test
    void shouldIncludeMonthlyPaymentWhenValidMessageWithPayment() {
        String messageBody = createValidMessageWithMonthlyPayment();
        Message message = createMessage(MESSAGE_ID, messageBody);

        when(updateLoanApplicationStatusUseCase.updateLoanApplicationStatusFromAutoValidation(any()))
                .thenReturn(Mono.empty());

        StepVerifier.create(sqsProcessor.apply(message))
                .verifyComplete();

        ArgumentCaptor<UpdateLoanAutomaticValidationCommand> commandCaptor = 
                ArgumentCaptor.forClass(UpdateLoanAutomaticValidationCommand.class);
        verify(updateLoanApplicationStatusUseCase).updateLoanApplicationStatusFromAutoValidation(commandCaptor.capture());

        UpdateLoanAutomaticValidationCommand capturedCommand = commandCaptor.getValue();
        assertNotNull(capturedCommand.validationResponse().analysis().newLoanMonthlyPayment());
        assertEquals("500.0", capturedCommand.validationResponse().analysis().newLoanMonthlyPayment().toString());
    }

    @Test
    void shouldProcessMultipleMessagesIndependently() {
        String messageBody1 = createValidApprovedMessageBody();
        String messageBody2 = createValidRejectedMessageBody();
        Message message1 = createMessage("msg-1", messageBody1);
        Message message2 = createMessage("msg-2", messageBody2);

        when(updateLoanApplicationStatusUseCase.updateLoanApplicationStatusFromAutoValidation(any()))
                .thenReturn(Mono.empty());

        StepVerifier.create(sqsProcessor.apply(message1))
                .verifyComplete();

        StepVerifier.create(sqsProcessor.apply(message2))
                .verifyComplete();
    }

    private Message createMessage(String messageId, String body) {
        return Message.builder()
                .messageId(messageId)
                .body(body)
                .build();
    }

    private String createValidApprovedMessageBody() {
        return """
            {
                "statusCode": 200,
                "body": {
                    "loanApplicationId": "%s",
                    "analysis": {
                        "decision": "%s"
                    }
                }
            }
            """.formatted(LOAN_APPLICATION_ID, APPROVED_DECISION);
    }

    private String createValidRejectedMessageBody() {
        return """
            {
                "statusCode": 200,
                "body": {
                    "loanApplicationId": "%s",
                    "analysis": {
                        "decision": "%s"
                    },
                    "rejectionReason": "%s"
                }
            }
            """.formatted(LOAN_APPLICATION_ID, REJECTED_DECISION, REJECTION_REASON);
    }

    private String createValidMessageWithMonthlyPayment() {
        return """
            {
                "statusCode": 200,
                "body": {
                    "loanApplicationId": "%s",
                    "analysis": {
                        "decision": "%s",
                        "newLoanMonthlyPayment": %s
                    }
                }
            }
            """.formatted(LOAN_APPLICATION_ID, APPROVED_DECISION, MONTHLY_PAYMENT);
    }
}