FROM eclipse-temurin:25@sha256:dd23c917b42d5ba34b726c3b339ba0f71fac76a8bdebb936b511bb98832dc287

ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar

ENTRYPOINT ["java", "-jar", "/app.jar"]