# Bootstrap: creates the S3 bucket that holds the remote state of every env.
# Runs ONCE with LOCAL state (chicken and egg: the bucket does not exist yet).
# Bucket name must match nequi-tfstate-* or the deployer policy denies it.

terraform {
  required_version = ">= 1.10"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 6.0"
    }
  }
}

provider "aws" {
  region = var.region

  default_tags {
    tags = {
      Project   = "nequi-challenge"
      ManagedBy = "terraform"
      Stack     = "bootstrap"
    }
  }
}

# Account id makes the bucket name globally unique without hardcoding it.
data "aws_caller_identity" "current" {}

locals {
  bucket_name = "nequi-tfstate-${data.aws_caller_identity.current.account_id}"
}

resource "aws_s3_bucket" "tfstate" {
  bucket = local.bucket_name

  # Challenge account: we build and destroy fast. Lets `destroy` empty the
  # versioned bucket. In a real project this would be false.
  force_destroy = true
}

# Keeps old state versions: lets us recover from a bad apply.
resource "aws_s3_bucket_versioning" "tfstate" {
  bucket = aws_s3_bucket.tfstate.id

  versioning_configuration {
    status = "Enabled"
  }
}

# State can contain secrets (e.g. the DB password): encrypt at rest.
resource "aws_s3_bucket_server_side_encryption_configuration" "tfstate" {
  bucket = aws_s3_bucket.tfstate.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

# State must never be public, whatever ACL or policy someone adds later.
resource "aws_s3_bucket_public_access_block" "tfstate" {
  bucket = aws_s3_bucket.tfstate.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

# Rejects any request not sent over TLS.
data "aws_iam_policy_document" "tls_only" {
  statement {
    sid     = "DenyInsecureTransport"
    effect  = "Deny"
    actions = ["s3:*"]
    resources = [
      aws_s3_bucket.tfstate.arn,
      "${aws_s3_bucket.tfstate.arn}/*",
    ]

    principals {
      type        = "*"
      identifiers = ["*"]
    }

    condition {
      test     = "Bool"
      variable = "aws:SecureTransport"
      values   = ["false"]
    }
  }
}

resource "aws_s3_bucket_policy" "tfstate" {
  bucket = aws_s3_bucket.tfstate.id
  policy = data.aws_iam_policy_document.tls_only.json

  # Avoid racing with the public access block (both touch bucket policy).
  depends_on = [aws_s3_bucket_public_access_block.tfstate]
}
