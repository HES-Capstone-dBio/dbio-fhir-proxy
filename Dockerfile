FROM openjdk:17-alpine
ARG PROFILE
ENV PROFILE_VAR=$PROFILE
VOLUME /tmp
## Add the built jar for docker image building
ADD target/dbio-fhir-proxy-1.0-SNAPSHOT.jar /dbio-fhir-proxy-1.0-SNAPSHOT.jar
ENTRYPOINT ["java", "-jar", "/dbio-fhir-proxy-1.0-SNAPSHOT.jar"]
## DO NOT USE(The variable would not be substituted): ENTRYPOINT ["java","-Dspring.profiles.active=$PROFILE_VAR","-jar","/hello-world-docker-action.jar"]
## CAN ALSO USE: ENTRYPOINT java -Dspring.profiles.active=$PROFILE_VAR -jar /hello-world-docker-action.jar
EXPOSE 80