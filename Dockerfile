FROM openjdk:8-buster
ARG THIRD_PARTY_ETH_ADDRESS
ARG THIRD_PARTY_PRIVATE_KEY
ARG THIRD_PARTY_EMAIL
ENV THIRD_PARTY_ETH_ADDRESS $THIRD_PARTY_ETH_ADDRESS
ENV THIRD_PARTY_PRIVATE_KEY $THIRD_PARTY_PRIVATE_KEY
ENV THIRD_PARTY_EMAIL $THIRD_PARTY_EMAIL
ENV JWT_SIGNING_KEY /signing.pem
ADD target/dbio-fhir-proxy-*SNAPSHOT.jar /app.jar
ADD signing.pem /
COPY protocol-client/bin/ /libs
ENTRYPOINT ["java", "-Djava.library.path=/libs", "-jar", "/app.jar"]
