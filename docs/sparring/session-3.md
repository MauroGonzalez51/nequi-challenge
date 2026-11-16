# Session 3 (2026-11-16)

Covered: `Mono.error` eager vs lazy, `zip` in `ProductHandler.delete`, `flatMapSequential` vs the pool, trust vs permissions policy, `ErrorMapper` order and the `DataIntegrityViolationException` gap, sealed classes, text blocks, `transformDeferred`, Terraform IAM `*`, multi-stage Docker.

## Results

- Pass: trust vs permissions policy (definition), where the translation belongs (`ErrorMapper` maps domain to HTTP, persistence translates), `GetAuthorizationToken` and deployer `*` (after correction).
- Partial: `Mono.error` eager vs lazy (said the stack trace is lost, it is captured at the wrong place), `zip` (empty and error semantics right, missed which error wins), `flatMapSequential` vs pool (set concurrency to 15, should be well under the pool), `transformDeferred` (definition right, conclusion reversed), sealed classes (definition only), text blocks (definition only), multi-stage Docker (missed that size gain is about zero here, real gain is a pinned JDK).
- No answer, explained: `ErrorMapper` catch-all order.

## Decisions made

- `DataIntegrityViolationException` becomes `DataConflictException` (domain), answered as `409` with `DATA_CONFLICT`. Translation lives in `PersistenceResilience.withCircuitBreaker`, after the breaker. Not retried, not a breaker failure.
- `409` documented only on `addBranch` and `addProduct` in `openapi.yaml` (the endpoints with a foreign key to the parent).
- Candidate's first idea was 404 (`RECORD_NOT_FOUND`). Defensible only if limited to SQLState 23503 and because no endpoint deletes a parent. 409 chosen as the safe option.

## Changes made in code

- New `DataConflictException`, `onErrorMap` in `PersistenceResilience`, `onErrorResume` in `ErrorMapper`, `openapi.yaml` updated.
- Tests: `PersistenceResilienceTest` (two updated), `ErrorMapperTest`, `BranchHandlerTest` and `ProductHandlerTest` (one 409 case each).

## Still open

- Scenario for the 409 in `specs/` and `tasks.md` tick, after verifying.
- Pending topics are in `next-round.md`.
