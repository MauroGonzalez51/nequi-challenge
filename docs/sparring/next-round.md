# Next round (round 6)

Rewrite after every session. Only what is pending lives here. History is in `session-1.md` to `session-5.md`, the plan and blockers in `SPARRING.md`.

## Rules for the next round

1. **Random order** by rubric area (bank in `session-5.md` style, areas A to G). No topic twice in a row.
2. **Show the code.** Paste the snippet, the candidate explains, never recalls.
3. **Grade logic, not chat syntax.**
4. After each answer: grade, then 2 lines of the correct explanation. No repeated drilling inside a round.

## Retired (passed, do not ask again unless a new mistake shows up)

Text blocks, `switchIfEmpty` vs `defaultIfEmpty`, `zip` vs `merge`, multi-stage Docker (definition), trust vs permissions policy, why writes are not retried, timeout vs retry order, `Idempotency-Key` idea, top-stock tie-break, `concatMap` vs `flatMap`, network path and SG chain, cold publisher and dropped `Mono`, `minimum-number-of-calls`, unit vs integration, blocking in the event loop, `record` vs `@Data`.

## Still open (ask once, code shown)

- **N+1 in top-stock:** count the queries for 1000 branches (N+2) and give the grouped-query fix. Ask with `GetTopStockProductPerBranchUseCase` pasted.
- **Rollback mechanics:** why `latest` is hard to roll back (old image goes untagged) and the three-piece fix (immutable tags, task definition revision, ECS deployment circuit breaker).
- **Curveball 13:** "which part do you trust least". Answer with the template: X, evidence Y, plan Z.
- **Auth bypass:** API Gateway in front of a public ALB can be bypassed. Why, and the fix (internal ALB plus VPC Link, or restrict the SG).
- **Unique name race:** why `findByName` first is not enough (check-then-insert) and where the 409 is built (`PersistenceResilience` to `ErrorMapper`).
- **`expectError` vs `expectErrorSatisfies`.**
- **`@Setter` on domain models:** who needs them (the scaffold `ObjectMapperImp`, not R2DBC) and the alternatives.
- **From round 4** (`session-4.md`): latency arithmetic P3a, `sliding-window-size`, `wait-duration-in-open-state` 1s vs 5 min, `readMany` buffer and `concat` vs `zip`.
- **Old unanswered:** 13b (`verify(..., never())`), 15b (which blocker first), 16 (why no `Origin` means no CORS). Claude rewrites them from the code before asking.

## Areas not probed yet

- A: rename endpoints (`PUT`/`PATCH`, idempotent?), validation layers (negative stock, blank name), delete product from another branch, plain `Long` FKs, pagination.
- B: `subscribeOn` vs `publishOn`, `onErrorResume` vs `onErrorReturn` vs `onErrorMap`, `doOnNext` vs `map`, backpressure with `collectList()`.
- C: module graph from `build.gradle`, Entity vs Model cost, ISP in gateways, ArchUnit.
- D: remote state and `use_lockfile`, drift, module design (`modules/ecs`), secrets at each moment, cost.
- E: in-flight requests when the breaker opens, bulkhead, rate limiter, stale fallback.
- F: `WebTestClient` handler test (what is real), virtual time, JaCoCo vs mutation, README from a clean clone, commit history.
- G: "one more week", scaling writes, 10k products in one request, Postgres vs DynamoDB, line-by-line drill of a random file.

## Blockers still open

B1 to B5 in `SPARRING.md` section 1. Deploy `dev` the evening before and follow the README from a clean clone.

Pending from the `DataConflictException` change: add the 409 scenario in `specs/` and tick `tasks.md` only after verifying.
