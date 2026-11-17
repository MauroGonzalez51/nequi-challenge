# Session 6

Code shown in code questions, grade logic not chat syntax. 12 questions, plus one pending curveball.

| # | Area | Topic | Grade | Note |
|---|---|---|---|---|
| 1 | A/B | N+1 in top-stock (`GetTopStockProductPerBranchUseCase`) | Partial | Structure right (1 + 1 + N), arithmetic slip (1002, not 1003). `concatMap` vs `flatMapSequential(f, concurrency)` right. Fix incomplete: method belongs in `ProductRepository` (not Franchise), needs `LEFT JOIN` (branches without products must stay), and the stock tie-break (`DISTINCT ON (branch_id) ... ORDER BY branch_id, stock DESC, id`). |
| 2 | D | Rollback with mutable `latest` | Partial | Tags immutable (git SHA) right, plus ECR `image_tag_mutability = IMMUTABLE` missed. Wrong: `--force-new-deployment` re-pulls `:latest`, same broken image. ECS does not watch ECR. Rollback = `update-service --task-definition family:N-1`. Deployment circuit breaker (`rollback = true`) missed. Task definition revisions are created by us, not by the deploy. |
| 3 | G | Which part do you trust least | Pass / partial | Picked real items: N+1 and `@Setter` in the domain. Missing evidence for `@Setter`: `ObjectMapperConfig.java:13` says `ObjectMapperImp` maps through setters. Plan: explicit entity-to-model mapper through builders, then drop `@Setter`. The 4 patch use cases already use `toBuilder()`. |
| 4 | F | `thenAnswer(call -> Mono.just(call.getArgument(0)))` | Pass | Returns the object the use case passed to `save`, so the assert sees the trimmed name. `thenReturn(fixed)` would pass with the bug. `any(Branch.class)` checks type and rejects null. |
| 5 | F | `shouldDoNothingWhenTrimmedNameIsTheSame` | Pass | No-op rule right. `expectNext` checks the output, `never().save` checks the side effect. Unstubbed `save` returns null, so the test would probably fail with an NPE anyway. |
| 6 | E | Worst-case read latency, DB hung, breaker CLOSED | Pass | 3 x 2s + 200ms + 400ms = 6.6s. Two waits, none after the third attempt. |
| 7 | F | `UniqueNameIntegrationTest` | Pass after correction | `static @Container` is one container per class. `@BeforeAll` runs once per class, so the DB is shared (tests only avoid each other by using different names). Blocking: host resolution and `Files.readAllBytes`. Test covers the real index and the violation to `DataConflictException` mapping. |
| 8 | A | Unique name race | Pass | Check-then-insert is not atomic. Chain: Postgres 23505, `DataIntegrityViolationException`, `PersistenceResilience` `onErrorMap`, `DataConflictException` (in `domain/model`), `ErrorMapper` `onErrorResume`, 409 `DATA_CONFLICT`. |
| 9 | E | Does a 409 count for the breaker | Pass | `recordException(PersistenceFailures::isInfrastructureFailure)` is a whitelist. If it counted, 20 duplicates open the breaker and every user fails fast. |
| 10 | B | `onErrorMap` vs `onErrorResume` vs `onErrorReturn` | Partial | `onErrorReturn` is a fixed value, no access to the exception. `ErrorMapper` needs `exception -> Mono<ServerResponse>`. |
| 11 | B | `subscribeOn` vs `publishOn` | Pass | Schedulers are for blocking code. R2DBC is non-blocking on Netty threads. |
| 12 | G | "One more week" | Partial | Picked top-stock query and API Gateway + Cognito. Did not prioritise to one, did not say what to leave out. Auth bypass: a public ALB lets callers skip the gateway. |

## Learned this session

- Grade fairness: an arithmetic slip is not graded, missing design pieces are. Two of my "not answered" calls were wrong, check the answer before saying so.
- `@BeforeAll` means once per class. `@BeforeEach` is per test.
- Defence wording for `@Setter`: "known debt, did not change the scaffold mapper without mapping tests", not "it is fragile".
