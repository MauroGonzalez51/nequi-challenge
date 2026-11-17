# Next round (round 9)

Rewrite after every session. Only what is pending lives here. History is in `session-1.md` to `session-8.md`, the plan and blockers in `SPARRING.md`.

## Rules for the next round

1. **Random order** by rubric area. No topic twice in a row.
2. **Show the code.** Paste the snippet, the candidate explains, never recalls. Verify the snippet against the repo before asking.
3. **Grade logic, not chat syntax.** Read the whole answer before saying "not answered".
4. After each answer: grade, then 2 lines of the correct explanation. No repeated drilling inside a round.

## Retired (passed, do not ask again unless a new mistake shows up)

Rollback reason (N-1 still says `latest`), `@Setter` and `toBuilder`, breaker in flight and half-open, branch/product ownership and idempotency, FK race, layers and ISP.

## Priority

1. **Rollback, pieces:** one task definition revision per tag. The breaker is `deployment_circuit_breaker` on `aws_ecs_service`, not ECR.
2. **VPC Link:** the API Gateway enters the internal ALB. Nothing "leaves" the VPC.
3. **NAT, one line:** one NAT, AZ failure, new tasks cannot pull from ECR or read the secret, running tasks keep working. Production: one per AZ or VPC endpoints.
4. **Left out on purpose:** always say "throwaway environment, no sensitive data" with the auth answer.
5. **`minimum-number-of-calls`:** effective minimum is `min(100, sliding-window-size)` = 10. Without a search.
6. **Two translation sites:** `PersistenceResilience:89` (Spring to domain), `ErrorMapper:39` (domain to 409).
7. **Secret path:** env var in the container (not memory), `-out` plan holds it, Secrets Manager is the source.
8. **Top-stock N+1:** still in the code. Be able to say 6.6s, the one-query fix, and that it is in the README.

## Still open

- `lower(btrim(name))` in the index and the `btrim` limit (spaces only).
- `flatMap` vs `then` in the patch use cases.
- `Mono.justOrEmpty(name.trim())` NPE vs `.map(String::trim)`.
- Validation layers: `@NotNull` at the edge, `isBlank()` in the domain.
- BlockHound outside tests: not checked.

## Areas not probed yet

- A: pagination.
- E: bulkhead, rate limiter, stale fallback.
- F: `WebTestClient` handler test (what is real), virtual time, JaCoCo vs mutation.
- G: scaling writes, 10k products in one request, Postgres vs DynamoDB.

## Blockers still open

B1 (deploy `dev`), B3 (README from a clean clone, check it says how to start Postgres), B4 (`git log` review), B5 (grep sweep). B2 half closed. Before the day: fix the stale lock row in `SPARRING.md` section 2.5 (lock files are versioned).

## Pending changes to the specs

- Add the 409 scenario for `DataConflictException` (also FK violation) and one for duplicate names in `specs/`. Tick `tasks.md` only after verifying.
- Optional: a clearer 409 message in `ErrorMapper`.
