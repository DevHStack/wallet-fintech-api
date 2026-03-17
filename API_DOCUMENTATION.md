# Wallet API Documentation

## Overview
The Wallet Finance API is a comprehensive REST API for managing digital wallet accounts with support for deposits, withdrawals, transfers, and comprehensive audit logging.

## Base URL
```
http://localhost:8080/api
```

## Authentication
All endpoints (except health check) require **JWT Bearer Token** authentication.

### Authentication Header
```
Authorization: Bearer <JWT_TOKEN>
```

Example token (for development):
```
eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1c2VyLTEyMyJ9.TJVA95OrM7E2cBab30RMHrHDcEfxjoYZgeFONFh7HgQ
```

## Features
- ✅ Multi-currency account support
- ✅ Idempotent transactions (using Idempotency-Key header)
- ✅ Comprehensive audit logging
- ✅ Optimistic locking for concurrent updates
- ✅ Input validation
- ✅ JWT-based security
- ✅ RESTful error handling

---

## API Endpoints

### 1. Create Account

**Endpoint:** `POST /api/accounts`

**Description:** Creates a new wallet account for the authenticated user.

**Request Headers:**
```
Content-Type: application/json
Authorization: Bearer <JWT_TOKEN>
```

**Request Body:**
```json
{
  "ownerName": "John Doe",
  "currency": "USD"
}
```

**Response (201 Created):**
```json
{
  "id": 1,
  "ownerId": "user-123",
  "ownerName": "John Doe",
  "balance": 0.00,
  "currency": "USD",
  "createdAt": "2026-03-17T19:30:00Z",
  "version": 0
}
```

**Status Codes:**
- `201 Created` - Account successfully created
- `400 Bad Request` - Invalid input (missing name, blank values)
- `401 Unauthorized` - Missing or invalid JWT token

**Example:**
```bash
curl -X POST http://localhost:8080/api/accounts \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1c2VyLTEyMyJ9.TJVA95OrM7E2cBab30RMHrHDcEfxjoYZgeFONFh7HgQ" \
  -d '{
    "ownerName": "John Doe",
    "currency": "USD"
  }'
```

---

### 2. Get Account Details

**Endpoint:** `GET /api/accounts/{id}`

**Description:** Retrieves account details. Users can only access their own accounts.

**Path Parameters:**
- `id` (Long) - Account ID

**Request Headers:**
```
Authorization: Bearer <JWT_TOKEN>
```

**Response (200 OK):**
```json
{
  "id": 1,
  "ownerId": "user-123",
  "ownerName": "John Doe",
  "balance": 500.00,
  "currency": "USD",
  "createdAt": "2026-03-17T19:30:00Z",
  "version": 1
}
```

**Status Codes:**
- `200 OK` - Account retrieved successfully
- `403 Forbidden` - Account does not belong to authenticated user
- `404 Not Found` - Account not found
- `401 Unauthorized` - Missing or invalid JWT token

**Example:**
```bash
curl -X GET http://localhost:8080/api/accounts/1 \
  -H "Authorization: Bearer <JWT_TOKEN>"
```

---

### 3. Deposit Funds

**Endpoint:** `POST /api/accounts/{id}/deposit`

**Description:** Deposits funds into an account. Idempotent - safe to retry with the same Idempotency-Key.

**Path Parameters:**
- `id` (Long) - Account ID

**Request Headers:**
```
Content-Type: application/json
Authorization: Bearer <JWT_TOKEN>
Idempotency-Key: <optional-unique-key>  # For idempotent requests
```

**Request Body:**
```json
{
  "amount": 500.00,
  "currency": "USD"
}
```

**Response (200 OK):**
```json
{
  "id": 1,
  "ownerId": "user-123",
  "ownerName": "John Doe",
  "balance": 500.00,
  "currency": "USD",
  "createdAt": "2026-03-17T19:30:00Z",
  "version": 1
}
```

**Status Codes:**
- `200 OK` - Deposit successful
- `400 Bad Request` - Invalid amount (negative, zero, or mismatched currency)
- `403 Forbidden` - Access denied
- `404 Not Found` - Account not found
- `401 Unauthorized` - Missing or invalid JWT token

**Idempotency:**
Use the `Idempotency-Key` header to ensure the request is idempotent:
```bash
curl -X POST http://localhost:8080/api/accounts/1/deposit \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <JWT_TOKEN>" \
  -H "Idempotency-Key: 550e8400-e29b-41d4-a716-446655440000" \
  -d '{
    "amount": 500.00,
    "currency": "USD"
  }'
```

---

### 4. Withdraw Funds

**Endpoint:** `POST /api/accounts/{id}/withdraw`

**Description:** Withdraws funds from an account. Fails if insufficient balance. Idempotent with Idempotency-Key.

**Path Parameters:**
- `id` (Long) - Account ID

**Request Headers:**
```
Content-Type: application/json
Authorization: Bearer <JWT_TOKEN>
Idempotency-Key: <optional-unique-key>
```

**Request Body:**
```json
{
  "amount": 100.00,
  "currency": "USD"
}
```

**Response (200 OK):**
```json
{
  "id": 1,
  "ownerId": "user-123",
  "ownerName": "John Doe",
  "balance": 400.00,
  "currency": "USD",
  "createdAt": "2026-03-17T19:30:00Z",
  "version": 2
}
```

**Status Codes:**
- `200 OK` - Withdrawal successful
- `400 Bad Request` - Invalid amount or insufficient funds
- `403 Forbidden` - Access denied
- `404 Not Found` - Account not found
- `401 Unauthorized` - Missing or invalid JWT token

**Example:**
```bash
curl -X POST http://localhost:8080/api/accounts/1/withdraw \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <JWT_TOKEN>" \
  -d '{
    "amount": 100.00,
    "currency": "USD"
  }'
```

---

### 5. Transfer Funds

**Endpoint:** `POST /api/accounts/transfer`

**Description:** Transfers funds between two accounts. Both accounts must belong to the authenticated user. Idempotent with Idempotency-Key.

**Request Headers:**
```
Content-Type: application/json
Authorization: Bearer <JWT_TOKEN>
Idempotency-Key: <optional-unique-key>
```

**Request Body:**
```json
{
  "fromAccountId": 1,
  "toAccountId": 2,
  "amount": 250.00,
  "currency": "USD"
}
```

**Response (204 No Content):**
Empty response body on success.

**Status Codes:**
- `204 No Content` - Transfer successful
- `400 Bad Request` - Invalid transfer (same account, negative amount, insufficient funds)
- `403 Forbidden` - Access denied (accounts don't belong to user)
- `404 Not Found` - Account not found
- `401 Unauthorized` - Missing or invalid JWT token

**Example:**
```bash
curl -X POST http://localhost:8080/api/accounts/transfer \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <JWT_TOKEN>" \
  -d '{
    "fromAccountId": 1,
    "toAccountId": 2,
    "amount": 250.00,
    "currency": "USD"
  }'
```

---

## Common Response Format

### Success Response
```json
{
  "id": 1,
  "ownerId": "user-123",
  "ownerName": "John Doe",
  "balance": 500.00,
  "currency": "USD",
  "createdAt": "2026-03-17T19:30:00Z",
  "version": 1
}
```

### Error Response (400 Bad Request)
```json
{
  "type": "about:blank",
  "title": "Validation failed",
  "status": 400,
  "detail": "See the 'errors' property for details",
  "errors": {
    "amount": "Amount must be positive"
  }
}
```

### Error Response (401 Unauthorized)
```json
{
  "type": "about:blank",
  "title": "Unauthorized",
  "status": 401,
  "detail": "Invalid or missing JWT token"
}
```

---

## Idempotency

The `/deposit`, `/withdraw`, and `/transfer` endpoints support idempotency using the `Idempotency-Key` header.

**Benefits:**
- Safe to retry failed requests
- Prevents duplicate transactions
- No side effects from network retries

**How it works:**
1. Client generates a unique ID (UUID recommended)
2. Include it in the `Idempotency-Key` header
3. Server stores the request + response
4. Subsequent requests with the same key return the cached response

**Example:**
```bash
# First request
curl -X POST http://localhost:8080/api/accounts/1/deposit \
  -H "Idempotency-Key: 550e8400-e29b-41d4-a716-446655440000" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <JWT_TOKEN>" \
  -d '{"amount": 500.00, "currency": "USD"}'

# Second request with same key returns same response (no duplicate deposit)
curl -X POST http://localhost:8080/api/accounts/1/deposit \
  -H "Idempotency-Key: 550e8400-e29b-41d4-a716-446655440000" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <JWT_TOKEN>" \
  -d '{"amount": 500.00, "currency": "USD"}'
  # Returns same response, balance only increased once
```

---

## Error Codes

| Code | Meaning |
|------|---------|
| 200 | OK - Request succeeded |
| 201 | Created - New resource created |
| 204 | No Content - Request succeeded, no response body |
| 400 | Bad Request - Invalid input |
| 401 | Unauthorized - Missing/invalid JWT |
| 403 | Forbidden - Access denied |
| 404 | Not Found - Resource not found |
| 500 | Internal Server Error |

---

## Example Workflows

### Workflow 1: Create Account and Deposit

```bash
# 1. Create account
ACCOUNT_ID=$(curl -s -X POST http://localhost:8080/api/accounts \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <JWT_TOKEN>" \
  -d '{"ownerName": "John", "currency": "USD"}' | jq -r '.id')

# 2. Deposit $500
curl -X POST http://localhost:8080/api/accounts/$ACCOUNT_ID/deposit \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <JWT_TOKEN>" \
  -d '{"amount": 500, "currency": "USD"}'

# 3. View account
curl -X GET http://localhost:8080/api/accounts/$ACCOUNT_ID \
  -H "Authorization: Bearer <JWT_TOKEN>"
```

### Workflow 2: Multi-Account Transfer

```bash
# Create two accounts
ACCOUNT1=$(curl -s -X POST http://localhost:8080/api/accounts \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <JWT_TOKEN>" \
  -d '{"ownerName": "John", "currency": "USD"}' | jq -r '.id')

ACCOUNT2=$(curl -s -X POST http://localhost:8080/api/accounts \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <JWT_TOKEN>" \
  -d '{"ownerName": "Jane", "currency": "USD"}' | jq -r '.id')

# Deposit to first account
curl -X POST http://localhost:8080/api/accounts/$ACCOUNT1/deposit \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <JWT_TOKEN>" \
  -d '{"amount": 1000, "currency": "USD"}'

# Transfer $250 from account 1 to account 2
curl -X POST http://localhost:8080/api/accounts/transfer \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <JWT_TOKEN>" \
  -d "{\"fromAccountId\": $ACCOUNT1, \"toAccountId\": $ACCOUNT2, \"amount\": 250, \"currency\": \"USD\"}"

# Check balances
echo "Account 1 balance:"
curl -s -X GET http://localhost:8080/api/accounts/$ACCOUNT1 \
  -H "Authorization: Bearer <JWT_TOKEN>" | jq '.balance'

echo "Account 2 balance:"
curl -s -X GET http://localhost:8080/api/accounts/$ACCOUNT2 \
  -H "Authorization: Bearer <JWT_TOKEN>" | jq '.balance'
```

---

## Health Check

**Endpoint:** `GET /actuator/health`

No authentication required.

**Response:**
```json
{
  "status": "UP"
}
```

---

## Development

### Running the Application
```bash
./mvnw spring-boot:run
```

### Running Tests
```bash
./mvnw test
```

### Building JAR
```bash
./mvnw clean package
```

### Starting JAR
```bash
java -jar target/wallet-0.0.1-SNAPSHOT.jar
```

---

## Support

For issues or questions, refer to the repository:
- GitHub: https://github.com/DevHStack/wallet-fintech-api
- Issues: https://github.com/DevHStack/wallet-fintech-api/issues
