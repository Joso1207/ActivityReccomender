package org.chasapi.activityreccomender.webclient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.chasapi.activityreccomender.dto.WeatherResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Set;


@Service
public class OpenMeteoClient {

    @Value("${spring.meteo.key}")
    private String apiKey;

    private final WebClient client;

    public OpenMeteoClient(
            WebClient.Builder builder,
            @Value("${spring.meteo.hostname}") String baseUrl
    ) {
        this.client = builder
                .baseUrl(baseUrl)
                .build();
    }

    public Mono<WeatherResponse> getWeather(double latitude, double longitude){
        return client.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1/forecast")
                        .queryParam("latitude",latitude)
                        .queryParam("longitude",longitude)
                        .queryParam("current", "temperature_2m,precipitation,weather_code")
                        .queryParam("hourly","temperature_2m,weather_code,precipitation,precipitation_probability,wind_speed_10m")
                        .queryParam("forecast_days",1)
                        /* Not needed but showing how Query Auth works
                        .queryParam("apikey",apiKey)
                         */
                        .build())
                .retrieve()
                .bodyToMono(WeatherResponse.class);
    }
}
