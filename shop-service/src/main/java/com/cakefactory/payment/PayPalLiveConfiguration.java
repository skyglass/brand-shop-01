package com.cakefactory.payment;

import com.paypal.core.PayPalEnvironment;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Profile("live")
@Configuration
public class PayPalLiveConfiguration {

    @Value("${paypal.live.client-id}")
    String clientId;

    @Value("${paypal.live.client-secret}")
    String secret;

    @Bean
    PayPalEnvironment payPalEnvironment() {
        return new PayPalEnvironment.Live(clientId, secret);
    }
}
