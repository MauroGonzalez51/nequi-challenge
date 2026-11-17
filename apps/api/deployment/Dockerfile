# Build context: apps/api  ->  docker build -f deployment/Dockerfile apps/api
FROM eclipse-temurin:17-jdk-alpine AS build

WORKDIR /workspace

COPY gradlew settings.gradle build.gradle gradle.properties lombok.config ./

COPY gradle gradle

RUN ./gradlew --version --no-daemon

COPY domain domain
COPY infrastructure infrastructure
COPY applications applications

RUN ./gradlew bootJar -x test --no-daemon

FROM eclipse-temurin:17-jre-alpine

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

COPY --from=build --chown=appuser:appgroup /workspace/applications/app-service/build/libs/api.jar api.jar

ENV JAVA_OPTS="-XX:MaxRAMPercentage=70 -XX:+ExitOnOutOfMemoryError -XX:+HeapDumpOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom"

USER appuser

EXPOSE 8080

ENTRYPOINT ["/bin/sh", "-c", "exec java $JAVA_OPTS -jar /app/api.jar"]
