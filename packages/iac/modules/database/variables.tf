variable "name" {
  description = "Resource name prefix, e.g. nequi-dev. Must start with nequi- (deployer policy)."
  type        = string
}

variable "private_subnet_ids" {
  description = "Subnets for the DB subnet group (network.private_subnet_ids)."
  type        = list(string)
}

variable "rds_sg_id" {
  description = "Security group attached to the instance (network.rds_sg_id)."
  type        = string
}

variable "db_name" {
  description = "Initial database. RDS Postgres does not accept hyphens, so underscore."
  type        = string
  default     = "nequi_challenge"
}

variable "db_username" {
  description = "Master username. 'admin' is reserved in Postgres, do not use it."
  type        = string
  default     = "nequi_admin"
}

variable "engine_version" {
  description = "Postgres major version. AWS picks the latest minor."
  type        = string
  default     = "16"
}

variable "instance_class" {
  description = "Instance size. db.t4g.micro is free-tier eligible."
  type        = string
  default     = "db.t4g.micro"
}

variable "allocated_storage" {
  description = "Storage in GB."
  type        = number
  default     = 20
}

variable "multi_az" {
  description = "Standby replica in another AZ. Doubles cost, so on only in prod."
  type        = bool
  default     = false
}
