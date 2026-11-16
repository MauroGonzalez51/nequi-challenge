# Session 5

Organised by rubric area, random order, code shown in every code question. 13 questions.

| # | Area | Topic | Grade | Note |
|---|---|---|---|---|
| 1 | A | Where the top-stock decision lives, tie-break | Pass / partial | Tie-break (`OrderByStockDescIdAsc`) right. Weak "why": defend it as port declares intent, adapter implements efficiently. Cost: tie-break not in the port contract. |
| 2 | A/B | `concatMap` over N branches | Partial | `concatMap` vs `flatMap` trade-off right. Missed the N+1 count (N+2 queries) and the fix (one grouped query, e.g. `DISTINCT ON`). |
| 3 | D | Request path Internet to RDS | Pass | SG chain right. Missed: ALB port 80 open to `0.0.0.0/0`, target group, RDS has no egress. NAT carries ECR pull, Secrets Manager, CloudWatch (not the task definition). |
| 4 | F | `StepVerifier` second element, error | Pass / fail | First two right. `expectError` (type) vs `expectErrorSatisfies` (content) explained. |
| 5 | G | Authentication | Pass | Missed: ALB is public, so API Gateway can be bypassed. `authenticate-cognito` needs HTTPS. AuthN at the edge, AuthZ may reach the domain. |
| 6 | B | `DELETE` handler that drops the `Mono` | Pass | Logic fully right (cold publisher, nothing subscribes). Also: client gets a 204 with nothing deleted. `then` after `Mono<Void>`. |
| 7 | C | `@Builder` vs `@Data`, `record` | Partial | `toBuilder` is opt-in. `@Data` risk is setters and unstable `hashCode`, not null vs empty list. `@Setter` is on entities and on domain models (mapper needs it, not R2DBC). |
| 8 | E | `minimum-number-of-calls` 1 vs 5 vs 10 | Pass | |
| 9 | D | Rollback with mutable `latest` | Pass / partial | Fix learned: immutable ECR tags (git SHA), task definition revision rollback, ECS deployment circuit breaker with `rollback = true`. |
| 10 | F | Unit vs integration (B2) | Pass | Derived queries, `schema.sql`, FKs, column mapping. Defence: name the gap, point to the dev smoke test, state the plan. |
| 11 | A | No `UNIQUE` on `name` | Pass / partial | Missed check-then-insert race. DB constraint is the guarantee; `PersistenceResilience` already maps it to `DataConflictException` (409). |
| 12 | B | Blocking call in WebFlux | Pass | Netty event loop threads are few and serve many connections. A frozen loop fails the ALB health check and ECS replaces the task. |
| 13 | G | Which part do you trust least | Partial | Too vague. Use: "X, evidence Y, plan Z". |

## Learned this session

- Grade logic, not chat syntax (feedback saved in memory).
- Unique name: only `UNIQUE` in `schema.sql` plus a clearer 409 message in `ErrorMapper`. `findByName` is optional politeness, not a guarantee.
