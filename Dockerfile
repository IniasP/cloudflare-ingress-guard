FROM eclipse-temurin:25@sha256:acab08ae09273ee938c1da6111ed60ff51ab0ab18325e4b1b81178039059f86e

ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar

ENTRYPOINT ["java", "-jar", "/app.jar"]