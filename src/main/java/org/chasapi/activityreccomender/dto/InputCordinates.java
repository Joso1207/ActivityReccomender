package org.chasapi.activityreccomender.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.Builder;

@Builder
public record InputCordinates(
        @DecimalMax("90.0")
        @DecimalMin("-90.0")
        Double latitude,
        @DecimalMax("180.0")
        @DecimalMin("-180.0")
        Double longitude) {
}
