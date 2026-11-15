# Infraestructura (Terraform)

Infraestructura AWS de la API de franquicias: **ECS Fargate**, **ECR**, **ALB**, **RDS PostgreSQL**, **Secrets Manager**, **VPC** y **autoscaling**. Todo en Terraform, modular y con tres environments (`dev`, `staging`, `prod`).

## Arquitectura

```
Internet
   │ HTTP :80
   ▼
┌───────────────── VPC (2 AZ) ─────────────────┐
│  subnets públicas        subnets privadas    │
│  ┌─────────┐             ┌──────────────┐    │
│  │   ALB   │── :8080 ──▶ │ ECS Fargate  │    │
│  └─────────┘             │ (API)        │    │
│  ┌─────────┐             └──────┬───────┘    │
│  │   NAT   │◀── salida ─────────┤            │
│  └─────────┘                    │ :5432 SSL  │
│                          ┌──────▼───────┐    │
│                          │ RDS Postgres │    │
│                          └──────────────┘    │
└──────────────────────────────────────────────┘
 ECR (imagen) · Secrets Manager (credenciales de la DB) · CloudWatch Logs
```

- El ALB es lo único público. Las tasks y la base viven en subnets privadas.
- Los security groups encadenan el acceso: internet → ALB (80) → ECS (8080) → RDS (5432). Cada uno solo acepta tráfico del anterior.
- Las tasks salen a internet por un NAT (ECR, Secrets Manager y logs).
- Las credenciales de la base nunca van en variables de entorno: la task definition las inyecta desde Secrets Manager (`secrets`, no `environment`).

## Estructura

```
packages/iac/
├── bootstrap/      bucket S3 del state remoto (se aplica una vez, con state local)
├── modules/
│   ├── network/    VPC, subnets, NAT, rutas y security groups (alb, ecs, rds)
│   ├── ecr/        repositorio de imágenes, scan on push, conserva las últimas 10
│   ├── database/   RDS PostgreSQL 16, contraseña aleatoria y secret en Secrets Manager
│   ├── iam/        execution role (mínimo privilegio) y task role vacío
│   ├── alb/        load balancer, target group y listener HTTP
│   └── ecs/        cluster, task definition, service, log group y autoscaling por CPU
├── environments/   un directorio por environment que ensambla los módulos
│   ├── dev/
│   ├── staging/
│   └── prod/
├── policies/       política IAM del usuario que despliega
└── scripts/        scripts de operación (ver más abajo)
```

## Requisitos

- [Terraform](https://developer.hashicorp.com/terraform) 1.10 o superior (el state usa `use_lockfile`)
- AWS CLI v2, Docker y `jq`
- Java 17+ y el wrapper de Gradle del repo (el deploy compila el jar)
- Una cuenta AWS y credenciales de admin para el primer paso

## Inicio rápido

```bash
# 1. Una vez por cuenta: usuario deployer, perfil local y bucket de state.
#    Con credenciales de admin (no con el perfil nequi-tf).
AWS_PROFILE=<admin> scripts/init.sh

# 2. Desplegar un environment (compila, sube la imagen y aplica Terraform).
scripts/deploy.sh dev

# 3. Probar.
curl "$(terraform -chdir=environments/dev output -raw api_url)/actuator/health"

# 4. Apagar todo cuando termines (cobra por hora).
scripts/destroy.sh dev
scripts/destroy.sh bootstrap   # al final, solo cuando no quede ningún environment
```

Los scripts se corren desde `packages/iac`.

## Scripts

| Script | Cuándo | Qué hace |
|---|---|---|
| `scripts/init.sh [--yes]` | Una vez por cuenta | Corre `01`, `02` y el bootstrap en orden. Salta lo que ya está hecho y avisa si el usuario ya tiene 2 access keys. |
| `scripts/deploy.sh <env> [--yes]` | Cada deploy | Compila el jar, crea el repo ECR, construye y sube la imagen, aplica Terraform y fuerza el rollout de ECS. Espera a que el service estabilice e imprime el health. |
| `scripts/destroy.sh <env\|bootstrap> [--yes]` | Al terminar | Destruye un environment, o el bootstrap. El bootstrap se niega si algún environment aún tiene recursos. |
| `scripts/01-create-deployer.sh` | Una vez por cuenta | Crea grupo, política y usuario IAM `terraform-deployer`. Idempotente. |
| `scripts/02-create-access-key.sh` | Una vez por máquina | Crea una access key y la guarda en el perfil local `nequi-tf`. No imprime el secret. |
| `scripts/99-destroy-deployer.sh` | Al terminar el reto | Borra keys, usuario y grupo. Antes, destruir los environments. |

`--yes` salta la confirmación interactiva de Terraform. Variables opcionales: `AWS_PROFILE` (por defecto `nequi-tf`), `REGION` (`us-east-1`) e `IMAGE_TAG` (`latest`, solo en `deploy.sh`).

## Acceso a AWS sin usar root

Root no se usa para desplegar. `01-create-deployer.sh` crea el usuario IAM `terraform-deployer` con permisos acotados, y `02-create-access-key.sh` guarda su access key en el perfil local **`nequi-tf`**.

- **`terraform-deployer`** es el usuario IAM. Vive en AWS. Sus permisos van en el grupo `terraform-deployers`, así que re-correr `01` actualiza la política sin tocar nada más.
- **`nequi-tf`** es solo el nombre del perfil en `~/.aws/credentials`. No existe en AWS.

Comprobar que se actúa como el deployer y no como root:

```bash
export AWS_PROFILE=nequi-tf
aws sts get-caller-identity   # debe mostrar .../user/terraform-deployer
```

**Otra máquina:** no se recrea el usuario. Con credenciales de admin, correr `scripts/02-create-access-key.sh`. Un usuario IAM admite máximo 2 access keys: si falla, borrar una vieja con `aws iam delete-access-key`.

### Qué cubre la política

`policies/deployer-policy.json` separa dos niveles:

- **Quien despliega:** permisos por servicio (`ecs:*`, `rds:*`, `ec2:*`...) porque Terraform crea muchos tipos de recurso. IAM queda acotado a roles y políticas `nequi-*`, `iam:PassRole` solo hacia `ecs-tasks.amazonaws.com`, y S3 solo a buckets `nequi-tfstate-*`.
- **La app en runtime:** roles de mínimo privilegio, definidos en el módulo `iam`.

### Seguridad de la key

- Nunca va al repo, a un `.tfvars` ni a variables del contenedor. `.gitignore` excluye state, planes y `*.auto.tfvars`.
- El script deja `~/.aws/credentials` con permisos `600`.
- Si se filtra: IAM > Users > Security credentials > desactivar y borrar la key.

## Environments

Cada directorio de `environments/` es un root module independiente con su propio state. `main.tf`, `variables.tf` y `outputs.tf` son idénticos entre ellos. Solo cambian el `backend.tf` (la `key` del state) y el `*.tfvars`.

| Environment | RDS multi-AZ | Tasks (mín–máx) | State key |
|---|---|---|---|
| `dev` | no | 1–2 | `dev/terraform.tfstate` |
| `staging` | no | 1–2 | `staging/terraform.tfstate` |
| `prod` | sí | 2–4 | `prod/terraform.tfstate` |

**Por qué un directorio por environment y no workspaces:** el state y los valores de cada uno quedan visibles y separados, y un `apply` en dev no puede tocar prod por un workspace mal seleccionado. El costo es duplicar tres archivos. Los módulos sí se comparten.

### State remoto

El state vive en un bucket S3 con versionado, cifrado y acceso público bloqueado, con bloqueo por lockfile (sin DynamoDB). El bucket se llama `nequi-tfstate-<account-id>` y lo crea `bootstrap/`.

El nombre del bucket **no está en `backend.tf`**: un bloque `backend` no admite variables y un nombre fijo se rompe en otra cuenta. Se pasa en el `init`, calculado de la cuenta activa. Los scripts ya lo hacen. A mano:

```bash
export AWS_PROFILE=nequi-tf
cd environments/dev
terraform init -backend-config="bucket=nequi-tfstate-$(aws sts get-caller-identity --query Account --output text)"
terraform plan -var-file=dev.tfvars
```

### Hacer un `plan` sin desplegar

```bash
cd environments/staging   # o prod
terraform init -backend-config="bucket=nequi-tfstate-<account-id>"
terraform plan -var-file=staging.tfvars
```

## Cómo funciona el deploy

`deploy.sh` ejecuta, en este orden:

1. `./gradlew bootJar`: el Dockerfile solo hace `COPY *.jar`, así que el jar se compila afuera.
2. `terraform init` con el bucket calculado.
3. `terraform apply -target=module.ecr`: el repositorio debe existir antes del push. Es el único uso de `-target`, y rompe el huevo y la gallina de la primera corrida.
4. `docker build` y `docker push` (`linux/amd64`, el contexto es la carpeta del jar).
5. `terraform apply` completo.
6. `aws ecs update-service --force-new-deployment` y espera a que estabilice. `latest` es un tag mutable, así que Terraform no detecta el cambio de imagen y hay que forzar el rollout.

El primer deploy de un environment tarda unos 10 a 15 minutos. Casi todo es RDS.

## Decisiones y trade-offs

| Decisión | Por qué | Costo o riesgo |
|---|---|---|
| **Un solo NAT gateway** | Es lo más caro de la red. Una réplica por AZ duplicaría ese gasto. | Punto único de falla: si cae su AZ, las tasks pierden salida a internet. En producción real iría uno por AZ. |
| **ALB solo HTTP, sin dominio** | No hay dominio ni certificado en el reto. | Tráfico en claro hacia el ALB. Con dominio iría un listener 443 con ACM y redirect de 80 a 443. El analizador estático lo marca (S5332) y se acepta. |
| **ALB sin access logs** | Exigen un bucket S3 aparte con su política. | Menos trazabilidad. Marcado por el linter (S6258), aceptado. |
| **SSL a RDS en modo `require`** | RDS PostgreSQL 15 o superior rechaza conexiones sin cifrar (`rds.force_ssl`). | `require` cifra pero no valida el certificado del servidor. `verify-full` pide empaquetar el bundle de AWS. Mejora pendiente. |
| **Contraseña con `random_password`** | Evita credenciales en el código. | La contraseña queda en el state. Por eso el bucket está cifrado, versionado y sin acceso público. |
| **`force_destroy` / `force_delete` / sin snapshot final** | El entorno se construye y se destruye rápido. | En un sistema real se pierden datos. Es deliberado para el reto. |
| **`Resource: "*"` en la política del deployer** | Terraform necesita crear muchos tipos de recurso. | Permisos amplios para quien despliega, acotados en IAM y S3. Es un riesgo conocido. |
| **`ecr:GetAuthorizationToken` sobre `*`** | La acción no admite restringir el recurso. | Es el único `*` del execution role. El resto está limitado al repo, al log group y al secret. |
| **Autoscaling por CPU** | Target tracking al 60%, sin ajustar umbrales a mano. | En el arranque Spring Boot consume CPU y puede subir una task de más. |
| **Imagen con tag mutable `latest`** | Simple para un deploy manual. | No hay trazabilidad de versión ni rollback por tag. |

## Acceso a la base de datos

RDS no es público y su security group solo acepta tráfico de las tasks de ECS. Desde una laptop, `psql` no llega. Para inspeccionar datos hay que entrar por la red de la VPC (por ejemplo, un bastion con túnel SSM o ECS Exec). **No está implementado**: exigiría ampliar la política del deployer (instance profile, `iam:PassRole` hacia EC2 y SSM) y agregar el módulo correspondiente.

La contraseña vive en el secret `<name>/db` de Secrets Manager:

```bash
aws secretsmanager get-secret-value --secret-id nequi-dev/db --query SecretString --output text
```

## Costos

Estimado en `us-east-1` a precios de lista, para `dev` (verificar en AWS Pricing):

| Recurso | USD por hora |
|---|---|
| NAT gateway | 0.045 más datos procesados |
| ALB | 0.025 |
| RDS `db.t4g.micro` | 0.016 (free tier lo cubre si aplica) |
| Fargate 0.5 vCPU y 1 GB | 0.025 |
| IPv4 públicas | ~0.015 |
| **Total** | **~0.12** |

Probar unos minutos cuesta centavos. Olvidarlo un mes sale cerca de 85 USD. `prod` (multi-AZ y 2 tasks mínimo) ronda 0.19 por hora. Configurar un AWS Budget antes del primer deploy.

## Destruir

Orden obligatorio: **primero los environments, el bootstrap al final**. El bucket del bootstrap guarda el state de todos los environments y tiene `force_destroy`: borrarlo antes deja recursos huérfanos en AWS sin Terraform que los gestione.

```bash
scripts/destroy.sh dev
scripts/destroy.sh staging
scripts/destroy.sh prod
scripts/destroy.sh bootstrap        # se niega si algún environment aún tiene recursos
scripts/99-destroy-deployer.sh      # opcional: borra el usuario deployer
```

Después, revisar en la consola que no queden un NAT gateway, una Elastic IP o un ALB.

## Problemas comunes

| Síntoma | Causa y arreglo |
|---|---|
| Las tasks reinician y el log dice `no pg_hba.conf entry ... no encryption` | La app se conectó sin SSL. Debe tener `ssl-mode` en `require` (`DB_SSL_MODE`). |
| `CannotPullContainerError` | La imagen no está en ECR. Correr `scripts/deploy.sh <env>` o hacer el push y forzar el rollout. |
| `BucketAlreadyOwnedByYou` al aplicar el bootstrap | El bucket existe pero se perdió el state local de `bootstrap/`. Importarlo o borrarlo a mano. |
| `LimitExceeded` al crear la access key | El usuario ya tiene 2 keys. Borrar una con `aws iam delete-access-key`. |
| `init` pide el bucket | Falta `-backend-config="bucket=..."`. Los scripts lo pasan solos. |
