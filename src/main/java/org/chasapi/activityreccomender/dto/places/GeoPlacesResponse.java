package org.chasapi.activityreccomender.dto.places;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record GeoPlacesResponse(
        Boolean isAvailable,

        @JsonProperty("features")
        List<Place> place){
}
