package org.chasapi.activityreccomender;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.chasapi.activityreccomender.webclient.GeoApifyClient;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class GeoApifyClientIntegrationTests {

    @Autowired
    private GeoApifyClient client;
    @Autowired
    private CircuitBreakerRegistry cbRegistry;
    private static MockWebServer mockWebServer;

    @BeforeAll
    static void setup() throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.geoApify.hostname", () -> mockWebServer.url("/").toString());
        registry.add("spring.geoApify.key",()->"test-api-key");
    }

    @BeforeEach
    void resetCircuitBreaker() {
        cbRegistry.circuitBreaker("geoapify-api").reset();


    }


    @BeforeEach
    void drainRequests() throws InterruptedException {
        RecordedRequest request;
        while ((request = mockWebServer.takeRequest(500, TimeUnit.MILLISECONDS)) != null) {
            System.out.println("drained request: " + request);
        }
    }

    @AfterAll
    static void teardown() throws Exception {
        mockWebServer.shutdown();
    }

    @Test
    void shouldReturnPlacesSuccessfully() {

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {
                          "type": "FeatureCollection",
                          "features": []
                        }
                        """));

        StepVerifier.create(
                        client.getPlacesNearLocation(
                                59.3,
                                18.0,
                                List.of("catering.restaurant")
                        )
                )
                .assertNext(response -> {
                    assertTrue(response.isAvailable());
                    assertNotNull(response.place());
                })
                .verifyComplete();
    }

    @Test
    void shouldRetryOnServerErrors() {

        mockWebServer.enqueue(new MockResponse().setResponseCode(500));
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {
                          "type": "FeatureCollection",
                          "features": []
                        }
                        """));

        StepVerifier.create(
                        client.getPlacesNearLocation(
                                59.3,
                                18.0,
                                List.of("catering.restaurant")
                        )
                )
                .assertNext(response -> assertTrue(response.isAvailable()))
                .verifyComplete();
    }

    @Test
    void shouldOpenCircuitBreakerAndReturnFallback() {

        // force failures to open circuit breaker
        for (int i = 0; i < 10; i++) {
            mockWebServer.enqueue(new MockResponse().setResponseCode(500));
        }

        // drive circuit breaker state
        for (int i = 0; i < 5; i++) {
            StepVerifier.create(
                            client.getPlacesNearLocation(59.3, 18.0, List.of("catering.restaurant"))
                    )
                    .assertNext(response -> assertFalse(response.isAvailable()))
                    .verifyComplete();
        }

        // final call should be short-circuited (fallback)
        StepVerifier.create(
                        client.getPlacesNearLocation(59.3, 18.0, List.of("catering.restaurant"))
                )
                .assertNext(response -> {
                    assertFalse(response.isAvailable());
                    assertTrue(response.place().isEmpty());
                })
                .verifyComplete();
    }
}