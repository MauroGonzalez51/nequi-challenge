# Spec Delta

## Purpose

Pillar 3 of the challenge: the service runs in AWS and its infrastructure is defined as modular, multi-environment Terraform.

## ADDED Requirements

### Requirement: Containerized deployment on ECS
The application SHALL run on ECS (Fargate recommended) from a Docker image stored in ECR, exposed through an Application Load Balancer. A local Dockerfile alone is not sufficient.

#### Scenario: Cloud availability
- **WHEN** the ALB URL is requested at defense time
- **THEN** the API responds from AWS

### Requirement: Secrets management
Secrets SHALL be stored in AWS Secrets Manager. Plain-text credentials MUST NOT appear in code or as direct container environment variables.

#### Scenario: Credential scan
- **WHEN** the repo and task definition are inspected
- **THEN** no plain-text credential exists; secrets are injected from Secrets Manager

### Requirement: Least-privilege IAM
IAM roles SHALL grant only the permissions the task execution and task roles need.

#### Scenario: Wildcard policy
- **WHEN** a policy uses `*` actions or resources without justification
- **THEN** the review flags it

### Requirement: Horizontal autoscaling
The ECS service SHALL have Auto Scaling configured.

#### Scenario: Scaling policy
- **WHEN** the service is inspected
- **THEN** a scaling target and policy exist

### Requirement: Modular Terraform
Terraform code SHALL be split into modules, use `variables.tf` and `outputs.tf`, and MUST NOT be a monolithic `main.tf`.

#### Scenario: Structure check
- **WHEN** the Terraform tree is inspected
- **THEN** infrastructure is split into reusable modules with declared inputs and outputs

### Requirement: Remote state
Terraform state SHALL be stored remotely (S3 or equivalent) and the candidate SHALL be able to explain state handling.

#### Scenario: Backend check
- **WHEN** the backend configuration is inspected
- **THEN** it points to remote storage

### Requirement: Multiple environments
The code SHALL support dev, staging and prod through workspaces or parameterized modules, and reflect reproducibility, idempotency and versioning.

#### Scenario: Second environment
- **WHEN** a different environment is selected
- **THEN** the same code deploys it with different variable values
