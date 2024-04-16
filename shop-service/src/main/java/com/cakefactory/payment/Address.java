package com.cakefactory.payment;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Address {
    private final String line1;
    private final String line2;
    private final String postcode;
}
