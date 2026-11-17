#!/usr/bin/env bash
# Manual deploy of one environment: ECR repo -> image (built inside Docker) -> full apply -> rollout.
# Usage: scripts/deploy.sh <dev|staging|prod> [--yes]
#   --yes  skips the interactive "apply" confirmation of Terraform.
# Re-runnable: every step is idempotent (terraform converges, the push overwrites :latest).
set -euo pipefail

ENVIRONMENT="${1:-}"
AUTO_APPROVE=""
[[ "${2:-}" == "--yes" ]] && AUTO_APPROVE="-auto-approve"

case "$ENVIRONMENT" in
  dev | staging | prod) ;;
  *)
    echo "Usage: $0 <dev|staging|prod> [--yes]" >&2
    exit 1
    ;;
esac

export AWS_PROFILE="${AWS_PROFILE:-nequi-tf}"
REGION="${REGION:-us-east-1}"
IMAGE_TAG="${IMAGE_TAG:-latest}"

IAC_DIR="$(cd "$(dirname "$0")/.." && pwd)"
API_DIR="$IAC_DIR/../../apps/api"
ENV_DIR="$IAC_DIR/environments/$ENVIRONMENT"

ACCOUNT_ID="$(aws sts get-caller-identity --query Account --output text)"
REGISTRY="$ACCOUNT_ID.dkr.ecr.$REGION.amazonaws.com"
# Same name the environment builds in main.tf: "${local.name}-api" with local.name = nequi-<env>.
IMAGE="$REGISTRY/nequi-$ENVIRONMENT-api:$IMAGE_TAG"

echo ">> Deploying '$ENVIRONMENT' to account $ACCOUNT_ID ($REGION), profile $AWS_PROFILE"

echo ">> [1/5] terraform init"
cd "$ENV_DIR"
terraform init -input=false -backend-config="bucket=nequi-tfstate-$ACCOUNT_ID"

# The repo must exist before the push, and the ECS service needs the image to stabilize.
# -target is normally discouraged; here it only breaks that chicken-and-egg on the first run.
echo ">> [2/5] Creating the ECR repository"
terraform apply -input=false -target=module.ecr -var-file="$ENVIRONMENT.tfvars" $AUTO_APPROVE

echo ">> [3/5] Building and pushing the image"
aws ecr get-login-password --region "$REGION" | docker login --username AWS --password-stdin "$REGISTRY"
# Fargate runs x86_64 by default. The Dockerfile is multi-stage and compiles the jar itself,
# so the build context is the whole API project (apps/api), not a prebuilt jar folder.
docker build --platform linux/amd64 -f "$API_DIR/deployment/Dockerfile" -t "$IMAGE" "$API_DIR"
docker push "$IMAGE"

echo ">> [4/5] terraform apply"
terraform apply -input=false -var-file="$ENVIRONMENT.tfvars" -var="image_tag=$IMAGE_TAG" $AUTO_APPROVE

# :latest is a mutable tag, so Terraform sees no change. Force ECS to pull the new image.
echo ">> [5/5] Rolling out the new image"
CLUSTER="$(terraform output -raw cluster_name)"
SERVICE="$(terraform output -raw service_name)"
aws ecs update-service --cluster "$CLUSTER" --service "$SERVICE" --force-new-deployment >/dev/null
aws ecs wait services-stable --cluster "$CLUSTER" --services "$SERVICE"

API_URL="$(terraform output -raw api_url)"
# services-stable only means ECS is steady. The ALB still needs a few health checks before it
# routes to the new task, and until then it answers 503. Poll until the API itself says UP.
echo ">> Waiting for the API to report healthy"
STATUS=""
for _ in $(seq 1 30); do
  STATUS="$(curl -fsS -m 10 "$API_URL/actuator/health" 2>/dev/null | jq -r .status 2>/dev/null || true)"
  [[ "$STATUS" == "UP" ]] && break
  sleep 5
done
[[ "$STATUS" == "UP" ]] || { echo "API did not become healthy after 150s: $API_URL" >&2; exit 1; }

echo ">> Done. Health: $STATUS"
echo ">> API: $API_URL"
