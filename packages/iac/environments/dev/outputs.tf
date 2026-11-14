output "api_url" {
  description = "Public URL of the API."
  value       = "http://${module.alb.dns_name}"
}

output "ecr_repository_url" {
  description = "Where to push the image."
  value       = module.ecr.repository_url
}

output "cluster_name" {
  value = module.ecs.cluster_name
}

output "service_name" {
  value = module.ecs.service_name
}
