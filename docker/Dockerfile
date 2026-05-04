FROM eclipse-temurin:21-jdk AS build

ARG REPOSITORY_URL=https://github.com/doublejkim/gaon-climate-core.git
ARG GIT_REF=main

WORKDIR /workspace

RUN apt-get update && \
    apt-get install -y --no-install-recommends git ca-certificates && \
    rm -rf /var/lib/apt/lists/*

RUN git init source && \
    cd source && \
    git remote add origin "${REPOSITORY_URL}" && \
    git fetch --depth 1 origin "${GIT_REF}" && \
    git checkout --detach FETCH_HEAD

WORKDIR /workspace/source

RUN chmod +x ./gradlew && \
    ./gradlew bootJar --no-daemon && \
    cp "$(find build/libs -name '*.jar' ! -name '*plain.jar' | head -n 1)" app.jar

FROM eclipse-temurin:21-jre
WORKDIR /app

ENV TZ=Asia/Seoul

COPY --from=build /workspace/source/app.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
