# Terraform — Infraestructura AgroMIS

## Estructura

```
modules/          # Modulos reutilizables
  vpc/            # VPC + subnets + NAT gateways
  eks/            # EKS cluster + node groups (core on-demand, batch spot)
  rds-postgres/   # RDS PostgreSQL 16 con KMS, Multi-AZ opcional
  elasticache-redis/  # ElastiCache Redis 7.x
  s3-datalake/    # 3 buckets (datalake, raw, backups) con lifecycle
  iam-oidc/       # Rol IRSA para service accounts de K8s
  bastion-ssm/    # Acceso a DB via SSM (sin bastion EC2)
  backups/        # Snapshots cross-region y Velero
  cost-alerts/    # AWS Budgets

environments/
  staging/        # Staging (us-east-1, instancias pequenas, single-AZ)
  production/     # Produccion (multi-AZ, instancias grandes)
```

## Requisitos previos

1. Bucket S3 para state: `agromis-terraform-state-staging`
2. Tabla DynamoDB para locks: `agromis-terraform-locks-staging`
3. AWS CLI configurado con credenciales

## Desplegar staging

```bash
cd environments/staging
terraform init
terraform plan -var="db_password=$DB_PASSWORD"
terraform apply -var="db_password=$DB_PASSWORD"
```

## Post-deploy

```bash
# Configurar kubeconfig
aws eks update-kubeconfig --name agromis-staging-cluster --region us-east-1
kubectl get nodes
```

## Tags estandar

Todos los recursos tienen:
- `Project=agromis`
- `Environment=staging|prod`
- `ManagedBy=terraform`
