variable "name" {
  description = "Resource name prefix, e.g. nequi-dev."
  type        = string
}

variable "region" {
  description = "Region for the awslogs driver."
  type        = string
}

variable "image" {
  description = "Full image URI with tag, e.g. <ecr.repository_url>:latest."
  type        = string
}

variable "app_port" {
  description = "Port the container listens on."
  type        = number
  default     = 8080
}

variable "cpu" {
  description = "Fargate task CPU units (256 = 0.25 vCPU)."
  type        = number
  default     = 512
}

variable "memory" {
  description = "Fargate task memory in MiB. Must be a valid pair with cpu."
  type        = number
  default     = 1024
}

variable "private_subnet_ids" {
  description = "Subnets of the tasks (network.private_subnet_ids)."
  type        = list(string)
}

variable "ecs_sg_id" {
  description = "Security group of the tasks (network.ecs_sg_id)."
  type        = string
}

variable "execution_role_arn" {
  description = "iam.execution_role_arn."
  type        = string
}

variable "task_role_arn" {
  description = "iam.task_role_arn."
  type        = string
}

variable "log_group_name" {
  description = "Same value passed to iam.log_group_name."
  type        = string
}

variable "log_retention_days" {
  description = "Retention of the log group."
  type        = number
  default     = 7
}

variable "db_secret_arn" {
  description = "database.secret_arn. JSON with DB_HOST, DB_PORT, DB_NAME, DB_USERNAME, DB_PASSWORD."
  type        = string
}

variable "target_group_arn" {
  description = "alb.target_group_arn."
  type        = string
}

variable "listener_arn" {
  description = "alb.listener_arn. Service waits for it."
  type        = string
}

variable "desired_count" {
  description = "Initial number of tasks. Autoscaling owns it afterwards."
  type        = number
  default     = 1
}

variable "min_capacity" {
  description = "Autoscaling floor."
  type        = number
  default     = 1
}

variable "max_capacity" {
  description = "Autoscaling ceiling."
  type        = number
  default     = 3
}

variable "cpu_target" {
  description = "Average CPU % the autoscaler tries to hold."
  type        = number
  default     = 60
}

variable "swagger_enabled" {
  description = "Serve Swagger UI and the OpenAPI spec (SWAGGER_ENABLED in the app). Off by default."
  type        = bool
  default     = false
}
