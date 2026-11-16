# Session 1 (2026-11-15)

Covered: resilience order and numbers, why writes are not retried, `switchIfEmpty`, `concatMap`, layers and types, Terraform state, Lombok, testing tools.

## Repeat next round (answer without looking)

- `defaultIfEmpty` vs `switchIfEmpty`, and why the `Supplier` in `Mono.error(() -> ...)`.
- Commit `5330a52` fixed the **query** (`OrderByIdAsc`), not the operator. `concatMap` only preserves upstream order.
- `flatMapSequential`: why not here (pool `MAX_SIZE = 15`, default concurrency 256).
- Domain is not "pure Java": it uses `Mono` and Lombok. It is free of Spring Web, R2DBC and Jackson.
- `StepVerifier` is for asserting signals, not debugging. `setName` is a setter.
- Empty vs not-found: "franchise without branches" returns an empty list, "branch without products" returns `product: null`.

## Gaps found

- No unique constraint on any `name`; a retried insert would duplicate silently (not in the spec).
- `DataIntegrityViolationException` (FK race) is not mapped in `ErrorMapper`, so it answers 500.
- `schema.sql` uses `CREATE TABLE IF NOT EXISTS`: adding a column needs an `ALTER`, there is no migration tool.
- No test against a real database (B2).
- Plain `pitest` fails at the root project; run per module (`:usecase:pitest`).
- The tests were not written by the candidate: pick 2 or 3 and be able to explain them line by line.

## Done

- Removed setters and dead fields (`branches`, `products`) from the models and DTOs; `openapi.yaml` updated.
- `.terraform.lock.hcl` removed from `.gitignore`.
- Mutants killed: `usecase` 83/83, `r2dbc-postgresql` 45/45, `reactive-web` 72/72. New tests: CORS, routes, `deleteById`, franchise adapter.

## Pending

- [ ] Open questions: 13b (`verify(..., never())`), 15b (which blocker first), 16 (why no `Origin` means no CORS).
- [ ] Topics not yet touched: Docker, IAM, Auto Scaling, `zip`/`merge`, SOLID letters, Java 17 sealed classes and text blocks.
  (All of these were covered in `session-2.md`. The open questions 13b, 15b and 16 are still unanswered.)

