package com.elorankingbot.patreon;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientFactory {

    @Bean
    public WebClient webClient() {
        return WebClient.create();
    }
}
