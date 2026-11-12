# Spec Delta

## Purpose

Pillar 1 of the challenge: the API is reactive end to end with Spring WebFlux. Violating the listed rules disqualifies the candidate.

## ADDED Requirements

### Requirement: Functional routing only
The application SHALL define endpoints exclusively with RouterFunctions and Handlers. `@RestController` MUST NOT be used.

#### Scenario: Controller annotation present
- **WHEN** any class carries `@RestController`
- **THEN** the rubric is failed (automatic disqualification)

### Requirement: No blocking
The application MUST NOT call `.block()` (or `blockFirst`/`blockLast`) anywhere, tests included, and MUST NOT mix Spring MVC with WebFlux.

#### Scenario: Blocking call found
- **WHEN** a search of main and test sources finds `.block(`
- **THEN** the rubric is failed

### Requirement: No Java Streams in reactive flows
Code inside reactive flows MUST NOT use `java.util.stream`.

#### Scenario: Stream import in a flow
- **WHEN** a reactive flow uses `.stream()` or imports `java.util.stream`
- **THEN** the rubric is failed

### Requirement: Operators instead of branching and try-catch
Conditional logic inside flows SHALL use operators (`filter`, `switchIfEmpty`, `map`, `flatMap`, ...), not `if`. Errors SHALL be handled in Handlers or with reactive operators (`onErrorResume`, `onErrorReturn`), never `try-catch` inside a flow.

#### Scenario: if or try-catch in a flow
- **WHEN** a reactive flow contains `if` or `try-catch`
- **THEN** the rubric is failed

### Requirement: Correct map and flatMap
`flatMap` SHALL be used when the inner operation returns a Publisher; `map` SHALL be used for synchronous transformations.

#### Scenario: Misuse
- **WHEN** `map` wraps a Mono/Flux-returning call, or `flatMap` wraps a plain synchronous value
- **THEN** the review flags it for correction

### Requirement: Backpressure understanding
The candidate SHALL be able to explain how WebFlux and Reactor handle backpressure and where it matters in this API.

#### Scenario: Defense question
- **WHEN** asked how backpressure works
- **THEN** the candidate explains subscriber demand (`request(n)`) and a relevant spot (for example the multi-branch listing)
