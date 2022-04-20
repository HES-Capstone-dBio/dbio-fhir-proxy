FROM openjdk:17-alpine
ENV JWT_SIGNING_KEY /signing.pem
ADD target/dbio-fhir-proxy-*SNAPSHOT.jar /app.jar
ADD signing.pem /
ENTRYPOINT ["java", "-jar", "/app.jar"]
