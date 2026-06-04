package org.chasapi.activityreccomender.service;

import org.chasapi.activityreccomender.dto.InputCordinates;
import org.chasapi.activityreccomender.dto.places.GeoPlacesResponse;
import org.chasapi.activityreccomender.webclient.GeoApifyClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
public class PlacesService {

    private final GeoApifyClient client;

    public PlacesService(GeoApifyClient client){
        this.client = client;
    }

    public Mono<GeoPlacesResponse> getPlacesWithCategories(InputCordinates cordinates, List<String> categories){
        return client.getPlacesNearLocation(cordinates.latitude(),cordinates.longitude(),categories);
    }


}
