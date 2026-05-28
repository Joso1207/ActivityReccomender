package org.chasapi.activityreccomender.dto;

import lombok.Builder;

import java.time.LocalDate;
import java.util.Map;

@Builder
public record WeatherResponse(
        double latitude,
        double longitude,
        String timezone,
        String timezone_abbreviation,
        CurrentWeatherData current,
        HourlyWeatherData hourly
) {}
