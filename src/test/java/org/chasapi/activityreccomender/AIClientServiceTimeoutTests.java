package org.chasapi.activityreccomender;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.chasapi.activityreccomender.dto.weather.WeatherResponse;
import org.chasapi.activityreccomender.webclient.GeoApifyClient;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.chasapi.activityreccomender.service.AiClientService;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.Objects;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = {
        "openai.timeouts.read=10"
})
@ActiveProfiles("test")
class AIClientServiceTimeoutTests {

    @Autowired
    private AiClientService service;

    @Test
    void shouldLogNetworkException() {

        // Attach in-memory log collector
        Logger logger = (Logger) LoggerFactory.getLogger(AiClientService.class);

        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            service.generateAIResponse(WeatherResponse.builder().build());
        } catch (Exception ignored) {
            // expected failure after retries
        }

        // Assert log
        await().atMost(Duration.ofSeconds(2))
                .untilAsserted(() -> {
                    boolean logged = appender.list.stream()
                            .map(ILoggingEvent::getFormattedMessage)
                            .filter(Objects::nonNull)
                            .anyMatch(msg ->
                                    msg.contains("Network exception: API failed to respond"));

                    assertTrue(logged);
                });

        logger.detachAppender(appender);
    }
}