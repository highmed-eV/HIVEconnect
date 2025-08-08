```bash
kubectl create secret generic postgres-ehrbase-secrets \
  --from-literal=username=ehrbase \
  --from-literal=password=postgres \
  --from-literal=database=ehrbase \
  --from-literal=postgresPassword=postgres
```
