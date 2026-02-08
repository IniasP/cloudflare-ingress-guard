FROM eclipse-temurin:25@sha256:5b691907413c1c67b1f2402a39c64736d02404e703aa6f688164be23f83f06c4

ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar

ENTRYPOINT ["java", "-jar", "/app.jar"]