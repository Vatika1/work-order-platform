# Local Testing

Everything runs on your machine: Postgres, Redis and LocalStack (fake S3) in Docker, the app in IntelliJ or Maven.

## 1. Start dependencies

Run from the **repo root** (uses the root `docker-compose.yml`, which matches `application-local.yml`):

```powershell
docker compose up -d
docker compose ps
```

| Service    | Container             | Local port | Notes                                          |
|------------|-----------------------|------------|------------------------------------------------|
| Postgres   | workorder-pg          | 5433       | db `workorder`, user `workorder`, pw `workorder` |
| Redis      | workorder-redis       | 6380       |                                                |
| LocalStack | workorder-localstack  | 4566       | bucket `vatika-work-order-staging-files` created on startup |

Stop: `docker compose down` (data kept) · Wipe DB too: `docker compose down -v`

> `backend/docker-compose.yml` is an older duplicate on different ports. Use the root one.
> Ports are 5433 / 6380 (not the defaults) because the surveillance project already uses 5432 / 6379.
> `localstack-init/init-s3.sh` must have LF line endings (IntelliJ status bar, bottom right), or the bucket isn't created.

## 2. Run the app (local profile)

The app will not start without a profile: there's no JWT default anymore.

- **IntelliJ:** Run → Edit Configurations → your app → Active profiles: `local`
- **Maven** (from `backend/`):

```powershell
./mvnw spring-boot:run "-Dspring-boot.run.profiles=local"
```

Base URL: `http://localhost:8080/api/v1`
Health: `http://localhost:8080/api/v1/actuator/health`

## 3. Run tests

From `backend/`:

```powershell
./mvnw test
```

Tests use the `test` profile (`application-test.yml`) and Testcontainers, so Docker must be running.
Note: `*IT` classes don't run in Maven yet (no failsafe plugin), only in IntelliJ.

## 4. Postman flow

Set the token from register/login on every request after that: Authorization → Bearer Token.

### Register (creates a new client company + CLIENT_ADMIN)
POST
```
http://localhost:8080/api/v1/auth/register
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
http://localhost:8080/api/v1/auth/login
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
http://localhost:8080/api/v1/work-orders
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
http://localhost:8080/api/v1/work-orders/{workOrderId}
```

### Transition work order
POST
```
http://localhost:8080/api/v1/work-orders/{workOrderId}/transitions
```
```json
{
  "targetStatus": "SUBMITTED"
}
```
Allowed: DRAFT → SUBMITTED / CANCELLED · SUBMITTED → IN_PROGRESS / CANCELLED · IN_PROGRESS → IN_REVIEW · IN_REVIEW → IN_PROGRESS / COMPLETED. Anything else → 409.

### Get upload URL
POST
```
http://localhost:8080/api/v1/files/upload-url
```
```json
{
  "workOrderId": "{workOrderId}",
  "fileName": "drawing.dwg",
  "contentType": "application/octet-stream"
}
```
Locally the `uploadUrl` points at `http://localhost:4566/...` (LocalStack).

### Upload the file to S3 (presigned URL)
PUT → paste the `uploadUrl` from the response
- Body → binary → pick any file
- Header `Content-Type` must exactly match the `contentType` you sent (`application/octet-stream`)
- No Authorization header (the URL itself is the permission)

### Confirm upload
POST, no body
```
http://localhost:8080/api/v1/files/{fileAssetId}/confirm
```
204 = file found in S3 · 409 = not uploaded yet

### Get download URL
GET
```
http://localhost:8080/api/v1/files/{fileAssetId}/download-url
```

## 5. Negative tests

| Test | How | Expected |
|------|-----|----------|
| No token | Any work order URL, no Authorization | 401 JSON, `"Authentication required"` |
| Garbage token | `Bearer thisIsNotAValidToken` | 401 JSON |
| Staff user (no tenant) | Login as staff, then any work order / file URL | 403, `"This operation requires a client account"` |
| Other tenant's order | Register a 2nd company, GET the 1st company's work order id | 404 |
| Illegal transition | DRAFT → COMPLETED | 409 |
| Missing field | Create work order without `title` | 400, `details` lists the field |

### Create a staff user (local DB only)
Staff can't register. Insert one directly (password = `password123`):

```sql
INSERT INTO users (id, email, password_hash, display_name, role, client_id, enabled)
VALUES (gen_random_uuid(), 'staff@test.com',
        '$2a$10$MifdxGFa/.0IA27MlbU6W.2p1Ki39wHtaaVKkmIL1aCKZY8wFAqCu',
        'Staff User', 'STAFF', NULL, true);
```

## 6. Database access

```powershell
docker exec -it workorder-pg psql -U workorder -d workorder
```

Useful queries:
```sql
\dt                                                -- list tables
SELECT email, role, client_id FROM users;
SELECT id, name FROM clients;
SELECT id, title, status, client_id FROM work_orders ORDER BY created_at DESC LIMIT 10;
SELECT * FROM flyway_schema_history;               -- applied migrations
```

## 7. LocalStack (fake S3)

```powershell
docker exec -it workorder-localstack awslocal s3 ls s3://vatika-work-order-staging-files --recursive
```
