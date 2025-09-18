package co.com.powerup.ags.loan.request.config;

import co.com.powerup.ags.loan.request.model.loanapplication.gateways.AutomaticValidationGateway;
import co.com.powerup.ags.loan.request.model.loanapplication.gateways.LoanApplicationRepository;
import co.com.powerup.ags.loan.request.model.loanapplicationstatus.gateways.LoanApplicationStatusRepository;
import co.com.powerup.ags.loan.request.model.loantype.gateways.LoanTypeRepository;
import co.com.powerup.ags.loan.request.model.notification.gateway.NotificationGateway;
import co.com.powerup.ags.loan.request.model.user.gateways.UserGateway;
import co.com.powerup.ags.loan.request.usecase.loanapplication.UpdateLoanApplicationStatusUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

public class UseCasesConfigTest {

    @Test
    void testUseCaseBeansExist() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(TestConfig.class)) {
            String[] beanNames = context.getBeanDefinitionNames();

            boolean useCaseBeanFound = false;
            for (String beanName : beanNames) {
                if (beanName.endsWith("UseCase")) {
                    useCaseBeanFound = true;
                    break;
                }
            }

            assertTrue(useCaseBeanFound, "No beans ending with 'Use Case' were found");
        }
    }

    @Configuration
    @Import(UseCasesConfig.class)
    static class TestConfig {
        
        @Bean
        public LoanApplicationRepository loanApplicationRepository() {
            return mock(LoanApplicationRepository.class);
        }
        
        @Bean
        public LoanTypeRepository loanTypeRepository() {
            return mock(LoanTypeRepository.class);
        }
        
        @Bean
        public LoanApplicationStatusRepository loanApplicationStatusRepository() {
            return mock(LoanApplicationStatusRepository.class);
        }
        
        @Bean
        public UserGateway userGateway() {
            return mock(UserGateway.class);
        }
        
        @Bean
        public NotificationGateway notificationGateway() {
            return mock(NotificationGateway.class);
        }
        
        @Bean
        public AutomaticValidationGateway automaticValidationGateway() {
            return mock(AutomaticValidationGateway.class);
        }

        @Bean
        public MyUseCase myUseCase() {
            return new MyUseCase();
        }
    }

    static class MyUseCase {
        public String execute() {
            return "MyUseCase Test";
        }
    }
}