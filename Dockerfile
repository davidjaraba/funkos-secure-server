FROM gradle:jdk17-alpine AS build

WORKDIR /app

COPY build.gradle.kts .
COPY gradlew .
COPY gradle gradle
COPY src src
COPY data data

RUN gradle wrapper --gradle-version 8.4

RUN ./gradlew build


FROM openjdk:17-alpine

WORKDIR /app

COPY cert cert
COPY data data

COPY --from=build /app/build/libs/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]