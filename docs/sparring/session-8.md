# Session 8

Day before the defense. 11 questions, candidate answered fast and informally. Two evaluator errors this session (see "Learned").

| # | Area | Topic | Grade | Note |
|---|---|---|---|---|
| 1 | D | Rollback with mutable `latest` (third time) | Pass with gaps | Reason right (N-1 still says `latest`), `IMMUTABLE` tags, descriptive tags (`semver-commit`), fire-fighting by retagging `latest` plus `--force-new-deployment` works. Missed: one task definition revision per tag. Wrong place: the deployment circuit breaker is `deployment_circuit_breaker` on `aws_ecs_service`, not on ECR. Check the lifecycle policy for untagged images (the bad image loses its tag). |
| 2 | D | Auth bypass | Partial | Bypass right (public ALB reachable directly), `internal = true` plus private subnets. Missed `aws_apigatewayv2_vpc_link`. SG reasoning half right: the real reason is API Gateway has no fixed IPs, so an allowlist of AWS ranges lets any account's gateway in. |
| 3 | G | Top-stock N+1 | Skipped | Candidate confirmed N+1 is still in the code (not fixed). Must be defended as a known trade-off: 6.6s, one-query fix with `DISTINCT ON`, left out for scope. Check it is in the README. |
| 4 | C | `@Setter`, `toBuilder` | Pass | Runtime failure, silent nulls, `ObjectMapperImp` maps through setters. `toBuilder()` copies and overrides, right for `PATCH`. Detail: needs `@Builder(toBuilder = true)`. |
| 5 | E | Breaker in flight, window, half-open | Partial | In-flight call finishes, new call gets `CallNotPermittedException` mapped to `ServiceUnavailableException`, half-open rejects extra calls: pass. Window: first said it opens, corrected after a search. Without `minimum-number-of-calls`, effective minimum is `min(100, sliding-window-size)` = 10, so 4 failed calls stay CLOSED. |
| 6 | A | Product from another branch, idempotency | Pass | Pertenencia via `filter` plus `switchIfEmpty`, 404. PATCH idempotent. Missed the verdict on DELETE: idempotent by state, 404 on the second call. |
| 7 | A | FK and check-then-insert | Pass | Evaluator error: `schema.sql` does have `REFERENCES` (real FKs). Race is closed by the FK, and no endpoint deletes branches or franchises. |
| 8 | C | FK violation mapping | Partial | `PersistenceResilience:89` maps every `DataIntegrityViolationException` to `DataConflictException`, `ErrorMapper:39` to 409. Candidate said "generic error", defensible, but listed 4 places instead of 2. |
| 9 | F | Clean clone, commit history (B3, B4) | Partial | Generic answer. Did not know what B3 is. Likely first failure: no Postgres running, check the README says how to start one. |
| 10 | D | Secret at each moment, lock file | Partial | State in clear (S3 encrypted), logs clean. Wrong: container holds it as an env var, not "memory"; plan on screen is `(sensitive value)` but a `-out` plan file holds it. Missed Secrets Manager itself. Lock files are versioned (10 of them, `modules/*` ones are not needed). |
| 11 | C | Layers, DynamoDB swap, ISP | Pass | Entity to model in the adapter by `ObjectMapper`, only infrastructure changes (also `app-service` wiring and `PersistenceResilience`), ISP by role rather than per use case. |
| 12 | G | Auth and NAT, "left out on purpose" | Partial | Auth: out of scope, risk, `internal` ALB plus API Gateway with Cognito. Missed "throwaway environment, no sensitive data". VPC Link direction wrong (gateway gets in, nothing goes out). NAT: not answered. |

## Learned this session

- Evaluator error: the `SPARRING.md` row on `.terraform.lock.hcl` is stale, and question 6 wrongly said there is no FK in `schema.sql`. Verify the code before asking.
- Candidate checks the code when unsure (good), but `minimum-number-of-calls` and the NAT risk must come without a search.
- Candidate prefers short informal answers, grades on logic.
