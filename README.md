# 🏥 MediBook — Auth Service

<div align="center">

```
███╗   ███╗███████╗██████╗ ██╗██████╗  ██████╗  ██████╗ ██╗  ██╗
████╗ ████║██╔════╝██╔══██╗██║██╔══██╗██╔═══██╗██╔═══██╗██║ ██╔╝
██╔████╔██║█████╗  ██║  ██║██║██████╔╝██║   ██║██║   ██║█████╔╝
██║╚██╔╝██║██╔══╝  ██║  ██║██║██╔══██╗██║   ██║██║   ██║██╔═██╗
██║ ╚═╝ ██║███████╗██████╔╝██║██████╔╝╚██████╔╝╚██████╔╝██║  ██╗
╚═╝     ╚═╝╚══════╝╚═════╝ ╚═╝╚═════╝  ╚═════╝  ╚═════╝ ╚═╝  ╚═╝
```

♨️🧑‍💻A Spring Boot microservices backend for MediBook, implementing JWT-based authentication, role-based access control, appointment lifecycle management, provider scheduling, payment integration, and electronic medical records with RESTful APIs🔥 and scalable architecture.🌿💡

---

### `auth-service` — Security Gateway for MediBook Platform

*Book Smarter. Heal Faster. Care Better.*

---

![Java](https://img.shields.io/badge/Java-17-orange?style=for-the-badge&logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.2.0-brightgreen?style=for-the-badge&logo=springboot)
![Spring Security](https://img.shields.io/badge/Spring_Security-6-green?style=for-the-badge&logo=springsecurity)
![MySQL](https://img.shields.io/badge/MySQL-8.0-blue?style=for-the-badge&logo=mysql)
![Redis](https://img.shields.io/badge/Redis-7-red?style=for-the-badge&logo=redis)
![JWT](https://img.shields.io/badge/JWT-0.11.5-black?style=for-the-badge&logo=jsonwebtokens)
![Swagger](https://img.shields.io/badge/Swagger-OpenAPI_3.0-85EA2D?style=for-the-badge&logo=swagger)
![Docker](https://img.shields.io/badge/Docker-Ready-2496ED?style=for-the-badge&logo=docker)
![Maven](https://img.shields.io/badge/Maven-3.9+-C71A36?style=for-the-badge&logo=apachemaven)
![Port](https://img.shields.io/badge/Port-8081-purple?style=for-the-badge)
![Branch](https://img.shields.io/badge/Branch-feature/auth--service-yellow?style=for-the-badge&logo=git)

</div>

---

## 📋 Table of Contents

- [Overview](#-overview)
- [Architecture Position](#-architecture-position)
- [Tech Stack](#-tech-stack)
- [Project Structure](#-project-structure)
- [Features](#-features)
- [Prerequisites](#-prerequisites)
- [Infrastructure Setup](#-infrastructure-setup)
- [Environment Variables](#-environment-variables)
- [How to Run](#-how-to-run)
- [Registration Flow (OTP)](#-registration-flow-otp)
- [API Reference](#-api-reference)
- [Postman Testing Guide](#-postman-testing-guide)
- [Security Architecture](#-security-architecture)
- [Database Schema](#-database-schema)
- [Redis Key Design](#-redis-key-design)
- [Error Responses](#-error-responses)
- [Swagger UI](#-swagger-ui)
- [Common Issues & Fixes](#-common-issues--fixes)

---

## 📖 Overview

The **Auth Service** is the **security gateway** of the MediBook Online Appointment Booking Platform. Every request that touches protected resources on the platform is validated through this service's JWT token logic.

It handles the complete user identity lifecycle:

```
Guest visits → Sends OTP → Verifies Email → Registers → Logs In → Gets JWT → Accesses Platform
```

It supports three user roles — **Patient**, **Provider**, and **Admin** — each with distinct permissions enforced via Spring Security's role-based access control.

---

## 🗺 Architecture Position

```
┌─────────────────────────────────────────────────────────────────────┐
│                     MediBook Microservices                          │
│                                                                     │
│   React Client / Postman                                            │
│         │                                                           │
│         ▼                                                           │
│   ┌──────────────┐   port 8080                                      │
│   │  API Gateway │ ◄──────── All external traffic enters here       │
│   └──────┬───────┘                                                  │
│          │  routes /api/v1/auth/**                                  │
│          ▼                                                           │
│   ┌──────────────┐   port 8081   ┌───────────┐                     │
│   │ auth-service │◄─────────────►│  MySQL    │ auth_db             │
│   │  (THIS ONE)  │               └───────────┘                     │
│   └──────┬───────┘                                                  │
│          │               ┌───────────┐                              │
│          └──────────────►│   Redis   │ OTP + verified email store  │
│                          └───────────┘                              │
│          │               ┌───────────┐                              │
│          └──────────────►│  Gmail    │ OTP email delivery           │
│                          │  SMTP     │                              │
│                          └───────────┘                              │
│                                                                     │
│   ┌────────────────┐   port 8761                                    │
│   │  Eureka Server │ ◄─── auth-service registers here              │
│   └────────────────┘                                                │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 🛠 Tech Stack

| Layer | Technology | Version | Purpose |
|-------|-----------|---------|---------|
| **Language** | Java | 17 | Core language |
| **Framework** | Spring Boot | 3.2.0 | Application framework |
| **Security** | Spring Security | 6 | Authentication & authorization |
| **Authentication** | JWT (jjwt) | 0.11.5 | Stateless token-based auth |
| **Social Login** | Spring OAuth2 Client | 3.2.0 | Google & GitHub login |
| **Database** | MySQL | 8.0 | Persistent user storage |
| **ORM** | Spring Data JPA + Hibernate | 6.3.1 | Database abstraction |
| **Cache/OTP Store** | Redis | 7 | OTP storage + email verification flags |
| **Email** | JavaMailSender (SMTP) | — | OTP delivery via Gmail |
| **Scheduling** | Quartz Scheduler | 2.3.2 | Background job support |
| **API Docs** | SpringDoc OpenAPI | 2.5.0 | Swagger UI at `/swagger-ui.html` |
| **Build** | Maven | 3.9+ | Dependency management |
| **Dev Tools** | Spring DevTools | — | Hot reload during development |
| **Boilerplate** | Lombok | 1.18.30 | Reduce boilerplate code |
| **Service Discovery** | Eureka Client | 2023.0.3 | Register with Eureka Server |
| **Monitoring** | Spring Actuator | — | Health, info, metrics endpoints |

---

## 📁 Project Structure

```
auth-service/
│
├── 📄 pom.xml                                    ← Maven dependencies
│
└── src/
    ├── main/
    │   ├── java/com/medibook/auth/
    │   │   │
    │   │   ├── 🚀 AuthServiceApplication.java     ← Spring Boot entry point
    │   │   │
    │   │   ├── config/                            ← Security & filter configuration
    │   │   │   ├── SecurityConfig.java            ← Spring Security filter chain
    │   │   │   ├── JwtAuthenticationFilter.java   ← JWT validation per-request filter
    │   │   │   └── CustomUserDetailsService.java  ← Loads user from DB for Spring Security
    │   │   │
    │   │   ├── dto/
    │   │   │   ├── request/                       ← Incoming request bodies
    │   │   │   │   ├── RegisterRequest.java        ← fullName, email, password, phone, role
    │   │   │   │   ├── LoginRequest.java           ← email, password
    │   │   │   │   ├── OtpRequest.java             ← email (for sending OTP)
    │   │   │   │   ├── OtpVerifyRequest.java       ← email + otp
    │   │   │   │   ├── UpdateProfileRequest.java   ← fullName, phone, profilePicUrl
    │   │   │   │   ├── ChangePasswordRequest.java  ← oldPassword, newPassword
    │   │   │   │   └── DeleteAccountOtpRequest.java← otp (for account deletion)
    │   │   │   │
    │   │   │   └── response/                      ← Outgoing response bodies
    │   │   │       ├── AuthResponse.java           ← token, tokenType, email, role, userId
    │   │   │       └── UserResponse.java           ← userId, fullName, email, phone, role...
    │   │   │
    │   │   ├── entity/
    │   │   │   └── User.java                      ← JPA entity → maps to `users` table
    │   │   │
    │   │   ├── exception/
    │   │   │   ├── GlobalExceptionHandler.java    ← Centralized error handling
    │   │   │   └── ResourceNotFoundException.java ← 404 exception type
    │   │   │
    │   │   ├── repository/
    │   │   │   └── UserRepository.java            ← Spring Data JPA interface
    │   │   │
    │   │   ├── resource/
    │   │   │   └── AuthResource.java              ← REST Controller (14 endpoints)
    │   │   │
    │   │   ├── service/
    │   │   │   ├── AuthService.java               ← Business contract (interface)
    │   │   │   ├── OtpService.java                ← OTP contract (interface)
    │   │   │   └── impl/
    │   │   │       ├── AuthServiceImpl.java        ← Business logic implementation
    │   │   │       └── OtpServiceImpl.java         ← Redis OTP + Gmail email logic
    │   │   │
    │   │   └── util/
    │   │       └── JwtUtil.java                   ← JWT generation, extraction, validation
    │   │
    │   └── resources/
    │       └── application.yml                    ← All config with env var support
    │
    └── test/
        └── java/com/medibook/auth/
            └── AuthServiceApplicationTests.java   ← Spring context load test
```

---

## ✨ Features

### Core Authentication
- ✅ **OTP-verified Registration** — Email must be verified via OTP before account creation
- ✅ **JWT Login** — Stateless token-based authentication (24-hour expiry)
- ✅ **Token Refresh** — Get a new JWT without re-login
- ✅ **Token Validation** — Any service can validate tokens via `/api/v1/auth/validate`
- ✅ **Logout** — Session invalidation (Redis blacklist ready)

### User Management
- ✅ **3 Roles** — `PATIENT`, `PROVIDER`, `ADMIN` with distinct permissions
- ✅ **Profile View & Update** — fullName, phone, profilePicUrl
- ✅ **Password Change** — Requires current password verification
- ✅ **Account Deactivation** — Soft disable (isActive = false)

### Account Deletion
- ✅ **Self Delete with OTP** — User requests OTP → verifies → account permanently deleted
- ✅ **Admin Delete** — Admin can delete any Patient or Provider account
- ✅ **Admin Protection** — Admin accounts cannot be deleted via the admin delete endpoint

### Security
- ✅ **BCrypt Password Hashing** — Strength 12
- ✅ **JWT Filter** — `JwtAuthenticationFilter` runs on every request
- ✅ **Role-Based Access** — `@PreAuthorize("hasRole('ADMIN')")` on admin endpoints
- ✅ **Method Security** — `@EnableMethodSecurity` active
- ✅ **Stateless Session** — `SessionCreationPolicy.STATELESS`
- ✅ **OAuth2 Ready** — Google and GitHub social login configured

### Infrastructure
- ✅ **Redis OTP Store** — 10-minute TTL, one-time use
- ✅ **Email Verification Flag** — 30-minute Redis TTL after OTP verified
- ✅ **Gmail SMTP** — App password-based secure email delivery
- ✅ **Eureka Discovery** — Registers with service registry (configurable)
- ✅ **Swagger UI** — Interactive API docs at `/swagger-ui.html`
- ✅ **Actuator** — Health, info, metrics at `/actuator`
- ✅ **Global Exception Handler** — Consistent JSON error responses

---

## ✅ Prerequisites

Make sure all of these are installed before running:

| Tool | Version | Check Command |
|------|---------|---------------|
| Java JDK | 17+ | `java -version` |
| Maven | 3.9+ | `mvn -version` |
| MySQL | 8.0 | `mysql --version` |
| Redis | 7 | `redis-cli ping` |
| Docker (optional) | Latest | `docker --version` |
| Git | Any | `git --version` |

---

## 🐳 Infrastructure Setup

### Option A — Docker (Recommended)

```bash
# Start MySQL
docker run --name medibook-mysql \
  -e MYSQL_ROOT_PASSWORD=root \
  -e MYSQL_USER=medibook_user \
  -e MYSQL_PASSWORD=medibook_pass \
  -p 3306:3306 \
  -d mysql:8.0

# Start Redis
docker run --name medibook-redis \
  -p 6379:6379 \
  -d redis:7

# Verify Redis is running
docker exec -it medibook-redis redis-cli ping
# Expected output: PONG
```

### Option B — Local Installation

```bash
# MySQL — start service
sudo service mysql start   # Linux
# or open MySQL Workbench / XAMPP on Windows

# Redis — start server
redis-server               # Linux/Mac
# or start Redis via Docker Desktop on Windows
```

### Create MySQL Database

```sql
-- Connect to MySQL
mysql -u root -proot

-- Create database and user
CREATE DATABASE auth_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'medibook_user'@'localhost' IDENTIFIED BY 'medibook_pass';
GRANT ALL PRIVILEGES ON auth_db.* TO 'medibook_user'@'localhost';
FLUSH PRIVILEGES;

-- Verify
SHOW DATABASES;
-- You should see: auth_db
```

> **Note:** Hibernate will **auto-create** the `users` table on first startup (`ddl-auto: update`).

---

## 🔐 Environment Variables

The service uses **environment variables** for all sensitive config. Set them before running.

### On Windows (Command Prompt / Eclipse Run Config)

```cmd
set DB_URL=jdbc:mysql://localhost:3306/auth_db?useSSL=false^&serverTimezone=UTC^&allowPublicKeyRetrieval=true
set DB_USERNAME=your-username
set DB_PASSWORD=your-password
set REDIS_HOST=localhost
set REDIS_PORT=6379
set MAIL_USERNAME=your-gmail@gmail.com
set MAIL_PASSWORD=your-16-char-app-password
set JWT_SECRET=medibook-super-secret-key-must-be-at-least-256-bits-long-change-in-prod
set JWT_EXPIRATION=86400000
set OTP_EXPIRY_MINUTES=10
set GOOGLE_CLIENT_ID=your-google-client-id
set GOOGLE_CLIENT_SECRET=your-google-client-secret
set GITHUB_CLIENT_ID=your-github-client-id
set GITHUB_CLIENT_SECRET=your-github-client-secret
set EUREKA_ENABLED=false
```

### Setting in Eclipse (Recommended for development)

1. Right-click `AuthServiceApplication.java` → **Run As → Run Configurations**
2. Go to **Environment** tab
3. Click **New** for each variable and add name + value
4. Click **Apply** → **Run**

### Gmail App Password Setup (Required for OTP emails)

```
Step 1: Go to myaccount.google.com
Step 2: Click "Security" in left menu
Step 3: Enable "2-Step Verification" (must be done first)
Step 4: Go to: myaccount.google.com/apppasswords
Step 5: Select app: "Mail", Select device: "Other (custom name)"
Step 6: Enter name: "MediBook"
Step 7: Click Generate
Step 8: Copy the 16-character password (e.g., abcd efgh ijkl mnop)
Step 9: Use this as MAIL_PASSWORD (remove spaces: abcdefghijklmnop)
Step 10: Use your actual Gmail address as MAIL_USERNAME
```

### Google OAuth2 Setup (Optional, for social login)

```
Step 1: Go to console.cloud.google.com
Step 2: Create project → name it "MediBook"
Step 3: APIs & Services → OAuth consent screen
        → External → fill App name: "MediBook"
        → Add scope: email, profile, openid
Step 4: APIs & Services → Credentials
        → Create Credentials → OAuth 2.0 Client ID
        → Application type: Web application
        → Authorized redirect URIs: http://localhost:8081/login/oauth2/code/google
Step 5: Copy Client ID → set as GOOGLE_CLIENT_ID
        Copy Client Secret → set as GOOGLE_CLIENT_SECRET
```

### GitHub OAuth2 Setup (Optional, for social login)

```
Step 1: github.com → Settings → Developer Settings → OAuth Apps
Step 2: New OAuth App
        → Application name: MediBook
        → Homepage URL: http://localhost:8081
        → Authorization callback URL: http://localhost:8081/login/oauth2/code/github
Step 3: Generate a client secret
Step 4: Copy Client ID → set as GITHUB_CLIENT_ID
        Copy Client Secret → set as GITHUB_CLIENT_SECRET
```

---

## ▶ How to Run

```bash
# Step 1 — Clone the repo (if not already done)
git clone https://github.com/Harshal-25C/MediBook-Microservices.git
cd MediBook-Microservices

# Step 2 — Checkout the auth-service branch
git checkout feature/UC1-auth-service

# Step 3 — Make sure MySQL + Redis are running
docker ps   # verify containers are up

# Step 4 — Set environment variables (Windows CMD)
set JWT_SECRET=medibook-super-secret-key-must-be-at-least-256-bits-long
set MAIL_USERNAME=your-gmail@gmail.com
set MAIL_PASSWORD=your-app-password
# ... (set all variables listed above)

# Step 5 — Build and run
cd auth-service
mvn clean install -DskipTests
mvn spring-boot:run

# OR run directly from Eclipse:
# Right-click AuthServiceApplication.java → Run As → Spring Boot App
```

### Successful Startup Output

```
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::                (v3.2.0)

Hibernate:
    create table users (
        user_id bigint not null auto_increment,
        ...
    ) engine=InnoDB

INFO  Tomcat started on port 8081 (http)
INFO  Started AuthServiceApplication in 20.5 seconds
Auth-Service is Running......!
```

> ⚠️ **Eureka warnings are normal** — `Cannot execute request on any known server` just means Eureka Server isn't running yet. The auth-service starts fine. To silence this, set `EUREKA_ENABLED=false`.

---

## 🔄 Registration Flow (OTP)

The registration process uses a **3-step OTP verification flow** to ensure only real email addresses are registered:

```
┌─────────────────────────────────────────────────────────────────┐
│                   REGISTRATION FLOW                             │
│                                                                 │
│  STEP 1: POST /send-otp                                         │
│  ┌──────────┐    email      ┌─────────────┐                    │
│  │  Client  │──────────────►│ auth-service│                    │
│  └──────────┘               └──────┬──────┘                    │
│                                    │ check email not exist     │
│                                    │ generate 6-digit OTP      │
│                                    │ store OTP in Redis         │
│                                    │   key: otp:email           │
│                                    │   TTL: 10 minutes          │
│                                    ▼                            │
│                             ┌─────────────┐                    │
│                             │ Gmail SMTP  │ sends OTP email     │
│                             └─────────────┘                    │
│                                                                 │
│  STEP 2: POST /verify-otp                                       │
│  ┌──────────┐  email+otp    ┌─────────────┐                    │
│  │  Client  │──────────────►│ auth-service│                    │
│  └──────────┘               └──────┬──────┘                    │
│                                    │ match OTP from Redis       │
│                                    │ if valid → delete OTP      │
│                                    │ set Redis flag:            │
│                                    │   key: email_verified:x   │
│                                    │   TTL: 30 minutes          │
│                                    ▼                            │
│                          { "verified": true }                   │
│                                                                 │
│  STEP 3: POST /register                                         │
│  ┌──────────┐  user data    ┌─────────────┐                    │
│  │  Client  │──────────────►│ auth-service│                    │
│  └──────────┘               └──────┬──────┘                    │
│                                    │ check email_verified flag  │
│                                    │ if missing → 409 error     │
│                                    │ if present → save to DB    │
│                                    │ delete Redis verified flag │
│                                    ▼                            │
│                          201 Created + UserResponse             │
└─────────────────────────────────────────────────────────────────┘
```

---

## 📡 API Reference

### Base URL
```
http://localhost:8081/api/v1/auth
```

---

### 1. Send OTP for Registration

| | |
|---|---|
| **Method** | `POST` |
| **URL** | `/send-otp` |
| **Auth** | ❌ Not required |
| **Description** | Sends a 6-digit OTP to the email for registration verification |

**Request Body**
```json
{
  "email": "newuser123@gmail.com"
}
```

**Response `200 OK`**
```json
{
  "message": "OTP sent to newuser123@gmail.com. Valid for 10 minutes."
}
```

**Error Cases**
| Code | Reason |
|------|--------|
| `400` | Email already registered |
| `500` | Gmail SMTP failure (check app password) |

---

### 2. Verify OTP

| | |
|---|---|
| **Method** | `POST` |
| **URL** | `/verify-otp` |
| **Auth** | ❌ Not required |
| **Description** | Verifies the OTP sent to email. Marks email as verified for 30 minutes |

**Request Body**
```json
{
  "email": "newuser123@gmail.com",
  "otp": "482910"
}
```

**Response `200 OK` — Valid OTP**
```json
{
  "verified": true,
  "message": "Email verified. Now call /api/v1/auth/register to complete registration."
}
```

**Response `400` — Invalid or Expired OTP**
```json
{
  "verified": false,
  "message": "Invalid or expired OTP. Please request a new OTP."
}
```

---

### 3. Register User

| | |
|---|---|
| **Method** | `POST` |
| **URL** | `/register` |
| **Auth** | ❌ Not required |
| **Description** | Creates a new user account. OTP must be verified first |

**Request Body — Patient**
```json
{
  "fullName": "Harsh Sarode",
  "email": "sarodeharsh@gmail.com",
  "password": "Harsh@123",
  "phone": "9876543210",
  "role": "PATIENT"
}
```

**Request Body — Provider**
```json
{
  "fullName": "Dr. Anvii Patil",
  "email": "patilanvii@gmail.com",
  "password": "AnviiP@123",
  "phone": "9876543211",
  "role": "PROVIDER"
}
```

**Request Body — Admin**
```json
{
  "fullName": "Harshal Choudhary",
  "email": "httpsharsh@gmail.com",
  "password": "#Harshal@123",
  "phone": "9876543212",
  "role": "ADMIN"
}
```

**Response `201 Created`**
```json
{
  "userId": 1,
  "fullName": "Harsh Sarode",
  "email": "sarodeharsh@gmail.com",
  "phone": "9876543210",
  "role": "PATIENT",
  "isActive": true,
  "profilePicUrl": null,
  "createdAt": "2026-04-19T16:10:00"
}
```

**Error Cases**
| Code | Reason |
|------|--------|
| `409` | Email not verified (OTP step skipped) |
| `400` | Email already registered |
| `400` | Validation errors (password too short, invalid phone, etc.) |

---

### 4. Login

| | |
|---|---|
| **Method** | `POST` |
| **URL** | `/login` |
| **Auth** | ❌ Not required |
| **Description** | Authenticates user and returns a JWT token |

**Request Body**
```json
{
  "email": "admin123@gmail.com",
  "password": "Admin@123"
}
```

**Response `200 OK`**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJyb2xlIjoiQURNSU4iLCJ1c2VySWQiOjEsInN1YiI6ImFkbWluMTIzQGdtYWlsLmNvbSIsImlhdCI6MTc0NTA1MDYwMCwiZXhwIjoxNzQ1MTM3MDAwfQ.xxxxxx",
  "tokenType": "Bearer",
  "email": "admin123@gmail.com",
  "role": "ADMIN",
  "userId": 1
}
```

> 📌 **Important:** Copy the `token` and `userId` — you will need them for all subsequent requests.

**Error Cases**
| Code | Reason |
|------|--------|
| `400` | Invalid credentials |
| `409` | Account is deactivated |
| `404` | User not found |

---

### 5. Validate Token

| | |
|---|---|
| **Method** | `GET` |
| **URL** | `/validate` |
| **Auth** | ✅ Bearer token in header |
| **Description** | Validates a JWT token — used by other microservices |

**Headers**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.....
```

**Response `200 OK`**
```json
{
  "valid": true
}
```

**Response `200 OK` — Invalid token**
```json
{
  "valid": false
}
```

---

### 6. Get Profile by User ID

| | |
|---|---|
| **Method** | `GET` |
| **URL** | `/profile/{userId}` |
| **Auth** | ✅ Bearer token required |
| **Description** | Returns the user profile for the given userId |

**Headers**
```
Authorization: Bearer <your_token>
```

**Response `200 OK`**
```json
{
  "userId": 1,
  "fullName": "Admin User",
  "email": "admin123@gmail.com",
  "phone": "9876543212",
  "role": "ADMIN",
  "isActive": true,
  "profilePicUrl": null,
  "createdAt": "2026-04-19T16:10:00"
}
```

**Error Cases**
| Code | Reason |
|------|--------|
| `401` | Missing or invalid token |
| `404` | User ID not found |

---

### 7. Update Profile

| | |
|---|---|
| **Method** | `PUT` |
| **URL** | `/profile/{userId}` |
| **Auth** | ✅ Bearer token required |
| **Description** | Updates fullName, phone, or profilePicUrl (all fields optional) |

**Headers**
```
Authorization: Bearer <your_token>
Content-Type: application/json
```

**Request Body**
```json
{
  "fullName": "Admin User Updated",
  "phone": "9999999999",
  "profilePicUrl": "https://example.com/profile.jpg"
}
```

**Response `200 OK`** — Returns updated UserResponse

---

### 8. Change Password

| | |
|---|---|
| **Method** | `PUT` |
| **URL** | `/password/{userId}` |
| **Auth** | ✅ Bearer token required |
| **Description** | Changes password — requires current password verification |

**Headers**
```
Authorization: Bearer <your_token>
Content-Type: application/json
```

**Request Body**
```json
{
  "oldPassword": "cryptoHarshhh@123",
  "newPassword": "PsycoHarshh@XX.jar"
}
```

**Response `204 No Content`** — Password changed successfully

**Error Cases**
| Code | Reason |
|------|--------|
| `400` | Current password is incorrect |
| `400` | New password too short (minimum 6 characters) |

---

### 9. Deactivate Account

| | |
|---|---|
| **Method** | `PUT` |
| **URL** | `/deactivate/{userId}` |
| **Auth** | ✅ Bearer token required |
| **Description** | Soft-deactivates account (isActive = false). User cannot login after this |

**Headers**
```
Authorization: Bearer <your_token>
```

**Response `204 No Content`** — Account deactivated

---

### 10. Refresh Token

| | |
|---|---|
| **Method** | `POST` |
| **URL** | `/refresh` |
| **Auth** | ✅ Bearer token required (can be near-expiry) |
| **Description** | Issues a new JWT token without re-login |

**Headers**
```
Authorization: Bearer <old_token>
```

**Response `200 OK`**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9.new_token_here...",
  "tokenType": "Bearer"
}
```

---

### 11. Logout

| | |
|---|---|
| **Method** | `POST` |
| **URL** | `/logout` |
| **Auth** | ✅ Bearer token required |
| **Description** | Logs out the user (logs the event; Redis blacklist integration ready) |

**Headers**
```
Authorization: Bearer <your_token>
```

**Response `204 No Content`**

---

### 12. Request OTP for Account Deletion

| | |
|---|---|
| **Method** | `POST` |
| **URL** | `/delete-account/request-otp` |
| **Auth** | ✅ Bearer token required |
| **Description** | Sends a one-time OTP to the logged-in user's email before permanent deletion |

**Headers**
```
Authorization: Bearer <your_token>
```

**Response `200 OK`**
```json
{
  "message": "Delete account OTP sent to your registered email."
}
```

---

### 13. Confirm Account Deletion with OTP

| | |
|---|---|
| **Method** | `DELETE` |
| **URL** | `/delete-account/confirm` |
| **Auth** | ✅ Bearer token required |
| **Description** | Permanently deletes the authenticated user's account after OTP verification |

**Headers**
```
Authorization: Bearer <your_token>
Content-Type: application/json
```

**Request Body**
```json
{
  "otp": "482910"
}
```

**Response `200 OK`**
```json
{
  "message": "Your account has been deleted permanently."
}
```

**Error Cases**
| Code | Reason |
|------|--------|
| `400` | Invalid or expired OTP |
| `404` | User not found |

---

### 14. Admin Delete Any User

| | |
|---|---|
| **Method** | `DELETE` |
| **URL** | `/admin/users/{userId}` |
| **Auth** | ✅ Bearer token required — **ADMIN role only** |
| **Description** | Admin permanently deletes any Patient or Provider account by userId |

**Headers**
```
Authorization: Bearer <admin_token>
```

**Response `200 OK`**
```json
{
  "message": "User deleted successfully by admin."
}
```

**Error Cases**
| Code | Reason |
|------|--------|
| `403` | Non-admin trying to use this endpoint |
| `409` | Trying to delete an Admin account |
| `404` | userId not found |

---

## 🧪 Postman Testing Guide

### Setup Postman Environment

Create a new environment in Postman with these variables:

| Variable | Initial Value | Description |
|----------|--------------|-------------|
| `baseUrl` | `http://localhost:8081/api/v1/auth` | Base URL |
| `token` | *(empty)* | User JWT token (fill after login) |
| `adminToken` | *(empty)* | Admin JWT token (fill after admin login) |
| `userId` | *(empty)* | User ID (fill after register/login) |
| `adminUserId` | *(empty)* | Admin user ID |
| `otp` | *(empty)* | Current OTP received in email |

Use in requests:
```
URL:    {{baseUrl}}/login
Header: Authorization: Bearer {{token}}
```

---

### 🚦 Recommended Testing Order

#### For a New Patient User

```
1. POST {{baseUrl}}/send-otp          → send OTP to newuser123@gmail.com
2. Check Gmail inbox → copy OTP
3. POST {{baseUrl}}/verify-otp        → verify OTP
4. POST {{baseUrl}}/register          → complete registration
5. POST {{baseUrl}}/login             → get token → save to {{token}}, save userId
6. GET  {{baseUrl}}/validate          → verify token is valid
7. GET  {{baseUrl}}/profile/{{userId}} → see profile
8. PUT  {{baseUrl}}/profile/{{userId}} → update name/phone
9. PUT  {{baseUrl}}/password/{{userId}} → change password
10. POST {{baseUrl}}/refresh           → get new token
11. POST {{baseUrl}}/delete-account/request-otp → get OTP for deletion
12. DELETE {{baseUrl}}/delete-account/confirm → delete account
```

#### For Admin Operations

```
1. Register admin account (follow Patient steps with role: "ADMIN")
2. POST {{baseUrl}}/login             → get adminToken
3. GET  {{baseUrl}}/validate          → verify admin token
4. GET  {{baseUrl}}/profile/{{adminUserId}} → view admin profile
5. DELETE {{baseUrl}}/admin/users/2   → delete user with ID 2
   (Use a non-admin userId. Trying to delete an admin will give 409.)
```

---

### 📋 Ready-to-Use Request Bodies

**Send OTP**
```json
{ "email": "newuser123@gmail.com" }
```

**Verify OTP**
```json
{ "email": "newuser123@gmail.com", "otp": "482910" }
```

**Register Patient**
```json
{
  "fullName": "Harsh Sarode",
  "email": "sarodeharsh@gmail.com",
  "password": "Harsh@123",
  "phone": "9876543210",
  "role": "PATIENT"
}
```

**Register Provider**
```json
{
  "fullName": "Dr. Anvii Patil",
  "email": "patilanvii@gmail.com",
  "password": "AnviiP@123",
  "phone": "9876543211",
  "role": "PROVIDER"
}
```

**Register Admin**
```json
{
  "fullName": "Harshal Choudhary",
  "email": "httpsharsh@gmail.com",
  "password": "#Harshal@123",
  "phone": "9876543212",
  "role": "ADMIN"
}
```

**Login**
```json
{ "email": "httpsharsh@gmail.com", "password": "#Harshal@123" }
```

**Update Profile**
```json
{
  "fullName": "Harsh Updated",
  "phone": "9988776655",
  "profilePicUrl": "https://example.com/harsh.jpg"
}
```

**Change Password**
```json
{ "oldPassword": "Admin@123", "newPassword": "Admin@456" }
```

**Delete Account (confirm)**
```json
{ "otp": "482910" }
```

---

### 🔑 Token Usage in Postman

For all **protected endpoints**, add this header:

| Key | Value |
|-----|-------|
| `Authorization` | `Bearer eyJhbGciOiJIUzI1NiJ9.....` |

**Protected endpoints (require Bearer token):**
- `/logout`
- `/refresh`
- `/profile/{userId}` (GET + PUT)
- `/password/{userId}`
- `/deactivate/{userId}`
- `/delete-account/request-otp`
- `/delete-account/confirm`
- `/admin/users/{userId}`

---

## 🔒 Security Architecture

```
Incoming Request
      │
      ▼
┌─────────────────────────────────┐
│     JwtAuthenticationFilter      │  ← Runs on EVERY request
│                                  │
│  1. Read Authorization header    │
│  2. Extract "Bearer " prefix     │
│  3. Parse JWT → extract email    │
│  4. Load UserDetails from DB     │
│  5. Validate token signature     │
│  6. Set SecurityContext if valid │
└──────────────┬──────────────────┘
               │
               ▼
┌─────────────────────────────────┐
│        SecurityConfig            │
│                                  │
│  PUBLIC (no token needed):       │
│  POST  /send-otp                 │
│  POST  /verify-otp               │
│  POST  /register                 │
│  POST  /login                    │
│  POST  /refresh                  │
│  GET   /validate                 │
│  GET   /swagger-ui/**            │
│  GET   /api-docs/**              │
│  GET   /actuator/health          │
│                                  │
│  PROTECTED (token required):     │
│  Everything else → authenticated │
└──────────────┬──────────────────┘
               │
               ▼
┌─────────────────────────────────┐
│     @PreAuthorize (Method Level) │
│                                  │
│  @PreAuthorize("hasRole('ADMIN')")│
│  → /admin/users/{userId}         │
│  → Returns 403 if not ADMIN role │
└─────────────────────────────────┘
```

### JWT Token Structure

```
Header:    { "alg": "HS256", "typ": "JWT" }
Payload:   {
             "role": "ADMIN",
             "userId": 1,
             "sub": "admin123@gmail.com",
             "iat": 1745050600,
             "exp": 1745137000
           }
Signature: HMAC-SHA256(base64(header) + "." + base64(payload), secret)
```

---

## 🗄 Database Schema

Table: **`users`** (auto-created by Hibernate on startup)

```sql
CREATE TABLE users (
    user_id         BIGINT          NOT NULL AUTO_INCREMENT,
    full_name       VARCHAR(255)    NOT NULL,
    email           VARCHAR(255)    NOT NULL UNIQUE,
    password_hash   VARCHAR(255),
    phone           VARCHAR(255),
    role            ENUM('PATIENT','PROVIDER','ADMIN') NOT NULL,
    provider        ENUM('LOCAL','GOOGLE','GITHUB'),
    is_active       BIT             NOT NULL DEFAULT 1,
    profile_pic_url VARCHAR(255),
    created_at      DATETIME(6),
    updated_at      DATETIME(6),
    PRIMARY KEY (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

## 🔑 Redis Key Design

| Key Pattern | Value | TTL | Purpose |
|-------------|-------|-----|---------|
| `otp:{email}` | `"482910"` | 10 minutes | Stores OTP for email verification |
| `email_verified:{email}` | `"true"` | 30 minutes | Flag set after OTP verified, cleared after register |

---

## ❌ Error Responses

All errors return consistent JSON:

```json
{
  "status": 400,
  "message": "Email already registered.",
  "timestamp": "2026-04-19T16:30:00"
}
```

| HTTP Code | When |
|-----------|------|
| `400 Bad Request` | Validation failure, invalid credentials, incorrect password, bad OTP |
| `401 Unauthorized` | Missing or malformed Authorization header |
| `403 Forbidden` | Non-admin accessing admin endpoint |
| `404 Not Found` | User ID or email not found |
| `409 Conflict` | Email not verified, account deactivated, trying to delete admin |

---

## 📖 Swagger UI

Once the service is running, open in your browser:

```
http://localhost:8081/swagger-ui.html
```

You can:
- See all 14 API endpoints grouped under **Authentication**
- Try each endpoint directly in the browser
- See request/response schemas

Also available:
```
http://localhost:8081/api-docs          ← OpenAPI JSON spec
http://localhost:8081/actuator/health   ← Service health check
http://localhost:8081/actuator/info     ← Service info
http://localhost:8081/actuator/metrics  ← JVM/HTTP metrics
```

---

## 🔧 Common Issues & Fixes

### Issue 1: Eureka Connection Refused warnings in console

```
Cannot execute request on any known server
```

**This is NOT an error.** auth-service started successfully. Eureka Server simply isn't running.

**Fix:** Set environment variable `EUREKA_ENABLED=false` to silence these warnings during development.

---

### Issue 2: Redis connection refused

```
Unable to connect to Redis; nested exception is io.lettuce.core.RedisConnectionException
```

**Fix:** Start Redis container:
```bash
docker run --name medibook-redis -p 6379:6379 -d redis:7
# Verify:
docker exec -it medibook-redis redis-cli ping
# Should print: PONG
```

---

### Issue 3: Mail authentication failed

```
535-5.7.8 Username and Password not accepted
```

**Fix:** You're using your regular Gmail password. You need an **App Password**.
Go to `myaccount.google.com/apppasswords` and generate one.

---

### Issue 4: OTP email not received

- Check spam/junk folder
- Verify `MAIL_USERNAME` is your actual Gmail
- Verify `MAIL_PASSWORD` is the 16-char app password (no spaces)
- Make sure 2-Step Verification is enabled on Gmail

---

### Issue 5: 409 Conflict on `/register` — "Email not verified"

You tried to register without completing the OTP verification step.

**Fix:** Follow the correct 3-step flow:
1. `POST /send-otp` first
2. `POST /verify-otp` with the OTP from email
3. Then `POST /register`

---

### Issue 6: MySQL dialect deprecation warning

```
HHH90000025: MySQL8Dialect does not need to be specified explicitly
```

**Not an error.** Just a Hibernate 6 informational warning. Already corrected in `application.yml` to use `org.hibernate.dialect.MySQLDialect`.

---

### Issue 7: Cannot find template location

```
Cannot find template location: classpath:/templates/
```

**Not an error.** auth-service is a REST API, not a Thymeleaf web app. Fixed in `application.yml`:
```yaml
spring:
  thymeleaf:
    check-template-location: false
```

---

## 📊 API Summary Table

| # | Method | Endpoint | Auth Required | Role |
|---|--------|----------|--------------|------|
| 1 | POST | `/send-otp` | ❌ | Any |
| 2 | POST | `/verify-otp` | ❌ | Any |
| 3 | POST | `/register` | ❌ | Any |
| 4 | POST | `/login` | ❌ | Any |
| 5 | GET | `/validate` | ✅ | Any |
| 6 | POST | `/refresh` | ✅ | Any |
| 7 | POST | `/logout` | ✅ | Any |
| 8 | GET | `/profile/{userId}` | ✅ | Any |
| 9 | PUT | `/profile/{userId}` | ✅ | Any |
| 10 | PUT | `/password/{userId}` | ✅ | Any |
| 11 | PUT | `/deactivate/{userId}` | ✅ | Any |
| 12 | POST | `/delete-account/request-otp` | ✅ | Any |
| 13 | DELETE | `/delete-account/confirm` | ✅ | Any |
| 14 | DELETE | `/admin/users/{userId}` | ✅ | **ADMIN only** |

---

## 🌿 Git Branch Info

```
Repository  : MediBook-Microservices
Branch      : feature/UC1-auth-service
Base Branch : develop
Merge Target: develop (PR required)
```

**Commit convention:**
```
feat(auth): add OTP-verified registration
fix(auth): handle expired token in refresh endpoint
refactor(auth): extract JWT claims parsing to JwtUtil
test(auth): add unit tests for AuthServiceImpl
docs(auth): update README with Postman guide
```

---

## 👨‍💻 Developer Notes

- All config values use environment variables with sensible defaults — never hardcode secrets
- `ddl-auto: update` is fine for development; change to `validate` in production
- Logout currently logs the event — production implementation should blacklist the token in Redis using the token's remaining TTL
- The `CustomUserDetailsService` loads the user on **every authenticated request** — consider adding a Redis/cache layer for production performance
- `@EnableScheduling` is active on the main class — ready for scheduled jobs (e.g., expired OTP cleanup)
- Admin accounts are protected from deletion via the admin delete endpoint

---

### Author👨‍💻

[Harshal Choudhary](https://github.com/Harshal-25C) - Software Developer👨‍💻 | Cloud Enthusiast              
B.Tech - `[Computer Science & Engineering]`         
Java | Spring Boot | Maven | JWT & Security | OAuth | React.js | Clean Architecture

---

<div align="center">

**MediBook Auth Service** | Part of MediBook Microservices Platform

*Confidential | MediBook Platform | Internal Use Only*

</div>
