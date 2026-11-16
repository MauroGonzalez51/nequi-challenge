# Next round (round 5)

Rewrite after every session. Only what is pending lives here. History is in `session-1.md` to `session-4.md`, the plan and blockers in `SPARRING.md`.

## Why this file changed

Rounds 3 and 4 spent most of the time on resilience, Docker and IAM. The jury will not follow our topic list: it follows the rubric in `openspec/changes/nequi-challenge/specs/` and the 9 endpoints. This round is organised by rubric area, with random picks and one question per area.

## Rules for the next round

1. **Random order.** Claude picks an area and a question from the bank below without announcing the ladder. No topic repeated twice in a row.
2. **Show the code.** Every code question comes with the snippet pasted. The candidate explains, never recalls.
3. **Retired topics** (passed at least once, do not ask again unless a new mistake shows up): text blocks, `switchIfEmpty` vs `defaultIfEmpty`, `zip` vs `merge`, multi-stage Docker (definition), trust vs permissions policy, why writes are not retried, timeout per attempt vs retry order, `Idempotency-Key` idea.
4. **Still open, ask once with code shown** (partial or fail): P3a latency arithmetic, `sliding-window-size`, `minimum-number-of-calls = 1`, `wait-duration-in-open-state` 1s vs 5 min, `readMany` buffer and `concat` vs `zip`. Details in `session-4.md`.
5. **Old unanswered:** 13b (`verify(..., never())`), 15b (which blocker first), 16 (why no `Origin` means no CORS). The wording is lost: Claude rewrites them from the code before asking.
6. After each answer: grade, then 2 lines of the correct explanation. No repeated drilling of the same item inside a round.

## Question bank by rubric area (pick any, in any order)

### A. Franchise API (9 endpoints, never probed yet)

- Open `RouterRest` and one handler: walk a `POST` from the router to the DB and back. Which class does what?
- Top-stock: show the use case. Where is the "highest stock" decision made and why there and not in SQL or the adapter? What happens with a tie in stock? With a branch without products (our answer: `product: null`)?
- Rename endpoints: one use case or three? Why `PUT`/`PATCH` and which one is used? Idempotent or not?
- Validation: negative stock, blank name, non-numeric stock. Which layer rejects each and with which status and `errorCode`?
- Delete product: what if the branch exists but the product belongs to another branch? Does the `zip` check it?
- Why are foreign keys plain `Long` columns? What does the DB guarantee and what does the app have to check?
- Pagination: `GET` of all branches of a franchise returns everything. Is that a problem? What would you add?
- No unique constraint on any `name`. Is that a bug or a decision? Defend it.

### B. Reactive and WebFlux

- Show a handler: what runs on which thread (Netty event loop vs R2DBC pool)? What happens if a use case does a blocking call?
- `subscribeOn` vs `publishOn`: when needed here, and why not used.
- Cold vs hot publisher: is a `Mono` from the repository cold? What happens if nobody subscribes?
- `Mono.fromCallable` vs `Mono.just` vs `Mono.defer`: pick one in our code and explain.
- Backpressure with `request(n)`: who sends demand in a `GET` that returns a `Flux`? What changes with `collectList()`?
- `onErrorResume` vs `onErrorReturn` vs `onErrorMap`: find one of each in the code, justify the choice.
- `doOnNext` vs `map`: why is a side effect inside `map` wrong?
- `Mono<Void>`: what does the empty completion mean on `DELETE`? Why `.then(noContent)`?
- MVC vs WebFlux: what would break if `spring-boot-starter-web` were added to the classpath?

### C. Architecture, SOLID, Java 17, Lombok

- Draw the module graph from `build.gradle` files: who depends on whom, and why can `usecase` not see `r2dbc-postgresql`?
- Why `Entity` and `Model` are different classes. What is the cost of the mapping?
- `@Builder` vs `@Data` vs `@Value` on a domain entity. What does each generate?
- Records: what do they generate, can they be mutated, can they extend a class, can Jackson deserialize them?
- Interface Segregation in our gateways: is `FranchiseGateway` too fat?
- Why does validation live in two places (handler and use case)? What does each one protect?
- Dependency rule test: how would you enforce it automatically (ArchUnit)?

### D. Terraform and AWS

- Walk the request path: Internet to ALB to target group to ECS task to RDS. Which security group allows what, and from where?
- Public vs private subnets: which resources sit in which, why does the task need a NAT gateway?
- `terraform plan` vs `apply` vs `destroy`. What is state drift and how would you detect it?
- Remote state: what does `use_lockfile` protect against? What if two people apply at once?
- Workspaces vs directories: defend our choice, show how workspaces would look.
- Module design: pick `modules/ecs`, list its inputs and outputs, and say why each output exists.
- ECS: task definition vs service vs cluster. What does the health check of the target group do vs the container health check?
- Secrets: where does the password live at each moment (Terraform state, Secrets Manager, task environment)? What rotates it?
- API Gateway vs ALB: what would API Gateway add (auth, throttling, usage plans, WAF)? Why is it not needed for the rubric?
- Cost: what costs money while `dev` is up, and which resource is the biggest line?
- Rollback: a bad image was pushed to `latest`. How do you recover?

### E. Resilience (only items not retired)

- Show `application.yaml` resilience block: explain each number as a jury member would ask "why this and not double?".
- What happens to in-flight requests when the breaker opens?
- Bulkhead and rate limiter: not implemented. When would you add each?
- Fallbacks: should a read return stale data when the breaker is open? Trade-off.

### F. Testing, quality, delivery

- Show a handler test with `WebTestClient`: what is real and what is mocked? Is it a unit or an integration test?
- Unit vs integration test: what would a Testcontainers test catch that ours cannot (SQL errors, `schema.sql`, FK behaviour)?
- `verifyComplete` vs `expectNext` vs `expectError` vs `expectErrorSatisfies`: when each.
- Virtual time (`StepVerifier.withVirtualTime`): when would you use it for the retry test instead of real 10ms waits?
- JaCoCo: 90% line coverage with weak asserts, what does it prove? What does mutation testing add?
- OpenAPI: where are the examples defined? Is the spec generated or written by hand here?
- README from a clean clone: what would fail today? (Blocker B3.)
- Commit history: pick three commits, defend the messages and the size.
- English only: show any comment or identifier that is not English (grep first).

### G. Curveballs (jury style, no code)

- "What would you change first with one more week?" (expect: integration test, auth, pagination, idempotency, ALB HTTPS).
- "How do you add authentication to this API?" (where in the chain: filter, `WebFilter`, API Gateway, Cognito).
- "How do you scale writes beyond one Postgres instance?"
- "A client sends 10k products in one request. What breaks?"
- "Why Postgres and not DynamoDB or Mongo for this model?"
- "Which part of the code do you trust least and why?"
- "Which line of code did the AI write that you could not defend yet?"
- Line-by-line drill: Claude opens a random file (handler, use case, adapter, config, Terraform module) and the candidate explains every line.

## Blockers still open

B1 to B5 in `SPARRING.md` section 1. Deploy `dev` the evening before and follow the README from a clean clone.

Pending from the `DataConflictException` change: add the 409 scenario in `specs/` and tick `tasks.md` only after verifying.
