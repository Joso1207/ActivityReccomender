package org.chasapi.activityreccomender.webclient;


import org.chasapi.activityreccomender.dto.places.GeoLocationResponse;
import org.chasapi.activityreccomender.dto.places.GeoPlacesResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreakerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;

@Service
public class GeoApifyClient {

    @Value("${spring.geoApify.key}")
    private String apiKey;

    private final WebClient client;
    private final ReactiveCircuitBreaker circuitBreaker;
    private final Scheduler retryScheduler;

    public GeoApifyClient(WebClient.Builder builder, @Value("${spring.geoApify.hostname}") String baseURL, ReactiveCircuitBreakerFactory<?,?> factory, Scheduler retryScheduler){
        this.circuitBreaker = factory.create("geoapify-api");
        this.retryScheduler = retryScheduler;
        this.client = builder.baseUrl(baseURL).build();
    }

    //using freeform adress query instead of City as using the structured address limits results to cities only which may even be in other countries,
    //Freeform allows one to specify more specificity
    public Mono<GeoLocationResponse> getGeoLocation(String query){
        Mono<GeoLocationResponse> request = client.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1/geocode/search")
                        .queryParam("text",query)
                        .queryParam("apiKey",apiKey)
                        .build())
                .retrieve()
                .bodyToMono(GeoLocationResponse.class)
                .map(response -> response.toBuilder().isAvailable(true).build())
                .retryWhen(Retry.backoff(3,Duration.ofSeconds(2)).scheduler(retryScheduler)
                        .filter(ex -> {
                            if (ex instanceof WebClientRequestException) {
                                return true;
                            }
                            if (ex instanceof WebClientResponseException responseEx) {
                                return responseEx.getStatusCode().is5xxServerError();
                            }
                            return false;
                        })
                );
        return circuitBreaker.run(request,ex->Mono.just(new GeoLocationResponse(null,false)));
    }

    public Mono<GeoPlacesResponse> getPlacesNearLocation(Double latitude, Double longitude, List<String> categories){
        Mono<GeoPlacesResponse> request = client.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v2/places")
                        .queryParam("categories",String.join(",",categories)) //limit to categories
                        .queryParam("filter","circle:"+longitude+","+latitude+","+5000) //Within 5km
                        .queryParam("limit",50)
                        .queryParam("apiKey",apiKey)
                        .build())
                .retrieve()
                .bodyToMono(GeoPlacesResponse.class)
                .map(response -> new GeoPlacesResponse(true,response.place()))
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(3)).scheduler(retryScheduler)
                        .filter(ex -> {
                            if (ex instanceof WebClientRequestException) {
                                return true;
                            }
                            if (ex instanceof WebClientResponseException responseEx) {
                                return responseEx.getStatusCode().is5xxServerError();
                            }
                            return false;
                        })
                );
        return circuitBreaker.run(
             request,
             ex -> Mono.just(
                     new GeoPlacesResponse(false,List.of()
                     )
             )
        );
    }
}
