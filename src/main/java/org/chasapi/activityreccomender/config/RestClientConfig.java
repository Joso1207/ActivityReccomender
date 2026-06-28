package org.chasapi.activityreccomender.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class RestClientConfig {

    @Bean
    RestClient openAiRestClient(
            @Value("${openai.timeouts.read}") int readTimeout,
            @Value("${openai.timeouts.connect}") int connectTimeout) {

        var factory = new SimpleClientHttpRequestFactory();
        factory.setReadTimeout(Duration.ofMillis(readTimeout));
        factory.setConnectTimeout(Duration.ofMillis(connectTimeout));

        return RestClient.builder()
                .requestFactory(factory)
                .build();
    }
}
