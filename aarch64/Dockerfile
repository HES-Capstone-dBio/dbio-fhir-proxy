FROM ssheldharv/dbio-fhir-proxy:latest
RUN rm /libs/libironoxide_java.so
RUN curl -L https://drive.google.com/file/d/1nch0wSRvwdNNKgFZs4K_J9bnNfJZjFYK -o /libs/libironoxide_java.so
ENTRYPOINT ["java", "-Djava.library.path=/libs", "-jar", "/app.jar"]
