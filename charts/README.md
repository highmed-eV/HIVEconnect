# Hive-Connect Helm Chart

Dieses Helm-Chart deployt den Hive-Connect-Service in einem Kubernetes-Cluster. Es unterstützt Konfigurationen für Datenbankzugriffe, Authentifizierung, SSL-Kommunikation und externe Services wie EHRbase und OpenFHIR.
Es benötigt für erfolgreiches Deployment die EHRbase, openFHIR und einen HAPIServer.
---

## 📦 Installation

### Voraussetzungen

- Helm 3.x
- Zugriff auf ein Kubernetes-Cluster
- Konfigurierte Secrets und ConfigMaps

### Install

```bash
helm install hive-connect ./hive-connect \
  --values values.yaml \
  --namespace your-namespace
```

## ⚙️ Konfiguration (`values.yaml`)

| Name | Wert | Erklärung |
|------|------|-----------|
| `name` | `hive-connect` | Name der Anwendung |
| `replicaCount` | `1` | Anzahl der Replikate |
| `global.certManager.enabled` | `false` | Ob cert-manager verwendet wird |
| `global.postgresql.servicePort` | `5432` | Port des PostgreSQL-Dienstes |
| `global.ehrBase.hostName` | `ehrbase` | Hostname des EHRbase-Dienstes |
| `global.ehrBase.port` | `8080` | Port des EHRbase-Dienstes |
| `global.ehrBase.contextPath` | `/ehrbase/` | Pfad zum EHRbase-Endpunkt |
| `global.openFHIR.enabled` | `true` | Ob OpenFHIR aktiviert ist |
| `global.openFHIR.hostName` | `openfhir` | Hostname des OpenFHIR-Dienstes |
| `global.openFHIR.port` | `8090` | Port des OpenFHIR-Dienstes |
| `imagePullSecrets[0].name` | `imagepullsecret-github-packages` | Secret für Image-Pull aus privatem Registry |
| `container.image` | `ghcr.io/highmed-ev/hive-connect` | Container-Image für Hive-Connect |
| `container.pullPolicy` | `Always` | Image Pull Policy |
| `container.environment.profile` | `deploy` | Spring-Profil |
| `container.environment.sqlDialect` | `org.hibernate.dialect.PostgreSQLDialect` | SQL-Dialekt für Hibernate |
| `appConfig.bootstrapdir` | `/app/templates` | Pfad zum Bootstrap-Verzeichnis |
| `appConfig.serverUrl` | `http://develop-hapi-fhir:8080/fhir` | URL des FHIR-Servers |
| `appConfig.postgresqlDatabase` | `hiveconnect` | Name der PostgreSQL-Datenbank |
| `appConfig.postgresqlUsername` | `hiveconnect` | Benutzername für PostgreSQL |
| `appConfig.postgresqlPassword` | `password` | Passwort für PostgreSQL |
| `appConfig.postgresqlDatabaseSecretName` | `hive-connect-postgres-hiveconnect-secret` | Name des Secrets mit DB-Zugangsdaten |
| `appConfig.databaseServiceName` | `develop-hive-connect-postgres-hiveconnect` | Service-Name der Datenbank |
| `appConfig.version` | `develop` | Version der Anwendung |
| `appConfig.enabled` | `true` | Ob die Anwendung aktiviert ist |
| `appConfig.ingressUrl` | `hive-connect.develop.de` | Ingress-URL der Anwendung |
| `appConfig.environment.ehrbaseUsername` | `ehrbase` | Benutzername für EHRbase |
| `appConfig.environment.ehrbasePassword` | `ehrbase` | Passwort für EHRbase |
| `appConfig.environment.ehrbaseAuthAdminUser` | `ehrbaseAdmin` | Admin-Benutzername für EHRbase |
| `appConfig.environment.ehrbaseAuthAdminPassword` | `ehrbaseAdmin` | Admin-Passwort für EHRbase |
| `appConfig.keycloakUrl` | `http://keycloak-http` | URL des Keycloak-Servers |
| `appConfig.keycloakRealm` | `realmName` | Keycloak Realm |
| `appConfig.authenticationType` | `basic` | Authentifizierungstyp (basic oder oauth2) |
| `appConfig.basicUsername` | `username` | Benutzername für Basic Auth |
| `appConfig.basicPassword` | `password` | Passwort für Basic Auth |
| `appConfig.demographicsPatientUrl` | `https://demographics-service` | URL zum Demographics-Service |
| `appConfig.terminologyServerUrl` | `https://terminology-server` | URL zum Terminologie-Server |
| `appConfig.fhirValidationOptionalIdentifier` | `true` | Ob FHIR-Identifier optional validiert werden |
| `appConfig.forceUpdate` | `true` | Ob Templates beim Start aktualisiert werden |
| `appConfig.certificateName` | `hiveconnect-certificate` | Name des TLS-Zertifikats |
| `appConfig.clientssl.keystorepass` | `keystorepass` | Passwort für Keystore |
| `appConfig.clientssl.truststorepass` | `truststorepass` | Passwort für Truststore |
| `appConfig.clientssl.enabled` | `true` | Ob SSL aktiviert ist |
| `appConfig.clientssl.keypassword` | `keypassword` | Passwort für den privaten Schlüssel |
| `appConfig.clientssl.keystore` | `/etc/hiveconnect/keystore.p12` | Pfad zum Keystore |
| `appConfig.clientssl.keystoretype` | `pkcs12` | Typ des Keystores |
| `appConfig.clientssl.truststore` | `/etc/hiveconnect/truststore.p12` | Pfad zum Truststore |
| `appConfig.clientssl.truststoretype` | `pkcs12` | Typ des Truststores |
| `service.enabled` | `true` | Ob der Service aktiviert ist |
| `service.port` | `8888` | Port des Hive-Connect-Service |
| `service.type` | `ClusterIP` | Typ des Kubernetes-Service |

## Init-Container
Ein Init-Container wartet auf die Erreichbarkeit der PostgreSQL-Datenbank, bevor der Hauptcontainer startet.

## openFHIR
Bei openFHIR handelt es sich um eine externe Komponente die als Dependency von HIVEconnect benötigt wird.

### Bei Fragen oder Problemen wende dich an das DevOps-Team oder öffne ein Issue im Repository.
