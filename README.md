# Wallet Service

This project is a Spring Boot wallet API for creating accounts and performing balance operations with persistence, validation, security, idempotency, and audit logging.

## What Is Implemented

- Account creation with owner name and currency
- Account lookup by id
- Deposit to an account
- Withdraw from an account
- Transfer funds between accounts
- Per-user account access checks
- Request validation with structured error responses
- Idempotency support for deposit, withdraw, and transfer requests
- Audit logging for important account actions
- Flyway-based database migrations
- H2 database for local development and testing
- JWT-based authentication for protected endpoints
- H2 console enabled for local database inspection

## Tech Stack

- Java 21
- Spring Boot 3
- Spring Web
- Spring Data JPA
- Spring Security OAuth2 Resource Server
- H2 Database
- Flyway
- Lombok
- JUnit + MockMvc

## API Endpoints

All account endpoints are under `/api/accounts` and require authentication.

### Create Account

`POST /api/accounts`

Example request:

```json
{
  "ownerName": "Alice",
  "currency": "EUR"
}
```

### Get Account

`GET /api/accounts/{id}`

The authenticated user can only access their own account.

### Deposit

`POST /api/accounts/{id}/deposit`

Optional header:

`Idempotency-Key: <unique-key>`

Example request:

```json
{
  "amount": 100.00,
  "currency": "EUR"
}
```

### Withdraw

`POST /api/accounts/{id}/withdraw`

Optional header:

`Idempotency-Key: <unique-key>`

Example request:

```json
{
  "amount": 40.00,
  "currency": "EUR"
}
```

### Transfer

`POST /api/accounts/transfer`

Optional header:

`Idempotency-Key: <unique-key>`

Example request:

```json
{
  "fromAccountId": 1,
  "toAccountId": 2,
  "amount": 75.00
}
```

## Business Rules

- New accounts start with a zero balance
- Currency defaults to `EUR` if not supplied during account creation
- Deposit and withdrawal amounts must be positive
- Withdrawals fail when funds are insufficient
- Transfers require different source and destination accounts
- Transfers only work between accounts with the same currency
- Users can act only on accounts they own

## Database

Schema changes are managed through Flyway migrations in:

`src/main/resources/db/migration`

Current migrations cover:

- Account table
- Account transaction table
- Idempotency table
- User scoping and expiry for idempotency records
- Audit log table

### Local DB Inspection

Run the app and open:

`http://localhost:8080/h2-console`

Use:

- JDBC URL: `jdbc:h2:mem:wallet`
- Username: `sa`
- Password: empty

Useful tables:

- `account`
- `account_transaction`
- `idempotency_record`
- `audit_log`
- `flyway_schema_history`

## Security

- All API endpoints require JWT authentication
- `/actuator/health` and `/actuator/prometheus` are public
- `/h2-console` is enabled for local inspection
- JWT secret is configured through `application.properties`

In tests, JWT subjects such as `user-123` are used to represent authenticated users.

## Testing

The project currently includes tests for:

- Application context startup
- Account creation and retrieval via controller
- Account service deposit, withdraw, and transfer logic
- Insufficient funds handling
- Idempotent deposit behavior

Run tests with:

```bash
./mvnw test
```

Run the application with:

```bash
./mvnw spring-boot:run
```
