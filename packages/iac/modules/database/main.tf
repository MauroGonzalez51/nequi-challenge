terraform {
  required_version = ">= 1.10"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 6.0"
    }
    random = {
      source  = "hashicorp/random"
      version = "~> 3.6"
    }
  }
}

# ---------------------------------------------------------------------------
# Password: generated, never typed. Lives in state (bucket is private) and in Secrets Manager.
# ---------------------------------------------------------------------------
resource "random_password" "db" {
  length = 24

  # Default specials include / @ " and space, which RDS rejects.
  override_special = "!#$%^&*()-_=+"
}

# ---------------------------------------------------------------------------
# Instance
# ---------------------------------------------------------------------------
resource "aws_db_subnet_group" "this" {
  name       = "${var.name}-db"
  subnet_ids = var.private_subnet_ids
}

resource "aws_db_instance" "this" {
  identifier = "${var.name}-db"

  engine         = "postgres"
  engine_version = var.engine_version
  instance_class = var.instance_class

  allocated_storage = var.allocated_storage
  storage_type      = "gp3"
  storage_encrypted = true

  db_name  = var.db_name
  username = var.db_username
  password = random_password.db.result
  port     = 5432

  db_subnet_group_name   = aws_db_subnet_group.this.name
  vpc_security_group_ids = [var.rds_sg_id]
  publicly_accessible    = false
  multi_az               = var.multi_az

  # Challenge account: destroy must not block on snapshots or protection.
  skip_final_snapshot     = true
  deletion_protection     = false
  backup_retention_period = 1

  # Lets password changes in Terraform apply right away instead of in the maintenance window.
  apply_immediately = true
}

# ---------------------------------------------------------------------------
# Secret: one JSON with the exact env var names that application.yaml reads.
# ECS injects each key with valueFrom = "<secret_arn>:<KEY>::".
# ---------------------------------------------------------------------------
resource "aws_secretsmanager_secret" "db" {
  name = "${var.name}/db"

  # 0 = delete immediately. Default (30 days) blocks re-creating the same name after destroy.
  recovery_window_in_days = 0
}

resource "aws_secretsmanager_secret_version" "db" {
  secret_id = aws_secretsmanager_secret.db.id

  secret_string = jsonencode({
    DB_HOST     = aws_db_instance.this.address
    DB_PORT     = tostring(aws_db_instance.this.port)
    DB_NAME     = var.db_name
    DB_USERNAME = var.db_username
    DB_PASSWORD = random_password.db.result
  })
}
