# Session 2 (2026-11-15)

Covered: `defaultIfEmpty` vs `switchIfEmpty`, the `5330a52` ordering fix, `flatMapSequential` vs the pool, empty vs not-found, `zip` vs `merge`, multi-stage Docker, execution role vs task role, trust policy vs permissions policy, Auto Scaling, SOLID (S, O, L, I, D), sealed classes, text blocks.

## Results

- Pass: P2 (ordering fix), P5 (`zip`, `merge`), P6 (`zip` in `ProductHandler.delete`), P7 (multi-stage), P8 (execution vs task role), P9 (`deploy.sh` builds the jar first), P16 (why the gateway lives in the domain).
- Partial: P1, P3, P11, P13 (S and O), P14 (L, I).
- Failed at first attempt, understood after the explanation: P10 (trust policy confused with permissions policy).
- No answer, explained: P12 (CPU is a weak metric for an I/O-bound WebFlux app), sealed classes.

## Repeat next round (answer without looking)

- `switchIfEmpty` takes a `Publisher`, not a `Supplier`. `Mono.error(() -> ...)` is the `Supplier` overload of `Mono.error`: the exception and its stack trace are built only on the error path.
- `flatMapSequential` has default concurrency 256 against a pool of 15. Waiting for a connection counts inside the 2s timeout, so it ends in timeouts, `503` and an open breaker.
- Say "independent, combined in one step" for `zip` in `ProductHandler.delete`, never "in parallel": those reads are in-memory and run on the same thread. If both ids are invalid, which error does the client see?
- Trust policy answers who can assume the role. Permissions policy answers what the role can do. Broken trust: the task cannot start. Broken permissions: the role is assumed, then `AccessDenied` on ECR, the secret or the log group.
- The `task` role in `modules/iam/main.tf` is empty on purpose. It exists so the container has an explicit AWS identity.
- `cpu_target` is 60, not 50. Know the number without saying "I think".
- Target tracking holds the average near the target, it is not a fixed threshold. The Spring startup spike can add a second task.
- DIP is not DI. DIP is the direction of dependencies and who owns the abstraction. DI is the mechanism Spring uses. The gateway lives in `domain` because otherwise `usecase` would depend on `infrastructure`, which already depends on the domain: a circular Gradle dependency.
- S example: use cases know nothing about HTTP, `ErrorMapper` is the only place that maps errors. O example: gateways. Be ready to admit that a new endpoint still edits `RouterRest`.
- Liskov: find out whether the domain exceptions share a base class. If not, say that L barely applies.
- Is CPU the best scaling metric here? Alternative: `ALBRequestCountPerTarget`. Scaling tasks does not fix a database bottleneck: every task brings its own pool of 15.

## Questions to ask next time

1. Open since session 1: 13b (`verify(..., never())`), 15b (which blocker first), 16 (why no `Origin` means no CORS). The exact wording is not recorded; rewrite these three before the next round.
2. Line-by-line drill, not done yet: pick 2 or 3 tests (see "Gaps found") and explain every line.
3. Resilience under pressure: the 50-question exam (`docs/exam-webflux-circuit-breaker.md`) is missing from the repo. Regenerate it or skip it.
4. "What breaks if" ladder on: `ErrorMapper` catch-all order, `DataIntegrityViolationException` answering 500, `collectList()` with unbounded demand.
5. Terraform and IAM weak spots from section 3: `Resource: "*"`, `latest` tag, `sslmode=require`, state holding the password.
6. Docker: why the scaffold skips multi-stage, and what you would change.

## Still not covered

- Sealed classes and text blocks were explained but never answered by the candidate. Ask for them in own words.
- API Gateway vs ALB, workspaces vs directories, JaCoCo meaning, mutation testing.
- Blockers B1 to B5 (section 1) are still open.
