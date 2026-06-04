package org.chasapi.activityreccomender.service;

import org.chasapi.activityreccomender.dto.ActivityResponse;
import org.chasapi.activityreccomender.dto.InputCordinates;
import org.chasapi.activityreccomender.dto.WeatherCode;
import org.chasapi.activityreccomender.exceptions.ExternalServiceUnavailable;
import org.chasapi.activityreccomender.exceptions.LocationNotFoundException;
import org.chasapi.activityreccomender.webclient.GeoApifyClient;
import org.chasapi.activityreccomender.webclient.OpenMeteoClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

//There was plans of splitting this into two different services but ultimately for this its not needed.
//The places and weather services would not have any additional logic other than calling the clients since its this class that decides what the clients should fetch

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
    private final List<String> unknownWeatherActivityes= List.of("Depends on weather, Shopping, Resturaunt, Entertainment");

    public Mono<ActivityResponse> getActivity(String query) {
        return placesClient.getGeoLocation(query)
                .onErrorMap(ex ->
                        new ExternalServiceUnavailable(
                                "GeoApify service is unavailable"
                        ))
                .flatMap(geoLocationResponse -> {

                    if (geoLocationResponse.features().isEmpty()) {
                        return Mono.error(
                                new LocationNotFoundException(
                                        "No location found for: " + query
                                )
                        );
                    }

                    InputCordinates coordinates = InputCordinates.builder()
                            .latitude(geoLocationResponse.features().getFirst().properties().lat())
                            .longitude(geoLocationResponse.features().getFirst().properties().lon())
                            .build();

                    return getActivitiesByCoordinate(coordinates);
                });
    }


    public Mono<ActivityResponse> getActivitiesByCoordinate(InputCordinates coordinates){
        return weatherClient.getWeather(coordinates.latitude(), coordinates.longitude()
        ).flatMap(weatherResponse ->{
            String category;


            if(!weatherResponse.isAvailable()){
                category = "entertainment,commercial,catering";
            }
            else if (weatherResponse.current().weather_code() <= WeatherCode.OVERCAST.getCode()){
                category = "leisure.park,heritage,tourism";
            } else {
                category = "entertainment";
            }

            return placesClient.getPlacesNearLocation(
                    coordinates.latitude(), coordinates.longitude(),
                    List.of(category)
            ).map(placeReponse ->{

                List<String> activities;
                if(!weatherResponse.isAvailable()){
                    activities = List.copyOf(unknownWeatherActivityes);
                }
                else if (weatherResponse.current().weather_code() <= WeatherCode.OVERCAST.getCode()){
                    activities = List.copyOf(outDoorActivities);
                } else {
                    activities = List.copyOf(indoorActivities);
                }

                return new ActivityResponse(
                        WeatherCode.fromCode(weatherResponse.current().weather_code()).getDescription(),
                        activities,
                        placeReponse.place(),
                        weatherResponse.isAvailable(),
                        placeReponse.isAvailable()

                );
            });
        });
    }
}
