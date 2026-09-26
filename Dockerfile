FROM eclipse-temurin:25@sha256:119a3d18f160a3e7655a66034d0f43beee31cd7b3b9142d57a5de29772011de6

ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar

ENTRYPOINT ["java", "-jar", "/app.jar"]