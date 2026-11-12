# Spec Delta

## Purpose

Cloud persistence and the full resilience pattern set required by the challenge.

## ADDED Requirements

### Requirement: Cloud persistence
Data SHALL persist in a cloud store (MongoDB Atlas, RDS MySQL/PostgreSQL, DynamoDB or ElastiCache Redis), reachable from the AWS-deployed service and not only locally.

#### Scenario: Deployed persistence
- **WHEN** a franchise is created through the cloud-deployed API
- **THEN** it is stored in the cloud database and survives a service restart

### Requirement: Circuit Breaker
Persistence calls SHALL be protected by a Resilience4j Circuit Breaker that opens after repeated failures.

#### Scenario: Repeated failures
- **WHEN** the configured failure threshold is exceeded
- **THEN** the circuit opens and calls fail fast with a controlled error

### Requirement: Timeout
Operations SHALL have a maximum wait time.

#### Scenario: Slow dependency
- **WHEN** a call exceeds the timeout
- **THEN** it is cancelled and a controlled error is returned

### Requirement: Retry with backoff
Transient failures SHALL be retried with a configured backoff.

#### Scenario: Transient failure
- **WHEN** a call fails transiently
- **THEN** it is retried with backoff up to the configured limit

### Requirement: Backpressure explanation
The candidate SHALL explain how WebFlux manages backpressure natively and where it is relevant.

#### Scenario: Defense question
- **WHEN** asked about backpressure
- **THEN** the candidate relates it to demand signaling in the chosen driver and endpoints
