FROM eclipse-temurin:25@sha256:12e44624adee6808a36d962717e1656e0afeeeff5a100f9cb00e0136513558f0

ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar

ENTRYPOINT ["java", "-jar", "/app.jar"]