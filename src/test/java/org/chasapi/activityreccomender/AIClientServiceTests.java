package org.chasapi.activityreccomender;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.validation.Validator;
import org.chasapi.activityreccomender.dto.AiResponseDTO;
import org.chasapi.activityreccomender.dto.weather.CurrentWeatherData;
import org.chasapi.activityreccomender.dto.weather.WeatherResponse;
import org.chasapi.activityreccomender.service.AiClientService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestClient;
import org.springframework.http.ResponseEntity;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.Objects;

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


@SpringBootTest
@ActiveProfiles("test")
class AIClientServiceTests {

    @Autowired
    private AiClientService service;

    @MockitoBean
    private RestClient restClient;

    @Autowired
    @Qualifier("asyncCacheManager")
    private CacheManager cacheManager;

    @BeforeEach
    void clearCache() {
        Objects.requireNonNull(cacheManager.getCache("AI_Response")).clear();
    }

    @Test
    void shouldRetryThreeTimesOn429() {

        stub429(restClient);

        assertFalse(service.generateAIResponse(
                            WeatherResponse.builder().build()
                        ).AI_Available()
        );

        verify(restClient, times(3)).post();
    }


    //Make the client respond with 429 only.
    static void stub429(RestClient client) {

        when(client.post()).thenAnswer(invocation -> {

            var post = mock(RestClient.RequestBodyUriSpec.class);
            var body = mock(RestClient.RequestBodySpec.class);
            var response = mock(RestClient.ResponseSpec.class);

            when(post.uri(anyString())).thenReturn(body);
            when(body.header(anyString(), anyString())).thenReturn(body);

            when(body.body(any(Map.class))).thenReturn(body);
            when(body.body(any())).thenReturn(body);

            when(body.retrieve()).thenReturn(response);

            when(response.toEntity(String.class))
                    .thenReturn(ResponseEntity.status(429).build());

            return post;
        });
    }

    static void stub200(RestClient client) {

        String string = """
    {
      "choices": [{
        "message": {
          "content": "{\\"summary\\":\\"Sunny\\",\\"confidence\\":0.9,\\"recommendations\\":[\\"beach\\"]}"
        }
      }]
    }
    """;

        when(client.post()).thenAnswer(invocation -> {

            var post = mock(RestClient.RequestBodyUriSpec.class);
            var body = mock(RestClient.RequestBodySpec.class);
            var response = mock(RestClient.ResponseSpec.class);

            when(post.uri(anyString())).thenReturn(body);
            when(body.header(anyString(), anyString())).thenReturn(body);

            when(body.body(any(Map.class))).thenReturn(body);
            when(body.body(any())).thenReturn(body);

            when(body.retrieve()).thenReturn(response);

            when(response.toEntity(String.class))
                    .thenReturn(ResponseEntity.ok(string));

            return post;
        });
    }

    static void stub500(RestClient client) {


        when(client.post()).thenAnswer(invocation -> {

            var post = mock(RestClient.RequestBodyUriSpec.class);
            var body = mock(RestClient.RequestBodySpec.class);
            var response = mock(RestClient.ResponseSpec.class);

            when(post.uri(anyString())).thenReturn(body);
            when(body.header(anyString(), anyString())).thenReturn(body);

            when(body.body(any(Map.class))).thenReturn(body);
            when(body.body(any())).thenReturn(body);

            when(body.retrieve()).thenReturn(response);

            when(response.toEntity(String.class))
                    .thenReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("ERROR"));

            return post;
        });
    }

    @Test
    void shouldReturnFallback_whenAiResponseStartsWithTextNotJson() {

        //in-memory log collector
        Logger logger = (Logger) LoggerFactory.getLogger(AiClientService.class);

        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        WeatherResponse weather = WeatherResponse.builder().isAvailable(true).build();


        // AI hallucination prefix before JSON
        String aiResponse = """
            Sure, here is the current weather:
            {
              "choices": [
                {
                  "message": {
                    "content": "{\\"summary\\":\\"sunny\\",\\"confidence\\":0.8,\\"recommendations\\":[\\"beach\\"]}"
                  }
                }
              ]
            }
            """;

        //Mocked RestClient Response
        when(restClient.post()).thenAnswer(invocation -> {

            RestClient.RequestBodyUriSpec post = mock(RestClient.RequestBodyUriSpec.class);
            RestClient.RequestBodySpec body = mock(RestClient.RequestBodySpec.class);
            RestClient.ResponseSpec response = mock(RestClient.ResponseSpec.class);

            when(post.uri(anyString())).thenReturn(body);

            when(body.header(anyString(), anyString())).thenReturn(body);

            when(body.body(any())).thenReturn(body);
            when(body.body(anyMap())).thenReturn(body);

            when(body.retrieve()).thenReturn(response);

            when(response.toEntity(String.class))
                    .thenReturn(ResponseEntity.ok(aiResponse));

            return post;
        });

        //invoke the call
        AiResponseDTO result = service.generateAIResponse(weather);

        //Check logs
        boolean logged = appender.list.stream()
                .anyMatch(event ->
                        event.getFormattedMessage()
                                .contains("Jackson Could not map the output,  Likely hallucination or malformed JSON")
                );

        //assertions
        assertTrue(logged);
        assertEquals("AI Weather Summary unavailable", result.summary());
        assertFalse(result.AI_Available());
    }
    @Test
    void shouldCacheSuccessfulResponse() {

        WeatherResponse weather = WeatherResponse.builder()
                .isAvailable(true)
                .build();

        stub200(restClient);

        AiResponseDTO first = service.generateAIResponse(weather);
        AiResponseDTO second = service.generateAIResponse(weather);

        assertTrue(first.AI_Available());
        assertTrue(second.AI_Available());

        // Second call should come from the cache
        verify(restClient, times(1)).post();
    }


    @Test
    void shouldNotCacheUnavailableResponse() {

        WeatherResponse weather = WeatherResponse.builder()
                .isAvailable(true)
                .build();

        stub500(restClient);

        AiResponseDTO first = service.generateAIResponse(weather);
        AiResponseDTO second = service.generateAIResponse(weather);

        assertFalse(first.AI_Available());
        assertFalse(second.AI_Available());

        // Both calls should invoke the API with retries, because the fallback isn't cached
        verify(restClient, times(6)).post();
    }



}

