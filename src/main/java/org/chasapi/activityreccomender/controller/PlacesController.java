package org.chasapi.activityreccomender.controller;

import org.chasapi.activityreccomender.dto.InputCordinates;
import org.chasapi.activityreccomender.dto.places.GeoApifyResponse;
import org.chasapi.activityreccomender.service.PlacesService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/places")
public class PlacesController {

    private final PlacesService service;

    public PlacesController(PlacesService service) {
        this.service = service;
    }

    @GetMapping("/near")
    public Mono<GeoApifyResponse> getPlaces(
            @RequestParam double latitude,
            @RequestParam double longitude,
            @RequestParam(name = "categories") List<String> categories
    ) {
        return service.getPlacesWithCategories(
                new InputCordinates(latitude, longitude),
                categories
        );
    }

}
