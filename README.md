<div align="center">

<img src="https://capsule-render.vercel.app/api?type=waving&color=0:0d4a3a,40:137057,80:1a9e6e,100:0d4a3a&height=220&section=header&text=🏥%20MediBook%20Record%20Service&fontSize=42&fontColor=ffffff&animation=fadeIn&fontAlignY=40&desc=UC8%20·%20Medical%20Records%20·%20Follow-Up%20Scheduler%20·%20Spring%20Boot%203.2&descAlignY=60&descAlign=50" width="100%"/>

<br/>

![Java](https://img.shields.io/badge/Java_17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot_3.2-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL_8-4479A1?style=for-the-badge&logo=mysql&logoColor=white)
![OpenFeign](https://img.shields.io/badge/OpenFeign-3_Clients-6DB33F?style=for-the-badge&logo=spring&logoColor=white)
![Gmail](https://img.shields.io/badge/Gmail_SMTP-EA4335?style=for-the-badge&logo=gmail&logoColor=white)
![Swagger](https://img.shields.io/badge/Swagger_UI-85EA2D?style=for-the-badge&logo=swagger&logoColor=black)

<br/>

![Port](https://img.shields.io/badge/PORT-8088-emerald?style=flat-square&color=059669)
![DB](https://img.shields.io/badge/DB-record__db-emerald?style=flat-square&color=059669)
![UC8](https://img.shields.io/badge/UC8-Record_Service-brightgreen?style=flat-square)
![License](https://img.shields.io/badge/License-MIT-yellow?style=flat-square)
![Scheduler](https://img.shields.io/badge/Scheduler-08:00_AM_Daily-orange?style=flat-square)
![Feign](https://img.shields.io/badge/Feign-3_Service_Clients-blue?style=flat-square)

</div>

---

<img src="https://user-images.githubusercontent.com/73097560/115834477-dbab4500-a447-11eb-908a-139a6edaec5c.gif" width="100%">

## 📋 Table of Contents

- [Overview](#-overview)
- [Complete System Architecture](#-complete-system-architecture)
- [Full Service Port Map](#-full-service-port-map)
- [Record Service Flow](#-record-service-flow)
- [Record Service Deep Dive](#-record-service-deep-dive)
  - [Tech Stack](#tech-stack)
  - [Project Structure](#project-structure)
  - [Entity: MedicalRecord](#entity-medicalrecord)
  - [DTOs](#dtos)
  - [Three Feign Clients](#three-feign-clients)
  - [Follow-Up Reminder Scheduler](#follow-up-reminder-scheduler)
  - [Repository Queries](#repository-queries)
  - [Business Logic Rules](#business-logic-rules)
  - [Access Control Rules](#access-control-rules)
- [API Endpoints Summary](#-api-endpoints-summary)
- [API Testing via Gateway](#-api-testing-via-api-gateway)
- [Error Responses](#-error-responses)
- [Environment Variables](#-environment-variables)
- [Running the Services](#-running-the-services)
- [Database Setup](#-database-setup)
- [Swagger UI](#-swagger-ui)

<img src="https://user-images.githubusercontent.com/73097560/115834477-dbab4500-a447-11eb-908a-139a6edaec5c.gif" width="100%">

## 🏥 Overview

The **Record Service** is the **UC8** microservice in the MediBook Online Appointment Booking System. It is the **final step in the patient care journey** — after a patient's appointment is marked `COMPLETED`, the doctor creates a medical record capturing the diagnosis, prescription, clinical notes, and a follow-up date.

```
Appointment COMPLETED (UC4)
        │
        ▼
Doctor creates MedicalRecord
  → Feign → appointment-service (verify COMPLETED status)
  → Saves: diagnosis, prescription, notes, attachmentUrl, followUpDate
        │
        ▼
Every morning 8:00 AM — FollowUpReminderScheduler
  → Finds all records where followUpDate = today
  → Feign → auth-service (get patient name + email)
  → Feign → notification-service (send EMAIL reminder)
```

**Key responsibilities:**

- Create medical records **only for COMPLETED appointments** (enforced via Feign call)
- Store diagnosis, prescription, clinical notes, S3 attachment URLs, follow-up dates
- Enforce **one record per appointment** (unique constraint on `appointmentId`)
- Provide patient and doctor views of records with proper access segmentation
- Attach document URLs (lab reports, X-rays) to existing records
- **`FollowUpReminderScheduler`** — runs daily at 08:00 AM, sends email reminders via `notification-service` Feign

<img src="https://user-images.githubusercontent.com/73097560/115834477-dbab4500-a447-11eb-908a-139a6edaec5c.gif" width="100%">

## 🏗️ Complete System Architecture

```
╔══════════════════════════════════════════════════════════════════════════════╗
║                         CLIENT (Browser / Mobile App)                       ║
╚════════════════════════════════════╦════════════════════════════════════════╝
                                     ║ All HTTP traffic
                                     ▼
╔══════════════════════════════════════════════════════════════════════════════╗
║                       API GATEWAY  :8080                                    ║
║   • JwtAuthenticationFilter (Global)                                        ║
║   • Forwards: X-User-Id · X-User-Role · X-User-Email                       ║
║   • CORS: localhost:5173, localhost:5174                                     ║
║   Routes:                                                                   ║
║     /auth/**          → auth-service          :8081                         ║
║     /providers/**     → provider-service      :8082                         ║
║     /slots/**         → schedule-service      :8083                         ║
║     /appointments/**  → appointment-service   :8084                         ║
║     /payments/**      → payment-service       :8085                         ║
║     /reviews/**       → review-service        :8086                         ║
║     /notifications/** → notification-service  :8087                         ║
║     /records/**       → record-service        :8088  ◄── THIS SERVICE       ║
╚════════════════════════════════════╦════════════════════════════════════════╝
                                     ║ Eureka lb://
   ┌─────────────┬───────────────────┼──────────────────────────┐
   ▼             ▼                   ▼                          ▼
┌──────────┐ ┌────────────────┐ ┌────────────────┐  ┌───────────────────┐
│auth-svc  │ │appointment-svc │ │notification-svc│  │  record-service   │
│ :8081    │ │    :8084       │ │    :8087       │  │     :8088         │
│ auth_db  │ │ appointment_db │ │notification_db │  │    record_db      │
└────┬─────┘ └───────┬────────┘ └───────┬────────┘  └────────┬──────────┘
     │               │                  │                     │
     │◄──── Feign ───┤                  │◄─── Feign ──────────┤
     │  GET /auth/   │                  │  POST /notifications/│
     │  profile/{id} │                  │  send               │
     │               │                  │                     │
     │◄──────────────┼──── Feign ───────┘                     │
     │           GET /appointments/{id}                       │
     │           (verify COMPLETED status)                    │
     │                                                        │
     │◄───────────────────── Feign (UserClient) ──────────────┘
         GET /auth/profile/{userId}
         (get patient name for scheduler)

╔══════════════════════════════════════════════════════════════════════════════╗
║                       EUREKA SERVER  :8761                                  ║
║             Service Registry  ·  admin / medibook123                        ║
╚══════════════════════════════════════════════════════════════════════════════╝

 ┌──────────────────────────────────────────────────────────────────┐
 │           FollowUpReminderScheduler  @Scheduled(cron = "0 0 8 * * *")      │
 │           Every day at 08:00 AM                                  │
 │                                                                  │
 │  1. RecordService.getFollowUpRecords(today)  [local call]        │
 │  2. For each record with followUpDate = today:                   │
 │     a. UserClient → auth-service  GET /auth/profile/{patientId} │
 │     b. NotificationClient → notification-service                │
 │           POST /notifications/send  (channel = EMAIL)            │
 └──────────────────────────────────────────────────────────────────┘
```

<img src="https://user-images.githubusercontent.com/73097560/115834477-dbab4500-a447-11eb-908a-139a6edaec5c.gif" width="100%">

## 🗺️ Full Service Port Map

| Service | Port | Database | Role |
|---|---|---|---|
| `eureka-server` | **8761** | — | Service registry — Start **first** |
| `api-gateway` | **8080** | — | Single entry point — Start **second** |
| `auth-service` | **8081** | `auth_db` | JWT, OTP, OAuth2, user profiles |
| `provider-service` | **8082** | `provider_db` | Doctor profiles (UC2) |
| `schedule-service` | **8083** | `schedule_db` | Availability slots (UC3) |
| `appointment-service` | **8084** | `appointment_db` | Appointments (UC4) — record-service calls this |
| `payment-service` | **8085** | `payment_db` | Payments (UC5) |
| `review-service` | **8086** | `review_db` | Reviews (UC6) |
| `notification-service` | **8087** | `notification_db` | Notifications (UC7) — record-service calls this |
| `record-service` | **8088** | `record_db` | **← This service (UC8)** |

<img src="https://user-images.githubusercontent.com/73097560/115834477-dbab4500-a447-11eb-908a-139a6edaec5c.gif" width="100%">

## 🔄 Record Service Flow

### Primary Flow — Creating a Medical Record

```
                    ┌────────────────────────┐
                    │   Doctor Dashboard     │
                    │  Appointment = COMPLETED│
                    └───────────┬────────────┘
                                │ POST /records/create
                                ▼
                    ┌────────────────────────┐
                    │  RecordResource.java   │
                    │  createRecord()        │
                    └───────────┬────────────┘
                                │
                    ┌───────────▼────────────┐
                    │ RecordServiceImpl.java │
                    │                        │
                    │ 1. Feign call          │
                    │    → appointment-svc   │
                    │    GET /appointments/  │
                    │    {appointmentId}     │
                    │                        │
                    │ 2. Verify status       │
                    │    == "COMPLETED"      │
                    │    (400 if not)        │
                    │                        │
                    │ 3. Check no duplicate  │
                    │    (409 if exists)     │
                    │                        │
                    │ 4. Validate diagnosis  │
                    │    not empty           │
                    │                        │
                    │ 5. Validate followUp   │
                    │    not in past         │
                    │                        │
                    │ 6. Save MedicalRecord  │
                    │    → record_db         │
                    └───────────┬────────────┘
                                │
                    ┌───────────▼────────────┐
                    │  201 Created           │
                    │  MedicalRecord JSON    │
                    └────────────────────────┘
```

### Follow-Up Reminder Flow (Automated Daily)

```
  08:00 AM every day
         │
  @Scheduled(cron = "0 0 8 * * *")
         │
  FollowUpReminderScheduler.sendFollowUpReminders()
         │
         ├─── recordService.getFollowUpRecords(today)   [local]
         │           │
         │           └── RecordRepository.findByFollowUpDate(today)
         │
         ▼
  For each MedicalRecord where followUpDate = today:
         │
         ├─── UserClient Feign → auth-service
         │    GET /auth/profile/{patientId}
         │    → gets patient.fullName, patient.email
         │
         └─── NotificationClient Feign → notification-service
              POST /notifications/send
              {
                recipientId: patientId,
                type: "FOLLOWUP",
                title: "Follow-Up Reminder 🏥",
                message: "Dear {name}, today is your scheduled follow-up...",
                channel: "EMAIL",
                relatedId: recordId,
                relatedType: "RECORD"
              }
              → notification-service sends Gmail email to patient
```

<img src="https://user-images.githubusercontent.com/73097560/115834477-dbab4500-a447-11eb-908a-139a6edaec5c.gif" width="100%">

## 🔬 Record Service Deep Dive

### Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.2.0 |
| Cloud | Spring Cloud 2023.0.0 (Eureka Client, OpenFeign) |
| Database | MySQL 8 — `record_db` |
| ORM | Spring Data JPA / Hibernate |
| Feign Clients | 3 clients: `AppointmentClient`, `UserClient`, `NotificationClient` |
| Email | Spring Boot Mail + JavaMailSender (Gmail SMTP, used by scheduler) |
| Security | Spring Security (stateless, all routes permitAll — JWT at gateway) |
| Scheduler | Spring `@EnableScheduling` + `@Scheduled` (daily 08:00 AM) |
| Docs | Springdoc OpenAPI 2.3.0 (Swagger UI) |
| Build | Maven 3 |
| Lombok | 1.18.30 |
| JWT | JJWT 0.11.5 |

### Project Structure

```
record-service/
└── src/main/java/com/medibook/record/
    ├── RecordServiceApplication.java        # Main — @EnableScheduling, @EnableFeignClients
    ├── client/
    │   ├── AppointmentClient.java           # Feign → appointment-service (verify COMPLETED)
    │   ├── NotificationClient.java          # Feign → notification-service (send email reminder)
    │   └── UserClient.java                  # Feign → auth-service (get patient profile)
    ├── config/
    │   └── SecurityConfig.java              # Stateless, permitAll (JWT validated at gateway)
    ├── dto/
    │   ├── AppointmentDto.java              # Feign response: appointmentId, patientId, providerId, status
    │   ├── NotificationDto.java             # Feign request to notification-service
    │   ├── RecordRequest.java               # REST API request DTO (with validation)
    │   └── UserDto.java                     # Feign response: userId, fullName, email, phone, role
    ├── entity/
    │   └── MedicalRecord.java               # JPA entity → medical_records table (unique on appointmentId)
    ├── exception/
    │   ├── BadRequestException.java         # 400
    │   ├── DuplicateResourceException.java  # 409 — duplicate appointmentId
    │   ├── ResourceNotFoundException.java   # 404
    │   ├── ForbiddenException.java          # 403
    │   ├── UnauthorizedException.java       # 401
    │   ├── ErrorResponse.java               # Standardized error body
    │   └── GlobalExceptionHandler.java      # @ControllerAdvice catches all
    ├── repository/
    │   └── RecordRepository.java            # JPA repo with 9 custom queries
    ├── resource/
    │   └── RecordResource.java              # REST controller — /records/**
    ├── scheduler/
    │   └── FollowUpReminderScheduler.java   # Cron 08:00 AM daily — send email follow-up reminders
    └── service/
        ├── RecordService.java               # Interface contract (11 methods)
        └── impl/
            └── RecordServiceImpl.java       # Business logic implementation
```

### Entity: MedicalRecord

Maps to the `medical_records` table in `record_db`.

| Column | Type | Nullable | Constraint | Description |
|---|---|---|---|---|
| `recordId` | INT (PK, AI) | No | — | Auto-generated primary key |
| `appointmentId` | INT | No | **UNIQUE** | One record per appointment — enforced by DB unique constraint |
| `patientId` | INT | No | — | Patient who received treatment (from UC1) |
| `providerId` | INT | No | — | Doctor who created this record (from UC2) |
| `diagnosis` | VARCHAR | No | — | Primary medical finding e.g. "Viral chest infection" |
| `prescription` | VARCHAR | Yes | — | Full medicine prescription text |
| `notes` | TEXT | Yes | — | Extra clinical observations |
| `attachmentUrl` | VARCHAR | Yes | — | S3 URL of lab report / X-ray document |
| `followUpDate` | DATE | Yes | — | When patient should revisit — triggers email reminder |
| `createdAt` | DATETIME | No | `updatable=false` | Set via `@PrePersist` — HIPAA audit trail |
| `updatedAt` | DATETIME | Yes | — | Updated via `@PreUpdate` — tracks doctor edits |

> **Key Design:** `appointmentId` has a `unique = true` DB constraint. This means `DuplicateResourceException` (409) is thrown if a doctor attempts to create a second record for the same appointment.

### DTOs

**`RecordRequest`** — doctor's medical record creation / update form:

| Field | Type | Required | Validation | Description |
|---|---|---|---|---|
| `appointmentId` | int | Yes | `@NotNull` | Links record to the completed appointment |
| `patientId` | int | Yes | `@NotNull` | Patient receiving the record |
| `providerId` | int | Yes | `@NotNull` | Doctor creating the record |
| `diagnosis` | String | Yes | `@NotBlank` | Primary clinical diagnosis |
| `prescription` | String | No | — | Full medicine prescription |
| `notes` | String | No | — | Additional clinical observations |
| `attachmentUrl` | String | No | — | S3/URL of attached lab report or scan |
| `followUpDate` | LocalDate | No | — | Must not be in the past if set |

**`AppointmentDto`** — Feign response from `appointment-service`:

| Field | Description |
|---|---|
| `appointmentId` | The appointment ID |
| `patientId` | Patient involved |
| `providerId` | Doctor involved |
| `status` | Must be `COMPLETED` for record creation to proceed |

**`NotificationDto`** — Feign request sent to `notification-service`:

| Field | Value in Scheduler |
|---|---|
| `recipientId` | `record.getPatientId()` |
| `type` | `"FOLLOWUP"` |
| `title` | `"Follow-Up Reminder 🏥"` |
| `message` | `"Dear {name}, today is your scheduled follow-up..."` |
| `channel` | `"EMAIL"` |
| `relatedId` | `record.getRecordId()` |
| `relatedType` | `"RECORD"` |

**`UserDto`** — Feign response from `auth-service`:

| Field | Description |
|---|---|
| `userId` | User ID |
| `fullName` | Patient's full name (used in email body) |
| `email` | For notification delivery |
| `phone` | Phone number |
| `role` | `PATIENT` / `DOCTOR` / `ADMIN` |

### Three Feign Clients

| Client | Service | Endpoint | When Called |
|---|---|---|---|
| `AppointmentClient` | `appointment-service` | `GET /appointments/{appointmentId}` | `createRecord()` — validates appointment is COMPLETED before allowing record creation |
| `UserClient` | `auth-service` | `GET /auth/profile/{userId}` | `FollowUpReminderScheduler` — gets patient name and email for the reminder message |
| `NotificationClient` | `notification-service` | `POST /notifications/send` | `FollowUpReminderScheduler` — triggers EMAIL notification delivery |

### Follow-Up Reminder Scheduler

```java
@Scheduled(cron = "0 0 8 * * *")  // 08:00:00 every morning
public void sendFollowUpReminders()
```

**Cron breakdown:** `second=0, minute=0, hour=8, day=*, month=*, weekday=*`

**Full execution logic:**
1. `recordService.getFollowUpRecords(LocalDate.now())` — finds all `MedicalRecord` where `followUpDate = today`
2. For each record found:
   - Calls `UserClient` → `auth-service` to get `patient.fullName` and `patient.email`
   - Builds a `NotificationDto` with `type=FOLLOWUP`, `channel=EMAIL`
   - Calls `NotificationClient` → `notification-service` POST `/notifications/send`
   - `notification-service` then sends a real Gmail email to the patient
3. Individual record failures are caught and logged — one failure does **not** stop the rest
4. Fatal outer errors are caught — scheduler never crashes, runs again next morning

### Repository Queries

| Method | Query Type | Used For |
|---|---|---|
| `findByAppointmentId(int)` | Derived | Get record by appointment, also used in duplicate check |
| `existsByAppointmentId(int)` | Derived | Duplicate check before creating |
| `findByPatientIdOrderByCreatedAtDesc(int)` | Derived | Patient's records — newest first |
| `findByProviderId(int)` | Derived | Doctor's created records |
| `findByFollowUpDate(LocalDate)` | Derived | Scheduler daily run — today's follow-ups |
| `findUpcomingFollowUps(patientId, today)` | `@Query` JPQL | Patient dashboard upcoming follow-ups |
| `countByPatientId(int)` | Derived | Patient profile record count |
| `findByRecordId(int)` | Derived | Get record by PK |
| `deleteByRecordId(int)` | `@Modifying` JPQL | Admin delete |

### Business Logic Rules

| Operation | Guard Rules |
|---|---|
| **Create Record** | Appointment must be `COMPLETED` (Feign call); no duplicate record for same appointment; diagnosis cannot be blank; `followUpDate` cannot be in the past |
| **Update Record** | Diagnosis cannot be empty on update; `followUpDate` cannot be in the past |
| **Attach Document** | `attachmentUrl` cannot be null or blank |
| **Get Follow-Up Records** | Date cannot be null |
| **Delete Record** | Record must exist (404 if not found) |

### Access Control Rules

The service implements three-tier access segmentation per PDF requirements:

| Role | Access Rule |
|---|---|
| **Patient** | Can view only records where `patientId` matches their own user ID |
| **Doctor** | Can view only records where `providerId` matches their own provider ID |
| **Admin** | Read-only access to all records; can delete |

> Note: These rules are enforced at the application/gateway level. The service itself `permitAll` — the JWT forwarded headers (`X-User-Id`, `X-User-Role`) enable the frontend/gateway to enforce the access rules.

<img src="https://user-images.githubusercontent.com/73097560/115834477-dbab4500-a447-11eb-908a-139a6edaec5c.gif" width="100%">

## 📡 API Endpoints Summary

**Base URL (via gateway):** `http://localhost:8080`
**Base URL (direct):** `http://localhost:8088`

All endpoints are prefixed with `/records`.

| Method | Endpoint | Auth | Who | Description |
|---|---|---|---|---|
| `POST` | `/records/create` | Required | Doctor | Create a medical record for a COMPLETED appointment |
| `GET` | `/records/appointment/{appointmentId}` | Required | Doctor / Patient | Get record linked to a specific appointment |
| `GET` | `/records/patient/{patientId}` | Required | Patient | All records for a patient (newest first) |
| `GET` | `/records/provider/{providerId}` | Required | Doctor | All records created by a doctor |
| `GET` | `/records/{recordId}` | Required | Doctor / Admin | Get a single record by ID |
| `PUT` | `/records/{recordId}` | Required | Doctor | Update an existing record |
| `DELETE` | `/records/{recordId}` | Required | Admin | Delete a record permanently |
| `PUT` | `/records/{recordId}/attach?url=` | Required | Doctor | Attach document URL (S3 lab report, X-ray) |
| `GET` | `/records/patient/{patientId}/followups` | Required | Patient / Doctor | Get upcoming follow-up records |
| `GET` | `/records/followups/today` | Required | System / Admin | Get all records with followUpDate = today |
| `GET` | `/records/patient/{patientId}/count` | Required | Patient / Admin | Total medical record count for patient |

<img src="https://user-images.githubusercontent.com/73097560/115834477-dbab4500-a447-11eb-908a-139a6edaec5c.gif" width="100%">

## 🧪 API Testing via API Gateway

> All examples use `http://localhost:8080` (API Gateway).
> Replace `<YOUR_JWT_TOKEN>` with a token from auth-service login.

---

### 🔐 Step 0 — Get JWT Tokens

**Register a doctor:**

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

**Login to get token:**

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
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "type": "Bearer",
  "userId": 1,
  "email": "arjun.sharma@medibook.com",
  "role": "DOCTOR"
}
```

> **Pre-requisite:** Before calling `POST /records/create`, the appointment must already be `COMPLETED`. Use `PUT /appointments/{id}/complete` first (see UC4 README).

---

### 1️⃣ Create a Medical Record

**`POST /records/create`**

Doctor creates a medical record after marking an appointment as COMPLETED. The service calls `appointment-service` via Feign to verify the appointment status before saving.

```bash
curl -X POST http://localhost:8080/records/create \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <DOCTOR_TOKEN>" \
  -d '{
    "appointmentId": 1,
    "patientId": 2,
    "providerId": 1,
    "diagnosis": "Viral Upper Respiratory Tract Infection",
    "prescription": "Amoxicillin 500mg — twice daily for 5 days. Paracetamol 500mg — as needed for fever.",
    "notes": "Patient should rest, drink plenty of fluids, and avoid cold exposure. Return if symptoms worsen.",
    "attachmentUrl": null,
    "followUpDate": "2026-05-22"
  }'
```

**Expected Response — 201 Created:**

```json
{
  "recordId": 1,
  "appointmentId": 1,
  "patientId": 2,
  "providerId": 1,
  "diagnosis": "Viral Upper Respiratory Tract Infection",
  "prescription": "Amoxicillin 500mg — twice daily for 5 days. Paracetamol 500mg — as needed for fever.",
  "notes": "Patient should rest, drink plenty of fluids, and avoid cold exposure. Return if symptoms worsen.",
  "attachmentUrl": null,
  "followUpDate": "2026-05-22",
  "createdAt": "2026-04-22T15:00:00",
  "updatedAt": "2026-04-22T15:00:00"
}
```

> **Side effect:** On `2026-05-22` at 08:00 AM, `FollowUpReminderScheduler` will automatically send an email reminder to the patient.

---

### 2️⃣ Create a Record Without Follow-Up

**`POST /records/create`**

Simple consultation with no follow-up required.

```bash
curl -X POST http://localhost:8080/records/create \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <DOCTOR_TOKEN>" \
  -d '{
    "appointmentId": 2,
    "patientId": 3,
    "providerId": 1,
    "diagnosis": "Tension Headache",
    "prescription": "Ibuprofen 400mg after meals for 3 days. Avoid screen time.",
    "notes": "Advise stress reduction and proper sleep hygiene.",
    "followUpDate": null
  }'
```

**Expected Response — 201 Created:**

```json
{
  "recordId": 2,
  "appointmentId": 2,
  "patientId": 3,
  "providerId": 1,
  "diagnosis": "Tension Headache",
  "prescription": "Ibuprofen 400mg after meals for 3 days. Avoid screen time.",
  "notes": "Advise stress reduction and proper sleep hygiene.",
  "attachmentUrl": null,
  "followUpDate": null,
  "createdAt": "2026-04-22T15:05:00",
  "updatedAt": "2026-04-22T15:05:00"
}
```

---

### 3️⃣ Get Medical Record by Appointment ID

**`GET /records/appointment/{appointmentId}`**

Fetch the medical record linked to a specific appointment. Used when a patient opens their appointment and clicks "View Medical Record."

```bash
curl -X GET http://localhost:8080/records/appointment/1 \
  -H "Authorization: Bearer <YOUR_JWT_TOKEN>"
```

**Expected Response — 200 OK:**

```json
{
  "recordId": 1,
  "appointmentId": 1,
  "patientId": 2,
  "providerId": 1,
  "diagnosis": "Viral Upper Respiratory Tract Infection",
  "prescription": "Amoxicillin 500mg — twice daily for 5 days. Paracetamol 500mg — as needed for fever.",
  "notes": "Patient should rest, drink plenty of fluids, and avoid cold exposure. Return if symptoms worsen.",
  "attachmentUrl": null,
  "followUpDate": "2026-05-22",
  "createdAt": "2026-04-22T15:00:00",
  "updatedAt": "2026-04-22T15:00:00"
}
```

---

### 4️⃣ Get All Medical Records for a Patient

**`GET /records/patient/{patientId}`**

Patient views their complete medical history — all records, newest first. Access-controlled: patient should only see records matching their own `patientId`.

```bash
curl -X GET http://localhost:8080/records/patient/2 \
  -H "Authorization: Bearer <PATIENT_TOKEN>"
```

**Expected Response — 200 OK:**

```json
[
  {
    "recordId": 1,
    "appointmentId": 1,
    "patientId": 2,
    "providerId": 1,
    "diagnosis": "Viral Upper Respiratory Tract Infection",
    "prescription": "Amoxicillin 500mg — twice daily for 5 days.",
    "notes": "Rest, fluids, avoid cold.",
    "attachmentUrl": null,
    "followUpDate": "2026-05-22",
    "createdAt": "2026-04-22T15:00:00",
    "updatedAt": "2026-04-22T15:00:00"
  },
  {
    "recordId": 5,
    "appointmentId": 7,
    "patientId": 2,
    "providerId": 2,
    "diagnosis": "Type 2 Diabetes — Initial Diagnosis",
    "prescription": "Metformin 500mg twice daily with meals.",
    "notes": "HbA1c: 7.8%. Follow low-sugar diet. Weekly glucose monitoring.",
    "attachmentUrl": "https://s3.amazonaws.com/medibook/lab-report-hba1c.pdf",
    "followUpDate": "2026-06-01",
    "createdAt": "2026-04-10T09:30:00",
    "updatedAt": "2026-04-10T09:30:00"
  }
]
```

---

### 5️⃣ Get All Records Created by a Doctor

**`GET /records/provider/{providerId}`**

Doctor views all the medical records they have created. Used on the doctor's dashboard under "My Records."

```bash
curl -X GET http://localhost:8080/records/provider/1 \
  -H "Authorization: Bearer <DOCTOR_TOKEN>"
```

**Expected Response — 200 OK:**

```json
[
  {
    "recordId": 1,
    "appointmentId": 1,
    "patientId": 2,
    "providerId": 1,
    "diagnosis": "Viral Upper Respiratory Tract Infection",
    "prescription": "Amoxicillin 500mg — twice daily for 5 days.",
    "followUpDate": "2026-05-22",
    "createdAt": "2026-04-22T15:00:00",
    "updatedAt": "2026-04-22T15:00:00"
  },
  {
    "recordId": 2,
    "appointmentId": 2,
    "patientId": 3,
    "providerId": 1,
    "diagnosis": "Tension Headache",
    "prescription": "Ibuprofen 400mg after meals for 3 days.",
    "followUpDate": null,
    "createdAt": "2026-04-22T15:05:00",
    "updatedAt": "2026-04-22T15:05:00"
  }
]
```

---

### 6️⃣ Get a Single Record by ID

**`GET /records/{recordId}`**

Fetch a specific medical record by its primary key. Used for admin audits and doctor record updates.

```bash
curl -X GET http://localhost:8080/records/1 \
  -H "Authorization: Bearer <YOUR_JWT_TOKEN>"
```

**Expected Response — 200 OK:**

```json
{
  "recordId": 1,
  "appointmentId": 1,
  "patientId": 2,
  "providerId": 1,
  "diagnosis": "Viral Upper Respiratory Tract Infection",
  "prescription": "Amoxicillin 500mg — twice daily for 5 days. Paracetamol 500mg — as needed for fever.",
  "notes": "Patient should rest, drink plenty of fluids, and avoid cold exposure. Return if symptoms worsen.",
  "attachmentUrl": null,
  "followUpDate": "2026-05-22",
  "createdAt": "2026-04-22T15:00:00",
  "updatedAt": "2026-04-22T15:00:00"
}
```

---

### 7️⃣ Update a Medical Record

**`PUT /records/{recordId}`**

Doctor edits an existing record — updates diagnosis, prescription, notes, or changes the follow-up date. Follow-up date cannot be in the past.

```bash
curl -X PUT http://localhost:8080/records/1 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <DOCTOR_TOKEN>" \
  -d '{
    "appointmentId": 1,
    "patientId": 2,
    "providerId": 1,
    "diagnosis": "Viral Upper Respiratory Tract Infection with Secondary Bacterial Component",
    "prescription": "Amoxicillin-Clavulanate 625mg — twice daily for 7 days. Paracetamol 500mg — as needed.",
    "notes": "Patient condition worsened slightly. Extended antibiotic course. Rest for 5 days.",
    "attachmentUrl": null,
    "followUpDate": "2026-05-29"
  }'
```

**Expected Response — 200 OK:**

```json
{
  "recordId": 1,
  "appointmentId": 1,
  "patientId": 2,
  "providerId": 1,
  "diagnosis": "Viral Upper Respiratory Tract Infection with Secondary Bacterial Component",
  "prescription": "Amoxicillin-Clavulanate 625mg — twice daily for 7 days. Paracetamol 500mg — as needed.",
  "notes": "Patient condition worsened slightly. Extended antibiotic course. Rest for 5 days.",
  "attachmentUrl": null,
  "followUpDate": "2026-05-29",
  "createdAt": "2026-04-22T15:00:00",
  "updatedAt": "2026-04-22T15:30:00"
}
```

---

### 8️⃣ Attach a Document (Lab Report / X-Ray)

**`PUT /records/{recordId}/attach?url=`**

Doctor uploads a lab report or X-ray to S3 and attaches the URL to the medical record. In production this would be an S3 pre-signed URL.

```bash
curl -X PUT "http://localhost:8080/records/1/attach?url=https://s3.amazonaws.com/medibook/records/chest-xray-patient2-apr22.pdf" \
  -H "Authorization: Bearer <DOCTOR_TOKEN>"
```

**Expected Response — 200 OK:**

```json
{
  "message": "Document attached successfully.",
  "attachmentUrl": "https://s3.amazonaws.com/medibook/records/chest-xray-patient2-apr22.pdf"
}
```

---

### 9️⃣ Get Upcoming Follow-Up Records for a Patient

**`GET /records/patient/{patientId}/followups`**

Returns all records for a patient where `followUpDate >= today`. Shown on the patient dashboard as "Upcoming Follow-Ups."

```bash
curl -X GET http://localhost:8080/records/patient/2/followups \
  -H "Authorization: Bearer <PATIENT_TOKEN>"
```

**Expected Response — 200 OK:**

```json
[
  {
    "recordId": 1,
    "appointmentId": 1,
    "patientId": 2,
    "providerId": 1,
    "diagnosis": "Viral Upper Respiratory Tract Infection",
    "prescription": "Amoxicillin 500mg — twice daily for 5 days.",
    "notes": "Rest, fluids, avoid cold.",
    "attachmentUrl": "https://s3.amazonaws.com/medibook/records/chest-xray-patient2-apr22.pdf",
    "followUpDate": "2026-05-29",
    "createdAt": "2026-04-22T15:00:00",
    "updatedAt": "2026-04-22T15:30:00"
  },
  {
    "recordId": 5,
    "appointmentId": 7,
    "patientId": 2,
    "providerId": 2,
    "diagnosis": "Type 2 Diabetes — Initial Diagnosis",
    "followUpDate": "2026-06-01",
    "createdAt": "2026-04-10T09:30:00"
  }
]
```

---

### 🔟 Get Today's Follow-Up Records (System / Admin)

**`GET /records/followups/today`**

Returns all records where `followUpDate = today`. This is exactly what `FollowUpReminderScheduler` calls at 08:00 AM to determine which patients to email.

```bash
curl -X GET http://localhost:8080/records/followups/today \
  -H "Authorization: Bearer <ADMIN_TOKEN>"
```

**Expected Response — 200 OK (on 2026-05-22):**

```json
[
  {
    "recordId": 1,
    "appointmentId": 1,
    "patientId": 2,
    "providerId": 1,
    "diagnosis": "Viral Upper Respiratory Tract Infection",
    "followUpDate": "2026-05-22",
    "createdAt": "2026-04-22T15:00:00"
  }
]
```

> If no records have `followUpDate = today`, returns an empty array `[]`.

---

### 1️⃣1️⃣ Get Record Count for a Patient

**`GET /records/patient/{patientId}/count`**

Returns the total number of medical records a patient has. Used on patient profile pages.

```bash
curl -X GET http://localhost:8080/records/patient/2/count \
  -H "Authorization: Bearer <PATIENT_TOKEN>"
```

**Expected Response — 200 OK:**

```json
{
  "patientId": 2,
  "totalRecords": 5
}
```

---

### 1️⃣2️⃣ Delete a Medical Record (Admin Only)

**`DELETE /records/{recordId}`**

Permanently deletes a medical record. Only admins should have access to this endpoint.

```bash
curl -X DELETE http://localhost:8080/records/3 \
  -H "Authorization: Bearer <ADMIN_TOKEN>"
```

**Expected Response — 200 OK:**

```json
{
  "message": "Medical record deleted successfully."
}
```

---

### 1️⃣3️⃣ Full End-to-End Flow Test

Complete sequence from appointment completion to medical record with follow-up:

```bash
# Step 1 — Complete the appointment (appointment-service UC4)
curl -X PUT http://localhost:8080/appointments/1/complete \
  -H "Authorization: Bearer <DOCTOR_TOKEN>"

# Step 2 — Create medical record (record-service UC8)
curl -X POST http://localhost:8080/records/create \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <DOCTOR_TOKEN>" \
  -d '{
    "appointmentId": 1,
    "patientId": 2,
    "providerId": 1,
    "diagnosis": "Hypertension Stage 1",
    "prescription": "Amlodipine 5mg once daily. Low-sodium diet.",
    "notes": "BP: 145/92. Monitor weekly.",
    "followUpDate": "2026-05-22"
  }'

# Step 3 — Patient views their records
curl -X GET http://localhost:8080/records/patient/2 \
  -H "Authorization: Bearer <PATIENT_TOKEN>"

# Step 4 — Doctor attaches blood test report
curl -X PUT "http://localhost:8080/records/1/attach?url=https://s3.amazonaws.com/medibook/bp-test.pdf" \
  -H "Authorization: Bearer <DOCTOR_TOKEN>"

# Step 5 — Patient checks upcoming follow-ups
curl -X GET http://localhost:8080/records/patient/2/followups \
  -H "Authorization: Bearer <PATIENT_TOKEN>"

# Step 6 — (Automatic at 08:00 AM on 2026-05-22)
# FollowUpReminderScheduler runs:
#   → Finds record #1 with followUpDate = today
#   → Feign → auth-service: gets patient.fullName, patient.email
#   → Feign → notification-service: sends EMAIL to patient
#   Patient receives: "Dear Priya, today is your scheduled follow-up..."
```

---

### 1️⃣4️⃣ Test Error — Appointment Not Completed

Attempting to create a record for an appointment that isn't `COMPLETED` yet:

```bash
curl -X POST http://localhost:8080/records/create \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <DOCTOR_TOKEN>" \
  -d '{
    "appointmentId": 99,
    "patientId": 2,
    "providerId": 1,
    "diagnosis": "Test diagnosis"
  }'
```

**Expected Response — 400 Bad Request:**

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Medical record can only be created for COMPLETED appointments. Status: SCHEDULED",
  "timestamp": "2026-04-22T16:00:00"
}
```

---

### 1️⃣5️⃣ Test Error — Duplicate Record

Attempting to create a second record for the same appointment:

```bash
curl -X POST http://localhost:8080/records/create \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <DOCTOR_TOKEN>" \
  -d '{
    "appointmentId": 1,
    "patientId": 2,
    "providerId": 1,
    "diagnosis": "Second attempt for same appointment"
  }'
```

**Expected Response — 409 Conflict:**

```json
{
  "status": 409,
  "error": "Conflict",
  "message": "Medical record already exists for appointment: 1",
  "timestamp": "2026-04-22T16:01:00"
}
```

<img src="https://user-images.githubusercontent.com/73097560/115834477-dbab4500-a447-11eb-908a-139a6edaec5c.gif" width="100%">

## ❌ Error Responses

All exceptions are caught by `GlobalExceptionHandler` (`@ControllerAdvice`) and return a consistent JSON shape:

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Medical record can only be created for COMPLETED appointments. Status: SCHEDULED",
  "timestamp": "2026-04-22T16:00:00"
}
```

| HTTP Status | Scenario |
|---|---|
| `400` | Appointment not COMPLETED; blank diagnosis on create/update; follow-up date in past; null attachment URL; null date in scheduler |
| `401` | Missing or invalid JWT token (rejected at gateway) |
| `403` | Insufficient role / forbidden access |
| `404` | Record not found by recordId or appointmentId |
| `409` | Medical record already exists for this appointment (unique constraint) |
| `500` | Unexpected server error |

**Complete error message reference:**

| Trigger | Error Message |
|---|---|
| Appointment not COMPLETED | `"Medical record can only be created for COMPLETED appointments. Status: {status}"` |
| Duplicate record | `"Medical record already exists for appointment: {appointmentId}"` |
| Blank diagnosis on create | `"Diagnosis is required for medical record."` |
| Follow-up date in past | `"Follow up date cannot be in the past."` |
| Blank diagnosis on update | `"Diagnosis cannot be empty."` |
| Blank attachment URL | `"Attachment URL cannot be empty."` |
| Null date in query | `"Date cannot be null."` |
| Record not found | `"MedicalRecord not found with id: {recordId}"` |
| Record not found by appointment | `"MedicalRecord not found with appointmentId: {appointmentId}"` |

<img src="https://user-images.githubusercontent.com/73097560/115834477-dbab4500-a447-11eb-908a-139a6edaec5c.gif" width="100%">

## ⚙️ Environment Variables

| Variable | Required | Default (dev) | Description |
|---|---|---|---|
| `JWT_SECRET` | **Yes** | — | Must match `api-gateway` and `auth-service`. Strong Base64 (min 256-bit) |
| `DB_USERNAME` | No | `medibook_user` | MySQL username |
| `DB_PASSWORD` | No | `medibook_pass` | MySQL password |
| `MAIL_USERNAME` | **Yes** (for scheduler) | — | Gmail address for sending follow-up emails |
| `MAIL_PASSWORD` | **Yes** (for scheduler) | — | Gmail App Password (16-char — NOT your account password) |
| `EUREKA_DEFAULT_ZONE` | No | `http://admin:medibook123@localhost:8761/eureka/` | Eureka registry URL |

**Export in bash:**

```bash
export JWT_SECRET="myVeryStrongBase64SecretKeyForMediBookThatIs256BitsLong"
export DB_USERNAME="medibook_user"
export DB_PASSWORD="medibook_pass"
export MAIL_USERNAME="yourapp@gmail.com"
export MAIL_PASSWORD="abcd efgh ijkl mnop"
```

> **Gmail App Password Setup:**  Google Account → Security → 2-Step Verification → App Passwords → Generate → Copy 16-char password

<img src="https://user-images.githubusercontent.com/73097560/115834477-dbab4500-a447-11eb-908a-139a6edaec5c.gif" width="100%">

## 🚀 Running the Services

### Prerequisites

- Java 17+ and Maven 3.8+
- MySQL 8 running locally
- All upstream services running (Eureka → Gateway → Auth → Provider → Schedule → Appointment → Notification)

### Startup Order

```bash
# 1. Eureka Server — MUST be first
cd eureka-server && mvn spring-boot:run

# 2. API Gateway — MUST be second
cd api-gateway && JWT_SECRET=<secret> mvn spring-boot:run

# 3. Auth Service (record-service calls this via Feign)
cd auth-service && JWT_SECRET=<secret> mvn spring-boot:run

# 4. Provider Service
cd provider-service && JWT_SECRET=<secret> mvn spring-boot:run

# 5. Schedule Service
cd schedule-service && JWT_SECRET=<secret> mvn spring-boot:run

# 6. Appointment Service (record-service calls this via Feign)
cd appointment-service && JWT_SECRET=<secret> mvn spring-boot:run

# 7. Notification Service (record-service calls this via Feign)
cd notification-service \
  && JWT_SECRET=<secret> MAIL_USERNAME=<gmail> MAIL_PASSWORD=<app-pw> \
  mvn spring-boot:run

# 8. Record Service — THIS SERVICE (depends on 3, 6, and 7 above)
cd record-service \
  && JWT_SECRET=<secret> MAIL_USERNAME=<gmail> MAIL_PASSWORD=<app-pw> \
  mvn spring-boot:run
```

### Build and Run as JAR

```bash
cd record-service
mvn clean package -DskipTests

java -jar target/record-service-1.0.0.jar \
  --jwt.secret=<JWT_SECRET> \
  --spring.datasource.username=medibook_user \
  --spring.datasource.password=medibook_pass \
  --spring.mail.username=yourapp@gmail.com \
  --spring.mail.password="abcd efgh ijkl mnop"
```

<img src="https://user-images.githubusercontent.com/73097560/115834477-dbab4500-a447-11eb-908a-139a6edaec5c.gif" width="100%">

## 🗄️ Database Setup

```sql
-- Create all required databases
CREATE DATABASE IF NOT EXISTS record_db;
CREATE DATABASE IF NOT EXISTS notification_db;
CREATE DATABASE IF NOT EXISTS appointment_db;
CREATE DATABASE IF NOT EXISTS auth_db;
CREATE DATABASE IF NOT EXISTS provider_db;
CREATE DATABASE IF NOT EXISTS schedule_db;

-- Create shared user
CREATE USER IF NOT EXISTS 'medibook_user'@'localhost' IDENTIFIED BY 'medibook_pass';

-- Grant permissions
GRANT ALL PRIVILEGES ON record_db.*       TO 'medibook_user'@'localhost';
GRANT ALL PRIVILEGES ON notification_db.* TO 'medibook_user'@'localhost';
GRANT ALL PRIVILEGES ON appointment_db.*  TO 'medibook_user'@'localhost';
GRANT ALL PRIVILEGES ON auth_db.*         TO 'medibook_user'@'localhost';
GRANT ALL PRIVILEGES ON provider_db.*     TO 'medibook_user'@'localhost';
GRANT ALL PRIVILEGES ON schedule_db.*     TO 'medibook_user'@'localhost';

FLUSH PRIVILEGES;
```

Hibernate `ddl-auto: update` auto-creates the `medical_records` table on first startup.

**Expected table:**

```sql
CREATE TABLE medical_records (
  record_id       INT AUTO_INCREMENT PRIMARY KEY,
  appointment_id  INT          NOT NULL UNIQUE,   -- one record per appointment
  patient_id      INT          NOT NULL,
  provider_id     INT          NOT NULL,
  diagnosis       VARCHAR(255) NOT NULL,
  prescription    VARCHAR(500),
  notes           TEXT,
  attachment_url  VARCHAR(500),
  follow_up_date  DATE,
  created_at      DATETIME     NOT NULL,
  updated_at      DATETIME
);
```

<img src="https://user-images.githubusercontent.com/73097560/115834477-dbab4500-a447-11eb-908a-139a6edaec5c.gif" width="100%">

## 📖 Swagger UI

Direct access to each service's Swagger UI (bypasses gateway):

| Service | Swagger URL |
|---|---|
| **record-service** | http://localhost:8088/swagger-ui.html |
| notification-service | http://localhost:8087/swagger-ui.html |
| appointment-service | http://localhost:8084/swagger-ui.html |
| schedule-service | http://localhost:8083/swagger-ui.html |
| auth-service | http://localhost:8081/swagger-ui.html |
| provider-service | http://localhost:8082/swagger-ui.html |

OpenAPI JSON docs available at `/api-docs` on each service.

<img src="https://user-images.githubusercontent.com/73097560/115834477-dbab4500-a447-11eb-908a-139a6edaec5c.gif" width="100%">

## Author👨‍💻

[Harshal Choudhary](https://github.com/Harshal-25C) - Software Developer👨‍💻 | Cloud Enthusiast  
B.Tech - `[Computer Science & Engineering]`  
Java | Spring Boot | Spring Security | JWT | MySQL | Clean Architecture

<img src="https://user-images.githubusercontent.com/73097560/115834477-dbab4500-a447-11eb-908a-139a6edaec5c.gif" width="100%">

<div align="center">

<img src="https://capsule-render.vercel.app/api?type=waving&color=0:0d4a3a,40:137057,80:1a9e6e,100:0d4a3a&height=120&section=footer" width="100%"/>

**MediBook Microservices — UC8 Record Service**

`3 Feign Clients` · `@Scheduled 08:00 AM` · `HIPAA Audit Trail` · `Spring Boot 3.2` · `Java 17`

![MIT License](https://img.shields.io/badge/License-MIT-yellow?style=flat-square)
![UC8](https://img.shields.io/badge/Feature-UC8_Record_Service-brightgreen?style=flat-square)
![Feign](https://img.shields.io/badge/OpenFeign-3_Clients-6DB33F?style=flat-square&logo=spring)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.2.0-6DB33F?style=flat-square&logo=springboot)

</div>