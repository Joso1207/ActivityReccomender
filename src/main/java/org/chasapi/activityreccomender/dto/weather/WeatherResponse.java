package org.chasapi.activityreccomender.dto.weather;

import lombok.Builder;

@Builder
public record WeatherResponse(
        double latitude,
        double longitude,
        String timezone,
        String timezone_abbreviation,
        CurrentWeatherData current,
        HourlyWeatherData hourly
) {}
