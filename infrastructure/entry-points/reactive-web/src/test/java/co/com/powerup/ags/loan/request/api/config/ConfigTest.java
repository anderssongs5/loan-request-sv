package co.com.powerup.ags.loan.request.api.config;

import co.com.powerup.ags.loan.request.api.HandlerV1;
import co.com.powerup.ags.loan.request.api.RouterRest;
import co.com.powerup.ags.loan.request.model.common.PagedResponse;
import co.com.powerup.ags.loan.request.usecase.borrowingcapacity.BorrowingCapacityUseCase;
import co.com.powerup.ags.loan.request.usecase.loanapplication.LoanApplicationUseCase;
import co.com.powerup.ags.loan.request.usecase.loanapplication.UpdateLoanApplicationStatusUseCase;
import co.com.powerup.ags.loan.request.usecase.loanapplication.dto.GetLoanApplicationsByStatusesCommand;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;

@ContextConfiguration(classes = {RouterRest.class, HandlerV1.class})
@WebFluxTest(excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.reactive.ReactiveUserDetailsServiceAutoConfiguration.class
})
@Import({CorsConfig.class, SecurityHeadersConfig.class})
class ConfigTest {

    @Autowired
    private WebTestClient webTestClient;
    
    @MockitoBean
    private LoanApplicationUseCase loanApplicationUseCase;
    
    @MockitoBean
    private UpdateLoanApplicationStatusUseCase updateLoanApplicationStatusUseCase;

    @MockitoBean
    private BorrowingCapacityUseCase borrowingCapacityUseCase;

    @Test
    void corsConfigurationShouldAllowOrigins() {
        Mockito.when(loanApplicationUseCase.getLoanRequestsRequiringReview(any(GetLoanApplicationsByStatusesCommand.class)))
                .thenReturn(Mono.just(PagedResponse.of(List.of(), 0, 0, 0)));
        
        webTestClient.get()
                .uri("/api/v1/loan-requests")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("Content-Security-Policy",
                        "default-src 'self'; frame-ancestors 'self'; form-action 'self'")
                .expectHeader().valueEquals("Strict-Transport-Security", "max-age=31536000;")
                .expectHeader().valueEquals("X-Content-Type-Options", "nosniff")
                .expectHeader().valueEquals("Server", "")
                .expectHeader().valueEquals("Cache-Control", "no-store")
                .expectHeader().valueEquals("Pragma", "no-cache")
                .expectHeader().valueEquals("Referrer-Policy", "strict-origin-when-cross-origin");
    }

}