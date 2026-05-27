# Levanto una imagen con gradle y jdk21
FROM gradle:8.7-jdk21 AS build

WORKDIR /app

# Genero dependencias desde gradle
COPY build.gradle.kts settings.gradle.kts ./
COPY gradle ./gradle
RUN gradle dependencies --no-daemon || true

COPY src ./src
RUN gradle bootJar --no-daemon -x test

# Run
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Utilidades mínimas para healthchecks (compose)
RUN apk add --no-cache wget

# Usuario
RUN addgroup -S spring && adduser -S spring -G spring
USER spring

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
