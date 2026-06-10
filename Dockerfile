FROM eclipse-temurin:25@sha256:edb3aa0f621796d8f5f9d602c7611ffdf015cd89e6ddda1894d85a3a99d170a8

ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar

ENTRYPOINT ["java", "-jar", "/app.jar"]