# Road Cutting Permission — Take-Home Assignment

## Overview

This project implements a multi-tenant **Road Cutting Permission** service with a Spring Boot backend, PostgreSQL database, Flyway migrations, configuration-driven fee calculation, workflow management, and a React/TypeScript frontend.

The solution supports:

* Fee calculation based on external JSON configuration
* Tenant-specific fee overrides
* Road type validation
* Server-side fee recomputation
* Application creation
* Financial-year-based application numbers
* Role-based application workflow
* Application transition history
* Tenant isolation
* Application search with pagination
* PostgreSQL persistence
* Flyway database migrations
* Automated unit tests
* React/TypeScript applicant and officer portal
* Backend/frontend integration

Authentication is intentionally out of scope as specified in the assignment. Caller identity and roles are supplied through `RequestInfo.userInfo` in the request.

---

# Technology Stack

## Backend

* Java 21
* Spring Boot 3.5.x
* Spring Web
* Spring Data JPA
* PostgreSQL 16
* Flyway
* Maven
* Bean Validation
* JUnit 5
* Mockito
* Docker / Docker Compose

## Frontend

* React
* TypeScript
* Vite
* npm
* Fetch API
* Responsive UI

## Configuration

Business configuration is externalized into:

```text
src/main/resources/fee-rates.json
src/main/resources/workflow-rules.json
```

This keeps fee rules and workflow transitions outside the core business logic.

---

# Project Structure

```text
road-cutting-permission/
│
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com.parakore/
│   │   │       ├── application/
│   │   │       │   ├── controller/
│   │   │       │   ├── dto/
│   │   │       │   ├── entity/
│   │   │       │   ├── repository/
│   │   │       │   └── service/
│   │   │       │
│   │   │       ├── common/
│   │   │       │   └── exception/
│   │   │       │
│   │   │       ├── config/
│   │   │       │
│   │   │       ├── fee/
│   │   │       │   ├── dto/
│   │   │       │   └── service/
│   │   │       │
│   │   │       └── RoadCuttingPermissionApplication.java
│   │   │
│   │   └── resources/
│   │       ├── db/
│   │       │   └── migration/
│   │       │       └── V1__create_initial_schema.sql
│   │       ├── application.yml
│   │       ├── fee-rates.json
│   │       └── workflow-rules.json
│   │
│   └── test/
│       └── java/
│           └── com.parakore/
│
├── frontend/
│   ├── src/
│   │   ├── App.tsx
│   │   ├── App.css
│   │   ├── index.css
│   │   └── main.tsx
│   ├── package.json
│   └── vite.config.ts
│
├── docker-compose.yml
├── pom.xml
└── README.md
```

---

# Architecture

The application is implemented as a layered Spring Boot service.

```text
React / TypeScript Frontend
          |
          | HTTP / JSON
          v
Spring Boot REST Controllers
          |
          v
Application Services
          |
     +----+----+
     |         |
     v         v
Fee Service   Workflow Service
     |         |
     v         v
JSON Config   Workflow Config
     |
     v
PostgreSQL / JPA
```

The frontend communicates with the backend REST APIs and displays application creation, fee calculation results, search results and workflow actions.

---

# Running the Application

## Prerequisites

Install:

* Java 21 or later
* Maven
* Node.js / npm
* Docker Desktop

Verify:

```bash
java -version
mvn -version
node --version
npm --version
```

---

# 1. Start PostgreSQL

From the project root:

```bash
docker compose up -d
```

Verify the container:

```bash
docker ps
```

PostgreSQL is exposed on:

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

---

# 2. Start the Backend

From the project root:

```bash
mvn spring-boot:run
```

The backend runs on:

```text
http://localhost:8080
```

Flyway automatically creates the database schema during startup.

---

# 3. Start the React Frontend

Open a second terminal:

```bash
cd frontend
npm install
npm run dev
```

The frontend runs on:

```text
http://localhost:5173
```

Open the URL in a browser.

The frontend communicates with the Spring Boot backend running on port `8080`.

---

# Production Frontend Build

The React application can be built using:

```bash
cd frontend
npm run build
```

The production files are generated in:

```text
frontend/dist
```

The production build was successfully verified during development.

---

# Database Migration

Flyway is enabled through `application.yml`.

Migration location:

```text
src/main/resources/db/migration
```

Initial migration:

```text
V1__create_initial_schema.sql
```

Hibernate uses:

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate
```

Therefore Hibernate validates the schema rather than automatically modifying it.

The main tables are:

```text
applications
application_transitions
application_sequences
```

---

# API Endpoints

Base path:

```text
/rcp/v1
```

## 1. Calculate Fee

```text
POST /rcp/v1/_calculate
```

Provides a stateless fee calculation/preview.

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

The response contains:

* Area
* Restoration charge
* Permission fee
* Urgency surcharge
* Security deposit
* Total amount
* Review reference

The implementation uses `BigDecimal` for monetary calculations.

---

# 2. Create Application

```text
POST /rcp/v1/_create
```

A successful application starts in:

```text
APPLIED
```

During creation:

1. Tenant identity is validated.
2. The application date is determined server-side.
3. Fees are recalculated server-side.
4. A financial-year sequence is obtained.
5. The application number is generated.
6. The application is persisted.
7. A `CREATE` transition is recorded.

Example application number:

```text
DEH-RCP-000007-2026-27
```

The number contains:

* Tenant/city prefix
* Module code
* Zero-padded sequence
* Financial year

---

# 3. Workflow Action

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

The caller's role is supplied through:

```text
RequestInfo.userInfo.roles
```

The role is checked against the configuration-driven workflow rules.

Workflow:

```text
                    VERIFY
APPLIED ------------------------------> PENDING_APPROVAL
   |                                         |
   |                                         |
   | CANCEL                           +------+------+
   |                                  |             |
   v                               APPROVE        REJECT
CANCELLED                            |             |
                                     v             v
                                 APPROVED       REJECTED

PENDING_APPROVAL
       |
       | SEND_BACK
       v
    APPLIED
```

Every successful workflow action is recorded in:

```text
application_transitions
```

---

# 4. Search Applications

```text
POST /rcp/v1/_search
```

Supported filters:

* Application number
* Status
* Mobile number
* Applicant UUID
* Offset
* Limit

The maximum server-side limit is:

```text
100
```

Searches are always tenant scoped.

Examples of supported statuses:

```text
APPLIED
PENDING_APPROVAL
APPROVED
REJECTED
CANCELLED
```

---

# Fee Calculation Rules

Fee configuration is stored in:

```text
src/main/resources/fee-rates.json
```

The calculation service accesses the configuration through the `FeeRateProvider` abstraction.

This allows the configuration source to be replaced later without changing the core calculation logic.

---

## Area

```text
ceil(length × width)
```

The multiplication is performed first and the resulting area is rounded up.

Example:

```text
12.5 × 1.2 = 15
```

---

## Restoration Charge

```text
area × restorationRatePerSqm
```

---

## Permission Fee

```text
area × permissionRatePerSqmPerDay × durationInDays
```

For:

```text
GOVERNMENT_AGENCY
```

the permission fee is zero.

---

## Urgency Surcharge

The surcharge applies when:

```text
proposedStartDate - applicationDate < urgencyThresholdDays
```

The comparison is strict.

Therefore, when the proposed start date is exactly three days away and the threshold is three days:

```text
No urgency surcharge
```

---

## Security Deposit

```text
max(
    minSecurityDeposit,
    restorationCharge × securityDepositPercent
)
```

---

## Total

```text
restoration
+ permission
+ urgency surcharge
+ security deposit
```

Monetary calculations use `BigDecimal`.

---

# Tenant-Specific Configuration

The configuration contains:

```text
dehradun
haridwar
```

Haridwar overrides selected BT values:

```json
{
  "code": "BT",
  "permissionRatePerSqmPerDay": 20,
  "minSecurityDeposit": 7500
}
```

Other values fall back to the default configuration.

Tenant ID is also included in database queries to prevent cross-tenant application access.

---

# Worked Example

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

---

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

---

# Workflow Configuration

Workflow rules are stored in:

```text
src/main/resources/workflow-rules.json
```

Each transition defines:

* Current state
* Action
* Target state
* Permitted role

Example:

```json
{
  "from": "APPLIED",
  "action": "VERIFY",
  "to": "PENDING_APPROVAL",
  "roles": ["VERIFIER"]
}
```

This avoids hard-coding the complete workflow transition matrix inside the service.

Invalid actions and unauthorized roles are rejected with client errors.

---

# Tenant Isolation

Every application lookup is tenant scoped.

For example:

```java
findByTenantIdAndApplicationNumber(
    tenantId,
    applicationNumber
)
```

Similarly, search operations use tenant-specific repository methods.

This prevents an application belonging to one tenant from being returned to another tenant through the normal API.

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

Examples of handled business errors include:

* Invalid road type
* Inactive road type
* Invalid dimensions
* Invalid duration
* Invalid workflow action
* Unauthorized workflow role
* Tenant mismatch
* Application not found
* Invalid configured target status

---

# Application Number Concurrency

Application numbers are generated using:

```text
application_sequences
```

The sequence is selected using a database row lock for:

```text
tenant + financial year + module
```

The sequence value is incremented inside the same transaction.

The database also has a unique constraint on:

```text
application_number
```

This provides an additional uniqueness guarantee.

---

# Audit / Transition History

Every application creation and successful workflow action creates a record in:

```text
application_transitions
```

The transition stores:

* Application ID
* Previous status
* Action
* New status
* Actor UUID
* Actor username
* Actor role
* Comment
* Timestamp

This provides an audit trail for the application lifecycle.

---

# Frontend

The React/TypeScript frontend provides a simple portal for interacting with the backend.

The implemented UI supports:

## Applicant functionality

* Select tenant
* Enter applicant UUID
* Enter mobile number
* Select road type
* Enter road dimensions
* Enter duration
* Select applicant type
* Select proposed start date
* Create road cutting application
* Display calculated fee breakdown
* Display generated application number

## Application search

The UI supports:

* Search by application number
* Filter by status
* Search by mobile number
* Display application results
* Display application status
* Display calculated amount
* Display available workflow actions

## Officer workflow

The frontend exposes available actions based on the current application status and selected role, including:

```text
Verify
Approve
Reject
Send Back
Cancel
```

The frontend was manually verified against the running Spring Boot backend.

---

# End-to-End Verification

The complete application flow was manually tested.

## Application Creation

A successful request produced:

```text
DEH-RCP-000007-2026-27
```

with:

```text
Status: APPLIED
Area: 50 sqm
Restoration Charge: ₹60,000
Permission Fee: ₹1,500
Urgency Surcharge: ₹150
Security Deposit: ₹15,000
Total Amount: ₹76,650
```

## Search

The frontend successfully retrieved persisted applications from PostgreSQL.

Example:

```text
Search completed. 7 application(s) found.
```

## Workflow

The same application was successfully transitioned through:

```text
CREATE
   ↓
APPLIED
   ↓ VERIFY
PENDING_APPROVAL
   ↓ APPROVE
APPROVED
```

This confirms the integration between:

```text
React
  ↓
Spring Boot REST API
  ↓
Application Service
  ↓
PostgreSQL
```

---

# Testing

Tests are implemented using:

* JUnit 5
* Mockito
* Spring test support

Current test areas include:

* Application creation
* Tenant mismatch during creation
* Search by application number
* Search by status
* Search by mobile number
* Search without filters
* Workflow rule validation
* Fee calculation
* Global exception handling

Run the backend tests:

```bash
mvn clean test
```

Build the backend:

```bash
mvn clean package
```

Build the frontend:

```bash
cd frontend
npm run build
```

Both backend and frontend builds were successfully verified during development.

---

# Manual Verification

The application was manually tested using:

* PowerShell
* PostgreSQL
* Docker
* Browser-based React frontend

Verified functionality includes:

```text
Fee calculation
      ↓
Application creation
      ↓
PostgreSQL persistence
      ↓
Application search
      ↓
Workflow verification
      ↓
Approval
```

---

# Assumptions

1. Authentication is out of scope as specified by the assignment.
2. `RequestInfo.userInfo` represents the caller identity.
3. Tenant ID from the caller must match the tenant used for the operation.
4. Fee configuration is loaded from bundled JSON configuration.
5. Application numbers use the `RCP` module code.
6. The financial year runs from April through March.
7. Transition history is treated as audit data.
8. Application fees are stored as a snapshot during application creation.
9. Existing applications retain their stored fee values even if future configuration changes are made.

---

# Rate Versioning

The current implementation loads fee rates from JSON configuration at application startup.

Historical rate versions are not currently maintained.

For a production implementation, rate configuration could be versioned using:

* Effective-from date
* Effective-to date
* Government order/version ID
* Configuration version

The selected rate version could then be stored against each application so historical applications always retain the exact rate configuration under which they were created.

---

# Concurrency

Application number generation uses:

* Database-backed sequence records
* Row-level locking
* Database uniqueness constraint

This prevents duplicate application numbers under concurrent creation.

The application entity also contains a version field.

Full optimistic-lock conflict handling for simultaneous workflow actions has not been added in this time-boxed implementation.

For production, optimistic locking could be enabled explicitly so that concurrent officer updates produce a clear conflict response instead of allowing a stale update.

---

# AI Usage

AI assistance was used during development for:

* Understanding and breaking down the assignment
* Reviewing implementation approaches
* Generating and refining test cases
* Troubleshooting Docker/PostgreSQL/Flyway issues
* Reviewing API behaviour
* Identifying edge cases
* Reviewing fee and workflow logic
* Assisting with React/TypeScript implementation

AI-generated suggestions were reviewed and tested locally.

The implementation was verified through actual Maven builds, automated tests, API requests, PostgreSQL inspection and browser-based frontend testing.

---

# Scope / Time-Box Decision

The assignment was treated as a time-boxed exercise.

Priority was given to:

1. Correct fee calculation
2. Configuration-driven rates
3. Tenant isolation
4. Application persistence
5. Application numbering
6. Workflow and role checks
7. Transition history
8. Search
9. Database migration
10. Automated tests
11. React frontend integration
12. End-to-end verification

The core backend and frontend integration were completed and manually verified.

---

# Future Improvements

Potential production improvements include:

* Real authentication and authorization
* Optimistic locking for workflow actions
* Versioned fee configuration
* API integration tests
* More validation boundary tests
* Centralized exception/error codes
* Production deployment configuration
* External configuration service
* Notification integration
* Payment integration
* Improved frontend role/login management
* Automated frontend tests
* CI/CD pipeline

These were intentionally kept outside the core time-boxed implementation.

---

# Submission Checklist

* [x] Spring Boot backend
* [x] Java 21
* [x] PostgreSQL
* [x] Flyway migration
* [x] Configuration-driven fee rates
* [x] Tenant fee override
* [x] BigDecimal monetary calculations
* [x] Fee calculation endpoint
* [x] Server-side fee calculation during create
* [x] Application creation
* [x] Application numbering
* [x] Financial year handling
* [x] Workflow configuration
* [x] Role-based workflow
* [x] Transition history
* [x] Search endpoint
* [x] Tenant-scoped persistence
* [x] Validation/error handling
* [x] Automated tests
* [x] Manual API verification
* [x] Manual database verification
* [x] React/TypeScript frontend
* [x] Create application UI
* [x] Search application UI
* [x] Workflow action UI
* [x] Backend/frontend integration
* [x] Frontend production build
* [x] `reviewRef: K7Q2`

---

# Final Note

This submission provides a working end-to-end Road Cutting Permission application.

The backend implements the core business requirements including fee calculation, tenant-specific configuration, application persistence, application numbering, workflow management, role validation, transition history and search.

The React/TypeScript frontend provides the corresponding application creation, search and workflow experience and has been manually verified against the running backend.

The project can be started locally using Docker for PostgreSQL, Maven for the backend and npm/Vite for the frontend.

**Spec revision: 3.1-KESTREL**
