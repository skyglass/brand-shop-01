package com.cakefactory.payment;

import com.paypal.core.PayPalEnvironment;
import com.paypal.core.PayPalHttpClient;
import com.paypal.http.HttpResponse;
import com.paypal.orders.*;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
class PayPalPaymentService implements PaymentService {

    private final PayPalHttpClient client;
    private final String APPROVE_LINK_REL = "approve";

    public PayPalPaymentService(PayPalEnvironment payPalEnvironment) {
        this.client = new PayPalHttpClient(payPalEnvironment);
    }

    @SneakyThrows
    @Override
    public PendingPayment create(Order orderToPay, URI returnUri) {
        com.paypal.orders.Order order;
        OrderRequest orderRequest = new OrderRequest();

        orderRequest.checkoutPaymentIntent("CAPTURE");
        List<PurchaseUnitRequest> purchaseUnits = new ArrayList<>();

        purchaseUnits.add(new PurchaseUnitRequest().amountWithBreakdown(new AmountWithBreakdown().currencyCode(PaymentService.DEFAULT_CURRENCY).value(orderToPay.getTotal())));
        orderRequest.purchaseUnits(purchaseUnits);
        OrdersCreateRequest request = new OrdersCreateRequest().requestBody(orderRequest);
        orderRequest.applicationContext(new ApplicationContext().returnUrl(returnUri.toString()).brandName("Cake Factory"));

        HttpResponse<com.paypal.orders.Order> response = client.execute(request);
        order = response.result();

        log.debug("Created order {}", order.id());

        LinkDescription approveUri = order.links().stream().filter(link -> APPROVE_LINK_REL.equals(link.rel()))
                .findFirst()
                .orElseThrow();

        return new PendingPayment(order.id(), URI.create(approveUri.href()));
    }

    @SneakyThrows
    @Override
    public String complete(String orderId) {
        OrdersCaptureRequest request = new OrdersCaptureRequest(orderId);
        HttpResponse<com.paypal.orders.Order> response = client.execute(request);
        return response.result().status();
    }
}
