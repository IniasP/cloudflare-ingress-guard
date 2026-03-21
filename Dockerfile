FROM eclipse-temurin:25@sha256:bee2e23ab444ed60daf8123e36478bc4a286ba7835bec6f9daf9eba1d50a86a2

ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar

ENTRYPOINT ["java", "-jar", "/app.jar"]