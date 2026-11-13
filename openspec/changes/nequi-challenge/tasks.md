# Tasks

Candidate writes the code. Claude verifies each item against the named spec and marks it only after the stated check passes.

## 1. Project foundation

- [x] 1.1 Decide the persistence store and record it in design.md; verified when the decision is listed there
- [x] 1.2 Confirm Gradle modules follow the Bancolombia scaffold (domain, infrastructure, entrypoints); verified with `./gradlew build` passing in `apps/api` and the layer review against `clean-architecture`
- [x] 1.3 Confirm Lombok, Java 17 and `jakarta.*` conventions; verified by a search finding no `javax.` imports

## 2. Domain model and use cases

- [x] 2.1 Model Franchise, Branch and Product as `@Builder` entities and declare ports in the domain; verified by review against `franchise-api` and `clean-architecture`
- [x] 2.2 Implement use cases for create franchise, add branch, add product, remove product, update stock; verified by StepVerifier unit tests covering success and not-found scenarios
- [x] 2.3 Implement the top-stock-per-branch use case in the domain; verified by a StepVerifier test with several branches and an empty branch
- [x] 2.4 Implement the three rename use cases; verified by StepVerifier tests for success and not-found

## 3. Reactive entrypoints

- [x] 3.1 Expose the nine endpoints with RouterFunctions and Handlers using Record DTOs and jakarta validation; verified by handler tests via WebTestClient (no `.block()`)
- [x] 3.2 Handle errors with `onErrorResume`/`onErrorReturn` mapped to HTTP statuses; verified by tests for 400 and 404 responses
- [x] 3.3 Run the disqualification scan (`@RestController`, `.block(`, `java.util.stream`, `try`/`if` in flows); verified when it returns no hits

## 4. Persistence and resilience

- [ ] 4.1 Implement the persistence adapter with the chosen cloud store behind the domain ports; verified by an integration test and a successful run against the cloud store
- [x] 4.2 Add Circuit Breaker, Timeout and Retry with backoff on persistence calls; verified by tests that trigger each pattern
- [x] 4.3 Be able to explain backpressure for this API; verified by a verbal walkthrough reviewed by Claude

## 5. Quality and docs

- [x] 5.1 Configure JaCoCo; verified by a generated report after `./gradlew test`
- [x] 5.2 Document all endpoints with OpenAPI including examples; verified by opening Swagger UI and finding all nine endpoints with examples
- [ ] 5.3 Write the README and verify it from a clean clone; verified by following only its steps successfully

## 6. Docker and AWS

- [ ] 6.1 Write a multi-stage, non-root Dockerfile; verified by building and running the container locally
- [ ] 6.2 Create modular Terraform (network, ECR, ECS, ALB, IAM, secrets, autoscaling, database) with `variables.tf`, `outputs.tf` and S3 remote state; verified by `terraform validate` and `terraform plan`
- [ ] 6.3 Support dev, staging and prod through workspaces or parameterized modules; verified by a plan for two environments
- [ ] 6.4 Store secrets in Secrets Manager and scope IAM to least privilege; verified by a scan showing no plaintext credentials or wildcard policies
- [ ] 6.5 Push the image to ECR and deploy; verified by a successful request to the ALB URL

## 7. Delivery readiness

- [ ] 7.1 Public repo with a clean descriptive commit history; verified by reading `git log`
- [ ] 7.2 Defense dry run: explain any chosen file line by line; verified by Claude questioning the candidate on each pillar and the study topics in the source doc
