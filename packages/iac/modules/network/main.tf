terraform {
  required_version = ">= 1.10"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 6.0"
    }
  }
}

# Only AZs that are currently usable in the region.
data "aws_availability_zones" "available" {
  state = "available"
}

locals {
  azs = slice(data.aws_availability_zones.available.names, 0, var.az_count)

  # { "us-east-1a" = 0, "us-east-1b" = 1 }: for_each key is the AZ, value feeds cidrsubnet.
  az_index = { for i, az in local.azs : az => i }
}

# ---------------------------------------------------------------------------
# VPC and internet access
# ---------------------------------------------------------------------------

resource "aws_vpc" "this" {
  cidr_block = var.vpc_cidr

  # RDS endpoints are DNS names: without these the tasks cannot resolve them.
  enable_dns_support   = true
  enable_dns_hostnames = true

  tags = { Name = "${var.name}-vpc" }
}

resource "aws_internet_gateway" "this" {
  vpc_id = aws_vpc.this.id

  tags = { Name = "${var.name}-igw" }
}

# ---------------------------------------------------------------------------
# Subnets: public (ALB, NAT) and private (ECS tasks, RDS), one of each per AZ
# ---------------------------------------------------------------------------

resource "aws_subnet" "public" {
  for_each = local.az_index

  vpc_id            = aws_vpc.this.id
  availability_zone = each.key
  cidr_block        = cidrsubnet(var.vpc_cidr, 8, each.value)

  tags = { Name = "${var.name}-public-${each.key}" }
}

resource "aws_subnet" "private" {
  for_each = local.az_index

  vpc_id            = aws_vpc.this.id
  availability_zone = each.key
  # +10 offset so private ranges never overlap the public ones.
  cidr_block = cidrsubnet(var.vpc_cidr, 8, each.value + 10)

  tags = { Name = "${var.name}-private-${each.key}" }
}

# ---------------------------------------------------------------------------
# Outbound path for private subnets: ONE NAT gateway (cost decision, see README)
# ---------------------------------------------------------------------------

resource "aws_eip" "nat" {
  domain = "vpc"

  tags = { Name = "${var.name}-nat-eip" }
}

resource "aws_nat_gateway" "this" {
  allocation_id = aws_eip.nat.id
  # The NAT must sit in a PUBLIC subnet to reach the internet gateway.
  subnet_id = aws_subnet.public[local.azs[0]].id

  tags = { Name = "${var.name}-nat" }

  depends_on = [aws_internet_gateway.this]
}

# ---------------------------------------------------------------------------
# Route tables
# ---------------------------------------------------------------------------

resource "aws_route_table" "public" {
  vpc_id = aws_vpc.this.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.this.id
  }

  tags = { Name = "${var.name}-public-rt" }
}

resource "aws_route_table" "private" {
  vpc_id = aws_vpc.this.id

  route {
    cidr_block     = "0.0.0.0/0"
    nat_gateway_id = aws_nat_gateway.this.id
  }

  tags = { Name = "${var.name}-private-rt" }
}

resource "aws_route_table_association" "public" {
  for_each = aws_subnet.public

  subnet_id      = each.value.id
  route_table_id = aws_route_table.public.id
}

resource "aws_route_table_association" "private" {
  for_each = aws_subnet.private

  subnet_id      = each.value.id
  route_table_id = aws_route_table.private.id
}

# ---------------------------------------------------------------------------
# Security groups: ALB -> ECS -> RDS, chained by SG reference, not by CIDR.
# All three live here because ecs references alb and rds references ecs:
# splitting them across modules would create circular module dependencies.
# Rules are separate resources so the groups can reference each other.
# ---------------------------------------------------------------------------

resource "aws_security_group" "alb" {
  name        = "${var.name}-alb"
  description = "Public ALB"
  vpc_id      = aws_vpc.this.id

  tags = { Name = "${var.name}-alb-sg" }
}

resource "aws_security_group" "ecs" {
  name        = "${var.name}-ecs"
  description = "ECS tasks, only reachable from the ALB"
  vpc_id      = aws_vpc.this.id

  tags = { Name = "${var.name}-ecs-sg" }
}

resource "aws_security_group" "rds" {
  name        = "${var.name}-rds"
  description = "PostgreSQL, only reachable from ECS tasks"
  vpc_id      = aws_vpc.this.id

  tags = { Name = "${var.name}-rds-sg" }
}

# Internet -> ALB on 80 (HTTP only: no domain, so no certificate).
resource "aws_vpc_security_group_ingress_rule" "alb_http" {
  security_group_id = aws_security_group.alb.id
  description       = "HTTP from the internet"
  ip_protocol       = "tcp"
  from_port         = 80
  to_port           = 80
  cidr_ipv4         = "0.0.0.0/0"
}

# ALB -> ECS: the ALB may only talk to the app port.
resource "aws_vpc_security_group_egress_rule" "alb_to_ecs" {
  security_group_id            = aws_security_group.alb.id
  description                  = "App port to ECS tasks"
  ip_protocol                  = "tcp"
  from_port                    = var.app_port
  to_port                      = var.app_port
  referenced_security_group_id = aws_security_group.ecs.id
}

resource "aws_vpc_security_group_ingress_rule" "ecs_from_alb" {
  security_group_id            = aws_security_group.ecs.id
  description                  = "App port from the ALB"
  ip_protocol                  = "tcp"
  from_port                    = var.app_port
  to_port                      = var.app_port
  referenced_security_group_id = aws_security_group.alb.id
}

# Tasks need outbound for ECR, Secrets Manager, CloudWatch (via NAT) and RDS.
resource "aws_vpc_security_group_egress_rule" "ecs_all" {
  security_group_id = aws_security_group.ecs.id
  description       = "All outbound"
  ip_protocol       = "-1"
  cidr_ipv4         = "0.0.0.0/0"
}

# ECS -> RDS on 5432. RDS gets no egress rule: it never starts connections.
resource "aws_vpc_security_group_ingress_rule" "rds_from_ecs" {
  security_group_id            = aws_security_group.rds.id
  description                  = "PostgreSQL from ECS tasks"
  ip_protocol                  = "tcp"
  from_port                    = 5432
  to_port                      = 5432
  referenced_security_group_id = aws_security_group.ecs.id
}
