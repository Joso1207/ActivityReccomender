package org.chasapi.activityreccomender.dto.geoApify;

import java.util.List;

public record PlaceProperties (String formatted,
                               String opening_hours,
                              List<String> categories){ }
