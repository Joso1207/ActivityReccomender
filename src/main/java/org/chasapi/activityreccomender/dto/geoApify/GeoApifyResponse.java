package org.chasapi.activityreccomender.dto.geoApify;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record GeoApifyResponse (
        String type,

        @JsonProperty("features")
        List<Place> place){
}
