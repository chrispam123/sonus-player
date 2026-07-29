# Sonus Infra — Terraform

Infraestructura como código del backend de Sonus. Despliega en AWS con Terraform.

## Recursos que crea

| Recurso | Nombre en AWS | Descripción |
|---------|--------------|-------------|
| API Gateway | `sonus-develop-apigateway` | Endpoints HTTP públicos |
| Lambda x3 | `sonus-develop-lambda-receptor/lyrics/mood` | Funciones Kotlin |
| SQS x2 | `sonus-develop-sqs-lyrics/mood` | Colas de mensajes |
| SQS DLQ x2 | `sonus-develop-sqs-lyrics-dlq/mood-dlq` | Colas de mensajes fallidos |
| DynamoDB x3 | `sonus-develop-lyrics-cache/request-status/mood-history` | Tablas NoSQL |
| IAM Roles x3 | `sonus-develop-lambda-*-role` | Permisos de las Lambdas |
| CloudWatch | `/aws/apigateway/sonus-develop` | Logs de API Gateway |

## Archivos

```
infra/
├── backend.tf          ← Estado remoto S3 (leer antes de usar)
├── main.tf             ← Provider AWS + locals (prefijo de nombres)
├── variables.tf        ← Variables configurables
├── dynamodb.tf         ← 3 tablas DynamoDB
├── sqs.tf              ← 2 colas + 2 DLQs
├── lambda.tf           ← 3 funciones Lambda + triggers SQS
├── api_gateway.tf      ← HTTP API + rutas + permisos
├── iam.tf              ← Roles y políticas de permisos
├── outputs.tf          ← URLs que imprime al finalizar
└── environments/
    ├── develop.tfvars  ← Variables para desarrollo
    └── prod.tfvars     ← Variables para producción
```

## Uso paso a paso

### PASO 0 — Solo la primera vez (crear bucket S3 para el estado)

```bash
# Crear el bucket
aws s3 mb s3://sonus-terraform-state --region us-east-1

# Activar versionado (para recuperar estados anteriores)
aws s3api put-bucket-versioning \
  --bucket sonus-terraform-state \
  --versioning-configuration Status=Enabled

# Bloquear acceso público
aws s3api put-public-access-block \
  --bucket sonus-terraform-state \
  --public-access-block-configuration \
  "BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true"
```

### PASO 1 — Compilar los JARs de Kotlin

```bash
cd backend
./gradlew :lambda-receptor:shadowJar
./gradlew :lambda-lyrics:shadowJar
./gradlew :lambda-mood:shadowJar
cd ..
```

### PASO 2 — Inicializar Terraform

```bash
cd infra
terraform init
```

### PASO 3 — Ver qué va a crear (sin ejecutar nada)

```bash
terraform plan \
  -var-file="environments/develop.tfvars" \
  -var="deepseek_api_key=sk-tu-key-aqui"
```

### PASO 4 — Desplegar en develop

```bash
terraform apply \
  -var-file="environments/develop.tfvars" \
  -var="deepseek_api_key=sk-tu-key-aqui"
```

Terraform mostrará la URL del API Gateway al finalizar:
```
api_url = "https://abc123.execute-api.us-east-1.amazonaws.com/develop"
```

### PASO 5 — Desplegar en producción

```bash
terraform apply \
  -var-file="environments/prod.tfvars" \
  -var="deepseek_api_key=sk-tu-key-aqui"
```

### Destruir todos los recursos (ahorra costos cuando no usas)

```bash
terraform destroy -var-file="environments/develop.tfvars"
```

## Variables requeridas

| Variable | Descripción | Dónde obtenerla |
|----------|-------------|-----------------|
| `deepseek_api_key` | API key de DeepSeek | platform.deepseek.com |

Las demás variables tienen valores por defecto en `develop.tfvars`.
