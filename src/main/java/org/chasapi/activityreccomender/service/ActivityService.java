package org.chasapi.activityreccomender.service;

import org.chasapi.activityreccomender.dto.ActivityResponse;
import org.chasapi.activityreccomender.dto.InputCordinates;
import org.chasapi.activityreccomender.dto.WeatherCode;
import org.chasapi.activityreccomender.webclient.GeoApifyClient;
import org.chasapi.activityreccomender.webclient.OpenMeteoClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
public class ActivityService {

    private final OpenMeteoClient weatherClient;
    private final GeoApifyClient placesClient;

    public ActivityService(OpenMeteoClient weatherClient, GeoApifyClient placesClient) {
        this.weatherClient = weatherClient;
        this.placesClient = placesClient;
    }

    private final List<String> indoorActivities = List.of("Stay inside","Bowling","Cinema","Entertainment");
    private final List<String> outDoorActivities = List.of("Enjoy the weather","Camp","BirdWatch","Swim");

    public Mono<ActivityResponse> getActivities(InputCordinates cordinates){
        return weatherClient.getWeather(cordinates.latitude(),cordinates.longitude()
        ).flatMap(weatherResponse ->{
            String category;

            if (weatherResponse.current().weather_code() <= WeatherCode.OVERCAST.getCode()){
                category = "leisure.park";
            } else {
                category = "entertainment";
            }

            return placesClient.getPlacesNearLocation(
                    cordinates.latitude(), cordinates.longitude(),
                    List.of(category)
            ).map(placeReponse ->{

                List<String> activities;
                if (weatherResponse.current().weather_code() <= WeatherCode.OVERCAST.getCode()){
                    activities = List.copyOf(outDoorActivities);
                } else {
                    activities = List.copyOf(indoorActivities);
                }

                return new ActivityResponse(
                        WeatherCode.fromCode(weatherResponse.current().weather_code()).getDescription(),
                        activities,
                        placeReponse.place());
            });
        });
    }
}
