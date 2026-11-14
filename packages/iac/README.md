# Infraestructura (Terraform)

Infraestructura AWS del reto: ECS Fargate, ECR, ALB, RDS PostgreSQL, Secrets Manager y autoscaling.
Este README crece con cada flujo. Por ahora cubre el **acceso a AWS**.

## Estructura

```
packages/iac/
├── policies/   # JSON de IAM (permisos del usuario que despliega)
├── scripts/    # scripts numerados, uno por flujo
├── bootstrap/  # (pendiente) bucket S3 del state remoto
├── modules/    # (pendiente) network, ecr, database, alb, iam, ecs
└── envs/       # (pendiente) dev, staging, prod
```

## Acceso a AWS sin usar root

Root no se usa para desplegar. Se crea un usuario IAM `terraform-deployer` con permisos acotados.
El usuario **vive en AWS**; en cada máquina solo existe una access key en `~/.aws/credentials`.

| Script | Cuándo | Qué hace |
|---|---|---|
| `scripts/01-create-deployer.sh` | Una vez por cuenta AWS | Crea grupo, política y usuario. Idempotente. |
| `scripts/02-create-access-key.sh` | Una vez por máquina | Crea una access key y la guarda en el perfil `nequi-tf`. No imprime el secret. |
| `scripts/99-destroy-deployer.sh` | Al terminar el reto | Borra keys, usuario y grupo. Antes, `terraform destroy` en cada env. |

### Flujo desde cero

1. Autenticarse con root (o un admin) en el CLI y correr `scripts/01-create-deployer.sh`.
2. Correr `scripts/02-create-access-key.sh`.
3. Desde ahora trabajar solo con el perfil:
   ```bash
   export AWS_PROFILE=nequi-tf
   aws sts get-caller-identity   # debe mostrar .../user/terraform-deployer, no root
   ```
4. Opcional, en la consola con root: IAM > Users > `terraform-deployer` > Security credentials.
   Habilitar acceso a consola (password generado, guardado en el gestor de contraseñas) y activar MFA.
   El password sirve solo para la web; Terraform usa la access key.

### Otra máquina

No se recrea el usuario. Autenticarse con root o con el usuario IAM y correr `scripts/02-create-access-key.sh`.
Un usuario IAM admite máximo 2 access keys: borrar la vieja en IAM si el script falla.

### Qué cubre la política

`policies/deployer-policy.json` separa dos niveles:

- **Quien despliega:** permisos por servicio (`ecs:*`, `rds:*`...) porque Terraform crea muchos tipos de recurso.
  IAM queda acotado a roles y políticas con prefijo `nequi-*`, `iam:PassRole` solo hacia `ecs-tasks.amazonaws.com`,
  y S3 solo a buckets `nequi-tfstate-*`.
- **La app en runtime:** roles con mínimo privilegio (módulo `iam`, pendiente). Es lo que evalúa la rúbrica.

### Seguridad de la key

- Nunca va al repo, a un `.tfvars` ni a variables del contenedor (`.gitignore` ya excluye state y tfvars con secretos).
- El script deja `~/.aws/credentials` con permisos `600`.
- Si se filtra: IAM > Users > Security credentials > desactivar y borrar la key.
