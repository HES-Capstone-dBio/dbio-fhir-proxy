FROM openjdk:17-alpine

COPY $HOME/app.jar /app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
