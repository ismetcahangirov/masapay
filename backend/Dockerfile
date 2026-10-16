# Backend image: build the Spring Boot jar with the Gradle wrapper, run it on a JRE.

# --- build stage -----------------------------------------------------------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Wrapper and build scripts first, so dependency downloads are cached between
# builds that only change source code.
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon -q > /dev/null

COPY src ./src
RUN ./gradlew bootJar --no-daemon -x test

# --- run stage -------------------------------------------------------------
FROM eclipse-temurin:21-jre
WORKDIR /app

# Run as an unprivileged user rather than root.
RUN groupadd --system app && useradd --system --gid app app
USER app

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
