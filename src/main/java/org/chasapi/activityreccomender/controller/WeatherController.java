package org.chasapi.activityreccomender.controller;

import org.chasapi.activityreccomender.dto.InputCordinates;
import org.chasapi.activityreccomender.dto.WeatherResponse;
import org.chasapi.activityreccomender.service.WeatherService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/weather")
public class WeatherController {
    private final WeatherService service;

    public WeatherController(WeatherService service){
        this.service = service;
    }

    @GetMapping("/current")
    public Mono<WeatherResponse> getWeather(@RequestBody InputCordinates cords){

        return service.getWeather(cords.latitude(),cords.longitude());

    }



}
