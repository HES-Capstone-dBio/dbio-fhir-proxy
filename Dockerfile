FROM openjdk:17-alpine
ARG FHIR_PROXY_SIGNING_PEM
ENV JWT_SIGNING_KEY /signing.pem
ADD target/dbio-fhir-proxy-*SNAPSHOT.jar /app.jar
RUN echo $FHIR_PROXY_SIGNING_PEM > /signing.pem
ENTRYPOINT ["java", "-jar", "/app.jar"]
