HIVEconnect - Deployment
================================

Introduction
------------

The HIVEconnect is a broker between an HL7 FHIR client and an openEHR server. It helps to transform data in the FHIR format to openEHR compositions. It implements FHIR endpoints based on the HAPI FHIR implementation, and transforms data from FHIR to openEHR, stores the FHIR resource in HAPI server and openEHR composition in ehrBase.

This document outlines the setup of HIVEconnect Server and the related servers like openFHIR, Hapi FHIR server and openEHR EhrBase..

HIVEconnect Solution
--------------------

HIVEconnect is the System exposes REST apis to create/update FHIR resources. These APIs accept FHIR resources/Bundles, transform the data to openEHR compositions using openFHIR. This is then persisted into openEHR CDR.

![](/docs/images/hiveconnect.png)

Following are the steps to be followed to deploy HIVEconnect server:

1) openFHIR: Deploy and configure openFHIR.
2) Deploy HAPI FHIR Server and Ehrbase.
3) Deploy HIVEconnect

openFHIR Deployment
-----------------------

openFHIR is an engine that implements [FHIR Connect specification](https://github.com/better-care/fhir-connect-mapping-spec) and facilitates bidirectional mappings between openEHR and FHIR.

Documentation: <https://open-fhir.com/documentation/index.html>

### OpenFHIR Database

At the moment, mongodb and postgres are supported by openfhir. By default, mongodb will be used. To change this, configure "db.type" property.

##### MongoDB configuration

| **Variable**               | **Description**            | **Default Value**                                      |
|---------------------------|----------------------------|--------------------------------------------------------|
| SPRING_DATA_MONGODB_URI   | Database connection URL    | mongodb://openfhir:openfhir@mongodb:27017/openfhir     |
| DB_TYPE                   | DB used                    | mongo                                                  |


##### Postgres configuration

| **Variable**                  | **Description**            | **Default Value**                                      |
|------------------------------|----------------------------|--------------------------------------------------------|
| SPRING_DATASOURCE_URL        | Database connection URL    | jdbc:postgresql://ehrbase-db:5432/openfhir             |
| SPRING_DATASOURCE_USERNAME   | Database username          | postgres                                               |
| SPRING_DATASOURCE_PASSWORD   | Database password          | postgres                                               |
| DB_TYPE                      | DB used                    | postgres                                               |


### OpenFHIR Docker

openFHIR can be easily installed using Docker. The official Docker image is available at ghcr.io/medblocks/openfhir:main.

#### DockerFile

```Dockerfile
# java build image, java 17
FROM maven:3.8.3-openjdk-17-slim AS build
WORKDIR /app
COPY .. .
# build without tests
RUN mvn clean package -DskipTests

# Path: Dockerfile
# java runtime image, java 17
FROM openjdk:17-slim
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

RUN mkdir -p /app/bootstrap
COPY --from=build /app/src/test/resources/kds_new /app/bootstrap/
VOLUME /app/bootstrap

CMD ["java", "-jar", "app.jar"]

```

#### BUILD

```bash
docker build -t openfhir
```

#### RUN

Note: If database is hosted on docker please use host.docker.internal instead of localhost in windows

##### Postgres DB

```bash
docker run --network openfhir-net --name openfhir-postgres \
-e POSTGRES_USER=postgres \
-e POSTGRES_PASSWORD=postgres \
-d -p 15432:15432 postgres:latest
```

```bash
docker run --network openfhir-net --name openfhir \
-e SPRING_DATASOURCE_URL=jdbc:postgresql://openfhir-postgres:5432/openfhir \
-e SPRING_DATASOURCE_USERNAME=postgres \
-e SPRING_DATASOURCE_PASSWORD=postgres \
-e BOOTSTRAP_DIR=/app/bootstrap \
-d -p 8090:8090 openfhir:latest
```

### OpenFHIR Bootstrap

Copy all the fhir connect config files (context and module mapping yml files) from

```bash
/openFHIR/src/test/resources/kds_newto path//to//app//bootstrap/
```

Note: This is now taken care in the Docker file

```Dockerfile
RUN mkdir -p /app/bootstrap`
COPY --from=build /app/src/test/resources/kds_new /app/bootstrap/
VOLUME /app/bootstrap
```

#### Configuration

| **Variable**                          | **Description**                                                                 | **Default Value**                     |
|--------------------------------------|---------------------------------------------------------------------------------|---------------------------------------|
| BOOTSTRAP_DIR                        | Path where all FHIR Connect config files are available                         | path\\to\\app\\bootstrap\\            |
| BOOTSTRAP_RECURSIVELY-OPEN-DIRECTORIES | True: when bootstrap dir contains multiple subdirectories<br>False: when bootstrap dir is a single folder | true/false                            |

Installation of HAPI FHIR server and openEHR Ehrbase server
-----------------------------------------------------------

```bash
cd docker
docker-compose -f docker-compose-light.yml up
```

docker-compose-light.yml

```yaml
version: '3'
services:
  ehrbase:
    image: ehrbase/ehrbase:2.7.0
    ports:
      - 8080:8080
    networks:
      - ehrbase-network
    environment:
      DB_URL: jdbc:postgresql://ehrbase-db:5432/ehrbase
      DB_USER: ehrbase
      DB_PASS: ehrbase
      DB_USER_ADMIN: ehrbase
      DB_PASS_ADMIN: ehrbase
      SECURITY_AUTHTYPE: BASIC
      SECURITY_AUTHUSER: myuser
      SECURITY_AUTHPASSWORD: myPassword432
      SECURITY_AUTHADMINUSER: myadmin
      SECURITY_AUTHADMINPASSWORD: mySuperAwesomePassword123
      SYSTEM_NAME: local.ehrbase.org
      ADMIN_API_ACTIVE: 'true'
      SERVER_DISABLESTRICTVALIDATION: 'true'
    depends_on:
      - ehrbase-db
    restart: on-failure
  ehrbase-db:
    image: ehrbase/ehrbase-postgres:13.4
    ports:
      - 15432:5432
    networks:
      - ehrbase-network
    environment:
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
      EHRBASE_USER: ehrbase
      EHRBASE_PASSWORD: ehrbase
  hapi-fhir-server:
    image: hapiproject/hapi:latest
    container_name: hapi-fhir-server
    ports:
      - 8088:8080
    networks:
      - ehrbase-network
    volumes:
      - ./hive-connect/src/main/resources/application.yml:/etc/hapi-fhir/hapi-fhir-config.yaml
    environment:
#      HAPI_FHIR_VALIDATION_REQUESTS_ENABLED: 'true'
      HAPI_FHIR_SERVER_VALIDATION_FLAG_FAIL_ON_SEVERITY: error
      HAPI_FHIR_SERVER_VALIDATION_FLAG_ENFORCE_REQUIREMENT: 'true'
      HAPI_FHIR_CONFIG_PATH: /etc/hapi-fhir/hapi-fhir-config.yaml
networks:
  ehrbase-network: { }

```


HIVEconnect Setup and Installation
---------------------------------

HIVEconnect servers can be installed and run as docker.

### HIVEconnect Database

At this moment postgres database is supported

#### Configuration

| **Variable**                | **Description**            | **Default Value**                                      |
|----------------------------|----------------------------|--------------------------------------------------------|
| SPRING_DATASOURCE_URL      | Database connection URL    | jdbc:postgresql://ehrbase-db:5432/fbridge              |
| SPRING_DATASOURCE_USERNAME | Database username          | postgres                                               |
| SPRING_DATASOURCE_PASSWORD | Database password          | postgres                                               |

### OpenFHIR

Note: openFHIR service has to be deployed independently, installation is available above

#### Configuration

| **Variable**           | **Description**         | **Default Value**         |
|------------------------|-------------------------|---------------------------|
| OPENFHIR_SERVER_URL    | OpenFHIR server URL     | http://openfhir:8090      |


### OpenEhr

#### configuration

| **Variable**               | **Description**         | **Default Value**                  |
|---------------------------|-------------------------|------------------------------------|
| HIVE_CONNECT_OPENEHR_URL   | OpenEhr server URL      | http://ehrbase:8080/ehrbase/       |


#### Authorization

At the moment, Basic and Oauth2 authorization are supported. By default, Basic will be used. To change this, configure "HIVE_CONNECT_OPEHREHR_SECURITY_TYPE" property.

##### Basic

| **Variable**                                 | **Description**                 | **Default Value**    |
|---------------------------------------------|---------------------------------|------------------------|
| HIVE_CONNECT_OPEHREHR_SECURITY_TYPE          | OpenEHR service security type   | BASIC                  |
| HIVE_CONNECT_OPEHREHR_SECURITY_USER_NAME     | OpenEHR service username        | myuser                 |
| HIVE_CONNECT_OPEHREHR_SECURITY_USER_PASSWORD | OpenEHR service password        | myPassword432          |


##### OAuth2

| **Variable**                                 | **Description**                 | **Default Value**     |
|---------------------------------------------|---------------------------------|------------------------|
| HIVE_CONNECT_OPEHREHR_SECURITY_TYPE          | OpenEHR service security type   | OAUTH2                 |
| HIVE_CONNECT_OPEHREHR_TOKEN_URL              | OpenEHR service token URL       | Tokenurl               |
| HIVE_CONNECT_OPEHREHR_CLIENT_ID              | OpenEHR service client ID       | clientId               |
| HIVE_CONNECT_OPEHREHR_CLIENT_SECRET          | OpenEHR service client secret   | clientSecret           |

### FHIR Server

#### Configuration

| **Variable**   | **Description**     | **Default Value**                        |
|----------------|---------------------|------------------------------------------|
| SERVERURL      | FHIR server URL     | http://hapi-fhir-server:8080/fhir        |


### HIVEconnect Security

#### Authorization

| **Variable**                          | **Description**         | **Default Value**     |
|--------------------------------------|-------------------------|------------------------|
| HIVE_CONNECT_SECURITY_TYPE            | Security of HIVEconnect | BASIC                  |
| HIVE_CONNECT_SECURITY_USER_NAME       | Basic auth username     | myUser                 |
| HIVE_CONNECT_SECURITY_USER_PASSWORD   | Basic auth password     | secretPassword         |

### HIVEconnect Bootstrap

#### Configuration

| **Variable**                 | **Description**                                                        | **Default Value**           |
|-----------------------------|------------------------------------------------------------------------|-----------------------------|
| HIVE_CONNECT_BOOTSTRAP_DIR   | Path to directory where all openEHR templates (OPT format) are available | src/main/resources/opt      |


### HIVEconnect Docker

HIVEconnect can be easily installed using Docker. The official Docker image is available at [dockerhub/numforschungsdatenplattform/hive-connect](https://hub.docker.com/repository/docker/numforschungsdatenplattform/hive-connect/general)

Where v0.0.1 is the version of the release

#### Pull

```bash
docker pull numforschungsdatenplattform/hive-connect:0.1.0
```

#### Build

```bash
dockerbuild -f docker/Dockerfile -t hive-connect:latest
```

#### RUN

Note: please use "host.docker.internal" if any of openEHR server, fhir server and database are hosted in docker to that specific service/

To run Open-FHIR using Docker, use the following command:

##### BASICauthorization for openEHR server

```bash
docker run \
-v path\to\src\main\resources\opt:/app/src/main/resources/opt \
-e "HIVE_CONNECT_OPENEHR_URL=http://localhost:8080/ehrbase/" \
    -e "HIVE_CONNECT_OPEHREHR_SECURITY_TYPE=BASIC" \
    -e "HIVE_CONNECT_OPEHREHR_SECURITY_USER_NAME=myUSer" \
    -e "HIVE_CONNECT_OPEHREHR_SECURITY_USER_PASSWORD=myPassword432" \
    -e "SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/hiveconnect" \
    -e "SPRING_DATASOURCE_USERNAME=postgres" \
    -e "SPRING_DATASOURCE_PASSWORD=postgres" \
    -e "SERVERURL=http://localhost:8080/fhir" \
    -e "OPENFHIR_SERVER_URL=http://localhost:8090" \
    -e "HIVE_CONNECT_SECURITY_TYPE=BASIC" \
    -e "HIVE_CONNECT_SECURITY_USER_NAME=hiveconnect-user" \
    -e "HIVE_CONNECT_SECURITY_USER_PASSWORD=myPassword1234" \
    -e "HIVE_CONNECT_BOOTSTRAP_DIR=src/main/resources/opt" \
    -p 8888:8888  \
    --name=hive-connect numforschungsdatenplattform/hive-connect:0.1.0
```


### Testing HIVEconnect server

Test via postman

<https://drive.google.com/drive/folders/1VGLUR5Q7NAfcCrzD_MuRposjjTXfXkeE>

Env:

<https://drive.google.com/drive/folders/1VGLUR5Q7NAfcCrzD_MuRposjjTXfXkeE>

1.  Create Patient: This will create a Patient in the HAPI FHIR server and EHRID in ehrBase

```bash

POST {{fhir_base_url}}/Patient

{
    "resourceType": "Patient",
    "meta": {
        "profile": [
            "https://www.netzwerk-universitaetsmedizin.de/fhir/StructureDefinition/Patient"
        ]
    },
    "extension": [
        {
            "url": "https://www.netzwerk-universitaetsmedizin.de/fhir/StructureDefinition/ethnic-group",
            "valueCoding": {
                "system": "http://snomed.info/sct",
                "code": "186019001",
                "display": "Other ethnic, mixed origin"
            }
        },
        {
            "extension": [
                {
                    "url": "dateTimeOfDocumentation",
                    "valueDateTime": "2020-10-01"
                },
                {
                    "url": "age",
                    "valueAge": {
                        "value": 68,
                        "unit": "years",
                        "system": "http://unitsofmeasure.org",
                        "code": "a"
                    }
                }
            ],
            "url": "https://www.netzwerk-universitaetsmedizin.de/fhir/StructureDefinition/age"
        }
    ],
    "identifier": [
        {
            "system": "http://www.netzwerk-universitaetsmedizin.de/sid/crr-pseudonym",
            "value": "codex_6348b5"
        }
    ],
    "birthDate": "1952-09-30"
}
```


2.  Create KDS_Diagnose: Provide the PatientId returned in the previous request. {{patientId_fhir-server}}  
This will create the bundle in HPI server and a composition in ehrBase.

```bash
POST {{baseUrl}}/fhir/Bundle
{
  "resourceType": "Bundle",
  "id": "example-bundle",
  "type": "transaction",
  "entry": [
    {
      "fullUrl": "urn:uuid:fe7dd9ae-d1a1-425f-8572-7aef16f257ff",
      "resource": {
        "resourceType": "Condition",
        "id": "example-condition",
        "meta": {
          "source": "Hospital-ABC",
          "profile": [
            "https://www.medizininformatik-initiative.de/fhir/core/modul-diagnose/StructureDefinition/Diagnose"
          ]
        },
        "extension": [
          {
            "url": "http://hl7.org/fhir/StructureDefinition/condition-related",
            "valueReference": {
              "reference": "urn:uuid:fe7dd9ae-d1a1-425f-8572-7aef16f257fd",
              "type": "Condition",
              "identifier": {
                "type": {
                  "coding": [
                    {
                      "system": "http://terminology.hl7.org/CodeSystem/v2-0203",
                      "code": "MR",
                      "display": "Medical record number"
                    }
                  ],
                  "text": "Medical record number"
                },
                "system": "http://localhost:8888/hive-connect/fhir/identifiers/conditions",
                "value": "COND-123456",
                "period": {
                  "start": "1993-09-08",
                  "end": "1993-12-13"
                }
              },
              "display": "Related Condition"
            }
          },
          {
            "url": "http://hl7.org/fhir/StructureDefinition/condition-assertedDate",
            "valueDateTime": "2022-02-03T04:05:06Z"
          }
        ],
        "identifier": [
          {
            "system": "http://localhost:8888/hive-connect/fhir/identifiers/conditions",
            "value": "1234567890",
            "use": "official",
            "type": {
              "coding": [
                {
                  "system": "http://terminology.hl7.org/CodeSystem/v2-0203",
                  "code": "MR",
                  "display": "Medical record number"
                }
              ],
              "text": "Medical Record Number"
            },
            "assigner": {
              "display": "Example Hospital"
            },
            "period": {
              "start": "2023-01-01",
              "end": "2024-01-01"
            }
          }
        ],
        "clinicalStatus": {
          "coding": [
            {
              "system": "http://terminology.hl7.org/CodeSystem/condition-clinical",
              "code": "active",
              "display": "Active"
            }
          ],
          "text": "Active"
        },
        "verificationStatus": {
          "coding": [
            {
              "system": "http://terminology.hl7.org/CodeSystem/condition-ver-status",
              "code": "confirmed",
              "display": "Confirmed"
            }
          ],
          "text": "Confirmed"
        },
        "category": [
          {
            "coding": [
              {
                "system": "http://terminology.hl7.org/CodeSystem/condition-category",
                "code": "problem-list-item",
                "display": "Problem List Item"
              }
            ],
            "text": "Problem List Item"
          }
        ],
        "severity": {
          "coding": [
            {
              "system": "http://snomed.info/sct",
              "code": "24484000",
              "display": "Severe"
            }
          ],
          "text": "Severe"
        },
        "code": {
          "coding": [
            {
              "extension": [
                {
                  "url": "http://fhir.de/StructureDefinition/icd-10-gm-mehrfachcodierungs-kennzeichen",
                  "valueCoding": {
                    "system": "http://fhir.de/CodeSystem/icd-10-gm-mehrfachcodierungs-kennzeichen",
                    "code": "†",
                    "display": "†"
                  }
                },
                {
                  "url": "http://fhir.de/StructureDefinition/seitenlokalisation",
                  "valueCoding": {
                    "system": "https://fhir.kbv.de/CodeSystem/KBV_CS_SFHIR_ICD_SEITENLOKALISATION",
                    "code": "L",
                    "display": "links"
                  }
                },
                {
                  "url": "http://fhir.de/StructureDefinition/icd-10-gm-diagnosesicherheit",
                  "valueCoding": {
                    "system": "https://fhir.kbv.de/CodeSystem/KBV_CS_SFHIR_ICD_DIAGNOSESICHERHEIT",
                    "code": "G",
                    "display": "gesicherte Diagnose"
                  }
                }
              ],
              "system": "http://fhir.de/CodeSystem/bfarm/icd-10-gm",
              "code": "C34.1",
              "display": "Malignant neoplasm of the upper lobe, bronchus, or lung",
              "version": "2024"
            },
            {
              "system": "http://fhir.de/CodeSystem/bfarm/alpha-id",
              "code": "057E3",
              "display": "secondary malignant neoplasm of lymph nodes",
              "version": "2024"
            },
            {
              "system": "http://snomed.info/sct",
              "code": "128462008",
              "display": "Metastatic malignant neoplasm (disorder)",
              "version": "2024"
            },
            {
              "system": "http://www.orpha.net",
              "code": "1777",
              "display": "lung cancer associated with hereditary syndromes"
            },
            {
              "system": "http://terminology.hl7.org/CodeSystem/icd-o-3",
              "code": "C77.0",
              "display": "Malignant neoplasm of lymph nodes"
            }
          ],
          "text": "Metastatic malignant neoplasm (disorder)"
        },
        "bodySite": [
          {
            "coding": [
              {
                "system": "http://snomed.info/sct",
                "version": "20230131",
                "code": "321667001",
                "display": "Respiratory tract"
              },
              {
                "system": "http://terminology.hl7.org/CodeSystem/icd-o-3",
                "version": "2022",
                "code": "C34.1",
                "display": "Upper lobe, bronchus or lung"
              }
            ],
            "text": "Respiratory tract, Upper lobe, bronchus or lung"
          }
        ],
        "encounter": {
          "reference": "urn:uuid:a6ea47e6-f871-4a1d-a03a-6a8cd3d5909e",
          "display": "Inpatient Encounter for Asthma",
          "type": "Encounter",
          "identifier": {
            "system": "http://localhost:8888/hive-connect/fhir/identifiers/encounters",
            "value": "ENC123456",
            "use": "official",
            "type": {
              "coding": [
                {
                  "system": "http://terminology.hl7.org/CodeSystem/v2-0203",
                  "code": "VN",
                  "display": "Visit Number"
                }
              ],
              "text": "Visit Number"
            },
            "assigner": {
              "display": "Example Hospital"
            },
            "period": {
              "start": "2024-01-01T08:00:00Z",
              "end": "2024-01-01T10:00:00Z"
            }
          }
        },
        "subject": {
          "reference": "Patient/{{patientId_fhir-server}}"
        },
        "onsetDateTime": "2024-02-08T00:00:00Z",
        "recordedDate": "2022-02-03T00:00:00Z",
        "note": [
          {
            "text": "Patient confirmed for secondary malignant neoplasm of lymph node.",
            "time": "2024-02-09T12:00:00Z"
          }
        ]
      },
      "request": {
        "method": "POST",
        "url": "Condition"
      }
    },
    {
      "fullUrl": "urn:uuid:fe7dd9ae-d1a1-425f-8572-7aef16f257fd",
      "resource": {
        "resourceType": "Condition",
        "id": "reference-condition",
        "meta": {
          "profile": [
            "http://hl7.org/fhir/StructureDefinition/Condition"
          ],
          "source": "Hospital-ABC"
        },
        "identifier": [
          {
            "system": "http://localhost:8888/hive-connect/fhir/identifiers/conditions",
            "value": "COND-123456",
            "use": "official",
            "type": {
              "coding": [
                {
                  "system": "http://terminology.hl7.org/CodeSystem/v2-0203",
                  "code": "MR",
                  "display": "Medical record number"
                }
              ],
              "text": "Medical Record Number"
            },
            "assigner": {
              "display": "Example Hospital"
            }
          }
        ],
        // "clinicalStatus": {
        //   "coding": [
        //     {
        //       "system": "http://terminology.hl7.org/CodeSystem/condition-clinical",
        //       "code": "active",
        //       "display": "Active"
        //     }
        //   ],
        //   "text": "Active"
        // },
        "verificationStatus": {
          "coding": [
            {
              "system": "http://terminology.hl7.org/CodeSystem/condition-ver-status",
              "code": "refuted",
              "display": "Refuted"
            }
          ],
          "text": "Refuted"
        },
        "category": [
          {
            "coding": [
              {
                "system": "http://terminology.hl7.org/CodeSystem/condition-category",
                "code": "problem-list-item",
                "display": "Problem List Item"
              }
            ],
            "text": "Problem List Item"
          }
        ],
        "severity": {
          "coding": [
            {
              "system": "http://snomed.info/sct",
              "code": "246112005",
              "display": "Severity"
            }
          ],
          "text": "Severe"
        },
        "code": {
          "coding": [
            {
              "system": "http://fhir.de/CodeSystem/bfarm/icd-10-gm",
              "code": "C34.1",
              "display": "Malignant neoplasm of upper lobe, bronchus or lung",
              "version": "2024",
              "extension": [
                {
                  "url": "http://fhir.de/StructureDefinition/icd-10-gm-mehrfachcodierungs-kennzeichen",
                  "valueCoding": {
                    "system": "http://fhir.de/CodeSystem/icd-10-gm-mehrfachcodierungs-kennzeichen",
                    "code": "†",
                    "display": "†"
                  }
                },
                {
                  "url": "http://fhir.de/StructureDefinition/seitenlokalisation",
                  "valueCoding": {
                    "system": "https://fhir.kbv.de/CodeSystem/KBV_CS_SFHIR_ICD_SEITENLOKALISATION",
                    "code": "R",
                    "display": "rechts"
                  }
                },
                {
                  "url": "http://fhir.de/StructureDefinition/icd-10-gm-diagnosesicherheit",
                  "valueCoding": {
                    "system": "https://fhir.kbv.de/CodeSystem/KBV_CS_SFHIR_ICD_DIAGNOSESICHERHEIT",
                    "code": "A",
                    "display": "ausgeschlossen"
                  }
                }
              ]
            },
            {
              "system": "http://terminology.hl7.org/CodeSystem/icd-o-3",
              "code": "C34.1",
              "display": "Malignant neoplasm of upper lobe, bronchus or lung"
            },
            {
              "system": "http://fhir.de/CodeSystem/bfarm/alpha-id",
              "code": "098H5",
              "display": "Malignant neoplasm of upper lobe, bronchus or lung, unspecified"
            },
            {
              "system": "http://snomed.info/sct",
              "code": "254626006",
              "display": "Adenocarcinoma of lung (disorder)"
            },
            {
              "system": "http://www.orpha.net",
              "code": "830",
              "display": "Malignant neoplasm of upper lobe, bronchus or lung"
            }
          ],
          "text": "Malignant neoplasm of upper lobe, bronchus or lung"
        },
        "bodySite": [
          {
            "coding": [
              {
                "system": "http://snomed.info/sct",
                "code": "368209003",
                "display": "Right upper arm"
              },
              {
                "system": "http://terminology.hl7.org/CodeSystem/icd-o-3",
                "version": "2022",
                "code": "C34.1",
                "display": "Upper lobe, bronchus or lung"
              }
            ],
            "text": "Entire cardiovascular system"
          }
        ],
        "subject": {
          "reference": "Patient/{{patientId_fhir-server}}"
        },
        "encounter": {
          "reference": "urn:uuid:a6ea47e6-f871-4a1d-a03a-6a8cd3d5909e",
          "display": "Inpatient Encounter for Asthma",
          "type": "Encounter",
          "identifier": {
            "system": "http://localhost:8888/hive-connect/fhir/identifiers/encounters",
            "value": "encounter-id-1245",
            "use": "official",
            "type": {
              "coding": [
                {
                  "system": "http://terminology.hl7.org/CodeSystem/v2-0203",
                  "code": "VN",
                  "display": "Visit Number"
                }
              ],
              "text": "Visit Number"
            },
            "assigner": {
              "display": "Example Hospital"
            },
            "period": {
              "start": "2024-01-01T08:00:00Z",
              "end": "2024-01-01T10:00:00Z"
            }
          }
        },
        "onsetDateTime": "2024-02-08T00:00:00Z",
        "recordedDate": "2024-05-02T00:00:00Z",
        "recorder": {
          // "reference": "Practitioner/example-recorder",
          "display": "Dr. John Doe"
        },
        "asserter": {
        //   "reference": "http://external.fhir.server/Practitioner/f201",
          "display": "Dr. John Doe"
        },
        "stage": [
          {
            "summary": {
              "coding": [
                {
                  "system": "http://snomed.info/sct",
                  "code": "258219007",
                  "display": "Stage 2"
                }
              ],
              "text": "Stage 2"
            }
          }
        ],
        "evidence": [
          {
            "code": [
              {
                "coding": [
                  {
                    "system": "http://snomed.info/sct",
                    "code": "127489000",
                    "display": "Has active ingredient"
                  }
                ],
                "text": "Has active ingredient"
              }
            ]
          }
        ],
        "note": [
          {
            "text": "The patient has a history of high blood pressure, now presenting with severe hypertension."
          }
        ]
      },
      "request": {
        "method": "POST",
        "url": "Condition"
      }
    },
    {
      "fullUrl": "urn:uuid:a6ea47e6-f871-4a1d-a03a-6a8cd3d5909e",
      "resource": {
        "meta": {
          "profile": [
            "https://www.medizininformatik-initiative.de/fhir/core/modul-fall/StructureDefinition/KontaktGesundheitseinrichtung"
          ]
        },
        "resourceType": "Encounter",
        "id": "a6ea47e6-f871-4a1d-a03a-6a8cd3d5909e",
        "class": {
          "code": "AMB",
          "display": "ambulatory",
          "system": "http://terminology.hl7.org/CodeSystem/v3-ActCode"
        },
        "identifier": [
          {
            "use": "official",
            "type": {
              "coding": [
                {
                  "system": "http://terminology.hl7.org/CodeSystem/v2-0203",
                  "code": "VN",
                  "display": "Visit number"
                }
              ],
              "text": "Visit number"
            },
            "system": "http://localhost:8888/hive-connect/fhir/identifiers/encounters",
            "value": "encounter-id-1245"
          }
        ],
        "status": "finished",
        "subject": {
          "reference": "Patient/{{patientId_fhir-server}}"
        },
        "period": {
          "start": "2024-08-21T09:00:00+01:00",
          "end": "2024-08-21T09:30:00+01:00"
        }
      },
      "request": {
        "method": "POST",
        "url": "Encounter"
      }
    }
  ]
}
```

### Debug HIVEconnect server

The output of openFHir (openEHR composition) can be logged by setting the below configuration

```yaml
hive-connect:
  debug:
    enabled: true
    mapping-output-directory: ${java.io.tmpdir}/mappings
```
