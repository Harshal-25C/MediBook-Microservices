<div align="center">

<img src="https://capsule-render.vercel.app/api?type=waving&color=0:0f0c29,50:302b63,100:24243e&height=220&section=header&text=🔔%20MediBook%20Notification%20Service&fontSize=38&fontColor=ffffff&animation=fadeIn&fontAlignY=40&desc=UC7%20·%20RabbitMQ%20Consumer%20·%20Email%20·%20In-App%20·%20Spring%20Boot%203.2&descAlignY=60&descAlign=50" width="100%"/>

<br/>

<a href="#">
  <img src="https://readme-typing-svg.demolab.com?font=JetBrains+Mono&size=20&duration=2800&pause=900&color=A78BFA&center=true&vCenter=true&multiline=true&width=700&height=70&lines=Consume+RabbitMQ+Events+→+Store+In-App+Alerts;Send+Emails+via+Gmail+SMTP+·+Smart+Badge+Count;BOOKING+·+CANCELLATION+·+PAYMENT+·+REMINDER" alt="Typing SVG" />
</a>

<br/>

![Java](https://img.shields.io/badge/Java_17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot_3.2-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![RabbitMQ](https://img.shields.io/badge/RabbitMQ_Consumer-FF6600?style=for-the-badge&logo=rabbitmq&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL_8-4479A1?style=for-the-badge&logo=mysql&logoColor=white)
![Gmail](https://img.shields.io/badge/Gmail_SMTP-EA4335?style=for-the-badge&logo=gmail&logoColor=white)
![Swagger](https://img.shields.io/badge/Swagger_UI-85EA2D?style=for-the-badge&logo=swagger&logoColor=black)

<br/>

![Port](https://img.shields.io/badge/PORT-8087-blueviolet?style=flat-square)
![DB](https://img.shields.io/badge/DB-notification__db-blueviolet?style=flat-square)
![UC7](https://img.shields.io/badge/UC7-Notification_Service-brightgreen?style=flat-square)
![License](https://img.shields.io/badge/License-MIT-yellow?style=flat-square)
![Queues](https://img.shields.io/badge/Queues-3_RabbitMQ-orange?style=flat-square)
![Channels](https://img.shields.io/badge/Channels-APP_%7C_EMAIL_%7C_SMS-purple?style=flat-square)

</div>

---

<img src="https://user-images.githubusercontent.com/73097560/115834477-dbab4500-a447-11eb-908a-139a6edaec5c.gif" width="100%">

## 📋 Table of Contents

- [Overview](#-overview)
- [Complete System Architecture](#-complete-system-architecture)
- [Full Service Port Map](#-full-service-port-map)
- [RabbitMQ Event Flow](#-rabbitmq-event-flow)
- [Notification Service Deep Dive](#-notification-service-deep-dive)
  - [Tech Stack](#tech-stack)
  - [Project Structure](#project-structure)
  - [Entity: Notification](#entity-notification)
  - [DTOs](#dtos)
  - [RabbitMQ Consumer](#rabbitmq-consumer--appointmenteventconsumer)
  - [Feign Client — UserClient](#feign-client--userclient)
  - [Email Integration](#email-integration--gmail-smtp)
  - [Business Logic Rules](#business-logic-rules)
- [API Endpoints Summary](#-api-endpoints-summary)
- [API Testing via Gateway](#-api-testing-via-api-gateway)
- [Error Responses](#-error-responses)
- [Environment Variables](#-environment-variables)
- [Running the Services](#-running-the-services)
- [Database Setup](#-database-setup)
- [Swagger UI](#-swagger-ui)

<img src="https://user-images.githubusercontent.com/73097560/115834477-dbab4500-a447-11eb-908a-139a6edaec5c.gif" width="100%">

## 🔔 Overview

The **Notification Service** is the **UC7** microservice in the MediBook Online Appointment Booking System. It is the **sole consumer** of all appointment lifecycle events from RabbitMQ and is responsible for delivering alerts to users across multiple channels.

```
appointment-service publishes → RabbitMQ (3 queues)
                               ↓
                  notification-service CONSUMES
                               ↓
            ┌──────────────────┼───────────────────┐
            ▼                  ▼                   ▼
       APP Channel         EMAIL Channel       SMS Channel
   (saved to DB)       (Gmail SMTP sends)   (mock / Twilio later)
   shown in bell        real inbox email     phone message
```

**Key responsibilities:**

- **RabbitMQ Consumer** — listens on 3 queues: `appointment.booked`, `appointment.cancelled`, `appointment.completed`
- **In-App Notifications** — stores notifications in `notification_db`, serves them via REST API (bell icon, badge count)
- **Email Notifications** — sends real emails via Gmail SMTP using `JavaMailSender`
- **SMS Notifications** — mock implementation (Twilio-ready), toggled via config flag
- **Bulk Notifications** — admin endpoint to announce to many users at once
- **Read/Unread tracking** — supports per-user unread badge count and mark-as-read
- **Feign Client** — fetches user email from `auth-service` when sending EMAIL channel notifications

<img src="https://user-images.githubusercontent.com/73097560/115834477-dbab4500-a447-11eb-908a-139a6edaec5c.gif" width="100%">

## 🏗️ Complete System Architecture

```
╔══════════════════════════════════════════════════════════════════════════╗
║                        CLIENT (Browser / Mobile App)                    ║
╚═══════════════════════════════════╦════════════════════════════════════╝
                                    ║ All HTTP traffic
                                    ▼
╔══════════════════════════════════════════════════════════════════════════╗
║                     API GATEWAY  :8080                                  ║
║  • JwtAuthenticationFilter (Global)                                     ║
║  • Forwards: X-User-Id · X-User-Role · X-User-Email                    ║
║  • CORS: localhost:5173, localhost:5174                                  ║
║  Routes:                                                                ║
║    /auth/**        → auth-service        :8081                          ║
║    /providers/**   → provider-service    :8082                          ║
║    /slots/**       → schedule-service    :8083                          ║
║    /appointments/**→ appointment-service :8084                          ║
║    /payments/**    → payment-service     :8085                          ║
║    /reviews/**     → review-service      :8086                          ║
║    /notifications/**→ notification-service:8087  ◄── THIS SERVICE       ║
║    /records/**     → record-service      :8088                          ║
╚═════════════════════════════════╦════════════════════════════════════════╝
                                  ║ lb:// (Eureka)
        ┌─────────────────────────┼─────────────────────────────────┐
        ▼                         ▼                                 ▼
 ┌─────────────┐      ┌────────────────────┐            ┌──────────────────┐
 │auth-service │      │ appointment-service│            │schedule-service  │
 │  :8081      │◄─────┤    :8084           ├───Feign───►│  :8083           │
 │  auth_db    │      │  appointment_db    │            │  schedule_db     │
 └─────────────┘      └─────────┬──────────┘            └──────────────────┘
        ▲                       │ RabbitMQ PUBLISH
        │ Feign                 ▼
        │             ┌──────────────────────────────────┐
        │             │        RabbitMQ  :5672           │
        │             │    Exchange: medibook.exchange    │
        │             │    (TopicExchange, durable)       │
        │             │                                  │
        │             │  appointment.booked    ──────┐   │
        │             │  appointment.cancelled ──────┤   │
        │             │  appointment.completed ──────┘   │
        │             └──────────────┬───────────────────┘
        │                            │ @RabbitListener CONSUME
        │                            ▼
        │             ┌──────────────────────────────────┐
        └─────────────┤    notification-service  :8087   │
                      │       notification_db             │
                      │                                  │
                      │  ┌─────────────────────────────┐ │
                      │  │ AppointmentEventConsumer     │ │
                      │  │ • handleBooked()             │ │
                      │  │ • handleCancelled()          │ │
                      │  │ • handleCompleted()          │ │
                      │  └─────────────────────────────┘ │
                      │                                  │
                      │  Channels:                       │
                      │  APP   → saved to DB             │
                      │  EMAIL → Gmail SMTP              │
                      │  SMS   → Mock (Twilio-ready)     │
                      └──────────────────────────────────┘

╔══════════════════════════════════════════════════════════════════════════╗
║                    EUREKA SERVER  :8761                                 ║
║           Service Registry  ·  admin / medibook123                     ║
║           Dashboard: http://localhost:8761                              ║
╚══════════════════════════════════════════════════════════════════════════╝
```

<img src="https://user-images.githubusercontent.com/73097560/115834477-dbab4500-a447-11eb-908a-139a6edaec5c.gif" width="100%">

## 🗺️ Full Service Port Map

| Service | Port | Database | Role in System |
|---|---|---|---|
| `eureka-server` | **8761** | — | Service registry — Start **first** |
| `api-gateway` | **8080** | — | Single entry point — Start **second** |
| `auth-service` | **8081** | `auth_db` | JWT, OTP, OAuth2, user profiles |
| `provider-service` | **8082** | `provider_db` | Doctor profiles (UC2) |
| `schedule-service` | **8083** | `schedule_db` | Availability slots (UC3) |
| `appointment-service` | **8084** | `appointment_db` | Appointments — **RabbitMQ PUBLISHER** (UC4) |
| `payment-service` | **8085** | `payment_db` | Payments (UC5) |
| `review-service` | **8086** | `review_db` | Reviews (UC6) |
| `notification-service` | **8087** | `notification_db` | **← This service (UC7) — RabbitMQ CONSUMER** |
| `record-service` | **8088** | `record_db` | Medical records (UC8) |

<img src="https://user-images.githubusercontent.com/73097560/115834477-dbab4500-a447-11eb-908a-139a6edaec5c.gif" width="100%">

## 🐇 RabbitMQ Event Flow

This is the core of UC7. Understanding this flow is critical.

### Producer Side — `appointment-service`

```java
// Exchange declared in appointment-service RabbitMQConfig.java
Exchange:  medibook.exchange  (TopicExchange, durable=true)

Queue 1:   medibook.appointment.booked     ← routing key: appointment.booked
Queue 2:   medibook.appointment.cancelled  ← routing key: appointment.cancelled
Queue 3:   medibook.appointment.completed  ← routing key: appointment.completed
```

### Consumer Side — `notification-service`

```java
// AppointmentEventConsumer.java
@RabbitListener(queues = "medibook.appointment.booked")
public void handleBooked(AppointmentEventDto event)

@RabbitListener(queues = "medibook.appointment.cancelled")
public void handleCancelled(AppointmentEventDto event)

@RabbitListener(queues = "medibook.appointment.completed")
public void handleCompleted(AppointmentEventDto event)
```

### End-to-End Event Flow

```
appointment-service                 RabbitMQ                 notification-service
       │                               │                             │
       │  bookAppointment()            │                             │
       ├─── publishBooked() ──────────►│                             │
       │   routing: appointment.booked │                             │
       │                               ├── deliver to consumer ─────►│
       │                               │                             │ handleBooked()
       │                               │                             │  → save APP notification
       │                               │                             │  → if EMAIL: send Gmail
       │                               │                             │
       │  cancelAppointment()          │                             │
       ├─── publishCancelled() ───────►│                             │
       │  routing: appointment.cancelled                             │
       │                               ├── deliver to consumer ─────►│
       │                               │                             │ handleCancelled()
       │                               │                             │  → save APP notification
       │                               │                             │
       │  completeAppointment()        │                             │
       ├─── publishCompleted() ───────►│                             │
       │  routing: appointment.completed                             │
       │                               ├── deliver to consumer ─────►│
       │                               │                             │ handleCompleted()
       │                               │                             │  → save APP notification
```

### Event Payload — `AppointmentEventDto`

```json
{
  "appointmentId": 1,
  "patientId": 1,
  "providerId": 1,
  "eventType": "BOOKED",
  "serviceType": "General Consultation",
  "modeOfConsultation": "IN_PERSON",
  "appointmentDate": "2026-05-15",
  "startTime": "10:00",
  "endTime": "10:30",
  "message": "Your appointment on 2026-05-15 at 10:00"
}
```

### What Each Handler Creates

| Queue | Event Type | Title Created | Notification Type | Channel |
|---|---|---|---|---|
| `medibook.appointment.booked` | `BOOKED` | "Appointment Confirmed!" | `BOOKING` | `APP` |
| `medibook.appointment.cancelled` | `CANCELLED` | "Appointment Cancelled" | `CANCELLATION` | `APP` |
| `medibook.appointment.completed` | `COMPLETED` | "Appointment Completed" | `BOOKING` | `APP` |

<img src="https://user-images.githubusercontent.com/73097560/115834477-dbab4500-a447-11eb-908a-139a6edaec5c.gif" width="100%">

## 🔬 Notification Service Deep Dive

### Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.2.0 |
| Cloud | Spring Cloud 2023.0.0 (Eureka Client, OpenFeign) |
| Database | MySQL 8 — `notification_db` |
| ORM | Spring Data JPA / Hibernate |
| Messaging | Spring AMQP / RabbitMQ (`@RabbitListener` — CONSUMER) |
| Email | Spring Boot Mail + JavaMailSender (Gmail SMTP) |
| SMS | Mock implementation (Twilio-ready, disabled by default) |
| Security | Spring Security (stateless, JWT validated at gateway) |
| Docs | Springdoc OpenAPI 2.3.0 (Swagger UI) |
| Serialization | Jackson2JsonMessageConverter (JSON over RabbitMQ) |
| Build | Maven 3 |
| Lombok | 1.18.30 |
| JWT | JJWT 0.11.5 |

### Project Structure

```
notification-service/
└── src/main/java/com/medibook/notification/
    ├── NotificationServiceApplication.java      # Main — @EnableRabbit, @EnableFeignClients
    ├── client/
    │   └── UserClient.java                      # Feign → auth-service (/auth/profile/{userId})
    ├── config/
    │   ├── RabbitMQConfig.java                  # Jackson2JsonMessageConverter bean
    │   └── SecurityConfig.java                  # Stateless, all routes permitAll (JWT at gateway)
    ├── dto/
    │   ├── AppointmentEventDto.java             # RabbitMQ message shape (from appointment-service)
    │   ├── NotificationRequest.java             # REST API request DTO (with validation)
    │   └── UserDto.java                         # Feign response from auth-service
    ├── entity/
    │   └── Notification.java                    # JPA entity → notifications table
    ├── exception/
    │   ├── BadRequestException.java             # 400
    │   ├── ResourceNotFoundException.java       # 404
    │   ├── DuplicateResourceException.java      # 409
    │   ├── ForbiddenException.java              # 403
    │   ├── UnauthorizedException.java           # 401
    │   ├── ErrorResponse.java                   # Standardized error shape
    │   └── GlobalExceptionHandler.java          # @ControllerAdvice catches all
    ├── messaging/
    │   └── AppointmentEventConsumer.java        # @RabbitListener on 3 queues
    ├── repository/
    │   └── NotificationRepository.java          # JPA repo with custom JPQL queries
    ├── resource/
    │   └── NotificationResource.java            # REST controller — /notifications/**
    └── service/
        ├── NotificationService.java             # Interface contract
        └── impl/
            └── NotificationServiceImpl.java     # Business logic + email sending
```

### Entity: Notification

Maps to the `notifications` table in `notification_db`.

| Column | Type | Nullable | Default | Description |
|---|---|---|---|---|
| `notificationId` | INT (PK, AI) | No | — | Auto-generated primary key |
| `recipientId` | INT | No | — | User ID of the notification recipient |
| `type` | VARCHAR | No | — | `BOOKING` / `REMINDER` / `CANCELLATION` / `PAYMENT` / `FOLLOWUP` / `ANNOUNCEMENT` |
| `title` | VARCHAR | No | — | Short heading shown in notification bell |
| `message` | VARCHAR(1000) | No | — | Full notification message body |
| `channel` | VARCHAR | No | — | `APP` / `EMAIL` / `SMS` |
| `relatedId` | INT | Yes | 0 | ID of the linked record (e.g. `appointmentId`) |
| `relatedType` | VARCHAR | Yes | — | Type of linked record: `APPOINTMENT` / `PAYMENT` / `RECORD` |
| `isRead` | BOOLEAN | No | `false` | `false` = unread (shown in badge); `true` = read |
| `sentAt` | DATETIME | No | auto | Timestamp set automatically via `@PrePersist` |

### DTOs

**`NotificationRequest`** — REST API input for manual/programmatic sends:

| Field | Type | Required | Validation | Description |
|---|---|---|---|---|
| `recipientId` | int | Yes | `@NotNull` | Target user ID |
| `type` | String | Yes | `@NotBlank` | `BOOKING` / `REMINDER` / `CANCELLATION` / `PAYMENT` / `FOLLOWUP` / `ANNOUNCEMENT` |
| `email` | String | No | — | Override email address (skips Feign lookup to auth-service) |
| `title` | String | Yes | `@NotBlank` | Short notification heading |
| `message` | String | Yes | `@NotBlank` | Full notification body text |
| `channel` | String | No | `APP` | `APP` / `EMAIL` / `SMS` |
| `relatedId` | int | No | 0 | ID of linked record for deep-linking |
| `relatedType` | String | No | — | Type of linked record |

**`AppointmentEventDto`** — shape of RabbitMQ messages consumed from `appointment-service`:

| Field | Description |
|---|---|
| `appointmentId` | The appointment that triggered this event |
| `patientId` | Used as `recipientId` for the notification |
| `providerId` | Doctor involved |
| `eventType` | `BOOKED` / `CANCELLED` / `COMPLETED` |
| `serviceType` | Medical service type |
| `modeOfConsultation` | `IN_PERSON` or `TELECONSULTATION` |
| `appointmentDate` | Date string for the message body |
| `startTime` / `endTime` | Time strings |
| `message` | Pre-built text from `appointment-service` |

**`UserDto`** — Feign response from `auth-service`:

| Field | Description |
|---|---|
| `userId` | User ID |
| `fullName` | User's full name |
| `email` | Used to send email when `channel=EMAIL` and no `email` field in request |
| `phone` | For SMS channel |
| `role` | `PATIENT` / `DOCTOR` / `ADMIN` |
| `isActive` | Account status |

### RabbitMQ Consumer — `AppointmentEventConsumer`

Three `@RabbitListener` methods, one per queue:

```java
@RabbitListener(queues = "medibook.appointment.booked")
public void handleBooked(AppointmentEventDto event)
// Creates: type=BOOKING, title="Appointment Confirmed!"
// Message: "Your appointment is confirmed for {date} at {startTime}"

@RabbitListener(queues = "medibook.appointment.cancelled")
public void handleCancelled(AppointmentEventDto event)
// Creates: type=CANCELLATION, title="Appointment Cancelled"
// Message: "Your appointment on {date} has been cancelled."

@RabbitListener(queues = "medibook.appointment.completed")
public void handleCompleted(AppointmentEventDto event)
// Creates: type=BOOKING, title="Appointment Completed"
// Message: "Your appointment has been completed. Thank you for choosing MediBook!"
```

All three create a `NotificationRequest` and call `notificationService.send()` — the same path as a manual REST API call, meaning they go through the same validation and channel routing.

### Feign Client — `UserClient`

Used to resolve user email when `channel=EMAIL` is requested but no `email` field is provided in the request.

```java
@FeignClient(name = "auth-service")
public interface UserClient {
    @GetMapping("/auth/profile/{userId}")
    UserDto getUserById(@PathVariable("userId") int userId);
}
```

Only called during `send()` when `channel=EMAIL` and `request.getEmail()` is null or empty.

### Email Integration — Gmail SMTP

Configured in `application.yml`:

```yaml
spring:
  mail:
    host: smtp.gmail.com
    port: 587
    username: ${MAIL_USERNAME}   # your Gmail address
    password: ${MAIL_PASSWORD}   # Gmail App Password (NOT your account password)
    properties:
      mail.smtp.auth: true
      mail.smtp.starttls.enable: true
```

**Setup steps for Gmail:**
1. Enable 2-Factor Authentication on your Google account
2. Go to Google Account → Security → App Passwords
3. Generate an App Password for "Mail"
4. Use that 16-character password as `MAIL_PASSWORD`

Email send path in `NotificationServiceImpl.send()`:
```
channel == "EMAIL" AND emailEnabled == true
    → if request.email != null → use it directly
    → else → Feign call to auth-service → get user.email
    → SimpleMailMessage → JavaMailSender.send()
    → on failure: logs error, does NOT throw (non-blocking)
```

**Email is toggled globally:**
```yaml
notification:
  email:
    enabled: true   # set to false to disable all email sending
  sms:
    enabled: false  # SMS is mock-only for now
```

### Business Logic Rules

| Operation | Validation / Guards |
|---|---|
| `send()` | `channel` must be `APP`, `EMAIL`, or `SMS`; `type` must be one of the 6 valid types |
| `markAsRead()` | Throws `BadRequestException` if notification is already read |
| `sendBulk()` | `recipientIds` list cannot be null/empty; title and message cannot be blank |
| `sendEmail()` | `toEmail` cannot be null/empty; wraps `JavaMailSender` in try-catch (never crashes on email failure) |
| `sendSms()` | `phoneNumber` cannot be null/empty; currently logs only (mock) |
| `deleteNotification()` | Throws `ResourceNotFoundException` if notification not found |
| Email fallback | If `email` field not in request → Feign to auth-service for user email; if that also fails → error logged, app notification still saved |

<img src="https://user-images.githubusercontent.com/73097560/115834477-dbab4500-a447-11eb-908a-139a6edaec5c.gif" width="100%">

## 📡 API Endpoints Summary

**Base URL (via gateway):** `http://localhost:8080`
**Base URL (direct):** `http://localhost:8087`

All endpoints are prefixed with `/notifications`.

| Method | Endpoint | Auth | Who Calls | Description |
|---|---|---|---|---|
| `POST` | `/notifications/send` | Required | Any service / Admin | Send a single notification (APP, EMAIL, or SMS) |
| `POST` | `/notifications/bulk` | Required | Admin | Send announcement to multiple users |
| `GET` | `/notifications/recipient/{recipientId}` | Required | Patient / Doctor | Get all notifications for a user (newest first) |
| `GET` | `/notifications/unread/count/{recipientId}` | Required | UI (bell icon) | Get unread notification badge count |
| `PUT` | `/notifications/{notificationId}/read` | Required | Patient | Mark one notification as read |
| `PUT` | `/notifications/read/all/{recipientId}` | Required | Patient | Mark ALL notifications as read |
| `DELETE` | `/notifications/{notificationId}` | Required | Patient | Delete a notification |
| `GET` | `/notifications/all` | Required | Admin | Get all platform notifications (admin log) |

<img src="https://user-images.githubusercontent.com/73097560/115834477-dbab4500-a447-11eb-908a-139a6edaec5c.gif" width="100%">

## 🧪 API Testing via API Gateway

> All examples use `http://localhost:8080` (API Gateway).
> Replace `<YOUR_JWT_TOKEN>` with a token from auth-service login.

---

### 🔐 Step 0 — Get a JWT Token

```bash
# Register
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Priya Patel",
    "email": "priya.patel@medibook.com",
    "password": "Patient@123",
    "phone": "9876543210",
    "role": "PATIENT"
  }'

# Login
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "priya.patel@medibook.com",
    "password": "Patient@123"
  }'
```

**Sample Response:**

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "type": "Bearer",
  "userId": 1,
  "email": "priya.patel@medibook.com",
  "role": "PATIENT"
}
```

---

### 1️⃣ Send an In-App Notification (APP channel)

**`POST /notifications/send`**

Creates and stores a notification in the `notifications` table. Recipient sees it in their notification bell. This is the most common channel — all RabbitMQ-driven notifications use `APP`.

```bash
curl -X POST http://localhost:8080/notifications/send \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <YOUR_JWT_TOKEN>" \
  -d '{
    "recipientId": 1,
    "type": "BOOKING",
    "title": "Appointment Confirmed!",
    "message": "Your appointment with Dr. Sharma is confirmed for 2026-05-15 at 10:00 AM.",
    "channel": "APP",
    "relatedId": 1,
    "relatedType": "APPOINTMENT"
  }'
```

**Expected Response — 201 Created:**

```json
{
  "notificationId": 1,
  "recipientId": 1,
  "type": "BOOKING",
  "title": "Appointment Confirmed!",
  "message": "Your appointment with Dr. Sharma is confirmed for 2026-05-15 at 10:00 AM.",
  "channel": "APP",
  "relatedId": 1,
  "relatedType": "APPOINTMENT",
  "read": false,
  "sentAt": "2026-04-22T10:00:00"
}
```

---

### 2️⃣ Send an Email Notification

**`POST /notifications/send`**

Sends a real email via Gmail SMTP **and** saves the notification to DB. Requires `MAIL_USERNAME` and `MAIL_PASSWORD` environment variables to be set.

```bash
curl -X POST http://localhost:8080/notifications/send \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <YOUR_JWT_TOKEN>" \
  -d '{
    "recipientId": 1,
    "type": "BOOKING",
    "email": "priya.patel@medibook.com",
    "title": "Appointment Confirmation — MediBook",
    "message": "Dear Priya, your appointment with Dr. Sharma on 2026-05-15 at 10:00 AM has been confirmed. Mode: In-Person. Please arrive 10 minutes early.",
    "channel": "EMAIL",
    "relatedId": 1,
    "relatedType": "APPOINTMENT"
  }'
```

**Expected Response — 201 Created:**

```json
{
  "notificationId": 2,
  "recipientId": 1,
  "type": "BOOKING",
  "title": "Appointment Confirmation — MediBook",
  "message": "Dear Priya, your appointment with Dr. Sharma on 2026-05-15 at 10:00 AM has been confirmed...",
  "channel": "EMAIL",
  "relatedId": 1,
  "relatedType": "APPOINTMENT",
  "read": false,
  "sentAt": "2026-04-22T10:01:00"
}
```

> **Note:** If `email` field is omitted, the service calls `auth-service` via Feign to fetch the user's email from their profile.

---

### 3️⃣ Send a Cancellation Notification

**`POST /notifications/send`**

```bash
curl -X POST http://localhost:8080/notifications/send \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <YOUR_JWT_TOKEN>" \
  -d '{
    "recipientId": 1,
    "type": "CANCELLATION",
    "title": "Appointment Cancelled",
    "message": "Your appointment on 2026-05-15 at 10:00 AM has been cancelled. The slot has been released.",
    "channel": "APP",
    "relatedId": 1,
    "relatedType": "APPOINTMENT"
  }'
```

**Expected Response — 201 Created:**

```json
{
  "notificationId": 3,
  "recipientId": 1,
  "type": "CANCELLATION",
  "title": "Appointment Cancelled",
  "message": "Your appointment on 2026-05-15 at 10:00 AM has been cancelled. The slot has been released.",
  "channel": "APP",
  "relatedId": 1,
  "relatedType": "APPOINTMENT",
  "read": false,
  "sentAt": "2026-04-22T10:02:00"
}
```

---

### 4️⃣ Send a Payment Receipt Notification

**`POST /notifications/send`**

```bash
curl -X POST http://localhost:8080/notifications/send \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <YOUR_JWT_TOKEN>" \
  -d '{
    "recipientId": 1,
    "type": "PAYMENT",
    "title": "Payment Successful",
    "message": "Payment of ₹500 received for your appointment on 2026-05-15. Transaction ID: TXN20260422001.",
    "channel": "APP",
    "relatedId": 101,
    "relatedType": "PAYMENT"
  }'
```

**Expected Response — 201 Created:**

```json
{
  "notificationId": 4,
  "recipientId": 1,
  "type": "PAYMENT",
  "title": "Payment Successful",
  "message": "Payment of ₹500 received for your appointment on 2026-05-15. Transaction ID: TXN20260422001.",
  "channel": "APP",
  "relatedId": 101,
  "relatedType": "PAYMENT",
  "read": false,
  "sentAt": "2026-04-22T10:03:00"
}
```

---

### 5️⃣ Send a Reminder Notification

**`POST /notifications/send`**

```bash
curl -X POST http://localhost:8080/notifications/send \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <YOUR_JWT_TOKEN>" \
  -d '{
    "recipientId": 1,
    "type": "REMINDER",
    "title": "Appointment Tomorrow at 10:00 AM",
    "message": "Reminder: You have an appointment with Dr. Sharma tomorrow at 10:00 AM. Please be on time.",
    "channel": "APP",
    "relatedId": 1,
    "relatedType": "APPOINTMENT"
  }'
```

**Expected Response — 201 Created:**

```json
{
  "notificationId": 5,
  "recipientId": 1,
  "type": "REMINDER",
  "title": "Appointment Tomorrow at 10:00 AM",
  "message": "Reminder: You have an appointment with Dr. Sharma tomorrow at 10:00 AM. Please be on time.",
  "channel": "APP",
  "relatedId": 1,
  "relatedType": "APPOINTMENT",
  "read": false,
  "sentAt": "2026-04-22T10:04:00"
}
```

---

### 6️⃣ Send Bulk Notification (Admin Announcement)

**`POST /notifications/bulk`**

Admin broadcasts a platform-wide announcement to multiple users at once. Internally loops and calls `send()` for each `recipientId`.

```bash
curl -X POST http://localhost:8080/notifications/bulk \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <YOUR_JWT_TOKEN>" \
  -d '{
    "recipientIds": [1, 2, 3, 4, 5],
    "title": "Scheduled Maintenance — MediBook",
    "message": "MediBook will be under maintenance on 2026-04-25 from 2:00 AM to 4:00 AM IST. We apologise for any inconvenience."
  }'
```

**Expected Response — 200 OK:**

```json
{
  "message": "Bulk notification sent to 5 users."
}
```

> Each recipient gets an individual `ANNOUNCEMENT` type `APP` notification saved to their account.

---

### 7️⃣ Get All Notifications for a User

**`GET /notifications/recipient/{recipientId}`**

Returns all notifications for a user, ordered newest first. This powers the notification bell dropdown.

```bash
curl -X GET http://localhost:8080/notifications/recipient/1 \
  -H "Authorization: Bearer <YOUR_JWT_TOKEN>"
```

**Expected Response — 200 OK:**

```json
[
  {
    "notificationId": 5,
    "recipientId": 1,
    "type": "REMINDER",
    "title": "Appointment Tomorrow at 10:00 AM",
    "message": "Reminder: You have an appointment with Dr. Sharma tomorrow at 10:00 AM.",
    "channel": "APP",
    "relatedId": 1,
    "relatedType": "APPOINTMENT",
    "read": false,
    "sentAt": "2026-04-22T10:04:00"
  },
  {
    "notificationId": 4,
    "recipientId": 1,
    "type": "PAYMENT",
    "title": "Payment Successful",
    "message": "Payment of ₹500 received for your appointment on 2026-05-15.",
    "channel": "APP",
    "relatedId": 101,
    "relatedType": "PAYMENT",
    "read": false,
    "sentAt": "2026-04-22T10:03:00"
  },
  {
    "notificationId": 3,
    "recipientId": 1,
    "type": "CANCELLATION",
    "title": "Appointment Cancelled",
    "message": "Your appointment on 2026-05-15 at 10:00 AM has been cancelled.",
    "channel": "APP",
    "relatedId": 1,
    "relatedType": "APPOINTMENT",
    "read": true,
    "sentAt": "2026-04-22T10:02:00"
  }
]
```

---

### 8️⃣ Get Unread Notification Count (Bell Badge)

**`GET /notifications/unread/count/{recipientId}`**

Returns the count of unread notifications. Called on every page load to update the red badge number on the bell icon.

```bash
curl -X GET http://localhost:8080/notifications/unread/count/1 \
  -H "Authorization: Bearer <YOUR_JWT_TOKEN>"
```

**Expected Response — 200 OK:**

```json
{
  "recipientId": 1,
  "unreadCount": 4
}
```

> When `unreadCount` is 0, the badge is hidden. When > 0, shown as a red number on the bell icon.

---

### 9️⃣ Mark a Single Notification as Read

**`PUT /notifications/{notificationId}/read`**

Patient clicks a notification — marks it as read. Badge count decrements by 1.

```bash
curl -X PUT http://localhost:8080/notifications/5/read \
  -H "Authorization: Bearer <YOUR_JWT_TOKEN>"
```

**Expected Response — 200 OK:**

```json
{
  "message": "Notification marked as read."
}
```

**Error if already read — 400 Bad Request:**

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Notification is already marked as read.",
  "timestamp": "2026-04-22T10:15:00"
}
```

---

### 🔟 Mark All Notifications as Read

**`PUT /notifications/read/all/{recipientId}`**

Patient clicks "Mark all as read." All notifications for this user are set to `isRead = true`. Badge count drops to 0.

```bash
curl -X PUT http://localhost:8080/notifications/read/all/1 \
  -H "Authorization: Bearer <YOUR_JWT_TOKEN>"
```

**Expected Response — 200 OK:**

```json
{
  "message": "All notifications marked as read."
}
```

---

### 1️⃣1️⃣ Delete a Notification

**`DELETE /notifications/{notificationId}`**

Patient removes a notification from their list permanently.

```bash
curl -X DELETE http://localhost:8080/notifications/3 \
  -H "Authorization: Bearer <YOUR_JWT_TOKEN>"
```

**Expected Response — 200 OK:**

```json
{
  "message": "Notification deleted."
}
```

---

### 1️⃣2️⃣ Get All Notifications (Admin Log)

**`GET /notifications/all`**

Admin views all notifications across all users, ordered newest first. Used for platform-level monitoring.

```bash
curl -X GET http://localhost:8080/notifications/all \
  -H "Authorization: Bearer <YOUR_JWT_TOKEN>"
```

**Expected Response — 200 OK:**

```json
[
  {
    "notificationId": 5,
    "recipientId": 1,
    "type": "REMINDER",
    "title": "Appointment Tomorrow at 10:00 AM",
    "channel": "APP",
    "read": false,
    "sentAt": "2026-04-22T10:04:00"
  },
  {
    "notificationId": 4,
    "recipientId": 1,
    "type": "PAYMENT",
    "title": "Payment Successful",
    "channel": "APP",
    "read": false,
    "sentAt": "2026-04-22T10:03:00"
  }
]
```

---

### 1️⃣3️⃣ Simulate RabbitMQ BOOKED Event (End-to-End Test)

This shows the full async flow from booking an appointment to receiving a notification.

```bash
# Step 1 — Book an appointment (triggers appointment-service to publish to RabbitMQ)
curl -X POST http://localhost:8080/appointments/book \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <PATIENT_TOKEN>" \
  -d '{
    "patientId": 1,
    "providerId": 1,
    "patientEmail": "priya.patel@medibook.com",
    "slotId": 5,
    "serviceType": "General Consultation",
    "appointmentDate": "2026-05-15",
    "startTime": "10:00",
    "endTime": "10:30",
    "modeOfConsultation": "IN_PERSON"
  }'

# Step 2 — Confirm payment (triggers BOOKED event on RabbitMQ → notification-service consumes)
curl -X PUT "http://localhost:8080/appointments/1/status?status=CONFIRMED" \
  -H "Authorization: Bearer <PATIENT_TOKEN>"

# Step 3 — Check notification was auto-created (should see BOOKING notification)
curl -X GET http://localhost:8080/notifications/recipient/1 \
  -H "Authorization: Bearer <PATIENT_TOKEN>"

# Step 4 — Check badge count (should be > 0)
curl -X GET http://localhost:8080/notifications/unread/count/1 \
  -H "Authorization: Bearer <PATIENT_TOKEN>"

# Step 5 — Cancel appointment (triggers CANCELLED event → new notification)
curl -X PUT http://localhost:8080/appointments/1/cancel \
  -H "Authorization: Bearer <PATIENT_TOKEN>"

# Step 6 — Verify cancellation notification appeared
curl -X GET http://localhost:8080/notifications/recipient/1 \
  -H "Authorization: Bearer <PATIENT_TOKEN>"
```

---

### 1️⃣4️⃣ Send Follow-Up Reminder (from Medical Records UC8)

**`POST /notifications/send`**

Medical record service can trigger follow-up reminders.

```bash
curl -X POST http://localhost:8080/notifications/send \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <YOUR_JWT_TOKEN>" \
  -d '{
    "recipientId": 1,
    "type": "FOLLOWUP",
    "title": "Follow-Up Reminder",
    "message": "Dr. Sharma recommends a follow-up visit on 2026-06-15. Please book your appointment.",
    "channel": "APP",
    "relatedId": 1,
    "relatedType": "RECORD"
  }'
```

**Expected Response — 201 Created:**

```json
{
  "notificationId": 10,
  "recipientId": 1,
  "type": "FOLLOWUP",
  "title": "Follow-Up Reminder",
  "message": "Dr. Sharma recommends a follow-up visit on 2026-06-15. Please book your appointment.",
  "channel": "APP",
  "relatedId": 1,
  "relatedType": "RECORD",
  "read": false,
  "sentAt": "2026-04-22T11:00:00"
}
```

<img src="https://user-images.githubusercontent.com/73097560/115834477-dbab4500-a447-11eb-908a-139a6edaec5c.gif" width="100%">

## ❌ Error Responses

All errors are caught by `GlobalExceptionHandler` (`@ControllerAdvice`) and return:

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid channel. Allowed values: APP, EMAIL, SMS",
  "timestamp": "2026-04-22T10:30:00"
}
```

| HTTP Status | Scenario |
|---|---|
| `400` | Invalid `channel` or `type` value; already-read notification; empty bulk recipient list |
| `401` | Missing or invalid JWT token (rejected at gateway) |
| `403` | Insufficient role / forbidden access |
| `404` | Notification not found for given ID |
| `409` | Duplicate resource conflict |
| `500` | Unexpected server error |

**Common error messages:**

| Scenario | Error Message |
|---|---|
| Invalid channel | `"Invalid channel. Allowed values: APP, EMAIL, SMS"` |
| Invalid type | `"Invalid type."` |
| Already marked read | `"Notification is already marked as read."` |
| Empty recipient list | `"Recipient list cannot be empty."` |
| Empty title | `"Title cannot be empty."` |
| Empty message | `"Message cannot be empty."` |
| Notification not found | `"Notification not found with id: <notificationId>"` |
| Empty email | `"Recipient email cannot be empty."` |
| Empty phone | `"Phone number cannot be empty."` |

<img src="https://user-images.githubusercontent.com/73097560/115834477-dbab4500-a447-11eb-908a-139a6edaec5c.gif" width="100%">

## ⚙️ Environment Variables

| Variable | Required | Default (dev) | Description |
|---|---|---|---|
| `JWT_SECRET` | **Yes** | — | Must match `api-gateway` and `auth-service`. Strong Base64 (min 256-bit) |
| `DB_USERNAME` | No | `medibook_user` | MySQL username |
| `DB_PASSWORD` | No | `medibook_pass` | MySQL password |
| `MAIL_USERNAME` | **Yes** (for email) | — | Your Gmail address (e.g., `yourapp@gmail.com`) |
| `MAIL_PASSWORD` | **Yes** (for email) | — | Gmail App Password (16-char, NOT your account password) |
| `EUREKA_DEFAULT_ZONE` | No | `http://admin:medibook123@localhost:8761/eureka/` | Eureka URL |
| RabbitMQ host | No | `localhost` | `spring.rabbitmq.host` in `application.yml` |
| RabbitMQ port | No | `5672` | `spring.rabbitmq.port` in `application.yml` |
| RabbitMQ credentials | No | `guest / guest` | Default for local dev |
| Email enabled | No | `true` | `notification.email.enabled` in `application.yml` |
| SMS enabled | No | `false` | `notification.sms.enabled` in `application.yml` |

**Export in bash:**

```bash
export JWT_SECRET="myVeryStrongBase64SecretKeyForMediBookThatIs256BitsLong"
export DB_USERNAME="medibook_user"
export DB_PASSWORD="medibook_pass"
export MAIL_USERNAME="yourapp@gmail.com"
export MAIL_PASSWORD="abcd efgh ijkl mnop"   # Gmail App Password (with spaces is OK)
```

<img src="https://user-images.githubusercontent.com/73097560/115834477-dbab4500-a447-11eb-908a-139a6edaec5c.gif" width="100%">

## 🚀 Running the Services

### Prerequisites

- Java 17+
- Maven 3.8+
- MySQL 8 running locally
- RabbitMQ running locally
- Gmail account with App Password configured
- All upstream services running (Eureka → Gateway → Auth → Provider → Schedule → Appointment)

### Start RabbitMQ via Docker

```bash
docker run -d \
  --name rabbitmq \
  -p 5672:5672 \
  -p 15672:15672 \
  rabbitmq:3-management
```

RabbitMQ Management UI: `http://localhost:15672` (guest / guest)

You can verify the 3 queues (`medibook.appointment.booked`, `medibook.appointment.cancelled`, `medibook.appointment.completed`) appear here after both `appointment-service` and `notification-service` start.

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

# 5. Schedule Service
cd schedule-service && JWT_SECRET=<secret> mvn spring-boot:run

# 6. Appointment Service (RabbitMQ PUBLISHER — creates queues)
cd appointment-service && JWT_SECRET=<secret> mvn spring-boot:run

# 7. Notification Service — THIS SERVICE (RabbitMQ CONSUMER)
cd notification-service \
  && JWT_SECRET=<secret> \
  && MAIL_USERNAME=<gmail> \
  && MAIL_PASSWORD=<app-password> \
  mvn spring-boot:run
```

### Build and Run as JAR

```bash
cd notification-service
mvn clean package -DskipTests

java -jar target/notification-service-1.0.0.jar \
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
CREATE DATABASE IF NOT EXISTS notification_db;
CREATE DATABASE IF NOT EXISTS appointment_db;
CREATE DATABASE IF NOT EXISTS auth_db;
CREATE DATABASE IF NOT EXISTS provider_db;
CREATE DATABASE IF NOT EXISTS schedule_db;

-- Create shared user
CREATE USER IF NOT EXISTS 'medibook_user'@'localhost' IDENTIFIED BY 'medibook_pass';

-- Grant permissions
GRANT ALL PRIVILEGES ON notification_db.* TO 'medibook_user'@'localhost';
GRANT ALL PRIVILEGES ON appointment_db.*  TO 'medibook_user'@'localhost';
GRANT ALL PRIVILEGES ON auth_db.*         TO 'medibook_user'@'localhost';
GRANT ALL PRIVILEGES ON provider_db.*     TO 'medibook_user'@'localhost';
GRANT ALL PRIVILEGES ON schedule_db.*     TO 'medibook_user'@'localhost';

FLUSH PRIVILEGES;
```

Hibernate `ddl-auto: update` auto-creates the `notifications` table on first startup.

**Expected table:**

```sql
CREATE TABLE notifications (
  notification_id  INT AUTO_INCREMENT PRIMARY KEY,
  recipient_id     INT           NOT NULL,
  type             VARCHAR(50)   NOT NULL,
  title            VARCHAR(255)  NOT NULL,
  message          VARCHAR(1000) NOT NULL,
  channel          VARCHAR(20)   NOT NULL,
  related_id       INT           DEFAULT 0,
  related_type     VARCHAR(50),
  is_read          TINYINT(1)    NOT NULL DEFAULT 0,
  sent_at          DATETIME      NOT NULL
);
```

<img src="https://user-images.githubusercontent.com/73097560/115834477-dbab4500-a447-11eb-908a-139a6edaec5c.gif" width="100%">

## 📖 Swagger UI

Direct access to each service's Swagger UI (bypasses gateway):

| Service | Swagger URL |
|---|---|
| **notification-service** | http://localhost:8087/swagger-ui.html |
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

<img src="https://capsule-render.vercel.app/api?type=waving&color=0:24243e,50:302b63,100:0f0c29&height=120&section=footer" width="100%"/>

**MediBook Microservices — UC7 Notification Service**

`@RabbitListener` · `JavaMailSender` · `APP | EMAIL | SMS` · Spring Boot 3.2 · Java 17

![MIT License](https://img.shields.io/badge/License-MIT-yellow?style=flat-square)
![UC7](https://img.shields.io/badge/Feature-UC7_Notification-blueviolet?style=flat-square)
![RabbitMQ](https://img.shields.io/badge/RabbitMQ-Consumer-FF6600?style=flat-square&logo=rabbitmq)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.2.0-6DB33F?style=flat-square&logo=springboot)

</div>