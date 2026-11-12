# Design

## Context

The candidate writes all code; Claude is evaluator and mentor. The repo already has a Gradle bootstrap in `apps/api` (Bancolombia clean-architecture plugin, modules `domain/model`, `domain/usecase`, `infrastructure/entry-points/reactive-web`, `applications/app-service`). No Terraform exists yet. See proposal.md for motivation.

## Goals / Non-Goals

**Goals:**
- Keep the challenge requirements locally, in checkable form, so they never need re-reading from the Google Doc.
- Make Claude's role explicit so help builds understanding instead of replacing it.

**Non-Goals:**
- Claude does not implement the API, Terraform or tests.
- No new requirements beyond the source document.

## Decisions

**Evaluator protocol.** Claude reviews code the candidate wrote, checks it against the specs, and reports pass/fail with the reason. When asked for help, Claude explains the concept and points at the relevant spec or docs, and asks the candidate to write the fix. Claude writes production code only on explicit request. Alternative considered: Claude implements and the candidate reviews; rejected because the defense requires line-by-line understanding.

**Specs per pillar.** One capability per grading area, so each can be reviewed independently. Alternative: a single spec; rejected as hard to navigate.

**Disqualification checks are mechanical.** `@RestController`, `.block(`, `java.util.stream`, `javax.`, `try {` inside flows and `if (` inside flows can be grepped, so Claude runs them on request.

**Persistence is a candidate choice.** The doc allows MongoDB Atlas, RDS, DynamoDB or Redis. The spec stays store-neutral. Claude recommended DynamoDB or MongoDB Atlas; the final choice is recorded under Open Questions.

## Risks / Trade-offs

- [Rubric drifts from the source doc] → Specs were written from the full doc text (paragraphs and tables); re-sync if the doc changes.
- [Grep checks miss edge cases such as `if` inside lambdas that are not reactive flows] → Claude explains each finding and the candidate decides.
- [Candidate leans on Claude for code] → Protocol above; defense explainability requirement.

## Resolved Decisions

- Persistence: PostgreSQL via R2DBC (RDS PostgreSQL in AWS). Schema created by `schema.sql` run at startup with `spring.sql.init.mode=always`; connection data from environment variables (`.env` locally, Secrets Manager in AWS).
