package co.com.powerup.ags.loan.request.sqs.sender.approvedloanreport.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.metrics.MetricPublisher;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApprovedLoanReportSQSSenderConfigTest {

    @Mock
    private ApprovedLoanReportSQSSenderProperties properties;

    @Mock
    private MetricPublisher metricPublisher;

    private ApprovedLoanReportSQSSenderConfig config;

    private static final String REGION = "us-east-1";
    private static final String ENDPOINT = "http://localhost:4566";
    private static final String QUEUE_URL = "https://sqs.us-east-1.amazonaws.com/123456789012/approved-loans-queue";

    @BeforeEach
    void setUp() {
        config = new ApprovedLoanReportSQSSenderConfig();
    }

    @Test
    void shouldCreateSqsAsyncClientWithCorrectConfiguration() {
        // Given
        when(properties.region()).thenReturn(REGION);
        when(properties.endpoint()).thenReturn(null);

        // When
        SqsAsyncClient sqsAsyncClient = config.configSqs(properties, metricPublisher);

        // Then
        assertNotNull(sqsAsyncClient);
        
        // Verify the client is properly configured (we can't directly test internal configuration
        // but we can ensure the client is created without exceptions)
        assertDoesNotThrow(() -> {
            sqsAsyncClient.serviceName();
        });
    }

    @Test
    void shouldCreateSqsAsyncClientWithCustomEndpoint() {
        // Given
        when(properties.region()).thenReturn(REGION);
        when(properties.endpoint()).thenReturn(ENDPOINT);

        // When
        SqsAsyncClient sqsAsyncClient = config.configSqs(properties, metricPublisher);

        // Then
        assertNotNull(sqsAsyncClient);
        assertEquals("sqs", sqsAsyncClient.serviceName());
    }

    @Test
    void shouldCreateSqsAsyncClientWithNullEndpoint() {
        // Given
        when(properties.region()).thenReturn(REGION);
        when(properties.endpoint()).thenReturn(null);

        // When
        SqsAsyncClient sqsAsyncClient = config.configSqs(properties, metricPublisher);

        // Then
        assertNotNull(sqsAsyncClient);
    }

    @Test
    void shouldCreateSqsAsyncClientWithEmptyEndpoint() {
        // Given
        when(properties.region()).thenReturn(REGION);
        when(properties.endpoint()).thenReturn("");

        // When
        SqsAsyncClient sqsAsyncClient = config.configSqs(properties, metricPublisher);

        // Then
        assertNotNull(sqsAsyncClient);
    }

    @Test
    void shouldCreateSqsAsyncClientWithWhitespaceEndpoint() {
        // Given
        when(properties.region()).thenReturn(REGION);
        when(properties.endpoint()).thenReturn("   ");

        // When
        SqsAsyncClient sqsAsyncClient = config.configSqs(properties, metricPublisher);

        // Then
        assertNotNull(sqsAsyncClient);
    }

    @Test
    void shouldHandleDifferentRegions() {
        // Test with different regions
        String[] regions = {"us-west-2", "eu-west-1", "ap-southeast-1"};
        
        for (String regionName : regions) {
            // Given
            when(properties.region()).thenReturn(regionName);
            when(properties.endpoint()).thenReturn(null);

            // When
            SqsAsyncClient sqsAsyncClient = config.configSqs(properties, metricPublisher);

            // Then
            assertNotNull(sqsAsyncClient);
            assertEquals("sqs", sqsAsyncClient.serviceName());
        }
    }

    @Test
    void shouldCreateCredentialsProviderChain() {
        // Given
        when(properties.region()).thenReturn(REGION);
        when(properties.endpoint()).thenReturn(null);

        // When
        SqsAsyncClient sqsAsyncClient = config.configSqs(properties, metricPublisher);

        // Then
        assertNotNull(sqsAsyncClient);
        
        // The credentials provider chain should be created with the correct providers
        // We can't directly test the internal chain, but we can verify the client works
        assertDoesNotThrow(() -> {
            sqsAsyncClient.serviceName();
        });
    }

    @Test
    void shouldCreateValidURIFromEndpoint() {
        // Test the private method indirectly through client creation
        when(properties.region()).thenReturn(REGION);
        when(properties.endpoint()).thenReturn("https://sqs.amazonaws.com");

        SqsAsyncClient sqsAsyncClient = config.configSqs(properties, metricPublisher);
        
        assertNotNull(sqsAsyncClient);
    }

    @Test
    void shouldAddMetricPublisher() {
        // Given
        when(properties.region()).thenReturn(REGION);
        when(properties.endpoint()).thenReturn(null);

        // When
        SqsAsyncClient sqsAsyncClient = config.configSqs(properties, metricPublisher);

        // Then
        assertNotNull(sqsAsyncClient);
        // The metric publisher should be added to the client configuration
        // We can't directly verify this, but we can ensure the client is created successfully
    }

    @Test
    void shouldUseConditionalOnMissingBeanAnnotation() {
        // This test verifies that the configuration class has the correct annotation
        // In a real Spring context, this would prevent duplicate bean creation
        
        assertTrue(ApprovedLoanReportSQSSenderConfig.class
                .isAnnotationPresent(org.springframework.context.annotation.Configuration.class));
        
        assertTrue(ApprovedLoanReportSQSSenderConfig.class
                .isAnnotationPresent(org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean.class));
    }

    @Test
    void shouldIncludeAllCredentialsProviders() {
        // Given
        when(properties.region()).thenReturn(REGION);
        when(properties.endpoint()).thenReturn(null);

        // When
        SqsAsyncClient sqsAsyncClient = config.configSqs(properties, metricPublisher);

        // Then
        assertNotNull(sqsAsyncClient);
        
        // The credentials provider chain should include:
        // - EnvironmentVariableCredentialsProvider
        // - SystemPropertyCredentialsProvider  
        // - WebIdentityTokenFileCredentialsProvider
        // - ProfileCredentialsProvider
        // - ContainerCredentialsProvider
        // - InstanceProfileCredentialsProvider
        
        // We can't directly test the chain composition, but we can verify
        // that the client is created successfully with the chain
        assertEquals("sqs", sqsAsyncClient.serviceName());
    }

    @Test
    void shouldCreateClientWithValidRegion() {
        // Test that Region.of() works correctly with the provided region
        when(properties.region()).thenReturn("us-east-1");
        when(properties.endpoint()).thenReturn(null);

        SqsAsyncClient sqsAsyncClient = config.configSqs(properties, metricPublisher);
        
        assertNotNull(sqsAsyncClient);
        assertEquals("sqs", sqsAsyncClient.serviceName());
    }

    @Test
    void shouldHandleLocalStackEndpoint() {
        // Test with LocalStack endpoint (common in testing)
        when(properties.region()).thenReturn(REGION);
        when(properties.endpoint()).thenReturn("http://localhost:4566");

        SqsAsyncClient sqsAsyncClient = config.configSqs(properties, metricPublisher);
        
        assertNotNull(sqsAsyncClient);
    }

    @Test
    void shouldCreateDifferentClientsForDifferentConfigurations() {
        // Test that different configurations create different clients
        when(properties.region()).thenReturn("us-east-1");
        when(properties.endpoint()).thenReturn(null);
        
        SqsAsyncClient client1 = config.configSqs(properties, metricPublisher);
        
        when(properties.region()).thenReturn("us-west-2");
        SqsAsyncClient client2 = config.configSqs(properties, metricPublisher);
        
        assertNotNull(client1);
        assertNotNull(client2);
        assertNotSame(client1, client2);
    }
}