package org.chasapi.activityreccomender.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record HourlyWeatherData(
        List<String> time,
        List<Double> temperature_2m,
        List<Integer> weather_code,
        List<Double> precipitation,
        List<Integer> precipitation_probability,
        List<Double> wind_speed_10m
) {}
