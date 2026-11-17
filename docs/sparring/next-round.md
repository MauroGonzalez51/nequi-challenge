# Next round (round 6)

Rewrite after every session. Only what is pending lives here. History is in `session-1.md` to `session-5.md`, the plan and blockers in `SPARRING.md`.

## Rules for the next round

1. **Random order** by rubric area (bank in `session-5.md` style, areas A to G). No topic twice in a row.
2. **Show the code.** Paste the snippet, the candidate explains, never recalls.
3. **Grade logic, not chat syntax.**
4. After each answer: grade, then 2 lines of the correct explanation. No repeated drilling inside a round.
5. **Line-by-line drill on code written with Claude's help.** The candidate did not write the tests (session 1 gap) and Claude wrote 9 unit tests plus the Testcontainers test on 2026-11-16. Pick 2 or 3 and explain every line (see "Priority" below).

## Retired (passed, do not ask again unless a new mistake shows up)

Text blocks, `switchIfEmpty` vs `defaultIfEmpty`, `zip` vs `merge`, multi-stage Docker (definition), trust vs permissions policy, why writes are not retried, timeout vs retry order, `Idempotency-Key` idea, top-stock tie-break, `concatMap` vs `flatMap`, network path and SG chain, cold publisher and dropped `Mono`, `minimum-number-of-calls`, unit vs integration, blocking in the event loop, `record` vs `@Data`.

## Priority (about 2 days left, ask these first)

1. **N+1 in top-stock:** count the queries for 1000 branches (N+2) and give the grouped-query fix. Ask with `GetTopStockProductPerBranchUseCase` pasted.
2. **Rollback mechanics:** why `latest` is hard to roll back (old image goes untagged) and the three-piece fix (immutable tags, task definition revision, ECS deployment circuit breaker).
3. **Curveball 13:** "which part do you trust least". Answer with the template: X, evidence Y, plan Z.
4. **Latency arithmetic (session 4, P3a):** worst-case read with the DB hung and the breaker CLOSED. 3 attempts x 2s timeout plus waits, about 6.6s.
5. **Line-by-line drill of code written with Claude:** pick 2 or 3 from this list and explain every line.
   - `thenAnswer(call -> Mono.just(call.getArgument(0)))` in the trim tests: what it returns and why the assertion can read the saved name.
   - `shouldDoNothingWhenTrimmedNameIsTheSame`: which bug it would catch and why `never().save` matters.
   - `UniqueNameIntegrationTest`: why `trust`, why the host is resolved and `schema.sql` read in `@BeforeAll`, what `@Container static` does.

## From 2026-11-16 (unique names, trim, integration test): ask once, code shown

- **Unique name race (closes session 5, Q11):** why `findByName` first is not enough (check-then-insert) and why the index is the guarantee. Where the 409 is built: `PersistenceResilience` to `ErrorMapper`. Decision recorded in `design.md`.
- **Why `lower(btrim(name))` in the index** and not normalizing in Java alone: case-insensitive match without storing a changed name. Known limit: `btrim` strips spaces only, not tabs.
- **`flatMap` vs `then` in the patch use cases:** `then` drops the upstream value, so the trimmed name was lost. Nesting the rest of the chain inside `flatMap(trimmed -> ...)` keeps it in scope.
- **`Mono.justOrEmpty(name.trim())` breaks the null case:** `name.trim()` runs before the Mono exists, so a null name throws NPE instead of `ValidationException`. Why the trim goes in `.map(String::trim)` after `justOrEmpty(name)`.
- **Validation layers:** `@NotNull` in the DTO is shape (edge), `isBlank()` in the use case is the domain rule. Why not `@NotBlank` in the DTO (the rule would live in two places).
- **BlockHound and the SCRAM handshake:** what BlockHound detects, why Postgres 16 SCRAM reads `/dev/urandom` on the Netty thread, and why the test container uses `trust`. The app against real SCRAM still does that read; whether BlockHound is active outside tests was not checked.

## If there is time

- **Auth bypass:** API Gateway in front of a public ALB can be bypassed. Why, and the fix (internal ALB plus VPC Link, or restrict the SG).
- **`expectError` vs `expectErrorSatisfies`.**
- **`@Setter` on domain models:** who needs them (the scaffold `ObjectMapperImp`, not R2DBC) and the alternatives.
- **From round 4** (`session-4.md`): `sliding-window-size`, `wait-duration-in-open-state` 1s vs 5 min, `readMany` buffer and `concat` vs `zip`.

## Dropped

The old unanswered 13b (`verify(..., never())`), 15b (which blocker first) and 16 (why no `Origin` means no CORS) were removed. Their exact wording was never recorded and they stayed unanswered for four sessions. `verify(..., never())` is covered by the line-by-line drill on `shouldDoNothingWhenTrimmedNameIsTheSame`; "which blocker first" is answered by the order in `SPARRING.md` section 1.

## Areas not probed yet (pick at most two)

- A: rename endpoints (`PUT`/`PATCH`, idempotent?), delete product from another branch, plain `Long` FKs, pagination.
- B: `subscribeOn` vs `publishOn`, `onErrorResume` vs `onErrorReturn` vs `onErrorMap`, `doOnNext` vs `map`, backpressure with `collectList()`.
- C: module graph from `build.gradle`, Entity vs Model cost, ISP in gateways, ArchUnit.
- D: remote state and `use_lockfile`, drift, module design (`modules/ecs`), secrets at each moment, cost.
- E: in-flight requests when the breaker opens, bulkhead, rate limiter, stale fallback.
- F: `WebTestClient` handler test (what is real), virtual time, JaCoCo vs mutation, README from a clean clone, commit history.
- G: "one more week", scaling writes, 10k products in one request, Postgres vs DynamoDB.

## Blockers still open

B1, B3, B4 and B5 in `SPARRING.md` section 1. B2 is half closed: the Testcontainers integration test exists (`UniqueNameIntegrationTest`), but `tasks.md` 4.1 also needs a successful run against the cloud store. Deploy `dev` the evening before and follow the README from a clean clone.

## Pending changes to the specs

- Add the 409 scenario for the `DataConflictException` change in `specs/`, and one for duplicate names (per parent, ignoring case and surrounding spaces). Tick `tasks.md` only after verifying.
- Optional, from session 5: a clearer 409 message in `ErrorMapper`. Today it is the generic "The operation conflicts with the current state of the data".
