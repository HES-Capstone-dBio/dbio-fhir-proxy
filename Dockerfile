FROM openjdk:17-alpine
ADD target/dbio-fhir-proxy-*.jar /app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
