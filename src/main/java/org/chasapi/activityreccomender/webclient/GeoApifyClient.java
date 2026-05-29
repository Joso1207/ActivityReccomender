package org.chasapi.activityreccomender.webclient;

import org.chasapi.activityreccomender.dto.geoApify.GeoApifyResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
public class GeoApifyClient {

    @Value("${spring.geoApify.key}")
    private String apiKey;

    private final WebClient client;

    public GeoApifyClient(WebClient.Builder builder,@Value("${spring.geoApify.hostname}") String baseURL){
        this.client = builder.baseUrl(baseURL).build();
    }

    public Mono<GeoApifyResponse> getPlacesNearLocation(Double latitude, Double longitude, List<String> categories){
        return client.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v2/places")
                        .queryParam("categories",String.join(",",categories)) //limit to categories
                        .queryParam("filter","circle:"+longitude+","+latitude+","+5000) //Within 5km
                        .queryParam("limit",50)
                        .queryParam("apiKey",apiKey)
                        .build())
                .retrieve()
                .bodyToMono(GeoApifyResponse.class);
    }
}
