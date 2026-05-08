FROM eclipse-temurin:25@sha256:e23592541431eaeef5c13c84c21db71f97cdca0e70181ea6222ec9bccac24f6c

ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar

ENTRYPOINT ["java", "-jar", "/app.jar"]