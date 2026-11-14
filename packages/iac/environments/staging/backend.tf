terraform {
  required_version = ">= 1.10"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 6.0"
    }
  }

  # Backend blocks cannot use variables, so `bucket` is NOT set here: it is passed at init
  # with -backend-config (see README), computed from the active account id.
  # One key per environment, so each one has its own state file.
  backend "s3" {
    key          = "staging/terraform.tfstate"
    region       = "us-east-1"
    encrypt      = true
    use_lockfile = true
  }
}
