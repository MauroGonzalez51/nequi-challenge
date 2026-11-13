# API de gestión de franquicias

Reto técnico Nequi. API reactiva para administrar **franquicias**, sus **sucursales** y los **productos** de cada sucursal.

Una franquicia tiene muchas sucursales y una sucursal tiene muchos productos.

## Stack

| Pieza | Tecnología |
|---|---|
| Lenguaje | Java 17 |
| Framework | Spring Boot 4.1.2, Spring WebFlux (Reactor Netty) |
| Persistencia | PostgreSQL con Spring Data R2DBC |
| Resiliencia | Resilience4j (circuit breaker) y operadores de Reactor (timeout, retry) |
| Documentación | OpenAPI 3 con Swagger UI (springdoc 3.2.0) |
| Pruebas | JUnit 5, Mockito, `StepVerifier`, JaCoCo |
| Build | Gradle, orquestado con [Moon](https://moonrepo.dev) |

## Arquitectura

Clean Architecture (hexagonal) siguiendo el scaffold de Bancolombia. El código está en `apps/api`:

```
apps/api
├── domain
│   ├── model        entidades (@Builder), puertos (gateways) y excepciones de dominio
│   └── usecase      casos de uso; aquí viven todas las reglas de negocio
├── infrastructure
│   ├── driven-adapters/r2dbc-postgresql   adaptadores de los puertos, repositorios y resiliencia
│   └── entry-points/reactive-web          RouterFunctions, handlers y DTOs (records)
└── applications/app-service               arranque, application.yaml y schema.sql
```

Reglas que se respetan:

- Las dependencias apuntan hacia el dominio. El dominio no importa Spring, R2DBC ni nada de infraestructura.
- Los puertos son interfaces del dominio. Los adaptadores están en infraestructura.
- Los casos de uso reciben y devuelven tipos de dominio, nunca DTOs ni entidades de base de datos.
- Solo RouterFunctions y handlers. No hay `@RestController`.
- Entidades con Lombok `@Builder`, DTOs como Java Records y `jakarta.*` en lugar de `javax.*`.

## Requisitos

- Java 17
- [Moon](https://moonrepo.dev) (opcional, ver abajo cómo correr sin él)
- Una base PostgreSQL accesible

## Configuración

La aplicación lee la conexión de variables de entorno. Para correr en local:

```bash
cp apps/api/.env.example apps/api/.env
```

y editá `apps/api/.env` con los datos de tu base:

| Variable | Ejemplo |
|---|---|
| `DB_HOST` | `localhost` |
| `DB_PORT` | `5432` |
| `DB_NAME` | `nequi-challenge` |
| `DB_USERNAME` | `tu_usuario` |
| `DB_PASSWORD` | `tu_clave` |

`apps/api/.env` está en `.gitignore`: las credenciales no se versionan.

Las tablas se crean solas al arrancar con `schema.sql`, que usa `CREATE TABLE IF NOT EXISTS`, así que se puede ejecutar varias veces. Solo hace falta que la base exista.

Otras propiedades en `application.yaml`:

- `server.port`: puerto HTTP, 8080 por defecto. Se puede cambiar con la variable `SERVER_PORT`.
- `cors.allowed-origins`: orígenes permitidos por CORS.
- `resilience.persistence.*`: ver [Resiliencia](#resiliencia).

## Ejecutar

Con Moon, que carga `apps/api/.env` por vos:

```bash
moon run api:gradlew -- bootRun
```

Sin Moon, con las variables exportadas:

```bash
cd apps/api
DB_HOST=localhost DB_PORT=5432 DB_NAME=nequi-challenge DB_USERNAME=tu_usuario DB_PASSWORD=tu_clave \
  ./gradlew bootRun
```

La API queda en `http://localhost:8080`. El estado de salud está en `/actuator/health`.

## API

Documentación interactiva: **`http://localhost:8080/swagger-ui.html`**.
La especificación OpenAPI está en `apps/api/applications/app-service/src/main/resources/static/openapi.yaml` y se sirve en `/openapi.yaml`.

| Método | Ruta | Qué hace |
|---|---|---|
| `POST` | `/api/franchises` | Crea una franquicia |
| `PATCH` | `/api/franchises/{franchiseId}/name` | Cambia el nombre de una franquicia |
| `GET` | `/api/franchises/{franchiseId}/top-stock-products` | Producto con más stock de cada sucursal de la franquicia |
| `POST` | `/api/franchises/{franchiseId}/branches` | Agrega una sucursal a una franquicia |
| `PATCH` | `/api/branches/{branchId}/name` | Cambia el nombre de una sucursal |
| `POST` | `/api/branches/{branchId}/products` | Agrega un producto a una sucursal |
| `DELETE` | `/api/branches/{branchId}/products/{productId}` | Elimina un producto de una sucursal |
| `PATCH` | `/api/products/{productId}/stock` | Modifica el stock de un producto |
| `PATCH` | `/api/products/{productId}/name` | Cambia el nombre de un producto |

Un ejemplo completo:

```bash
curl -X POST localhost:8080/api/franchises -H 'Content-Type: application/json' -d '{"name":"Nequi"}'
# {"id":1,"name":"Nequi","branches":[]}

curl -X POST localhost:8080/api/franchises/1/branches -H 'Content-Type: application/json' -d '{"name":"Cartagena"}'
# {"id":1,"name":"Cartagena","products":[]}

curl -X POST localhost:8080/api/branches/1/products -H 'Content-Type: application/json' -d '{"name":"Empanada","stock":20}'
# {"id":1,"name":"Empanada","stock":20}

curl localhost:8080/api/franchises/1/top-stock-products
# [{"id":1,"name":"Cartagena","product":{"id":1,"name":"Empanada","stock":20}}]
```

Comportamientos a tener en cuenta:

- En `top-stock-products`, una sucursal sin productos aparece igual, con `"product": null`. Si dos productos empatan en stock gana el de menor id. Una franquicia sin sucursales devuelve `[]`.
- Los `PATCH` son idempotentes: enviar el valor que ya tiene el recurso no cambia nada y responde `200`.
- El stock es un entero mayor o igual a 0. Un decimal como `1.5` se rechaza.
- Los campos `branches` de la franquicia y `products` de la sucursal son siempre listas vacías por ahora.

### Errores

Todos los errores salen como `{"message": "..."}`:

| Código | Cuándo |
|---|---|
| `400` | JSON inválido, falta el body, un campo obligatorio no viene, un id no es numérico, o un valor no es válido (nombre vacío, stock negativo) |
| `404` | La franquicia, la sucursal o el producto no existe. Un producto de otra sucursal también da `404` |
| `503` | La base de datos no está disponible. El mensaje es fijo y no filtra detalles internos |

### Validación en dos capas

- **En el borde** (Bean Validation, `jakarta.validation`): que los campos obligatorios vengan. Responde `Field 'name' is required`.
- **En el dominio**: los valores. Nombre no vacío, stock mayor o igual a 0, ids que existen. Las reglas de negocio viven solo en los casos de uso.

## Resiliencia

Toda llamada a la base pasa por `PersistenceResilience`, con este orden de adentro hacia afuera: **timeout → retry con backoff → circuit breaker**.

El breaker va por fuera para contar un fallo por operación completa y, cuando está abierto, fallar al instante sin reintentar.

- Las **lecturas** se reintentan. Las **escrituras no**: un insert cuya respuesta se perdió por un timeout puede haberse confirmado, y reintentarlo duplicaría la fila. Las escrituras sí llevan timeout y breaker.
- Solo cuentan como fallo los errores de infraestructura (conexión caída, timeout). Un error de integridad como una clave duplicada significa que la base respondió, así que no se reintenta ni abre el circuito.
- Lo que falla porque la base no está usable sale como `ServiceUnavailableException` y se responde `503`.

Valores por defecto, en `application.yaml`:

| Propiedad | Valor |
|---|---|
| `resilience.persistence.timeout` | `2s`, por intento |
| `resilience.persistence.retry.max-attempts` | `3`, contando el primer intento |
| `resilience.persistence.retry.backoff` | `200ms`, creciente |
| `resilience.persistence.circuit-breaker.failure-rate-threshold` | `50` (%) |
| `resilience.persistence.circuit-breaker.sliding-window-size` | `10` |
| `resilience.persistence.circuit-breaker.minimum-number-of-calls` | `5` |
| `resilience.persistence.circuit-breaker.wait-duration-in-open-state` | `10s` |
| `resilience.persistence.circuit-breaker.permitted-number-of-calls-in-half-open-state` | `3` |

## Pruebas y cobertura

```bash
moon run api:gradlew -- test
```

Las pruebas usan JUnit 5, Mockito y `StepVerifier`. Nunca se usa `.block()`. Cubren los casos de uso, los handlers (con `WebTestClient`), los adaptadores y cada patrón de resiliencia.

JaCoCo genera el reporte de cada módulo al correr las pruebas, en `build/reports/jacocoHtml/index.html` y `build/reports/jacoco.xml` dentro del módulo. Por ejemplo, `apps/api/domain/usecase/build/reports/jacocoHtml/index.html`.

## Decisiones de diseño

- **Top-stock con consultas derivadas.** El caso de uso busca las sucursales de la franquicia y, por cada una, su producto con más stock con `findFirstByBranchIdOrderByStockDescIdAsc`. Son 1 + N consultas, simple de leer y de probar. `concatMap` mantiene el orden de las sucursales.
- **`collectList()` en el handler de top-stock.** Cede el control de flujo del backpressure: pide demanda ilimitada y junta el resultado en memoria. Se eligió porque el resultado es pequeño y porque así un error de dominio, como el 404, se convierte en respuesta antes de enviar el status. Con un resultado enorme habría que devolver el `Flux` directamente y aceptar perder ese mapeo de errores.
- **Un solo punto de traducción de errores.** `ErrorMapper` convierte las excepciones de dominio en códigos HTTP, y es el único lugar que sabe de HTTP. Las capas de entrada no conocen Resilience4j ni R2DBC: la resiliencia traduce a una excepción de dominio en el borde del adaptador.
- **Lectura del request en un solo lugar.** `RequestParams` parsea los ids de la ruta, lee el body y lo valida, así los handlers no repiten esa lógica.
