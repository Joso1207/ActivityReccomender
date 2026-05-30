package org.chasapi.activityreccomender.dto.places;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record GeoApifyResponse (
        String type,

        @JsonProperty("features")
        List<Place> place){
}
