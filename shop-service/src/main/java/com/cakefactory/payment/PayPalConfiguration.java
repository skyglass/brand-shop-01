package com.cakefactory.payment;

import com.paypal.core.PayPalEnvironment;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Profile("!live")
@Configuration
public class PayPalConfiguration {

    @Value("${paypal.sandbox.client-id}")
    String clientId;

    @Value("${paypal.sandbox.client-secret}")
    String secret;

    @Bean
    PayPalEnvironment payPalEnvironment() {
        return new PayPalEnvironment.Sandbox(clientId, secret);
    }
}
