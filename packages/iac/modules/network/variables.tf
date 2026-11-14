variable "name" {
  description = "Resource name prefix, e.g. nequi-dev."
  type        = string
}

variable "vpc_cidr" {
  description = "CIDR of the VPC. Subnets are carved from it with cidrsubnet."
  type        = string
  default     = "10.0.0.0/16"
}

variable "az_count" {
  description = "Number of AZs to use. The ALB needs at least 2."
  type        = number
  default     = 2

  validation {
    condition     = var.az_count >= 2
    error_message = "The ALB requires subnets in at least 2 AZs."
  }
}

variable "app_port" {
  description = "Port the application listens on (server.port in application.yaml)."
  type        = number
  default     = 8080
}
