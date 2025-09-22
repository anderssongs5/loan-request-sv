package co.com.powerup.ags.loan.request.sqs.sender.approvedloanreport.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "adapter.sqs.approved-loan-report")
public record ApprovedLoanReportSQSSenderProperties(
     String region,
     String queueUrl,
     String endpoint){
}
