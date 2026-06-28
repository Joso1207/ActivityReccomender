#  Activity Recommender (AI Weather Service)

This service generates activity recommendations based on real-world weather data and AI reasoning.  
It combines **Open-Meteo weather data**, **Geoapify activity categories**, and an **AI model (OpenAI-compatible API)** to produce structured recommendations.

In order to run the project you need to supply your own API-KEYS from openAI and GeoApify.

Assign them to Vars

#### geoKey
#### openAiToken


---

#  Tech Stack

##  Core
- Java 21
- Spring Boot 4.0.6
- Gradle (Groovy DSL)

---

##  Spring Modules Used

- **spring-boot-starter-webflux**
  - Used for reactive HTTP communication (AI API + external services)

- **spring-boot-starter-validation**
  - Jakarta Bean Validation for validating AI-generated DTOs
 
- **spring-cloud-starter-circuitbreaker-reactor-resilience4j**
- **spring-cloud-starter-circuitbreaker-spring-retry**
  - CircuitBreaker pattern with Retry for Webclient (RestClient uses manual Retry)


##  Libraries

- Lombok (boilerplate reduction)

---

#  External Data Sources

##  Open-Meteo (Weather Data Provider)

This application uses **Open-Meteo** to retrieve weather data.

- No API key required
- Provides:
  - temperature
  - precipitation
  - weather codes
  - forecast data

🔗 https://open-meteo.com/

Used as the **weather data source** used in AI processing.

---

##  Geoapify (Activity Categories & Location Enrichment)

Geoapify is used to map weather conditions to activity categories such as:

- beach
- tourism
- leisure
- sport
- camping
- commercial
- entertainment

🔗 https://www.geoapify.com/

---

##  OpenAI API

Used to generate structured natural language summaries and recommendations.

Expected output format:

```json
{
  "summary": "Short weather description",
  "confidence": 0.0,
  "recommendations": ["beach", "tourism"]
}
