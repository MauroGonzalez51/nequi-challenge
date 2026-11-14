variable "name" {
  description = "Role name prefix, e.g. nequi-dev. Must start with nequi- (deployer policy)."
  type        = string
}

variable "ecr_repository_arn" {
  description = "Repository the execution role may pull from (ecr.repository_arn)."
  type        = string
}

variable "db_secret_arn" {
  description = "Secret the execution role may read (database.secret_arn)."
  type        = string
}

variable "log_group_name" {
  description = "CloudWatch log group the task writes to. The ecs module creates it with this same name."
  type        = string
}
