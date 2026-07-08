package org.chasapi.activityreccomender.dto.weather;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
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
