#!/usr/bin/env bash
# One-time setup of a fresh AWS account: deployer user (01), local access key (02), bootstrap.
# Usage: scripts/init.sh [--yes]
#   --yes  skips the interactive confirmation of the bootstrap apply.
# Run it with ADMIN credentials (root or an admin profile), NOT with the nequi-tf profile:
# steps 01 and 02 create that user. If you already run as the deployer, it only does the bootstrap.
# Re-runnable: each step is skipped or converges when its work is already done.
set -euo pipefail

AUTO_APPROVE=""
[[ "${1:-}" == "--yes" ]] && AUTO_APPROVE="-auto-approve"

# Same variables the child scripts read, exported so all three agree.
export USER_NAME="${USER_NAME:-terraform-deployer}"
export PROFILE="${PROFILE:-nequi-tf}"
export REGION="${REGION:-us-east-1}"

SCRIPTS_DIR="$(cd "$(dirname "$0")" && pwd)"
BOOTSTRAP_DIR="$SCRIPTS_DIR/../bootstrap"

# Prints the ARN of the credentials in use, or nothing when they are missing or invalid.
caller_arn() {
  aws sts get-caller-identity --query Arn --output text 2>/dev/null || true
}

deployer_ready() {
  [[ "$(AWS_PROFILE="$PROFILE" caller_arn)" == *"user/$USER_NAME" ]]
}

ARN="$(caller_arn)"
if [[ -z "$ARN" ]]; then
  echo "No valid AWS credentials. Export an admin profile first (AWS_PROFILE=<admin>)." >&2
  exit 1
fi
echo ">> Running as: $ARN"

if [[ "$ARN" == *"user/$USER_NAME" ]]; then
  echo ">> Already the deployer: skipping steps 01 and 02."
else
  echo ">> [1/3] Deployer user, group and policy (01)"
  "$SCRIPTS_DIR/01-create-deployer.sh"

  echo ">> [2/3] Access key and local profile '$PROFILE' (02)"
  if deployer_ready; then
    echo "   Profile '$PROFILE' already works: skipping 02."
  else
    # An IAM user holds at most 2 keys. A third one fails, so stop with the fix instead.
    KEYS="$(aws iam list-access-keys --user-name "$USER_NAME" --query 'length(AccessKeyMetadata)' --output text)"
    if [[ "$KEYS" -ge 2 ]]; then
      echo "User '$USER_NAME' already has 2 access keys. Delete one and re-run:" >&2
      echo "  aws iam list-access-keys --user-name $USER_NAME" >&2
      echo "  aws iam delete-access-key --user-name $USER_NAME --access-key-id <id>" >&2
      exit 1
    fi
    "$SCRIPTS_DIR/02-create-access-key.sh"

    # A new key takes a few seconds to propagate across IAM.
    echo "   Waiting for the new key to become valid..."
    for _ in $(seq 1 10); do
      deployer_ready && break
      sleep 3
    done
    deployer_ready || { echo "Profile '$PROFILE' still invalid after 30s. Retry in a minute." >&2; exit 1; }
  fi
fi

echo ">> [3/3] Bootstrap (state bucket)"
export AWS_PROFILE="$PROFILE"
cd "$BOOTSTRAP_DIR"
terraform init -input=false
terraform apply -input=false $AUTO_APPROVE

echo ">> Done. Deploy an environment with: scripts/deploy.sh dev"
