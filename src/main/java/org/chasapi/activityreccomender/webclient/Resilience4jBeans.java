package org.chasapi.activityreccomender.webclient;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

@Configuration
public class Resilience4jBeans {
        @Bean
        Scheduler retryScheduler() {
            return Schedulers.boundedElastic();
        }
}

