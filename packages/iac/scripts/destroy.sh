#!/usr/bin/env bash
# Destroys one environment, or the bootstrap (state bucket) once every environment is gone.
# Usage: scripts/destroy.sh <dev|staging|prod|bootstrap> [--yes]
#   --yes  skips the interactive confirmation of Terraform.
# Order matters: environments first, bootstrap LAST. The bootstrap bucket holds the state of
# every environment (force_destroy = true), so destroying it early orphans live resources.
set -euo pipefail

TARGET="${1:-}"
AUTO_APPROVE=""
[[ "${2:-}" == "--yes" ]] && AUTO_APPROVE="-auto-approve"

case "$TARGET" in
  dev | staging | prod | bootstrap) ;;
  *)
    echo "Usage: $0 <dev|staging|prod|bootstrap> [--yes]" >&2
    exit 1
    ;;
esac

export AWS_PROFILE="${AWS_PROFILE:-nequi-tf}"

IAC_DIR="$(cd "$(dirname "$0")/.." && pwd)"
ACCOUNT_ID="$(aws sts get-caller-identity --query Account --output text)"
BUCKET="nequi-tfstate-$ACCOUNT_ID"

echo ">> Destroying '$TARGET' in account $ACCOUNT_ID, profile $AWS_PROFILE"

if [[ "$TARGET" == "bootstrap" ]]; then
  # Refuse while any environment state still lists resources.
  for env in dev staging prod; do
    # A missing state file means that environment was never deployed: nothing to protect.
    if COUNT="$(aws s3 cp "s3://$BUCKET/$env/terraform.tfstate" - 2>/dev/null | jq '.resources | length')"; then
      if [[ "$COUNT" -gt 0 ]]; then
        echo "Refusing: '$env' still has $COUNT resource group(s) in its state. Destroy it first." >&2
        exit 1
      fi
    fi
  done

  cd "$IAC_DIR/bootstrap"
  terraform init -input=false
  terraform destroy -input=false $AUTO_APPROVE
  echo ">> Bootstrap destroyed. The IAM users (terraform-deployer, nequi-tunnel) and the bastion role still exist."
  echo ">> To remove them, run with an ADMIN profile (not nequi-tf): AWS_PROFILE=<admin> scripts/99-destroy-iam.sh"
  exit 0
fi

# A tunnel left open (start-tunnel.sh killed without cleanup) leaves an EC2 outside Terraform's state.
# Its ENI sits in the ECS security group and a private subnet, so destroy would fail with
# DependencyViolation. Terminate it first. The deployer has ec2:*, no extra permissions needed.
BASTION_IDS="$(aws ec2 describe-instances \
  --filters "Name=tag:Name,Values=nequi-$TARGET-db-bastion" "Name=instance-state-name,Values=pending,running,stopping,stopped" \
  --query 'Reservations[].Instances[].InstanceId' --output text)"
if [[ -n "$BASTION_IDS" ]]; then
  echo ">> Tunnel bastion still alive ($BASTION_IDS): terminating before destroy"
  # shellcheck disable=SC2086  # space-separated list on purpose
  aws ec2 terminate-instances --instance-ids $BASTION_IDS >/dev/null
  # shellcheck disable=SC2086
  aws ec2 wait instance-terminated --instance-ids $BASTION_IDS
fi

cd "$IAC_DIR/environments/$TARGET"
terraform init -input=false -backend-config="bucket=$BUCKET"
terraform destroy -input=false -var-file="$TARGET.tfvars" $AUTO_APPROVE
echo ">> '$TARGET' destroyed. Check the console for leftovers (NAT gateway, Elastic IP, ALB)."
