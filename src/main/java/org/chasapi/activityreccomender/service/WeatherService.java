package org.chasapi.activityreccomender.service;

import org.chasapi.activityreccomender.dto.weather.WeatherResponse;
import org.chasapi.activityreccomender.webclient.OpenMeteoClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class WeatherService {

    private final OpenMeteoClient client;

    public WeatherService(OpenMeteoClient client){
        this.client = client;
    }

    public Mono<WeatherResponse> getWeather(Double lat,Double lon){
        return client.getWeather(lat,lon);
    }



}
