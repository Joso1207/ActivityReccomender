package org.chasapi.activityreccomender;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class ActivityReccomenderApplication {

    public static void main(String[] args) {
        SpringApplication.run(ActivityReccomenderApplication.class, args);
    }

}
