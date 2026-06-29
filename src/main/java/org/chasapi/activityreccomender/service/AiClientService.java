package org.chasapi.activityreccomender.service;

import jakarta.annotation.PostConstruct;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.extern.java.Log;
import lombok.extern.slf4j.Slf4j;
import org.chasapi.activityreccomender.dto.AiResponseDTO;
import org.chasapi.activityreccomender.dto.weather.WeatherResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import tools.jackson.core.JacksonException;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.databind.DatabindException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

@Service
@Slf4j
public class AiClientService {

    @Value("${spring.openai.token}")
    private String apiKey;

    private final ObjectMapper objectMapper;
    private final RestClient client;
    private final Validator validator;


    public AiClientService(RestClient openAiClient , ObjectMapper objectMapper, Validator validator) {
        this.validator = validator;
        this.client = openAiClient;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    private void postConstruct(){
        if (apiKey.isBlank() || apiKey == null){
            throw new IllegalStateException("CRITICAL: API KEY is missing");
        }
    }



    public AiResponseDTO generateAIResponse(WeatherResponse weatherServiceResponseDTO)  {
      int retries = 3;
      long delay = 1000;

        Map<String,Object> requestBody = Map.of(
                "model", "gpt-4o",
                "temperature",0.1,
                "messages", List.of(
                        Map.of("role","user","content",systemPrompt()),
                        Map.of("role","user","content",objectMapper.writeValueAsString(weatherServiceResponseDTO))
                )
        );

        for(int i = 0; i<retries;i++){

            int attempt = i+1;
            System.out.println("Attempt# " +  attempt);
            try{
                ResponseEntity<String> response = client.post()
                        .uri("https://api.openai.com/v1/chat/completions")
                        .header("Authorization","Bearer "+apiKey)
                        .header("Content-Type","application/json")
                        .body(requestBody)
                        .retrieve()
                        .toEntity(String.class);

                if(response.getStatusCode().is2xxSuccessful()){
                    JsonNode root = objectMapper.readTree(response.getBody());
                    return parseToDTO(root.path("choices").get(0).path("message").path("content").asString());
                }
                if(response.getStatusCode().isSameCodeAs(HttpStatus.TOO_MANY_REQUESTS)){
                    log.atError().log("To many Requests");
                    Thread.sleep((long) (Math.pow(2,i)*delay));
                    continue;
                }
                if(response.getStatusCode().is5xxServerError()){
                    log.atError().log("5XX error detected.");
                    Thread.sleep((long) (Math.pow(2,i)*delay));
                    continue;
                }
                if(response.getStatusCode().is4xxClientError() && !response.getStatusCode().isSameCodeAs(HttpStatus.TOO_MANY_REQUESTS)){
                    log.atError().log("Response code is {} and indicate unretryable error",response.getStatusCode());
                    throw new IllegalStateException("Response was 4xx error");
                }
            }catch (ResourceAccessException ex){
                log.atError().log("Network exception: API failed to respond",ex);
            }catch(JacksonException ex) {
                log.atError().log("Jackson Could not map the output,  Likely hallucination or malformed JSON");
                return fallback();
            }
            catch (Exception e) {
                log.atError().log();
                throw new RuntimeException("Integration error", e);
            }
        }

       throw new RuntimeException("Retries Expended");
    }



    private AiResponseDTO parseToDTO(String parseData){
        try{
            AiResponseDTO response = objectMapper.readValue(parseData, AiResponseDTO.class);

            Set<ConstraintViolation<AiResponseDTO>> violations = validator.validate(response);
            if(!violations.isEmpty()){
                return fallback().toBuilder().summary("Ai violated constraints, assuming fallback").build();
            }

            if(response.confidence()<0.6){

                return fallback().toBuilder()
                        .summary("Could not establish confidence in prediction, giving indoor fallback")
                        .confidence(response.confidence())
                        .build();
            }

            return response.toBuilder().AI_Available(true).build();
        }catch (DatabindException ex){
            return fallback().toBuilder().summary("Failed to Bind response Data").build();
        }
    }

    public static AiResponseDTO fallback(){
        return AiResponseDTO.builder()
                .summary("AI Weather Summary unavailable")
                .confidence(1.0)
                .AI_Available(false)
                .recommendations(List.of("entertainment","commercial","catering"))
                .build();
    }

    private String systemPrompt() {
        return """
                You are a weather summarization service,  Your task is to convert weather data into a WeatherSummaryDTO.
                This DTO has the following Schema
                
                {
                    "summary":"string",
                    "confidence":"float",
                    "recommendations": ["string"]
                }
                
                Summary: A concise maximum 50 character sentence describing weather based of the weather code, temperature and precipation.
                Confidence: An estimation of your confidence in your answer
                Recommendations: return 1-5 recommendations from the list of viable categories
                
                Rules:
                -Return valid JSON format only
                -Do not include markdown
                -Do not include conversional text
                -You may not include reasoning output any text outside of the specified JSON
                -The JSON must match the schema
                -Reccomendations may only be from the following list
                -Only use information present in the given query
                -Do not invent weather warnings
                -Do not use information other than the DTO you are given.
                -If information is missing, make the best summary out of available fields
                -Reccomendations must exactly match the list of allowed options.
              
                -Your list of viable activity categories come from GeoApifys categories and are;
                    activity
                    commercial
                    catering
                    entertainment
                    heritage
                    leisure
                    natural
                    tourism
                    camping
                    beach
                    sport
                Attempt to make the best recommendation from the weather data based on wether the category describes outdoor or indoor activities
                """;
    }


}
