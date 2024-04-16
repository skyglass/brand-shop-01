package com.cakefactory.payment;

import java.net.URI;

public interface PaymentService {
    String DEFAULT_CURRENCY = "GBP";

    PendingPayment create(Order orderToPay, URI returnUri);
    String complete(String orderId);
}
