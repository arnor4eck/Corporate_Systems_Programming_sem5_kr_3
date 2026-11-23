FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /workspace
COPY gradlew gradlew.bat settings.gradle build.gradle ./
COPY gradle ./gradle
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew
RUN --mount=type=cache,id=pks-3-gradle,target=/root/.gradle,sharing=locked \
    ./gradlew dependencies --configuration runtimeClasspath --no-daemon --info --stacktrace
COPY src ./src
RUN --mount=type=cache,id=pks-3-gradle,target=/root/.gradle,sharing=locked \
    ./gradlew bootJar --no-daemon --stacktrace

FROM eclipse-temurin:21-jre-jammy
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/* && useradd --system --uid 10001 app
WORKDIR /app
COPY --from=build /workspace/build/libs/*-SNAPSHOT.jar /app/app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
