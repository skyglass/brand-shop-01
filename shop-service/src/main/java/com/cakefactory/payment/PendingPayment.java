package com.cakefactory.payment;

import lombok.Data;

import java.net.URI;

@Data
public class PendingPayment {

    private final String id;
    private final URI approveUri;

}
