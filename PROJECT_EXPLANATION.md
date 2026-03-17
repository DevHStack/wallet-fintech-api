# Wallet Service: Detailed Project Explanation

## 1. Project Overview

This project is a backend wallet service built with Spring Boot. Its main purpose is to let authenticated users create accounts and perform core wallet operations such as deposit, withdrawal, and transfer while keeping a record of transactions and applying basic safety mechanisms like validation, authorization, idempotency, and audit logging.

At a high level, the application is designed around these goals:

- Expose REST APIs for wallet operations
- Persist account and transaction data in a relational database
- Protect APIs with JWT-based authentication
- Prevent duplicate financial operations through idempotency
- Keep an audit trail of important actions
- Manage database schema changes in a controlled way with Flyway

The project currently uses an H2 in-memory database for local development and tests, which makes the service easy to run without external infrastructure.

## 2. Main Functional Features

The service currently implements the following business capabilities:

- Create a wallet account for an authenticated user
- Retrieve account details by id
- Deposit money into an account
- Withdraw money from an account
- Transfer funds from one account to another
- Record ledger-style transaction entries for account operations
- Store audit log entries for important business events
- Support idempotent financial requests using an `Idempotency-Key`

These features are exposed through the account API controller and backed by JPA entities, repositories, and service-layer logic.

## 3. High-Level Architecture

The project follows a standard layered Spring Boot structure:

- Controller layer: handles HTTP requests and responses
- Service layer: contains business logic
- Repository layer: handles persistence using Spring Data JPA
- Entity layer: maps Java objects to database tables
- Configuration layer: defines security and exception handling
- Migration layer: manages schema evolution through Flyway SQL scripts

The request flow is roughly:

1. A client sends an authenticated HTTP request to a REST endpoint.
2. Spring Security validates the JWT and builds the authenticated principal.
3. The controller validates the incoming request body.
4. The controller delegates the business operation to the service layer.
5. The service updates domain entities and saves related transaction or audit records.
6. JPA persists the changes to the H2 database.
7. A JSON response is returned to the client.

For deposit, withdraw, and transfer, the request can first pass through idempotency handling so duplicate retries do not execute the same operation twice.

## 4. Package and Responsibility Breakdown

### `com.hiral.wallet.account`

This package contains the core wallet domain.

- `Account`: JPA entity representing a wallet account
- `AccountController`: REST endpoints for account operations
- `AccountService`: business logic for create, get, deposit, withdraw, and transfer
- `AccountRepository`: persistence access for accounts
- `CreateAccountRequest`, `DepositRequest`, `WithdrawRequest`, `TransferRequest`: validated request DTOs

### `com.hiral.wallet.account.transaction`

This package tracks account-level financial events.

- `AccountTransaction`: JPA entity for transaction history
- `AccountTransactionRepository`: repository for transaction records
- `TransactionType`: enum defining `DEPOSIT`, `WITHDRAWAL`, `TRANSFER_OUT`, and `TRANSFER_IN`

### `com.hiral.wallet.idempotency`

This package prevents repeated execution of the same financial request.

- `IdempotencyService`: core idempotency orchestration
- `IdempotencyRecord`: entity storing request hash, response payload, and expiry
- `IdempotencyRecordRepository`: persistence access for idempotency records
- `IdempotencyCleanupTask`: scheduled cleanup of expired records
- `IdempotencyConflictException`: raised when the same key is reused with a different request body

### `com.hiral.wallet.audit`

This package stores business audit events.

- `AuditLog`: JPA entity for audit records
- `AuditLogRepository`: repository for audit logs
- `AuditService`: service that creates audit entries

### `com.hiral.wallet.config`

This package contains cross-cutting configuration.

- `SecurityConfig`: configures JWT authentication, public endpoints, and H2 console access
- `RestExceptionHandler`: converts validation and runtime failures into HTTP error responses

### `com.hiral.wallet.currency`

This package contains a currency conversion helper.

- `CurrencyConversionService`: defines hardcoded conversion rates for `EUR`, `USD`, and `GBP`

This service appears to be groundwork for future expansion. In the current account operations, deposits and withdrawals require the request currency to match the account currency, and transfers require both accounts to share the same currency. That means conversion logic exists in the codebase but is not currently part of the main transaction flow.

## 5. Domain Model Explanation

### Account

The `Account` entity represents a user-owned wallet account.

Important fields:

- `id`: primary key
- `ownerName`: display name associated with the account
- `ownerId`: authenticated user id that owns the account
- `currency`: account currency, defaulting to `EUR`
- `balance`: current account balance
- `version`: optimistic locking field
- `createdAt`: timestamp when the account was created

Behavior:

- On first persist, default values are assigned if missing
- Balance defaults to zero
- Currency defaults to `EUR`
- `createdAt` is automatically populated

The presence of `@Version` indicates optimistic locking support, which is useful for handling concurrent updates safely at the JPA level.

### AccountTransaction

The `AccountTransaction` entity acts like a transaction ledger attached to an account.

Important fields:

- `account`: owning account
- `type`: transaction type
- `amount`: amount applied by the transaction
- `balanceAfter`: account balance after the operation
- `createdAt`: timestamp of the transaction
- `description`: human-readable description

Examples:

- A deposit creates a `DEPOSIT` transaction
- A withdrawal creates a `WITHDRAWAL` transaction with a negative amount
- A transfer creates two records: `TRANSFER_OUT` on the source account and `TRANSFER_IN` on the destination account

### IdempotencyRecord

This entity stores the state needed to replay repeated requests safely.

Important fields:

- `idempotencyKey`: unique stored key
- `userId`: user who issued the request
- `requestHash`: SHA-256 hash of the request payload
- `endpoint`: logical endpoint name such as `deposit`, `withdraw`, or `transfer`
- `responsePayload`: serialized response for replay
- `expiresAt`: expiry timestamp
- `createdAt`: creation time

This allows the service to distinguish between:

- A harmless retry of the same request
- An incorrect reuse of the same key with a different payload

### AuditLog

The `AuditLog` entity provides an application-level audit trail.

Important fields:

- `userId`: user who performed the action
- `accountId`: related account, if any
- `action`: business event name
- `details`: human-readable summary
- `createdAt`: timestamp

This is separate from transaction history and is intended for operational traceability.

## 6. REST API Design

The main REST controller is `AccountController`.

Base path:

`/api/accounts`

### Create Account

Endpoint:

`POST /api/accounts`

Purpose:

- Create a new wallet account for the authenticated user

Input:

- `ownerName`
- `currency` (optional in practice because the DTO defaults to `EUR`)

Behavior:

- Reads the authenticated user from `Principal`
- Creates a new account under that user
- Returns HTTP `201 Created`

### Get Account

Endpoint:

`GET /api/accounts/{id}`

Purpose:

- Fetch an account by id

Behavior:

- Loads the account from the database
- Checks that `account.ownerId` matches the authenticated user
- Returns `403 Forbidden` if the user does not own the account

### Deposit

Endpoint:

`POST /api/accounts/{id}/deposit`

Purpose:

- Add funds to an account

Special behavior:

- Supports `Idempotency-Key` header
- Uses idempotency service before calling the deposit business logic

### Withdraw

Endpoint:

`POST /api/accounts/{id}/withdraw`

Purpose:

- Remove funds from an account

Special behavior:

- Supports `Idempotency-Key`
- Rejects requests with insufficient balance

### Transfer

Endpoint:

`POST /api/accounts/transfer`

Purpose:

- Move funds between two accounts

Special behavior:

- Supports `Idempotency-Key`
- Requires source and destination accounts to differ
- Requires same currency on both accounts
- Only the owner of the source account can initiate the transfer

## 7. Business Logic Details

The central business logic lives in `AccountService`.

### Account Creation

When `createAccount` is called:

- The user id must be present
- The owner name must not be blank
- Currency defaults to `EUR` if omitted
- A new `Account` entity is created and saved
- A zero-value transaction entry is recorded with description `Account created`
- An audit log is recorded

This means account creation produces both a domain object and a traceable event history.

### Deposit

When `deposit` is called:

- Amount must be positive
- The target account must belong to the authenticated user
- Request currency must match account currency
- The amount is added to the current balance
- A `DEPOSIT` transaction record is stored
- An audit entry is created

### Withdraw

When `withdraw` is called:

- Amount must be positive
- The account must belong to the authenticated user
- Request currency must match account currency
- The current balance must be sufficient
- The amount is subtracted from the balance
- A `WITHDRAWAL` transaction is stored with a negative amount
- An audit entry is created

### Transfer

When `transfer` is called:

- Source and destination account ids must be different
- Amount must be positive
- The source account must belong to the authenticated user
- Source and destination accounts must have the same currency
- Source balance must be sufficient
- Source balance is decreased
- Destination balance is increased
- Two transaction records are created
- One audit log entry is created

The implementation deliberately loads accounts in numeric id order before deciding which one is source and destination. That is a practical concurrency precaution intended to reduce deadlock risk when two accounts are involved in the same transaction.

## 8. Validation and Error Handling

Request validation is handled using Jakarta Bean Validation annotations on the request DTOs.

Examples:

- `@NotBlank` for `ownerName`
- `@NotNull` and `@DecimalMin("0.0001")` for monetary amounts
- `@Pattern("[A-Z]{3}")` for currency codes

The application uses `RestExceptionHandler` to return structured error responses:

- Validation failures are returned as `ProblemDetail`
- Field-level validation errors are collected into an `errors` property
- Constraint violations also produce a `400 Bad Request`
- Any uncaught exception currently falls back to `500 Internal Server Error`

The account domain also defines specific exception types such as:

- `AccountNotFoundException`
- `BadRequestException`
- `InsufficientFundsException`
- `InvalidAmountException`

These support clearer business error signaling in the service layer.

## 9. Security Model

Security is configured in `SecurityConfig`.

The application uses Spring Security with OAuth2 Resource Server support and JWT validation.

Current behavior:

- Most endpoints require authentication
- `/actuator/health` and `/actuator/prometheus` are public
- `/h2-console/**` is temporarily allowed for local inspection
- CSRF is ignored for the H2 console path
- Frame options are relaxed to `sameOrigin` so the H2 console UI can render

The authenticated user identity comes from the JWT subject, which is later used as `ownerId` and as the user scope for account operations and idempotency.

This creates a straightforward authorization model:

- A user can create their own accounts
- A user can retrieve only their own accounts
- A user can deposit or withdraw only on their own accounts
- A user can transfer from only their own source account

## 10. Idempotency Design

Idempotency is one of the most important reliability features in this project.

Financial APIs are often retried by clients because of network failures, timeouts, or uncertain responses. Without idempotency, the same deposit or transfer might be executed twice.

### How it Works

For deposit, withdraw, and transfer:

1. The client may send an `Idempotency-Key` header.
2. The service combines:
   - authenticated user id
   - logical endpoint name
   - client-provided key
3. That combination becomes the stored unique key.
4. The request body is serialized and hashed using SHA-256.
5. If a record already exists:
   - the service compares the stored hash with the new hash
   - if they match, it replays the stored response
   - if they differ, it raises an idempotency conflict
6. If no record exists:
   - the action is executed
   - the response is serialized and stored
   - the idempotency record is saved with an expiry timestamp

### Why the User Scope Matters

Because the stored key includes the user id and endpoint name, these requests do not collide:

- user A using key `abc` for a deposit
- user B using key `abc` for a deposit
- the same user using key `abc` for a transfer instead of a deposit

That makes idempotency safer in multi-user environments.

### Cleanup

Expired idempotency records are removed by `IdempotencyCleanupTask`, which runs on a fixed delay defined by:

- `idempotency.cleanup-interval-ms`

The expiry itself is based on:

- `idempotency.ttl-seconds`

## 11. Audit Logging Design

Audit logging is intentionally separate from account transaction history.

This is a useful distinction:

- transaction history explains money movement
- audit logs explain business actions and actor context

Current audit events include:

- account creation
- deposits
- withdrawals
- transfers

This can later support:

- compliance reporting
- admin investigation
- troubleshooting
- user activity traceability

## 12. Database and Persistence Design

The project uses Spring Data JPA with H2 as the runtime database for local use.

Key tables:

- `account`
- `account_transaction`
- `idempotency_record`
- `audit_log`
- `flyway_schema_history`

### Migration Strategy

Database changes are managed with Flyway SQL migrations under:

`src/main/resources/db/migration`

Current migrations:

- `V1__create_wallet_schema.sql`
- `V2__create_idempotency_table.sql`
- `V3__add_currency_and_owner_to_account.sql`
- `V4__add_userid_and_expiry_to_idempotency.sql`
- `V5__add_audit_log.sql`

This approach is important because the application uses:

- `spring.jpa.hibernate.ddl-auto=validate`

That means Hibernate does not create or update the schema automatically. Instead, it validates that the mapped entities match the database schema created by Flyway. This is a safer and more production-aligned setup than relying on automatic schema generation.

## 13. Configuration Summary

The main runtime settings are in `application.properties`.

Important configuration items include:

- H2 in-memory datasource
- Flyway enabled
- Hibernate schema validation
- SQL logging enabled
- idempotency TTL and cleanup interval
- JWT secret
- H2 console enabled at `/h2-console`

Because the database is in-memory:

- data exists only while the application is running
- restarting the app recreates the schema from Flyway migrations
- this is convenient for development but not for persistent environments

## 14. Testing Strategy

The project includes integration-style tests rather than only isolated unit tests.

### `WalletApplicationTests`

Purpose:

- Verifies that the Spring application context starts successfully

### `AccountControllerTest`

Purpose:

- Verifies account creation and retrieval through the REST API
- Uses `MockMvc`
- Uses a mocked JWT principal through Spring Security test support

### `AccountServiceTest`

Purpose:

- Verifies core service logic for:
  - create account
  - deposit
  - withdraw
  - transfer
  - insufficient funds handling

This test class uses Spring Boot and transactions so each test can run against the real persistence and service wiring.

### `IdempotencyTest`

Purpose:

- Verifies that reusing the same idempotency key for the same deposit request does not apply the deposit twice

This is especially valuable because idempotency is one of the most failure-sensitive parts of financial APIs.

## 15. Design Strengths

There are several solid engineering choices already present in this codebase:

- Clear separation between controller, service, repository, and entity layers
- Controlled schema evolution with Flyway
- JWT-based authentication instead of open endpoints
- Per-user ownership checks on accounts
- Idempotency support for retry-safe write operations
- Transaction history and audit history are modeled separately
- Request validation is explicit and centralized
- Transfer logic accounts for deadlock reduction by stable load ordering
- Optimistic locking support is prepared through the account version field

## 16. Current Limitations and Future Improvement Areas

The project is in a good state for a learning or interview-style backend service, but there are still natural next steps.

### Currency Conversion Not Yet Integrated

There is a `CurrencyConversionService`, but the account operations currently reject mismatched currencies rather than converting them. This suggests a future enhancement path for multi-currency support.

### Error Mapping Could Be More Specific

The global exception handler currently has a generic catch-all for unexpected exceptions. A next improvement would be to map business exceptions like not found, bad request, insufficient funds, and idempotency conflicts to more precise HTTP status codes with explicit problem details.

### No Transaction History API Yet

Transaction records are stored, but there is no endpoint yet to fetch account transaction history. The repository already supports querying transactions by account id, so an API endpoint could be added fairly easily.

### H2 Is Development-Oriented

The current in-memory H2 setup is ideal for fast local work, but a real deployment would usually switch to a persistent database such as PostgreSQL or MySQL.

### JWT Setup Is Simplified

The JWT secret is locally configured in properties, which is fine for development. In production, secret management and token issuance would usually be handled more securely and externally.

## 17. End-to-End Example Flow

Here is a complete example of how the system behaves when a user deposits funds:

1. The client sends `POST /api/accounts/{id}/deposit` with a JWT and an optional `Idempotency-Key`.
2. Spring Security authenticates the JWT and creates the `Principal`.
3. The controller validates the request body.
4. The controller calls `IdempotencyService.execute(...)`.
5. The idempotency service checks whether this exact user-key-endpoint combination already exists.
6. If it is a duplicate retry of the same request, the old response is returned.
7. If it is new, the idempotency service runs the deposit action.
8. `AccountService.deposit(...)` verifies:
   - amount is positive
   - account belongs to the user
   - request currency matches account currency
9. The balance is updated.
10. A `DEPOSIT` transaction record is stored.
11. An audit log entry is stored.
12. The response is returned and persisted in the idempotency table for safe replay.

This same pattern is reused for withdrawal and transfer with operation-specific business rules.

## 18. Conclusion

This project is a well-structured Spring Boot wallet backend that demonstrates several important backend engineering concepts in one codebase:

- REST API design
- layered architecture
- relational persistence
- schema migration
- authentication and authorization
- request validation
- idempotent write operations
- auditability
- integration testing

It is already more than a simple CRUD project because it includes operational and reliability concerns that are especially relevant for financial systems. The strongest parts of the implementation are the ownership checks, transaction recording, Flyway migration setup, and idempotency handling.

The most natural next enhancements would be:

- better exception-to-HTTP mapping
- transaction history endpoints
- persistent production database support
- multi-currency transfer or conversion rules
- stronger production-grade security and observability
