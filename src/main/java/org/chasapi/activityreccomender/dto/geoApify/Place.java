package org.chasapi.activityreccomender.dto.geoApify;

import lombok.Builder;

@Builder
public record Place(String type,
        PlaceProperties properties) {
}
