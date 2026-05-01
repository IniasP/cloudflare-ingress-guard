FROM eclipse-temurin:25@sha256:c3a5cfd77c9a43dd95269a266290d365b79b174381d8336a3f76a7ae117beefa

ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar

ENTRYPOINT ["java", "-jar", "/app.jar"]