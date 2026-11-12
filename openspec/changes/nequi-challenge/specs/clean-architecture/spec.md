# Spec Delta

## Purpose

Pillar 2 of the challenge: hexagonal architecture following the Bancolombia Clean Architecture scaffold, with the language conventions the evaluator checks.

## ADDED Requirements

### Requirement: Scaffold and layering
The project SHALL follow the Bancolombia Clean Architecture scaffold with domain (entities, ports, use cases), infrastructure (adapters, repositories, configuration) and entrypoints (handlers, routers) separated.

#### Scenario: Dependency direction
- **WHEN** imports are inspected
- **THEN** domain depends on no framework or infrastructure code, and infrastructure/entrypoints depend on domain

### Requirement: Business rules live in the domain
Business rules SHALL live in the domain layer and MUST NOT appear in infrastructure code.

#### Scenario: Rule in an adapter
- **WHEN** a repository or handler implements a business decision (for example choosing the top-stock product)
- **THEN** the review flags it to move into a use case

### Requirement: Ports in domain, adapters in infrastructure
Port interfaces SHALL be declared in the domain; implementations SHALL be adapters in infrastructure.

#### Scenario: Port location
- **WHEN** a repository interface is declared outside the domain
- **THEN** the review flags it

### Requirement: Language conventions
Domain entities SHALL use Lombok `@Builder`. DTOs and transfer objects SHALL use Java Records (Java 17). The code SHALL use `jakarta.*`, never `javax.*`.

#### Scenario: Convention check
- **WHEN** sources are searched for `javax.` imports or mutable DTO classes
- **THEN** none are found
