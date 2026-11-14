variable "name" {
  description = "Resource name prefix, e.g. nequi-dev. Max 32 chars with suffix (ALB and target group limit)."
  type        = string
}

variable "vpc_id" {
  description = "VPC of the target group (network.vpc_id)."
  type        = string
}

variable "public_subnet_ids" {
  description = "Subnets of the ALB (network.public_subnet_ids)."
  type        = list(string)
}

variable "alb_sg_id" {
  description = "Security group of the ALB (network.alb_sg_id)."
  type        = string
}

variable "app_port" {
  description = "Port the application listens on."
  type        = number
  default     = 8080
}

variable "health_check_path" {
  description = "Path the ALB probes on each task."
  type        = string
  default     = "/actuator/health"
}
