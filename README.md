## Development Status Beta

⚠️ This project is currently in beta.
It is under active development and may change frequently. Features may be incomplete or unstable.
Feedback and contributions are welcome as we work toward a stable release. ⚠️

This project is currently in its alpha stage. It is actively being developed, and we are in the process of conducting thorough quality assurance testing. Therefore, expect potential instability and incomplete features.

# HIVEconnect

The purpose of HIVEconnect  application is to act as a broker between an HL7 FHIR client and an openEHR server.

The implementation is based on [Apache Camel](https://camel.apache.org/) and [Open eHealth Integration Platform](https://github.com/oehf/ipf).

## Getting Started

### Prerequisites

* JDK (>= 17)
* Apache Maven (>= 3.6.0)
* HAPI FHIR Server (>= v7.4.2) (or similiar FHIR platform)
* OpenFHIR (>= v0.0.1) 
* EHRbase (>= v2.0.0) (or similiar openEHR platform)


### Build the application

```shell script
$ mvn clean install
```

### Build the application and execute integration tests

```shell
$ mvn clean install -DskipITs=false
```

:warning: When using `-DskipITs=false` option, please make sure you have an EHRbase instance up and running. The easiest way to achieve that is to use one of the provided docker-compose files in the **docker** folder:

```shell script
# Start up an EHRbase instance
cd docker
docker-compose -f docker-compose-light.yml up
```
Note: Ensure that the OPENFHIR application is up and running before starting HIVEconnect
### Run the application

```bash
$ java -jar fhir-bridge-1.0.0-SNAPSHOT.jar
```

## Docker and Docker Compose

### Build the Docker image

```
$ docker build -f docker/Dockerfile -t fhir-bridge:latest . 
```

### Start a Docker container
Note: Ensure all containers are created using same network
```bash
$ docker run --network=docker_ehrbase-network -p 8888:8888 -e \
                  "FHIR_BRIDGE_OPENEHR_URL=http://{ehrbase-container-name or host}:8080/ehrbase/" \
                  -e "SPRING_DATASOURCE_URL=jdbc:postgresql://{database-container-name or host}:5432/fbridge" \
                  -e "SPRING_DATASOURCE_USERNAME=postgres" -e "SPRING_DATASOURCE_PASSWORD=postgres" \
                  -e "SERVERURL=http://{hapi-fhir-server-container-name or host}:8080/fhir" \
                  -e "OPENFHIR_SERVER_URL=http://{openfhir-container-name or host}:8090" \
                  --name=fhir-bridge fhir-bridge:latest
```

## License

Copyright 2025 HiGHmed

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
