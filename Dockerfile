# syntax = docker/dockerfile:1.2
FROM openjdk:11-jdk as builder
USER root
WORKDIR /builder
ADD . /builder
RUN ./gradlew build --info --stacktrace -Pruns-in-docker

FROM openjdk:11-jre-slim
WORKDIR /app
EXPOSE 8080
COPY --from=builder /builder/build/libs/captcha.jar .
CMD ["java", "-jar", "captcha.jar"]
