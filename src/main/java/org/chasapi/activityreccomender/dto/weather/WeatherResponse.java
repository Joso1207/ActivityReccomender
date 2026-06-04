package org.chasapi.activityreccomender.dto.weather;

import lombok.Builder;

@Builder(toBuilder = true)
public record WeatherResponse(
        Double latitude,
        Double longitude,
        String timezone,
        String timezone_abbreviation,
        CurrentWeatherData current,
        HourlyWeatherData hourly,
        Boolean isAvailable
) {}
