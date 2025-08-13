# Hive-Connect Helm Chart

Dieses Helm-Chart deployt den Hive-Connect-Service in einem Kubernetes-Cluster. Es unterstützt Konfigurationen für Datenbankzugriffe, Authentifizierung, SSL-Kommunikation und externe Services wie EHRbase und OpenFHIR.
Es benötigt für erfolgreiches Deployment die EHRbase, openFHIR und einen HAPIServer.

## 📦 Installation

### Voraussetzungen

- Helm 3.x
- Zugriff auf ein Kubernetes-Cluster
- Konfigurierte Secrets und ConfigMaps

### Install

```bash
helm install hive-connect ./hive-connect \
  --values values.yaml \
  --namespace hive-connect
```

## Values

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| ehrbase.appConfig.HealthEnabled | bool | `true` |  |
| ehrbase.appConfig.KubeHealthEnabled | bool | `true` |  |
| ehrbase.appConfig.adminApiActive | bool | `true` |  |
| ehrbase.appConfig.adminApiAllowDeleteAll | bool | `true` |  |
| ehrbase.appConfig.aqlConfigUseJsQuery | bool | `false` |  |
| ehrbase.appConfig.cacheEnabled | bool | `true` |  |
| ehrbase.appConfig.certificateName | string | `"ehrbase-certificate"` |  |
| ehrbase.appConfig.databaseServiceName | string | `"postgres-ehrbase"` |  |
| ehrbase.appConfig.dbAdminPassword | string | `"postgres"` |  |
| ehrbase.appConfig.dbAdminUser | string | `"ehrbase"` |  |
| ehrbase.appConfig.dbName | string | `"ehrbase"` |  |
| ehrbase.appConfig.dbPassword | string | `"postgres"` |  |
| ehrbase.appConfig.dbPort | int | `5432` |  |
| ehrbase.appConfig.dbUser | string | `"ehrbase"` |  |
| ehrbase.appConfig.disableStrictValidation | bool | `true` |  |
| ehrbase.appConfig.ehrbaseAuthAdminPassword | string | `"ehrbase"` |  |
| ehrbase.appConfig.ehrbaseAuthAdminUser | string | `"ehrbase"` |  |
| ehrbase.appConfig.ehrbaseAuthPassword | string | `"ehrbase"` |  |
| ehrbase.appConfig.ehrbaseAuthType | string | `"BASIC"` |  |
| ehrbase.appConfig.ehrbaseAuthUser | string | `"ehrbaseAuth"` |  |
| ehrbase.appConfig.ehrbasePassword | string | `"ehrbase"` |  |
| ehrbase.appConfig.ehrbaseUser | string | `"ehrbase"` |  |
| ehrbase.appConfig.ingressUrl | string | `"ehrbase.develop.dev.num-rdp.de"` |  |
| ehrbase.appConfig.loggingPluginVersion | string | `"develop"` |  |
| ehrbase.appConfig.managementEndpointEnvEnabled | bool | `true` |  |
| ehrbase.appConfig.managementEndpointInfoEnabled | bool | `true` |  |
| ehrbase.appConfig.managementEndpointMetricsEnabled | bool | `true` |  |
| ehrbase.appConfig.managementEndpointPrometheusEnabled | bool | `true` |  |
| ehrbase.appConfig.postgresqlDatabase | string | `"ehrbase"` |  |
| ehrbase.appConfig.postgresqlDatabaseSecretName | string | `"hive-connect-postgres-ehrbase-secret"` |  |
| ehrbase.appConfig.systemName | string | `"local.ehrbase.org"` |  |
| ehrbase.appConfig.version | string | `"2.13.0"` |  |
| ehrbase.container.ehrbaseLoggingPlugin.image | string | `"ghcr.io/highmed-ev/ehrbase-logging-plugin"` |  |
| ehrbase.container.ehrbaseLoggingPlugin.tag | string | `"develop"` |  |
| ehrbase.container.image | string | `"ehrbase/ehrbase"` |  |
| ehrbase.container.pullPolicy | string | `"Always"` |  |
| ehrbase.container.resources | object | `{}` |  |
| ehrbase.externalSecrets | bool | `false` |  |
| ehrbase.global.certManager.enabled | bool | `false` |  |
| ehrbase.global.ehrBase.enabled | bool | `true` |  |
| ehrbase.global.ehrBase.loggingPluginVersion | string | `"develop"` |  |
| ehrbase.global.ehrBase.pullPushIntervalInS | string | `"60"` |  |
| ehrbase.global.postgresql.storageClass | string | `"default"` |  |
| ehrbase.imagePullSecrets[0].name | string | `"imagepullsecret-github-packages"` |  |
| ehrbase.name | string | `"ehrbase"` |  |
| ehrbase.replicaCount | int | `1` |  |
| ehrbase.service.port | int | `8080` |  |
| ehrbase.service.type | string | `"ClusterIP"` |  |
| externalSecrets.databaseKey | string | `"database"` |  |
| externalSecrets.passwordKey | string | `"password"` |  |
| externalSecrets.postgresPasswordKey | string | `"postgresPassword"` |  |
| externalSecrets.secretName | string | `"postgres-ehrbase-secrets"` |  |
| externalSecrets.usernameKey | string | `"username"` |  |
| hapi-fhir.externalDatabase.database | string | `"hapi"` |  |
| hapi-fhir.externalDatabase.existingSecret | string | `""` |  |
| hapi-fhir.externalDatabase.existingSecretKey | string | `""` |  |
| hapi-fhir.externalDatabase.host | string | `"postgres-hapi"` |  |
| hapi-fhir.externalDatabase.password | string | `"postgres"` |  |
| hapi-fhir.externalDatabase.user | string | `"hapi"` |  |
| hapi-fhir.extraEnv[0].name | string | `"HAPI.FHIR.ALLOWED_BUNDLE_TYPES"` |  |
| hapi-fhir.extraEnv[0].value | string | `"COLLECTION,DOCUMENT,MESSAGE,TRANSACTION"` |  |
| hapi-fhir.image.pullPolicy | string | `"Always"` |  |
| hapi-fhir.image.registry | string | `"ghcr.io"` |  |
| hapi-fhir.image.repository | string | `"num-forschungsdatenplattform/num-hapi"` |  |
| hapi-fhir.image.tag | string | `"v6.8.3"` |  |
| hapi-fhir.imagePullSecrets[0].name | string | `"imagepullsecret-github-packages"` |  |
| hapi-fhir.postgresql.auth.database | string | `"hapi"` |  |
| hapi-fhir.postgresql.enabled | bool | `false` |  |
| hiveconnect.appConfig.authenticationType | string | `"basic"` |  |
| hiveconnect.appConfig.basicPassword | string | `"password"` |  |
| hiveconnect.appConfig.basicUsername | string | `"username"` |  |
| hiveconnect.appConfig.bootstrapdir | string | `"/app/templates"` |  |
| hiveconnect.appConfig.certificateName | string | `"hiveconnect-certificate"` |  |
| hiveconnect.appConfig.clientssl.enabled | string | `"true"` |  |
| hiveconnect.appConfig.clientssl.keypassword | string | `"keypassword"` |  |
| hiveconnect.appConfig.clientssl.keystore | string | `"/etc/hiveconnect/keystore.p12"` |  |
| hiveconnect.appConfig.clientssl.keystorepass | string | `"keystorepass"` |  |
| hiveconnect.appConfig.clientssl.keystoretype | string | `"pkcs12"` |  |
| hiveconnect.appConfig.clientssl.truststore | string | `"/etc/hiveconnect/truststore.p12"` |  |
| hiveconnect.appConfig.clientssl.truststorepass | string | `"truststorepass"` |  |
| hiveconnect.appConfig.clientssl.truststoretype | string | `"pkcs12"` |  |
| hiveconnect.appConfig.databaseServiceName | string | `"postgres-hiveconnect"` |  |
| hiveconnect.appConfig.demographicsPatientUrl | string | `"https://demographics-service"` |  |
| hiveconnect.appConfig.enabled | bool | `true` |  |
| hiveconnect.appConfig.environment.ehrbaseAuthAdminPassword | string | `"ehrbaseAdmin"` |  |
| hiveconnect.appConfig.environment.ehrbaseAuthAdminUser | string | `"ehrbaseAdmin"` |  |
| hiveconnect.appConfig.environment.ehrbasePassword | string | `"ehrbase"` |  |
| hiveconnect.appConfig.environment.ehrbaseUsername | string | `"ehrbase"` |  |
| hiveconnect.appConfig.fhirValidationOptionalIdentifier | bool | `true` |  |
| hiveconnect.appConfig.forceUpdate | bool | `true` |  |
| hiveconnect.appConfig.ingressUrl | string | `"hive-connect.develop.de"` |  |
| hiveconnect.appConfig.keycloakRealm | string | `"realmName"` |  |
| hiveconnect.appConfig.keycloakUrl | string | `"http://keycloak-http"` |  |
| hiveconnect.appConfig.postgresqlDatabase | string | `"hiveconnect"` |  |
| hiveconnect.appConfig.postgresqlDatabaseSecretName | string | `"hive-connect-postgres-hiveconnect-secret"` |  |
| hiveconnect.appConfig.postgresqlPassword | string | `"postgres"` |  |
| hiveconnect.appConfig.postgresqlUsername | string | `"hiveconnect"` |  |
| hiveconnect.appConfig.serverUrl | string | `"http://develop-hive-connect-hapi-fhir:8080/fhir"` |  |
| hiveconnect.appConfig.terminologyServerUrl | string | `"https://terminology-server"` |  |
| hiveconnect.appConfig.version | string | `"develop"` |  |
| hiveconnect.container.environment.profile | string | `"deploy"` |  |
| hiveconnect.container.environment.sqlDialect | string | `"org.hibernate.dialect.PostgreSQLDialect"` |  |
| hiveconnect.container.image | string | `"ghcr.io/highmed-ev/hive-connect"` |  |
| hiveconnect.container.pullPolicy | string | `"Always"` |  |
| hiveconnect.container.resources | object | `{}` |  |
| hiveconnect.externalSecrets | bool | `false` |  |
| hiveconnect.global.certManager.enabled | bool | `false` |  |
| hiveconnect.global.ehrBase.contextPath | string | `"/ehrbase/"` |  |
| hiveconnect.global.ehrBase.hostName | string | `"http://ehrbase"` |  |
| hiveconnect.global.ehrBase.port | int | `8080` |  |
| hiveconnect.global.openFHIR.enabled | bool | `true` |  |
| hiveconnect.global.openFHIR.hostName | string | `"http://openfhir"` |  |
| hiveconnect.global.openFHIR.port | int | `8080` |  |
| hiveconnect.global.postgresql.servicePort | int | `5432` |  |
| hiveconnect.imagePullSecrets[0].name | string | `"imagepullsecret-github-packages"` |  |
| hiveconnect.service.enabled | bool | `true` |  |
| hiveconnect.service.port | int | `8888` |  |
| hiveconnect.service.type | string | `"ClusterIP"` |  |
| name | string | `"hive-connect"` |  |
| openfhir.appConfig.databaseHost | string | `"postgres-openfhir"` |  |
| openfhir.appConfig.databasePort | int | `5432` |  |
| openfhir.appConfig.env.BOOTSTRAP_DIR | string | `"/app/bootstrap"` |  |
| openfhir.appConfig.env.BOOTSTRAP_RECURSIVELY_OPEN_DIRECTORIES | bool | `true` |  |
| openfhir.appConfig.env.DB_TYPE | string | `"postgres"` |  |
| openfhir.appConfig.ingressUrl | string | `"openfhir"` |  |
| openfhir.appConfig.postgresqlDatabase | string | `"openfhir"` |  |
| openfhir.appConfig.postgresqlDatabasePassword | string | `"postgres"` |  |
| openfhir.appConfig.postgresqlDatabaseSecretName | string | `"openfhir-postgres-app"` |  |
| openfhir.appConfig.postgresqlDatabaseUser | string | `"openfhir"` |  |
| openfhir.appConfig.version | string | `"1.3.0"` |  |
| openfhir.container.image | string | `"ghcr.io/highmed-ev/openfhir"` |  |
| openfhir.container.pullPolicy | string | `"Always"` |  |
| openfhir.container.resources | object | `{}` |  |
| openfhir.container.tag | string | `"bootstrap"` |  |
| openfhir.externalSecrets | bool | `false` |  |
| openfhir.global.env | string | `"env"` |  |
| openfhir.imagePullSecrets[0].name | string | `"imagepullsecret-github-packages"` |  |
| openfhir.ingress.enabled | bool | `false` |  |
| openfhir.ingress.host | string | `"openfhir.crr.dev.num-codex.de"` |  |
| openfhir.name | string | `"openfhir"` |  |
| openfhir.replicaCount | int | `1` |  |
| openfhir.service.enabled | bool | `true` |  |
| openfhir.service.port | int | `8080` |  |
| openfhir.service.type | string | `"ClusterIP"` |  |
| openfhir.storage.storageClass | string | `"ssd"` |  |
| postgres-ehrbase.auth.database | string | `"ehrbase"` |  |
| postgres-ehrbase.auth.password | string | `"postgres"` |  |
| postgres-ehrbase.auth.postgresPassword | string | `"postgres"` |  |
| postgres-ehrbase.auth.username | string | `"ehrbase"` |  |
| postgres-ehrbase.primary.persistence.enabled | bool | `true` |  |
| postgres-ehrbase.primary.persistence.size | string | `"1Gi"` |  |
| postgres-ehrbase.primary.persistence.storageClass | string | `"ssd"` |  |
| postgres-ehrbase.service.port | int | `5432` |  |
| postgres-ehrbase.service.targetPort | int | `5432` |  |
| postgres-ehrbase.service.type | string | `"ClusterIP"` |  |
| postgres-ehrbase.useExternalSecrets | bool | `false` |  |
| postgres-hapi.auth.database | string | `"hapi"` |  |
| postgres-hapi.auth.password | string | `"postgres"` |  |
| postgres-hapi.auth.postgresPassword | string | `"postgres"` |  |
| postgres-hapi.auth.username | string | `"hapi"` |  |
| postgres-hapi.externalSecrets.databaseKey | string | `"database"` |  |
| postgres-hapi.externalSecrets.passwordKey | string | `"password"` |  |
| postgres-hapi.externalSecrets.postgresPasswordKey | string | `"postgresPassword"` |  |
| postgres-hapi.externalSecrets.secretName | string | `"postgres-hapi-secrets"` |  |
| postgres-hapi.externalSecrets.usernameKey | string | `"username"` |  |
| postgres-hapi.primary.persistence.enabled | bool | `true` |  |
| postgres-hapi.primary.persistence.size | string | `"1Gi"` |  |
| postgres-hapi.primary.persistence.storageClass | string | `"ssd"` |  |
| postgres-hapi.service.port | int | `5432` |  |
| postgres-hapi.service.targetPort | int | `5432` |  |
| postgres-hapi.service.type | string | `"ClusterIP"` |  |
| postgres-hapi.useExternalSecrets | bool | `false` |  |
| postgres-hiveconnect.auth.database | string | `"hiveconnect"` |  |
| postgres-hiveconnect.auth.password | string | `"postgres"` |  |
| postgres-hiveconnect.auth.postgresPassword | string | `"postgres"` |  |
| postgres-hiveconnect.auth.username | string | `"hiveconnect"` |  |
| postgres-hiveconnect.externalSecrets.databaseKey | string | `"database"` |  |
| postgres-hiveconnect.externalSecrets.passwordKey | string | `"password"` |  |
| postgres-hiveconnect.externalSecrets.postgresPasswordKey | string | `"postgresPassword"` |  |
| postgres-hiveconnect.externalSecrets.secretName | string | `"postgres-hiveconnect-secrets"` |  |
| postgres-hiveconnect.externalSecrets.usernameKey | string | `"username"` |  |
| postgres-hiveconnect.primary.persistence.enabled | bool | `true` |  |
| postgres-hiveconnect.primary.persistence.size | string | `"1Gi"` |  |
| postgres-hiveconnect.primary.persistence.storageClass | string | `"ssd"` |  |
| postgres-hiveconnect.service.port | int | `5432` |  |
| postgres-hiveconnect.service.targetPort | int | `5432` |  |
| postgres-hiveconnect.service.type | string | `"ClusterIP"` |  |
| postgres-hiveconnect.useExternalSecrets | bool | `false` |  |
| postgres-openfhir.auth.database | string | `"openfhir"` |  |
| postgres-openfhir.auth.password | string | `"postgres"` |  |
| postgres-openfhir.auth.postgresPassword | string | `"postgres"` |  |
| postgres-openfhir.auth.username | string | `"openfhir"` |  |
| postgres-openfhir.externalSecrets.databaseKey | string | `"database"` |  |
| postgres-openfhir.externalSecrets.passwordKey | string | `"password"` |  |
| postgres-openfhir.externalSecrets.postgresPasswordKey | string | `"postgresPassword"` |  |
| postgres-openfhir.externalSecrets.secretName | string | `"postgres-openfhir-secrets"` |  |
| postgres-openfhir.externalSecrets.usernameKey | string | `"username"` |  |
| postgres-openfhir.primary.persistence.enabled | bool | `true` |  |
| postgres-openfhir.primary.persistence.size | string | `"1Gi"` |  |
| postgres-openfhir.primary.persistence.storageClass | string | `"ssd"` |  |
| postgres-openfhir.service.port | int | `5432` |  |
| postgres-openfhir.service.targetPort | int | `5432` |  |
| postgres-openfhir.service.type | string | `"ClusterIP"` |  |
| postgres-openfhir.useExternalSecrets | bool | `false` |  |
| replicaCount | int | `1` |  |

## openFHIR
Bei openFHIR handelt es sich um eine externe Komponente die als Dependency von HIVEconnect benötigt wird.

## Issues
Bei Fragen oder Problemen wende dich an das DevOps-Team oder öffne ein Issue im Repository.
