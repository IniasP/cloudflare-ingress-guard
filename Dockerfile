FROM eclipse-temurin:25@sha256:bd02aad397764a2d2ea35351655d72db47dcc4b1644f65289b10946878df4623

ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar

ENTRYPOINT ["java", "-jar", "/app.jar"]