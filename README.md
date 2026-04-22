<div align="center">

<!-- Animated Banner -->
<img src="https://capsule-render.vercel.app/api?type=waving&color=gradient&customColorList=6,11,20&height=200&section=header&text=MediBook%20Appointment%20Service&fontSize=40&fontColor=fff&animation=fadeIn&fontAlignY=38&desc=UC4%20·%20Microservices%20·%20Spring%20Boot%203.2&descAlignY=55&descAlign=50" width="100%"/>

<!-- Animated Typing -->
<a href="#">
  <img src="https://readme-typing-svg.demolab.com?font=JetBrains+Mono&size=22&duration=3000&pause=800&color=00D4FF&center=true&vCenter=true&multiline=true&width=600&height=60&lines=Book+·+Cancel+·+Reschedule+·+Complete;Powered+by+RabbitMQ+%2B+Feign+%2B+Eureka" alt="Typing SVG" />
</a>

<br/>

<!-- Badges -->
![Java](https://img.shields.io/badge/Java_17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot_3.2-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL_8-4479A1?style=for-the-badge&logo=mysql&logoColor=white)
![RabbitMQ](https://img.shields.io/badge/RabbitMQ-FF6600?style=for-the-badge&logo=rabbitmq&logoColor=white)
![Spring Cloud](https://img.shields.io/badge/Spring_Cloud-6DB33F?style=for-the-badge&logo=spring&logoColor=white)
![Swagger](https://img.shields.io/badge/Swagger_UI-85EA2D?style=for-the-badge&logo=swagger&logoColor=black)

<br/>

![Port](https://img.shields.io/badge/PORT-8084-blue?style=flat-square)
![Database](https://img.shields.io/badge/DB-appointment__db-blue?style=flat-square)
![UC4](https://img.shields.io/badge/UC4-Appointment_Service-brightgreen?style=flat-square)
![License](https://img.shields.io/badge/License-MIT-yellow?style=flat-square)

</div>

---

<img src="https://user-images.githubusercontent.com/73097560/115834477-dbab4500-a447-11eb-908a-139a6edaec5c.gif" width="100%">

## 📋 Table of Contents

- [Overview](#-overview)
- [System Architecture](#-system-architecture)
- [Service Port Map](#-service-port-map)
- [Appointment Service Deep Dive](#-appointment-service-deep-dive)
  - [Tech Stack](#tech-stack)
  - [Project Structure](#project-structure)
  - [Entity: Appointment](#entity-appointment)
  - [DTOs](#dtos)
  - [Feign Client — SlotClient](#feign-client--slotclient)
  - [RabbitMQ Messaging](#-rabbitmq-messaging)
  - [NoShow Detection Scheduler](#noshow-detection-scheduler)
  - [Business Logic Rules](#business-logic-rules)
- [Appointment Lifecycle](#-appointment-lifecycle)
- [API Endpoints Summary](#-api-endpoints-summary)
- [API Testing via Gateway](#-api-testing-via-api-gateway)
- [Error Responses](#-error-responses)
- [Environment Variables](#-environment-variables)
- [Running the Services](#-running-the-services)
- [Database Setup](#-database-setup)
- [Swagger UI](#-swagger-ui)

<img src="https://user-images.githubusercontent.com/73097560/115834477-dbab4500-a447-11eb-908a-139a6edaec5c.gif" width="100%">

## 🏥 Overview

The **Appointment Service** is the **UC4** microservice in the MediBook Online Appointment Booking System. It acts as the central coordinator for everything appointment-related — orchestrating communication between the patient, the doctor's slot availability (UC3 / schedule-service), payment processing (UC5), reviews (UC6), and notifications (UC7).

```
Patient books a slot → Appointment created → Feign → schedule-service marks slot BOOKED
                                           → RabbitMQ → notification-service sends alerts
```

**Key responsibilities:**

- Book, cancel, reschedule, and complete appointments
- Coordinate slot locking/releasing with `schedule-service` via **OpenFeign**
- Publish lifecycle events (`BOOKED`, `CANCELLED`, `COMPLETED`) to **RabbitMQ** for `notification-service`
- Automatically detect and flag `NO_SHOW` appointments via a scheduled job (runs every hour)
- Expose appointment history to patients and doctors

<img src="https://user-images.githubusercontent.com/73097560/115834477-dbab4500-a447-11eb-908a-139a6edaec5c.gif" width="100%">

## 🏗️ System Architecture

```
┌──────────────────────────────────────────────────────────────────────┐
│                       CLIENT (Browser / App)                          │
└───────────────────────────────┬──────────────────────────────────────┘
                                │ HTTP Requests
                                ▼
┌──────────────────────────────────────────────────────────────────────┐
│                    API GATEWAY  :8080                                 │
│   • JWT Authentication (JwtAuthenticationFilter)                     │
│   • Forwards: X-User-Id, X-User-Role, X-User-Email                  │
│   • Routes /appointments/** → lb://appointment-service               │
└──────────────────────────────┬───────────────────────────────────────┘
                               │ Eureka lb://
        ┌──────────────────────┼───────────────────────┐
        ▼                      ▼                       ▼
┌──────────────┐   ┌──────────────────────┐   ┌──────────────────┐
│ auth-service │   │  appointment-service │   │ schedule-service │
│   :8081      │   │       :8084          ├──►│    :8083         │
│  auth_db     │   │   appointment_db     │   │  schedule_db     │
└──────────────┘   └──────────┬───────────┘   └──────────────────┘
                              │ RabbitMQ publish
                              ▼
                   ┌────────────────────────┐
                   │   RabbitMQ  :5672      │
                   │  medibook.exchange     │
                   │  (TopicExchange)       │
                   └────────────┬───────────┘
                                │ Consumed by
                                ▼
                   ┌────────────────────────┐
                   │  notification-service  │
                   │       :8087            │
                   └────────────────────────┘

┌──────────────────────────────────────────────────────────────────────┐
│                     EUREKA SERVER  :8761                              │
│            Service Registry  (admin / medibook123)                   │
└──────────────────────────────────────────────────────────────────────┘
```

<img src="https://user-images.githubusercontent.com/73097560/115834477-dbab4500-a447-11eb-908a-139a6edaec5c.gif" width="100%">

## 🗺️ Service Port Map

| Service | Port | Database | Notes |
|---|---|---|---|
| `eureka-server` | **8761** | — | Start **first** always |
| `api-gateway` | **8080** | — | Start **second**, all traffic here |
| `auth-service` | **8081** | `auth_db` | JWT issuance, OTP, OAuth2 |
| `provider-service` | **8082** | `provider_db` | Doctor profile management (UC2) |
| `schedule-service` | **8083** | `schedule_db` | Slot management (UC3) |
| `appointment-service` | **8084** | `appointment_db` | **← This service (UC4)** |
| `payment-service` | **8085** | `payment_db` | UC5 |
| `review-service` | **8086** | `review_db` | UC6 |
| `notification-service` | **8087** | `notification_db` | UC7 — consumes RabbitMQ events |
| `record-service` | **8088** | `record_db` | UC8 |

<img src="https://user-images.githubusercontent.com/73097560/115834477-dbab4500-a447-11eb-908a-139a6edaec5c.gif" width="100%">

## 🔬 Appointment Service Deep Dive

### Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.2.0 |
| Cloud | Spring Cloud 2023.0.0 (Eureka Client, OpenFeign) |
| Database | MySQL 8 — `appointment_db` |
| ORM | Spring Data JPA / Hibernate |
| Messaging | Spring AMQP / RabbitMQ (TopicExchange, 3 queues) |
| Security | Spring Security (stateless, JWT validated at gateway) |
| Scheduler | Spring `@Scheduled` + Quartz on classpath |
| Docs | Springdoc OpenAPI 2.3.0 (Swagger UI) |
| Serialization | Jackson2JsonMessageConverter (JSON over RabbitMQ) |
| Build | Maven 3, parent POM at root |
| Lombok | 1.18.30 |
| JWT Library | JJWT 0.11.5 |

### Project Structure

```
appointment-service/
└── src/main/java/com/medibook/appointment/
    ├── AppointmentServiceApplication.java       # Main — @EnableFeignClients, @EnableDiscoveryClient
    ├── client/
    │   └── SlotClient.java                      # Feign → schedule-service (get/book/release slot)
    ├── config/
    │   ├── RabbitMQConfig.java                  # Exchange, 3 queues, bindings, JSON converter
    │   └── SecurityConfig.java                  # Stateless, all routes permitAll (JWT at gateway)
    ├── dto/
    │   ├── AppointmentRequest.java              # Patient booking form DTO (with validation)
    │   ├── AppointmentEventDto.java             # RabbitMQ message payload (BOOKED/CANCELLED/COMPLETED)
    │   └── SlotDto.java                         # Feign response from schedule-service
    ├── entity/
    │   └── Appointment.java                     # JPA entity → appointments table
    ├── exception/
    │   ├── BadRequestException.java             # 400
    │   ├── ResourceNotFoundException.java       # 404
    │   ├── DuplicateResourceException.java      # 409
    │   ├── ForbiddenException.java              # 403
    │   ├── UnauthorizedException.java           # 401
    │   ├── ErrorResponse.java                   # Standardized error shape
    │   └── GlobalExceptionHandler.java          # @ControllerAdvice — catches all
    ├── messaging/
    │   └── AppointmentEventPublisher.java       # RabbitMQ producer (publishBooked/Cancelled/Completed)
    ├── repository/
    │   └── AppointmentRepository.java           # JPA repo with custom JPQL queries
    ├── resource/
    │   └── AppointmentResource.java             # REST controller — /appointments/**
    ├── scheduler/
    │   └── NoShowDetectionScheduler.java        # Cron: every hour, marks NO_SHOW
    └── service/
        ├── AppointmentService.java              # Interface contract
        └── impl/
            └── AppointmentServiceImpl.java      # Business logic implementation
```

### Entity: Appointment

Maps to the `appointments` table in `appointment_db`.

| Column | Type | Nullable | Default | Description |
|---|---|---|---|---|
| `appointmentId` | INT (PK, AI) | No | — | Auto-generated primary key |
| `patientId` | INT | No | — | User ID of the patient (from UC1) |
| `providerId` | INT | No | — | Provider ID of the doctor (from UC2) |
| `patientEmail` | VARCHAR | Yes | — | Patient email for notification routing |
| `slotId` | INT | No | — | FK to `availability_slots` (from UC3) |
| `serviceType` | VARCHAR | No | — | e.g., General Consultation, Dental Checkup |
| `appointmentDate` | DATE | No | — | Calendar date of the appointment |
| `startTime` | TIME | No | — | Appointment start time |
| `endTime` | TIME | No | — | Appointment end time |
| `status` | VARCHAR | No | `SCHEDULED` | `PENDING_PAYMENT` → `SCHEDULED` / `CONFIRMED` / `CANCELLED` / `COMPLETED` / `NO_SHOW` |
| `notes` | TEXT | Yes | — | Optional patient notes |
| `modeOfConsultation` | VARCHAR | No | — | `IN_PERSON` or `TELECONSULTATION` |
| `createdAt` | DATETIME | No | auto | Set via `@PrePersist` |
| `updatedAt` | DATETIME | Yes | auto | Updated via `@PreUpdate` |

> **Status Flow:** `PENDING_PAYMENT` → (payment webhook) → `CONFIRMED` / `SCHEDULED` → `COMPLETED` / `CANCELLED` / `NO_SHOW`

### DTOs

**`AppointmentRequest`** — the patient's booking form:

| Field | Type | Required | Validation | Description |
|---|---|---|---|---|
| `patientId` | int | Yes | `@NotNull` | Patient user ID |
| `providerId` | int | Yes | `@NotNull` | Doctor's provider ID |
| `patientEmail` | String | No | — | Used for RabbitMQ notification routing |
| `slotId` | int | Yes | `@NotNull` | Selected availability slot ID |
| `serviceType` | String | Yes | `@NotBlank` | Type of medical service requested |
| `appointmentDate` | LocalDate | Yes | `@NotNull` | Date matching the selected slot |
| `startTime` | LocalTime | Yes | `@NotNull` | Start time matching the selected slot |
| `endTime` | LocalTime | Yes | `@NotNull` | End time matching the selected slot |
| `modeOfConsultation` | String | Yes | `@NotBlank` | `IN_PERSON` or `TELECONSULTATION` |
| `notes` | String | No | — | Optional pre-consultation notes |

**`AppointmentEventDto`** — RabbitMQ message payload:

| Field | Description |
|---|---|
| `appointmentId` | The appointment being notified about |
| `patientId` | Patient to notify |
| `providerId` | Doctor to notify |
| `eventType` | `BOOKED`, `CANCELLED`, or `COMPLETED` |
| `serviceType` | For the notification message body |
| `modeOfConsultation` | Consultation type for context |
| `appointmentDate` | Human-readable date string |
| `startTime` / `endTime` | Appointment time strings |
| `message` | Pre-built human-readable notification text |

**`SlotDto`** — Feign response from `schedule-service`:

| Field | Description |
|---|---|
| `slotId` | Slot identifier |
| `providerId` | Doctor who owns this slot |
| `date` | Slot date |
| `startTime` / `endTime` | Slot time window |
| `durationMinutes` | Duration in minutes |
| `isBooked` | Whether slot is already taken |
| `isBlocked` | Whether doctor has blocked it |

### Feign Client — SlotClient

Communicates directly with `schedule-service` using Spring Cloud OpenFeign. Resolves the service by name via Eureka (`lb://schedule-service`).

```java
@FeignClient(name = "schedule-service")
public interface SlotClient {
    @GetMapping("/slots/{slotId}")
    SlotDto getSlotById(@PathVariable("slotId") int slotId);

    @PutMapping("/slots/{slotId}/book")
    void bookSlot(@PathVariable("slotId") int slotId);

    @PutMapping("/slots/{slotId}/release")
    void releaseSlot(@PathVariable("slotId") int slotId);
}
```

| Call | When | Why |
|---|---|---|
| `getSlotById()` | `bookAppointment()` and `rescheduleAppointment()` | Validate slot is available and belongs to the right provider |
| `bookSlot()` | `updateStatus("CONFIRMED")` and `rescheduleAppointment()` | Lock the slot so no other patient can book it |
| `releaseSlot()` | `cancelAppointment()` and `rescheduleAppointment()` | Free the slot when appointment is cancelled or moved |

### 📨 RabbitMQ Messaging

#### Exchange & Queue Topology

```
Exchange: medibook.exchange  (TopicExchange, durable=true)
│
├── Routing Key: appointment.booked    ──► Queue: medibook.appointment.booked
├── Routing Key: appointment.cancelled ──► Queue: medibook.appointment.cancelled
└── Routing Key: appointment.completed ──► Queue: medibook.appointment.completed
```

All messages serialized as **JSON** via `Jackson2JsonMessageConverter`.

#### Event Triggers

| Business Method | Event Published | Routing Key | Message Text |
|---|---|---|---|
| `updateStatus("CONFIRMED")` | `BOOKED` | `appointment.booked` | "Your appointment on {date} at {time}" |
| `cancelAppointment()` | `CANCELLED` | `appointment.cancelled` | "Your appointment on {date} at {time} has been cancelled." |
| `completeAppointment()` | `COMPLETED` | `appointment.completed` | "...has been marked as completed. Please check your medical records." |

`notification-service` (UC7) listens on all three queues and sends in-app / email alerts to both the patient and doctor.

### NoShow Detection Scheduler

```java
@Scheduled(cron = "0 0 * * * *")  // Every hour, on the hour (1:00, 2:00 ... 23:00, 0:00)
public void detectNoShows()
```

**Detection logic:**
1. Fetches all appointments with `status = SCHEDULED`
2. For each appointment checks two conditions:
   - `appointmentDate < today` → past date, never completed → **NO_SHOW**
   - `appointmentDate == today AND endTime < now` → time passed today → **NO_SHOW**
3. Calls `updateStatus(appointmentId, "NO_SHOW")` for each match
4. Logs success count but **never crashes** on error — next hour it runs again

### Business Logic Rules

| Operation | Guard Rules |
|---|---|
| **Book** | Slot must not be booked; slot must not be blocked; slot's `providerId` must match request's `providerId` |
| **Cancel** | Cannot cancel a `COMPLETED` appointment; cannot cancel already `CANCELLED` |
| **Reschedule** | Only `SCHEDULED` appointments can be rescheduled; new slot must not be booked |
| **Complete** | Only `SCHEDULED` appointments can be completed |
| **Status → CONFIRMED** | Books the slot via Feign + publishes `BOOKED` RabbitMQ event |
| **Status → CANCELLED** | Releases the slot via Feign + publishes `CANCELLED` RabbitMQ event |

<img src="https://user-images.githubusercontent.com/73097560/115834477-dbab4500-a447-11eb-908a-139a6edaec5c.gif" width="100%">

## 🔄 Appointment Lifecycle

```
              ┌─────────────────┐
              │  Patient Books   │
              └────────┬────────┘
                       │ POST /appointments/book
                       ▼
          ┌────────────────────────┐
          │    PENDING_PAYMENT     │ ← Initial status on create
          └────────────┬───────────┘
                       │ PUT /status?status=CONFIRMED
                       │ (payment-service confirms)
                       ▼
          ┌────────────────────────┐
    ┌────►│       SCHEDULED        │◄────────────────────────────┐
    │     └──────┬────────┬────────┘                             │
    │            │        │                                      │
    │  Reschedule│        │ Cancel                    Reschedule │
    │  (new slot)│        ▼                           (old→new)  │
    │            │  ┌──────────┐                                 │
    │            │  │CANCELLED │                                 │
    │            │  │  Slot    │                                 │
    │            │  │ Released │                                 │
    │            │  └──────────┘                                 │
    │            │                                               │
    │ Release ◄──┘                                   Book ──────►│
    │ old slot                                       new slot    │
    └────────────────────────────────────────────────────────────┘

          ┌────────────────────────┐
          │  Doctor Marks Complete  │
          └────────────┬────────────┘
                       ▼
          ┌────────────────────────┐
          │       COMPLETED        │
          │  Unlocks Review (UC6)  │
          │  Unlocks Records (UC8) │
          └────────────────────────┘

          ┌────────────────────────┐
          │  NoShow Scheduler      │
          │  (every hour)          │
          └────────────┬────────────┘
                       ▼
          ┌────────────────────────┐
          │        NO_SHOW         │
          │  (auto-detected)       │
          └────────────────────────┘
```

<img src="https://user-images.githubusercontent.com/73097560/115834477-dbab4500-a447-11eb-908a-139a6edaec5c.gif" width="100%">

## 📡 API Endpoints Summary

**Base URL (via gateway):** `http://localhost:8080`
**Base URL (direct):** `http://localhost:8084`

All endpoints are prefixed with `/appointments`.

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `POST` | `/appointments/book` | Required | Book a new appointment |
| `GET` | `/appointments/{appointmentId}` | Required | Get appointment by ID |
| `GET` | `/appointments/patient/{patientId}` | Required | All appointments for a patient |
| `GET` | `/appointments/patient/{patientId}/upcoming` | Required | Upcoming SCHEDULED appointments only |
| `GET` | `/appointments/provider/{providerId}` | Required | All appointments for a doctor |
| `GET` | `/appointments/provider/{providerId}/date?date=` | Required | Doctor's appointments on a specific date |
| `GET` | `/appointments/provider/{providerId}/count` | Required | Total appointment count for a doctor |
| `PUT` | `/appointments/{appointmentId}/cancel` | Required | Cancel an appointment |
| `PUT` | `/appointments/{appointmentId}/reschedule` | Required | Move to a new slot |
| `PUT` | `/appointments/{appointmentId}/complete` | Required | Doctor marks consultation as done |
| `PUT` | `/appointments/{appointmentId}/status?status=` | Required | Manually update appointment status |

<img src="https://user-images.githubusercontent.com/73097560/115834477-dbab4500-a447-11eb-908a-139a6edaec5c.gif" width="100%">

## 🧪 API Testing via API Gateway

> All examples target `http://localhost:8080` (API Gateway).
> Replace `<YOUR_JWT_TOKEN>` with a real token from auth-service login.

---

### 🔐 Step 0 — Obtain a JWT Token

**Register a patient:**

```bash
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Priya Patel",
    "email": "priya.patel@medibook.com",
    "password": "Patient@123",
    "phone": "9876543211",
    "role": "PATIENT"
  }'
```

**Login:**

```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "priya.patel@medibook.com",
    "password": "Patient@123"
  }'
```

**Sample response:**

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJwcml5YS5wYXRlbEBtZWRpYm9vay5jb20iLCJyb2xlIjoiUEFUSUVOVCIsInVzZXJJZCI6MX0...",
  "type": "Bearer",
  "userId": 1,
  "email": "priya.patel@medibook.com",
  "role": "PATIENT"
}
```

Use `token` as `Bearer <token>` in the `Authorization` header for all protected requests.

---

### 1️⃣ Book an Appointment (IN_PERSON)

**`POST /appointments/book`**

Patient books a slot. The service fetches slot details from `schedule-service` via Feign, validates availability, creates the appointment with status `PENDING_PAYMENT`, and returns booking confirmation.

```bash
curl -X POST http://localhost:8080/appointments/book \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <YOUR_JWT_TOKEN>" \
  -d '{
    "patientId": 1,
    "providerId": 1,
    "patientEmail": "priya.patel@medibook.com",
    "slotId": 5,
    "serviceType": "General Consultation",
    "appointmentDate": "2026-05-15",
    "startTime": "10:00",
    "endTime": "10:30",
    "modeOfConsultation": "IN_PERSON",
    "notes": "Persistent headaches for the past week."
  }'
```

**Expected Response — 201 Created:**

```json
{
  "message": "Appointment booked successfully.",
  "appointmentId": 1,
  "status": "PENDING_PAYMENT",
  "appointmentDate": "2026-05-15",
  "startTime": "10:00:00",
  "modeOfConsultation": "IN_PERSON"
}
```

---

### 2️⃣ Book a Teleconsultation Appointment

**`POST /appointments/book`**

```bash
curl -X POST http://localhost:8080/appointments/book \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <YOUR_JWT_TOKEN>" \
  -d '{
    "patientId": 2,
    "providerId": 1,
    "patientEmail": "rahul.mehta@medibook.com",
    "slotId": 6,
    "serviceType": "Follow-Up Consultation",
    "appointmentDate": "2026-05-16",
    "startTime": "11:00",
    "endTime": "11:30",
    "modeOfConsultation": "TELECONSULTATION",
    "notes": "Follow-up for blood pressure medication review."
  }'
```

**Expected Response — 201 Created:**

```json
{
  "message": "Appointment booked successfully.",
  "appointmentId": 2,
  "status": "PENDING_PAYMENT",
  "appointmentDate": "2026-05-16",
  "startTime": "11:00:00",
  "modeOfConsultation": "TELECONSULTATION"
}
```

---

### 3️⃣ Confirm Appointment After Payment

**`PUT /appointments/{appointmentId}/status?status=CONFIRMED`**

Called by `payment-service` after payment is processed. Transitions the appointment to `CONFIRMED` / `SCHEDULED`, books the slot via Feign (`bookSlot()`), and fires a `BOOKED` event on RabbitMQ.

```bash
curl -X PUT "http://localhost:8080/appointments/1/status?status=CONFIRMED" \
  -H "Authorization: Bearer <YOUR_JWT_TOKEN>"
```

**Expected Response — 200 OK:**

```json
{
  "message": "Appointment status updated to: CONFIRMED"
}
```

> **Side effects:** `schedule-service` sets `isBooked = true` on the slot. `notification-service` receives `BOOKED` event on `medibook.appointment.booked` queue.

---

### 4️⃣ Get Appointment by ID

**`GET /appointments/{appointmentId}`**

Fetch complete details of a specific appointment.

```bash
curl -X GET http://localhost:8080/appointments/1 \
  -H "Authorization: Bearer <YOUR_JWT_TOKEN>"
```

**Expected Response — 200 OK:**

```json
{
  "appointmentId": 1,
  "patientId": 1,
  "providerId": 1,
  "patientEmail": "priya.patel@medibook.com",
  "slotId": 5,
  "serviceType": "General Consultation",
  "appointmentDate": "2026-05-15",
  "startTime": "10:00:00",
  "endTime": "10:30:00",
  "status": "SCHEDULED",
  "notes": "Persistent headaches for the past week.",
  "modeOfConsultation": "IN_PERSON",
  "createdAt": "2026-04-22T09:00:00",
  "updatedAt": "2026-04-22T09:05:00"
}
```

---

### 5️⃣ Get All Appointments for a Patient

**`GET /appointments/patient/{patientId}`**

Returns the patient's complete appointment history across all statuses.

```bash
curl -X GET http://localhost:8080/appointments/patient/1 \
  -H "Authorization: Bearer <YOUR_JWT_TOKEN>"
```

**Expected Response — 200 OK:**

```json
[
  {
    "appointmentId": 1,
    "patientId": 1,
    "providerId": 1,
    "slotId": 5,
    "serviceType": "General Consultation",
    "appointmentDate": "2026-05-15",
    "startTime": "10:00:00",
    "endTime": "10:30:00",
    "status": "SCHEDULED",
    "modeOfConsultation": "IN_PERSON",
    "createdAt": "2026-04-22T09:00:00"
  },
  {
    "appointmentId": 3,
    "patientId": 1,
    "providerId": 2,
    "slotId": 9,
    "serviceType": "Dental Checkup",
    "appointmentDate": "2026-04-10",
    "startTime": "14:00:00",
    "endTime": "14:30:00",
    "status": "COMPLETED",
    "modeOfConsultation": "IN_PERSON",
    "createdAt": "2026-04-05T10:00:00"
  }
]
```

---

### 6️⃣ Get Upcoming Appointments for a Patient

**`GET /appointments/patient/{patientId}/upcoming`**

Returns only `SCHEDULED` appointments on or after today. Used on the patient dashboard.

```bash
curl -X GET http://localhost:8080/appointments/patient/1/upcoming \
  -H "Authorization: Bearer <YOUR_JWT_TOKEN>"
```

**Expected Response — 200 OK:**

```json
[
  {
    "appointmentId": 1,
    "patientId": 1,
    "providerId": 1,
    "slotId": 5,
    "serviceType": "General Consultation",
    "appointmentDate": "2026-05-15",
    "startTime": "10:00:00",
    "endTime": "10:30:00",
    "status": "SCHEDULED",
    "modeOfConsultation": "IN_PERSON",
    "notes": "Persistent headaches for the past week.",
    "createdAt": "2026-04-22T09:00:00"
  }
]
```

---

### 7️⃣ Get All Appointments for a Doctor

**`GET /appointments/provider/{providerId}`**

Doctor views all their bookings — entire history across all statuses.

```bash
curl -X GET http://localhost:8080/appointments/provider/1 \
  -H "Authorization: Bearer <YOUR_JWT_TOKEN>"
```

**Expected Response — 200 OK:**

```json
[
  {
    "appointmentId": 1,
    "patientId": 1,
    "providerId": 1,
    "slotId": 5,
    "serviceType": "General Consultation",
    "appointmentDate": "2026-05-15",
    "startTime": "10:00:00",
    "endTime": "10:30:00",
    "status": "SCHEDULED",
    "modeOfConsultation": "IN_PERSON"
  },
  {
    "appointmentId": 2,
    "patientId": 2,
    "providerId": 1,
    "slotId": 6,
    "serviceType": "Follow-Up Consultation",
    "appointmentDate": "2026-05-16",
    "startTime": "11:00:00",
    "endTime": "11:30:00",
    "status": "PENDING_PAYMENT",
    "modeOfConsultation": "TELECONSULTATION"
  }
]
```

---

### 8️⃣ Get Doctor's Appointments on a Specific Date

**`GET /appointments/provider/{providerId}/date?date=YYYY-MM-DD`**

Doctor views their daily schedule. Perfect for the clinic dashboard's "today's appointments."

```bash
curl -X GET "http://localhost:8080/appointments/provider/1/date?date=2026-05-15" \
  -H "Authorization: Bearer <YOUR_JWT_TOKEN>"
```

**Expected Response — 200 OK:**

```json
[
  {
    "appointmentId": 1,
    "patientId": 1,
    "providerId": 1,
    "slotId": 5,
    "serviceType": "General Consultation",
    "appointmentDate": "2026-05-15",
    "startTime": "10:00:00",
    "endTime": "10:30:00",
    "status": "SCHEDULED",
    "modeOfConsultation": "IN_PERSON",
    "notes": "Persistent headaches for the past week."
  }
]
```

---

### 9️⃣ Get Total Appointment Count for a Doctor

**`GET /appointments/provider/{providerId}/count`**

Returns total appointment count. Used in dashboards and admin analytics.

```bash
curl -X GET http://localhost:8080/appointments/provider/1/count \
  -H "Authorization: Bearer <YOUR_JWT_TOKEN>"
```

**Expected Response — 200 OK:**

```json
{
  "providerId": 1,
  "totalAppointments": 47
}
```

---

### 🔟 Cancel an Appointment

**`PUT /appointments/{appointmentId}/cancel`**

Cancels a scheduled appointment. Releases the slot in `schedule-service` via Feign and publishes a `CANCELLED` event to RabbitMQ. Cannot cancel `COMPLETED` or already `CANCELLED` appointments.

```bash
curl -X PUT http://localhost:8080/appointments/1/cancel \
  -H "Authorization: Bearer <YOUR_JWT_TOKEN>"
```

**Expected Response — 200 OK:**

```json
{
  "message": "Appointment cancelled successfully. Slot has been released for other patients."
}
```

> **Side effects:** `schedule-service` sets `isBooked = false` on the slot. `notification-service` receives `CANCELLED` event on `medibook.appointment.cancelled` queue.

---

### 1️⃣1️⃣ Reschedule an Appointment

**`PUT /appointments/{appointmentId}/reschedule`**

Moves the appointment to a different available slot with the same doctor. Old slot is released, new slot is booked. Only `SCHEDULED` appointments can be rescheduled.

```bash
curl -X PUT http://localhost:8080/appointments/1/reschedule \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <YOUR_JWT_TOKEN>" \
  -d '{
    "newSlotId": "8",
    "newDate": "2026-05-20",
    "newStartTime": "14:00",
    "newEndTime": "14:30"
  }'
```

**Expected Response — 200 OK:**

```json
{
  "appointmentId": 1,
  "patientId": 1,
  "providerId": 1,
  "slotId": 8,
  "serviceType": "General Consultation",
  "appointmentDate": "2026-05-20",
  "startTime": "14:00:00",
  "endTime": "14:30:00",
  "status": "SCHEDULED",
  "modeOfConsultation": "IN_PERSON",
  "notes": "Persistent headaches for the past week.",
  "createdAt": "2026-04-22T09:00:00",
  "updatedAt": "2026-04-22T10:30:00"
}
```

---

### 1️⃣2️⃣ Mark Appointment as Completed

**`PUT /appointments/{appointmentId}/complete`**

Doctor confirms the consultation is done. Transitions status to `COMPLETED` and publishes a `COMPLETED` RabbitMQ event. Only `SCHEDULED` appointments can be completed.

After completion:
- Patient unlocks ability to submit a review (UC6)
- Doctor unlocks ability to create a medical record (UC8)

```bash
curl -X PUT http://localhost:8080/appointments/1/complete \
  -H "Authorization: Bearer <YOUR_JWT_TOKEN>"
```

**Expected Response — 200 OK:**

```json
{
  "message": "Appointment marked as completed. Patient can now submit a review."
}
```

---

### 1️⃣3️⃣ Manually Mark as No-Show

**`PUT /appointments/{appointmentId}/status?status=NO_SHOW`**

Admin or scheduler marks an appointment as no-show. The `NoShowDetectionScheduler` does this automatically every hour, but admins can trigger it manually.

```bash
curl -X PUT "http://localhost:8080/appointments/2/status?status=NO_SHOW" \
  -H "Authorization: Bearer <YOUR_JWT_TOKEN>"
```

**Expected Response — 200 OK:**

```json
{
  "message": "Appointment status updated to: NO_SHOW"
}
```

---

### 1️⃣4️⃣ Full End-to-End Patient Journey

Complete sequence simulating a real patient booking flow:

```bash
# 1. Browse available slots (public — no token needed)
curl -X GET "http://localhost:8080/slots/available/1?date=2026-05-15"

# 2. Book the appointment
curl -X POST http://localhost:8080/appointments/book \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <PATIENT_TOKEN>" \
  -d '{"patientId":1,"providerId":1,"patientEmail":"priya@test.com","slotId":5,"serviceType":"General Consultation","appointmentDate":"2026-05-15","startTime":"10:00","endTime":"10:30","modeOfConsultation":"IN_PERSON"}'

# 3. Confirm after payment (payment-service webhook simulation)
curl -X PUT "http://localhost:8080/appointments/1/status?status=CONFIRMED" \
  -H "Authorization: Bearer <PATIENT_TOKEN>"

# 4. Verify the slot is now booked (should not appear as available)
curl -X GET "http://localhost:8080/slots/available/1?date=2026-05-15"

# 5. View upcoming appointments on patient dashboard
curl -X GET http://localhost:8080/appointments/patient/1/upcoming \
  -H "Authorization: Bearer <PATIENT_TOKEN>"

# 6. Doctor views their schedule for that day
curl -X GET "http://localhost:8080/appointments/provider/1/date?date=2026-05-15" \
  -H "Authorization: Bearer <DOCTOR_TOKEN>"

# 7. Doctor marks appointment as completed
curl -X PUT http://localhost:8080/appointments/1/complete \
  -H "Authorization: Bearer <DOCTOR_TOKEN>"

# 8. Verify final status
curl -X GET http://localhost:8080/appointments/1 \
  -H "Authorization: Bearer <PATIENT_TOKEN>"
```

<img src="https://user-images.githubusercontent.com/73097560/115834477-dbab4500-a447-11eb-908a-139a6edaec5c.gif" width="100%">

## ❌ Error Responses

All exceptions are caught by `GlobalExceptionHandler` (`@ControllerAdvice`) and return a consistent JSON structure:

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "This slot is already booked. Please choose another slot.",
  "timestamp": "2026-04-22T09:15:00"
}
```

| HTTP Status | Scenario |
|---|---|
| `400` | Slot already booked / blocked, provider mismatch, cancelling completed, rescheduling non-scheduled |
| `401` | Missing or invalid JWT token (rejected at gateway) |
| `403` | Insufficient role / forbidden access |
| `404` | Appointment not found for given ID |
| `409` | Duplicate resource conflict |
| `500` | Unexpected server error |

**Common error messages:**

| Scenario | Error Message |
|---|---|
| Slot already booked | `"This slot is already booked. Please choose another slot."` |
| Slot blocked by doctor | `"This slot is blocked by the doctor."` |
| Provider mismatch | `"Slot does not belong to the selected provider."` |
| Cancel completed appointment | `"Cannot cancel a completed appointment."` |
| Cancel already cancelled | `"Appointment is already cancelled."` |
| Reschedule non-SCHEDULED | `"Only SCHEDULED appointments can be rescheduled."` |
| New slot already booked | `"New slot is already booked."` |
| Complete non-SCHEDULED | `"Only SCHEDULED appointments can be marked complete."` |
| Appointment not found | `"Appointment not found with id: <appointmentId>"` |

<img src="https://user-images.githubusercontent.com/73097560/115834477-dbab4500-a447-11eb-908a-139a6edaec5c.gif" width="100%">

## ⚙️ Environment Variables

| Variable | Required | Default (dev) | Description |
|---|---|---|---|
| `JWT_SECRET` | **Yes** | — | Must match `api-gateway` and `auth-service`. Strong Base64 (min 256-bit) |
| `DB_USERNAME` | No | `medibook_user` | MySQL username |
| `DB_PASSWORD` | No | `medibook_pass` | MySQL password |
| `EUREKA_DEFAULT_ZONE` | No | `http://admin:medibook123@localhost:8761/eureka/` | Eureka registry URL |
| RabbitMQ host | No | `localhost` | `spring.rabbitmq.host` in `application.yml` |
| RabbitMQ port | No | `5672` | `spring.rabbitmq.port` in `application.yml` |
| RabbitMQ credentials | No | `guest / guest` | Default for local dev |

**Export in bash:**

```bash
export JWT_SECRET="myVeryStrongBase64SecretKeyForMediBookThatIs256BitsLong"
export DB_USERNAME="medibook_user"
export DB_PASSWORD="medibook_pass"
```

<img src="https://user-images.githubusercontent.com/73097560/115834477-dbab4500-a447-11eb-908a-139a6edaec5c.gif" width="100%">

## 🚀 Running the Services

### Prerequisites

- Java 17+
- Maven 3.8+
- MySQL 8 running locally
- RabbitMQ running locally
- All upstream services running (Eureka, Gateway, Auth, Provider, Schedule)

### Start RabbitMQ (Docker)

```bash
docker run -d \
  --name rabbitmq \
  -p 5672:5672 \
  -p 15672:15672 \
  rabbitmq:3-management
```

RabbitMQ Management UI: `http://localhost:15672` (guest / guest)

### Startup Order

```bash
# 1. Eureka Server — MUST be first
cd eureka-server && mvn spring-boot:run

# 2. API Gateway — MUST be second
cd api-gateway && JWT_SECRET=<secret> mvn spring-boot:run

# 3. Auth Service
cd auth-service && JWT_SECRET=<secret> mvn spring-boot:run

# 4. Provider Service
cd provider-service && JWT_SECRET=<secret> mvn spring-boot:run

# 5. Schedule Service — appointment-service calls this via Feign
cd schedule-service && JWT_SECRET=<secret> mvn spring-boot:run

# 6. Appointment Service — THIS SERVICE
cd appointment-service && JWT_SECRET=<secret> mvn spring-boot:run
```

### Build and Run as JAR

```bash
cd appointment-service
mvn clean package -DskipTests

java -jar target/appointment-service-1.0.0.jar \
  --jwt.secret=<JWT_SECRET> \
  --spring.datasource.username=medibook_user \
  --spring.datasource.password=medibook_pass
```

<img src="https://user-images.githubusercontent.com/73097560/115834477-dbab4500-a447-11eb-908a-139a6edaec5c.gif" width="100%">

## 🗄️ Database Setup

```sql
-- Create all required databases
CREATE DATABASE IF NOT EXISTS appointment_db;
CREATE DATABASE IF NOT EXISTS auth_db;
CREATE DATABASE IF NOT EXISTS provider_db;
CREATE DATABASE IF NOT EXISTS schedule_db;

-- Create shared user
CREATE USER IF NOT EXISTS 'medibook_user'@'localhost' IDENTIFIED BY 'medibook_pass';

-- Grant permissions
GRANT ALL PRIVILEGES ON appointment_db.* TO 'medibook_user'@'localhost';
GRANT ALL PRIVILEGES ON auth_db.*        TO 'medibook_user'@'localhost';
GRANT ALL PRIVILEGES ON provider_db.*    TO 'medibook_user'@'localhost';
GRANT ALL PRIVILEGES ON schedule_db.*    TO 'medibook_user'@'localhost';

FLUSH PRIVILEGES;
```

Hibernate `ddl-auto: update` auto-creates the `appointments` table on first startup.

**Expected table:**

```sql
CREATE TABLE appointments (
  appointment_id        INT AUTO_INCREMENT PRIMARY KEY,
  patient_id            INT          NOT NULL,
  provider_id           INT          NOT NULL,
  patient_email         VARCHAR(255),
  slot_id               INT          NOT NULL,
  service_type          VARCHAR(255) NOT NULL,
  appointment_date      DATE         NOT NULL,
  start_time            TIME         NOT NULL,
  end_time              TIME         NOT NULL,
  status                VARCHAR(50)  NOT NULL DEFAULT 'SCHEDULED',
  notes                 TEXT,
  mode_of_consultation  VARCHAR(50)  NOT NULL,
  created_at            DATETIME     NOT NULL,
  updated_at            DATETIME
);
```

<img src="https://user-images.githubusercontent.com/73097560/115834477-dbab4500-a447-11eb-908a-139a6edaec5c.gif" width="100%">

## 📖 Swagger UI

Direct access to each service's Swagger UI (bypassing gateway):

| Service | URL |
|---|---|
| **appointment-service** | http://localhost:8084/swagger-ui.html |
| schedule-service | http://localhost:8083/swagger-ui.html |
| auth-service | http://localhost:8081/swagger-ui.html |
| provider-service | http://localhost:8082/swagger-ui.html |

OpenAPI JSON docs available at `/api-docs` on each service.

<img src="https://user-images.githubusercontent.com/73097560/115834477-dbab4500-a447-11eb-908a-139a6edaec5c.gif" width="100%">

<div align="center">

<img src="https://capsule-render.vercel.app/api?type=waving&color=gradient&customColorList=6,11,20&height=100&section=footer" width="100%"/>

**MediBook Microservices — UC4 Appointment Service**

Made with ❤️ · Spring Boot 3.2 · Java 17 · RabbitMQ · OpenFeign · Eureka

![MIT License](https://img.shields.io/badge/License-MIT-yellow?style=flat-square)
![UC4](https://img.shields.io/badge/Feature-UC4_Appointment-brightgreen?style=flat-square)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.2.0-6DB33F?style=flat-square&logo=springboot)

</div>