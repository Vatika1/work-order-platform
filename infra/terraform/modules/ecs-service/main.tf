data "aws_caller_identity" "current" {}

locals {
  jwt_secret_arn = "arn:aws:ssm:${var.region}:${data.aws_caller_identity.current.account_id}:parameter${var.jwt_secret_ssm_name}"
}

resource "aws_ecs_cluster" "this" {
  name = "${var.name}-cluster"
}

resource "aws_cloudwatch_log_group" "this" {
  name              = "/ecs/${var.name}"
  retention_in_days = 14
}

# --- IAM ---

data "aws_iam_policy_document" "ecs_assume" {
  statement {
    actions = ["sts:AssumeRole"]
    principals {
      type        = "Service"
      identifiers = ["ecs-tasks.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "execution" {
  name               = "${var.name}-execution-role"
  assume_role_policy = data.aws_iam_policy_document.ecs_assume.json
}

resource "aws_iam_role_policy_attachment" "execution" {
  role       = aws_iam_role.execution.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

resource "aws_iam_role_policy" "execution_ssm" {
  name = "${var.name}-execution-ssm"
  role = aws_iam_role.execution.id
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect   = "Allow"
      Action   = ["ssm:GetParameters"]
      Resource = local.jwt_secret_arn
    }]
  })
}

resource "aws_iam_role" "task" {
  name               = "${var.name}-task-role"
  assume_role_policy = data.aws_iam_policy_document.ecs_assume.json
}

resource "aws_iam_role_policy" "task_s3" {
  name = "${var.name}-task-s3"
  role = aws_iam_role.task.id
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect   = "Allow"
        Action   = ["s3:PutObject", "s3:GetObject", "s3:DeleteObject"]
        Resource = "${var.bucket_arn}/*"
      },
      {
        Effect   = "Allow"
        Action   = ["s3:ListBucket"]
        Resource = var.bucket_arn
      }
    ]
  })
}

# --- Task + service ---

resource "aws_ecs_task_definition" "this" {
  family                   = var.name
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = 512
  memory                   = 1024
  execution_role_arn       = aws_iam_role.execution.arn
  task_role_arn            = aws_iam_role.task.arn

  container_definitions = jsonencode([{
    name      = "app"
    image     = var.image
    essential = true
    portMappings = [{ containerPort = 8080, protocol = "tcp" }]
    environment = [
      { name = "SPRING_DATASOURCE_URL",      value = "jdbc:postgresql://${var.db_endpoint}:5432/${var.db_name}" },
      { name = "SPRING_DATASOURCE_USERNAME", value = var.db_username },
      { name = "SPRING_DATASOURCE_PASSWORD", value = var.db_password },
      { name = "APP_S3_BUCKET",              value = var.bucket_name },
      { name = "AWS_REGION",                 value = var.region },
      { name = "SPRING_PROFILES_ACTIVE",     value = var.spring_profile }
    ]
    secrets = [
      { name = "JWT_SECRET", valueFrom = local.jwt_secret_arn }
    ]
    logConfiguration = {
      logDriver = "awslogs"
      options = {
        "awslogs-group"         = aws_cloudwatch_log_group.this.name
        "awslogs-region"        = var.region
        "awslogs-stream-prefix" = "app"
      }
    }
  }])
}

resource "aws_ecs_service" "this" {
  name            = "${var.name}-service"
  cluster         = aws_ecs_cluster.this.id
  task_definition = aws_ecs_task_definition.this.arn
  desired_count   = 1
  launch_type     = "FARGATE"
  health_check_grace_period_seconds = 150

  network_configuration {
    subnets          = var.subnet_ids
    security_groups  = [var.security_group_id]
    assign_public_ip = true
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.this.arn
    container_name   = "app"
    container_port   = 8080
  }
}

# --- ALB ---

resource "aws_security_group" "alb" {
  name   = "${var.name}-alb-sg"
  vpc_id = var.vpc_id

  ingress {
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_lb" "this" {
  name               = "${var.name}-alb"
  internal           = false
  load_balancer_type = "application"
  security_groups    = [aws_security_group.alb.id]
  subnets            = var.subnet_ids
}

resource "aws_lb_target_group" "this" {
  name        = "${var.name}-tg"
  port        = 8080
  protocol    = "HTTP"
  vpc_id      = var.vpc_id
  target_type = "ip"

  health_check {
    path                = "/api/v1/actuator/health"
    port                = "8080"
    healthy_threshold   = 2
    unhealthy_threshold = 3
    interval            = 30
    timeout             = 5
  }
}

resource "aws_lb_listener" "http" {
  load_balancer_arn = aws_lb.this.arn
  port              = 80
  protocol          = "HTTP"

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.this.arn
  }
}

