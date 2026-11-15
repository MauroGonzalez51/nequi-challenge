output "state_bucket" {
  description = "Bucket name to put in each environments/<env>/backend.tf."
  value       = aws_s3_bucket.tfstate.bucket
}
