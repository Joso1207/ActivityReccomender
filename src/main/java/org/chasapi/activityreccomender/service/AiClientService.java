package org.chasapi.activityreccomender.service;

import jakarta.annotation.PostConstruct;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.chasapi.activityreccomender.dto.AiResponseDTO;
import org.chasapi.activityreccomender.dto.weather.WeatherResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.DatabindException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class AiClientService {

    private final ObjectMapper objectMapper;
    @Value("${spring.openai.token}")
    private String apiKey;

    private final RestClient client;
    private final Validator validator;

    public AiClientService(ObjectMapper objectMapper, Validator validator) {
        this.validator = validator;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();

        requestFactory.setConnectTimeout(Duration.ofSeconds(2));
        requestFactory.setReadTimeout(Duration.ofSeconds(8));

        this.client = RestClient.builder().requestFactory(requestFactory).build();
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    private void postConstruct(){
        if (apiKey.isBlank() || apiKey == null){
            throw new IllegalStateException("CRITICAL: API KEY is missing");
        }
    }

    public AiResponseDTO generateAIResponse(WeatherResponse weatherServiceResponseDTO){
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
            System.out.println("OPENAI KEY: " + apiKey);
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
            }catch (ResourceAccessException ex){
                throw new RuntimeException("Network exception: API failed to respond",ex);
            } catch (Exception e) {
                throw new RuntimeException("Integration error", e);
            }
        }

       throw new RuntimeException("Unreachable statement, implement graceful backoff");
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

            return response;
        }catch (DatabindException ex){
            return fallback();
        }
    }

    public AiResponseDTO fallback(){
        return AiResponseDTO.builder()
                .summary("AI Weather Summary unavailable")
                .confidence(1.0)
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
