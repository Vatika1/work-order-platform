locals {
  ns            = var.namespace
  rds_log_group = "/aws/rds/instance/${var.rds_instance_id}/postgresql"
}

# ================= DASHBOARD =================
resource "aws_cloudwatch_dashboard" "main" {
  dashboard_name = "${var.name}-overview"

  dashboard_body = jsonencode({
    widgets = [
      # ---------- Row 1: API ----------
      {
        type = "metric", x = 0, y = 0, width = 8, height = 6
        properties = {
          title = "API latency p50 / p95 / p99 (ms, worst endpoint)", region = var.region, view = "timeSeries"
          metrics = [
            [{ expression = "MAX(SEARCH('Namespace=\"${local.ns}\" MetricName=\"http.server.requests.percentile.value\" phi=\"0.5\"', 'Maximum', 60))", id = "p50", label = "p50" }],
            [{ expression = "MAX(SEARCH('Namespace=\"${local.ns}\" MetricName=\"http.server.requests.percentile.value\" phi=\"0.95\"', 'Maximum', 60))", id = "p95", label = "p95" }],
            [{ expression = "MAX(SEARCH('Namespace=\"${local.ns}\" MetricName=\"http.server.requests.percentile.value\" phi=\"0.99\"', 'Maximum', 60))", id = "p99", label = "p99" }]
          ]
        }
      },
      {
        type = "metric", x = 8, y = 0, width = 8, height = 6
        properties = {
          title = "Requests per minute", region = var.region, view = "timeSeries", stat = "Sum", period = 60
          metrics = [
            ["AWS/ApplicationELB", "RequestCount", "LoadBalancer", var.alb_arn_suffix]
          ]
        }
      },
      {
        type = "metric", x = 16, y = 0, width = 8, height = 6
        properties = {
          title = "4xx / 5xx", region = var.region, view = "timeSeries", stat = "Sum", period = 60
          metrics = [
            ["AWS/ApplicationELB", "HTTPCode_Target_4XX_Count", "LoadBalancer", var.alb_arn_suffix, { label = "4xx" }],
            ["AWS/ApplicationELB", "HTTPCode_Target_5XX_Count", "LoadBalancer", var.alb_arn_suffix, { label = "5xx", color = "#d62728" }]
          ]
        }
      },

      # ---------- Row 2: Database ----------
      {
        type = "metric", x = 0, y = 6, width = 8, height = 6
        properties = {
          title = "Hikari connections (active / idle / pending)", region = var.region, view = "timeSeries"
          metrics = [
            [{ expression = "MAX(SEARCH('Namespace=\"${local.ns}\" MetricName=\"hikaricp.connections.active.value\"', 'Maximum', 60))", id = "act", label = "active" }],
            [{ expression = "MAX(SEARCH('Namespace=\"${local.ns}\" MetricName=\"hikaricp.connections.idle.value\"', 'Maximum', 60))", id = "idle", label = "idle" }],
            [{ expression = "MAX(SEARCH('Namespace=\"${local.ns}\" MetricName=\"hikaricp.connections.pending.value\"', 'Maximum', 60))", id = "pend", label = "pending" }]
          ]
        }
      },
      {
        type = "metric", x = 8, y = 6, width = 8, height = 6
        properties = {
          title = "RDS CPU % / connections", region = var.region, view = "timeSeries", period = 60
          metrics = [
            ["AWS/RDS", "CPUUtilization", "DBInstanceIdentifier", var.rds_instance_id, { stat = "Average" }],
            ["AWS/RDS", "DatabaseConnections", "DBInstanceIdentifier", var.rds_instance_id, { stat = "Maximum", yAxis = "right" }]
          ]
        }
      },
      {
        type = "metric", x = 16, y = 6, width = 8, height = 6
        properties = {
          title = "RDS read / write IOPS", region = var.region, view = "timeSeries", stat = "Average", period = 60
          metrics = [
            ["AWS/RDS", "ReadIOPS", "DBInstanceIdentifier", var.rds_instance_id],
            ["AWS/RDS", "WriteIOPS", "DBInstanceIdentifier", var.rds_instance_id]
          ]
        }
      },

      # ---------- Row 3: Infra ----------
      {
        type = "metric", x = 0, y = 12, width = 8, height = 6
        properties = {
          title = "ECS CPU / memory %", region = var.region, view = "timeSeries", stat = "Average", period = 60
          metrics = [
            ["AWS/ECS", "CPUUtilization", "ClusterName", var.ecs_cluster_name, "ServiceName", var.ecs_service_name],
            ["AWS/ECS", "MemoryUtilization", "ClusterName", var.ecs_cluster_name, "ServiceName", var.ecs_service_name]
          ]
        }
      },
      {
        type = "metric", x = 8, y = 12, width = 8, height = 6
        properties = {
          title = "ALB healthy / unhealthy targets", region = var.region, view = "timeSeries", stat = "Minimum", period = 60
          metrics = [
            ["AWS/ApplicationELB", "HealthyHostCount", "TargetGroup", var.tg_arn_suffix, "LoadBalancer", var.alb_arn_suffix],
            ["AWS/ApplicationELB", "UnHealthyHostCount", "TargetGroup", var.tg_arn_suffix, "LoadBalancer", var.alb_arn_suffix]
          ]
        }
      },
      {
        type = "metric", x = 16, y = 12, width = 8, height = 6
        properties = {
          title = "ALB response time p99 (s)", region = var.region, view = "timeSeries", stat = "p99", period = 60
          metrics = [
            ["AWS/ApplicationELB", "TargetResponseTime", "LoadBalancer", var.alb_arn_suffix]
          ]
        }
      },

      # ---------- Row 4: Slowest requests table ----------
      {
        type = "log", x = 0, y = 18, width = 24, height = 6
        properties = {
          title  = "Slowest requests"
          region = var.region
          view   = "table"
          query  = "SOURCE '${var.log_group}' | fields @timestamp, method, route, status, durationMs, clientId, correlationId | filter ispresent(durationMs) | sort durationMs desc | limit 20"
        }
      }
    ]
  })
}

# ================= SAVED LOGS INSIGHTS QUERIES =================
resource "aws_cloudwatch_query_definition" "slowest_requests" {
  name            = "${var.name}/1-slowest-requests"
  log_group_names = [var.log_group]
  query_string    = <<-EOT
    fields @timestamp, method, route, status, durationMs, clientId, correlationId
    | filter ispresent(durationMs)
    | sort durationMs desc
    | limit 50
  EOT
}

resource "aws_cloudwatch_query_definition" "slowest_routes" {
  name            = "${var.name}/2-slowest-routes-p95"
  log_group_names = [var.log_group]
  query_string    = <<-EOT
    filter ispresent(durationMs)
    | stats count(*) as requests, avg(durationMs) as avgMs, pct(durationMs, 95) as p95Ms, max(durationMs) as maxMs by method, route
    | sort p95Ms desc
  EOT
}

resource "aws_cloudwatch_query_definition" "errors_by_route" {
  name            = "${var.name}/3-errors-by-route"
  log_group_names = [var.log_group]
  query_string    = <<-EOT
    filter status >= 400
    | stats count(*) as errors by method, route, status
    | sort errors desc
  EOT
}

resource "aws_cloudwatch_query_definition" "latency_by_tenant" {
  name            = "${var.name}/4-latency-by-tenant"
  log_group_names = [var.log_group]
  query_string    = <<-EOT
    filter ispresent(durationMs)
    | stats count(*) as requests, pct(durationMs, 95) as p95Ms by clientId
    | sort p95Ms desc
  EOT
}

resource "aws_cloudwatch_query_definition" "slow_sql" {
  name            = "${var.name}/5-slow-sql"
  log_group_names = [local.rds_log_group]
  query_string    = <<-EOT
    filter @message like /duration:/
    | parse @message /duration: (?<ms>[\d.]+) ms\s+(?<stmt>.*)/
    | sort ms desc
    | limit 50
  EOT
}