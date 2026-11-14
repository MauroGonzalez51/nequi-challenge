#!/usr/bin/env bash
# Step 0, run ONCE per AWS account, as root or an admin.
# Creates the IAM group, the policy and the user that Terraform will use.
# Idempotent: safe to re-run, it only creates what is missing and
# overwrites the group policy with the JSON in policies/.
# It does NOT create an access key (see 02-create-access-key.sh).
set -euo pipefail

GROUP_NAME="${GROUP_NAME:-terraform-deployers}"
USER_NAME="${USER_NAME:-terraform-deployer}"
POLICY_NAME="${POLICY_NAME:-nequi-terraform-deployer}"
POLICY_FILE="$(dirname "$0")/../policies/deployer-policy.json"

echo "Account: $(aws sts get-caller-identity --query Account --output text)"

# Create the group if it does not exist.
aws iam get-group --group-name "$GROUP_NAME" >/dev/null 2>&1 \
  || aws iam create-group --group-name "$GROUP_NAME" >/dev/null

# Inline policy: put-group-policy replaces it, so re-running updates it.
aws iam put-group-policy \
  --group-name "$GROUP_NAME" \
  --policy-name "$POLICY_NAME" \
  --policy-document "file://$POLICY_FILE"

# Create the user if it does not exist, then make sure it is in the group.
aws iam get-user --user-name "$USER_NAME" >/dev/null 2>&1 \
  || aws iam create-user --user-name "$USER_NAME" >/dev/null
aws iam add-user-to-group --group-name "$GROUP_NAME" --user-name "$USER_NAME"

echo "Done. User '$USER_NAME' in group '$GROUP_NAME'. Next: 02-create-access-key.sh"
