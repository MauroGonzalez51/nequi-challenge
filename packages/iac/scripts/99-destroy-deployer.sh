#!/usr/bin/env bash
# Cleanup after the challenge. Run as root or an admin, NOT as the deployer.
# Deletes every access key, the user, the group policy and the group.
# Run `terraform destroy` in every env BEFORE this, or you lose the permissions
# needed to clean up.
set -euo pipefail

GROUP_NAME="${GROUP_NAME:-terraform-deployers}"
USER_NAME="${USER_NAME:-terraform-deployer}"
POLICY_NAME="${POLICY_NAME:-nequi-terraform-deployer}"

read -r -p "Delete user '$USER_NAME' and group '$GROUP_NAME'? [y/N] " answer
[[ "$answer" == "y" ]] || { echo "Aborted."; exit 1; }

# Access keys must go first, IAM refuses to delete a user that still has them.
for key_id in $(aws iam list-access-keys --user-name "$USER_NAME" \
  --query 'AccessKeyMetadata[].AccessKeyId' --output text); do
  aws iam delete-access-key --user-name "$USER_NAME" --access-key-id "$key_id"
done

# Console password, if one was created. Ignore the error when there is none.
aws iam delete-login-profile --user-name "$USER_NAME" 2>/dev/null || true

# MFA devices must be deactivated and deleted before the user.
for serial in $(aws iam list-mfa-devices --user-name "$USER_NAME" \
  --query 'MFADevices[].SerialNumber' --output text); do
  aws iam deactivate-mfa-device --user-name "$USER_NAME" --serial-number "$serial"
  aws iam delete-virtual-mfa-device --serial-number "$serial"
done

aws iam remove-user-from-group --group-name "$GROUP_NAME" --user-name "$USER_NAME"
aws iam delete-user --user-name "$USER_NAME"
aws iam delete-group-policy --group-name "$GROUP_NAME" --policy-name "$POLICY_NAME"
aws iam delete-group --group-name "$GROUP_NAME"

echo "Deployer user and group deleted."
