package org.chasapi.activityreccomender.dto.places;

import java.sql.Array;
import java.util.List;

public record LocationGeometry (String type, List<Double> coordinates){
}
