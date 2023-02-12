# Laelith VTT application
VTT application for the Laelith meta-verse project.

## Dependency
JDK 17

## setup for local development
Add vtt.test.laelith.com to your hosts file:
``` bash
echo "::1 vtt.test.laelith.com" | sudo tee -a /etc/hosts
```
## Run the server locally
``` bash
./gradlew bootRun
```
## Build a docker image
``` bash
./gradlew bootBuildImage
```
### Run the docker image
Run on port 8081
``` bash
docker run -p 8081:8080 -it vtt:0.1.0-SNAPSHOT
```
