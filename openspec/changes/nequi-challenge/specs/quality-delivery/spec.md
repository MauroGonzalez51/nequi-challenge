# Spec Delta

## Purpose

Testing, documentation, packaging and delivery requirements, including the defense expectation that the candidate understands every line.

## ADDED Requirements

### Requirement: Reactive tests
Reactive flows SHALL be tested with `StepVerifier` (reactor-test); tests MUST NOT use `.block()`. Unit tests SHALL use JUnit 5 and Mockito, plus integration tests where appropriate.

#### Scenario: Test review
- **WHEN** tests are inspected
- **THEN** they verify business scenarios (not just getters/setters or models) via StepVerifier

### Requirement: Coverage
JaCoCo coverage reporting SHALL be configured.

#### Scenario: Report generation
- **WHEN** the build's test task runs
- **THEN** a JaCoCo report is produced

### Requirement: API documentation
All endpoints SHALL be documented with OpenAPI/Swagger, including request and response examples.

#### Scenario: Swagger coverage
- **WHEN** the OpenAPI document is opened
- **THEN** all nine endpoints appear with examples

### Requirement: Reproducible README
`README.md` SHALL let anyone build and run the project from scratch using only its steps, verified by the candidate before delivery.

#### Scenario: Fresh setup
- **WHEN** someone follows only the README on a clean machine
- **THEN** the project runs

### Requirement: Docker packaging
The app SHALL be packaged with Docker and the image published to ECR. Multi-stage build and secure practices SHOULD be followed.

#### Scenario: Image in registry
- **WHEN** ECR is inspected
- **THEN** the application image is present

### Requirement: English code
All identifiers and technical comments SHALL be in English.

#### Scenario: Language scan
- **WHEN** sources are inspected
- **THEN** no non-English identifiers or technical comments exist

### Requirement: Repository and delivery
The code SHALL live in a public GitHub or GitLab repo with a clean, descriptive commit history showing real progress, and the app SHALL be deployed and reachable at defense time.

#### Scenario: History review
- **WHEN** the commit log is read
- **THEN** commits are descriptive and incremental

### Requirement: Explainability
The candidate SHALL be able to explain any line of code and justify decisions; AI assistance is allowed but unexplained code counts as unknown.

#### Scenario: Line-by-line question
- **WHEN** the evaluator asks why a fragment exists
- **THEN** the candidate justifies it
