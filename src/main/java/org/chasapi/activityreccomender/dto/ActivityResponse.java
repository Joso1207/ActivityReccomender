package org.chasapi.activityreccomender.dto;

import lombok.Builder;
import org.chasapi.activityreccomender.dto.places.Place;

import java.util.List;

@Builder
public record ActivityResponse(String weather, List<String> recommended_activity, List<Place> places,Boolean weather_available,Boolean geosearch_available) {



}
