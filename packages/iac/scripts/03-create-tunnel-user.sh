#!/usr/bin/env bash
# Run ONCE per account, as root or an admin (init.sh calls it).
# Creates what start-tunnel.sh needs, so start-tunnel.sh itself needs almost no IAM power:
#   - role + instance profile 'nequi-db-bastion' (SSM only), attached to the throwaway EC2
#   - IAM user 'nequi-tunnel' in group 'nequi-tunnelers', which holds policies/tunnel-policy.json
#   - its access key, saved in the local profile 'nequi-tunnel' (the secret is never printed)
# Idempotent: re-running converges. Re-running also refreshes the user policy.
# An IAM user holds at most 2 keys: delete an old one first if the key step fails.
set -euo pipefail

# Not USER_NAME / PROFILE: init.sh exports those for the deployer and they would clash.
TUNNEL_USER="${TUNNEL_USER:-nequi-tunnel}"
TUNNEL_PROFILE="${TUNNEL_PROFILE:-nequi-tunnel}"
REGION="${REGION:-us-east-1}"
ROLE_NAME="nequi-db-bastion"
TUNNEL_GROUP="${TUNNEL_GROUP:-nequi-tunnelers}"
POLICY_NAME="nequi-tunnel"
POLICY_FILE="$(dirname "$0")/../policies/tunnel-policy.json"
SSM_POLICY="arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore"

echo "Account: $(aws sts get-caller-identity --query Account --output text)"

# --- Role the EC2 assumes: it gets temporary credentials from it, no access key involved. ---
aws iam get-role --role-name "$ROLE_NAME" >/dev/null 2>&1 \
  || aws iam create-role --role-name "$ROLE_NAME" \
    --assume-role-policy-document '{"Version":"2012-10-17","Statement":[{"Effect":"Allow","Principal":{"Service":"ec2.amazonaws.com"},"Action":"sts:AssumeRole"}]}' >/dev/null
aws iam attach-role-policy --role-name "$ROLE_NAME" --policy-arn "$SSM_POLICY"

aws iam get-instance-profile --instance-profile-name "$ROLE_NAME" >/dev/null 2>&1 \
  || aws iam create-instance-profile --instance-profile-name "$ROLE_NAME" >/dev/null
# Fails with LimitExceeded when the role is already in the profile: that is the goal state.
aws iam add-role-to-instance-profile --instance-profile-name "$ROLE_NAME" --role-name "$ROLE_NAME" 2>/dev/null || true

# --- Group, policy and user. The policy goes on a GROUP, not on the user: inline user policies cap
# at 2048 bytes, group ones at 5120. put-group-policy replaces the policy, so re-running updates it. ---
aws iam get-group --group-name "$TUNNEL_GROUP" >/dev/null 2>&1 \
  || aws iam create-group --group-name "$TUNNEL_GROUP" >/dev/null
aws iam put-group-policy --group-name "$TUNNEL_GROUP" --policy-name "$POLICY_NAME" --policy-document "file://$POLICY_FILE"

aws iam get-user --user-name "$TUNNEL_USER" >/dev/null 2>&1 \
  || aws iam create-user --user-name "$TUNNEL_USER" >/dev/null
aws iam add-user-to-group --group-name "$TUNNEL_GROUP" --user-name "$TUNNEL_USER"

# --- Access key + local profile, only when the profile does not already work. ---
profile_ready() {
  [[ "$(AWS_PROFILE="$TUNNEL_PROFILE" aws sts get-caller-identity --query Arn --output text 2>/dev/null || true)" == *"user/$TUNNEL_USER" ]]
}

if profile_ready; then
  echo "Profile '$TUNNEL_PROFILE' already works: skipping the access key."
else
  KEYS="$(aws iam list-access-keys --user-name "$TUNNEL_USER" --query 'length(AccessKeyMetadata)' --output text)"
  if [[ "$KEYS" -ge 2 ]]; then
    echo "User '$TUNNEL_USER' already has 2 access keys. Delete one and re-run:" >&2
    echo "  aws iam list-access-keys --user-name $TUNNEL_USER" >&2
    echo "  aws iam delete-access-key --user-name $TUNNEL_USER --access-key-id <id>" >&2
    exit 1
  fi

  read -r ACCESS_KEY_ID SECRET_ACCESS_KEY < <(
    aws iam create-access-key --user-name "$TUNNEL_USER" \
      --query 'AccessKey.[AccessKeyId,SecretAccessKey]' --output text
  )
  aws configure set aws_access_key_id "$ACCESS_KEY_ID" --profile "$TUNNEL_PROFILE"
  aws configure set aws_secret_access_key "$SECRET_ACCESS_KEY" --profile "$TUNNEL_PROFILE"
  aws configure set region "$REGION" --profile "$TUNNEL_PROFILE"
  chmod 600 "$HOME/.aws/credentials"

  # A new key takes a few seconds to propagate across IAM.
  for _ in $(seq 1 10); do
    profile_ready && break
    sleep 3
  done
  profile_ready || { echo "Profile '$TUNNEL_PROFILE' still invalid after 30s. Retry in a minute." >&2; exit 1; }
  echo "Profile '$TUNNEL_PROFILE' saved (key $ACCESS_KEY_ID)."
fi

echo "Done. Open a tunnel with: scripts/start-tunnel.sh <dev|staging|prod>"
