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
* OpenFHIR (>= v0.0.1) (https://github.com/medblocks/openFHIR/tree/main)
* EHRbase (>= v2.0.0) (or similiar openEHR platform)


### Deployment documentation

[Find the documenation here](docker/deployment.md)

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
