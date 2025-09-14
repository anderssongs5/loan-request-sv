package co.com.powerup.ags.loan.request.sqs.sender.notification.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "adapter.sqs.notification")
public record SQSSenderProperties(
     String region,
     String queueUrl,
     String endpoint){
}
