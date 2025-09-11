package co.com.powerup.ags.loan.request.consumer.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class RestConsumerConfigTest {

    @ParameterizedTest
    @CsvSource(value = {
            "http://localhost:8080, 5000",
            "https://api.example.com, 3000",
            "http://localhost:8080, 0",
            "http://localhost:8080, -1000",
            ", 5000",
            "'', 5000"
    }, nullValues = {"null"})
    void shouldCreateRestConsumerConfigWithDifferentUrlsAndTimeouts(String url, int timeout) {
        RestConsumerConfig config = new RestConsumerConfig(url, timeout);

        assertThat(config).isNotNull();
    }

    @Test
    void shouldCreateWebClientWithCorrectConfiguration() {
        String url = "http://localhost:8080";
        int timeout = 5000;
        RestConsumerConfig config = new RestConsumerConfig(url, timeout);
        WebClient.Builder builder = WebClient.builder();

        WebClient webClient = config.getWebClient(builder);

        assertThat(webClient).isNotNull();
    }

    @Test
    void shouldCreateWebClientWithMockedBuilder() {
        String url = "http://localhost:8080";
        int timeout = 5000;
        RestConsumerConfig config = new RestConsumerConfig(url, timeout);
        WebClient.Builder builder = mock(WebClient.Builder.class);
        WebClient mockWebClient = mock(WebClient.class);

        org.mockito.Mockito.when(builder.baseUrl(url)).thenReturn(builder);
        org.mockito.Mockito.when(builder.defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")).thenReturn(builder);
        org.mockito.Mockito.when(builder.clientConnector(org.mockito.ArgumentMatchers.any())).thenReturn(builder);
        org.mockito.Mockito.when(builder.build()).thenReturn(mockWebClient);

        WebClient result = config.getWebClient(builder);

        assertThat(result).isEqualTo(mockWebClient);
    }
}