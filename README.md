# Road Cutting Permission — Take-Home Assignment

## Overview

This project implements the backend API for a multi-tenant Road Cutting Permission service using Java 17+, Spring Boot 3, PostgreSQL and Flyway.

The service supports:

* Fee calculation based on external JSON configuration
* Tenant-specific fee overrides
* Road type validation
* Application creation
* Server-side fee recomputation
* Application lifecycle/workflow
* Role-based workflow actions
* Application transition history
* Tenant isolation
* Application search with pagination
* Financial-year-based application numbers
* PostgreSQL persistence
* Flyway database migrations
* Automated unit tests

Authentication is intentionally out of scope as specified in the assignment. The caller identity and role are taken from `RequestInfo.userInfo` in the request body.

---

## Technology Stack

### Backend

* Java 17+
* Spring Boot 3
* Spring Web
* Spring Data JPA
* PostgreSQL 16
* Flyway
* Maven
* Bean Validation
* JUnit 5
* Mockito
* Docker / Docker Compose

### Configuration

The following configuration is externalized:

* `src/main/resources/fee-rates.json`
* `src/main/resources/workflow-rules.json`

This keeps fee rules and workflow transitions outside the service business logic.

---

## Project Structure

```text
src
├── main
│   ├── java
│   │   └── com.parakore
│   │       ├── application
│   │       │   ├── controller
│   │       │   ├── dto
│   │       │   ├── entity
│   │       │   ├── repository
│   │       │   └── service
│   │       ├── common
│   │       │   └── exception
│   │       ├── config
│   │       ├── fee
│   │       │   ├── dto
│   │       │   └── service
│   │       └── RoadCuttingPermissionApplication.java
│   │
│   └── resources
│       ├── db
│       │   └── migration
│       │       └── V1__create_initial_schema.sql
│       ├── application.yml
│       ├── fee-rates.json
│       └── workflow-rules.json
│
└── test
    └── java
        └── com.parakore
```

---

## Running the Backend

### Prerequisites

* Java 17 or later
* Maven
* Docker Desktop

### Start PostgreSQL

The project includes Docker Compose configuration.

```bash
docker compose up -d
```

Verify that PostgreSQL is running:

```bash
docker ps
```

The PostgreSQL container is exposed on:

```text
localhost:5432
```

Database:

```text
rcp
```

Username:

```text
postgres
```

Password:

```text
postgres
```

### Start the application

From the project root:

```bash
mvn spring-boot:run
```

The API runs on:

```text
http://localhost:8080
```

---

## Database Migration

Flyway is enabled and migrations are located under:

```text
src/main/resources/db/migration
```

The initial schema is created by:

```text
V1__create_initial_schema.sql
```

Hibernate is configured with:

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate
```

Therefore, Hibernate does not automatically modify the database schema.

---

# API Endpoints

## 1. Calculate Fee

```text
POST /rcp/v1/_calculate
```

This endpoint provides a stateless fee preview.

Example:

```json
{
  "RequestInfo": {
    "apiId": "portal",
    "msgId": "abc|en_IN",
    "userInfo": {
      "uuid": "u-1",
      "userName": "9990000001",
      "tenantId": "dehradun",
      "roles": [
        {
          "code": "APPLICANT"
        }
      ]
    }
  },
  "Calculation": {
    "tenantId": "dehradun",
    "roadType": "BT",
    "lengthInMeters": 12.5,
    "widthInMeters": 1.2,
    "durationInDays": 6,
    "applicantType": "PRIVATE",
    "proposedStartDate": "2026-03-02",
    "applicationDate": "2026-03-01"
  }
}
```

The response contains the complete fee breakdown and:

```json
"reviewRef": "K7Q2"
```

Money calculations use `BigDecimal`, not `double`.

---

## 2. Create Application

```text
POST /rcp/v1/_create
```

The application is created in:

```text
APPLIED
```

The fee is always recalculated on the server during creation. The client cannot be trusted to supply the final amount.

The service assigns an application number such as:

```text
DEH-RCP-000006-2026-27
```

The number contains:

* City prefix
* RCP module code
* Zero-padded sequence
* Indian financial year

---

## 3. Workflow Action

```text
POST /rcp/v1/_action
```

Supported actions:

```text
VERIFY
APPROVE
REJECT
SEND_BACK
CANCEL
```

The caller's role is read from:

```text
RequestInfo.userInfo.roles
```

The role is checked server-side.

Example workflow:

```text
APPLIED
   |
 VERIFY
   v
PENDING_APPROVAL
   |
   +---- APPROVE ----> APPROVED
   |
   +---- REJECT -----> REJECTED
   |
   +---- SEND_BACK --> APPLIED

APPLIED
   |
 CANCEL
   v
CANCELLED
```

Every successful transition is stored in `application_transitions`.

---

## 4. Search Applications

```text
POST /rcp/v1/_search
```

Supported filters include:

* application number
* status
* mobile number
* applicant UUID
* offset
* limit

The limit has a server-side maximum of 100.

All searches are tenant scoped.

---

# Fee Calculation Rules

Fee configuration is stored in:

```text
src/main/resources/fee-rates.json
```

The calculation service accesses configuration through the `FeeRateProvider` interface.

This allows the configuration source to be replaced later by another implementation such as a remote configuration service without changing the calculation logic.

### Area

```text
ceil(length × width)
```

The multiplication is performed first and the resulting area is rounded up.

For example:

```text
12.5 × 1.2 = 15
```

### Restoration Charge

```text
area × restorationRatePerSqm
```

### Permission Fee

```text
area × permissionRatePerSqmPerDay × durationInDays
```

For:

```text
GOVERNMENT_AGENCY
```

the permission fee is zero.

### Urgency Surcharge

A surcharge is applied only when:

```text
proposedStartDate - applicationDate < urgencyThresholdDays
```

The comparison is strict.

Therefore, exactly 3 days before the proposed start date does not attract the surcharge.

### Security Deposit

```text
max(
    minSecurityDeposit,
    restorationCharge × securityDepositPercent
)
```

### Total

```text
restoration
+ permission
+ urgency surcharge
+ security deposit
```

The final amount uses `HALF_UP` rounding.

---

# Tenant-Specific Configuration

The default configuration contains:

```text
dehradun
haridwar
```

Haridwar overrides only selected BT fields:

```json
{
  "code": "BT",
  "permissionRatePerSqmPerDay": 20,
  "minSecurityDeposit": 7500
}
```

All other values fall back to the defaults.

Tenant ID is also included in database queries so that data from one tenant cannot be accessed through another tenant.

---

# Worked Example Verification

## Dehradun

Input:

```text
Road type: BT
Length: 12.5 m
Width: 1.2 m
Duration: 6 days
Applicant: PRIVATE
Application date: 2026-03-01
Start date: 2026-03-02
```

Result:

```text
Area                  = 15
Restoration           = 18,000
Permission            = 1,350
Urgency surcharge     = 135
Security deposit      = 5,000
Total                 = 24,485
```

## Haridwar

With the Haridwar BT override:

```text
Area                  = 15
Restoration           = 18,000
Permission            = 1,800
Urgency surcharge     = 180
Security deposit      = 7,500
Total                 = 27,480
```

The implementation uses `BigDecimal` for monetary calculations.

---

# Workflow Configuration

Workflow rules are stored in:

```text
src/main/resources/workflow-rules.json
```

The configuration defines:

* current state
* action
* target state
* permitted role

For example:

```json
{
  "from": "APPLIED",
  "action": "VERIFY",
  "to": "PENDING_APPROVAL",
  "roles": ["VERIFIER"]
}
```

This avoids putting the workflow transition matrix directly into service branching logic.

Illegal transitions and incorrect roles return a 4xx response rather than silently doing nothing or returning a 500.

---

# Error Handling

A global exception handler provides a consistent error response.

Example:

```json
{
  "ResponseInfo": {
    "status": "failed"
  },
  "Errors": [
    {
      "code": "INVALID_REQUEST",
      "message": "..."
    }
  ]
}
```

Validation and business-rule failures are returned as client errors.

Examples include:

* invalid road type
* inactive road type
* invalid dimensions
* invalid duration
* invalid workflow action
* incorrect actor role
* tenant mismatch
* invalid application state

---

# Application Number Concurrency

Application numbers are generated using a database-backed sequence table:

```text
application_sequences
```

The sequence is selected using a database row lock (`findForUpdate`) for the tenant, financial year and module.

Therefore, two simultaneous creates for the same tenant/year/module cannot receive the same sequence value.

The database also has a unique constraint on:

```text
application_number
```

which provides an additional uniqueness guarantee.

---

# Audit / Transition History

Each workflow action creates a record in:

```text
application_transitions
```

The record contains:

* application ID
* previous status
* action
* new status
* actor UUID
* actor username
* actor role
* comment
* timestamp

This provides the audit trail needed for displaying the application lifecycle.

---

# Testing

Tests are implemented using JUnit 5 and Mockito.

Current test areas include:

* application creation
* tenant mismatch during creation
* search by application number
* search by status
* search by mobile number
* search without filters
* workflow rule validation
* fee calculation
* global exception handling

Run all tests with:

```bash
mvn test
```

Build the project with:

```bash
mvn clean package
```

The project was verified with a successful Maven build.

---

# Manual API Verification

The backend was also manually verified using PowerShell and the running PostgreSQL container.

A calculation request successfully returned the fee breakdown.

An application was successfully created with:

```text
DEH-RCP-000006-2026-27
```

The application was then successfully transitioned:

```text
CREATE
APPLIED
    ↓
VERIFY
PENDING_APPROVAL
    ↓
APPROVE
APPROVED
```

The PostgreSQL database confirmed the application status and transition history.

---

# Frontend Scope

The assignment requires a React/TypeScript portal for both applicant and officer workflows.

The React frontend was not completed within the available time box. I deliberately prioritized the backend core requirements, including fee calculation, configuration-driven rates, tenant isolation, workflow configuration, persistence, search, database migrations and automated tests.

I have therefore not represented the frontend as complete in this submission. The backend APIs were manually tested using PowerShell and PostgreSQL and are ready to be consumed by the required portal.

---

# Deliberately Not Built

The following items were intentionally not implemented:

* Authentication/login
* External identity provider integration
* Production deployment
* Remote fee configuration service
* Payment integration
* Notification integration
* Complete React portal
* Additional stretch functionality

Authentication was explicitly out of scope in the assignment.

The frontend and stretch functionality were left incomplete because the implementation was time-boxed and priority was given to the backend core requirements.

---

# Assumptions

1. `RequestInfo.userInfo` is trusted as the caller identity because authentication is explicitly out of scope.
2. Tenant ID from the caller must match the tenant ID used for the requested operation.
3. Fee configuration is loaded once from the bundled JSON file at application startup.
4. Application numbers use the `RCP` module code.
5. The financial year runs from April through March.
6. Application transition history is immutable audit data.
7. Application fees are stored as a snapshot when the application is created so that later configuration changes do not silently alter the stored amount.

---

# Rate Versioning

The current implementation loads rates from a JSON configuration at startup, but it does not maintain historical versions of that configuration.

The important consequence is that simply changing the JSON file and restarting the service could cause future calculations to use the new rates, while existing applications retain their stored calculated amounts.

For a production implementation, I would introduce versioned rate configurations with an effective-from date and a government-order/version identifier. The selected rate version would be stored against the application when it is created. This would allow historical applications to continue using the exact rate configuration under which they were issued, while new applications use the current effective configuration.

---

# Concurrency

Application number generation uses a database-backed sequence table and a row-level lock for the tenant, financial year and module. The application number also has a database unique constraint.

This prevents two concurrent creates from intentionally receiving the same application number.

For workflow actions, the application is loaded and updated inside a transactional service method. The database entity contains a version field, but full optimistic-lock conflict handling has not been completed in this time-boxed implementation.

In a production version, I would use optimistic locking explicitly for workflow updates so that if two officers act on the same application simultaneously, one action succeeds and the stale action receives a clear conflict response rather than being allowed to overwrite the newer state.

---

# Least Satisfactory Decision

The decision I am least happy with is stopping before completing the React portal. The time box forced me to prioritize the backend because the core requirements around fee correctness, tenant isolation, configuration-driven rules, workflow and persistence were more important than producing a partially working UI.

With two additional days, I would complete the React/TypeScript portal, add API integration tests, strengthen optimistic locking for concurrent workflow actions, and add more boundary tests around validation and configuration.

The main risk of changing this after the service is live is API contract compatibility. Any frontend or external consumer already relying on the existing API responses should not be broken, so additional fields and behaviours should be introduced in a backward-compatible manner.

---

# AI Usage

AI assistance was used during development for:

* understanding and breaking down the assignment
* reviewing implementation approaches
* generating and refining test cases
* troubleshooting Docker/PostgreSQL/Flyway issues
* reviewing API behaviour
* identifying edge cases in fee and workflow rules

One place where AI helped was identifying the importance of configuration-driven workflow transitions and tenant-scoped database queries.

One area where AI could be misleading is assuming that code is correct simply because it compiles or a happy-path request succeeds. I therefore verified the implementation by running the application, executing API requests manually and checking the resulting PostgreSQL records and transition history.

All submitted code was reviewed and tested in the local environment.

---

# Time / Scope Decision

The assignment was treated as a strict time-boxed exercise.

I prioritized:

1. Correct fee calculation
2. Configuration-driven rates
3. Tenant isolation
4. Application persistence
5. Workflow and role checks
6. Transition history
7. Search
8. Database migration
9. Automated tests
10. Manual API/database verification

I deliberately stopped rather than expanding the scope with unfinished functionality.

---

# Submission Checklist

* [x] Spring Boot backend
* [x] PostgreSQL
* [x] Flyway migration
* [x] Configuration-driven fee rates
* [x] Tenant fee override
* [x] BigDecimal monetary calculations
* [x] Fee calculation endpoint
* [x] Server-side fee calculation during create
* [x] Application creation
* [x] Application numbering
* [x] Workflow configuration
* [x] Role-based workflow
* [x] Transition history
* [x] Search endpoint
* [x] Tenant-scoped persistence
* [x] Validation/error handling
* [x] Automated tests
* [x] Manual API verification
* [x] Manual database verification
* [x] `reviewRef: K7Q2`
* [ ] React portal
* [ ] Stretch item

---

## Final Note

This submission intentionally prioritizes a working, tested backend core over an incomplete frontend implementation. The API, database schema, fee configuration, workflow configuration and tests are included in the repository and can be run locally using the commands above.

Spec revision: 3.1-KESTREL
