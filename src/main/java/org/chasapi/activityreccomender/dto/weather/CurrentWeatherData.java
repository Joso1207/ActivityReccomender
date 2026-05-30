package org.chasapi.activityreccomender.dto.weather;

import lombok.Builder;

@Builder
public record CurrentWeatherData(
        String time,
        int interval,
        double temperature_2m,
        double precipitation,
        int weather_code
) {}
