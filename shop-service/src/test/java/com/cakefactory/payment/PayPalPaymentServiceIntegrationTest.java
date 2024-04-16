package com.cakefactory.payment;

import com.paypal.core.PayPalEnvironment;
import com.paypal.core.PayPalHttpClient;
import com.paypal.orders.OrdersGetRequest;
import com.paypal.orders.PurchaseUnit;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureTestDatabase
class PayPalPaymentServiceIntegrationTest {

    private Order payment;
    private PayPalHttpClient testPayPalClient;

    @Autowired
    PayPalEnvironment payPalEnvironment;

    @Autowired
    PaymentService paymentService;

    @BeforeEach
    void setUp() {
        testPayPalClient = new PayPalHttpClient(payPalEnvironment);

        payment = Order.builder()
                .items(List.of(
                        OrderItem.builder().title("Item 1").value(BigDecimal.valueOf(1.10)).build(),
                        OrderItem.builder().title("Item 2").value(BigDecimal.valueOf(2.20)).build()))
                .build();
    }

    @Test
    void returnsApprovalUri() {
        URI approveUri = paymentService.create(this.payment, URI.create("http://localhost:8080")).getApproveUri();
        assertThat(approveUri).hasHost("www.sandbox.paypal.com");
        assertThat(approveUri).hasPath("/checkoutnow");
    }

    @Test
    void setsAmount() {
        com.paypal.orders.Order testOrder = createTestOrder();

        PurchaseUnit unit = testOrder.purchaseUnits().get(0);
        assertThat(unit.amountWithBreakdown().value()).isEqualTo("3.30");
        assertThat(unit.amountWithBreakdown().currencyCode()).isEqualTo(PaymentService.DEFAULT_CURRENCY);
    }

    @SneakyThrows
    private com.paypal.orders.Order createTestOrder() {
        PendingPayment pendingPayment = paymentService.create(this.payment, URI.create("https://example.com"));
        return getOrder(pendingPayment.getId());
    }

    private com.paypal.orders.Order getOrder(String orderId) throws java.io.IOException {
        OrdersGetRequest getRequest = new OrdersGetRequest(orderId);
        return testPayPalClient.execute(getRequest).result();
    }
}