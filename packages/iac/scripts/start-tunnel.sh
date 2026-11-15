#!/usr/bin/env bash
# Opens a tunnel from localhost to the private RDS of one environment, through a throwaway EC2.
# Usage: scripts/start-tunnel.sh <dev|staging|prod> [local-port]
#   Profile: AWS_PROFILE, default 'nequi-tunnel' (created by 03-create-tunnel-user.sh via init.sh).
#
# Creates ONE t3.nano in the private subnet carrying the ECS security group (the one RDS already
# trusts, so no rule changes), then an SSM port-forward to RDS. Ctrl+C or SIGTERM terminates it.
# The EC2 is not in Terraform state: destroy.sh cannot see it, and it holds an ENI in Terraform's
# SG and subnet, so it must never outlive the tunnel. The role it uses is permanent and harmless.
set -euo pipefail

ENV="${1:-}"
LOCAL_PORT="${2:-15432}"

case "$ENV" in
  dev | staging | prod) ;;
  *)
    echo "Usage: $0 <dev|staging|prod> [local-port]" >&2
    exit 1
    ;;
esac

export AWS_PROFILE="${AWS_PROFILE:-nequi-tunnel}"
export AWS_REGION="${REGION:-us-east-1}"
export AWS_DEFAULT_REGION="$AWS_REGION"
export AWS_PAGER=""

INSTANCE_NAME="nequi-$ENV-db-bastion"
INSTANCE_PROFILE="nequi-db-bastion" # created by 03-create-tunnel-user.sh
PASS_FILE="" # filled later; nuke() deletes it

die() {
  echo "$*" >&2
  exit 1
}

# ---------------------------------------------------------------------------
# Guards
# ---------------------------------------------------------------------------
command -v session-manager-plugin >/dev/null || die "session-manager-plugin not found"
command -v jq >/dev/null || die "jq not found."
aws sts get-caller-identity >/dev/null 2>&1 || die "Profile '$AWS_PROFILE' is invalid. Run scripts/init.sh first."

# ---------------------------------------------------------------------------
# Cleanup: terminates the bastion of THIS environment. Idempotent. Also runs BEFORE create
# to sweep one left by a run that died without cleaning up (kill -9, power loss).
# Subshell body: its `set +e` cannot leak into the main script.
# ---------------------------------------------------------------------------
sweep() (
  set +e

  ids="$(aws ec2 describe-instances \
    --filters "Name=tag:Name,Values=$INSTANCE_NAME" "Name=instance-state-name,Values=pending,running,stopping,stopped" \
    --query 'Reservations[].Instances[].InstanceId' --output text 2>/dev/null)"
  [[ -n "$ids" ]] || exit 0

  echo ">> Terminating $ids"
  # shellcheck disable=SC2086  # ids is a space-separated list on purpose
  aws ec2 terminate-instances --instance-ids $ids >/dev/null
  # shellcheck disable=SC2086
  aws ec2 wait instance-terminated --instance-ids $ids
  echo ">> Done: instance terminated."
)

nuke() {
  # A second Ctrl+C during cleanup would leave the instance behind.
  trap '' INT TERM
  if [[ -n "$PASS_FILE" ]]; then rm -f -- "$PASS_FILE"; fi
  sweep
}

sweep >/dev/null 2>&1

# EXIT covers normal and error exits; INT and TERM turn the signal into an exit so EXIT fires once.
trap nuke EXIT
trap 'exit 130' INT
trap 'exit 143' TERM

# ---------------------------------------------------------------------------
# Lookups
# ---------------------------------------------------------------------------
SG_ID="$(aws ec2 describe-security-groups \
  --filters "Name=group-name,Values=nequi-$ENV-ecs" \
  --query 'SecurityGroups[0].GroupId' --output text)"
[[ "$SG_ID" != "None" ]] || die "Security group nequi-$ENV-ecs not found. Is '$ENV' deployed in $AWS_REGION?"

SUBNET_ID="$(aws ec2 describe-subnets \
  --filters "Name=tag:Name,Values=nequi-$ENV-private-*" \
  --query 'Subnets[0].SubnetId' --output text)"
[[ "$SUBNET_ID" != "None" ]] || die "No private subnet found for '$ENV'."

AMI="$(aws ssm get-parameter \
  --name /aws/service/ami-amazon-linux-latest/al2023-ami-kernel-default-x86_64 \
  --query Parameter.Value --output text)"

SECRET="$(aws secretsmanager get-secret-value --secret-id "nequi-$ENV/db" --query SecretString --output text)"
DB_HOST="$(jq -r .DB_HOST <<<"$SECRET")"
DB_PORT="$(jq -r .DB_PORT <<<"$SECRET")"

# ---------------------------------------------------------------------------
# EC2 + SSM agent
# ---------------------------------------------------------------------------
echo ">> Launching EC2 in $SUBNET_ID with $SG_ID"
INSTANCE_ID="$(aws ec2 run-instances \
  --image-id "$AMI" --instance-type t3.nano \
  --subnet-id "$SUBNET_ID" --security-group-ids "$SG_ID" \
  --iam-instance-profile "Name=$INSTANCE_PROFILE" \
  --metadata-options HttpTokens=required \
  --tag-specifications "ResourceType=instance,Tags=[{Key=Name,Value=$INSTANCE_NAME}]" \
  --query 'Instances[0].InstanceId' --output text)"

echo ">> Waiting for the SSM agent on $INSTANCE_ID (1 to 2 min)"
STATUS=""
for _ in $(seq 1 30); do
  STATUS="$(aws ssm describe-instance-information \
    --filters "Key=InstanceIds,Values=$INSTANCE_ID" \
    --query 'InstanceInformationList[0].PingStatus' --output text 2>/dev/null || true)"
  [[ "$STATUS" == "Online" ]] && break
  sleep 10
done
[[ "$STATUS" == "Online" ]] || die "SSM agent never came online."

# ---------------------------------------------------------------------------
# Credentials. The password never reaches the terminal or the clipboard (clipboard managers keep
# history, scrollback keeps output). It goes to a 0600 file in pgpass format, deleted on exit.
# ---------------------------------------------------------------------------
DB_NAME="$(jq -r .DB_NAME <<<"$SECRET")"
DB_USER="$(jq -r .DB_USERNAME <<<"$SECRET")"

# umask 077 in a subshell: the file is born 0600, there is no window where others can read it.
PASS_FILE="$(umask 077 && mktemp "${TMPDIR:-/tmp}/nequi-$ENV-pgpass.XXXXXX")"
jq -r --arg port "$LOCAL_PORT" '"localhost:\($port):\(.DB_NAME):\(.DB_USERNAME):\(.DB_PASSWORD)"' <<<"$SECRET" >"$PASS_FILE"
unset SECRET

echo
echo "  Host:          localhost"
echo "  Port:          $LOCAL_PORT"
echo "  Database:      $DB_NAME"
echo "  User:          $DB_USER"
echo "  SSL:           require (DBeaver > SSL tab > Use SSL)"
echo "  Password file: $PASS_FILE (0600, pgpass format, deleted on exit)"
echo "  psql:          PGPASSFILE=$PASS_FILE psql 'host=localhost port=$LOCAL_PORT dbname=$DB_NAME user=$DB_USER sslmode=require'"
echo
echo ">> Tunnel open on localhost:$LOCAL_PORT. Ctrl+C closes it and terminates the EC2."

# Foreground on purpose: Ctrl+C reaches this process and the script then runs the EXIT trap.
aws ssm start-session --target "$INSTANCE_ID" \
  --document-name AWS-StartPortForwardingSessionToRemoteHost \
  --parameters "{\"host\":[\"$DB_HOST\"],\"portNumber\":[\"$DB_PORT\"],\"localPortNumber\":[\"$LOCAL_PORT\"]}"
