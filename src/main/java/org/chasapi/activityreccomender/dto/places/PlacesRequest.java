package org.chasapi.activityreccomender.dto.places;


import org.chasapi.activityreccomender.dto.InputCordinates;

import java.util.List;

public record PlacesRequest(InputCordinates cords, List<String> categories){
}
