provider "aws" {
  region = var.region

  default_tags {
    tags = {
      Project     = "nequi-challenge"
      Environment = var.environment
      ManagedBy   = "terraform"
    }
  }
}

locals {
  name = "nequi-${var.environment}"

  # Computed once: iam grants logs access to it, ecs creates it.
  log_group_name = "/ecs/${local.name}-api"
}

module "network" {
  source = "../../modules/network"

  name = local.name
}

module "ecr" {
  source = "../../modules/ecr"

  name = "${local.name}-api"
}

module "database" {
  source = "../../modules/database"

  name               = local.name
  private_subnet_ids = module.network.private_subnet_ids
  rds_sg_id          = module.network.rds_sg_id
  multi_az           = var.multi_az
}

module "iam" {
  source = "../../modules/iam"

  name               = local.name
  ecr_repository_arn = module.ecr.repository_arn
  db_secret_arn      = module.database.secret_arn
  log_group_name     = local.log_group_name
}

module "alb" {
  source = "../../modules/alb"

  name              = local.name
  vpc_id            = module.network.vpc_id
  public_subnet_ids = module.network.public_subnet_ids
  alb_sg_id         = module.network.alb_sg_id
}

module "ecs" {
  source = "../../modules/ecs"

  name               = local.name
  region             = var.region
  image              = "${module.ecr.repository_url}:${var.image_tag}"
  private_subnet_ids = module.network.private_subnet_ids
  ecs_sg_id          = module.network.ecs_sg_id
  execution_role_arn = module.iam.execution_role_arn
  task_role_arn      = module.iam.task_role_arn
  log_group_name     = local.log_group_name
  db_secret_arn      = module.database.secret_arn
  target_group_arn   = module.alb.target_group_arn
  listener_arn       = module.alb.listener_arn
  desired_count      = var.desired_count
  min_capacity       = var.min_capacity
  max_capacity       = var.max_capacity
}
