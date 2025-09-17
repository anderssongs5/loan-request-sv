package co.com.powerup.ags.loan.request.sqs.sender.notification.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.metrics.MetricPublisher;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class NotificationLoanAutomaticValidationSQSSenderConfigTest {

    @Mock
    private MetricPublisher metricPublisher;

    private NotificationSQSSenderConfig notificationSqsSenderConfig;

    @BeforeEach
    void setUp() {
        notificationSqsSenderConfig = new NotificationSQSSenderConfig();
    }

    @Test
    void shouldCreateSqsAsyncClientWithValidProperties() {
        NotificationSQSSenderProperties properties = new NotificationSQSSenderProperties(
                "us-east-1",
                "https://sqs.us-east-1.amazonaws.com/123456789012/test-queue",
                null
        );

        SqsAsyncClient client = notificationSqsSenderConfig.configSqs(properties, metricPublisher);

        assertNotNull(client);
    }

    @Test
    void shouldCreateSqsAsyncClientWithCustomEndpoint() {
        NotificationSQSSenderProperties properties = new NotificationSQSSenderProperties(
                "us-west-2",
                "https://sqs.us-west-2.amazonaws.com/123456789012/custom-queue",
                "http://localhost:4566"
        );

        SqsAsyncClient client = notificationSqsSenderConfig.configSqs(properties, metricPublisher);

        assertNotNull(client);
    }

    @Test
    void shouldCreateSqsAsyncClientWithNullEndpoint() {
        NotificationSQSSenderProperties properties = new NotificationSQSSenderProperties(
                "eu-west-1",
                "https://sqs.eu-west-1.amazonaws.com/123456789012/notification-queue",
                null
        );

        SqsAsyncClient client = notificationSqsSenderConfig.configSqs(properties, metricPublisher);

        assertNotNull(client);
    }

    @Test
    void shouldHandleLocalStackEndpoint() {
        NotificationSQSSenderProperties properties = new NotificationSQSSenderProperties(
                "us-east-1",
                "http://localhost:4566/000000000000/test-queue",
                "http://localhost:4566"
        );

        SqsAsyncClient client = notificationSqsSenderConfig.configSqs(properties, metricPublisher);

        assertNotNull(client);
    }

    @Test
    void shouldCreateSqsClientWithDifferentRegions() {
        String[] regions = {"us-east-1", "us-west-2", "eu-west-1", "ap-southeast-1", "ca-central-1"};
        
        for (String region : regions) {
            NotificationSQSSenderProperties properties = new NotificationSQSSenderProperties(
                    region,
                    "https://sqs." + region + ".amazonaws.com/123456789012/test-queue",
                    null
            );

            SqsAsyncClient client = notificationSqsSenderConfig.configSqs(properties, metricPublisher);

            assertNotNull(client, "Client should be created for region: " + region);
        }
    }

    @Test
    void shouldCreateClientWithMetricPublisher() {
        NotificationSQSSenderProperties properties = new NotificationSQSSenderProperties(
                "us-east-1",
                "https://sqs.us-east-1.amazonaws.com/123456789012/metrics-queue",
                null
        );

        SqsAsyncClient client = notificationSqsSenderConfig.configSqs(properties, metricPublisher);

        assertNotNull(client);
        assertNotNull(metricPublisher);
    }

    @Test
    void shouldHandleHttpsEndpoint() {
        NotificationSQSSenderProperties properties = new NotificationSQSSenderProperties(
                "us-east-1",
                "https://sqs.us-east-1.amazonaws.com/123456789012/secure-queue",
                "https://custom-sqs-endpoint.example.com"
        );

        SqsAsyncClient client = notificationSqsSenderConfig.configSqs(properties, metricPublisher);

        assertNotNull(client);
    }

    @Test
    void shouldCreateClientForDevelopmentEnvironment() {
        NotificationSQSSenderProperties properties = new NotificationSQSSenderProperties(
                "us-east-1",
                "http://localhost:4566/000000000000/dev-queue",
                "http://localhost:4566"
        );

        SqsAsyncClient client = notificationSqsSenderConfig.configSqs(properties, metricPublisher);

        assertNotNull(client);
    }

    @Test
    void shouldCreateClientForProductionEnvironment() {
        NotificationSQSSenderProperties properties = new NotificationSQSSenderProperties(
                "us-east-1",
                "https://sqs.us-east-1.amazonaws.com/123456789012/prod-loan-notifications",
                null
        );

        SqsAsyncClient client = notificationSqsSenderConfig.configSqs(properties, metricPublisher);

        assertNotNull(client);
    }

    @Test
    void shouldHandleLongQueueUrls() {
        String longQueueName = "very-long-queue-name-for-loan-application-status-update-notifications-with-detailed-context";
        NotificationSQSSenderProperties properties = new NotificationSQSSenderProperties(
                "us-east-1",
                "https://sqs.us-east-1.amazonaws.com/123456789012/" + longQueueName,
                null
        );

        SqsAsyncClient client = notificationSqsSenderConfig.configSqs(properties, metricPublisher);

        assertNotNull(client);
    }

    @Test
    void shouldCreateClientWithCustomPort() {
        NotificationSQSSenderProperties properties = new NotificationSQSSenderProperties(
                "us-east-1",
                "http://localhost:9324/000000000000/elasticmq-queue",
                "http://localhost:9324"
        );

        SqsAsyncClient client = notificationSqsSenderConfig.configSqs(properties, metricPublisher);

        assertNotNull(client);
    }
}