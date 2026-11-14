variable "name" {
  description = "Repository name, e.g. nequi-dev-api. Must start with nequi- (deployer policy)."
  type        = string
}

variable "keep_last_images" {
  description = "Lifecycle policy: how many images to keep. Older ones expire (storage costs money)."
  type        = number
  default     = 10
}
