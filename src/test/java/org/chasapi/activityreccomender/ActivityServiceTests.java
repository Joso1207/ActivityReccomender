package org.chasapi.activityreccomender;

import org.chasapi.activityreccomender.dto.*;
import org.chasapi.activityreccomender.dto.places.GeoLocationResponse;
import org.chasapi.activityreccomender.dto.places.GeoPlacesResponse;
import org.chasapi.activityreccomender.dto.places.Place;
import org.chasapi.activityreccomender.dto.weather.CurrentWeatherData;
import org.chasapi.activityreccomender.dto.weather.WeatherResponse;
import org.chasapi.activityreccomender.exceptions.ExternalServiceUnavailable;
import org.chasapi.activityreccomender.exceptions.LocationNotFoundException;
import org.chasapi.activityreccomender.service.ActivityService;
import org.chasapi.activityreccomender.service.AiClientService;
import org.chasapi.activityreccomender.webclient.GeoApifyClient;
import org.chasapi.activityreccomender.webclient.OpenMeteoClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActivityServiceTests {

    @Mock
    private OpenMeteoClient weatherClient;

    @Mock
    private AiClientService aiClient;

    @Mock
    private GeoApifyClient geoApifyClient;

    @InjectMocks
    private ActivityService activityService;

    @Test
    void shouldThrowLocationNotFoundExceptionWhenNoFeaturesReturned() {

        GeoLocationResponse response =
                new GeoLocationResponse(List.of(),true);

        when(geoApifyClient.getGeoLocation("Atlantis"))
                .thenReturn(Mono.just(response));

        StepVerifier.create(activityService.getActivity("Atlantis"))
                .expectError(LocationNotFoundException.class)
                .verify();
    }

    @Test
    void shouldThrowExternalServiceUnavailableWhenGeoApiFails() {

        when(geoApifyClient.getGeoLocation("Stockholm"))
                .thenReturn(Mono.error(new RuntimeException("timeout")));

        StepVerifier.create(activityService.getActivity("Stockholm"))
                .expectError(ExternalServiceUnavailable.class)
                .verify();
    }

    @Test
    void shouldReturnOutdoorActivitiesWhenWeatherIsGood() {

        InputCordinates coordinates =
                InputCordinates.builder()
                        .latitude(59.3293)
                        .longitude(18.0686)
                        .build();

        WeatherResponse weatherResponse = WeatherResponse.builder()
                .current(CurrentWeatherData.builder().weather_code(1).build())
                .isAvailable(true)
                .build();

        GeoPlacesResponse placeResponse = new GeoPlacesResponse(
                true,
                List.of(Place.builder().type("--").build()));

        when(weatherClient.getWeather(59.3293, 18.0686))
                .thenReturn(Mono.just(weatherResponse));

        when(aiClient.generateAIResponse(weatherResponse))
                .thenReturn(new AiResponseDTO("Enjoy the weather",1.0,true,List.of("leisure.park","heritage","tourism")));

        when(geoApifyClient.getPlacesNearLocation(
                59.3293,
                18.0686,
                List.of("leisure.park","heritage","tourism"))
        ).thenReturn(Mono.just(placeResponse));

        StepVerifier.create(
                        activityService.getActivitiesByCoordinate(coordinates))
                .assertNext(response -> {

                    assert response.weather_available();
                    assert response.geosearch_available();
                    assert response.AI_Available();

                    assert response.aiSummary()
                            .contains("Enjoy the weather");
                    assert response.recommended_activity()
                            .contains("tourism");
                })
                .verifyComplete();
    }



    @Test
    void shouldReturnIndoorActivitiesWhenWeatherIsBad() {

        InputCordinates coordinates =
                InputCordinates.builder()
                        .latitude(59.3293)
                        .longitude(18.0686)
                        .build();

        WeatherResponse weatherResponse = WeatherResponse.builder()
                .current(CurrentWeatherData.builder().weather_code(95).build())
                .isAvailable(true)
                .build();

        GeoPlacesResponse placeResponse = new GeoPlacesResponse(
                true,
                List.of(Place.builder().type("--").build()));

        when(weatherClient.getWeather(59.3293, 18.0686))
                .thenReturn(Mono.just(weatherResponse));

        when(aiClient.generateAIResponse(weatherResponse))
                .thenReturn(new AiResponseDTO("ITS RAINING CATS AND DOGS",1.0,true,List.of("entertainment","commercial")));


        when(geoApifyClient.getPlacesNearLocation(
                59.3293,
                18.0686,
                List.of("entertainment","commercial")
        )).thenReturn(Mono.just(placeResponse));

        StepVerifier.create(
                        activityService.getActivitiesByCoordinate(coordinates))
                .assertNext(response -> {

                    assert response.weather_available();

                    assert response.recommended_activity()
                            .contains("commercial");

                    assert response.recommended_activity()
                            .contains("entertainment");
                })
                .verifyComplete();
    }

    @Test
    void shouldReturnUnknownActivitiesWhenWeatherUnavailable() {

        InputCordinates coordinates =
                InputCordinates.builder()
                        .latitude(59.3293)
                        .longitude(18.0686)
                        .build();

        WeatherResponse weatherResponse = WeatherResponse.builder()
                .current(CurrentWeatherData.builder().weather_code(-1).build())
                .isAvailable(false)
                .build();

        GeoPlacesResponse placeResponse = new GeoPlacesResponse(
                true,
                List.of(Place.builder().type("Mall").build()));

        when(weatherClient.getWeather(59.3293, 18.0686))
                .thenReturn(Mono.just(weatherResponse));


        /* Stubbing AIClient is not neccesary,  It already returns fallback(with httpcall) when weatherservice is unavailable
        when(aiClient.generateAIResponse(weatherResponse))
                .thenReturn(fallback);
         */
        when(geoApifyClient.getPlacesNearLocation(
                59.3293,
                18.0686,
                AiClientService.fallback().recommendations()
        )).thenReturn(Mono.just(placeResponse));

        StepVerifier.create(
                        activityService.getActivitiesByCoordinate(coordinates))
                .assertNext(response -> {

                    assert !response.weather_available();

                    assert response.recommended_activity()
                            .contains(
                                    "commercial"
                            );
                })
                .verifyComplete();
    }
}