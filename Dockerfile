FROM openjdk:17-alpine
RUN echo "listing targets" && ls /home/runner/work/dBio-fhir-proxy/dBio-fhir-proxy/target
RUN \
  ls /home/runner/work/dBio-fhir-proxy/dBio-fhir-proxy/target/dbio-fhir-proxy-*.jar \
  | head -1 \
  | xargs -i cp {} /app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
