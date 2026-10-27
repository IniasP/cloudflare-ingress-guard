FROM eclipse-temurin:25@sha256:f5b4ef3a13414837e75a0d124b28e7263690df23a780749880407768a6d42618

ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar

ENTRYPOINT ["java", "-jar", "/app.jar"]