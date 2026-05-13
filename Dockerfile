FROM eclipse-temurin:25@sha256:c2b7ea21649875fb9052237ac4e3cd4ef63968a2a389a0a1b1a72a5e53e5c93f

ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar

ENTRYPOINT ["java", "-jar", "/app.jar"]