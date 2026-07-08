package org.chasapi.activityreccomender.dto.places;


import jakarta.validation.Valid;
import org.chasapi.activityreccomender.dto.InputCordinates;

import java.util.List;

public record PlacesRequest(@Valid InputCordinates cords, List<String> categories){
}
