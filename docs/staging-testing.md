# Staging Testing (ECS via ALB)

## 1. Get the ALB URL

The ALB DNS name changes every time `staging-ephemeral` is destroyed and recreated, so look it up after each `terraform apply`:

```powershell
cd infra/terraform/envs/staging-ephemeral
terraform output alb_url
```

Wait ~2 minutes after apply for the app to boot, then check health:

```powershell
curl.exe -i <alb_url>/api/v1/actuator/health
```

## 2. Postman setup (once)

Create two environments in Postman (top right → Environments):

| Environment | Variable | Value |
|-------------|----------|-------|
| `local` | `baseUrl` | `http://localhost:8080/api/v1` |
| `staging` | `baseUrl` | `<alb_url>/api/v1` |

Add a second variable `token` (empty) to both.

Use `{{baseUrl}}` in every request URL and `{{token}}` as the Bearer token. Switch environments with the dropdown. After a new `terraform apply`, only update `baseUrl` in `staging`.

Optional — save the token automatically: in the login request → Scripts → Post-response:
```javascript
pm.environment.set("token", pm.response.json().accessToken);
```

## 3. Requests

Staging RDS is separate from your local DB: register a fresh user the first time (or after the JWT secret changes, log in again).

### Register
POST
```
{{baseUrl}}/auth/register
```
```json
{
  "email": "acme@test.com",
  "password": "password123",
  "displayName": "Acme Admin",
  "companyName": "Acme"
}
```

### Login
POST
```
{{baseUrl}}/auth/login
```
```json
{
  "email": "acme@test.com",
  "password": "password123"
}
```

### Create work order
POST
```
{{baseUrl}}/work-orders
```
```json
{
  "title": "Bracket redesign",
  "description": "Update mounting holes"
}
```

### Get work order
GET
```
{{baseUrl}}/work-orders/{workOrderId}
```

### Transition
POST
```
{{baseUrl}}/work-orders/{workOrderId}/transitions
```
```json
{
  "targetStatus": "SUBMITTED"
}
```

### Upload URL
POST
```
{{baseUrl}}/files/upload-url
```
```json
{
  "workOrderId": "{workOrderId}",
  "fileName": "drawing.dwg",
  "contentType": "application/octet-stream"
}
```
On staging `uploadUrl` must start with `https://vatika-work-order-staging-files.s3.ca-central-1.amazonaws.com/`. If it says `localhost`, the config is wrong.

### Upload to S3
PUT → paste `uploadUrl`, Body → binary → any file, header `Content-Type: application/octet-stream`, no Authorization.

### Confirm
POST, no body
```
{{baseUrl}}/files/{fileAssetId}/confirm
```

### Download URL
GET
```
{{baseUrl}}/files/{fileAssetId}/download-url
```

## 4. What to verify on staging

| Check | Expected |
|-------|----------|
| Health | `{"status":"UP"}` |
| Login | token returned |
| No token on any work order URL | 401 JSON with `correlationId` |
| Upload URL | real S3 host, not localhost |
| Confirm after upload | 204 |
| Logs | JSON lines in `/ecs/vatika-work-order-staging` |

Watch logs while testing:
```powershell
aws logs tail /ecs/vatika-work-order-staging --follow --region ca-central-1
```

Staff-user tests (403) are local only: staging RDS is private, so you can't insert a staff user there yet.

## 5. When done

```powershell
cd infra/terraform/envs/staging-ephemeral
terraform destroy
```
