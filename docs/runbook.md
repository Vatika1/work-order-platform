# Staging Runbook

AWS account region: `ca-central-1`. Everything is named `vatika-work-order-staging-*`.

## Stacks

| Stack | Folder | Contains | Destroy nightly? |
|-------|--------|----------|------------------|
| Persistent | `infra/terraform/envs/staging-persistent` | VPC, RDS, ECR, S3 bucket, GitHub OIDC role | No |
| Ephemeral | `infra/terraform/envs/staging-ephemeral` | ECS cluster/service/task, ALB, log group | Yes |

State: S3 bucket `vatika-work-order-tfstate` (`staging/persistent/...`, `staging/ephemeral/...`).

Both stacks need the RDS password. Set it in every new PowerShell window (or keep it in a gitignored `terraform.tfvars`):
```powershell
$env:TF_VAR_db_password = "<from your password manager>"
```

## Start / stop staging

```powershell
cd infra/terraform/envs/staging-ephemeral
terraform init        # first time or after module changes
terraform validate
terraform plan
terraform apply
terraform output      # alb_url, cluster and service names
```

Stop for the night (keeps RDS, data, ECR images, bucket):
```powershell
terraform destroy
```

> App boot takes ~70–100s; the ALB waits 150s before health checks count.

### Rebuilding everything from scratch
Persistent first (ephemeral reads its outputs), then an image, then ephemeral:
```powershell
cd infra/terraform/envs/staging-persistent
terraform init
terraform apply                       # ~10 min, RDS is the slow part
# push an image (next section) — ECR is empty after a persistent destroy,
# and ECS crash-loops if there's no image to pull
cd ../staging-ephemeral
terraform init
terraform apply
```

## Push an image manually (without CI)

From `backend/`. Log in in two steps: piping `get-login-password` straight into `docker login` mangles the token in PowerShell (400 Bad Request).
```powershell
$token = aws ecr get-login-password --region ca-central-1
docker login --username AWS --password $token 674326380423.dkr.ecr.ca-central-1.amazonaws.com
docker build -t 674326380423.dkr.ecr.ca-central-1.amazonaws.com/vatika-work-order-staging:latest .
docker push 674326380423.dkr.ecr.ca-central-1.amazonaws.com/vatika-work-order-staging:latest
aws ecs update-service --cluster vatika-work-order-staging-cluster --service vatika-work-order-staging-service --force-new-deployment --region ca-central-1
```

## Get the ALB URL

The URL changes every time ephemeral is destroyed and recreated.

```powershell
cd infra/terraform/envs/staging-ephemeral
terraform output alb_url
```

Or console: EC2 → Load Balancers → `vatika-work-order-staging-alb` → DNS name.

Base URL for Postman: `<alb_url>/api/v1`
Health: `<alb_url>/api/v1/actuator/health`

Postman requests are the same as `local-testing.md`, with `http://localhost:8080` replaced by the ALB URL.
On staging the upload URL must start with `https://vatika-work-order-staging-files.s3.ca-central-1.amazonaws.com/`.

## Deploy

Push to `main` → GitHub Actions (`.github/workflows/ci.yml`): test → build image → push to ECR → force new ECS deployment.

Before every push, from `backend/` (same as what CI runs):
```powershell
./mvnw verify
```

Order when a change needs both infra and code (e.g. a new env var or secret):
1. `terraform apply` in staging-ephemeral
2. then push the code

## Check the service

```powershell
aws ecs describe-services --cluster vatika-work-order-staging-cluster --services vatika-work-order-staging-service --region ca-central-1 --query "services[0].{running:runningCount,desired:desiredCount,events:events[0:3].message}"
```
Want running = desired. `0` running for more than ~3 min = crash loop → check logs.

Health check through the ALB:
```powershell
curl.exe -i <alb_url>/api/v1/actuator/health
```

Which AWS account the CLI is using (if commands fail with access errors):
```powershell
aws sts get-caller-identity
```

## Logs (CloudWatch)

Log group: `/ecs/vatika-work-order-staging` (14-day retention). Logs are JSON on staging.

Live tail:
```powershell
aws logs tail /ecs/vatika-work-order-staging --follow --region ca-central-1
```

Last 15 minutes:
```powershell
aws logs tail /ecs/vatika-work-order-staging --since 15m --region ca-central-1
```

Errors only:
```powershell
aws logs tail /ecs/vatika-work-order-staging --since 1h --filter-pattern ERROR --region ca-central-1
```

Console: CloudWatch → Log groups → `/ecs/vatika-work-order-staging`.

Logs Insights — trace one request (paste the `X-Correlation-Id` from a Postman response):
```
fields @timestamp, level, message, clientId, userId
| filter correlationId = "PASTE-ID-HERE"
| sort @timestamp asc
```

## Secrets

JWT secret (SSM SecureString, created once, outside Terraform):
```powershell
aws ssm get-parameter --name "/vatika-work-order/staging/jwt-secret" --region ca-central-1 --query "Parameter.Name"
```

Rotate it (invalidates all existing tokens; redeploy afterwards):
```powershell
$bytes = New-Object byte[] 48
[System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
$secret = [Convert]::ToBase64String($bytes)
aws ssm put-parameter --name "/vatika-work-order/staging/jwt-secret" --type SecureString --value $secret --overwrite --region ca-central-1
aws ecs update-service --cluster vatika-work-order-staging-cluster --service vatika-work-order-staging-service --force-new-deployment --region ca-central-1
```

## Troubleshooting

| Symptom | Check |
|---------|-------|
| ALB returns 503 | Task still booting (wait ~2 min) or crashing: check `describe-services` events and logs |
| Task keeps restarting | Logs for `Could not resolve placeholder` (missing env var/secret) or startup exceptions |
| No logs at all | `SPRING_PROFILES_ACTIVE=staging` missing on the task |
| Upload URL points at localhost | `app.aws.endpoint` set somewhere outside the local profile |
| Confirm returns 500 instead of 409 | Task role missing `s3:ListBucket` |
| 401 on every request | Old token from before a secret change — log in again |
