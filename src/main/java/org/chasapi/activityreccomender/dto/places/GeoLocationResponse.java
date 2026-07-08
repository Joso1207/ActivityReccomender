package org.chasapi.activityreccomender.dto.places;


import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.List;

@Builder(toBuilder = true)
public record GeoLocationResponse( @NotNull List<@Valid Location> features, Boolean isAvailable) {
}
