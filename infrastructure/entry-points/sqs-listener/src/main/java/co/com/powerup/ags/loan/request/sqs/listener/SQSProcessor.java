package co.com.powerup.ags.loan.request.sqs.listener;

import co.com.powerup.ags.loan.request.usecase.loanapplication.UpdateLoanApplicationStatusUseCase;
import co.com.powerup.ags.loan.request.usecase.loanapplication.dto.UpdateLoanAutomaticValidationCommand;
import co.com.powerup.ags.loan.request.usecase.loanapplication.dto.ValidationResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.model.Message;

import java.util.function.Function;

@Service
@Log4j2
@RequiredArgsConstructor
public class SQSProcessor implements Function<Message, Mono<Void>> {
    
    private final UpdateLoanApplicationStatusUseCase updateLoanApplicationStatusUseCase;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Mono<Void> apply(Message message) {
        log.info("Message received with id: {} and body {}:", message.messageId(), message.body());
        
        UpdateLoanAutomaticValidationCommand command = getUpdateLoanAutomaticValidationCommand(message);
        
        return updateLoanApplicationStatusUseCase.updateLoanApplicationStatusFromAutoValidation(command);
    }
    
    private static UpdateLoanAutomaticValidationCommand getUpdateLoanAutomaticValidationCommand(Message message) {
        try {
            JsonNode response = objectMapper.readTree(message.body());
            ValidationResponse validationResponse = objectMapper.readValue(response.get("body").toString(), ValidationResponse.class);
            return new UpdateLoanAutomaticValidationCommand(response.get("statusCode").asInt(), validationResponse);
        } catch (JsonProcessingException e) {
            log.info("Error mapping message response with id {} and body {}", message.messageId(), message.body());
            throw new RuntimeException(e);
        }
    }
}
