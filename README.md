<div align="center">

<img src="https://capsule-render.vercel.app/api?type=waving&color=0:0d4a3a,40:137057,80:1a9e6e,100:0d4a3a&height=220&section=header&text=🏥%20MediBook%20Appointment%20Booking%20System&fontSize=42&fontColor=ffffff&animation=fadeIn&fontAlignY=40&desc=Microservice-Architecture%20·%20Follow-Up%20Scheduler%20·%20Spring%20Boot%203.2&descAlignY=60&descAlign=50" width="100%"/>

<br/>

![Java](https://img.shields.io/badge/Java-17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.2.0-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)
![Spring Cloud](https://img.shields.io/badge/Spring_Cloud-2023.0.3-6DB33F?style=for-the-badge&logo=spring&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?style=for-the-badge&logo=mysql&logoColor=white)
![RabbitMQ](https://img.shields.io/badge/RabbitMQ-3.x-FF6600?style=for-the-badge&logo=rabbitmq&logoColor=white)
![JWT](https://img.shields.io/badge/JWT-Auth-000000?style=for-the-badge&logo=jsonwebtokens&logoColor=white)
![OpenFeign](https://img.shields.io/badge/OpenFeign-3_Clients-6DB33F?style=for-the-badge&logo=spring&logoColor=white)
![Gmail](https://img.shields.io/badge/Gmail_SMTP-EA4335?style=for-the-badge&logo=gmail&logoColor=white)
![Swagger](https://img.shields.io/badge/Swagger_UI-85EA2D?style=for-the-badge&logo=swagger&logoColor=black)

<br/>

> **MediBook** is a production-grade online medical appointment booking system built on a **microservices architecture** using Spring Boot, Netflix Eureka, API Gateway, RabbitMQ, and JWT authentication.

</div>

---

## 📋 Table of Contents

- [🏗️ Architecture Overview](#architecture-overview)
- [🚀 Services](#services)
- [⚙️ Prerequisites & Setup](#prerequisites--setup)
- [🔐 Environment Variables](#environment-variables)
- [▶️ Startup Order](#startup-order)
- [🔑 Auth Service](#-auth-service--port-8081)
- [👨‍⚕️ Provider Service](#-provider-service--port-8082)
- [📅 Schedule Service](#-schedule-service--port-8083)
- [🗓️ Appointment Service](#-appointment-service--port-8084)
- [💳 Payment Service](#-payment-service--port-8085)
- [⭐ Review Service](#-review-service--port-8086)
- [🔔 Notification Service](#-notification-service--port-8087)
- [📋 Record Service](#-record-service--port-8088)
- [🛡️ Admin Service](#-admin-service--port-8089)
- [🌐 API Gateway](#-api-gateway--port-8080)
- [📊 Complete API Reference](#complete-api-reference)

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────┐
│                        MEDIBOOK BACKEND                             │
│                                                                     │
│   React Frontend (5173)                                             │
│          │                                                          │
│          ▼                                                          │
│   ┌─────────────┐         ┌──────────────────┐                     │
│   │ API Gateway │────────▶│  Eureka Server   │                     │
│   │   :8080     │         │     :8761        │                     │
│   └──────┬──────┘         └──────────────────┘                     │
│          │                                                          │
│    ┌─────┴──────────────────────────────────────┐                  │
│    │               Routes to:                   │                  │
│    ▼         ▼         ▼         ▼         ▼    │                  │
│  Auth    Provider  Schedule  Appoint  Payment   │                  │
│  :8081   :8082     :8083     :8084    :8085     │                  │
│                                                 │                  │
│    ▼         ▼         ▼         ▼              │                  │
│  Review  Notif    Record    Admin               │                  │
│  :8086   :8087    :8088     :8089               │                  │
│                                                 │                  │
│         ┌──────────────┐                        │                  │
│         │   RabbitMQ   │ ← Async Events         │                  │
│         │    :5672     │   (Appointment→Notif)  │                  │
│         └──────────────┘                        │                  │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 🚀 Services

| Service | Port | Database | Description |
|---------|------|----------|-------------|
| 🔭 **Eureka Server** | `8761` | — | Service discovery & registry |
| 🌐 **API Gateway** | `8080` | — | Single entry point, JWT validation, routing |
| 🔐 **Auth Service** | `8081` | `auth_db` | User registration, login, OTP, JWT, OAuth2 |
| 👨‍⚕️ **Provider Service** | `8082` | `provider_db` | Doctor profiles, verification, search |
| 📅 **Schedule Service** | `8083` | `schedule_db` | Slot management, recurring slots |
| 🗓️ **Appointment Service** | `8084` | `appointment_db` | Booking, cancellation, status management |
| 💳 **Payment Service** | `8085` | `payment_db` | Payment initiation, verification, refunds |
| ⭐ **Review Service** | `8086` | `review_db` | Patient reviews, ratings |
| 🔔 **Notification Service** | `8087` | `notification_db` | Email & in-app notifications via RabbitMQ |
| 📋 **Record Service** | `8088` | `record_db` | Medical records, prescriptions, follow-ups |
| 🛡️ **Admin Service** | `8089` | `auth_db` (shared) | Admin seeding, user management |

---

## ⚙️ Prerequisites & Setup

```bash
# Required software
Java 17+
Maven 3.8+
MySQL 8.0+
RabbitMQ 3.x
```

### Clone & Build

```bash
git clone https://github.com/your-username/medibook-backend.git
cd medibook-backend

# Build all modules
mvn clean install -DskipTests
```

### MySQL Setup

```sql
-- Run once to create the DB user
CREATE USER 'medibook_user'@'localhost' IDENTIFIED BY 'medibook_pass';
GRANT ALL PRIVILEGES ON *.* TO 'medibook_user'@'localhost';
FLUSH PRIVILEGES;

-- Databases are auto-created by each service on first run (createDatabaseIfNotExist=true)
```

---

## 🔐 Environment Variables

Create a `.env` file or set these in your system / IDE run configuration:

```env
# Database
DB_USERNAME=medibook_user
DB_PASSWORD=medibook_pass

# JWT (must be same across ALL services — minimum 256-bit key)
JWT_SECRET=MediBookSuperSecretKey2024MustBeAtLeast256BitsLong!!

# Mail (Gmail App Password — NOT your Gmail password)
MAIL_USERNAME=your-gmail@gmail.com
MAIL_PASSWORD=your-16-char-app-password

# Google OAuth2
GOOGLE_CLIENT_ID=your-google-client-id
GOOGLE_CLIENT_SECRET=your-google-client-secret

# Eureka (optional — defaults are set)
EUREKA_DEFAULT_ZONE=http://admin:medibook123@localhost:8761/eureka/
```

---

## ▶️ Startup Order

> ⚠️ **Must start in this exact order** — each service registers with Eureka

```bash
# 1️⃣  Start Eureka Server FIRST
cd eureka-server && mvn spring-boot:run

# 2️⃣  Start API Gateway SECOND
cd api-gateway && mvn spring-boot:run

# 3️⃣  Start all other services (any order after Eureka is up)
cd auth-service        && mvn spring-boot:run &
cd provider-service    && mvn spring-boot:run &
cd schedule-service    && mvn spring-boot:run &
cd appointment-service && mvn spring-boot:run &
cd payment-service     && mvn spring-boot:run &
cd review-service      && mvn spring-boot:run &
cd notification-service && mvn spring-boot:run &
cd record-service      && mvn spring-boot:run &

# 4️⃣  Start Admin Service LAST (seeds admin accounts into auth_db)
cd admin-service && mvn spring-boot:run
```

> ✅ Eureka Dashboard: http://localhost:8761 &nbsp;|&nbsp; All services should appear as **UP**

---

## 🔑 Auth Service — Port `8081`

> All calls go through API Gateway → `http://localhost:8080`

### Register a Patient / Provider

```http
POST http://localhost:8080/auth/register
Content-Type: application/json

{
  "fullName": "Rahul Sharma",
  "email": "rahul@example.com",
  "password": "Rahul@123",
  "phone": "9876543210",
  "role": "Patient"
}
```

**Response `201 Created`:**
```json
{
  "message": "Registration successful",
  "userId": 10,
  "role": "Patient"
}
```

---

### Login (Step 1 — triggers OTP)

```http
POST http://localhost:8080/auth/login
Content-Type: application/json

{
  "email": "rahul@example.com",
  "password": "Rahul@123"
}
```

**Response `200 OK`:**
```json
{
  "otpSent": true,
  "email": "rahul@example.com",
  "message": "OTP sent to your email"
}
```

---

### Verify OTP (Step 2 — get JWT)

```http
POST http://localhost:8080/auth/verify-otp
Content-Type: application/json

{
  "email": "rahul@example.com",
  "otp": "483920"
}
```

**Response `200 OK`:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "userId": 10,
  "role": "Patient",
  "fullName": "Rahul Sharma",
  "message": "Login successful"
}
```

> 💡 **Save this token** — add it as `Authorization: Bearer <token>` for all protected endpoints

---

### Admin Login

```http
POST http://localhost:8080/auth/login
Content-Type: application/json

{
  "email": "harshalchoudhary340@gmail.com",
  "password": "#Harshal@123"
}
```

---

### Forgot Password

```http
POST http://localhost:8080/auth/forgot-password
Content-Type: application/json

{ "email": "rahul@example.com" }
```

---

### Reset Password

```http
POST http://localhost:8080/auth/reset-password
Content-Type: application/json

{
  "token": "uuid-from-reset-email",
  "newPassword": "NewPass@456"
}
```

---

### Get User Profile

```http
GET http://localhost:8080/auth/profile/10
Authorization: Bearer <token>
```

---

### Update Profile

```http
PUT http://localhost:8080/auth/profile/10
Authorization: Bearer <token>
Content-Type: application/json

{
  "fullName": "Rahul Kumar Sharma",
  "phone": "9876543211",
  "profilePicUrl": "https://example.com/pic.jpg"
}
```

---

### Change Password

```http
PUT http://localhost:8080/auth/password/10
Authorization: Bearer <token>
Content-Type: application/json

{ "newPassword": "NewSecure@789" }
```

---

### Deactivate Account

```http
PUT http://localhost:8080/auth/deactivate/10
Authorization: Bearer <token>
```

---

### Resend OTP

```http
POST http://localhost:8080/auth/resend-otp
Content-Type: application/json

{ "email": "rahul@example.com" }
```

---

### Get All Users (Admin)

```http
GET http://localhost:8080/auth/users
Authorization: Bearer <admin-token>
```

---

### Get Users by Role (Admin)

```http
GET http://localhost:8080/auth/users/role/Provider
Authorization: Bearer <admin-token>
```

---

## 👨‍⚕️ Provider Service — Port `8082`

### Register Provider Profile

> First register the user with `role: "Provider"` via auth-service, then register their profile here.

```http
POST http://localhost:8080/providers/register
Authorization: Bearer <provider-token>
Content-Type: application/json

{
  "userId": 3,
  "specialization": "Ophthalmology",
  "qualification": "MBBS, MS (Ophthalmology) - AIIMS Delhi",
  "experienceYears": 8,
  "bio": "Specialist in retinal disorders and cataract surgery with 8 years of experience.",
  "clinicName": "Vision Care Clinic",
  "clinicAddress": "42, MG Road, Indore, MP 452001"
}
```

**Response `201 Created`:**
```json
{
  "providerId": 1,
  "userId": 3,
  "fullName": "Dr. Kanchan Choudhary",
  "specialization": "Ophthalmology",
  "isVerified": false,
  "averageRating": 0.0
}
```

---

### Get Provider by ID

```http
GET http://localhost:8080/providers/1
```

---

### Get Provider by User ID

```http
GET http://localhost:8080/providers/user/3
Authorization: Bearer <token>
```

---

### Search Providers

```http
GET http://localhost:8080/providers/search?keyword=eye
```

---

### Get All Providers

```http
GET http://localhost:8080/providers/all
```

---

### Get by Specialization

```http
GET http://localhost:8080/providers/specialization/Ophthalmology
```

---

### Verify Provider (Admin only)

```http
PUT http://localhost:8080/providers/1/verify
Authorization: Bearer <admin-token>
```

---

### Update Provider Profile

```http
PUT http://localhost:8080/providers/1
Authorization: Bearer <provider-token>
Content-Type: application/json

{
  "userId": 3,
  "specialization": "Ophthalmology",
  "qualification": "MBBS, MS, Fellowship in Vitreo-Retinal Surgery",
  "experienceYears": 9,
  "bio": "Updated bio with fellowship.",
  "clinicName": "Advanced Eye Care",
  "clinicAddress": "55, Vijay Nagar, Indore"
}
```

---

### Update Availability

```http
PUT http://localhost:8080/providers/1/availability
Authorization: Bearer <provider-token>
Content-Type: application/json

{ "isAvailable": true }
```

---

## 📅 Schedule Service — Port `8083`

### Add Single Slot

```http
POST http://localhost:8080/slots/add
Authorization: Bearer <provider-token>
Content-Type: application/json

{
  "providerId": 1,
  "date": "2026-05-15",
  "startTime": "10:00",
  "endTime": "10:30",
  "durationMinutes": 30,
  "recurrence": "NONE"
}
```

**Response `201 Created`:**
```json
{
  "slotId": 12,
  "providerId": 1,
  "date": "2026-05-15",
  "startTime": "10:00",
  "endTime": "10:30",
  "status": "AVAILABLE"
}
```

---

### Add Bulk Slots

```http
POST http://localhost:8080/slots/bulk
Authorization: Bearer <provider-token>
Content-Type: application/json

[
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
  }
]
```

---

### Add Recurring Slots

```http
POST http://localhost:8080/slots/recurring
Authorization: Bearer <provider-token>
Content-Type: application/json

{
  "providerId": 1,
  "date": "2026-05-01",
  "startTime": "11:00",
  "endTime": "11:30",
  "durationMinutes": 30,
  "recurrence": "WEEKLY",
  "recurrenceEndDate": "2026-05-31"
}
```

---

### Get Available Slots for Provider

```http
GET http://localhost:8080/slots/available/1
```

---

### Get All Slots for Provider

```http
GET http://localhost:8080/slots/provider/1
Authorization: Bearer <provider-token>
```

---

### Block a Slot

```http
PUT http://localhost:8080/slots/12/block
Authorization: Bearer <provider-token>
```

---

### Unblock a Slot

```http
PUT http://localhost:8080/slots/12/unblock
Authorization: Bearer <provider-token>
```

---

### Delete a Slot

```http
DELETE http://localhost:8080/slots/12
Authorization: Bearer <provider-token>
```

---

## 🗓️ Appointment Service — Port `8084`

### Book Appointment

```http
POST http://localhost:8080/appointments/book
Authorization: Bearer <patient-token>
Content-Type: application/json

{
  "patientId": 5,
  "providerId": 1,
  "slotId": 12,
  "serviceType": "Eye Consultation",
  "appointmentDate": "2026-05-15",
  "startTime": "10:00",
  "endTime": "10:30",
  "modeOfConsultation": "IN_PERSON",
  "notes": "Experiencing blurry vision and eye pain for 3 days.",
  "patientEmail": "rahul@example.com"
}
```

**Response `201 Created`:**
```json
{
  "message": "Appointment booked successfully.",
  "appointmentId": 3,
  "status": "SCHEDULED",
  "appointmentDate": "2026-05-15",
  "startTime": "10:00",
  "modeOfConsultation": "IN_PERSON"
}
```

---

### Get Appointment by ID

```http
GET http://localhost:8080/appointments/3
Authorization: Bearer <token>
```

---

### Get Patient's Appointments

```http
GET http://localhost:8080/appointments/patient/5
Authorization: Bearer <patient-token>
```

---

### Get Patient's Upcoming Appointments

```http
GET http://localhost:8080/appointments/patient/5/upcoming
Authorization: Bearer <patient-token>
```

---

### Get Provider's Appointments

```http
GET http://localhost:8080/appointments/provider/1
Authorization: Bearer <provider-token>
```

---

### Get Provider's Appointments by Date

```http
GET http://localhost:8080/appointments/provider/1/date?date=2026-05-15
Authorization: Bearer <provider-token>
```

---

### Mark as Completed (Provider)

```http
PUT http://localhost:8080/appointments/3/complete?providerId=1
Authorization: Bearer <provider-token>
```

---

### Mark as No-Show (Provider)

```http
PUT http://localhost:8080/appointments/3/no-show?providerId=1
Authorization: Bearer <provider-token>
```

---

### Cancel Appointment

```http
PUT http://localhost:8080/appointments/3/cancel
Authorization: Bearer <token>
```

---

### Reschedule Appointment

```http
PUT http://localhost:8080/appointments/3/reschedule
Authorization: Bearer <patient-token>
Content-Type: application/json

{
  "slotId": 15,
  "appointmentDate": "2026-05-20",
  "startTime": "11:00",
  "endTime": "11:30"
}
```

---

## 💳 Payment Service — Port `8085`

### Initiate Payment

```http
POST http://localhost:8080/payments/initiate
Authorization: Bearer <patient-token>
Content-Type: application/json

{
  "appointmentId": 3,
  "patientId": 5,
  "amount": 500.00,
  "paymentMethod": "UPI",
  "currency": "INR"
}
```

**Response `201 Created`:**
```json
{
  "paymentId": 7,
  "appointmentId": 3,
  "amount": 500.00,
  "status": "PENDING",
  "transactionId": "TXN_20260515_ABC123",
  "paymentMethod": "UPI"
}
```

---

### Verify Payment

```http
POST http://localhost:8080/payments/verify
Authorization: Bearer <patient-token>
Content-Type: application/json

{
  "transactionId": "TXN_20260515_ABC123",
  "paymentId": 7
}
```

**Response `200 OK`:**
```json
{
  "paymentId": 7,
  "status": "SUCCESS",
  "message": "Payment verified successfully"
}
```

---

### Get Payment by Appointment

```http
GET http://localhost:8080/payments/appointment/3
Authorization: Bearer <token>
```

---

### Get Patient's Payment History

```http
GET http://localhost:8080/payments/patient/5
Authorization: Bearer <patient-token>
```

---

### Refund Payment

```http
POST http://localhost:8080/payments/7/refund
Authorization: Bearer <admin-token>
```

---

### Get Total Revenue (Admin)

```http
GET http://localhost:8080/payments/revenue/total
Authorization: Bearer <admin-token>
```

---

### Get Provider's Payments

```http
GET http://localhost:8080/payments/provider/1
Authorization: Bearer <provider-token>
```

---

## ⭐ Review Service — Port `8086`

### Submit Review

```http
POST http://localhost:8080/reviews/submit
Authorization: Bearer <patient-token>
Content-Type: application/json

{
  "appointmentId": 3,
  "patientId": 5,
  "providerId": 1,
  "rating": 5,
  "comment": "Excellent doctor! Very thorough examination. Highly recommended.",
  "isAnonymous": false
}
```

**Response `201 Created`:**
```json
{
  "reviewId": 4,
  "providerId": 1,
  "rating": 5,
  "comment": "Excellent doctor!...",
  "createdAt": "2026-05-15T14:30:00"
}
```

---

### Get Provider's Reviews

```http
GET http://localhost:8080/reviews/provider/1
```

---

### Get Provider's Average Rating

```http
GET http://localhost:8080/reviews/provider/1/average
```

**Response:**
```json
{ "providerId": 1, "averageRating": 4.7 }
```

---

### Get Patient's Reviews

```http
GET http://localhost:8080/reviews/patient/5
Authorization: Bearer <patient-token>
```

---

### Update Review

```http
PUT http://localhost:8080/reviews/4
Authorization: Bearer <patient-token>
Content-Type: application/json

{
  "appointmentId": 3,
  "patientId": 5,
  "providerId": 1,
  "rating": 4,
  "comment": "Updated: Good doctor, slightly long wait time.",
  "isAnonymous": false
}
```

---

### Delete Review

```http
DELETE http://localhost:8080/reviews/4
Authorization: Bearer <patient-token>
```

---

## 🔔 Notification Service — Port `8087`

### Send Notification (Manual)

```http
POST http://localhost:8080/notifications/send
Authorization: Bearer <token>
Content-Type: application/json

{
  "recipientId": 5,
  "type": "APPOINTMENT_REMINDER",
  "title": "Appointment Tomorrow",
  "message": "You have an appointment with Dr. Kanchan tomorrow at 10:00 AM.",
  "channel": "APP",
  "relatedId": 3,
  "relatedType": "APPOINTMENT"
}
```

---

### Send Email Notification

```http
POST http://localhost:8080/notifications/send
Authorization: Bearer <token>
Content-Type: application/json

{
  "recipientId": 5,
  "type": "APPOINTMENT_CONFIRMED",
  "title": "Your Appointment is Confirmed - MediBook",
  "message": "Your appointment #3 with Dr. Kanchan on May 15 at 10:00 AM is confirmed.",
  "channel": "EMAIL",
  "relatedId": 3,
  "relatedType": "APPOINTMENT"
}
```

---

### Get User's Notifications

```http
GET http://localhost:8080/notifications/recipient/5
Authorization: Bearer <patient-token>
```

---

### Get Unread Count

```http
GET http://localhost:8080/notifications/unread/count/5
Authorization: Bearer <patient-token>
```

**Response:**
```json
{ "recipientId": 5, "unreadCount": 3 }
```

---

### Mark Notification as Read

```http
PUT http://localhost:8080/notifications/12/read
Authorization: Bearer <patient-token>
```

---

### Mark All as Read

```http
PUT http://localhost:8080/notifications/read/all/5
Authorization: Bearer <patient-token>
```

---

### Delete Notification

```http
DELETE http://localhost:8080/notifications/12
Authorization: Bearer <patient-token>
```

---

## 📋 Record Service — Port `8088`

> ⚠️ Appointment must be `COMPLETED` before creating a medical record.

### Create Medical Record

```http
POST http://localhost:8080/records/create
Authorization: Bearer <provider-token>
Content-Type: application/json

{
  "appointmentId": 3,
  "patientId": 5,
  "providerId": 1,
  "diagnosis": "Bacterial Conjunctivitis (Pink Eye)",
  "prescription": "Tab Ciprofloxacin 500mg - Twice daily for 5 days\nEye drops Moxifloxacin 0.5% - 1 drop in affected eye 4 times daily\nTab Dexamethasone - Once in morning with Ciprofloxacin & Once at night",
  "notes": "Patient presented with redness, discharge and itching in right eye.\nBP: 120/80 mmHg | Weight: 65kg | Height: 5'11\"\nAdvised to avoid eye contact and wash hands frequently.",
  "followUpDate": "2026-05-25"
}
```

**Response `201 Created`:**
```json
{
  "recordId": 8,
  "appointmentId": 3,
  "patientId": 5,
  "providerId": 1,
  "diagnosis": "Bacterial Conjunctivitis (Pink Eye)",
  "followUpDate": "2026-05-25",
  "createdAt": "2026-05-15T14:45:00"
}
```

---

### Get Record by Appointment

```http
GET http://localhost:8080/records/appointment/3
Authorization: Bearer <token>
```

---

### Get Patient's Records

```http
GET http://localhost:8080/records/patient/5
Authorization: Bearer <patient-token>
```

---

### Get Provider's Records

```http
GET http://localhost:8080/records/provider/1
Authorization: Bearer <provider-token>
```

---

### Get Record by ID

```http
GET http://localhost:8080/records/8
Authorization: Bearer <token>
```

---

### Update Medical Record

```http
PUT http://localhost:8080/records/8
Authorization: Bearer <provider-token>
Content-Type: application/json

{
  "appointmentId": 3,
  "patientId": 5,
  "providerId": 1,
  "diagnosis": "Bacterial Conjunctivitis — Improving",
  "prescription": "Continue Ciprofloxacin for 3 more days. Stop Dexamethasone.",
  "notes": "Follow-up visit: Significant improvement noted.",
  "followUpDate": null
}
```

---

### Attach Document to Record

```http
PUT http://localhost:8080/records/8/attach?url=https://storage.example.com/lab-report-patient5.pdf
Authorization: Bearer <provider-token>
```

---

### Get Upcoming Follow-ups for Patient

```http
GET http://localhost:8080/records/patient/5/followups
Authorization: Bearer <patient-token>
```

---

### Get Today's Follow-ups (Scheduler)

```http
GET http://localhost:8080/records/followups/today
Authorization: Bearer <admin-token>
```

---

### Get Record Count for Patient

```http
GET http://localhost:8080/records/patient/5/count
Authorization: Bearer <patient-token>
```

---

### Delete Record (Admin)

```http
DELETE http://localhost:8080/records/8
Authorization: Bearer <admin-token>
```

---

## 🛡️ Admin Service — Port `8089`

> All endpoints require `Authorization: Bearer <admin-token>` — only users with `role: Admin` can access.

### How Admins are Seeded

Pre-configured admins in `admin-service/src/main/resources/application.yml` are **auto-registered at startup**:

```yaml
app:
  admins:
    - fullName: Harshal Choudhary
      email: harshalchoudhary340@gmail.com
      password: "#Harshal@123"
    - fullName: Aditya Landge
      email: adityalandge64@gmail.com
      password: "#Harsh@123"
```

> 💡 These admins can log in via `POST /auth/login` and receive a JWT with `role: Admin`.

---

### Admin Ping / Health Check

```http
GET http://localhost:8080/admin/ping
Authorization: Bearer <admin-token>
```

**Response:**
```json
{
  "status": "UP",
  "service": "admin-service",
  "message": "Admin service is running"
}
```

---

### Get All Users

```http
GET http://localhost:8080/admin/users
Authorization: Bearer <admin-token>
```

---

### Get User by ID

```http
GET http://localhost:8080/admin/users/5
Authorization: Bearer <admin-token>
```

---

### Get Users by Role

```http
GET http://localhost:8080/admin/users/role/Patient
Authorization: Bearer <admin-token>

# Role values: Patient | Provider | Admin
```

---

### Deactivate a User

```http
PUT http://localhost:8080/admin/users/5/deactivate
Authorization: Bearer <admin-token>
```

**Response:**
```json
{ "message": "User deactivated successfully" }
```

---

### Reactivate a User

```http
PUT http://localhost:8080/admin/users/5/reactivate
Authorization: Bearer <admin-token>
```

---

### Get All Admins

```http
GET http://localhost:8080/admin/admins
Authorization: Bearer <admin-token>
```

---

### Add New Admin at Runtime

> No restart required — admin is instantly created in DB.

```http
POST http://localhost:8080/admin/admins
Authorization: Bearer <admin-token>
Content-Type: application/json

{
  "fullName": "New Admin User",
  "email": "newadmin@medibook.com",
  "password": "Admin@Secure123"
}
```

**Response `201 Created`:**
```json
{
  "message": "Admin created successfully",
  "userId": 25,
  "email": "newadmin@medibook.com"
}
```

---

## 🌐 API Gateway — Port `8080`

The gateway is the **single entry point** for all services. All routes are automatically load-balanced via Eureka.

### Route Map

| Prefix | Routed To |
|--------|-----------|
| `/auth/**` | auth-service `:8081` |
| `/providers/**` | provider-service `:8082` |
| `/slots/**` | schedule-service `:8083` |
| `/appointments/**` | appointment-service `:8084` |
| `/payments/**` | payment-service `:8085` |
| `/reviews/**` | review-service `:8086` |
| `/notifications/**` | notification-service `:8087` |
| `/records/**` | record-service `:8088` |
| `/admin/**` | admin-service `:8089` |

### Public Endpoints (No JWT required)

```
POST  /auth/register
POST  /auth/login
POST  /auth/verify-otp
POST  /auth/resend-otp
POST  /auth/forgot-password
POST  /auth/verify-reset-otp
POST  /auth/reset-password
POST  /auth/google/complete
POST  /auth/add-phone
GET   /auth/profile/{userId}
GET   /providers/**
GET   /slots/available/**
```

---

## 📊 Complete API Reference

<details>
<summary><b>🔐 Auth Service — All Endpoints</b></summary>

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `POST` | `/auth/register` | ❌ | Register Patient / Provider |
| `POST` | `/auth/login` | ❌ | Login (sends OTP) |
| `POST` | `/auth/verify-otp` | ❌ | Verify OTP → get JWT |
| `POST` | `/auth/resend-otp` | ❌ | Resend OTP |
| `POST` | `/auth/logout` | ✅ | Logout |
| `POST` | `/auth/refresh` | ✅ | Refresh JWT token |
| `GET`  | `/auth/profile/{userId}` | ❌ | Get user profile |
| `PUT`  | `/auth/profile/{userId}` | ✅ | Update profile |
| `PUT`  | `/auth/password/{userId}` | ✅ | Change password |
| `PUT`  | `/auth/deactivate/{userId}` | ✅ | Deactivate account |
| `PUT`  | `/auth/reactivate/{userId}` | ✅ | Reactivate account |
| `POST` | `/auth/forgot-password` | ❌ | Forgot password |
| `POST` | `/auth/verify-reset-otp` | ❌ | Verify reset OTP |
| `POST` | `/auth/reset-password` | ❌ | Reset password |
| `POST` | `/auth/google/complete` | ❌ | Complete Google OAuth2 |
| `POST` | `/auth/add-phone` | ❌ | Add phone number |
| `GET`  | `/auth/users` | ✅ Admin | Get all users |
| `GET`  | `/auth/users/role/{role}` | ✅ Admin | Get users by role |

</details>

<details>
<summary><b>👨‍⚕️ Provider Service — All Endpoints</b></summary>

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `POST` | `/providers/register` | ✅ | Register provider profile |
| `GET`  | `/providers/{providerId}` | ❌ | Get provider by ID |
| `GET`  | `/providers/user/{userId}` | ✅ | Get provider by user ID |
| `GET`  | `/providers/specialization/{spec}` | ❌ | Get by specialization |
| `GET`  | `/providers/search?keyword=` | ❌ | Search providers |
| `GET`  | `/providers/available` | ❌ | Get available providers |
| `GET`  | `/providers/all` | ❌ | Get all providers |
| `PUT`  | `/providers/{providerId}` | ✅ | Update provider |
| `PUT`  | `/providers/{providerId}/verify` | ✅ Admin | Verify provider |
| `PUT`  | `/providers/{providerId}/availability` | ✅ | Toggle availability |
| `PUT`  | `/providers/{providerId}/rating` | ✅ | Update rating |
| `DELETE` | `/providers/{providerId}` | ✅ Admin | Delete provider |

</details>

<details>
<summary><b>📅 Schedule Service — All Endpoints</b></summary>

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `POST` | `/slots/add` | ✅ | Add single slot |
| `POST` | `/slots/bulk` | ✅ | Add bulk slots |
| `POST` | `/slots/recurring` | ✅ | Add recurring slots |
| `GET`  | `/slots/provider/{providerId}` | ✅ | Get all provider slots |
| `GET`  | `/slots/available/{providerId}` | ❌ | Get available slots |
| `GET`  | `/slots/{slotId}` | ✅ | Get slot by ID |
| `PUT`  | `/slots/{slotId}` | ✅ | Update slot |
| `PUT`  | `/slots/{slotId}/block` | ✅ | Block slot |
| `PUT`  | `/slots/{slotId}/unblock` | ✅ | Unblock slot |
| `PUT`  | `/slots/{slotId}/book` | ✅ | Book slot |
| `PUT`  | `/slots/{slotId}/release` | ✅ | Release slot |
| `DELETE` | `/slots/{slotId}` | ✅ | Delete slot |

</details>

<details>
<summary><b>🗓️ Appointment Service — All Endpoints</b></summary>

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `POST` | `/appointments/book` | ✅ | Book appointment |
| `GET`  | `/appointments/{appointmentId}` | ✅ | Get by ID |
| `GET`  | `/appointments/patient/{patientId}` | ✅ | Patient's appointments |
| `GET`  | `/appointments/patient/{patientId}/upcoming` | ✅ | Upcoming appointments |
| `GET`  | `/appointments/provider/{providerId}` | ✅ | Provider's appointments |
| `GET`  | `/appointments/provider/{providerId}/date` | ✅ | By provider and date |
| `GET`  | `/appointments/provider/{providerId}/count` | ✅ | Appointment count |
| `PUT`  | `/appointments/{id}/cancel` | ✅ | Cancel appointment |
| `PUT`  | `/appointments/{id}/reschedule` | ✅ | Reschedule |
| `PUT`  | `/appointments/{id}/complete` | ✅ Provider | Mark complete |
| `PUT`  | `/appointments/{id}/no-show` | ✅ Provider | Mark no-show |
| `PUT`  | `/appointments/{id}/status` | ✅ | Update status |

</details>

<details>
<summary><b>💳 Payment Service — All Endpoints</b></summary>

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `POST` | `/payments/initiate` | ✅ | Initiate payment |
| `POST` | `/payments/verify` | ✅ | Verify payment |
| `GET`  | `/payments/appointment/{appointmentId}` | ✅ | Get by appointment |
| `GET`  | `/payments/{paymentId}` | ✅ | Get by ID |
| `GET`  | `/payments/patient/{patientId}` | ✅ | Patient's payments |
| `GET`  | `/payments/provider/{providerId}` | ✅ | Provider's payments |
| `GET`  | `/payments/revenue/total` | ✅ Admin | Total revenue |
| `GET`  | `/payments/status` | ✅ | Payment status |
| `POST` | `/payments/{paymentId}/refund` | ✅ Admin | Refund payment |
| `PUT`  | `/payments/{paymentId}/status` | ✅ Admin | Update status |

</details>

<details>
<summary><b>⭐ Review Service — All Endpoints</b></summary>

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `POST` | `/reviews/submit` | ✅ Patient | Submit review |
| `GET`  | `/reviews/provider/{providerId}` | ❌ | Provider's reviews |
| `GET`  | `/reviews/patient/{patientId}` | ✅ | Patient's reviews |
| `GET`  | `/reviews/{reviewId}` | ✅ | Get by ID |
| `GET`  | `/reviews/provider/{providerId}/average` | ❌ | Average rating |
| `GET`  | `/reviews/provider/{providerId}/count` | ❌ | Review count |
| `PUT`  | `/reviews/{reviewId}` | ✅ Patient | Update review |
| `DELETE` | `/reviews/{reviewId}` | ✅ | Delete review |

</details>

<details>
<summary><b>🔔 Notification Service — All Endpoints</b></summary>

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `POST` | `/notifications/send` | ✅ | Send notification |
| `POST` | `/notifications/bulk` | ✅ Admin | Send bulk notifications |
| `GET`  | `/notifications/recipient/{recipientId}` | ✅ | Get user's notifications |
| `GET`  | `/notifications/unread/count/{recipientId}` | ✅ | Unread count |
| `GET`  | `/notifications/all` | ✅ Admin | All notifications |
| `PUT`  | `/notifications/{notificationId}/read` | ✅ | Mark as read |
| `PUT`  | `/notifications/read/all/{recipientId}` | ✅ | Mark all as read |
| `DELETE` | `/notifications/{notificationId}` | ✅ | Delete notification |

</details>

<details>
<summary><b>📋 Record Service — All Endpoints</b></summary>

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `POST` | `/records/create` | ✅ Provider | Create medical record |
| `GET`  | `/records/appointment/{appointmentId}` | ✅ | Get by appointment |
| `GET`  | `/records/patient/{patientId}` | ✅ | Patient's records |
| `GET`  | `/records/provider/{providerId}` | ✅ | Provider's records |
| `GET`  | `/records/{recordId}` | ✅ | Get by ID |
| `GET`  | `/records/patient/{patientId}/followups` | ✅ | Upcoming follow-ups |
| `GET`  | `/records/patient/{patientId}/count` | ✅ | Record count |
| `GET`  | `/records/followups/today` | ✅ Admin | Today's follow-ups |
| `PUT`  | `/records/{recordId}` | ✅ Provider | Update record |
| `PUT`  | `/records/{recordId}/attach` | ✅ Provider | Attach document |
| `DELETE` | `/records/{recordId}` | ✅ Admin | Delete record |

</details>

<details>
<summary><b>🛡️ Admin Service — All Endpoints</b></summary>

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `GET`  | `/admin/ping` | ✅ Admin | Health check |
| `GET`  | `/admin/users` | ✅ Admin | All users |
| `GET`  | `/admin/users/{userId}` | ✅ Admin | User by ID |
| `GET`  | `/admin/users/role/{role}` | ✅ Admin | Users by role |
| `PUT`  | `/admin/users/{userId}/deactivate` | ✅ Admin | Deactivate user |
| `PUT`  | `/admin/users/{userId}/reactivate` | ✅ Admin | Reactivate user |
| `GET`  | `/admin/admins` | ✅ Admin | All admins |
| `POST` | `/admin/admins` | ✅ Admin | Add new admin |

</details>

---

## 💡 Common Workflows

### 🏥 Full Patient Journey

```
1. POST /auth/register          → Register as Patient
2. POST /auth/login             → Login (OTP sent)
3. POST /auth/verify-otp        → Get JWT token
4. GET  /providers/all          → Browse doctors
5. GET  /slots/available/{id}   → View available slots
6. POST /appointments/book      → Book appointment
7. POST /payments/initiate      → Pay for appointment
8. POST /payments/verify        → Confirm payment
9. GET  /appointments/patient/{id}/upcoming  → View upcoming
10. GET /records/patient/{id}   → View medical records
11. POST /reviews/submit        → Leave review after visit
```

### 👨‍⚕️ Full Provider Journey

```
1. POST /auth/register           → Register as Provider
2. POST /providers/register      → Create provider profile
3. POST /slots/add               → Add availability slots
4. GET  /appointments/provider/{id}  → View appointments
5. PUT  /appointments/{id}/complete  → Mark appointment done
6. POST /records/create          → Create medical record
```

### 🛡️ Admin Journey

```
1. POST /auth/login              → Login as Admin
2. GET  /admin/users             → View all users
3. PUT  /providers/{id}/verify   → Verify a provider
4. PUT  /admin/users/{id}/deactivate  → Deactivate bad actor
5. GET  /payments/revenue/total  → Check revenue
6. POST /admin/admins            → Add another admin
```

---

## 🔧 Swagger / API Docs

Each service exposes its own Swagger UI (accessible **directly** on service port — not via gateway):

| Service | Swagger URL |
|---------|-------------|
| Auth Service | http://localhost:8081/swagger-ui.html |
| Provider Service | http://localhost:8082/swagger-ui.html |
| Schedule Service | http://localhost:8083/swagger-ui.html |
| Appointment Service | http://localhost:8084/swagger-ui.html |
| Payment Service | http://localhost:8085/swagger-ui.html |
| Review Service | http://localhost:8086/swagger-ui.html |
| Notification Service | http://localhost:8087/swagger-ui.html |
| Record Service | http://localhost:8088/swagger-ui.html |
| Admin Service | http://localhost:8089/swagger-ui.html |

---

## Author👨‍💻

[Harshal Choudhary](https://github.com/Harshal-25C) - Software Developer👨‍💻 | Cloud Enthusiast  
B.Tech - `[Computer Science & Engineering]`  
Java | Spring Boot | Spring Security | JWT | MySQL | Clean Architecture

---

<div align="center">

<img src="https://readme-typing-svg.demolab.com?font=Fira+Code&size=16&duration=3000&pause=1000&color=00D4AA&center=true&vCenter=true&width=600&lines=Built+with+❤️+by+Harshal+Choudhary;MediBook+—+Book+Smarter.+Heal+Faster." alt="Footer" />

![Made with Spring Boot](https://img.shields.io/badge/Made_with-Spring_Boot-6DB33F?style=for-the-badge&logo=spring-boot)
![Microservices](https://img.shields.io/badge/Architecture-Microservices-blue?style=for-the-badge)
![Open Source](https://img.shields.io/badge/Open-Source-orange?style=for-the-badge&logo=github)

</div>