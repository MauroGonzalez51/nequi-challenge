variable "environment" {
  description = "Environment name (dev, staging, prod). Part of every resource name."
  type        = string
}

variable "region" {
  description = "AWS region."
  type        = string
  default     = "us-east-1"
}

variable "image_tag" {
  description = "Tag of the API image in ECR."
  type        = string
  default     = "latest"
}

variable "multi_az" {
  description = "RDS standby in another AZ."
  type        = bool
  default     = false
}

variable "desired_count" {
  description = "Initial number of ECS tasks."
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

variable "swagger_enabled" {
  description = "Expose Swagger UI and the OpenAPI spec."
  type        = bool
  default     = false
}
