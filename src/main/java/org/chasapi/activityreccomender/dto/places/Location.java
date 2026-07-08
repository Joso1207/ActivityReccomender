package org.chasapi.activityreccomender.dto.places;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record Location(@NotNull LocationProperties properties,@Valid @NotNull LocationGeometry geometry){

}
