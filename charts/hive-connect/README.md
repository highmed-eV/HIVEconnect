# 📘 Hive-Connect Helm-Chart Dokumentation

Dieses Helm-Chart installiert den **Hive-Connect-Service** in einem Kubernetes-Cluster. Es ermöglicht die Integration von medizinischen Systemen wie EHRbase, openFHIR und HAPI FHIR. Die Konfiguration unterstützt u. a. Datenbankanbindung, Authentifizierung, SSL-Verschlüsselung und die Verwendung externer Secrets.

---

## ⚙️ Voraussetzungen

* Helm v3+
* Zugriff auf ein Kubernetes-Cluster
* Konfigurierte `Secrets` und `ConfigMaps`, falls `externalSecrets` verwendet werden
* Folgende Dienste müssen bereitgestellt werden:
  * [EHRbase](https://ehrbase.org/)
  * [OpenFHIR](https://github.com/highmed/openfhir)
  * [HAPI FHIR](https://hapifhir.io/)

---

## 🚀 Installation

```bash
helm install hive-connect ./hive-connect \
  --values values.yaml \
  --namespace hive-connect
```

Das Chart kann auch mit eigenen Werten angepasst installiert oder aktualisiert werden:

```bash
helm upgrade --install hive-connect ./hive-connect \
  -f my-values.yaml \
  --namespace hive-connect
```

---

## 🔧 Konfiguration (`values.yaml`)

Die Konfigurationswerte sind umfangreich. Die wichtigsten Blöcke:

### Hive-Connect Einstellungen (`hiveconnect.appConfig`)

| Key                            | Beschreibung                               |
| ------------------------------ | ------------------------------------------ |
| `authenticationType`           | Authentifizierungstyp (z. B. `basic`)      |
| `keycloakUrl`                  | URL zum Keycloak-Server                    |
| `postgresqlDatabaseSecretName` | Secret-Name mit den DB-Credentials         |
| `clientssl.*`                  | SSL-Konfiguration für Client-Kommunikation |
| `serverUrl`                    | Ziel-FHIR-Server (z. B. HAPI FHIR)         |

### EHRbase Einstellungen (`ehrbase.appConfig`)

| Key                    | Beschreibung                                   |
| ---------------------- | ---------------------------------------------- |
| `ehrbaseAuthType`      | BASIC oder andere Auth-Methode                 |
| `ingressUrl`           | Ingress-Host für den Zugriff auf EHRbase       |
| `managementEndpoint*`  | Aktiviert Management- und Monitoring-Endpunkte |
| `loggingPluginVersion` | Version des Logging-Plugins (z. B. `develop`)  |

### openFHIR Einstellungen (`openfhir.appConfig`)

| Key                            | Beschreibung                     |
| ------------------------------ | -------------------------------- |
| `version`                      | openFHIR-Version                 |
| `postgresqlDatabaseSecretName` | DB-Secret                        |
| `env.*`                        | Bootstrap-Parameter für openFHIR |

### PostgreSQL-Datenbanken

Jeder Service verfügt über eine eigene PostgreSQL-Instanz (sofern nicht deaktiviert) mit konfigurierbaren:

* Zugangsdaten (`auth.*`)
* Speicher (`primary.persistence.*`)
* Service-Ports

Beispielhafte Services:

* `postgres-ehrbase`
* `postgres-hiveconnect`
* `postgres-openfhir`
* `postgres-hapi`

Diese können durch `externalSecrets` abgesichert werden, um sensible Werte nicht direkt im Chart zu speichern.

---

## 🔐 Externe Secrets

Mit aktivierter Option `externalSecrets` können Passwörter, Datenbanknamen und Benutzernamen aus einem externen Secret Management System (z. B. Vault, AWS Secrets Manager) bezogen werden:

```yaml
hiveconnect:
  externalSecrets: true

postgres-hiveconnect:
  useExternalSecrets: true
  externalSecrets:
    secretName: postgres-hiveconnect-secrets
    usernameKey: username
    passwordKey: password
    postgresPasswordKey: postgresPassword
    databaseKey: database
```

### 🔑 Beispiel-Secret (Kubernetes YAML)

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: postgres-hiveconnect-secrets
  namespace: hive-connect
stringData:
  username: hiveconnect
  password: postgres
  postgresPassword: postgres
  database: hiveconnect
```

### 🔍 Secret direkt mit `kubectl` erstellen

```bash
kubectl create secret generic postgres-hiveconnect-secrets \
  --namespace hive-connect \
  --from-literal=username=hiveconnect \
  --from-literal=password=postgres \
  --from-literal=postgresPassword=postgres \
  --from-literal=database=hiveconnect
```

> 💡 Hinweis: Wird `externalSecrets` deaktiviert, müssen die Zugangsdaten direkt im `values.yaml` hinterlegt werden.

---

## 🌐 Ingress & Services

* Standardmäßig wird **ClusterIP** verwendet
* Ingress kann pro Komponente aktiviert werden (`ingress.enabled`)
* Zertifikate können optional mit cert-manager ausgestellt werden (`certManager.enabled`)

---

## 📚 Komponentenübersicht

| Komponente           | Beschreibung                               |
| -------------------- | ------------------------------------------ |
| **hiveconnect**      | Hauptservice für die Datenübertragung      |
| **ehrbase**          | Elektronisches Patientenakten-System       |
| **openfhir**         | Open-Source FHIR-Server                    |
| **hapi-fhir**        | Alternative FHIR-Schnittstelle             |
| **PostgreSQL**       | Datenbank für jede Komponente einzeln      |
| **external-secrets** | Optional für sichere Credential-Verwaltung |

---

## 🧪 Beispiel für eigene Werte (`my-values.yaml`)

```yaml
hiveconnect:
  appConfig:
    authenticationType: "basic"
    basicUsername: "admin"
    basicPassword: "securepass"
    ingressUrl: "hive-connect.develop.example.com"
  service:
    type: LoadBalancer

postgres-hiveconnect:
  primary:
    persistence:
      size: 5Gi
      storageClass: "ssd"
```

---

## 🐛 Troubleshooting

1. **Dienste nicht erreichbar?** &#x20;
   → Es empfiehlt sich, die Netzwerkverbindungen zwischen den Pods und Services zu überprüfen, z. B. mit `kubectl port-forward`.

2. **Fehlende Secrets?** &#x20;
   → Falls `externalSecrets: true` aktiviert ist, müssen die entsprechenden Kubernetes-Secrets vorhanden sein.

3. **SSL-Fehler?** &#x20;
   → Die Keystore-/Truststore-Pfade und Passwörter sollten überprüft werden.

---

## 📞 Support & Kontakt

Bei Fragen oder Problemen:

* Kann sich an das zuständige DevOps-Team gewendet werden
* Alternativ kann ein GitHub-Issue im Repository erstellt werden

---
