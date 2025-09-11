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

RUN groupadd -r appuser && useradd -r -g appuser appuser

WORKDIR /app

COPY --from=builder /app/applications/app-service/build/libs/loan-request-sv.jar app.jar

RUN chown appuser:appuser app.jar

USER appuser

ENV SERVER_PORT=8081

EXPOSE ${SERVER_PORT}

ENTRYPOINT ["sh", "-c", "java -jar app.jar \
    --server.port=${SERVER_PORT} \
    --adapters.r2dbc.host=${DB_HOST} \
    --adapters.r2dbc.port=${DB_PORT} \
    --adapters.r2dbc.database=${DB_DATABASE} \
    --adapters.r2dbc.schema=${DB_SCHEMA} \
    --adapters.r2dbc.username=${DB_USERNAME} \
    --adapters.r2dbc.password=${DB_PASSWORD} \
    --adapters.restconsumer.url=${REST_CONSUMER_BASE_URL} \
    --security.introspectionBaseUrl=${SECURITY_INTROSPECTION_BASE_URL}"]
