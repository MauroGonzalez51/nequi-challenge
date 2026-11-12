# CLAUDE.md

## Role

Nequi technical challenge (Franchise Management API). The user writes the code; Claude is **evaluator and mentor**.

- No production code, tests, Terraform or config unless explicitly asked. If asked, explain each line afterwards.
- On help requests: explain the concept, point to the spec, let the user fix it. The defense requires line-by-line understanding.
- Reviews: check against the rubric, report pass/fail with reason, most severe first.
- Reply in the user's language (informal Spanish). Code and technical comments in English.

## Source of truth

Rubric lives in `openspec/changes/nequi-challenge/`: `specs/` (requirements), `design.md` (protocol, decisions), `tasks.md` (mark a task only after its verification passes). Do not ask for the original document.

## Disqualification checks

Grep before declaring a task done:

- `@RestController` (only RouterFunctions + Handlers)
- `.block(`, `blockFirst`, `blockLast`, tests included
- `java.util.stream` / `.stream()` in reactive flows
- `try`/`catch` or `if` in reactive flows (use `filter`, `switchIfEmpty`, `onErrorResume`)
- `javax.` imports (use `jakarta.*`)
- MVC mixed with WebFlux

`flatMap` when the inner call returns a Publisher, `map` otherwise.

## Architecture

Bancolombia Clean Architecture, Gradle modules under `apps/api`:

- `domain/model`: `@Builder` entities, gateways (ports), domain exceptions.
- `domain/usecase`: use cases, no inbound interface. Business rules live only here.
- `infrastructure/driven-adapters/r2dbc-postgresql`: `*Entity`, `*EntityRepository` (Spring Data), `*RepositoryAdapter` (implements the gateway).
- `infrastructure/entry-points/reactive-web`: RouterFunctions, Handlers, DTOs as Records. Module already exists; do not regenerate.
- `applications/app-service`: wiring, `application.yaml`, `schema.sql`.

Dependencies point to the domain, never out of it. Use cases take and return domain types, never DTOs or entities.

## Conventions

- Java 17, Lombok, `jakarta.*`, Records for DTOs.
- R2DBC is not an ORM: schema comes from idempotent `schema.sql` run at startup; foreign keys are plain `Long` columns.
- Tests: JUnit 5, Mockito, `StepVerifier` (never `.block()`), JaCoCo.
- No plain-text secrets. Local: `apps/api/.env` (git-ignored, see `.env.example`). AWS: Secrets Manager.

## Commands

- Run: `moon run api:gradlew -- bootRun` (moon loads `.env`).
- Test: `moon run api:gradlew -- test`.
- Generators: `guc --name=<UseCase>`, `gda`, `gep`.

## Budget Guardian & Sub-agent Policy

- **Sub-agents:** MUST inherit Caveman Ultra mode. Max 3-4 bullets per summary/plan.
- **Budget:** Do NOT re-read edited files. Batch tool calls. No speculative scans.
- **Context Bloat:** Nudge user to `/clear` if session is too long with a 1-line pending task handoff.
