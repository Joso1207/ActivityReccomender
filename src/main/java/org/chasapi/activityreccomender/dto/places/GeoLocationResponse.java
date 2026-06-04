package org.chasapi.activityreccomender.dto.places;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.List;

@Builder(toBuilder = true)
public record GeoLocationResponse(List<Location> features, Boolean isAvailable) {
}
