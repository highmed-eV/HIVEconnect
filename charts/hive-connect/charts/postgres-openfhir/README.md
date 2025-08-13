```bash
kubectl create secret generic postgres-openfhir-secrets \
  --from-literal=username=openfhir \
  --from-literal=password=postgres \
  --from-literal=database=openfhir \
  --from-literal=postgresPassword=postgres
```
