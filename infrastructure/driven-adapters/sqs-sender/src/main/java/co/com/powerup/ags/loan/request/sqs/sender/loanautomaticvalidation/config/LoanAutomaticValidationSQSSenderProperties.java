package co.com.powerup.ags.loan.request.sqs.sender.loanautomaticvalidation.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "adapter.sqs.automaticvalidation")
public record LoanAutomaticValidationSQSSenderProperties(
     String region,
     String queueUrl,
     String endpoint){
}
