#!/usr/bin/env bash
# Run once PER MACHINE. Creates an access key for the deployer user and
# stores it in the local AWS profile. The secret is never printed.
# An IAM user can hold at most 2 keys: delete an old one first if it fails.
set -euo pipefail

USER_NAME="${USER_NAME:-terraform-deployer}"
PROFILE="${PROFILE:-nequi-tf}"
REGION="${REGION:-us-east-1}"

read -r ACCESS_KEY_ID SECRET_ACCESS_KEY < <(
  aws iam create-access-key \
    --user-name "$USER_NAME" \
    --query 'AccessKey.[AccessKeyId,SecretAccessKey]' \
    --output text
)

aws configure set aws_access_key_id "$ACCESS_KEY_ID" --profile "$PROFILE"
aws configure set aws_secret_access_key "$SECRET_ACCESS_KEY" --profile "$PROFILE"
aws configure set region "$REGION" --profile "$PROFILE"

# Credentials file must be readable only by the owner.
chmod 600 "$HOME/.aws/credentials"

echo "Profile '$PROFILE' saved (key $ACCESS_KEY_ID)."
echo "Use it with: export AWS_PROFILE=$PROFILE && aws sts get-caller-identity"
