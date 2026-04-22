<div align="center">

# ⭐ MediBook — Review Service

### `feature/UC6-review-service`

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.0-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://www.oracle.com/java/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?style=for-the-badge&logo=mysql&logoColor=white)](https://www.mysql.com/)
[![Feign](https://img.shields.io/badge/Feign-Clients_×2-00C7B7?style=for-the-badge&logo=spring&logoColor=white)](https://spring.io/projects/spring-cloud-openfeign)
[![Spring Security](https://img.shields.io/badge/Spring%20Security-JWT-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white)](https://spring.io/projects/spring-security)
[![Eureka](https://img.shields.io/badge/Eureka-Discovery-blue?style=for-the-badge&logo=spring&logoColor=white)](https://spring.io/projects/spring-cloud-netflix)
[![Actuator](https://img.shields.io/badge/Actuator-Health_%2F_Metrics-brightgreen?style=for-the-badge&logo=spring&logoColor=white)](https://docs.spring.io/spring-boot/docs/current/actuator-api/)
[![License](https://img.shields.io/badge/License-MIT-yellow?style=for-the-badge)](LICENSE)

> **UC6** · Doctor Rating Engine · Anonymous Reviews · Auto avgRating Sync · 2 Feign Clients

★★★★★

</div>

---

## 📋 Table of Contents

| # | Section |
|---|---------|
| 01 | [Project Overview](#-project-overview) |
| 02 | [System Architecture](#-system-architecture) |
| 03 | [All Microservices](#-all-microservices) |
| 04 | [Review Service Deep Dive](#-review-service-deep-dive) |
| 05 | [API Reference](#-api-reference) |
| 06 | [Review Submission Flow](#-review-submission-flow) |
| 07 | [Star Rating System](#-star-rating-system) |
| 08 | [API Testing Guide](#-api-testing-guide) |
| 09 | [Environment Variables](#-environment-variables) |
| 10 | [Quick Start](#-quick-start) |

---

## 🏥 Project Overview

**MediBook** is an Online Appointment Booking System built as a distributed microservices platform using Spring Boot, Spring Cloud, and Spring Security. Patients book appointments with healthcare providers, manage time slots, process payments, and submit post-appointment reviews — all coordinated through an API Gateway with JWT authentication.

The **UC6 Review Service** is the reputation engine of the platform. It enables patients to submit star ratings and written feedback after completed appointments, with full anonymous support. Every new review automatically recalculates and pushes the updated average rating to `provider-service` via Feign — keeping doctor profiles accurate in real time.

> ⭐ **Key Design Constraint:** Reviews are gated behind `COMPLETED` appointment status. The service makes a Feign call to `appointment-service` to verify this before allowing submission — preventing reviews for no-shows or pending appointments.

> 🔄 **Auto Rating Sync:** Every submit, update, and delete triggers `updateDoctorRating()` which recalculates the provider average using `AVG(rating)` and immediately calls `PUT /providers/{id}/rating` on `provider-service` via Feign.

---

## 🏗 System Architecture

```mermaid
graph TD
    FE["🖥️ React Frontend\n:5173"]
    GW["🌐 API Gateway :8080\nJWT Filter + Routing"]
    AUTH["🔑 Auth Service :8081"]
    PROV["👨‍⚕️ Provider Service :8082\nstores avgRating"]
    SCHED["📅 Schedule Service :8083"]
    APPT["🏥 Appointment Service :8084\nstatus: COMPLETED?"]
    PAY["💳 Payment Service :8085"]
    REV["⭐ Review Service :8086\nFeign × 2"]
    EUR["🔍 Eureka Server :8761"]
    DB[("review_db\nMySQL")]

    FE -->|HTTP + JWT| GW
    GW -->|/reviews/**| REV
    GW -->|/providers/**| PROV
    GW -->|/appointments/**| APPT
    GW -->|/auth/**| AUTH
    GW -->|/payments/**| PAY
    GW -->|/slots/**| SCHED
    REV -->|"Feign: GET /appointments/{id}"| APPT
    REV -->|"Feign: PUT /providers/{id}/rating"| PROV
    REV --- DB
    AUTH -.->|registers| EUR
    GW -.->|registers| EUR
    REV -.->|registers| EUR
    APPT -.->|registers| EUR
    PROV -.->|registers| EUR
```

---

## 🧩 All Microservices

| Service | Port | Key Tech | Description |
|---------|------|----------|-------------|
| 🔍 **Eureka Server** | `8761` | Spring Eureka | Service discovery. **Start this first.** Credentials: `admin / medibook123` |
| 🌐 **API Gateway** | `8080` | Spring Cloud Gateway, JWT | Single entry point. Routes all traffic. CORS for `:5173` & `:5174`. |
| 🔑 **Auth Service** | `8081` | Spring Security, JWT, SMTP | Registration, login, token generation. Roles: `PATIENT`, `DOCTOR`, `ADMIN`. |
| 👨‍⚕️ **Provider Service** | `8082` | JPA, Feign target | Doctor profile management. Stores `avgRating`. Exposes `PUT /providers/{id}/rating`. |
| 📅 **Schedule Service** | `8083` | JPA | Time-slot management. Create & book available slots with locking. |
| 🏥 **Appointment Service** | `8084` | Feign, RabbitMQ | Core booking engine. `SCHEDULED → COMPLETED → CANCELLED`. Feign target for review-service. |
| 💳 **Payment Service** | `8085` | Razorpay SDK, HMAC | UC5 — Razorpay-ready payment engine. Initiation, verification, refunds. |
| ⭐ **Review Service** ★ | `8086` | Feign ×2, Actuator | **UC6 — This service.** Star ratings, anonymous reviews, auto avgRating sync. |

---

## ⭐ Review Service Deep Dive

### 📦 Entity: `Review`

Maps to the `reviews` MySQL table. `@UniqueConstraint` on `appointment_id` enforces one review per appointment at the database level.

| Field | Type | Constraint | Description |
|-------|------|-----------|-------------|
| `reviewId` | INT | PK, AUTO | Primary key, auto-generated |
| `appointmentId` | INT | UNIQUE, NOT NULL | One review per appointment — DB enforced |
| `patientId` | INT | NOT NULL | Links to auth-service user |
| `providerId` | INT | NOT NULL | Links to provider-service — used for rating recalc |
| `rating` | INT | NOT NULL | 1 to 5 stars — validated in DTO + service layer |
| `comment` | TEXT | NULLABLE | Optional written feedback from patient |
| `isAnonymous` | BOOLEAN | NOT NULL, default `false` | `true` → display as "Anonymous Patient" |
| `createdAt` | DATETIME | NOT NULL, immutable | Auto-set on `@PrePersist` |
| `updatedAt` | DATETIME | — | Auto-set on `@PreUpdate` |

### 🔌 Service Interface: `ReviewService`

```java
public interface ReviewService {
    Review submitReview(ReviewRequest request);
    List<Review> getReviewsByProvider(int providerId);
    List<Review> getReviewsByPatient(int patientId);
    Review getReviewById(int reviewId);
    Review updateReview(int reviewId, ReviewRequest request);
    void deleteReview(int reviewId);
    double getAverageRating(int providerId);
    long getReviewCount(int providerId);
}
```

### 🔗 Two Feign Clients

```java
// 1. Validates appointment status before allowing review submission
@FeignClient(name = "appointment-service")
public interface AppointmentClient {
    @GetMapping("/appointments/{appointmentId}")
    AppointmentDto getById(@PathVariable int appointmentId);
}

// 2. Auto-syncs avgRating to provider-service after every review change
@FeignClient(name = "provider-service")
public interface ProviderClient {
    @PutMapping("/providers/{providerId}/rating")
    void updateRating(@PathVariable int providerId,
                      @RequestParam double avgRating);
}
```

### 🗄️ Key Repository Queries

```java
// All reviews for a doctor — newest first
List<Review> findByProviderIdOrderByCreatedAtDesc(int providerId);

// Calculate average rating (called after every write operation)
@Query("SELECT AVG(r.rating) FROM Review r WHERE r.providerId = :providerId")
Double calculateAverageRatingByProviderId(@Param("providerId") int providerId);

// Count for doctor profile: "150 reviews"
long countByProviderId(int providerId);

// Duplicate check — one review per appointment
Optional<Review> findByAppointmentId(int appointmentId);

// Filter by star rating (admin moderation / patient filtering)
List<Review> findByProviderIdAndRating(int providerId, int rating);
```

### ⚙️ Auto Rating Recalculation

```java
// Triggered automatically after EVERY write: submit, update, delete
private void updateDoctorRating(int providerId) {
    double newAvg = getAverageRating(providerId);      // SQL AVG query
    providerClient.updateRating(providerId, newAvg);   // Feign PUT call
}

public double getAverageRating(int providerId) {
    Double avg = reviewRepository.calculateAverageRatingByProviderId(providerId);
    return avg != null ? Math.round(avg * 10.0) / 10.0 : 0.0;
    // Rounded to 1 decimal place: 4.6667 → 4.7
}
```

---

## 📡 API Reference

> **Base URL (via Gateway):** `http://localhost:8080/reviews`
> **Direct URL:** `http://localhost:8086/reviews`
> **Swagger UI:** `http://localhost:8086/swagger-ui.html`
> **Actuator:** `http://localhost:8086/actuator/health`
>
> All endpoints require: `Authorization: Bearer <JWT_TOKEN>`

### Patient Operations

| Method | Endpoint | Status | Description |
|--------|----------|--------|-------------|
| `POST` | `/reviews/submit` | 201 | Submit new review — requires COMPLETED appointment |
| `PUT` | `/reviews/{reviewId}` | 200 | Edit review — updates rating, comment, anonymous flag |

### Query Endpoints

| Method | Endpoint | Status | Description |
|--------|----------|--------|-------------|
| `GET` | `/reviews/provider/{providerId}` | 200 | All reviews for a doctor (newest first) |
| `GET` | `/reviews/provider/{providerId}/average` | 200 | Average star rating for a doctor |
| `GET` | `/reviews/provider/{providerId}/count` | 200 | Total review count for a doctor |
| `GET` | `/reviews/patient/{patientId}` | 200 | All reviews submitted by a patient |
| `GET` | `/reviews/{reviewId}` | 200 | Single review by ID |

### Admin Operations

| Method | Endpoint | Status | Description |
|--------|----------|--------|-------------|
| `DELETE` | `/reviews/{reviewId}` | 200 | Delete review — triggers avgRating recalculation |

---

## 🔄 Review Submission Flow

```mermaid
sequenceDiagram
    actor Patient
    participant GW as API Gateway :8080
    participant RevSvc as Review Service :8086
    participant ApptSvc as Appointment Service :8084
    participant ProvSvc as Provider Service :8082
    participant DB as review_db (MySQL)

    Patient->>GW: POST /reviews/submit + JWT
    GW->>RevSvc: JWT validated, forward request
    RevSvc->>ApptSvc: Feign: GET /appointments/{id}
    ApptSvc-->>RevSvc: AppointmentDto {status: "COMPLETED"}

    alt Status not COMPLETED
        RevSvc-->>Patient: 400 Bad Request
    end

    RevSvc->>DB: findByAppointmentId() duplicate check
    alt Review already exists
        RevSvc-->>Patient: 409 Conflict
    end

    RevSvc->>DB: save(Review) with @PrePersist timestamps
    DB-->>RevSvc: Review saved

    RevSvc->>DB: calculateAverageRatingByProviderId()
    DB-->>RevSvc: AVG = 4.7

    RevSvc->>ProvSvc: Feign: PUT /providers/{id}/rating?avgRating=4.7
    ProvSvc-->>RevSvc: 200 OK

    RevSvc-->>Patient: 201 Created with Review object
```

---

## ⭐ Star Rating System

| Stars | Value | Label |
|-------|-------|-------|
| ★☆☆☆☆ | 1 | Very Poor |
| ★★☆☆☆ | 2 | Poor |
| ★★★☆☆ | 3 | Average |
| ★★★★☆ | 4 | Good |
| ★★★★★ | 5 | Excellent |

> ⚠️ **Business Rules:** Rating must be 1–5 (validated by `@Min(1) @Max(5)` in DTO and again in service). Appointment must be `COMPLETED` (verified via Feign). One review per appointment (unique DB constraint + service-level check). Every write auto-syncs provider `avgRating`.

---

## 🧪 API Testing Guide

> ⚠️ **Prerequisites:** All services running · MySQL up · An appointment with status `COMPLETED` exists · Valid JWT token

### Step 1 — Get JWT Token

```bash
# Register patient
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Test Patient","email":"patient@test.com","password":"Password@123","role":"PATIENT"}'

# Login → save token
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"patient@test.com","password":"Password@123"}'

TOKEN="eyJhbGciOiJIUzI1NiJ9..."
```

---

### Step 2 — Submit Reviews

```bash
# 5-star named review
curl -X POST http://localhost:8080/reviews/submit \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "appointmentId": 7,
    "patientId": 12,
    "providerId": 3,
    "rating": 5,
    "comment": "Excellent consultation. Very professional.",
    "isAnonymous": false
  }'

# 3-star anonymous review
curl -X POST http://localhost:8080/reviews/submit \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "appointmentId": 8,
    "patientId": 12,
    "providerId": 3,
    "rating": 3,
    "comment": "Average experience, long wait time.",
    "isAnonymous": true
  }'
```

**✅ 201 Created:**
```json
{
  "reviewId": 1,
  "appointmentId": 7,
  "patientId": 12,
  "providerId": 3,
  "rating": 5,
  "comment": "Excellent consultation. Very professional.",
  "anonymous": false,
  "createdAt": "2026-04-22T10:30:00",
  "updatedAt": "2026-04-22T10:30:00"
}
```

---

### Step 3 — Query Reviews

```bash
# All reviews for doctor (newest first)
curl http://localhost:8080/reviews/provider/3 \
  -H "Authorization: Bearer $TOKEN"

# Average star rating
curl http://localhost:8080/reviews/provider/3/average \
  -H "Authorization: Bearer $TOKEN"
# → {"providerId": 3, "averageRating": 4.7}

# Total review count
curl http://localhost:8080/reviews/provider/3/count \
  -H "Authorization: Bearer $TOKEN"
# → {"providerId": 3, "totalReviews": 2}

# All reviews by patient
curl http://localhost:8080/reviews/patient/12 \
  -H "Authorization: Bearer $TOKEN"

# Single review
curl http://localhost:8080/reviews/1 \
  -H "Authorization: Bearer $TOKEN"
```

---

### Step 4 — Update Review

```bash
curl -X PUT http://localhost:8080/reviews/1 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "appointmentId": 7,
    "patientId": 12,
    "providerId": 3,
    "rating": 4,
    "comment": "Good doctor but the waiting room was crowded.",
    "isAnonymous": true
  }'
```

---

### Step 5 — Admin Delete

```bash
curl -X DELETE http://localhost:8080/reviews/1 \
  -H "Authorization: Bearer $ADMIN_TOKEN"

# → {"message": "Review deleted successfully."}
# → provider avgRating is automatically recalculated
```

---

### ❌ Error Scenarios

```bash
# 400 — appointment not COMPLETED
curl -X POST http://localhost:8080/reviews/submit \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"appointmentId":99,"patientId":12,"providerId":3,"rating":5}'

# 409 — duplicate review for same appointment
curl -X POST http://localhost:8080/reviews/submit \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"appointmentId":7,"patientId":12,"providerId":3,"rating":4}'

# 400 — rating out of valid range (must be 1–5)
curl -X POST http://localhost:8080/reviews/submit \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"appointmentId":10,"patientId":12,"providerId":3,"rating":6}'

# 404 — review not found
curl http://localhost:8080/reviews/9999 \
  -H "Authorization: Bearer $TOKEN"

# 401 — missing authorization
curl http://localhost:8080/reviews/provider/3/average
```

---

### 🧪 Swagger UI & Actuator

```bash
# Interactive Swagger UI
open http://localhost:8086/swagger-ui.html

# Actuator endpoints
curl http://localhost:8086/actuator/health
curl http://localhost:8086/actuator/metrics
curl http://localhost:8086/actuator/info
```

---

## ⚙️ Environment Variables

### Review Service

| Variable | Default | Required | Description |
|----------|---------|----------|-------------|
| `JWT_SECRET` | — | ✅ Required | Must match across **all** services |
| `DB_URL` | `jdbc:mysql://localhost:3306/review_db` | ⬜ Optional | Full JDBC connection URL |
| `DB_USERNAME` | `medibook_user` | ⬜ Optional | MySQL username |
| `DB_PASSWORD` | `medibook_pass` | ⬜ Optional | MySQL password |
| `EUREKA_DEFAULT_ZONE` | `http://admin:medibook123@localhost:8761/eureka/` | ⬜ Optional | Eureka server URL |

### Auth Service (Additional)

| Variable | Description |
|----------|-------------|
| `MAIL_HOST` | SMTP host (default: `smtp.gmail.com`) |
| `MAIL_PORT` | SMTP port (default: `587`) |
| `MAIL_USERNAME` | Gmail address for sending OTP emails |
| `MAIL_PASSWORD` | Gmail **App Password** (not account password) |

---

## 🚀 Quick Start

### 1. Database Setup

```sql
CREATE USER 'medibook_user'@'localhost' IDENTIFIED BY 'medibook_pass';
GRANT ALL PRIVILEGES ON *.* TO 'medibook_user'@'localhost';
FLUSH PRIVILEGES;
-- review_db auto-created by Spring (createDatabaseIfNotExist=true in JDBC URL)
```

### 2. Startup Order

```bash
# 1️⃣  Eureka Server FIRST
cd eureka-server       && mvn spring-boot:run

# 2️⃣  API Gateway SECOND
cd api-gateway         && mvn spring-boot:run

# 3️⃣  All other services
cd auth-service        && mvn spring-boot:run &
cd provider-service    && mvn spring-boot:run &
cd schedule-service    && mvn spring-boot:run &
cd appointment-service && mvn spring-boot:run &
cd payment-service     && mvn spring-boot:run &
cd review-service      && mvn spring-boot:run &
```

### 3. Port Reference

| Service | Port | URL |
|---------|------|-----|
| 🔍 Eureka Server | `8761` | http://localhost:8761 — `admin / medibook123` |
| 🌐 API Gateway | `8080` | http://localhost:8080 |
| 🔑 Auth Service | `8081` | http://localhost:8081 |
| 👨‍⚕️ Provider Service | `8082` | http://localhost:8082 |
| 📅 Schedule Service | `8083` | http://localhost:8083 |
| 🏥 Appointment Service | `8084` | http://localhost:8084 |
| 💳 Payment Service | `8085` | http://localhost:8085 |
| ⭐ **Review Service** ★ | **`8086`** | http://localhost:8086 · Swagger: `/swagger-ui.html` · Actuator: `/actuator/health` |

### 4. Build

```bash
# All modules
mvn clean install -DskipTests

# Review service only
cd review-service && mvn clean package -DskipTests
```

---

## 📁 Project Structure

```
MediBook-Microservices/
├── pom.xml
├── eureka-server/
├── api-gateway/
├── auth-service/
├── provider-service/
├── schedule-service/
├── appointment-service/
├── payment-service/
└── review-service/                ← UC6 ⭐
    └── src/main/java/com/medibook/review/
        ├── resource/              ← ReviewResource (REST)
        ├── service/               ← ReviewService + ReviewServiceImpl
        ├── entity/                ← Review entity
        ├── dto/request/           ← ReviewRequest, AppointmentDto
        ├── repository/            ← ReviewRepository (JPA)
        ├── client/                ← AppointmentClient, ProviderClient (Feign ×2)
        ├── config/                ← SecurityConfig
        └── exception/             ← GlobalExceptionHandler + custom exceptions
```

---

## Author👨‍💻

[Harshal Choudhary](https://github.com/Harshal-25C) - Software Developer👨‍💻 | Cloud Enthusiast  
B.Tech - `[Computer Science & Engineering]`  
Java | Spring Boot | Spring Security | JWT | MySQL | Clean Architecture

---

<div align="center">

**MediBook Microservices** · `feature/UC6-review-service`

Spring Boot 3.2 · Java 17 · MySQL · Spring Cloud Eureka · Feign ×2 · JWT

★★★★★

[![Made with Spring](https://img.shields.io/badge/Made%20with-Spring%20Boot-6DB33F?style=flat-square&logo=springboot)](https://spring.io)

</div>