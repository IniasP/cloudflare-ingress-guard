FROM eclipse-temurin:25@sha256:c3b86e3867140345626eba1927a29e2df58c8619ec1c9b0951c0fdd7df97a145

ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar

ENTRYPOINT ["java", "-jar", "/app.jar"]