# Session 4 (round 4)

Date: 2026-11-16. Focus: resilience (`PersistenceResilience` and its tests). Closed, nothing pending inside this session.

## Graded

| Q | Topic | Result | Note |
|---|-------|--------|------|
| P1 | Text blocks | pass | Multiline strings, valid use is a custom `@Query`, none in the repo. Extra: compiler strips incidental indentation, `"""` must be followed by a newline, `.formatted()` interpolates. |
| P2 | `PersistenceResilience` order and writes | pass, 4 details | See below. |
| P3a | Worst-case latency of a read, DB down, breaker CLOSED | partial | Jitter explained well (thundering herd). Missed the main term: a hung DB makes every attempt consume the full 2s `timeout`. "Fails instantly" only holds for connection refused. |
| P3b | Safe retry of writes | pass | `Idempotency-Key`, generated once per operation and reused on retries. Missed the server side (see below). |
| P4 | Breaker numbers | partial | Definitions right. `sliding-window-size` not answered. `minimum-number-of-calls = 1` answered wrong. Wait and half-open defined but not defended. |
| P5 | Test: write is not retried | pass | `shouldNotRetryAWrite` (lines 92-104). Read/write contrast with the same error: `calls == 1` vs `calls == 3`. |
| P5b | Test: timeout on a read | pass | Timeout is per attempt, retry is outside it, so `calls == 3`. `WAIT` is only the test ceiling. |
| P6 | Test: breaker ignores integrity errors | pass idea, partial reasoning | Test catches a `DataIntegrityViolation` counted as a breaker failure. The "5 iterations" reasoning was muddled (see below). |
| P7 | Test: `readMany` retry | partial | Confused a read with a write ("already in the DB"). Compared `concat` with `zip`, wrong. |

## Facts to keep (answer in own words next time)

- **P3a arithmetic:** 3 attempts x 2s timeout = 6s, plus waits of ~200ms and ~400ms (with jitter 100-300 and 200-600, max 900ms). Nominal about 6.6s, worst case about 6.9s before a 503. That is the cost of retry while the breaker is still CLOSED.
- **Retry filter** is `isInfrastructureFailure` (timeouts and connection errors). `DataIntegrityViolation` is never retried.
- **Backoff defaults:** factor 2, jitter 50%. With a 200ms base the first wait is about 100-300ms, the second about 200-600ms. 600ms caps only the second wait.
- **Breaker outermost, two reasons:** an open circuit fails fast with no retry wait, and it counts one failure per whole operation, not one per attempt.
- **Lost response on a write:** the app-side `timeout` (2s) cancels while Postgres may already have committed. It is not the HTTP response to the user.
- **P3b, server side of idempotency:** store the key under a unique constraint, atomic with the insert (otherwise two concurrent requests with the same key both pass). Store the original response and replay it. Same key with a different payload is rejected (422). A TTL cleans old keys.
- **`sliding-window-size 10`:** count-based window, the failure rate is computed over the last 10 calls. The 11th call evicts the oldest. The alternative is a time-based window.
- **`minimum-number-of-calls`:** with value 1, a single failed call is 1/1 = 100% >= 50% and the breaker opens on the first failure. With value 5 and threshold 50 it needs 3 failures out of 5 (60%); 2/5 = 40% does not open.
- **`wait-duration-in-open-state 10s`:** 1s makes the breaker flap, each half-open round sends 3 real calls to a dead DB (each waits 2s). 5 min keeps returning 503 for minutes after the DB is healthy again. 10s detects recovery fast and limits probe noise to 3 calls per 10s. Before arguing "switch to a replica", check `multi_az` in Terraform: a Multi-AZ failover takes longer than 10s, so `wait` alone does not cover it.
- **`permitted-number-of-calls-in-half-open-state 3`:** same threshold applies. 2 of 3 failing is 66% >= 50%, back to OPEN. 1 of 3 failing is 33%, CLOSED. The decision is taken when all 3 trial calls finish, extra calls are rejected meanwhile.
- **P6:** the breaker test config is `properties(100ms, 3, 2)`: threshold 50, window 10, `minimum = 2` (`TestResilience.java:20`). Two failures already open it (see the positive test, lines 144-167). The 5 iterations of the negative test are a safety margin above the minimum, not an exact count.
- **P7, `readMany` (`PersistenceResilience.java:61-63`):** `read(() -> call.get().collectList()).flatMapIterable(items -> items)`. The partial list of a failed attempt is discarded, the client never sees the first `1`. Without the buffer a retry would resubscribe and the client would see `1, 1, 2`. Cost: everything in memory and unbounded demand, the same trade-off as `collectList()` in top-stock (SPARRING.md decision 10).
- **`concat` is not `zip`:** `zip` combines elements pairwise into a `Tuple2` and subscribes to all sources at once. `concat` subscribes to the first, waits for it to complete, then subscribes to the next, emitting in order. Its relative is `concatMap`.

## Results summary

- Pass: P1, P2, P3b, P5, P5b, P6 (idea).
- Partial: P3a, P4 (wait, half-open), P7.
- Fail: `sliding-window-size`, `minimum-number-of-calls = 1`.

## Teaching note

The candidate asked for the code to be shown in every question ("the goal is not to hold the code in memory, only to explain it"). Rule applied from P5 on: paste the snippet, ask to explain it.
