# SPARRING.md: defense preparation plan

Defense: **Wednesday 2026-11-18**, demoing `dev`.
Source of truth: the original challenge document (`reto_nequi_v2.docx`, "Reto Técnico: API de Franquicias") and `openspec/changes/nequi-challenge/`.

The rule that decides everything: **if you cannot explain a line and justify it, the jury treats it as not known.** The goal of the sparring is not to recite theory but to defend *this* code, these decisions and these trade-offs.

## 1. Blockers to close first (not study topics)

These are in the original document and not yet verified. Each one can fail the defense on its own.

| # | Requirement in the document | Status | Action |
|---|---|---|---|
| B1 | "The application must be **deployed and reachable in the cloud** at the time of the defense" | `dev` was destroyed after testing | Deploy `dev` the evening before (`scripts/deploy.sh dev`, ~15 min). Run the smoke test in section 6. Destroy it after. |
| B2 | "Unit tests **and integration** where it makes sense" | Unit and `WebTestClient` handler tests, plus one Testcontainers integration test (`UniqueNameIntegrationTest`: save and read, duplicate name gives `DataConflictException`). Still no run against the cloud store (`tasks.md` 4.1). | Be ready to explain the test line by line, and cover the gap that branch and product indexes have no automated test. |
| B3 | README must let anyone run the project from scratch, "verified by the candidate" | Written, never followed from a clean clone | Clone to a temp folder and follow it literally. |
| B4 | Public repo, clean commit history with real progress | Several changes still uncommitted | Review `git status` and the commit messages before the day. |
| B5 | Disqualification checks (`@RestController`, `.block(`, `java.util.stream`, `try`/`catch` and `if` in reactive flows, `javax.`, MVC mixed with WebFlux) | Checked per task, not as a final sweep | Run the grep sweep from `CLAUDE.md` once more on the final commit. |

The document also mentions a **Study Guide and a Defense Guide** "delivered with this document". They are not in the repo. If you have them, they are the best source for question style.

## 2. Topics and where the jury will probe

Topics come straight from the document's "Topics to study" list. The last two columns are what makes the sparring specific to this project.

### 2.1 Reactive programming and WebFlux

| Topic | What gets probed | In our code | Known weak spot |
|---|---|---|---|
| `Mono` vs `Flux` | When each one, what `Mono<Void>` means | Use cases return `Mono`/`Flux` | none |
| `map` vs `flatMap` | The exact rule: Publisher inside means `flatMap` | Rule checked per task | Be ready to point at one of each in the code |
| `filter` / `switchIfEmpty` instead of `if` | Why `if` is banned in flows | Use cases use `switchIfEmpty(Mono.error(...))` | Be ready to explain `defaultIfEmpty` vs `switchIfEmpty` |
| `zip`, `merge` | Difference, when to use each | `zip` appears in 1 file, `merge` nowhere | Know both even where unused |
| `concatMap` vs `flatMap` | Ordering vs concurrency | Top-stock uses `concatMap` | Our order bug: unordered query, fixed with `OrderByIdAsc` |
| Reactive error handling | `onErrorResume` vs `onErrorReturn`, why no `try`/`catch` | `ErrorMapper` chain, one place for HTTP | Why a single mapper |
| Backpressure | What it is, who signals demand, where it matters | Native in WebFlux | `collectList()` in the top-stock handler asks for unbounded demand. Trade-off is in the README. |
| `RouterFunction` / `HandlerFunction` | Difference with `@RestController`, why the rule | `RouterRest`, 3 handlers, `RequestParams` | MVC vs WebFlux: threads, event loop, Netty |

### 2.2 Architecture, SOLID, Java and Lombok

| Topic | What gets probed | In our code | Known weak spot |
|---|---|---|---|
| Hexagonal / Bancolombia scaffold | Layers, ports, adapters, dependency rule | `domain`, `infrastructure`, `applications` | Why use cases never see DTOs or entities |
| Where business rules live | "Only in the domain" | Validation in two layers (edge vs domain) | Be ready to justify the split |
| SOLID in a reactive context | Pick one principle and show it | Ports are the DIP example | Prepare one concrete example for each letter |
| Java 17 | Records, **sealed classes**, **text blocks**, `jakarta` vs `javax` | Records for DTOs and properties (12 files) | **No sealed classes or text blocks in the code.** Concepts only. |
| Lombok | `@Builder`, `@Data`, `@Value`, when each | `@Builder` on entities | **No `@Data`.** Know why `@Data` is risky on entities (mutability, `equals`/`hashCode`). |

### 2.3 Resilience

| Topic | What gets probed | In our code | Known weak spot |
|---|---|---|---|
| Circuit Breaker | States, thresholds, half-open | `PersistenceResilience`, Resilience4j | Defend every number in `application.yaml` |
| Timeout and Retry | Order of operators, backoff | `timeout -> retry -> circuit breaker` | Why the breaker is outermost |
| Retry on writes | Why writes are not retried | Reads retried, writes not | Duplicate insert if the response was lost |
| Backpressure | See 2.1 | | |

### 2.4 Testing

| Topic | What gets probed | In our code | Known weak spot |
|---|---|---|---|
| `StepVerifier` | Why never `.block()` | Used across modules | Be ready to explain `verifyComplete` vs `expectNext` |
| Unit vs integration | The real difference | Unit tests plus `WebTestClient` handler tests | One integration test with a real database (B2); branch and product indexes are not covered. |
| JaCoCo | What the number means | Reports per module | Coverage is not the same as meaningful tests |
| Mutation testing | Arcmutate runs in the build | Mutants killed rate shown at build time | Optional, but it shows test strength |

### 2.5 Terraform and AWS

| Topic | What gets probed | In our code | Known weak spot |
|---|---|---|---|
| Modules, variables, outputs | What each file is for | 6 modules, `environments/<env>` | none |
| State | Why remote, locking | S3, `use_lockfile`, one key per environment | Bucket name passed at `init`, not hardcoded |
| Workspaces vs directories | The trade-off | **Directories per environment** | Defend the choice, know how workspaces would work |
| IaC principles | Reproducibility, idempotency, versioning | Plan converges, provider pinned `~> 6.0` | `.terraform.lock.hcl` is git-ignored. Be ready to justify. |
| ECR / ECS Fargate / ALB | Why Fargate, how they connect | Full chain in `modules/` | Rollout with a mutable `latest` tag |
| **API Gateway** | What it adds over an ALB | **Not used.** Entry is the ALB. | Not a requirement, but it is a study topic. Know what it would add here. |
| Secrets Manager | How the secret reaches the app | ECS injects `secrets` at task start (`valueFrom`) | Value lives in Terraform state and in the container env at runtime |
| IAM least privilege | Execution role vs task role | Execution role scoped to one repo, log group, secret | `ecr:GetAuthorizationToken` on `*`, `Resource: "*"` in the deployer policy |
| Auto Scaling | What it tracks | CPU target tracking at 60% | Startup CPU can add a task |

### 2.6 Docker

| Topic | What gets probed | In our code | Known weak spot |
|---|---|---|---|
| Image build | Layers, cache | Scaffold Dockerfile | **Single stage, only `COPY *.jar`.** The jar is built outside. |
| Multi-stage builds | Why they exist | **Not used** | Know the pattern and why the scaffold skips it |
| Security practices | Non-root, minimal image | Runs as `appuser` | Alpine base, no scan beyond ECR scan on push |

## 3. Decisions to defend (honest list)

Know the reason **and** the cost of each. These were chosen on purpose.

1. One NAT gateway: single point of failure, chosen for cost.
2. ALB is HTTP only, no domain, no access logs.
3. DB connection uses `sslmode=require`: encrypts but does not verify the certificate.
4. `random_password` puts the DB password in the state.
5. `force_destroy`, `force_delete` and no final snapshot: deliberate for a throwaway account.
6. `Resource: "*"` in the deployer policy and `ecr:GetAuthorizationToken` on `*`.
7. Mutable `latest` image tag: no rollback by tag.
8. Directories per environment instead of workspaces.
9. Swagger is on in `dev` and `staging`, off in `prod`, by a Terraform flag.
10. `collectList()` in one handler instead of streaming the `Flux`.
11. No authentication on the API.
12. No way to inspect the DB directly (private subnets). Bastion not built.

## 4. Schedule

| Day | Focus | Output |
|---|---|---|
| Sat 14 | Close B2 to B5. Reactive core and WebFlux (2.1). | Gap list, grep sweep clean |
| Sun 15 | Architecture, SOLID, Java 17, Lombok (2.2). Resilience (2.3). | One drill per topic |
| Mon 16 | Terraform and AWS (2.5), Docker (2.6). Bastion if you still want it. | Defense of every decision in section 3 |
| Tue 17 | Mixed round, defense speed. Deploy `dev`, run the smoke test, follow the README from scratch. | Live environment ready |
| Wed 18 | Final smoke test before the defense. Destroy after. | |

## 5. Sparring format

- **Evaluator mode.** Claude asks, you answer without looking. No answer given before your attempt.
- **Difficulty ladder per topic:** definition, then "where in our code", then "why not the alternative", then "what breaks if...".
- **Line-by-line drill:** pick a file, you explain every line. The document says the interviewer will do this.
- **Grading:** pass, partial or fail, with the reason against the actual code. Failed items go back in the next round.
- **Answer key stays out of the repo.** It lives in Claude's memory (see the exam answer key).
- Language: questions and feedback in Spanish. This document is in English because the code is.

## 6. Demo checklist for Wednesday

```bash
cd packages/iac
scripts/deploy.sh dev                                   # the evening before, ~15 min
API=$(terraform -chdir=environments/dev output -raw api_url)
curl "$API/actuator/health"                             # {"status":"UP"}
# open $API/swagger-ui.html and try the endpoints from the UI
scripts/destroy.sh dev                                  # after the defense
```

- Have the ALB URL, the ECR repository and the ECS service open in the AWS console.
- Have one error response ready to show `errorCode`.
- Have a fallback if AWS fails: run locally and show the Terraform `plan`.
- Keep a budget alarm. The environment costs about 0.12 USD per hour while up.

## 7. Open items

- [ ] B1 to B5 above
- [ ] Bastion or equivalent for DB inspection (requires deployer policy changes, short-lived)
- [ ] `terraform plan` for two environments as evidence (task 6.3)
- [ ] Tick `tasks.md` 6.2 to 6.5 only after each verification passes
- [ ] Delete `packages/iac/HANDOFF.md`
- [ ] Decide what to do with this file: keep out of the repo or commit

## 8. Session log

Load only `docs/sparring/next-round.md` when starting a round. Older sessions are history, open them only when needed.

- `docs/sparring/next-round.md`: what is pending now. Rewritten after every session, it does not grow.
- `docs/sparring/session-1.md`: resilience order, `switchIfEmpty`, `concatMap`, layers, Terraform state, Lombok, testing tools.
- `docs/sparring/session-2.md`: `zip` vs `merge`, multi-stage Docker, IAM roles, Auto Scaling, SOLID, sealed classes.
- `docs/sparring/session-3.md`: `Mono.error` lazy, `flatMapSequential` vs pool, `transformDeferred`, `DataConflictException` (409), IAM `*`, multi-stage Docker.
- `docs/sparring/session-4.md`: `PersistenceResilience` order and writes, worst-case read latency, breaker numbers, resilience tests, `readMany` buffer, `concat` vs `zip`.
- `docs/sparring/session-5.md`: top-stock and N+1, network path, `StepVerifier`, auth, cold publisher, `@Data`, rollback, integration tests, unique names, blocking in WebFlux.
- `docs/sparring/session-6.md`: N+1 fix in full, rollback pieces, `@Setter` evidence, `thenAnswer` and no-op tests, latency 6.6s, integration test line by line, unique race and 409 chain, 409 vs breaker, `onError*`, `subscribeOn`/`publishOn`, "one more week".
- `docs/sparring/session-7.md`: curveballs 12 and 13 again, auth bypass (`internal` ALB, VPC Link, `authenticate-cognito`), rollback with mutable `latest` (N-1 still points to `latest`).
