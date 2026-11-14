output "secret_arn" {
  description = "For ECS task definition secrets (valueFrom) and the execution role policy."
  value       = aws_secretsmanager_secret.db.arn
}

output "endpoint" {
  description = "RDS host. Private, only reachable from inside the VPC."
  value       = aws_db_instance.this.address
}
