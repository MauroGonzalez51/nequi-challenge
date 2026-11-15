#!/usr/bin/env bash
# Cleanup after the challenge. Run as root or an admin, NOT as the deployer.
# Deletes every access key, the user, the group policy and the group, plus the tunnel user and bastion role.
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

# --- Tunnel user (03-create-tunnel-user.sh), same order: keys first, then the user. ---
TUNNEL_USER="${TUNNEL_USER:-nequi-tunnel}"
ROLE_NAME="nequi-db-bastion"

# `|| true` everywhere: each piece may already be gone.
for key_id in $(aws iam list-access-keys --user-name "$TUNNEL_USER" \
  --query 'AccessKeyMetadata[].AccessKeyId' --output text 2>/dev/null || true); do
  aws iam delete-access-key --user-name "$TUNNEL_USER" --access-key-id "$key_id"
done
TUNNEL_GROUP="${TUNNEL_GROUP:-nequi-tunnelers}"
aws iam remove-user-from-group --group-name "$TUNNEL_GROUP" --user-name "$TUNNEL_USER" 2>/dev/null || true
aws iam delete-user --user-name "$TUNNEL_USER" 2>/dev/null || true
aws iam delete-group-policy --group-name "$TUNNEL_GROUP" --policy-name nequi-tunnel 2>/dev/null || true
aws iam delete-group --group-name "$TUNNEL_GROUP" 2>/dev/null || true

# The role cannot go while it sits in the instance profile or has a policy attached.
aws iam remove-role-from-instance-profile --instance-profile-name "$ROLE_NAME" --role-name "$ROLE_NAME" 2>/dev/null || true
aws iam delete-instance-profile --instance-profile-name "$ROLE_NAME" 2>/dev/null || true
aws iam detach-role-policy --role-name "$ROLE_NAME" --policy-arn arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore 2>/dev/null || true
aws iam delete-role --role-name "$ROLE_NAME" 2>/dev/null || true

echo "Tunnel user, role and instance profile deleted."
