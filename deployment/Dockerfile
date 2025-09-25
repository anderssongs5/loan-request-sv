FROM gradle:8-jdk21 AS builder

WORKDIR /app

COPY gradlew .
COPY gradlew.bat .
COPY gradle/ gradle/
COPY build.gradle .
COPY settings.gradle .
COPY main.gradle .
COPY gradle.properties .
COPY lombok.config .
COPY applications/ applications/
COPY domain/ domain/
COPY infrastructure/ infrastructure/

RUN ./gradlew clean bootJar -x test -x validateStructure

FROM openjdk:21-jdk-slim

# Create user, install packages, and setup permissions in single layer
RUN groupadd -r appuser && useradd -r -g appuser appuser && \
    apt-get update && apt-get install -y curl wget && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY --from=builder /app/applications/app-service/build/libs/loan-request-sv.jar app.jar

USER appuser

ENV SERVER_PORT=8081

EXPOSE ${SERVER_PORT}

ENTRYPOINT ["sh", "-c", "java -jar app.jar"]
