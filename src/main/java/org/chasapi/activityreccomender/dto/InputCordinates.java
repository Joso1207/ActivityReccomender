package org.chasapi.activityreccomender.dto;

import lombok.Builder;

@Builder
public record InputCordinates(Double latitude, Double longitude) {
}
