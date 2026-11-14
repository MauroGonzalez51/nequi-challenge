output "target_group_arn" {
  description = "For the ECS service load_balancer block."
  value       = aws_lb_target_group.this.arn
}

output "dns_name" {
  description = "Public URL of the API (http://<dns_name>)."
  value       = aws_lb.this.dns_name
}

output "listener_arn" {
  description = "ECS service depends on it so tasks register after the listener exists."
  value       = aws_lb_listener.http.arn
}
