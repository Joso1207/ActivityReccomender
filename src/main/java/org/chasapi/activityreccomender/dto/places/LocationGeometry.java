package org.chasapi.activityreccomender.dto.places;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.chasapi.activityreccomender.dto.InputCordinates;

import java.sql.Array;
import java.util.List;

public record LocationGeometry (@NotNull String type,@Valid @NotNull InputCordinates coordinates){
}
