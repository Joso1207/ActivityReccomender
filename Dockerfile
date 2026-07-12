# -----------------------------
# Build stage
# Use a JDK image to compile
FROM eclipse-temurin:21-jdk AS builder

# Set the working directory of the container
WORKDIR /application

# Copy the Gradle wrapper and build configuration first.
# This allows Docker to cache dependency resolution when only source code changes.
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./

# Make the Gradle wrapper executable.
RUN chmod +x gradlew

# Download and cache project dependencies.
RUN ./gradlew dependencies --no-daemon

# Copy the application source code to the image
COPY src src

# Build the executable Spring Boot JAR.
RUN ./gradlew bootJar --no-daemon

# -----------------------------
# Runtime stage

# Use a JRE image since this is what we use to run the app
FROM eclipse-temurin:21-jre

# Set the working directory.
WORKDIR /application

# Copy only the built JAR files from the builder stage.
# This keeps the final image small by excluding source code and build tools.
COPY --from=builder /application/build/libs/*.jar application.jar

# Document that the application listens on port 8080.
EXPOSE 8080

# Start the Spring Boot application.
ENTRYPOINT ["java", "-jar", "application.jar"]