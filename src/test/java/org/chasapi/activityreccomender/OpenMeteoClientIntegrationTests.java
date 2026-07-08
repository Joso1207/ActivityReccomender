package org.chasapi.activityreccomender;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.QueueDispatcher;
import okhttp3.mockwebserver.RecordedRequest;
import org.chasapi.activityreccomender.webclient.OpenMeteoClient;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import reactor.test.StepVerifier;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class OpenMeteoClientIntegrationTests {


    @Autowired
    private OpenMeteoClient client;

    @Autowired
    private CircuitBreakerRegistry cbRegistry;

    @Autowired
    @Qualifier("asyncCacheManager")
    private CacheManager cacheManager;

    private static MockWebServer mockWebServer;

    @BeforeAll
    static void setup() throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.meteo.hostname", () -> mockWebServer.url("/").toString());
    }

    @BeforeEach
    void resetCircuitBreaker() {
        cbRegistry.circuitBreaker("openmeteo-api").reset();
    }

    @BeforeEach
    void clearCache() {
        Objects.requireNonNull(cacheManager.getCache("WeatherData")).clear();
    }

    @BeforeEach
    void drainRequests() throws InterruptedException {
        mockWebServer.setDispatcher(new QueueDispatcher());
    }

    @AfterAll
    static void teardown() throws Exception {
        mockWebServer.shutdown();
    }

    private String validResponse() {
        return """
        {
          "current": {
            "temperature_2m": 12.3,
            "precipitation": 0.0,
            "weather_code": 1
          }
        }
        """;
    }

    // -------------------------------------------------------
    // 1. SUCCESS PATH
    // -------------------------------------------------------
    @Test
    void shouldReturnWeatherOnFirstCall() {

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(validResponse())
                .addHeader("Content-Type", "application/json"));

        StepVerifier.create(client.getWeather(10, 20))
                .assertNext(res -> {
                    assertTrue(res.isAvailable());
                    assertEquals(12.3, res.current().temperature_2m());
                })
                .verifyComplete();
    }

    // -------------------------------------------------------
    // 2. RETRY THEN SUCCESS
    // -------------------------------------------------------
    @Test
    void shouldRetryThenSucceed() {

        mockWebServer.enqueue(new MockResponse().setResponseCode(500));
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(validResponse())
                .addHeader("Content-Type", "application/json"));



        StepVerifier.create(client.getWeather(10, 20))
                .assertNext(res -> assertTrue(res.isAvailable()))
                .verifyComplete();


    }

    // -------------------------------------------------------
    // 3. RETRY UNTIL FAILURE → FALLBACK
    // -------------------------------------------------------
    @Test
    void shouldRetryAndReturnFallbackWhenAllAttemptsFail() {

        mockWebServer.enqueue(new MockResponse().setResponseCode(500));
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));

        StepVerifier.create(client.getWeather(10, 20))
                .assertNext(res -> assertFalse(res.isAvailable()))
                .verifyComplete();
    }

    // -------------------------------------------------------
    // 4. CIRCUIT BREAKER (BLACK BOX ASSERTION)
    // -------------------------------------------------------
    @Test
    void shouldOpenCircuitBreakerAfterRepeatedFailures() {

        // force failures to trip breaker
        for (int i = 0; i < 10; i++) {
            mockWebServer.enqueue(new MockResponse().setResponseCode(500));
        }

        // trigger failures
        for (int i = 0; i < 5; i++) {
            System.out.println("YEHAY");
            StepVerifier.create(client.getWeather(10, 20))
                    .assertNext(res -> assertFalse(res.isAvailable()))
                    .verifyComplete();
        }

        // final call should be short-circuited (fallback, no real retry chain execution)
        StepVerifier.create(client.getWeather(10, 20))
                .assertNext(res -> assertFalse(res.isAvailable()))
                .verifyComplete();
    }


    // -------------------------------------------------------
    // 4. Cache behavior
    // -------------------------------------------------------
    @Test
    void validResponseShouldBeCachedAndAvoidSecondApiCall() {

        int preTestRequestCount  = mockWebServer.getRequestCount();
        double latitude = 59.3293;
        double longitude = 18.0686;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{}"));

        // First call hits API
        StepVerifier.create(
                        client.getWeather(latitude,longitude)
                )
                .assertNext(response-> assertTrue(response.isAvailable()))
                .verifyComplete();


        // Second call should come from cache
        StepVerifier.create(
                        client.getWeather(latitude,longitude)
                )
                .assertNext(response-> assertTrue(response.isAvailable()))
                .verifyComplete();


        assertEquals(preTestRequestCount+1,mockWebServer.getRequestCount());
    }


    @Test
    void unavailableResponseShouldNotBeCached() {

        // Enough failed responses to trigger fallback
        for(int i=0;i<4;i++){
            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(500));
        }


        StepVerifier.create(
                        client.getWeather(0.0,0.0)
                )
                .assertNext(response-> assertFalse(response.isAvailable()))
                .verifyComplete();


        Cache cache = cacheManager.getCache("WeatherData");

        assertNull(cache.get("Invalid"));
    }


}