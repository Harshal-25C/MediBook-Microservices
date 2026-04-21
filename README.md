# MediBook — Schedule Service (UC3)

> **MediBook Microservices** · Feature Branch: `feature/UC3-schedule-service`  
> Part of the MediBook Online Appointment Booking System

---

## Table of Contents

1. [Overview](#overview)
2. [System Architecture](#system-architecture)
3. [Service Port Map](#service-port-map)
4. [Schedule Service — Deep Dive](#schedule-service--deep-dive)
   - [Tech Stack](#tech-stack)
   - [Project Structure](#project-structure)
   - [Entity: AvailabilitySlot](#entity-availabilityslot)
   - [DTOs](#dtos)
   - [Business Logic Highlights](#business-logic-highlights)
   - [Automated Scheduler](#automated-scheduler)
5. [API Gateway](#api-gateway)
   - [JWT Authentication Flow](#jwt-authentication-flow)
   - [Public vs Protected Routes](#public-vs-protected-routes)
   - [Forwarded Headers](#forwarded-headers)
6. [All Services Reference](#all-services-reference)
7. [API Endpoints](#api-endpoints)
8. [API Testing via API Gateway](#api-testing-via-api-gateway)
   - [Prerequisites — Get a JWT Token](#prerequisites--get-a-jwt-token)
   - [1. Add a Single Slot](#1-add-a-single-slot)
   - [2. Add Bulk Slots](#2-add-bulk-slots)
   - [3. Generate Recurring Slots (Daily)](#3-generate-recurring-slots-daily)
   - [4. Generate Recurring Slots (Weekly)](#4-generate-recurring-slots-weekly)
   - [5. Get All Slots for a Provider](#5-get-all-slots-for-a-provider)
   - [6. Get Available Slots for a Provider on a Date](#6-get-available-slots-for-a-provider-on-a-date)
   - [7. Get a Slot by ID](#7-get-a-slot-by-id)
   - [8. Update a Slot](#8-update-a-slot)
   - [9. Block a Slot](#9-block-a-slot)
   - [10. Unblock a Slot](#10-unblock-a-slot)
   - [11. Delete a Slot](#11-delete-a-slot)
   - [12. Book a Slot (Internal)](#12-book-a-slot-internal)
   - [13. Release a Slot (Internal)](#13-release-a-slot-internal)
9. [Error Responses](#error-responses)
10. [Environment Variables](#environment-variables)
11. [Running the Services](#running-the-services)
12. [Database Setup](#database-setup)
13. [Swagger UI](#swagger-ui)

---

## Overview

The **Schedule Service** is the UC3 microservice in MediBook responsible for managing doctor availability slots. It allows healthcare providers (doctors) to create, manage, and control time slots for patient appointments.

Key responsibilities:
- Create single or bulk availability slots for a provider
- Generate recurring slots (daily or weekly patterns)
- Block/unblock slots for personal unavailability
- Serve available slots to patients for appointment booking
- Integrate with `appointment-service` via internal Feign endpoints for slot booking and release
- Auto-purge expired unbooked slots every night at midnight via a scheduled job
- Prevent double-booking using JPA optimistic locking (`@Version`)

---

## System Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                          CLIENT (Browser / App)                  │
└──────────────────────────────┬──────────────────────────────────┘
                               │ HTTP Requests
                               ▼
┌─────────────────────────────────────────────────────────────────┐
│                    API GATEWAY  :8080                            │
│   • Global JWT Authentication Filter                            │
│   • Routes: /auth/**, /providers/**, /slots/**, /appointments/**│
│   • CORS handling (localhost:5173, localhost:5174)               │
└──────────────────────────────┬──────────────────────────────────┘
                               │ lb:// (Eureka Load Balancer)
           ┌───────────────────┼──────────────────────┐
           ▼                   ▼                      ▼
   ┌──────────────┐   ┌──────────────────┐   ┌──────────────────┐
   │ auth-service │   │ provider-service │   │ schedule-service │
   │   :8081      │   │    :8082         │   │    :8083         │
   │  auth_db     │   │  provider_db     │   │  schedule_db     │
   └──────────────┘   └──────────────────┘   └──────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│               EUREKA SERVER  :8761                              │
│   • Service Registry & Discovery                                │
│   • Basic Auth: admin / medibook123                             │
└─────────────────────────────────────────────────────────────────┘
```

---

## Service Port Map

| Service              | Port  | Database       | Notes                          |
|----------------------|-------|----------------|--------------------------------|
| `eureka-server`      | 8761  | —              | Start **first**                |
| `api-gateway`        | 8080  | —              | Start **second**, all traffic goes here |
| `auth-service`       | 8081  | `auth_db`      | JWT issuance, OTP, OAuth2      |
| `provider-service`   | 8082  | `provider_db`  | Doctor profile management      |
| `schedule-service`   | 8083  | `schedule_db`  | **This service — UC3**         |
| `appointment-service`| 8084  | `appointment_db`| UC4 — uses schedule internally |
| `payment-service`    | 8085  | `payment_db`   | UC5                            |
| `review-service`     | 8086  | `review_db`    | UC6                            |
| `notification-service`| 8087 | `notification_db`| UC7                          |
| `record-service`     | 8088  | `record_db`    | UC8                            |

---

## Schedule Service — Deep Dive

### Tech Stack

| Layer       | Technology                                               |
|-------------|----------------------------------------------------------|
| Language    | Java 17                                                  |
| Framework   | Spring Boot 3.2.0                                        |
| Cloud       | Spring Cloud 2023.0.0 (Eureka Client, OpenFeign)         |
| Database    | MySQL 8 — `schedule_db`                                  |
| ORM         | Spring Data JPA / Hibernate                              |
| Security    | Spring Security (stateless, JWT validated at gateway)    |
| Scheduler   | Spring `@Scheduled` (Quartz also on classpath)           |
| Docs        | Springdoc OpenAPI 2.3.0 (Swagger UI)                     |
| Build       | Maven 3, parent POM at root                              |
| Lombok      | 1.18.30                                                  |
| JWT Library | JJWT 0.11.5                                              |

### Project Structure

```
schedule-service/
└── src/main/java/com/medibook/schedule/
    ├── ScheduleServiceApplication.java      # Main entry point
    ├── config/
    │   └── SecurityConfig.java              # Stateless, all routes permitted (gateway validates JWT)
    ├── dto/
    │   └── request/
    │       └── SlotRequest.java             # Request DTO for creating/updating slots
    ├── entity/
    │   └── AvailabilitySlot.java            # JPA entity → availability_slots table
    ├── exception/
    │   ├── BadRequestException.java         # 400 errors
    │   ├── ResourceNotFoundException.java   # 404 errors
    │   ├── DuplicateResourceException.java  # 409 errors
    │   ├── ForbiddenException.java          # 403 errors
    │   ├── UnauthorizedException.java       # 401 errors
    │   ├── ErrorResponse.java               # Standardized error response shape
    │   └── GlobalExceptionHandler.java      # @ControllerAdvice — catches all exceptions
    ├── repository/
    │   └── SlotRepository.java              # JPA repository with custom JPQL queries
    ├── resource/
    │   └── ScheduleResource.java            # REST Controller — /slots/**
    ├── scheduler/
    │   └── SlotExpiryScheduler.java         # Cron job: midnight expiry cleanup
    └── service/
        ├── ScheduleService.java             # Interface (contract)
        └── impl/
            └── ScheduleServiceImpl.java     # Business logic implementation
```

### Entity: AvailabilitySlot

Maps to the `availability_slots` table in `schedule_db`.

| Column           | Type          | Nullable | Default | Description                                         |
|------------------|---------------|----------|---------|-----------------------------------------------------|
| `slotId`         | INT (PK, AI)  | No       | —       | Auto-generated primary key                          |
| `providerId`     | INT           | No       | —       | FK reference to Provider (from UC2)                 |
| `date`           | DATE          | No       | —       | The calendar date of this slot                      |
| `startTime`      | TIME          | No       | —       | Slot start time (e.g., 10:00)                       |
| `endTime`        | TIME          | No       | —       | Slot end time (e.g., 10:30)                         |
| `durationMinutes`| INT           | No       | —       | Appointment duration in minutes                     |
| `isBooked`       | BOOLEAN       | No       | false   | true when a patient has booked this slot            |
| `isBlocked`      | BOOLEAN       | No       | false   | true when doctor has manually blocked this slot     |
| `recurrence`     | VARCHAR       | Yes      | `NONE`  | `NONE` / `DAILY` / `WEEKLY`                         |
| `createdAt`      | DATETIME      | No       | auto    | Set automatically via `@PrePersist`                 |
| `version`        | INT           | No       | 0       | JPA optimistic locking — prevents double booking    |

**Double-Booking Prevention:** The `@Version` field on the entity ensures that if two patients simultaneously attempt to book the same slot, the JPA optimistic lock detects the version conflict and throws an `OptimisticLockException`, allowing only one booking to succeed.

### DTOs

**`SlotRequest`** — used for all create and update operations:

| Field              | Type       | Required | Validation              | Description                                     |
|--------------------|------------|----------|-------------------------|-------------------------------------------------|
| `providerId`       | int        | Yes      | `@NotNull`              | Provider (doctor) ID                            |
| `date`             | LocalDate  | Yes      | `@NotNull`              | Slot date (e.g., `2026-05-10`)                  |
| `startTime`        | LocalTime  | Yes      | `@NotNull`              | Start time (e.g., `10:00`)                      |
| `endTime`          | LocalTime  | Yes      | `@NotNull`              | End time (e.g., `10:30`)                        |
| `durationMinutes`  | int        | Yes      | `@Min(1)`               | Duration in minutes, minimum 1                 |
| `recurrence`       | String     | No       | —                       | `NONE` (default) / `DAILY` / `WEEKLY`           |
| `recurrenceEndDate`| LocalDate  | No       | —                       | Required only when recurrence ≠ `NONE`          |

### Business Logic Highlights

**`addSlot`** — validates the date is not in the past before saving.

**`addBulkSlots`** — loops through a list of `SlotRequest` objects and calls `addSlot()` for each, ensuring individual validation per slot.

**`generateRecurringSlots`** — requires `recurrenceEndDate` to be set and after `date`. Iterates from `date` to `recurrenceEndDate`, incrementing by 1 day (DAILY) or 1 week (WEEKLY), creating a slot at each step.

**`blockSlot`** — sets `isBlocked = true`. A blocked slot is invisible to patients. Cannot block an already-booked slot.

**`unblockSlot`** — sets `isBlocked = false`. The slot immediately reappears in patient searches.

**`bookSlot`** — called internally by `appointment-service`. Validates the slot is not already booked or blocked, then sets `isBooked = true`. The `@Transactional` annotation combined with `@Version` ensures race-condition safety.

**`releaseSlot`** — called internally by `appointment-service` on appointment cancellation. Sets `isBooked = false`.

**`deleteSlot`** — prevents deletion of any slot that has been booked by a patient.

**`updateSlot`** — prevents updating a slot that is already booked. Also validates the new date is not in the past.

**`deleteExpiredSlots`** — finds all slots where `date < today` AND `isBooked = false` and deletes them all in one pass.

### Automated Scheduler

```
SlotExpiryScheduler.java
  @Scheduled(cron = "0 0 0 * * *")
  → Fires at 00:00:00 every day (midnight)
  → Calls scheduleService.deleteExpiredSlots()
  → Logs success/failure but does NOT crash on error
  → Next midnight it tries again automatically
```

Enabled by `@EnableScheduling` on `ScheduleServiceApplication`.

---

## API Gateway

- **Port:** `8080`
- **Technology:** Spring Cloud Gateway (WebFlux / reactive)
- **Service Discovery:** Eureka (`lb://` prefix for load-balanced routing)

### JWT Authentication Flow

```
1. Client sends request with header: Authorization: Bearer <token>
2. JwtAuthenticationFilter intercepts ALL requests at order -1
3. If path is PUBLIC → forward immediately (no token needed)
4. Otherwise → validate JWT using shared JWT_SECRET
5. On success → extract claims, add forwarded headers:
      X-User-Id    → userId from token claims
      X-User-Role  → role from token claims
      X-User-Email → subject (email) from token
6. On failure → return 401 Unauthorized immediately
```

The downstream services (including schedule-service) trust the forwarded `X-User-*` headers because all traffic must pass through the gateway.

### Public vs Protected Routes

**Public (no token required):**

| Method | Path                              | Description                        |
|--------|-----------------------------------|------------------------------------|
| POST   | `/auth/register`                  | Patient self-registration          |
| POST   | `/auth/login`                     | Email/password login               |
| POST   | `/auth/verify-otp`                | OTP verification after registration|
| POST   | `/auth/resend-otp`                | Resend OTP                         |
| POST   | `/auth/add-phone`                 | Add phone number                   |
| POST   | `/auth/forgot-password`           | Initiate password reset            |
| POST   | `/auth/verify-reset-otp`          | Verify OTP for password reset      |
| POST   | `/auth/reset-password`            | Set new password                   |
| POST   | `/auth/admin/register`            | Admin registration                 |
| POST   | `/auth/google/complete`           | Complete Google OAuth2 profile     |
| POST   | `/auth/refresh`                   | Refresh JWT token                  |
| GET    | `/providers/**`                   | Browse doctor profiles             |
| GET    | `/slots/available/**`             | View available slots (patients)    |
| ANY    | `/oauth2/**`                      | Google OAuth2 redirect             |
| ANY    | `/login/oauth2/**`                | OAuth2 callback                    |

**Protected (Bearer token required):**

| Path Prefix         | Typical Callers          |
|---------------------|--------------------------|
| `/slots/**`         | Doctors, Admins          |
| `/appointments/**`  | Patients, Doctors        |
| `/payments/**`      | Patients                 |
| `/reviews/**`       | Patients                 |
| `/records/**`       | Doctors, Admins          |
| `/notifications/**` | All authenticated users  |

### Forwarded Headers

The gateway strips no headers and adds these after JWT validation:

| Header         | Value                       |
|----------------|-----------------------------|
| `X-User-Id`    | Numeric user ID from claims |
| `X-User-Role`  | `PATIENT` / `DOCTOR` / `ADMIN` |
| `X-User-Email` | Email address (JWT subject) |

---

## All Services Reference

### Auth Service (port 8081)

Handles user registration, login, OTP verification, JWT issuance, password reset, and Google OAuth2.

- Database: `auth_db`
- Key entities: `User`, `OtpToken`, `PasswordResetToken`
- JWT secret must match the secret used by `api-gateway` and all downstream services

### Provider Service (port 8082)

Manages doctor/provider profiles. A user with role `DOCTOR` must create a provider profile before adding availability slots.

- Database: `provider_db`
- Key entity: `Provider`
- Calls `auth-service` via Feign (`UserClient`) to fetch user details

### Schedule Service (port 8083) — **This Service**

See detailed sections above.

### Eureka Server (port 8761)

Netflix Eureka service registry. All microservices register here on startup. The API Gateway uses Eureka for load-balanced routing (`lb://service-name`).

- Basic Auth: `admin` / `medibook123`
- Dashboard: `http://localhost:8761`

---

## API Endpoints

Base URL (via gateway): `http://localhost:8080`  
Base URL (direct): `http://localhost:8083`

All schedule endpoints are prefixed with `/slots`.

| Method | Path                          | Auth     | Description                                    |
|--------|-------------------------------|----------|------------------------------------------------|
| POST   | `/slots/add`                  | Required | Add a single availability slot                 |
| POST   | `/slots/bulk`                 | Required | Add multiple slots at once                     |
| POST   | `/slots/recurring`            | Required | Generate recurring slots (DAILY or WEEKLY)     |
| GET    | `/slots/provider/{providerId}`| Required | Get all slots for a provider (including booked/blocked) |
| GET    | `/slots/available/{providerId}?date=` | Public | Get available (unbooked, unblocked) slots for a date |
| GET    | `/slots/{slotId}`             | Required | Get a single slot by ID                        |
| PUT    | `/slots/{slotId}`             | Required | Update a slot's date/time/duration             |
| PUT    | `/slots/{slotId}/block`       | Required | Block a slot (invisible to patients)           |
| PUT    | `/slots/{slotId}/unblock`     | Required | Unblock a previously blocked slot              |
| DELETE | `/slots/{slotId}`             | Required | Delete a slot (only if not booked)             |
| PUT    | `/slots/{slotId}/book`        | Internal | Mark slot as booked (called by appointment-service) |
| PUT    | `/slots/{slotId}/release`     | Internal | Release a booked slot (called by appointment-service) |

---

## API Testing via API Gateway

All examples below use the API Gateway at `http://localhost:8080`. Replace token and IDs with your actual values.

### Prerequisites — Get a JWT Token

First register and login via the auth service to obtain a Bearer token.

**Register a user:**
```bash
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Dr. Arjun Sharma",
    "email": "arjun.sharma@medibook.com",
    "password": "Doctor@123",
    "phone": "9876543210",
    "role": "DOCTOR"
  }'
```

**Login to get the token:**
```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "arjun.sharma@medibook.com",
    "password": "Doctor@123"
  }'
```

**Sample response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhcmp1bi5zaGFybWFAbWVkaWJvb2suY29tIiwicm9sZSI6IkRPQ1RPUiIsInVzZXJJZCI6MX0...",
  "type": "Bearer",
  "userId": 1,
  "email": "arjun.sharma@medibook.com",
  "role": "DOCTOR"
}
```

Use the returned `token` value as `Bearer <token>` in all protected requests below.

---

### 1. Add a Single Slot

**POST** `/slots/add`

Creates one availability slot for a provider on a specific date and time.

```bash
curl -X POST http://localhost:8080/slots/add \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <YOUR_JWT_TOKEN>" \
  -d '{
    "providerId": 1,
    "date": "2026-05-15",
    "startTime": "10:00",
    "endTime": "10:30",
    "durationMinutes": 30,
    "recurrence": "NONE"
  }'
```

**Expected Response — 201 Created:**
```json
{
  "slotId": 1,
  "providerId": 1,
  "date": "2026-05-15",
  "startTime": "10:00:00",
  "endTime": "10:30:00",
  "durationMinutes": 30,
  "booked": false,
  "blocked": false,
  "recurrence": "NONE",
  "createdAt": "2026-04-22T08:30:00",
  "version": 0
}
```

---

### 2. Add Bulk Slots

**POST** `/slots/bulk`

Creates multiple slots in a single request. Useful when a doctor wants to add several non-recurring slots at once.

```bash
curl -X POST http://localhost:8080/slots/bulk \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <YOUR_JWT_TOKEN>" \
  -d '[
    {
      "providerId": 1,
      "date": "2026-05-15",
      "startTime": "09:00",
      "endTime": "09:30",
      "durationMinutes": 30,
      "recurrence": "NONE"
    },
    {
      "providerId": 1,
      "date": "2026-05-15",
      "startTime": "09:30",
      "endTime": "10:00",
      "durationMinutes": 30,
      "recurrence": "NONE"
    },
    {
      "providerId": 1,
      "date": "2026-05-16",
      "startTime": "11:00",
      "endTime": "11:30",
      "durationMinutes": 30,
      "recurrence": "NONE"
    }
  ]'
```

**Expected Response — 201 Created:**
```json
[
  {
    "slotId": 2,
    "providerId": 1,
    "date": "2026-05-15",
    "startTime": "09:00:00",
    "endTime": "09:30:00",
    "durationMinutes": 30,
    "booked": false,
    "blocked": false,
    "recurrence": "NONE",
    "createdAt": "2026-04-22T08:31:00",
    "version": 0
  },
  {
    "slotId": 3,
    "providerId": 1,
    "date": "2026-05-15",
    "startTime": "09:30:00",
    "endTime": "10:00:00",
    "durationMinutes": 30,
    "booked": false,
    "blocked": false,
    "recurrence": "NONE",
    "createdAt": "2026-04-22T08:31:00",
    "version": 0
  },
  {
    "slotId": 4,
    "providerId": 1,
    "date": "2026-05-16",
    "startTime": "11:00:00",
    "endTime": "11:30:00",
    "durationMinutes": 30,
    "booked": false,
    "blocked": false,
    "recurrence": "NONE",
    "createdAt": "2026-04-22T08:31:00",
    "version": 0
  }
]
```

---

### 3. Generate Recurring Slots (Daily)

**POST** `/slots/recurring`

Doctor wants a 10:00–10:30 slot every day from May 1 to May 7. The system creates 7 individual slots automatically.

```bash
curl -X POST http://localhost:8080/slots/recurring \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <YOUR_JWT_TOKEN>" \
  -d '{
    "providerId": 1,
    "date": "2026-05-01",
    "startTime": "10:00",
    "endTime": "10:30",
    "durationMinutes": 30,
    "recurrence": "DAILY",
    "recurrenceEndDate": "2026-05-07"
  }'
```

**Expected Response — 201 Created:**
```json
[
  { "slotId": 5, "providerId": 1, "date": "2026-05-01", "startTime": "10:00:00", "endTime": "10:30:00", "durationMinutes": 30, "booked": false, "blocked": false, "recurrence": "DAILY", "createdAt": "2026-04-22T08:32:00", "version": 0 },
  { "slotId": 6, "providerId": 1, "date": "2026-05-02", "startTime": "10:00:00", "endTime": "10:30:00", "durationMinutes": 30, "booked": false, "blocked": false, "recurrence": "DAILY", "createdAt": "2026-04-22T08:32:00", "version": 0 },
  { "slotId": 7, "providerId": 1, "date": "2026-05-03", "startTime": "10:00:00", "endTime": "10:30:00", "durationMinutes": 30, "booked": false, "blocked": false, "recurrence": "DAILY", "createdAt": "2026-04-22T08:32:00", "version": 0 },
  { "slotId": 8, "providerId": 1, "date": "2026-05-04", "startTime": "10:00:00", "endTime": "10:30:00", "durationMinutes": 30, "booked": false, "blocked": false, "recurrence": "DAILY", "createdAt": "2026-04-22T08:32:00", "version": 0 },
  { "slotId": 9, "providerId": 1, "date": "2026-05-05", "startTime": "10:00:00", "endTime": "10:30:00", "durationMinutes": 30, "booked": false, "blocked": false, "recurrence": "DAILY", "createdAt": "2026-04-22T08:32:00", "version": 0 },
  { "slotId": 10, "providerId": 1, "date": "2026-05-06", "startTime": "10:00:00", "endTime": "10:30:00", "durationMinutes": 30, "booked": false, "blocked": false, "recurrence": "DAILY", "createdAt": "2026-04-22T08:32:00", "version": 0 },
  { "slotId": 11, "providerId": 1, "date": "2026-05-07", "startTime": "10:00:00", "endTime": "10:30:00", "durationMinutes": 30, "booked": false, "blocked": false, "recurrence": "DAILY", "createdAt": "2026-04-22T08:32:00", "version": 0 }
]
```

---

### 4. Generate Recurring Slots (Weekly)

**POST** `/slots/recurring`

Doctor wants every Monday's 14:00–14:30 slot for the entire month of May (Mondays: May 5, 12, 19, 26).

```bash
curl -X POST http://localhost:8080/slots/recurring \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <YOUR_JWT_TOKEN>" \
  -d '{
    "providerId": 1,
    "date": "2026-05-05",
    "startTime": "14:00",
    "endTime": "14:30",
    "durationMinutes": 30,
    "recurrence": "WEEKLY",
    "recurrenceEndDate": "2026-05-31"
  }'
```

**Expected Response — 201 Created:**
```json
[
  { "slotId": 12, "providerId": 1, "date": "2026-05-05", "startTime": "14:00:00", "endTime": "14:30:00", "durationMinutes": 30, "booked": false, "blocked": false, "recurrence": "WEEKLY", "version": 0 },
  { "slotId": 13, "providerId": 1, "date": "2026-05-12", "startTime": "14:00:00", "endTime": "14:30:00", "durationMinutes": 30, "booked": false, "blocked": false, "recurrence": "WEEKLY", "version": 0 },
  { "slotId": 14, "providerId": 1, "date": "2026-05-19", "startTime": "14:00:00", "endTime": "14:30:00", "durationMinutes": 30, "booked": false, "blocked": false, "recurrence": "WEEKLY", "version": 0 },
  { "slotId": 15, "providerId": 1, "date": "2026-05-26", "startTime": "14:00:00", "endTime": "14:30:00", "durationMinutes": 30, "booked": false, "blocked": false, "recurrence": "WEEKLY", "version": 0 }
]
```

---

### 5. Get All Slots for a Provider

**GET** `/slots/provider/{providerId}`

Returns every slot for the provider — including booked, blocked, and available. Intended for doctor/admin views.

```bash
curl -X GET http://localhost:8080/slots/provider/1 \
  -H "Authorization: Bearer <YOUR_JWT_TOKEN>"
```

**Expected Response — 200 OK:**
```json
[
  {
    "slotId": 1,
    "providerId": 1,
    "date": "2026-05-15",
    "startTime": "10:00:00",
    "endTime": "10:30:00",
    "durationMinutes": 30,
    "booked": false,
    "blocked": false,
    "recurrence": "NONE",
    "createdAt": "2026-04-22T08:30:00",
    "version": 0
  }
]
```

---

### 6. Get Available Slots for a Provider on a Date

**GET** `/slots/available/{providerId}?date=YYYY-MM-DD`

Returns only slots that are **not booked** and **not blocked**. This is the public-facing patient endpoint — no token required.

```bash
curl -X GET "http://localhost:8080/slots/available/1?date=2026-05-15"
```

**Expected Response — 200 OK:**
```json
[
  {
    "slotId": 2,
    "providerId": 1,
    "date": "2026-05-15",
    "startTime": "09:00:00",
    "endTime": "09:30:00",
    "durationMinutes": 30,
    "booked": false,
    "blocked": false,
    "recurrence": "NONE",
    "createdAt": "2026-04-22T08:31:00",
    "version": 0
  },
  {
    "slotId": 3,
    "providerId": 1,
    "date": "2026-05-15",
    "startTime": "09:30:00",
    "endTime": "10:00:00",
    "durationMinutes": 30,
    "booked": false,
    "blocked": false,
    "recurrence": "NONE",
    "createdAt": "2026-04-22T08:31:00",
    "version": 0
  }
]
```

**Note:** If no slots are available for the given date, an empty array `[]` is returned.

---

### 7. Get a Slot by ID

**GET** `/slots/{slotId}`

Fetches the full details of a specific slot by its ID.

```bash
curl -X GET http://localhost:8080/slots/1 \
  -H "Authorization: Bearer <YOUR_JWT_TOKEN>"
```

**Expected Response — 200 OK:**
```json
{
  "slotId": 1,
  "providerId": 1,
  "date": "2026-05-15",
  "startTime": "10:00:00",
  "endTime": "10:30:00",
  "durationMinutes": 30,
  "booked": false,
  "blocked": false,
  "recurrence": "NONE",
  "createdAt": "2026-04-22T08:30:00",
  "version": 0
}
```

---

### 8. Update a Slot

**PUT** `/slots/{slotId}`

Updates the date, time, or duration of an existing slot. Cannot update a slot that has already been booked by a patient, and the new date cannot be in the past.

```bash
curl -X PUT http://localhost:8080/slots/1 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <YOUR_JWT_TOKEN>" \
  -d '{
    "providerId": 1,
    "date": "2026-05-20",
    "startTime": "11:00",
    "endTime": "11:45",
    "durationMinutes": 45,
    "recurrence": "NONE"
  }'
```

**Expected Response — 200 OK:**
```json
{
  "slotId": 1,
  "providerId": 1,
  "date": "2026-05-20",
  "startTime": "11:00:00",
  "endTime": "11:45:00",
  "durationMinutes": 45,
  "booked": false,
  "blocked": false,
  "recurrence": "NONE",
  "createdAt": "2026-04-22T08:30:00",
  "version": 1
}
```

---

### 9. Block a Slot

**PUT** `/slots/{slotId}/block`

Doctor blocks a slot (e.g., due to a personal commitment). Blocked slots are invisible to patients. Cannot block a slot that is already booked.

```bash
curl -X PUT http://localhost:8080/slots/1/block \
  -H "Authorization: Bearer <YOUR_JWT_TOKEN>"
```

**Expected Response — 200 OK:**
```json
{
  "message": "Slot blocked successfully. It is now invisible to patients."
}
```

---

### 10. Unblock a Slot

**PUT** `/slots/{slotId}/unblock`

Doctor unblocks a previously blocked slot. The slot immediately becomes visible and bookable by patients again.

```bash
curl -X PUT http://localhost:8080/slots/1/unblock \
  -H "Authorization: Bearer <YOUR_JWT_TOKEN>"
```

**Expected Response — 200 OK:**
```json
{
  "message": "Slot unblocked successfully. It is now visible and bookable by patients."
}
```

---

### 11. Delete a Slot

**DELETE** `/slots/{slotId}`

Permanently removes a slot from the calendar. Cannot delete a slot that has been booked by a patient — the appointment must be cancelled first.

```bash
curl -X DELETE http://localhost:8080/slots/1 \
  -H "Authorization: Bearer <YOUR_JWT_TOKEN>"
```

**Expected Response — 200 OK:**
```json
{
  "message": "Slot deleted successfully."
}
```

---

### 12. Book a Slot (Internal)

**PUT** `/slots/{slotId}/book`

> ⚠️ **Internal endpoint.** Called exclusively by `appointment-service` via Feign client when a patient creates an appointment. Not intended for direct external use.

```bash
curl -X PUT http://localhost:8080/slots/2/book \
  -H "Authorization: Bearer <YOUR_JWT_TOKEN>"
```

**Expected Response — 200 OK:**
```json
{
  "message": "Slot marked as booked."
}
```

---

### 13. Release a Slot (Internal)

**PUT** `/slots/{slotId}/release`

> ⚠️ **Internal endpoint.** Called exclusively by `appointment-service` via Feign client when a patient cancels an appointment. Restores the slot to available status.

```bash
curl -X PUT http://localhost:8080/slots/2/release \
  -H "Authorization: Bearer <YOUR_JWT_TOKEN>"
```

**Expected Response — 200 OK:**
```json
{
  "message": "Slot released."
}
```

---

## Error Responses

All errors are handled by `GlobalExceptionHandler` and returned as a consistent JSON structure.

**Error Response Shape:**
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Cannot create slot in the past. Please select a future date.",
  "timestamp": "2026-04-22T08:35:00"
}
```

| HTTP Status | Scenario                                                        |
|-------------|------------------------------------------------------------------|
| `400`       | Validation failure, past date, already booked/blocked, empty list |
| `401`       | Missing or invalid JWT token (rejected by gateway)              |
| `403`       | Forbidden — insufficient role                                   |
| `404`       | Slot not found for given `slotId`                               |
| `409`       | Duplicate resource conflict                                     |
| `500`       | Unexpected server error                                         |

**Common error messages:**

| Scenario                             | Message                                                                 |
|--------------------------------------|-------------------------------------------------------------------------|
| Past date on create                  | `"Cannot create slot in the past. Please select a future date."`        |
| Already booked — book attempt        | `"This slot is already booked. Please choose another slot."`            |
| Blocked — book attempt               | `"This slot is blocked by the doctor and cannot be booked."`            |
| Block already-booked slot            | `"Cannot block a slot that is already booked by a patient."`            |
| Already blocked                      | `"Slot is already blocked."`                                            |
| Unblock non-blocked slot             | `"Slot is not blocked."`                                                |
| Update a booked slot                 | `"Cannot update a slot that is already booked by a patient."`           |
| Delete a booked slot                 | `"Cannot delete a slot that is already booked by a patient."`           |
| Missing recurrenceEndDate            | `"Recurrence end date is required for recurring slots."`                |
| End date before start date           | `"Recurrence end date must be after start date."`                       |
| Invalid recurrence pattern           | `"Recurrence pattern must be DAILY or WEEKLY."`                         |
| Slot not found                       | `"Slot not found with id: <slotId>"`                                    |

---

## Environment Variables

These must be set before starting the service. Configure them in your shell, `.env` file, or CI/CD pipeline.

| Variable              | Required | Default (dev)                                                            | Description                                  |
|-----------------------|----------|--------------------------------------------------------------------------|----------------------------------------------|
| `JWT_SECRET`          | **Yes**  | —                                                                        | Must match the secret used in `api-gateway` and `auth-service`. Use a strong Base64 secret (min 256-bit). |
| `DB_URL`              | No       | `jdbc:mysql://localhost:3306/schedule_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true` | MySQL JDBC URL |
| `DB_USERNAME`         | No       | `medibook_user`                                                          | MySQL username                               |
| `DB_PASSWORD`         | No       | `medibook_pass`                                                          | MySQL password                               |
| `EUREKA_DEFAULT_ZONE` | No       | `http://admin:medibook123@localhost:8761/eureka/`                        | Eureka server URL                            |

**Example — exporting in bash:**
```bash
export JWT_SECRET="myVeryStrongBase64SecretKeyForMediBookThatIs256BitsLong"
export DB_URL="jdbc:mysql://localhost:3306/schedule_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true"
export DB_USERNAME="medibook_user"
export DB_PASSWORD="medibook_pass"
```

---

## Running the Services

### Startup Order

Services must be started in this order to ensure proper registration and routing:

```
1. eureka-server      (port 8761) — service registry must be up first
2. api-gateway        (port 8080) — routes depend on Eureka
3. auth-service       (port 8081)
4. provider-service   (port 8082)
5. schedule-service   (port 8083)   ← this service
```

### Build and Run (Maven)

```bash
# From the root of the project
cd MediBook-Microservices-feature-UC3-schedule-service

# Build all modules
mvn clean install -DskipTests

# Start Eureka Server
cd eureka-server
mvn spring-boot:run

# Start API Gateway (new terminal)
cd ../api-gateway
mvn spring-boot:run

# Start Auth Service (new terminal)
cd ../auth-service
JWT_SECRET=<your-secret> mvn spring-boot:run

# Start Provider Service (new terminal)
cd ../provider-service
JWT_SECRET=<your-secret> mvn spring-boot:run

# Start Schedule Service (new terminal)
cd ../schedule-service
JWT_SECRET=<your-secret> mvn spring-boot:run
```

### Build and Run (individual JAR)

```bash
cd schedule-service
mvn clean package -DskipTests

java -jar target/schedule-service-1.0.0.jar \
  --medibook.jwt.secret=<JWT_SECRET> \
  --spring.datasource.url=jdbc:mysql://localhost:3306/schedule_db \
  --spring.datasource.username=medibook_user \
  --spring.datasource.password=medibook_pass
```

---

## Database Setup

Create the MySQL database and user before starting the service.

```sql
-- Create database
CREATE DATABASE IF NOT EXISTS schedule_db;

-- (Repeat for other services)
CREATE DATABASE IF NOT EXISTS auth_db;
CREATE DATABASE IF NOT EXISTS provider_db;

-- Create shared user
CREATE USER IF NOT EXISTS 'medibook_user'@'localhost' IDENTIFIED BY 'medibook_pass';

-- Grant permissions
GRANT ALL PRIVILEGES ON schedule_db.* TO 'medibook_user'@'localhost';
GRANT ALL PRIVILEGES ON auth_db.*     TO 'medibook_user'@'localhost';
GRANT ALL PRIVILEGES ON provider_db.* TO 'medibook_user'@'localhost';

FLUSH PRIVILEGES;
```

Hibernate `ddl-auto: update` will create the `availability_slots` table automatically on first startup.

**Expected table (auto-created by Hibernate):**
```sql
CREATE TABLE availability_slots (
  slot_id          INT AUTO_INCREMENT PRIMARY KEY,
  provider_id      INT NOT NULL,
  date             DATE NOT NULL,
  start_time       TIME NOT NULL,
  end_time         TIME NOT NULL,
  duration_minutes INT NOT NULL,
  is_booked        TINYINT(1) NOT NULL DEFAULT 0,
  is_blocked       TINYINT(1) NOT NULL DEFAULT 0,
  recurrence       VARCHAR(20) DEFAULT 'NONE',
  created_at       DATETIME NOT NULL,
  version          INT NOT NULL DEFAULT 0
);
```

---

## Swagger UI

Each service exposes its own Swagger UI. Access them directly (bypassing the gateway):

| Service           | Swagger URL                                 |
|-------------------|---------------------------------------------|
| schedule-service  | http://localhost:8083/swagger-ui.html       |
| auth-service      | http://localhost:8081/swagger-ui.html       |
| provider-service  | http://localhost:8082/swagger-ui.html       |

OpenAPI JSON docs are available at `/api-docs` on each service.

---

## License

MIT License — see `LICENSE` file in the repository root.

---

### Author👨‍💻

[Harshal Choudhary](https://github.com/Harshal-25C) - Software Developer👨‍💻 | Cloud Enthusiast  
B.Tech - `[Computer Science & Engineering]`  
Java | Spring Boot | Spring Security | JWT | MySQL | Clean Architecture
