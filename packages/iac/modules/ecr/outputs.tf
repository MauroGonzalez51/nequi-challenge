output "repository_url" {
  description = "Image URI without tag. Used by the ECS task definition and docker push."
  value       = aws_ecr_repository.this.repository_url
}

output "repository_arn" {
  description = "For the task execution role policy (pull permission scoped to this repo)."
  value       = aws_ecr_repository.this.arn
}
