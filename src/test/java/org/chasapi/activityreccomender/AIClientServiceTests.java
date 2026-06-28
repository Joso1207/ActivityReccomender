package org.chasapi.activityreccomender;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.validation.Validator;
import org.chasapi.activityreccomender.dto.AiResponseDTO;
import org.chasapi.activityreccomender.dto.weather.CurrentWeatherData;
import org.chasapi.activityreccomender.dto.weather.WeatherResponse;
import org.chasapi.activityreccomender.service.AiClientService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestClient;
import org.springframework.http.ResponseEntity;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

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


    @Test
    void shouldRetryThreeTimesOn429() {

        stub429(restClient);

        assertThrows(RuntimeException.class,
                () -> service.generateAIResponse(
                        WeatherResponse.builder().build()));

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


}

