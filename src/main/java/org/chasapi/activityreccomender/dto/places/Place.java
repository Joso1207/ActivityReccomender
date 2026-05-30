package org.chasapi.activityreccomender.dto.places;

import lombok.Builder;

@Builder
public record Place(String type,
        PlaceProperties properties) {
}
