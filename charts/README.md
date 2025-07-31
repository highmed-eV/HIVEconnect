# 🐝 Hive-Connect Helm Chart

Dieses Helm-Chart deployt den Hive-Connect-Service in einem Kubernetes-Cluster. Es unterstützt Konfigurationen für Datenbankzugriffe, Authentifizierung, SSL-Kommunikation und externe Services wie EHRbase und OpenFHIR.

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

| Schlüssel                                 | Beschreibung                                 |
|------------------------------------------|----------------------------------------------|
| `name`                                   | Name der Anwendung                           |
| `replicaCount`                           | Anzahl der Replikate                         |
| `container.image`                        | Container-Image                              |
| `container.pullPolicy`                   | Pull-Policy                                  |
| `appConfig.version`                      | Version der Anwendung                        |
| `appConfig.databaseServiceName`          | Hostname des PostgreSQL-Services             |
| `appConfig.postgresqlDatabase`           | Name der Datenbank                           |
| `appConfig.postgresqlDatabaseSecretName` | Secret mit DB-Zugangsdaten                   |
| `global.postgresql.servicePort`          | Port des PostgreSQL-Services                 |
| `global.ehrBase.*`                       | Konfiguration für EHRbase                    |
| `global.openFHIR.*`                      | Konfiguration für OpenFHIR                   |
| `appConfig.authenticationType`           | `basic` oder `oauth2`                        |
| `appConfig.clientssl.enabled`            | SSL aktivieren (`\"true\"` oder `\"false\"`) |


## Init-Container
Ein Init-Container wartet auf die Erreichbarkeit der PostgreSQL-Datenbank, bevor der Hauptcontainer startet.

## openFHIR
Bei openFHIR handelt es sich um eine externe Komponete die als Dependency von HIVEconnect benötigt wird.

## Automatischer Rollout bei Secret-Änderungen
Durch die Annotation checksum/secrets wird ein Deployment automatisch neu gestartet, wenn sich ein Secret ändert.

## Weiterführende Hinweise
- Stelle sicher, dass alle Secrets und ConfigMaps vor dem Deployment vorhanden sind.
- Verwende helm template zur lokalen Validierung der Templates.
- Nutze helm lint zur Prüfung auf Syntaxfehler.

### Bei Fragen oder Problemen wende dich an das DevOps-Team oder öffne ein Issue im Repository.
