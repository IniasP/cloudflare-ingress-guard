FROM eclipse-temurin:25@sha256:317010528a5eafaa13a1ea9fa194fae3959c57646306972391bc785cd3acd91b

ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar

ENTRYPOINT ["java", "-jar", "/app.jar"]