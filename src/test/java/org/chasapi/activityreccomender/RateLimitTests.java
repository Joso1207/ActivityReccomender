package org.chasapi.activityreccomender;

import org.chasapi.activityreccomender.controller.ActivityController;
import org.chasapi.activityreccomender.dto.ActivityResponse;
import org.chasapi.activityreccomender.service.ActivityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;

import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureWebTestClient
class RateLimitTests {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private ActivityService activityService;


    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void printContextType() {
        System.out.println(applicationContext.getClass());
    }


    @BeforeEach
    void setUp() {
        when(activityService.getActivity(any()))
                .thenReturn(Mono.just(ActivityResponse.builder().build()));

        when(activityService.getActivitiesByCoordinate(any()))
                .thenReturn(Mono.just(ActivityResponse.builder().build()));
    }

    @Test
    void shouldRateLimitRequests() {
        for (int i = 0; i < 5; i++) {
            webTestClient.get()
                    .uri("/api/recomendations?query=test")
                    .exchange()
                    .expectStatus().isOk();
        }

        webTestClient.get()
                .uri("/api/recomendations?query=test")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }
}