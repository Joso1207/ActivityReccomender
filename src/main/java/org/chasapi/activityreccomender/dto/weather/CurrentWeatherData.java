package org.chasapi.activityreccomender.dto.weather;

import lombok.Builder;

@Builder
public record CurrentWeatherData(
        String time,
        Integer interval,
        Double temperature_2m,
        Double precipitation,
        Integer weather_code
) {}
