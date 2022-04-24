FROM openjdk:8-buster
ENV JWT_SIGNING_KEY /signing.pem
ADD target/dbio-fhir-proxy-*SNAPSHOT.jar /app.jar
ADD signing.pem /
COPY protocol-client/bin/ /libs
ENTRYPOINT ["java", "-Djava.library.path=/libs", "-jar", "/app.jar"]
