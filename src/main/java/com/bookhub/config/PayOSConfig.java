package com.bookhub.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import vn.payos.PayOS;

@Configuration
public class PayOSConfig {

    @Bean
    public PayOS payOS() {

        String clientId = "df266e8a-cc34-4c2a-89db-148a1c35a428";
        String apiKey = "b36bf994-0273-4b17-822e-406145ffde6d";
        String checksumKey = "634e23874c68792ac6e9309e2bffb5c20a09430e8de24a5a72b231cceb191d70";

        return new PayOS(clientId, apiKey, checksumKey);
    }
}