# Proposal

## Why

The Nequi technical challenge (reto_nequi_v2, July 2026) is graded at a live defense where the candidate must explain every line of code. The source requirements live in a Google Doc that is slow to re-read. This change keeps a local, testable copy of the requirements so Claude can act as evaluator and mentor while the candidate writes all code personally.

## What Changes

- Add a local requirements rubric, expressed as OpenSpec specs, covering the whole challenge: functional endpoints, three technical pillars, resilience, persistence, testing, docs, packaging and delivery.
- Add an evaluator protocol (design.md): Claude reviews and explains, does not write production code unless explicitly asked.
- Add a task checklist the candidate ticks off as each rubric area is built and verified.
- No application code is changed by this proposal.

## Capabilities

### New Capabilities
- `franchise-api`: the 9 functional endpoints and the franchise/branch/product data model.
- `reactive-webflux`: Pillar 1, end-to-end reactive rules and automatic-disqualification list.
- `clean-architecture`: Pillar 2, Bancolombia scaffold, hexagonal layering, Lombok/Records/Jakarta conventions.
- `aws-terraform-deployment`: Pillar 3, ECS Fargate/ECR/ALB/Secrets Manager/IAM/autoscaling and modular multi-environment Terraform.
- `resilience-persistence`: cloud persistence plus Circuit Breaker, Timeout, Retry and Backpressure.
- `quality-delivery`: StepVerifier/JaCoCo tests, OpenAPI docs, README, Docker/ECR, English-only code, repository and defense delivery.

### Modified Capabilities

None.

## Impact

- Only `openspec/` files are added. No effect on `apps/api`.
- Defines how later work in `apps/api` (Gradle, Bancolombia clean-architecture modules) and future Terraform code is judged.
