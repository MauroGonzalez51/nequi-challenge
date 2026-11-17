# Entrar a la base de datos (RDS)

RDS es privado: su security group solo acepta tráfico del SG de ECS (`nequi-<env>-ecs`). Desde la laptop se entra con un túnel SSM a través de una EC2 temporal. Todo lo hace `packages/iac/scripts/start-tunnel.sh`, sin tocar el Terraform.

```
DBeaver ─▶ localhost:15432 ══ SSM ══▶ EC2 temporal (SG nequi-<env>-ecs) ─▶ RDS :5432
```

Los scripts se corren desde `packages/iac`.

## Requisitos locales

- AWS CLI v2 y `jq`.
- `session-manager-plugin` (Arch: `aws-session-manager-plugin`, AUR). Sin esto `aws ssm start-session` falla.

## Preparación (una vez por cuenta, con admin)

Con el perfil admin (root o un admin IAM, el mismo de `init.sh`), no con `nequi-tf`:

```bash
# Cuenta nueva: init.sh corre 01, 02, 03 y el bootstrap.
AWS_PROFILE=<admin> scripts/init.sh

# Cuenta que ya tenía init: solo falta el paso 03.
AWS_PROFILE=<admin> scripts/03-create-tunnel-user.sh
```

`03` crea, de forma idempotente:

| Qué | Para qué |
|---|---|
| Rol + instance profile `nequi-db-bastion` | Lo asume la EC2. Solo tiene `AmazonSSMManagedInstanceCore`. |
| Usuario IAM `nequi-tunnel` en el grupo `nequi-tunnelers` | Quien corre `start-tunnel.sh`. La política (`policies/tunnel-policy.json`) va en el grupo: una política inline de usuario tope en 2048 bytes y esta pesa ~2100. |
| Access key en el perfil local `nequi-tunnel` | `start-tunnel.sh` lo usa por defecto. El secret no se imprime. |

Un usuario IAM admite máximo 2 access keys. Si `03` lo avisa, borrar una vieja con `aws iam delete-access-key`.

**Por qué el rol lo crea `03` y no `start-tunnel.sh`:** si el usuario del túnel pudiera crear roles, podría crearse uno con `AdministratorAccess` y lanzar una EC2 con él. Así, `nequi-tunnel` solo puede pasar ese rol fijo a EC2 y nada más de IAM.

## Cada vez que se necesite la base

```bash
scripts/start-tunnel.sh dev          # dev | staging | prod. Segundo argumento opcional: puerto local (15432).
AWS_PROFILE=otro scripts/start-tunnel.sh dev   # si no se quiere el perfil nequi-tunnel
```

El script:

1. Lanza una `t3.nano` en una subnet privada con el SG de ECS.
2. Espera al agente SSM (1 a 2 min).
3. Lee el secret `nequi-<env>/db` e imprime host, puerto, base y usuario. La contraseña va a un archivo `mktemp` (`0600`, formato pgpass) cuya ruta imprime. Se borra al salir.
4. Abre el port-forward y deja la terminal colgada.

**Ctrl+C o SIGTERM terminan la EC2.** Un segundo Ctrl+C durante la limpieza se ignora a propósito.

## DBeaver

Nueva conexión PostgreSQL:

| Campo | Valor |
|---|---|
| Host | `localhost` |
| Port | `15432` (o el que se pasó al script) |
| Database / Username | los que imprime el script |
| Password | `cut -d: -f5 <archivo>` y copiar de la pantalla, o `PGPASSFILE=<archivo> psql ...` (el script imprime el comando) |

Pestaña **SSL**: "Use SSL" en modo `require`. RDS PostgreSQL 15 o superior rechaza conexiones sin cifrar. Con `verify-full` falla: el certificado es del host de RDS, no de `localhost`.

## Por qué un archivo y no el portapapeles

Los gestores de portapapeles (cliphist, klipper, etc.) guardan historial, a veces en disco, y cualquier app puede leerlo. Imprimir la contraseña la deja en el scrollback. El archivo se crea con `umask 077` (nace `0600`, sin ventana de lectura) y lo borra el mismo trap que termina la EC2. Si el script muere con `SIGKILL` queda en `/tmp`: `0600`, y `/tmp` suele ser tmpfs (se borra al reiniciar). Borrarlo a mano.

## Cuidados

- La EC2 no está en el state de Terraform y su ENI vive en el SG y la subnet que Terraform administra. Si queda viva, `destroy.sh` fallaría con `DependencyViolation`. Por eso `destroy.sh <env>` busca `nequi-<env>-db-bastion` y la termina antes de destruir.
- Si `start-tunnel.sh` murió sin limpiar (`kill -9`, apagón), volver a correrlo: barre la EC2 anterior del mismo environment al arrancar. O correr `destroy.sh`.
- El rol `nequi-db-bastion` persiste entre corridas. Es gratis e inofensivo. `99-destroy-iam.sh` borra el rol, el instance profile y el usuario `nequi-tunnel`.
- Costo: `t3.nano` cuesta centavos por hora, y solo existe mientras el túnel está abierto.
- `prod` es la base real: cuidado con escrituras.

## Si algo falla

| Síntoma | Causa y arreglo |
|---|---|
| `session-manager-plugin not found` | Instalar el plugin (ver requisitos). |
| `Profile 'nequi-tunnel' is invalid` | Falta la preparación. Correr `03` con admin. |
| `UnauthorizedOperation` al lanzar la EC2 | La política `tunnel-policy.json` no cubre la llamada. Re-correr `03` para refrescarla y revisar el mensaje. |
| `Security group nequi-<env>-ecs not found` | El environment no está desplegado en la región (`REGION`, por defecto `us-east-1`). |
| `SSM agent never came online` | La subnet privada no sale por el NAT, o el rol no está en el instance profile. Re-correr `03`. |
| El puerto local está ocupado | Pasar otro: `scripts/start-tunnel.sh dev 25432`. |
