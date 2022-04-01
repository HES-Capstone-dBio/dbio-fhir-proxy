FROM openjdk:17-alpine
ADD target/dbio-fhir-proxy-*with-dependencies.jar /app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
