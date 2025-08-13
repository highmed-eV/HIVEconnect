```bash
kubectl create secret generic postgres-hiveconnect-secrets \
  --from-literal=username=hiveconnect \
  --from-literal=password=postgres \
  --from-literal=database=hiveconnect \
  --from-literal=postgresPassword=postgres
```
