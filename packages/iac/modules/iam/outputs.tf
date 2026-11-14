output "execution_role_arn" {
  description = "ECS task definition execution_role_arn."
  value       = aws_iam_role.execution.arn
}

output "task_role_arn" {
  description = "ECS task definition task_role_arn."
  value       = aws_iam_role.task.arn
}
