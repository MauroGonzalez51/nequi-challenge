# Next round (round 3)

Rewrite this file after every session. It holds only what is pending. History lives in `session-1.md` and `session-2.md`, the plan and blockers in `SPARRING.md`.

## Repeat without looking

- `switchIfEmpty` takes a `Publisher`, not a `Supplier`. `Mono.error(() -> ...)` builds the exception only on the error path.
- `flatMapSequential`: default concurrency 256 against a pool of 15. Waiting for a connection counts inside the 2s timeout.
- `zip` in `ProductHandler.delete`: say "independent, combined in one step", never "parallel". If both ids are invalid, which error does the client see?
- Trust policy (who can assume the role) vs permissions policy (what the role can do). Broken trust: the task cannot start. Broken permissions: `AccessDenied` after assuming.
- The `task` role is empty on purpose.
- `cpu_target` is 60. Target tracking holds the average near the target.
- Why only the CPU metric: memory is a bad signal on a JVM (the heap grows and stays high because of how the garbage collector works, not because of thread count). Requests per target needs a measured throughput we do not have. CPU needs no calibration.
- Scaling tasks does not fix a database bottleneck: each task brings a pool of 15.
- DIP is not DI. The gateway lives in `domain` or `usecase` would depend on `infrastructure`, a circular Gradle dependency.
- Liskov: do the domain exceptions share a base class? If not, say L barely applies.
- S and O examples, and the honest point that a new endpoint still edits `RouterRest`.

## Questions to ask

1. Open since session 1: 13b (`verify(..., never())`), 15b (which blocker first), 16 (why no `Origin` means no CORS). The exact wording is lost: rewrite these before asking.
2. Sealed classes and text blocks, in the candidate's own words.
3. Line-by-line drill: 2 or 3 tests, every line explained.
4. "What breaks if": `ErrorMapper` catch-all order, `DataIntegrityViolationException` answering 500, `collectList()` with unbounded demand.
5. Terraform and IAM weak spots: `Resource: "*"`, `latest` tag, `sslmode=require`, password in state.
6. Docker: why the scaffold skips multi-stage and what would change.
7. Not covered yet: API Gateway vs ALB, workspaces vs directories, JaCoCo, mutation testing.

## Blockers still open

B1 to B5 in `SPARRING.md` section 1. Deploy `dev` the evening before and follow the README from a clean clone.
