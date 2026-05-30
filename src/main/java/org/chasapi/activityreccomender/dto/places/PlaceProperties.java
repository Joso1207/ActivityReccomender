package org.chasapi.activityreccomender.dto.places;

import java.util.List;

public record PlaceProperties (String formatted,
                               String opening_hours,
                              List<String> categories){ }
