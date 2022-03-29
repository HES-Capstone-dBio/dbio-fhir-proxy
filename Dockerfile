FROM openjdk:17-alpine
COPY /home/runner/work/dBio-fhir-proxy/dBio-fhir-proxy/target/dbio-fhir-proxy-*.jar /app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
