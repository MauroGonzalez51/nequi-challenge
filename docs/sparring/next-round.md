# Next round (round 8)

Rewrite after every session. Only what is pending lives here. History is in `session-1.md` to `session-7.md`, the plan and blockers in `SPARRING.md`.

## Rules for the next round

1. **Random order** by rubric area. No topic twice in a row.
2. **Show the code.** Paste the snippet, the candidate explains, never recalls.
3. **Grade logic, not chat syntax.** Read the whole answer before saying "not answered".
4. After each answer: grade, then 2 lines of the correct explanation. No repeated drilling inside a round.

## Retired (passed, do not ask again unless a new mistake shows up)

Everything in the previous list, plus: curveball 12 (N+1 as the one-week pick, 6.6s), curveball 13 (`@Setter` cause and plan).

## Priority

1. **Rollback, third time:** why rolling back to N-1 fails with `:latest` (N-1 still says `latest`, now the new image). Then the three pieces: ECR `IMMUTABLE`, one task definition revision per tag, `deployment_circuit_breaker`.
2. **Auth bypass:** `internal = true` on `aws_lb` plus `aws_apigatewayv2_vpc_link`. Why SG restriction does not work, why `authenticate-cognito` does not fit (HTTPS, domain, browsers).
3. **Say what is left out on purpose** in every curveball (auth: public ALB, throwaway env, no sensitive data).
4. **`@Setter` evidence:** file and line (`ObjectMapperConfig.java:13`), and `toBuilder()` vs `builder()`.
5. **Top-stock fix, full answer:** `ProductRepository`, `LEFT JOIN`, `DISTINCT ON ... ORDER BY branch_id, stock DESC, id`, 2 queries total.

## Still open from 2026-11-16

- `lower(btrim(name))` in the index and the `btrim` limit (spaces only).
- `flatMap` vs `then` in the patch use cases (`then` drops the upstream value).
- `Mono.justOrEmpty(name.trim())` NPE vs `.map(String::trim)`.
- Validation layers: `@NotNull` at the edge, `isBlank()` in the domain.
- BlockHound outside tests: not checked.

## Areas not probed yet (pick at most two)

- A: rename endpoints (idempotent?), delete product from another branch, plain `Long` FKs, pagination.
- C: module graph from `build.gradle`, Entity vs Model cost, ISP in gateways, ArchUnit.
- D: remote state and `use_lockfile`, drift, module design (`modules/ecs`), secrets at each moment, cost.
- E: in-flight requests when the breaker opens, bulkhead, rate limiter, stale fallback, `sliding-window-size`.
- F: `WebTestClient` handler test (what is real), virtual time, JaCoCo vs mutation, README from a clean clone, commit history.
- G: scaling writes, 10k products in one request, Postgres vs DynamoDB.

## Blockers still open

B1, B3, B4 and B5 in `SPARRING.md` section 1. B2 is half closed (Testcontainers test exists, cloud run still missing). Deploy `dev` the evening before and follow the README from a clean clone.

## Pending changes to the specs

- Add the 409 scenario for `DataConflictException` and one for duplicate names (per parent, ignoring case and surrounding spaces) in `specs/`. Tick `tasks.md` only after verifying.
- Optional: a clearer 409 message in `ErrorMapper`.
