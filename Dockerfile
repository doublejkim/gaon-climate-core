FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

COPY gradle gradle
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY src src

RUN ./gradlew bootJar --no-daemon && \
    cp "$(find build/libs -name '*.jar' ! -name '*plain.jar' | head -n 1)" app.jar

FROM eclipse-temurin:21-jre
WORKDIR /app

ENV TZ=Asia/Seoul

COPY --from=build /app/app.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
