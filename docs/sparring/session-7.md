# Session 7

Short session, the day before the defense. 4 questions, answers were brief (candidate says they speak better live than in writing).

| # | Area | Topic | Grade | Note |
|---|---|---|---|---|
| 1 | G | Curveball 12, "one more week" | Pass (second attempt) | Picked N+1 on top-stock: it breaks a rubric endpoint, worst case 6.6s. Still missing: say what is left out on purpose (auth: public ALB, throwaway environment, no sensitive data). |
| 2 | G | Curveball 13, part trusted least | Pass with gaps | `@Setter` in `Franchise`, `Branch`, `Product`, cause right (`ObjectMapperImp` maps through setters). Missing: evidence `ObjectMapperConfig.java:13`. `toBuilder()` only copies a model with a change, entity-to-model needs `builder()` in a hand-written mapper. Risk: any layer can mutate the domain, against the immutable pattern of the 4 `Patch*UseCase`. Cost: more code, compile-time checked, no reflection. |
| 3 | D | Auth bypass | Partial | Bypass and "only the gateway is public" right. Missed: `internal = true` on `aws_lb` is what removes public IPs (a private subnet alone is not enough), and the `aws_apigatewayv2_vpc_link` that lets the gateway reach it. SG restriction does not work (API Gateway has no fixed IPs). `authenticate-cognito` is an ALB listener action (OIDC login, session cookie): needs HTTPS and a domain, and fits browsers, not Bearer-token API clients. |
| 4 | D | Rollback with mutable `latest` | Partial | Right: `update-service --task-definition family:N-1`, deployment circuit breaker (`deployment_circuit_breaker { enable = true, rollback = true }`), semver plus SHA tags. Wrong: "the old image keeps the digest". Task definition N-1 references `repo:latest`, which now points to the new image, so rollback redeploys the bad one. Missed: ECR `image_tag_mutability = "IMMUTABLE"`. Breaker fires on tasks that never get healthy, not on logic errors. |

## Learned this session

- Same gap as session 6 (rollback with `latest`): the mechanism is known, the reason it fails with a mutable tag is not. Next time lead with "N-1 still says `latest`".
- Evidence questions: always give file and line, not only the idea.
- Candidate self-assessment: solid on theory, answers shorter in writing than live.
