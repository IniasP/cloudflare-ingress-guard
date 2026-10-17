FROM eclipse-temurin:25@sha256:40cefbf1de9d958594a1a38caafdd87e09bb8935b72ae3688ffe578f4f92767a

ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar

ENTRYPOINT ["java", "-jar", "/app.jar"]