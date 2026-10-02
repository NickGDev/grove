# Grove — prod-shaped container image (deployed by docker-compose.yml).
# Stage 1 builds the boot jar with Gradle 9 / JDK 25; stage 2 runs it on a
# JRE-only image, with the jar's layers extracted so dependency layers cache
# independently of application code.
FROM gradle:9-jdk25 AS build
WORKDIR /build
COPY gradlew ./
COPY gradle/ gradle/
COPY build.gradle.kts settings.gradle.kts ./
# Warm the dependency cache; failures fall through to the real build below.
RUN ./gradlew --no-daemon dependencies --quiet > /dev/null 2>&1 || true
COPY src/ src/
RUN ./gradlew --no-daemon bootJar -x test
RUN cp build/libs/grove-*.jar application.jar \
    && java -Djarmode=tools -jar application.jar extract --layers --destination extracted

FROM eclipse-temurin:25-jre
WORKDIR /application
RUN useradd --system --home /application grove
COPY --from=build /build/extracted/dependencies/ ./
COPY --from=build /build/extracted/spring-boot-loader/ ./
COPY --from=build /build/extracted/snapshot-dependencies/ ./
COPY --from=build /build/extracted/application/ ./
USER grove
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0"
ENTRYPOINT ["java", "-jar", "application.jar"]
