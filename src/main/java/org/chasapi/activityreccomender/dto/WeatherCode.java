package org.chasapi.activityreccomender.dto;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public enum WeatherCode {

    DEFAULT(-1,"Unknown Weather"),

    CLEAR(0,"Clear Sky"),
    CLEARING(1,"Dissolving Clouds"),
    CLOUDY(2,"Cloudy"),
    OVERCAST(3,"Overcast Clouds"),
    FOG(45,"Fog"),
    RIME_FOG(48,"Rime Fog"),
    DRIZZLE_LIGHT_INTERMITTENT(50,"Intermittent Light Drizzle"),
    DRIZZLE_LIGHT_CONTINUOUS(51,"Continious Light Drizzle"),
    DRIZZLE_MODERATE_INTERMITTENT(52,"Intermittent Drizzle"),
    DRIZZLE_MODERATE_CONTINUOUS(53,"Continious Drizzle"),
    DRIZZLE_HEAVY_INTERMITTENT(54,"Intermittent Heavy Drizzle"),
    DRIZZLE_HEAVY_CONTINUOUS(55,"Continious Heavy Drizzle"),
    DRIZZLE_FREEZING_SLIGHT(56,"Slight Freezing Drizzle"),
    DRIZZLE_FREEZING_DENCE(57,"Dense Freezing Drizzle"),
    RAIN_LIGHT_INTERMITTENT(60,"Intermittent Light Rain"),
    RAIN_LIGHT_CONTINUOUS(61,"Continious Light Rain"),
    RAIN_MODERATE_INTERMITTENT(62,"Intermittent Rain"),
    RAIN_MODERATE_CONTINUOUS(63,"Continious Rain"),
    RAIN_HEAVY_INTERMITTENT(64,"Intermittent Heavy Rain"),
    RAIN_HEAVY_CONTINUOUS(65,"Continious Heavy Rain"),
    RAIN_FREEZING_SLIGHT(66,"Light Freezing Rain"),
    RAIN_FREEZING_DENCE(67,"Dense Freezing Rain"),
    SNOW_LIGHT_INTERMITTENT(70,"Intermittent Light Snowfall"),
    SNOW_LIGHT_CONTINUOUS(71,"Continious Light Snow"),
    SNOW_MODERATE_INTERMITTENT(72,"Intermittent Snowfall"),
    SNOW_MODERATE_CONTINUOUS(73,"Continious Snow"),
    SNOW_HEAVY_INTERMITTENT(74,"Intermittent Heavy Snowfall"),
    SNOW_HEAVY_CONTINUOUS(75,"Continious Heavy Snow"),
    SNOW_DIAMONDDUST(76,"Diamond dust snow"),
    SNOW_FREEZING_DENCE(77,"Snow Grains"),
    SHOWERS_LIGHT(80,"Light Rain-Showers"),
    SHOWERS_HEAVY(81,"Heavy Rain-Showers"),
    SHOWERS_VIOLENT(82,"Violent Rain-Showers"),
    SHOWERS_LIGHT_MIXED(83,"Light Snow-Mixed Showers"),
    SHOWERS_HEAVY_MIXED(84,"Heavy Snow-Mixed Showers"),
    SHOWERS_LIGHT_SNOW(85,"Light Snow Showers"),
    SHOWERS_HEAVY_SNOW(86,"Heavy Snow Showers"),
    THUNDER(95,"Thunder"),
    THUNDER_HAIL(96,"Thunder and Hail"),
    THUNDERSTORM(97,"Thunderstorm"),
    THUNDERSTORM_DUST(98,"Thunderstorm with Dust"),
    THUNDERSTORM_HAIL(99,"Thunderstorm with Hail");


    private final int code;
    private final String description;
    WeatherCode(int code, String description) {
        this.code = code;
        this.description = description;
    }

    private static final Map<Integer,WeatherCode> BY_CODE = new HashMap<>();

    static {
        for(WeatherCode weather : values()){
            BY_CODE.put(weather.code,weather);
        }
    }

    public static WeatherCode fromCode(int code){
        return BY_CODE.getOrDefault(code, DEFAULT);

    }


}
