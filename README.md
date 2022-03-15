# FHIR proxy server for the dBio system.

This project is the FHIR Proxy Server service of the dBio system.

## Running Locally

There are two primary ways to run this. Either from the command line with maven or within a IDE such as Intellij. This project uses Java 17. If you do not have Java 17 installed you can [download it here](https://www.oracle.com/java/technologies/downloads/)

### Using Spring Boot with :run

First make sure you have [maven](https://maven.apache.org/install.html) installed on your system.

From within the project directory execute the following from the command line:
```bash
mvn clean spring-boot:run -Pboot
```

### Using Intellij IDE


Open the project with the Intellij IDE. Go to File->Project Structure and set the both the SDK and Language level to 17 in the projects tab.

![project-version](/readme-images/project-version.png)


In this same window click on the "modules" tab. Set the language level to 17. Click Apply and then Close.

![module-version](/readme-images/module-version.png)

Next look for the "Add Configurations" button close to the top right of the IDE. Click it to bring up run configurations.

![add-configurations](/readme-images/add-configurations.png)

Click the "+" symbol at the top left of this new screen and select "Spring Boot". Configure it as seen in the image below.

![run-debug-menu](/readme-images/run-debug-menu.png)

Then click "Apply" and "Ok". Your new configuration is now done and you can run it by clicking the run button in the IDE.


## Testing

The server will then be accessible at http://localhost:8080/. To get the server's metadata send a GET request to http://localhost:8080/fhir/metadata. A good way to test this is by using [Postman](https://www.postman.com/downloads/). No, you can't use your browser at this time.

Currently there is test code to retrieve the information of a hardcoded patient. By sending a GET request to the following endpoint:

```
http://localhost:8080/fhir/Patient/1
```
