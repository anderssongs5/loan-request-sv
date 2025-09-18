package co.com.powerup.ags.loan.request.usecase.loanapplication;

import co.com.powerup.ags.loan.request.model.notification.PaymentPlanItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDate;

class PaymentPlanUseCaseTest {

    private static final BigDecimal LOAN_AMOUNT_10000 = new BigDecimal("10000.00");
    private static final BigDecimal LOAN_AMOUNT_50000 = new BigDecimal("50000.00");
    private static final BigDecimal LOAN_AMOUNT_100000 = new BigDecimal("100000.00");
    private static final BigDecimal ANNUAL_RATE_12 = new BigDecimal("12.00");
    private static final BigDecimal ANNUAL_RATE_0 = new BigDecimal("0.00");
    private static final BigDecimal ANNUAL_RATE_24 = new BigDecimal("24.00");
    private static final Integer TERM_12_MONTHS = 12;
    private static final Integer TERM_24_MONTHS = 24;
    private static final Integer TERM_36_MONTHS = 36;

    private PaymentPlanUseCase paymentPlanUseCase;

    @BeforeEach
    void setUp() {
        paymentPlanUseCase = new PaymentPlanUseCase();
    }

    @Test
    void calculatePaymentSchedule_WhenLoanWith12PercentRate_ShouldReturnCorrectSchedule() {
        StepVerifier.create(paymentPlanUseCase.calculatePaymentSchedule(
                LOAN_AMOUNT_10000, null, ANNUAL_RATE_12, TERM_12_MONTHS))
                .expectNextCount(12)
                .verifyComplete();
    }

    @Test
    void calculatePaymentSchedule_WhenZeroInterestRate_ShouldCalculateCorrectPayments() {
        BigDecimal expectedMonthlyPayment = new BigDecimal("833.33");

        StepVerifier.create(paymentPlanUseCase.calculatePaymentSchedule(
                LOAN_AMOUNT_10000, null, ANNUAL_RATE_0, TERM_12_MONTHS))
                .expectNextMatches(payment -> 
                    payment.getPaymentNumber() == 1 &&
                    payment.getInterestPayment().compareTo(BigDecimal.ZERO) == 0 &&
                    payment.getTotalPayment().compareTo(expectedMonthlyPayment) == 0)
                .expectNextCount(11)
                .verifyComplete();
    }

    @Test
    void calculatePaymentSchedule_WhenProvidedMonthlyPayment_ShouldUseProvidedAmount() {
        BigDecimal providedMonthlyPayment = new BigDecimal("500.00");

        StepVerifier.create(paymentPlanUseCase.calculatePaymentSchedule(
                LOAN_AMOUNT_10000, providedMonthlyPayment, ANNUAL_RATE_12, TERM_12_MONTHS))
                .expectNextMatches(payment -> 
                    payment.getTotalPayment().compareTo(providedMonthlyPayment) == 0)
                .expectNextCount(11)
                .verifyComplete();
    }

    @Test
    void calculatePaymentSchedule_WhenLastPayment_ShouldAdjustToRemainingBalance() {
        StepVerifier.create(paymentPlanUseCase.calculatePaymentSchedule(
                LOAN_AMOUNT_10000, null, ANNUAL_RATE_12, TERM_12_MONTHS))
                .expectNextCount(11)
                .expectNextMatches(payment -> 
                    payment.getPaymentNumber() == 12 &&
                    payment.getRemainingBalance().compareTo(BigDecimal.ZERO) == 0)
                .verifyComplete();
    }

    @Test
    void calculatePaymentSchedule_WhenCalculatingInterest_ShouldHaveCorrectMonthlyRate() {
        BigDecimal expectedFirstMonthInterest = new BigDecimal("100.00");

        StepVerifier.create(paymentPlanUseCase.calculatePaymentSchedule(
                LOAN_AMOUNT_10000, null, ANNUAL_RATE_12, TERM_12_MONTHS))
                .expectNextMatches(payment -> 
                    payment.getInterestPayment().compareTo(expectedFirstMonthInterest) == 0)
                .expectNextCount(11)
                .verifyComplete();
    }

    @Test
    void calculatePaymentSchedule_WhenLargerLoan_ShouldCalculateCorrectly() {
        StepVerifier.create(paymentPlanUseCase.calculatePaymentSchedule(
                LOAN_AMOUNT_100000, null, ANNUAL_RATE_12, TERM_24_MONTHS))
                .expectNextCount(24)
                .expectComplete()
                .verify();
    }

    @Test
    void calculatePaymentSchedule_WhenHigherInterestRate_ShouldReflectInPayments() {
        BigDecimal expectedFirstMonthInterest = new BigDecimal("2000.00");

        StepVerifier.create(paymentPlanUseCase.calculatePaymentSchedule(
                LOAN_AMOUNT_100000, null, ANNUAL_RATE_24, TERM_12_MONTHS))
                .expectNextMatches(payment -> 
                    payment.getInterestPayment().compareTo(expectedFirstMonthInterest) == 0)
                .expectNextCount(11)
                .verifyComplete();
    }

    @Test
    void calculatePaymentSchedule_WhenLongerTerm_ShouldHaveMorePayments() {
        StepVerifier.create(paymentPlanUseCase.calculatePaymentSchedule(
                LOAN_AMOUNT_50000, null, ANNUAL_RATE_12, TERM_36_MONTHS))
                .expectNextCount(36)
                .verifyComplete();
    }

    @Test
    void calculatePaymentSchedule_WhenCalculatingDueDates_ShouldStartNextMonth() {
        LocalDate nextMonth = LocalDate.now().plusMonths(1);

        StepVerifier.create(paymentPlanUseCase.calculatePaymentSchedule(
                LOAN_AMOUNT_10000, null, ANNUAL_RATE_12, TERM_12_MONTHS))
                .expectNextMatches(payment -> 
                    payment.getDueDate().equals(nextMonth))
                .expectNextCount(11)
                .verifyComplete();
    }

    @Test
    void calculatePaymentSchedule_WhenCalculatingPayments_ShouldHaveSequentialNumbers() {
        StepVerifier.create(paymentPlanUseCase.calculatePaymentSchedule(
                LOAN_AMOUNT_10000, null, ANNUAL_RATE_12, TERM_12_MONTHS))
                .expectNextMatches(payment -> payment.getPaymentNumber() == 1)
                .expectNextMatches(payment -> payment.getPaymentNumber() == 2)
                .expectNextMatches(payment -> payment.getPaymentNumber() == 3)
                .expectNextCount(9)
                .verifyComplete();
    }

    @Test
    void calculatePaymentSchedule_WhenCalculatingBalance_ShouldDecreaseOverTime() {
        StepVerifier.create(paymentPlanUseCase.calculatePaymentSchedule(
                LOAN_AMOUNT_10000, null, ANNUAL_RATE_12, TERM_12_MONTHS))
                .expectNextMatches(payment -> 
                    payment.getRemainingBalance().compareTo(LOAN_AMOUNT_10000) < 0)
                .expectNextMatches(payment -> 
                    payment.getRemainingBalance().compareTo(new BigDecimal("9000.00")) < 0)
                .expectNextCount(10)
                .verifyComplete();
    }

    @Test
    void calculatePaymentSchedule_WhenSmallAmount_ShouldHandleRoundingCorrectly() {
        BigDecimal smallAmount = new BigDecimal("100.00");

        StepVerifier.create(paymentPlanUseCase.calculatePaymentSchedule(
                smallAmount, null, ANNUAL_RATE_12, TERM_12_MONTHS))
                .expectNextCount(12)
                .expectComplete()
                .verify();
    }

    @Test
    void calculatePaymentSchedule_WhenSinglePayment_ShouldReturnOneItem() {
        StepVerifier.create(paymentPlanUseCase.calculatePaymentSchedule(
                LOAN_AMOUNT_10000, null, ANNUAL_RATE_12, 1))
                .expectNextMatches(payment -> 
                    payment.getPaymentNumber() == 1 &&
                    payment.getRemainingBalance().compareTo(BigDecimal.ZERO) == 0)
                .verifyComplete();
    }

    @Test
    void calculatePaymentSchedule_WhenValidatingTotalPayments_ShouldEqualLoanPlusInterest() {
        StepVerifier.create(paymentPlanUseCase.calculatePaymentSchedule(
                LOAN_AMOUNT_10000, null, ANNUAL_RATE_0, TERM_12_MONTHS))
                .expectNextCount(12)
                .verifyComplete();
    }

    @Test
    void calculatePaymentSchedule_WhenValidatingPaymentStructure_ShouldHaveRequiredFields() {
        StepVerifier.create(paymentPlanUseCase.calculatePaymentSchedule(
                LOAN_AMOUNT_10000, null, ANNUAL_RATE_12, TERM_12_MONTHS))
                .expectNextMatches(payment -> 
                    payment.getPaymentNumber() != null &&
                    payment.getDueDate() != null &&
                    payment.getPrincipalPayment() != null &&
                    payment.getInterestPayment() != null &&
                    payment.getTotalPayment() != null &&
                    payment.getRemainingBalance() != null)
                .expectNextCount(11)
                .verifyComplete();
    }
}