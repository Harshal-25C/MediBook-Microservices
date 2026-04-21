# 🏥 MediBook — Provider Service

<div align="center">

```
██████╗ ██████╗  ██████╗ ██╗   ██╗██╗██████╗ ███████╗██████╗
██╔══██╗██╔══██╗██╔═══██╗██║   ██║██║██╔══██╗██╔════╝██╔══██╗
██████╔╝██████╔╝██║   ██║██║   ██║██║██║  ██║█████╗  ██████╔╝
██╔═══╝ ██╔══██╗██║   ██║╚██╗ ██╔╝██║██║  ██║██╔══╝  ██╔══██╗
██║     ██║  ██║╚██████╔╝ ╚████╔╝ ██║██████╔╝███████╗██║  ██║
╚═╝     ╚═╝  ╚═╝ ╚═════╝   ╚═══╝  ╚═╝╚═════╝ ╚══════╝╚═╝  ╚═╝
```

### `provider-service` — Provider Profiles, Search, Verification & Rating

*Book Smarter. Heal Faster. Care Better.*

---

![Java](https://img.shields.io/badge/Java-17-orange?style=for-the-badge&logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.2.0-brightgreen?style=for-the-badge&logo=springboot)
![Spring Security](https://img.shields.io/badge/Spring_Security-6-green?style=for-the-badge&logo=springsecurity)
![MySQL](https://img.shields.io/badge/MySQL-8.0-blue?style=for-the-badge&logo=mysql)
![JWT](https://img.shields.io/badge/JWT-0.11.5-black?style=for-the-badge&logo=jsonwebtokens)
![Swagger](https://img.shields.io/badge/Swagger-OpenAPI_3.0-85EA2D?style=for-the-badge&logo=swagger)
![Maven](https://img.shields.io/badge/Maven-3.9+-C71A36?style=for-the-badge&logo=apachemaven)
![Docker](https://img.shields.io/badge/Docker-Ready-2496ED?style=for-the-badge&logo=docker)
![Port](https://img.shields.io/badge/Port-8082-purple?style=for-the-badge)
![Branch](https://img.shields.io/badge/Branch-feature/provider--service-yellow?style=for-the-badge&logo=git)

</div>

---

## 📋 Table of Contents

- [Overview](#-overview)
- [Architecture Position](#-architecture-position)
- [Tech Stack](#-tech-stack)
- [Project Structure](#-project-structure)
- [Features](#-features)
- [Database Schema](#-database-schema)
- [Prerequisites](#-prerequisites)
- [Environment Variables](#-environment-variables)
- [How to Run](#-how-to-run)
- [Security Architecture](#-security-architecture)
- [API Reference](#-api-reference)
- [Postman Testing Guide](#-postman-testing-guide)
- [Test Cases](#-test-cases)
- [Error Responses](#-error-responses)
- [Swagger UI](#-swagger-ui)
- [Common Issues & Fixes](#-common-issues--fixes)

---

## 📖 Overview

The **Provider Service** manages the complete lifecycle of healthcare provider profiles on the MediBook platform. It handles everything from initial profile creation to admin verification, availability management, and rating aggregation.

Per the MediBook case study (PDF Section 4.2), this service:

- Stores specialization, qualifications, experience, clinic details, and aggregated rating
- **Requires admin verification** before a provider appears in patient search results
- Supports full-text search by name, specialization, or location
- Exposes availability and verified-status flags
- Is the only service that can be browsed by **unauthenticated guests** (PDF requirement)

```
Guest/Patient → search providers → filter by spec/location/rating → view profile → book appointment
                                          ↑
                              provider-service answers all these
```

---

## 🗺 Architecture Position

```
┌──────────────────────────────────────────────────────────────────────┐
│                    MediBook Microservices                            │
│                                                                      │
│  React Client / Postman                                              │
│         │                                                            │
│         ▼  port 8080                                                 │
│  ┌──────────────┐                                                    │
│  │  API Gateway │  validates JWT → injects X-User-* headers         │
│  └──────┬───────┘                                                    │
│         │  routes /api/v1/providers/**                               │
│         ▼                                                            │
│  ┌──────────────────┐  port 8082   ┌─────────────────┐             │
│  │ provider-service │◄────────────►│   MySQL         │ provider_db │
│  │   (THIS ONE)     │              └─────────────────┘             │
│  └──────────────────┘                                               │
│         │                                                            │
│  Receives calls from:                                                │
│  ├── auth-service   — after PROVIDER registers, creates profile     │
│  ├── review-service — calls PUT /rating to update avgRating         │
│  └── appointment-service — reads provider details for booking       │
│                                                                      │
│  ┌────────────────┐  port 8761                                      │
│  │  Eureka Server │ ← provider-service registers here               │
│  └────────────────┘                                                  │
└──────────────────────────────────────────────────────────────────────┘
```

**Microservice boundary note:** `provider_db` stores only provider-specific data. The `userId` column is a plain `Long` reference to `auth-service`'s users table — no JPA join, no cross-service foreign key. This is intentional microservice design.

---

## 🛠 Tech Stack

| Layer | Technology | Version | Purpose |
|-------|-----------|---------|---------|
| **Language** | Java | 17 | Core language |
| **Framework** | Spring Boot | 3.2.0 | Application framework |
| **Security** | Spring Security | 6 | JWT-based stateless auth |
| **JWT Validation** | jjwt | 0.11.5 | Validate tokens from auth-service |
| **Database** | MySQL | 8.0 | Provider profile storage |
| **ORM** | Spring Data JPA + Hibernate | 6.3.1 | Database abstraction |
| **API Docs** | SpringDoc OpenAPI | 2.5.0 | Swagger UI |
| **Build** | Maven | 3.9+ | Dependency management |
| **Boilerplate** | Lombok | 1.18.30 | Reduce boilerplate |
| **Service Discovery** | Eureka Client | 2023.0.3 | Register with Eureka Server |
| **Monitoring** | Spring Boot Actuator | 3.2.0 | Health, info, metrics |
| **Dev Tools** | Spring DevTools | — | Hot reload |

---

## 📁 Project Structure

```
provider-service/
│
├── 📄 pom.xml                                        ← Maven dependencies
│
└── src/
    ├── main/
    │   ├── java/com/medibook/provider/
    │   │   │
    │   │   ├── 🚀 ProviderServiceApplication.java    ← Entry point
    │   │   │      @SpringBootApplication
    │   │   │      @EnableScheduling
    │   │   │
    │   │   ├── config/
    │   │   │   ├── GatewayJwtAuthenticationFilter.java ← Dual-mode JWT filter
    │   │   │   │     Scenario A: reads X-User-* headers (via API Gateway)
    │   │   │   │     Scenario B: parses Bearer token (direct Postman call)
    │   │   │   └── SecurityConfig.java               ← Public GET paths + STATELESS
    │   │   │
    │   │   ├── dto/
    │   │   │   ├── request/
    │   │   │   │   ├── ProviderRegistrationRequest.java ← userId, spec, qual, exp, bio, clinic
    │   │   │   │   └── UpdateProviderRequest.java       ← all fields optional (partial update)
    │   │   │   └── response/
    │   │   │       └── ProviderResponse.java            ← full provider profile response
    │   │   │
    │   │   ├── entity/
    │   │   │   └── Provider.java                     ← JPA entity → `providers` table
    │   │   │         3 database indexes for performance
    │   │   │         userId UNIQUE constraint
    │   │   │
    │   │   ├── exception/
    │   │   │   ├── GlobalExceptionHandler.java       ← 404/400/409/validation handler
    │   │   │   └── ResourceNotFoundException.java    ← 404 exception type
    │   │   │
    │   │   ├── repository/
    │   │   │   └── ProviderRepository.java           ← 10 query methods + 3 @Query JPQL
    │   │   │
    │   │   ├── resource/
    │   │   │   └── ProviderResource.java             ← REST controller, 15 endpoints
    │   │   │
    │   │   ├── service/
    │   │   │   ├── ProviderService.java              ← Business contract (interface)
    │   │   │   └── impl/
    │   │   │       └── ProviderServiceImpl.java      ← Business logic, 15 methods
    │   │   │
    │   │   └── util/
    │   │       └── JwtUtil.java                      ← extractEmail/Role/UserId + validate
    │   │
    │   └── resources/
    │       └── application.yml                       ← All config with env var defaults
    │
    └── test/
        └── java/com/medibook/provider/
            └── ProviderServiceApplicationTests.java  ← Context load test
```

---

## ✨ Features

### Profile Management
- ✅ **Register provider profile** — linked to userId from auth-service
- ✅ **Partial profile update** — only non-null fields are updated
- ✅ **Get by providerId** — direct lookup
- ✅ **Get by userId** — used after auth-service login to find provider profile
- ✅ **Delete provider** — Admin only, permanently removes profile

### Search & Discovery (all public — no auth needed)
- ✅ **Browse all providers** — list all profiles
- ✅ **Get verified-only providers** — pre-filtered list
- ✅ **Filter by specialization** — case-insensitive match
- ✅ **Full-text search** — searches specialization + clinic name
- ✅ **Advanced filter** — combine specialization + location + minRating
- ✅ **Count by specialization** — analytics endpoint

### Admin Operations
- ✅ **Verify provider** — marks `isVerified = true`, appears in patient searches
- ✅ **Reject provider** — marks `isVerified = false`, `isAvailable = false`
- ✅ **Delete provider** — permanent removal

### Provider Self-Management
- ✅ **Toggle availability** — provider can set themselves unavailable (e.g., leave)
- ✅ **Update profile** — update any profile field independently

### Inter-Service Integration
- ✅ **Update rating** — called by review-service after a new review is submitted
- ✅ **Dual JWT filter** — works with API Gateway (X-User-* headers) AND direct Postman calls (Bearer token)

### Infrastructure
- ✅ **3 database indexes** — `userId`, `specialization`, `isVerified` for fast queries
- ✅ **Unique constraint** on `userId` — one profile per auth-service user
- ✅ **Eureka Discovery** — registers with service registry
- ✅ **Swagger UI** — interactive API docs
- ✅ **Actuator** — health, info, metrics
- ✅ **Global Exception Handler** — consistent JSON error responses

---

## 🗄 Database Schema

Table: **`providers`** (auto-created by Hibernate on first startup — `ddl-auto: update`)

```sql
CREATE TABLE providers (
    provider_id      BIGINT       NOT NULL AUTO_INCREMENT,
    user_id          BIGINT       NOT NULL UNIQUE,
    specialization   VARCHAR(255) NOT NULL,
    qualification    TEXT         NOT NULL,
    experience_years INT,
    bio              TEXT,
    clinic_name      VARCHAR(255),
    clinic_address   TEXT,
    avg_rating       DOUBLE       DEFAULT 0.0,
    is_verified      BIT          DEFAULT 0,
    is_available     BIT          DEFAULT 1,
    created_at       DATETIME(6),
    updated_at       DATETIME(6),
    PRIMARY KEY (provider_id),

    UNIQUE KEY uq_user_id      (user_id),
    INDEX idx_user_id          (user_id),
    INDEX idx_specialization   (specialization),
    INDEX idx_is_verified      (is_verified)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**Key design decisions:**

- `user_id` is a plain `BIGINT` — not a foreign key. There is no JPA join to auth-service. This enforces the microservice boundary. The provider-service trusts that the userId was valid when passed by auth-service.
- `is_verified = 0` by default — admin must explicitly verify before patient-facing endpoints return the provider
- `is_available = 1` by default — provider is available immediately after registration
- `avg_rating = 0.0` by default — updated by the review-service via `PUT /{providerId}/rating`

---

## ✅ Prerequisites

| Tool | Version | Check Command |
|------|---------|---------------|
| Java JDK | 17+ | `java -version` |
| Maven | 3.9+ | `mvn -version` |
| MySQL | 8.0 | `mysql --version` |
| Git | Any | `git --version` |

> No Redis required. No SMTP required. provider-service only needs MySQL and optionally Eureka.

---

## 🔐 Environment Variables

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `JWT_SECRET` | ✅ Yes | — | Must match auth-service secret exactly |
| `DB_URL` | ❌ | `jdbc:mysql://localhost:3306/provider_db?...` | MySQL connection URL |
| `DB_USERNAME` | ❌ | `medibook_user` | MySQL username |
| `DB_PASSWORD` | ❌ | `medibook_pass` | MySQL password |
| `EUREKA_ENABLED` | ❌ | `false` | Set `true` when Eureka Server is running |
| `EUREKA_DEFAULT_ZONE` | ❌ | `http://localhost:8761/eureka/` | Eureka server URL |

### Set on Windows CMD

```cmd
set JWT_SECRET=medibook-super-secret-key-must-be-at-least-256-bits-long-change-in-prod
set DB_USERNAME=medibook_user
set DB_PASSWORD=medibook_pass
set EUREKA_ENABLED=false
```

### Set in Eclipse Run Config

1. Right-click `ProviderServiceApplication.java` → **Run As → Run Configurations**
2. **Environment** tab → **New** for each variable
3. **Apply** → **Run**

### Create MySQL Database

```sql
-- Run in MySQL Workbench or CLI
CREATE DATABASE provider_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
GRANT ALL PRIVILEGES ON provider_db.* TO 'medibook_user'@'localhost';
FLUSH PRIVILEGES;
```

---

## ▶ How to Run

```bash
# Step 1 — Checkout branch
git checkout feature/provider-service

# Step 2 — Set env variables
set JWT_SECRET=medibook-super-secret-key-must-be-at-least-256-bits-long-change-in-prod

# Step 3 — Build
cd provider-service
mvn clean install -DskipTests

# Step 4 — Run
mvn spring-boot:run
```

### Successful Startup Output

```
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
 :: Spring Boot ::                (v3.2.0)

Hibernate:
    create table providers (
        provider_id bigint not null auto_increment,
        ...
    ) engine=InnoDB

INFO  Tomcat started on port 8082 (http)
INFO  Started ProviderServiceApplication in 8.2 seconds
Provider-Service is Running......!
```

---

## 🔒 Security Architecture

```
Incoming Request to port 8082
         │
         ▼
┌──────────────────────────────────────────────────────────┐
│          GatewayJwtAuthenticationFilter                   │
│                (OncePerRequestFilter)                     │
│                                                          │
│  Check for X-User-Email + X-User-Role headers            │
│  (these are injected by API Gateway after JWT validation) │
│                                                          │
│  ┌── Scenario A: Via API Gateway ──────────────────────┐ │
│  │  X-User-Email: doc@gmail.com  present?              │ │
│  │  X-User-Role: PROVIDER        present?              │ │
│  │  → Yes → build SecurityContext directly             │ │
│  │          → skip JWT parsing → forward request       │ │
│  └──────────────────────────────────────────────────────┘ │
│                                                          │
│  ┌── Scenario B: Direct call (Postman → :8082) ────────┐ │
│  │  Authorization: Bearer <token>   present?           │ │
│  │  → Yes → JwtUtil.validateToken()                    │ │
│  │        → extractEmail() + extractRole()             │ │
│  │        → build SecurityContext                      │ │
│  └──────────────────────────────────────────────────────┘ │
└───────────────────────┬──────────────────────────────────┘
                        │
                        ▼
┌──────────────────────────────────────────────────────────┐
│                   SecurityConfig                          │
│                                                          │
│  PUBLIC GET (no token needed — PDF requirement):         │
│  GET  /api/v1/providers                                  │
│  GET  /api/v1/providers/**                               │
│  GET  /swagger-ui/**                                     │
│  GET  /api-docs/**                                       │
│  GET  /actuator/health                                   │
│                                                          │
│  ALL OTHER REQUESTS → Must be authenticated              │
└───────────────────────┬──────────────────────────────────┘
                        │
                        ▼
┌──────────────────────────────────────────────────────────┐
│               @PreAuthorize (Method Level)                │
│                                                          │
│  POST   /providers        → PROVIDER or ADMIN            │
│  PUT    /providers/{id}   → PROVIDER or ADMIN            │
│  PUT    /verify           → ADMIN only                   │
│  PUT    /reject           → ADMIN only                   │
│  DELETE /providers/{id}   → ADMIN only                   │
└──────────────────────────────────────────────────────────┘
```

---

## 📡 API Reference

### Base URL
```
http://localhost:8082/api/v1/providers
```
*(or `http://localhost:8080/api/v1/providers` when going through API Gateway)*

---

### 1. Register Provider Profile

| | |
|---|---|
| **Method** | `POST` |
| **URL** | `/api/v1/providers` |
| **Auth** | ✅ Required — `PROVIDER` or `ADMIN` role |
| **Description** | Creates a new provider profile. Must be called after registering as PROVIDER in auth-service |

**Headers**
```
Authorization: Bearer <provider_token>
Content-Type: application/json
```

**Request Body**
```json
{
  "userId": 2,
  "specialization": "Cardiology",
  "qualification": "MBBS, MD (Cardiology) — AIIMS Delhi",
  "experienceYears": 12,
  "bio": "Experienced cardiologist specializing in interventional cardiology and heart failure management.",
  "clinicName": "HeartCare Clinic",
  "clinicAddress": "402, MG Road, Indore, Madhya Pradesh 452001"
}
```

**Response `201 Created`**
```json
{
  "providerId": 1,
  "userId": 2,
  "specialization": "Cardiology",
  "qualification": "MBBS, MD (Cardiology) — AIIMS Delhi",
  "experienceYears": 12,
  "bio": "Experienced cardiologist specializing in interventional cardiology and heart failure management.",
  "clinicName": "HeartCare Clinic",
  "clinicAddress": "402, MG Road, Indore, Madhya Pradesh 452001",
  "avgRating": 0.0,
  "isVerified": false,
  "isAvailable": true,
  "createdAt": "2026-04-21T10:30:00"
}
```

**Error Cases**
| Code | Message |
|------|---------|
| `400` | `Provider profile already exists for userId: 2` |
| `400` | Validation failure (missing specialization, qualification etc.) |
| `401` | Missing or invalid token |
| `403` | PATIENT role cannot register provider profile |

---

### 2. Get All Providers

| | |
|---|---|
| **Method** | `GET` |
| **URL** | `/api/v1/providers` |
| **Auth** | ❌ Public — no token needed |
| **Description** | Returns all provider profiles regardless of verification status |

**Response `200 OK`**
```json
[
  {
    "providerId": 1,
    "userId": 2,
    "specialization": "Cardiology",
    "avgRating": 4.5,
    "isVerified": true,
    "isAvailable": true,
    ...
  }
]
```

---

### 3. Get Verified Providers

| | |
|---|---|
| **Method** | `GET` |
| **URL** | `/api/v1/providers/verified` |
| **Auth** | ❌ Public |
| **Description** | Returns only admin-verified providers — the list patients see when browsing |

**Response `200 OK`** — Array of providers where `isVerified = true`

---

### 4. Get Provider by ID

| | |
|---|---|
| **Method** | `GET` |
| **URL** | `/api/v1/providers/{providerId}` |
| **Auth** | ❌ Public |
| **Description** | Get a specific provider profile by their providerId |

**Response `200 OK`** — Single ProviderResponse

**Error Cases**
| Code | Message |
|------|---------|
| `404` | `Provider not found with id: 99` |

---

### 5. Get Provider by User ID

| | |
|---|---|
| **Method** | `GET` |
| **URL** | `/api/v1/providers/user/{userId}` |
| **Auth** | ❌ Public |
| **Description** | Find a provider using their auth-service userId — useful after login to load the provider's own dashboard |

**Response `200 OK`** — Single ProviderResponse

**Error Cases**
| Code | Message |
|------|---------|
| `404` | `Provider profile not found for userId: 5` |

---

### 6. Get by Specialization

| | |
|---|---|
| **Method** | `GET` |
| **URL** | `/api/v1/providers/specialization/{specialization}` |
| **Auth** | ❌ Public |
| **Description** | Case-insensitive filter by specialization — returns all verification statuses |

**Example**
```
GET /api/v1/providers/specialization/Cardiology
GET /api/v1/providers/specialization/cardiology   (same result)
GET /api/v1/providers/specialization/CARDIOLOGY   (same result)
```

**Response `200 OK`** — Array of ProviderResponse

---

### 7. Search Providers

| | |
|---|---|
| **Method** | `GET` |
| **URL** | `/api/v1/providers/search?q={query}` |
| **Auth** | ❌ Public |
| **Description** | Full-text search across specialization and clinic name fields |

**Examples**
```
GET /api/v1/providers/search?q=heart
GET /api/v1/providers/search?q=HeartCare
GET /api/v1/providers/search?q=Cardio
```

**Response `200 OK`** — Array of matching ProviderResponse objects

---

### 8. Advanced Filter

| | |
|---|---|
| **Method** | `GET` |
| **URL** | `/api/v1/providers/filter` |
| **Auth** | ❌ Public |
| **Description** | Filter **verified + available** providers by specialization, location, and/or minimum rating. Only returns `isVerified=true AND isAvailable=true` providers. All params optional |

**Query Parameters**
| Param | Type | Required | Description |
|-------|------|----------|-------------|
| `specialization` | String | ❌ | Case-insensitive exact match |
| `location` | String | ❌ | Partial match on clinicAddress |
| `minRating` | Double | ❌ | Minimum avgRating (0.0–5.0) |

**Examples**
```
GET /api/v1/providers/filter?specialization=Cardiology
GET /api/v1/providers/filter?location=Indore
GET /api/v1/providers/filter?minRating=4.0
GET /api/v1/providers/filter?specialization=Cardiology&location=Indore&minRating=4.0
GET /api/v1/providers/filter   (returns all verified + available)
```

**Response `200 OK`** — Array of verified+available ProviderResponse objects

---

### 9. Count by Specialization

| | |
|---|---|
| **Method** | `GET` |
| **URL** | `/api/v1/providers/count?specialization={spec}` |
| **Auth** | ❌ Public |
| **Description** | Count how many providers exist for a given specialization |

**Response `200 OK`**
```json
{
  "count": 7
}
```

---

### 10. Update Provider Profile

| | |
|---|---|
| **Method** | `PUT` |
| **URL** | `/api/v1/providers/{providerId}` |
| **Auth** | ✅ Required — `PROVIDER` or `ADMIN` |
| **Description** | Partial update — only sends fields you want to change. Null fields are ignored |

**Headers**
```
Authorization: Bearer <token>
Content-Type: application/json
```

**Request Body** — all fields optional
```json
{
  "bio": "Updated bio with new achievements.",
  "clinicAddress": "New Address, Indore",
  "experienceYears": 15
}
```

**Response `200 OK`** — Updated ProviderResponse

---

### 11. Verify Provider (Admin)

| | |
|---|---|
| **Method** | `PUT` |
| **URL** | `/api/v1/providers/{providerId}/verify` |
| **Auth** | ✅ Required — **ADMIN only** |
| **Description** | Admin approves provider credentials. Sets `isVerified = true`. Provider now appears in patient searches |

**Headers**
```
Authorization: Bearer <admin_token>
```

**Response `200 OK`**
```json
{
  "message": "Provider 1 has been verified successfully."
}
```

**Error Cases**
| Code | Message |
|------|---------|
| `409` | `Provider is already verified: 1` |
| `403` | Non-admin attempting verification |
| `404` | Provider not found |

---

### 12. Reject Provider (Admin)

| | |
|---|---|
| **Method** | `PUT` |
| **URL** | `/api/v1/providers/{providerId}/reject` |
| **Auth** | ✅ Required — **ADMIN only** |
| **Description** | Admin rejects/removes provider verification. Sets `isVerified = false` and `isAvailable = false`. Provider disappears from patient searches |

**Response `200 OK`**
```json
{
  "message": "Provider 1 has been rejected."
}
```

---

### 13. Set Availability

| | |
|---|---|
| **Method** | `PUT` |
| **URL** | `/api/v1/providers/{providerId}/availability?available={true/false}` |
| **Auth** | ✅ Required — `PROVIDER` or `ADMIN` |
| **Description** | Toggle provider availability. Providers can mark themselves unavailable during leave |

**Examples**
```
PUT /api/v1/providers/1/availability?available=false   (going on leave)
PUT /api/v1/providers/1/availability?available=true    (back from leave)
```

**Response `200 OK`**
```json
{
  "message": "Availability set to false for providerId: 1"
}
```

---

### 14. Update Rating

| | |
|---|---|
| **Method** | `PUT` |
| **URL** | `/api/v1/providers/{providerId}/rating?rating={value}` |
| **Auth** | ✅ Required |
| **Description** | Updates the provider's `avgRating`. This endpoint is called internally by `review-service` after every new review is submitted. Rating must be between 0.0 and 5.0 |

**Example**
```
PUT /api/v1/providers/1/rating?rating=4.3
```

**Response `200 OK`**
```json
{
  "message": "Rating updated to 4.3 for providerId: 1"
}
```

**Error Cases**
| Code | Message |
|------|---------|
| `400` | `Rating must be between 0 and 5` |
| `404` | Provider not found |

---

### 15. Delete Provider (Admin)

| | |
|---|---|
| **Method** | `DELETE` |
| **URL** | `/api/v1/providers/{providerId}` |
| **Auth** | ✅ Required — **ADMIN only** |
| **Description** | Permanently deletes a provider profile |

**Headers**
```
Authorization: Bearer <admin_token>
```

**Response `200 OK`**
```json
{
  "message": "Provider 1 deleted successfully."
}
```

---

## 📊 API Summary Table

| # | Method | Endpoint | Auth | Role |
|---|--------|----------|------|------|
| 1 | `POST` | `/api/v1/providers` | ✅ | PROVIDER / ADMIN |
| 2 | `GET` | `/api/v1/providers` | ❌ | Public |
| 3 | `GET` | `/api/v1/providers/verified` | ❌ | Public |
| 4 | `GET` | `/api/v1/providers/{providerId}` | ❌ | Public |
| 5 | `GET` | `/api/v1/providers/user/{userId}` | ❌ | Public |
| 6 | `GET` | `/api/v1/providers/specialization/{spec}` | ❌ | Public |
| 7 | `GET` | `/api/v1/providers/search?q=` | ❌ | Public |
| 8 | `GET` | `/api/v1/providers/filter` | ❌ | Public |
| 9 | `GET` | `/api/v1/providers/count?specialization=` | ❌ | Public |
| 10 | `PUT` | `/api/v1/providers/{providerId}` | ✅ | PROVIDER / ADMIN |
| 11 | `PUT` | `/api/v1/providers/{providerId}/verify` | ✅ | **ADMIN only** |
| 12 | `PUT` | `/api/v1/providers/{providerId}/reject` | ✅ | **ADMIN only** |
| 13 | `PUT` | `/api/v1/providers/{providerId}/availability` | ✅ | PROVIDER / ADMIN |
| 14 | `PUT` | `/api/v1/providers/{providerId}/rating` | ✅ | Any authenticated |
| 15 | `DELETE` | `/api/v1/providers/{providerId}` | ✅ | **ADMIN only** |

---

## 🧪 Postman Testing Guide

### Postman Environment Variables

| Variable | Value | Description |
|----------|-------|-------------|
| `baseUrl` | `http://localhost:8082/api/v1/providers` | Direct service URL |
| `gatewayUrl` | `http://localhost:8080/api/v1/providers` | Via API Gateway |
| `providerToken` | *(paste after login)* | PROVIDER JWT token |
| `adminToken` | *(paste after login)* | ADMIN JWT token |
| `providerId` | *(paste after register)* | Provider's providerId |
| `userId` | *(paste from auth-service login)* | Provider's userId |

### Recommended Testing Order

#### Step 1 — Register provider in auth-service first

```
POST http://localhost:8081/api/v1/auth/send-otp
Body: { "email": "dr.priya@gmail.com" }

POST http://localhost:8081/api/v1/auth/verify-otp
Body: { "email": "dr.priya@gmail.com", "otp": "123456" }

POST http://localhost:8081/api/v1/auth/register
Body:
{
  "fullName": "Dr. Priya Mehta",
  "email": "dr.priya@gmail.com",
  "password": "Priya@123",
  "phone": "9876543211",
  "role": "PROVIDER"
}

POST http://localhost:8081/api/v1/auth/login
Body: { "email": "dr.priya@gmail.com", "password": "Priya@123" }
→ Copy token → save as {{providerToken}}
→ Copy userId → save as {{userId}}
```

#### Step 2 — Register admin in auth-service

```
(Follow same OTP flow with role: "ADMIN")
POST http://localhost:8081/api/v1/auth/login
Body: { "email": "admin@gmail.com", "password": "Admin@123" }
→ Copy token → save as {{adminToken}}
```

#### Step 3 — Provider creates profile

```
POST {{baseUrl}}
Headers: Authorization: Bearer {{providerToken}}
Body:
{
  "userId": {{userId}},
  "specialization": "Cardiology",
  "qualification": "MBBS, MD (Cardiology) — AIIMS Delhi",
  "experienceYears": 12,
  "bio": "Specialist in interventional cardiology.",
  "clinicName": "HeartCare Clinic",
  "clinicAddress": "402, MG Road, Indore, MP 452001"
}
→ Copy providerId → save as {{providerId}}
→ Note: isVerified = false (not visible to patients yet)
```

#### Step 4 — Admin verifies provider

```
PUT {{baseUrl}}/{{providerId}}/verify
Headers: Authorization: Bearer {{adminToken}}
→ isVerified = true (now visible in patient searches)
```

#### Step 5 — Test public search (no token needed)

```
GET {{baseUrl}}
GET {{baseUrl}}/verified
GET {{baseUrl}}/{{providerId}}
GET {{baseUrl}}/user/{{userId}}
GET {{baseUrl}}/specialization/Cardiology
GET {{baseUrl}}/search?q=heart
GET {{baseUrl}}/filter?specialization=Cardiology&location=Indore&minRating=0.0
GET {{baseUrl}}/count?specialization=Cardiology
```

#### Step 6 — Provider updates own profile

```
PUT {{baseUrl}}/{{providerId}}
Headers: Authorization: Bearer {{providerToken}}
Body:
{
  "bio": "Updated: 13 years experience now.",
  "experienceYears": 13
}
```

#### Step 7 — Toggle availability

```
PUT {{baseUrl}}/{{providerId}}/availability?available=false
Headers: Authorization: Bearer {{providerToken}}
→ Provider goes on leave

PUT {{baseUrl}}/{{providerId}}/availability?available=true
Headers: Authorization: Bearer {{providerToken}}
→ Provider returns
```

#### Step 8 — Rating update (simulates review-service)

```
PUT {{baseUrl}}/{{providerId}}/rating?rating=4.5
Headers: Authorization: Bearer {{providerToken}}
```

#### Step 9 — Admin rejects provider

```
PUT {{baseUrl}}/{{providerId}}/reject
Headers: Authorization: Bearer {{adminToken}}
→ isVerified = false, isAvailable = false
→ No longer visible in /filter results
```

#### Step 10 — Admin deletes provider

```
DELETE {{baseUrl}}/{{providerId}}
Headers: Authorization: Bearer {{adminToken}}
→ 200 OK with deletion message
```

### Test 401 — No token on protected endpoint

```
POST {{baseUrl}}
(no Authorization header)
→ 401 Unauthorized
```

### Test 403 — Wrong role

```
PUT {{baseUrl}}/1/verify
Headers: Authorization: Bearer {{providerToken}}   ← PROVIDER token, not ADMIN
→ 403 Forbidden
```

---

## 🧪 Test Cases

Place these files in:
```
provider-service/src/test/java/com/medibook/provider/
├── service/impl/ProviderServiceImplTest.java
├── resource/ProviderResourceTest.java
└── exception/GlobalExceptionHandlerTest.java
```

---

### ProviderServiceImplTest.java

```java
package com.medibook.provider.service.impl;

import com.medibook.provider.dto.request.ProviderRegistrationRequest;
import com.medibook.provider.dto.request.UpdateProviderRequest;
import com.medibook.provider.dto.response.ProviderResponse;
import com.medibook.provider.entity.Provider;
import com.medibook.provider.exception.ResourceNotFoundException;
import com.medibook.provider.repository.ProviderRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProviderServiceImpl — Unit Tests")
class ProviderServiceImplTest {

    @Mock private ProviderRepository providerRepository;
    @InjectMocks private ProviderServiceImpl providerService;

    // ── Helper ────────────────────────────────────────────────────────────

    private Provider buildProvider(Long id, Long userId, boolean verified) {
        return Provider.builder()
                .providerId(id)
                .userId(userId)
                .specialization("Cardiology")
                .qualification("MBBS, MD")
                .experienceYears(10)
                .bio("Experienced cardiologist")
                .clinicName("HeartCare")
                .clinicAddress("Indore, MP")
                .avgRating(0.0)
                .isVerified(verified)
                .isAvailable(true)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private ProviderRegistrationRequest buildRegRequest(Long userId) {
        ProviderRegistrationRequest req = new ProviderRegistrationRequest();
        req.setUserId(userId);
        req.setSpecialization("Cardiology");
        req.setQualification("MBBS, MD");
        req.setExperienceYears(10);
        req.setBio("Cardiologist");
        req.setClinicName("HeartCare");
        req.setClinicAddress("Indore, MP");
        return req;
    }

    // ── registerProvider ──────────────────────────────────────────────────

    @Nested
    @DisplayName("registerProvider()")
    class RegisterProviderTests {

        @Test
        @DisplayName("throws IllegalArgumentException when userId already has a profile")
        void shouldThrow_whenUserIdAlreadyExists() {
            given(providerRepository.existsByUserId(2L)).willReturn(true);

            assertThatThrownBy(() -> providerService.registerProvider(buildRegRequest(2L)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("already exists");

            verify(providerRepository, never()).save(any());
        }

        @Test
        @DisplayName("saves provider with isVerified=false and isAvailable=true by default")
        void shouldSaveWithCorrectDefaults() {
            given(providerRepository.existsByUserId(2L)).willReturn(false);
            Provider saved = buildProvider(1L, 2L, false);
            given(providerRepository.save(any())).willReturn(saved);

            ProviderResponse resp = providerService.registerProvider(buildRegRequest(2L));

            assertThat(resp.getIsVerified()).isFalse();
            assertThat(resp.getIsAvailable()).isTrue();
            assertThat(resp.getAvgRating()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("returns correct ProviderResponse after successful registration")
        void shouldReturnCorrectResponse() {
            given(providerRepository.existsByUserId(2L)).willReturn(false);
            Provider saved = buildProvider(1L, 2L, false);
            given(providerRepository.save(any())).willReturn(saved);

            ProviderResponse resp = providerService.registerProvider(buildRegRequest(2L));

            assertThat(resp.getProviderId()).isEqualTo(1L);
            assertThat(resp.getUserId()).isEqualTo(2L);
            assertThat(resp.getSpecialization()).isEqualTo("Cardiology");
        }
    }

    // ── getProviderById ───────────────────────────────────────────────────

    @Nested
    @DisplayName("getProviderById()")
    class GetByIdTests {

        @Test
        @DisplayName("throws ResourceNotFoundException when providerId not found")
        void shouldThrow_whenNotFound() {
            given(providerRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> providerService.getProviderById(99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("99");
        }

        @Test
        @DisplayName("returns correct ProviderResponse when found")
        void shouldReturn_whenFound() {
            Provider p = buildProvider(1L, 2L, true);
            given(providerRepository.findById(1L)).willReturn(Optional.of(p));

            ProviderResponse resp = providerService.getProviderById(1L);

            assertThat(resp.getProviderId()).isEqualTo(1L);
            assertThat(resp.getIsVerified()).isTrue();
        }
    }

    // ── getProviderByUserId ───────────────────────────────────────────────

    @Nested
    @DisplayName("getProviderByUserId()")
    class GetByUserIdTests {

        @Test
        @DisplayName("throws ResourceNotFoundException when userId not found")
        void shouldThrow_whenUserIdNotFound() {
            given(providerRepository.findByUserId(5L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> providerService.getProviderByUserId(5L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("userId: 5");
        }

        @Test
        @DisplayName("returns ProviderResponse when userId exists")
        void shouldReturn_whenUserIdExists() {
            Provider p = buildProvider(1L, 5L, false);
            given(providerRepository.findByUserId(5L)).willReturn(Optional.of(p));

            ProviderResponse resp = providerService.getProviderByUserId(5L);

            assertThat(resp.getUserId()).isEqualTo(5L);
        }
    }

    // ── getBySpecialization ───────────────────────────────────────────────

    @Nested
    @DisplayName("getBySpecialization()")
    class GetBySpecializationTests {

        @Test
        @DisplayName("returns list of matching providers")
        void shouldReturnMatchingProviders() {
            List<Provider> providers = List.of(
                    buildProvider(1L, 2L, true),
                    buildProvider(2L, 3L, false));
            given(providerRepository.findBySpecializationIgnoreCase("Cardiology"))
                    .willReturn(providers);

            List<ProviderResponse> result = providerService.getBySpecialization("Cardiology");

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("returns empty list when no provider matches")
        void shouldReturnEmpty_whenNoMatch() {
            given(providerRepository.findBySpecializationIgnoreCase("Neurology"))
                    .willReturn(List.of());

            List<ProviderResponse> result = providerService.getBySpecialization("Neurology");

            assertThat(result).isEmpty();
        }
    }

    // ── searchProviders ───────────────────────────────────────────────────

    @Nested
    @DisplayName("searchProviders()")
    class SearchProvidersTests {

        @Test
        @DisplayName("returns matching providers from query")
        void shouldReturnMatchingProviders() {
            given(providerRepository.searchByNameOrSpecialization("heart"))
                    .willReturn(List.of(buildProvider(1L, 2L, true)));

            List<ProviderResponse> result = providerService.searchProviders("heart");

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("returns empty list when query has no match")
        void shouldReturnEmpty_whenNoMatch() {
            given(providerRepository.searchByNameOrSpecialization("xyz"))
                    .willReturn(List.of());

            assertThat(providerService.searchProviders("xyz")).isEmpty();
        }
    }

    // ── filterProviders ───────────────────────────────────────────────────

    @Nested
    @DisplayName("filterProviders()")
    class FilterProvidersTests {

        @Test
        @DisplayName("delegates to repository with correct params")
        void shouldDelegateToRepository() {
            given(providerRepository.filterProviders("Cardiology", "Indore", 4.0))
                    .willReturn(List.of(buildProvider(1L, 2L, true)));

            List<ProviderResponse> result =
                    providerService.filterProviders("Cardiology", "Indore", 4.0);

            assertThat(result).hasSize(1);
            verify(providerRepository).filterProviders("Cardiology", "Indore", 4.0);
        }

        @Test
        @DisplayName("passes null params when not provided")
        void shouldPassNullParams_whenNotProvided() {
            given(providerRepository.filterProviders(null, null, null))
                    .willReturn(List.of());

            providerService.filterProviders(null, null, null);

            verify(providerRepository).filterProviders(null, null, null);
        }
    }

    // ── updateProvider ────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateProvider()")
    class UpdateProviderTests {

        @Test
        @DisplayName("throws ResourceNotFoundException when providerId not found")
        void shouldThrow_whenNotFound() {
            given(providerRepository.findById(77L)).willReturn(Optional.empty());
            UpdateProviderRequest req = new UpdateProviderRequest();
            req.setBio("New bio");

            assertThatThrownBy(() -> providerService.updateProvider(77L, req))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("updates only non-null fields, leaves others unchanged")
        void shouldUpdateOnlyNonNullFields() {
            Provider p = buildProvider(1L, 2L, false);
            given(providerRepository.findById(1L)).willReturn(Optional.of(p));
            given(providerRepository.save(p)).willReturn(p);

            UpdateProviderRequest req = new UpdateProviderRequest();
            req.setBio("Updated bio");

            providerService.updateProvider(1L, req);

            assertThat(p.getBio()).isEqualTo("Updated bio");
            assertThat(p.getSpecialization()).isEqualTo("Cardiology"); // unchanged
            assertThat(p.getExperienceYears()).isEqualTo(10);          // unchanged
        }

        @Test
        @DisplayName("saves after update and returns updated response")
        void shouldSave_andReturnUpdated() {
            Provider p = buildProvider(1L, 2L, false);
            given(providerRepository.findById(1L)).willReturn(Optional.of(p));
            given(providerRepository.save(p)).willReturn(p);

            UpdateProviderRequest req = new UpdateProviderRequest();
            req.setExperienceYears(15);
            req.setClinicName("New Clinic Name");

            ProviderResponse resp = providerService.updateProvider(1L, req);

            assertThat(p.getExperienceYears()).isEqualTo(15);
            assertThat(p.getClinicName()).isEqualTo("New Clinic Name");
            verify(providerRepository).save(p);
        }
    }

    // ── verifyProvider ────────────────────────────────────────────────────

    @Nested
    @DisplayName("verifyProvider()")
    class VerifyProviderTests {

        @Test
        @DisplayName("throws ResourceNotFoundException when not found")
        void shouldThrow_whenNotFound() {
            given(providerRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> providerService.verifyProvider(99L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("throws IllegalStateException when already verified")
        void shouldThrow_whenAlreadyVerified() {
            Provider p = buildProvider(1L, 2L, true); // already verified
            given(providerRepository.findById(1L)).willReturn(Optional.of(p));

            assertThatThrownBy(() -> providerService.verifyProvider(1L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("already verified");
        }

        @Test
        @DisplayName("sets isVerified=true and saves when not yet verified")
        void shouldSetVerifiedTrue() {
            Provider p = buildProvider(1L, 2L, false); // not yet verified
            given(providerRepository.findById(1L)).willReturn(Optional.of(p));

            providerService.verifyProvider(1L);

            assertThat(p.getIsVerified()).isTrue();
            verify(providerRepository).save(p);
        }
    }

    // ── rejectProvider ────────────────────────────────────────────────────

    @Nested
    @DisplayName("rejectProvider()")
    class RejectProviderTests {

        @Test
        @DisplayName("sets isVerified=false and isAvailable=false")
        void shouldSetBothFlagsToFalse() {
            Provider p = buildProvider(1L, 2L, true);
            given(providerRepository.findById(1L)).willReturn(Optional.of(p));

            providerService.rejectProvider(1L);

            assertThat(p.getIsVerified()).isFalse();
            assertThat(p.getIsAvailable()).isFalse();
            verify(providerRepository).save(p);
        }
    }

    // ── setAvailability ───────────────────────────────────────────────────

    @Nested
    @DisplayName("setAvailability()")
    class SetAvailabilityTests {

        @Test
        @DisplayName("sets isAvailable=false correctly")
        void shouldSetAvailabilityFalse() {
            Provider p = buildProvider(1L, 2L, true);
            given(providerRepository.findById(1L)).willReturn(Optional.of(p));

            providerService.setAvailability(1L, false);

            assertThat(p.getIsAvailable()).isFalse();
            verify(providerRepository).save(p);
        }

        @Test
        @DisplayName("sets isAvailable=true correctly")
        void shouldSetAvailabilityTrue() {
            Provider p = buildProvider(1L, 2L, true);
            p.setIsAvailable(false);
            given(providerRepository.findById(1L)).willReturn(Optional.of(p));

            providerService.setAvailability(1L, true);

            assertThat(p.getIsAvailable()).isTrue();
        }
    }

    // ── deleteProvider ────────────────────────────────────────────────────

    @Nested
    @DisplayName("deleteProvider()")
    class DeleteProviderTests {

        @Test
        @DisplayName("throws ResourceNotFoundException when not found")
        void shouldThrow_whenNotFound() {
            given(providerRepository.findById(404L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> providerService.deleteProvider(404L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("calls repository.delete() for valid providerId")
        void shouldCallDelete_forValidId() {
            Provider p = buildProvider(1L, 2L, false);
            given(providerRepository.findById(1L)).willReturn(Optional.of(p));

            providerService.deleteProvider(1L);

            verify(providerRepository).delete(p);
        }
    }

    // ── updateRating ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateRating()")
    class UpdateRatingTests {

        @Test
        @DisplayName("throws IllegalArgumentException when rating > 5")
        void shouldThrow_whenRatingAbove5() {
            Provider p = buildProvider(1L, 2L, true);
            given(providerRepository.findById(1L)).willReturn(Optional.of(p));

            assertThatThrownBy(() -> providerService.updateRating(1L, 5.1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Rating must be between 0 and 5");
        }

        @Test
        @DisplayName("throws IllegalArgumentException when rating < 0")
        void shouldThrow_whenRatingBelow0() {
            Provider p = buildProvider(1L, 2L, true);
            given(providerRepository.findById(1L)).willReturn(Optional.of(p));

            assertThatThrownBy(() -> providerService.updateRating(1L, -0.1))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("updates avgRating correctly for valid value")
        void shouldUpdateRating_forValidValue() {
            Provider p = buildProvider(1L, 2L, true);
            given(providerRepository.findById(1L)).willReturn(Optional.of(p));

            providerService.updateRating(1L, 4.3);

            assertThat(p.getAvgRating()).isEqualTo(4.3);
            verify(providerRepository).save(p);
        }

        @Test
        @DisplayName("accepts boundary values 0.0 and 5.0")
        void shouldAcceptBoundaryValues() {
            Provider p1 = buildProvider(1L, 2L, true);
            given(providerRepository.findById(1L)).willReturn(Optional.of(p1));
            assertThatCode(() -> providerService.updateRating(1L, 0.0))
                    .doesNotThrowAnyException();

            Provider p2 = buildProvider(2L, 3L, true);
            given(providerRepository.findById(2L)).willReturn(Optional.of(p2));
            assertThatCode(() -> providerService.updateRating(2L, 5.0))
                    .doesNotThrowAnyException();
        }
    }

    // ── getAllProviders ───────────────────────────────────────────────────

    @Nested
    @DisplayName("getAllProviders()")
    class GetAllTests {

        @Test
        @DisplayName("returns all providers from repository")
        void shouldReturnAll() {
            given(providerRepository.findAll()).willReturn(List.of(
                    buildProvider(1L, 2L, true),
                    buildProvider(2L, 3L, false)));

            List<ProviderResponse> result = providerService.getAllProviders();

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("returns empty list when no providers exist")
        void shouldReturnEmpty_whenNoneExist() {
            given(providerRepository.findAll()).willReturn(List.of());

            assertThat(providerService.getAllProviders()).isEmpty();
        }
    }

    // ── getVerifiedProviders ──────────────────────────────────────────────

    @Nested
    @DisplayName("getVerifiedProviders()")
    class GetVerifiedTests {

        @Test
        @DisplayName("returns only verified providers")
        void shouldReturnOnlyVerified() {
            given(providerRepository.findByIsVerified(true))
                    .willReturn(List.of(buildProvider(1L, 2L, true)));

            List<ProviderResponse> result = providerService.getVerifiedProviders();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getIsVerified()).isTrue();
        }
    }

    // ── countBySpecialization ─────────────────────────────────────────────

    @Nested
    @DisplayName("countBySpecialization()")
    class CountTests {

        @Test
        @DisplayName("returns count from repository")
        void shouldReturnCount() {
            given(providerRepository.countBySpecialization("Cardiology")).willReturn(5);

            int count = providerService.countBySpecialization("Cardiology");

            assertThat(count).isEqualTo(5);
        }

        @Test
        @DisplayName("returns 0 when none found")
        void shouldReturnZero_whenNoneFound() {
            given(providerRepository.countBySpecialization("Neurology")).willReturn(0);

            assertThat(providerService.countBySpecialization("Neurology")).isEqualTo(0);
        }
    }
}
```

---

### ProviderResourceTest.java

```java
package com.medibook.provider.resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medibook.provider.config.GatewayJwtAuthenticationFilter;
import com.medibook.provider.config.SecurityConfig;
import com.medibook.provider.dto.request.ProviderRegistrationRequest;
import com.medibook.provider.dto.request.UpdateProviderRequest;
import com.medibook.provider.dto.response.ProviderResponse;
import com.medibook.provider.exception.ResourceNotFoundException;
import com.medibook.provider.service.ProviderService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProviderResource.class)
@Import({SecurityConfig.class, GatewayJwtAuthenticationFilter.class})
@DisplayName("ProviderResource — MockMvc Tests")
class ProviderResourceTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean ProviderService providerService;
    @MockBean com.medibook.provider.util.JwtUtil jwtUtil;

    private static final String BASE = "/api/v1/providers";

    private ProviderResponse sampleResponse(Long id, Long userId) {
        return ProviderResponse.builder()
                .providerId(id)
                .userId(userId)
                .specialization("Cardiology")
                .qualification("MBBS, MD")
                .experienceYears(10)
                .bio("Specialist")
                .clinicName("HeartCare")
                .clinicAddress("Indore, MP")
                .avgRating(0.0)
                .isVerified(false)
                .isAvailable(true)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ── GET /providers (public) ───────────────────────────────────────────

    @Test
    @DisplayName("GET /providers — 200 OK without auth (public endpoint)")
    void getAllProviders_shouldReturn200_withoutAuth() throws Exception {
        given(providerService.getAllProviders()).willReturn(List.of(sampleResponse(1L, 2L)));

        mockMvc.perform(get(BASE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].providerId").value(1))
                .andExpect(jsonPath("$[0].specialization").value("Cardiology"));
    }

    @Test
    @DisplayName("GET /providers/verified — 200 OK without auth")
    void getVerified_shouldReturn200_withoutAuth() throws Exception {
        given(providerService.getVerifiedProviders()).willReturn(List.of());

        mockMvc.perform(get(BASE + "/verified"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /providers/{id} — 200 OK without auth")
    void getById_shouldReturn200_withoutAuth() throws Exception {
        given(providerService.getProviderById(1L)).willReturn(sampleResponse(1L, 2L));

        mockMvc.perform(get(BASE + "/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.providerId").value(1));
    }

    @Test
    @DisplayName("GET /providers/{id} — 404 when not found")
    void getById_shouldReturn404_whenNotFound() throws Exception {
        given(providerService.getProviderById(99L))
                .willThrow(new ResourceNotFoundException("Provider not found with id: 99"));

        mockMvc.perform(get(BASE + "/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Provider not found with id: 99"));
    }

    @Test
    @DisplayName("GET /providers/user/{userId} — 200 OK without auth")
    void getByUserId_shouldReturn200() throws Exception {
        given(providerService.getProviderByUserId(2L)).willReturn(sampleResponse(1L, 2L));

        mockMvc.perform(get(BASE + "/user/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(2));
    }

    @Test
    @DisplayName("GET /providers/search?q=heart — 200 OK without auth")
    void search_shouldReturn200_withoutAuth() throws Exception {
        given(providerService.searchProviders("heart"))
                .willReturn(List.of(sampleResponse(1L, 2L)));

        mockMvc.perform(get(BASE + "/search").param("q", "heart"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].clinicName").value("HeartCare"));
    }

    @Test
    @DisplayName("GET /providers/filter — 200 OK with all params")
    void filter_shouldReturn200() throws Exception {
        given(providerService.filterProviders("Cardiology", "Indore", 4.0))
                .willReturn(List.of(sampleResponse(1L, 2L)));

        mockMvc.perform(get(BASE + "/filter")
                        .param("specialization", "Cardiology")
                        .param("location", "Indore")
                        .param("minRating", "4.0"))
                .andExpect(status().isOk());
    }

    // ── POST /providers (requires PROVIDER or ADMIN) ──────────────────────

    @Test
    @WithMockUser(roles = {"PROVIDER"})
    @DisplayName("POST /providers — 201 Created for PROVIDER role")
    void register_shouldReturn201_forProvider() throws Exception {
        ProviderRegistrationRequest req = new ProviderRegistrationRequest();
        req.setUserId(2L);
        req.setSpecialization("Cardiology");
        req.setQualification("MBBS, MD");

        given(providerService.registerProvider(any())).willReturn(sampleResponse(1L, 2L));

        mockMvc.perform(post(BASE)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.providerId").value(1));
    }

    @Test
    @DisplayName("POST /providers — 401 Unauthorized when no token")
    void register_shouldReturn401_whenNoAuth() throws Exception {
        mockMvc.perform(post(BASE)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = {"PATIENT"})
    @DisplayName("POST /providers — 403 Forbidden for PATIENT role")
    void register_shouldReturn403_forPatient() throws Exception {
        mockMvc.perform(post(BASE)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":2,\"specialization\":\"Cardiology\",\"qualification\":\"MBBS\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"PROVIDER"})
    @DisplayName("POST /providers — 400 when userId already has profile")
    void register_shouldReturn400_whenDuplicate() throws Exception {
        ProviderRegistrationRequest req = new ProviderRegistrationRequest();
        req.setUserId(2L);
        req.setSpecialization("Cardiology");
        req.setQualification("MBBS");

        given(providerService.registerProvider(any()))
                .willThrow(new IllegalArgumentException("Provider profile already exists for userId: 2"));

        mockMvc.perform(post(BASE)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Provider profile already exists for userId: 2"));
    }

    // ── PUT /providers/{id} ────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = {"PROVIDER"})
    @DisplayName("PUT /providers/{id} — 200 OK on successful update")
    void update_shouldReturn200() throws Exception {
        UpdateProviderRequest req = new UpdateProviderRequest();
        req.setBio("Updated bio");

        given(providerService.updateProvider(eq(1L), any())).willReturn(sampleResponse(1L, 2L));

        mockMvc.perform(put(BASE + "/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    // ── PUT /providers/{id}/verify (ADMIN only) ───────────────────────────

    @Test
    @WithMockUser(roles = {"ADMIN"})
    @DisplayName("PUT /verify — 200 OK for ADMIN role")
    void verify_shouldReturn200_forAdmin() throws Exception {
        doNothing().when(providerService).verifyProvider(1L);

        mockMvc.perform(put(BASE + "/1/verify").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message")
                        .value("Provider 1 has been verified successfully."));
    }

    @Test
    @WithMockUser(roles = {"PROVIDER"})
    @DisplayName("PUT /verify — 403 Forbidden for non-ADMIN")
    void verify_shouldReturn403_forProvider() throws Exception {
        mockMvc.perform(put(BASE + "/1/verify").with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    @DisplayName("PUT /verify — 409 Conflict when already verified")
    void verify_shouldReturn409_whenAlreadyVerified() throws Exception {
        doThrow(new IllegalStateException("Provider is already verified: 1"))
                .when(providerService).verifyProvider(1L);

        mockMvc.perform(put(BASE + "/1/verify").with(csrf()))
                .andExpect(status().isConflict());
    }

    // ── PUT /providers/{id}/reject (ADMIN only) ───────────────────────────

    @Test
    @WithMockUser(roles = {"ADMIN"})
    @DisplayName("PUT /reject — 200 OK for ADMIN")
    void reject_shouldReturn200_forAdmin() throws Exception {
        doNothing().when(providerService).rejectProvider(1L);

        mockMvc.perform(put(BASE + "/1/reject").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Provider 1 has been rejected."));
    }

    // ── PUT /providers/{id}/availability ──────────────────────────────────

    @Test
    @WithMockUser(roles = {"PROVIDER"})
    @DisplayName("PUT /availability — 200 OK for PROVIDER role")
    void setAvailability_shouldReturn200() throws Exception {
        doNothing().when(providerService).setAvailability(1L, false);

        mockMvc.perform(put(BASE + "/1/availability")
                        .with(csrf())
                        .param("available", "false"))
                .andExpect(status().isOk());
    }

    // ── PUT /providers/{id}/rating ────────────────────────────────────────

    @Test
    @WithMockUser(roles = {"PROVIDER"})
    @DisplayName("PUT /rating — 200 OK with valid rating")
    void updateRating_shouldReturn200() throws Exception {
        doNothing().when(providerService).updateRating(1L, 4.5);

        mockMvc.perform(put(BASE + "/1/rating")
                        .with(csrf())
                        .param("rating", "4.5"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = {"PROVIDER"})
    @DisplayName("PUT /rating — 400 when rating is out of bounds")
    void updateRating_shouldReturn400_forOutOfBoundsRating() throws Exception {
        doThrow(new IllegalArgumentException("Rating must be between 0 and 5"))
                .when(providerService).updateRating(1L, 6.0);

        mockMvc.perform(put(BASE + "/1/rating")
                        .with(csrf())
                        .param("rating", "6.0"))
                .andExpect(status().isBadRequest());
    }

    // ── DELETE /providers/{id} (ADMIN only) ───────────────────────────────

    @Test
    @WithMockUser(roles = {"ADMIN"})
    @DisplayName("DELETE /providers/{id} — 200 OK for ADMIN")
    void delete_shouldReturn200_forAdmin() throws Exception {
        doNothing().when(providerService).deleteProvider(1L);

        mockMvc.perform(delete(BASE + "/1").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Provider 1 deleted successfully."));
    }

    @Test
    @WithMockUser(roles = {"PROVIDER"})
    @DisplayName("DELETE /providers/{id} — 403 Forbidden for non-ADMIN")
    void delete_shouldReturn403_forNonAdmin() throws Exception {
        mockMvc.perform(delete(BASE + "/1").with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE /providers/{id} — 401 when no auth")
    void delete_shouldReturn401_whenNoAuth() throws Exception {
        mockMvc.perform(delete(BASE + "/1").with(csrf()))
                .andExpect(status().isUnauthorized());
    }
}
```

---

### GlobalExceptionHandlerTest.java

```java
package com.medibook.provider.exception;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
@DisplayName("GlobalExceptionHandler — Unit Tests")
class GlobalExceptionHandlerTest {

    @InjectMocks private GlobalExceptionHandler handler;

    @Test
    @DisplayName("ResourceNotFoundException → 404 with correct message")
    void shouldReturn404_forResourceNotFoundException() {
        var ex = new ResourceNotFoundException("Provider not found with id: 1");
        var response = handler.handleNotFound(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(404);
        assertThat(response.getBody().message()).isEqualTo("Provider not found with id: 1");
        assertThat(response.getBody().timestamp()).isNotNull();
    }

    @Test
    @DisplayName("IllegalArgumentException → 400 with correct message")
    void shouldReturn400_forIllegalArgumentException() {
        var ex = new IllegalArgumentException("Provider profile already exists for userId: 2");
        var response = handler.handleBadRequest(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().status()).isEqualTo(400);
        assertThat(response.getBody().message())
                .isEqualTo("Provider profile already exists for userId: 2");
    }

    @Test
    @DisplayName("IllegalStateException → 409 with correct message")
    void shouldReturn409_forIllegalStateException() {
        var ex = new IllegalStateException("Provider is already verified: 1");
        var response = handler.handleConflict(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().status()).isEqualTo(409);
        assertThat(response.getBody().message()).isEqualTo("Provider is already verified: 1");
    }

    @Test
    @DisplayName("MethodArgumentNotValidException → 400 with field errors map")
    @SuppressWarnings("unchecked")
    void shouldReturn400_withFieldErrors() {
        FieldError error = new FieldError("req", "specialization", "Specialization is required");
        BindingResult bindingResult = mock(BindingResult.class);
        given(bindingResult.getAllErrors()).willReturn(List.of(error));
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        given(ex.getBindingResult()).willReturn(bindingResult);

        var response = handler.handleValidation(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("status")).isEqualTo(400);
        Map<String, String> errors = (Map<String, String>) response.getBody().get("errors");
        assertThat(errors).containsEntry("specialization", "Specialization is required");
    }

    @Test
    @DisplayName("Multiple field errors all appear in errors map")
    @SuppressWarnings("unchecked")
    void shouldIncludeAllFieldErrors() {
        FieldError e1 = new FieldError("req", "specialization", "required");
        FieldError e2 = new FieldError("req", "qualification", "required");
        BindingResult br = mock(BindingResult.class);
        given(br.getAllErrors()).willReturn(List.of(e1, e2));
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        given(ex.getBindingResult()).willReturn(br);

        var body = handler.handleValidation(ex).getBody();
        Map<String, String> errors = (Map<String, String>) body.get("errors");

        assertThat(errors).hasSize(2);
        assertThat(errors).containsKey("specialization");
        assertThat(errors).containsKey("qualification");
    }
}
```

---

## ❌ Error Responses

All errors return consistent JSON matching the format from `GlobalExceptionHandler`:

```json
{
  "status": 404,
  "message": "Provider not found with id: 99",
  "timestamp": "2026-04-21T10:30:00"
}
```

| HTTP Code | Triggered By |
|-----------|-------------|
| `400 Bad Request` | Validation failure, duplicate userId, rating out of range |
| `401 Unauthorized` | Missing or invalid Authorization header on protected endpoint |
| `403 Forbidden` | Wrong role — e.g. PATIENT calling POST /providers or PROVIDER calling /verify |
| `404 Not Found` | providerId or userId not found in database |
| `409 Conflict` | Trying to verify an already-verified provider |

---

## 📖 Swagger UI

Once running, open:

```
http://localhost:8082/swagger-ui.html
```

All 15 endpoints are listed under the **Providers** tag with full request/response schemas.

```
http://localhost:8082/api-docs          ← OpenAPI JSON spec
http://localhost:8082/actuator/health   ← Service health
http://localhost:8082/actuator/info     ← Service info
http://localhost:8082/actuator/metrics  ← JVM metrics
```

---

## 🔧 Common Issues & Fixes

### Issue 1: 403 Forbidden on POST /providers

You are sending a PATIENT token instead of a PROVIDER token.

**Fix:** Login with a PROVIDER-role account and use that token.

---

### Issue 2: 400 — "Provider profile already exists for userId"

You already called `POST /providers` with this userId.

**Fix:** Each auth-service user can only have one provider profile. Use `GET /providers/user/{userId}` to check if it exists, then use `PUT /providers/{id}` to update it.

---

### Issue 3: Provider not showing in `/filter` results after registration

Newly registered providers have `isVerified = false` by default. The `/filter` endpoint only returns `isVerified = true AND isAvailable = true` providers.

**Fix:** Call `PUT /providers/{providerId}/verify` with an ADMIN token first.

---

### Issue 4: JWT_SECRET not set

```
java.lang.IllegalArgumentException: Could not resolve placeholder 'JWT_SECRET'
```

**Fix:** Set the environment variable before running:
```cmd
set JWT_SECRET=medibook-super-secret-key-must-be-at-least-256-bits-long-change-in-prod
```

---

### Issue 5: Eureka connection refused warnings

```
Cannot execute request on any known server
```

This is normal when Eureka is not running. The service itself starts fine.

**Fix:** Set `EUREKA_ENABLED=false` to silence these warnings during development.

---

### Issue 6: MySQL table not created

```
Table 'provider_db.providers' doesn't exist
```

**Fix:** Make sure `provider_db` database exists and `ddl-auto: update` is set. If the table still isn't created, check that the DB_URL env variable points to `provider_db`, not another database.

---

## 🌿 Git Branch Info

```
Repository  : MediBook-Microservices
Branch      : feature/provider-service
Base Branch : develop
Depends On  : feature/auth-service (userId comes from auth-service)
Merge Target: develop (PR required)
```

**Commit convention:**
```
feat(provider): add provider registration with userId validation
feat(provider): add admin verify/reject endpoints
feat(provider): add advanced filter query for patient search
fix(provider): fix filterProviders to only return verified+available
refactor(provider): extract mapToResponse to private helper
test(provider): add unit tests for ProviderServiceImpl
docs(provider): add README with full API reference
```

---

## 👨‍💻 Developer Notes

- `userId` is intentionally a plain `Long` with no JPA `@ManyToOne`. This is the microservice pattern — no cross-database joins. The provider-service trusts the userId it receives.
- `GatewayJwtAuthenticationFilter` supports both gateway and direct-call scenarios to allow isolated development and testing without needing the full platform running.
- `filterProviders()` is the most important query for patient-facing search — it enforces both `isVerified=true` AND `isAvailable=true` so only active, approved providers appear.
- The `/rating` endpoint has no role restriction by design — it's intended to be called by `review-service` as an internal service-to-service call. In production, add an inter-service API key or restrict to internal network.
- `ddl-auto: update` is appropriate for development. Change to `validate` in production after the schema is stable.

---

### Author👨‍💻

[Harshal Choudhary](https://github.com/Harshal-25C) - Software Developer👨‍💻 | Cloud Enthusiast  
B.Tech - `[Computer Science & Engineering]`  
Java | Spring Boot | Spring Security | JWT | MySQL | Clean Architecture

---

<div align="center">

**MediBook Provider Service** | Part of MediBook Microservices Platform

*Confidential | MediBook Platform | Internal Use Only*

</div>