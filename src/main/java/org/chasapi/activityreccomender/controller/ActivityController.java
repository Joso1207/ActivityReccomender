package org.chasapi.activityreccomender.controller;

import org.chasapi.activityreccomender.dto.ActivityResponse;
import org.chasapi.activityreccomender.dto.InputCordinates;
import org.chasapi.activityreccomender.service.ActivityService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api")
public class ActivityController {

    private final ActivityService service;

    public ActivityController(ActivityService service) {
        this.service = service;
    }

    @GetMapping("/activity")
    public Mono<ActivityResponse> getActivities(@RequestBody InputCordinates cordinates){
        return service.getActivities(cordinates);
    }

}
