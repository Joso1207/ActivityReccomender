package org.chasapi.activityreccomender.webclient;

import org.chasapi.activityreccomender.dto.weather.WeatherResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreakerFactory;
import org.springframework.core.codec.DecodingException;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

import java.time.Duration;


@Service
public class OpenMeteoClient {

    @Value("${spring.meteo.token}")
    private String Token;

    private final WebClient client;
    private final ReactiveCircuitBreaker circuitBreaker;
    private final Scheduler retryScheduler;

    public OpenMeteoClient(
            WebClient.Builder builder,
            @Value("${spring.meteo.hostname}") String baseUrl,
            ReactiveCircuitBreakerFactory<?,?> cbFactory, Scheduler retryScheduler
    ) {
        this.retryScheduler = retryScheduler;
        this.client = builder
                .baseUrl(baseUrl)
                .build();
        this.circuitBreaker = cbFactory.create("openmeteo-api");
    }

    public Mono<WeatherResponse> getWeather(double latitude, double longitude){
        Mono<WeatherResponse> response = client.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1/forecast")
                        .queryParam("latitude",latitude)
                        .queryParam("longitude",longitude)
                        .queryParam("current", "temperature_2m,precipitation,weather_code")
                        .queryParam("hourly","temperature_2m,weather_code,precipitation,precipitation_probability,wind_speed_10m")
                        .queryParam("forecast_days",1)
                        /* Not needed but showing how Auth Headers are sent,
                        .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                         */
                        .build())
                .retrieve()
                .bodyToMono(WeatherResponse.class)
                .retryWhen(
                        Retry.backoff(3, Duration.ofSeconds(2)).scheduler(retryScheduler)
                                .doBeforeRetry(signal ->
                                        System.out.println("RETRY #" + signal.totalRetriesInARow())
                                )
                                .filter(ex ->
                                        ex instanceof WebClientRequestException ||
                                                (ex instanceof WebClientResponseException r &&
                                                        r.getStatusCode().is5xxServerError())
                                ))
                .map(weatherResponse -> weatherResponse.toBuilder()
                        .isAvailable(true)
                        .build())
                .onErrorResume(ex -> ex instanceof DecodingException, ex ->
                    Mono.error(new RuntimeException(
                        "Failed to decode WeatherResponse from API", ex))
                )
                .doOnNext(r -> System.out.println("SUCCESS: " + r))
                .doOnError(e -> System.out.println("ERROR: " + e.getClass() + " - " + e.getMessage()))
                .doOnSubscribe(s -> System.out.println("SUBSCRIBED"))
                .doFinally(signal -> System.out.println("TERMINATED: " + signal))
                .doOnCancel(() -> {
                    System.out.println("CANCELLED - capturing stack");
                    new RuntimeException("cancel trace").printStackTrace();
        });




        return circuitBreaker.run(
                response,
                ex -> Mono.just(
                        WeatherResponse.builder()
                                .isAvailable(false)
                                .build()
                )
        );

    }
}
