FROM openjdk:11-jdk-slim as builder
USER root
WORKDIR /builder
ADD . /builder
RUN ./gradlew build --info --stacktrace

FROM openjdk:11-jre-slim
WORKDIR /app
EXPOSE 8080
COPY --from=builder /builder/build/libs/captcha.jar .
CMD ["java", "-jar", "captcha.jar"]
