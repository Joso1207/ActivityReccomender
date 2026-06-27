package org.chasapi.activityreccomender.service;

import org.chasapi.activityreccomender.dto.ActivityResponse;
import org.chasapi.activityreccomender.dto.AiResponseDTO;
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
    private final AiClientService aiService;

    public ActivityService(OpenMeteoClient weatherClient, GeoApifyClient placesClient, AiClientService aiService) {
        this.weatherClient = weatherClient;
        this.placesClient = placesClient;
        this.aiService = aiService;
    }

    /*
    private final List<String> indoorActivities = List.of("Stay inside","Bowling","Cinema","Entertainment");
    private final List<String> outDoorActivities = List.of("Enjoy the weather","Camp","BirdWatch","Swim");
    private final List<String> unknownWeatherActivityes= List.of("Depends on weather, Shopping, Resturaunt, Entertainment");
    */
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
            AiResponseDTO aiResponse;

            if(!weatherResponse.isAvailable()){
                aiResponse = aiService.fallback();
                category = "entertainment,commercial,catering";
            }
            else {
                aiResponse = aiService.generateAIResponse(weatherResponse);
                category = String.join(",",aiResponse.recommendations());
            }

            return placesClient.getPlacesNearLocation(
                    coordinates.latitude(), coordinates.longitude(),
                    List.of(category)
            ).map(placeReponse -> new ActivityResponse(
                    WeatherCode.fromCode(weatherResponse.current().weather_code()).getDescription(),
                    aiResponse.summary(),
                    List.of(category),
                    placeReponse.place(),
                    weatherResponse.isAvailable(),
                    placeReponse.isAvailable()

            ));
        });
    }
}
