```bash
kubectl create secret generic postgres-hapi-secrets \
  --from-literal=username=hapi \
  --from-literal=password=postgres \
  --from-literal=database=hapi \
  --from-literal=postgresPassword=postgres
```
