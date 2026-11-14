output "vpc_id" {
  value = aws_vpc.this.id
}

output "public_subnet_ids" {
  description = "For the ALB."
  value       = [for s in aws_subnet.public : s.id]
}

output "private_subnet_ids" {
  description = "For ECS tasks and the RDS subnet group."
  value       = [for s in aws_subnet.private : s.id]
}

output "alb_sg_id" {
  value = aws_security_group.alb.id
}

output "ecs_sg_id" {
  value = aws_security_group.ecs.id
}

output "rds_sg_id" {
  value = aws_security_group.rds.id
}
