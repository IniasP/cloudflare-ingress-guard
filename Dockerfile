FROM eclipse-temurin:25@sha256:8b7be4d361d7aa906eb8767f08941dc861d7148d3d99e81b3e9b53e7e1b9c809

ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar

ENTRYPOINT ["java", "-jar", "/app.jar"]