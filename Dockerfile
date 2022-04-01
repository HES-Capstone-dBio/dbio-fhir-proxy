FROM openjdk:17-alpine

RUN \
  ls /home/runner/.m2/repository/org/dbio/dbio-fhir-proxy/1.0-SNAPSHOT/dbio-fhir-proxy-*-SNAPSHOT.jar \
  | head -1 \
  | xargs -i cp {} /app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
