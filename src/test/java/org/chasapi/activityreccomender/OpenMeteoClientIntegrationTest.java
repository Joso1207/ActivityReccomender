package org.chasapi.activityreccomender;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.chasapi.activityreccomender.webclient.OpenMeteoClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OpenMeteoClientIntegrationTest {

    private static MockWebServer mockWebServer;

    @BeforeAll
    static void startServer() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
    }

    @AfterAll
    static void shutdown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void shouldCallWeatherApi() throws Exception {

        mockWebServer.enqueue(
                new MockResponse()
                        .setResponseCode(200)
                        .setHeader("Content-Type", "application/json")
                        .setBody("""
                                {
                                  "temperature": 20,
                                  "condition": "Sunny"
                                }
                                """)
        );

        OpenMeteoClient client = new OpenMeteoClient(
                WebClient.builder(),
                mockWebServer.url("/").toString()
        );

        StepVerifier.create(
                        client.getWeather(59.3293, 18.0686)
                )
                .expectNextCount(1)
                .verifyComplete();

        RecordedRequest request = mockWebServer.takeRequest();

        assertEquals("GET", request.getMethod());

        assertEquals(
                "/forecast?lat=59.3293&lon=18.0686",
                request.getPath()
        );
    }
}