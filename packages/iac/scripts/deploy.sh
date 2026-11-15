#!/usr/bin/env bash
# Manual deploy of one environment: jar -> ECR repo -> image -> full apply -> rollout.
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
LIBS_DIR="$API_DIR/applications/app-service/build/libs"

ACCOUNT_ID="$(aws sts get-caller-identity --query Account --output text)"
REGISTRY="$ACCOUNT_ID.dkr.ecr.$REGION.amazonaws.com"
# Same name the environment builds in main.tf: "${local.name}-api" with local.name = nequi-<env>.
IMAGE="$REGISTRY/nequi-$ENVIRONMENT-api:$IMAGE_TAG"

echo ">> Deploying '$ENVIRONMENT' to account $ACCOUNT_ID ($REGION), profile $AWS_PROFILE"

echo ">> [1/6] Building the jar"
(cd "$API_DIR" && ./gradlew bootJar -q)

echo ">> [2/6] terraform init"
cd "$ENV_DIR"
terraform init -input=false -backend-config="bucket=nequi-tfstate-$ACCOUNT_ID"

# The repo must exist before the push, and the ECS service needs the image to stabilize.
# -target is normally discouraged; here it only breaks that chicken-and-egg on the first run.
echo ">> [3/6] Creating the ECR repository"
terraform apply -input=false -target=module.ecr -var-file="$ENVIRONMENT.tfvars" $AUTO_APPROVE

echo ">> [4/6] Building and pushing the image"
aws ecr get-login-password --region "$REGION" | docker login --username AWS --password-stdin "$REGISTRY"
# Fargate runs x86_64 by default. Build context is the jar folder: the Dockerfile does COPY *.jar.
docker build --platform linux/amd64 -f "$API_DIR/deployment/Dockerfile" -t "$IMAGE" "$LIBS_DIR"
docker push "$IMAGE"

echo ">> [5/6] terraform apply"
terraform apply -input=false -var-file="$ENVIRONMENT.tfvars" -var="image_tag=$IMAGE_TAG" $AUTO_APPROVE

# :latest is a mutable tag, so Terraform sees no change. Force ECS to pull the new image.
echo ">> [6/6] Rolling out the new image"
CLUSTER="$(terraform output -raw cluster_name)"
SERVICE="$(terraform output -raw service_name)"
aws ecs update-service --cluster "$CLUSTER" --service "$SERVICE" --force-new-deployment >/dev/null
aws ecs wait services-stable --cluster "$CLUSTER" --services "$SERVICE"

API_URL="$(terraform output -raw api_url)"
echo ">> Done. Health: $(curl -s -m 10 "$API_URL/actuator/health")"
echo ">> API: $API_URL"
