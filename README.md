# 🏥 MediBook — API Gateway

<div align="center">

```
 █████╗ ██████╗ ██╗     ██████╗  █████╗ ████████╗███████╗██╗    ██╗ █████╗ ██╗   ██╗
██╔══██╗██╔══██╗██║    ██╔════╝ ██╔══██╗╚══██╔══╝██╔════╝██║    ██║██╔══██╗╚██╗ ██╔╝
███████║██████╔╝██║    ██║  ███╗███████║   ██║   █████╗  ██║ █╗ ██║███████║ ╚████╔╝
██╔══██║██╔═══╝ ██║    ██║   ██║██╔══██║   ██║   ██╔══╝  ██║███╗██║██╔══██║  ╚██╔╝
██║  ██║██║     ██║    ╚██████╔╝██║  ██║   ██║   ███████╗╚███╔███╔╝██║  ██║   ██║
╚═╝  ╚═╝╚═╝     ╚═╝     ╚═════╝ ╚═╝  ╚═╝   ╚═╝   ╚══════╝ ╚══╝╚══╝ ╚═╝  ╚═╝   ╚═╝
```

### `api-gateway` — Single Entry Point for the MediBook Platform

*Book Smarter. Heal Faster. Care Better.*

---

![Java](https://img.shields.io/badge/Java-17-orange?style=for-the-badge&logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.2.0-brightgreen?style=for-the-badge&logo=springboot)
![Spring Cloud Gateway](https://img.shields.io/badge/Spring_Cloud_Gateway-2023.0.0-6DB33F?style=for-the-badge&logo=spring)
![WebFlux](https://img.shields.io/badge/WebFlux-Reactive-blueviolet?style=for-the-badge&logo=spring)
![Eureka](https://img.shields.io/badge/Eureka_Client-Netflix-red?style=for-the-badge&logo=netflix)
![JWT](https://img.shields.io/badge/JWT-0.11.5-black?style=for-the-badge&logo=jsonwebtokens)
![Lombok](https://img.shields.io/badge/Lombok-1.18.30-pink?style=for-the-badge)
![Maven](https://img.shields.io/badge/Maven-3.9+-C71A36?style=for-the-badge&logo=apachemaven)
![Docker](https://img.shields.io/badge/Docker-Ready-2496ED?style=for-the-badge&logo=docker)
![Port](https://img.shields.io/badge/Port-8080-purple?style=for-the-badge)
![Branch](https://img.shields.io/badge/Branch-feature/api--gateway-yellow?style=for-the-badge&logo=git)

</div>

---

## 📋 Table of Contents

- [Overview](#-overview)
- [Architecture Position](#-architecture-position)
- [Tech Stack](#-tech-stack)
- [Project Structure](#-project-structure)
- [Features](#-features)
- [Route Table](#-route-table)
- [JWT Filter — How It Works](#-jwt-filter--how-it-works)
- [Public vs Protected Paths](#-public-vs-protected-paths)
- [Downstream Headers Forwarded](#-downstream-headers-forwarded)
- [CORS Configuration](#-cors-configuration)
- [Prerequisites](#-prerequisites)
- [Environment Variables](#-environment-variables)
- [How to Run](#-how-to-run)
- [Startup Order](#-startup-order)
- [Postman Testing via Gateway](#-postman-testing-via-gateway)
- [Actuator Endpoints](#-actuator-endpoints)
- [Common Issues & Fixes](#-common-issues--fixes)

---

## 📖 Overview

The **API Gateway** is the **single entry point** for the entire MediBook platform. Every request from any client — whether a React frontend, mobile app, or Postman — hits port `8080` first. The gateway then:

1. **Validates the JWT token** on protected routes using its own `JwtAuthenticationFilter`
2. **Routes the request** to the correct downstream microservice using Eureka-based load balancing
3. **Forwards user context** (`X-User-Email`, `X-User-Role`, `X-User-Id`) as headers to downstream services so they know who is calling
4. **Handles CORS** globally so the React frontend can communicate without browser errors

No client should ever call a microservice directly on its own port. Everything goes through `localhost:8080`.

```
Client (React / Postman)
         │
         ▼ port 8080
   [ API Gateway ]
         │
         ├── /api/v1/auth/**       →  auth-service       :8081
         ├── /api/v1/providers/**  →  provider-service   :8082
         ├── /api/v1/slots/**      →  schedule-service   :8083
         ├── /api/v1/appointments/**→ appointment-service:8084
         ├── /api/v1/payments/**   →  payment-service    :8085
         ├── /api/v1/reviews/**    →  review-service     :8086
         ├── /api/v1/notifications/**→ notification-service:8087
         ├── /api/v1/records/**    →  record-service     :8088
         ├── /api/v1/admin/**      →  admin-service      :8089
         └── /api/v1/gateway/**    →  payment-gateway-service:8090
```

---

## 🗺 Architecture Position

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                        MediBook Microservices Platform                       │
│                                                                              │
│  ┌──────────────────────────────────────────────────────────┐               │
│  │                   CLIENT LAYER                           │               │
│  │   React (localhost:3000)  │  Vite (localhost:5173)       │               │
│  │   Postman / Mobile App    │  Any HTTP client             │               │
│  └───────────────────────────┬──────────────────────────────┘               │
│                              │ ALL requests → port 8080                     │
│                              ▼                                               │
│  ┌───────────────────────────────────────────────────────────┐              │
│  │               API GATEWAY  (THIS SERVICE)                 │              │
│  │                    port: 8080                             │              │
│  │                                                           │              │
│  │  ① JwtAuthenticationFilter  (GlobalFilter, order = -1)   │              │
│  │     • Checks Bearer token on protected paths             │              │
│  │     • Parses JWT → extracts email, role, userId          │              │
│  │     • Forwards as X-User-* headers to downstream         │              │
│  │     • Returns 401 if token missing or invalid            │              │
│  │                                                           │              │
│  │  ② Route Predicates (Path matching)                       │              │
│  │     • /api/v1/auth/**  → lb://auth-service               │              │
│  │     • /api/v1/providers/** → lb://provider-service       │              │
│  │     • ... (10 routes total)                              │              │
│  │                                                           │              │
│  │  ③ Load Balancing (lb://)                                 │              │
│  │     • Resolves service hostnames via Eureka              │              │
│  │     • Distributes load if multiple instances running     │              │
│  └──────────────┬────────────────────────────────────────────┘              │
│                 │ registers & discovers services                             │
│                 ▼                                                            │
│  ┌──────────────────────────┐                                               │
│  │   Eureka Server  :8761   │ ← Service Registry                            │
│  └──────────────────────────┘                                               │
│                                                                              │
│  DOWNSTREAM MICROSERVICES (each on its own port + own MySQL DB)             │
│  ┌────────────┐ ┌─────────────────┐ ┌─────────────────┐ ┌───────────────┐ │
│  │auth-service│ │provider-service │ │schedule-service │ │appt-service   │ │
│  │  :8081     │ │  :8082          │ │  :8083          │ │  :8084        │ │
│  └────────────┘ └─────────────────┘ └─────────────────┘ └───────────────┘ │
│  ┌─────────────┐ ┌──────────────┐ ┌──────────────────┐ ┌───────────────┐  │
│  │pay-service  │ │review-service│ │notification-svc  │ │record-service │  │
│  │  :8085      │ │  :8086       │ │  :8087           │ │  :8088        │  │
│  └─────────────┘ └──────────────┘ └──────────────────┘ └───────────────┘  │
│  ┌──────────────┐ ┌──────────────────────┐                                  │
│  │admin-service │ │payment-gateway-svc   │                                  │
│  │  :8089       │ │  :8090               │                                  │
│  └──────────────┘ └──────────────────────┘                                  │
└──────────────────────────────────────────────────────────────────────────────┘
```

---

## 🛠 Tech Stack

| Layer | Technology | Version | Purpose |
|-------|-----------|---------|---------|
| **Language** | Java | 17 | Core language |
| **Framework** | Spring Boot | 3.2.0 | Application framework |
| **Gateway** | Spring Cloud Gateway | 2023.0.0 | Reactive routing + filtering |
| **Reactive Runtime** | Project Reactor (WebFlux) | — | Non-blocking I/O — **NOT Servlet-based** |
| **Service Discovery** | Spring Cloud Netflix Eureka Client | 2023.0.0 | Resolves `lb://service-name` to real IP:port |
| **Token Validation** | JWT (jjwt) | 0.11.5 | Parse + validate Bearer tokens at gateway level |
| **Boilerplate** | Lombok | 1.18.30 | `@Slf4j` logging |
| **Monitoring** | Spring Boot Actuator | 3.2.0 | `/actuator/health`, `/actuator/gateway` |
| **Build** | Maven | 3.9+ | Dependency management |

> ⚠️ **Important:** Spring Cloud Gateway is built on **Spring WebFlux (Reactive)**, NOT Spring Web (Servlet). This means you **cannot** add `spring-boot-starter-web` to this module — it will cause a startup conflict.

---

## 📁 Project Structure

```
api-gateway/
│
├── 📄 pom.xml                                      ← Maven dependencies
│
└── src/
    ├── main/
    │   ├── java/com/medibook/gateway/
    │   │   │
    │   │   ├── 🚀 ApiGatewayApplication.java        ← Spring Boot entry point
    │   │   │      @SpringBootApplication
    │   │   │      @EnableDiscoveryClient             ← registers with Eureka
    │   │   │
    │   │   └── filter/
    │   │       └── JwtAuthenticationFilter.java      ← GlobalFilter, order = -1
    │   │              Runs BEFORE all route filters
    │   │              Validates JWT on protected paths
    │   │              Forwards X-User-* headers downstream
    │   │
    │   └── resources/
    │       └── application.yml                      ← All routes, CORS, Eureka, JWT config
    │
    └── test/
        └── java/com/medibook/gateway/
            └── ApiGatewayApplicationTests.java      ← Spring context load test
```

---

## ✨ Features

### Routing
- ✅ **10 microservice routes** — every MediBook service has a dedicated path prefix
- ✅ **Eureka load-balanced routing** — uses `lb://service-name` so routes survive IP changes
- ✅ **Auto-discovery** — `discovery.locator.enabled: true` auto-routes any new registered service
- ✅ **Path preservation** — `StripPrefix=0` keeps the full original path intact when forwarding

### Security
- ✅ **Global JWT filter** — `JwtAuthenticationFilter` runs at order `-1` (first, before everything)
- ✅ **Public path whitelist** — OTP, register, login, refresh, provider browse, slot view, actuator, swagger skip JWT check
- ✅ **401 on missing/invalid token** — returns immediately without forwarding to downstream
- ✅ **JWT claims forwarding** — injects `X-User-Email`, `X-User-Role`, `X-User-Id` headers for downstream services

### CORS
- ✅ **Global CORS policy** — configured once at gateway, applies to all routes
- ✅ **React frontend support** — `localhost:3000` and `localhost:5173` (Vite) are allowed origins
- ✅ **All HTTP methods** — GET, POST, PUT, DELETE, OPTIONS all permitted
- ✅ **Credentials allowed** — `allowCredentials: true` for cookie-based flows

### Infrastructure
- ✅ **Eureka client** — registers itself and fetches registry for service resolution
- ✅ **Actuator** — exposes health, info, and gateway-specific endpoints
- ✅ **Environment variable config** — `JWT_SECRET` injected at runtime, never hardcoded

---

## 🗺 Route Table

All routes use `lb://` (load-balanced via Eureka). `StripPrefix=0` means the full path is preserved.

| Route ID | Path Prefix | Downstream Service | Port |
|----------|------------|-------------------|------|
| `auth-service` | `/api/v1/auth/**` | `lb://auth-service` | 8081 |
| `provider-service` | `/api/v1/providers/**` | `lb://provider-service` | 8082 |
| `schedule-service` | `/api/v1/slots/**` | `lb://schedule-service` | 8083 |
| `appointment-service` | `/api/v1/appointments/**` | `lb://appointment-service` | 8084 |
| `payment-service` | `/api/v1/payments/**` | `lb://payment-service` | 8085 |
| `review-service` | `/api/v1/reviews/**` | `lb://review-service` | 8086 |
| `notification-service` | `/api/v1/notifications/**` | `lb://notification-service` | 8087 |
| `record-service` | `/api/v1/records/**` | `lb://record-service` | 8088 |
| `admin-service` | `/api/v1/admin/**` | `lb://admin-service` | 8089 |
| `payment-gateway-service` | `/api/v1/gateway/**` | `lb://payment-gateway-service` | 8090 |

**Example routing in action:**
```
Client → POST http://localhost:8080/api/v1/auth/login
Gateway → validates path (public, no JWT needed)
Gateway → resolves lb://auth-service via Eureka → 127.0.0.1:8081
Gateway → forwards → POST http://127.0.0.1:8081/api/v1/auth/login
```

---

## 🔐 JWT Filter — How It Works

The `JwtAuthenticationFilter` implements `GlobalFilter` with `Ordered.getOrder() = -1`, meaning it executes **before any route filter** on every single request.

```
Incoming Request to Gateway
          │
          ▼
┌─────────────────────────────────────────────────────┐
│           JwtAuthenticationFilter                    │
│                                                      │
│  Step 1: Extract request path                        │
│          String path = request.getURI().getPath()   │
│                                                      │
│  Step 2: Check if path is PUBLIC                     │
│          PUBLIC_PATHS.stream()                       │
│              .anyMatch(p -> path.startsWith(p))      │
│                                                      │
│  Step 3a: PUBLIC PATH                                │
│           → chain.filter(exchange)   ✅ pass through │
│                                                      │
│  Step 3b: PROTECTED PATH                             │
│           → Read Authorization header                │
│           → Check "Bearer " prefix                   │
│           → If missing/bad → 401 UNAUTHORIZED        │
│                                                      │
│  Step 4: Parse JWT token                             │
│          Jwts.parserBuilder()                        │
│              .setSigningKey(hmacKey)                 │
│              .build()                                │
│              .parseClaimsJws(token)                  │
│              .getBody()                              │
│                                                      │
│  Step 5: On VALID token                              │
│          → Mutate request: add headers               │
│            X-User-Email = claims.getSubject()        │
│            X-User-Role  = claims.get("role")         │
│            X-User-Id    = claims.get("userId")       │
│          → chain.filter(mutatedExchange)  ✅ forward │
│                                                      │
│  Step 6: On INVALID / EXPIRED token                  │
│          → log.error(...)                            │
│          → response.setStatusCode(401)               │
│          → response.setComplete()  ❌ reject         │
└─────────────────────────────────────────────────────┘
          │
          ▼
   Downstream Microservice
   receives request WITH
   X-User-Email, X-User-Role, X-User-Id headers
```

---

## 🟢 Public vs Protected Paths

### Public Paths — No JWT Required

These paths start with the prefixes below and are allowed through without any token:

| Path Prefix | Why Public |
|------------|-----------|
| `/api/v1/auth/login` | User needs to login to get a token |
| `/api/v1/auth/register` | New user signup — no token yet |
| `/api/v1/auth/send-otp` | Pre-registration OTP — no token yet |
| `/api/v1/auth/verify-otp` | Pre-registration OTP verify — no token yet |
| `/api/v1/auth/refresh` | Token refresh — old token may be expired |
| `/api/v1/providers` | Guests can browse providers per PDF requirement |
| `/api/v1/slots` | Guests can view available slots per PDF requirement |
| `/actuator` | Health checks — no auth needed |
| `/swagger-ui` | API docs — no auth needed |
| `/api-docs` | OpenAPI spec — no auth needed |

### Protected Paths — JWT Required

Everything **not** in the public list above requires a valid `Bearer` token in the `Authorization` header. If the token is missing, malformed, expired, or has an invalid signature, the gateway returns:

```json
HTTP/1.1 401 Unauthorized
```

No request body is returned. The downstream service never receives the request.

---

## 📤 Downstream Headers Forwarded

When a valid JWT is presented, the gateway **mutates the request** before forwarding, adding these headers so downstream services know who is calling without re-validating the token:

| Header | Value Source | Example |
|--------|-------------|---------|
| `X-User-Email` | `claims.getSubject()` | `admin@medibook.com` |
| `X-User-Role` | `claims.get("role")` | `ADMIN` |
| `X-User-Id` | `claims.get("userId")` | `1` |

**How downstream services use these:**

```java
// In any downstream service controller:
@GetMapping("/my-endpoint")
public ResponseEntity<?> handle(
        @RequestHeader("X-User-Id") Long userId,
        @RequestHeader("X-User-Role") String role,
        @RequestHeader("X-User-Email") String email) {
    // No need to validate JWT again — gateway already did it
    // Just use these headers directly
}
```

---

## 🌐 CORS Configuration

Configured globally in `application.yml` — applies to every route automatically:

```yaml
globalcors:
  cors-configurations:
    '[/**]':
      allowedOrigins:
        - "http://localhost:3000"   # React CRA frontend
        - "http://localhost:5173"   # Vite dev server
      allowedMethods:
        - GET
        - POST
        - PUT
        - DELETE
        - OPTIONS
      allowedHeaders: "*"
      allowCredentials: true
```

**What this means:**
- React frontend running on port 3000 or 5173 can call `localhost:8080` without CORS errors
- All HTTP methods are allowed
- All request headers are accepted (`*`)
- Credentials (cookies, Authorization headers) are passed through

> For production, replace `allowedOrigins` with your actual deployed frontend domain, e.g., `https://medibook.in`.

---

## ✅ Prerequisites

| Tool | Version | Check Command |
|------|---------|---------------|
| Java JDK | 17+ | `java -version` |
| Maven | 3.9+ | `mvn -version` |
| Eureka Server | Running on :8761 | `curl http://localhost:8761/actuator/health` |
| Git | Any | `git --version` |

> The gateway itself needs **no database and no Redis**. It only needs Eureka to discover downstream services, and the JWT secret to validate tokens.

---

## 🔐 Environment Variables

Only **one** environment variable is required for the gateway:

| Variable | Required | Description | Example |
|----------|----------|-------------|---------|
| `JWT_SECRET` | ✅ Yes | Must match the secret used in `auth-service` exactly | `medibook-super-secret-key-at-least-256-bits-long` |

### Set on Windows (CMD)

```cmd
set JWT_SECRET=medibook-super-secret-key-must-be-at-least-256-bits-long-change-in-prod
```

### Set in Eclipse Run Config

1. Right-click `ApiGatewayApplication.java` → **Run As → Run Configurations**
2. Click **Environment** tab → **New**
3. Name: `JWT_SECRET`
4. Value: `medibook-super-secret-key-must-be-at-least-256-bits-long-change-in-prod`
5. Click **Apply** → **Run**

> **Critical:** The `JWT_SECRET` in the gateway must be **byte-for-byte identical** to the one in `auth-service`. If they differ, the gateway will reject every token as invalid even if auth-service issued it correctly.

---

## ▶ How to Run

```bash
# Step 1 — Clone and checkout branch
git clone https://github.com/your-username/MediBook-Microservices.git
cd MediBook-Microservices
git checkout feature/api-gateway

# Step 2 — Set the required env variable
set JWT_SECRET=medibook-super-secret-key-must-be-at-least-256-bits-long-change-in-prod

# Step 3 — Build
cd api-gateway
mvn clean install -DskipTests

# Step 4 — Run
mvn spring-boot:run

# OR in Eclipse:
# Right-click ApiGatewayApplication.java → Run As → Spring Boot App
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

INFO  Netty started on port 8080
INFO  Started ApiGatewayApplication in 4.3 seconds
API Gateway is Running.......!
```

> Notice it says **Netty** (not Tomcat) — this confirms the reactive WebFlux stack is running correctly.

---

## 🚦 Startup Order

The gateway **must start after** Eureka Server, because it needs Eureka to resolve `lb://service-name` routes. Microservices can start in any order after the gateway.

```
① Start Eureka Server    (port 8761)
   └─ Wait until "Started EurekaServerApplication" appears in console

② Start API Gateway      (port 8080)
   └─ Wait until "API Gateway is Running.......!" appears in console

③ Start auth-service     (port 8081)
④ Start provider-service (port 8082)
⑤ Start schedule-service (port 8083)
... and so on in any order
```

**Verify gateway registered with Eureka:**

Open `http://localhost:8761` in browser. You should see `API-GATEWAY` listed under "Instances currently registered with Eureka".

---

## 🧪 Postman Testing via Gateway

Once the gateway and at least `auth-service` are running, test **through the gateway** using port `8080` instead of `8081`:

### Base URL for all requests via gateway
```
http://localhost:8080
```

### 1. Send OTP (public — no token needed)

```
POST http://localhost:8080/api/v1/auth/send-otp
Content-Type: application/json

{
  "email": "newuser@gmail.com"
}
```

### 2. Verify OTP (public)

```
POST http://localhost:8080/api/v1/auth/verify-otp
Content-Type: application/json

{
  "email": "newuser@gmail.com",
  "otp": "482910"
}
```

### 3. Register (public)

```
POST http://localhost:8080/api/v1/auth/register
Content-Type: application/json

{
  "fullName": "Harshal Choudhary",
  "email": "newuser@gmail.com",
  "password": "Harshal@123",
  "phone": "9876543210",
  "role": "PATIENT"
}
```

### 4. Login (public — get your token here)

```
POST http://localhost:8080/api/v1/auth/login
Content-Type: application/json

{
  "email": "newuser@gmail.com",
  "password": "Harshal@123"
}
```

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9.....",
  "tokenType": "Bearer",
  "email": "newuser@gmail.com",
  "role": "PATIENT",
  "userId": 1
}
```
> Copy the `token`. You need it for all protected requests below.

### 5. Get Profile (protected — token required)

```
GET http://localhost:8080/api/v1/auth/profile/1
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.....
```

**What the gateway does behind the scenes:**
```
1. JwtAuthenticationFilter intercepts request
2. Reads Authorization header → extracts token
3. Parses JWT → subject = "newuser@gmail.com", role = "PATIENT", userId = 1
4. Adds headers: X-User-Email, X-User-Role, X-User-Id
5. Routes to lb://auth-service → resolves to localhost:8081
6. Forwards GET http://localhost:8081/api/v1/auth/profile/1 + X-User-* headers
7. auth-service responds → gateway forwards response back to client
```

### 6. Test 401 — missing token

```
GET http://localhost:8080/api/v1/auth/profile/1
(no Authorization header)
```

**Expected response:**
```
HTTP 401 Unauthorized
(empty body — gateway rejects before reaching auth-service)
```

### 7. Test 401 — tampered token

```
GET http://localhost:8080/api/v1/auth/profile/1
Authorization: Bearer invalidtoken.tampered.value
```

**Expected response:**
```
HTTP 401 Unauthorized
(gateway logs: JWT validation failed)
```

### 8. Browse Providers (public — no token needed)

```
GET http://localhost:8080/api/v1/providers
```

> No token required because `/api/v1/providers` is in the public paths list — guests can browse providers per the MediBook PDF requirements.

---

## 📊 Actuator Endpoints

The gateway exposes Spring Boot Actuator at:

| Endpoint | URL | Description |
|----------|-----|-------------|
| Health | `http://localhost:8080/actuator/health` | Gateway and Eureka connectivity status |
| Info | `http://localhost:8080/actuator/info` | Service name and version |
| Gateway routes | `http://localhost:8080/actuator/gateway/routes` | Lists all configured routes with predicates and filters |

**Sample `/actuator/gateway/routes` response:**
```json
[
  {
    "predicate": "Paths: [/api/v1/auth/**], match trailing slash: true",
    "route_id": "auth-service",
    "filters": ["[[StripPrefix parts = 0], order = 1]"],
    "uri": "lb://auth-service",
    "order": 0
  },
  {
    "predicate": "Paths: [/api/v1/providers/**], match trailing slash: true",
    "route_id": "provider-service",
    "uri": "lb://provider-service",
    "order": 0
  }
]
```

---

## ❌ Common Issues & Fixes

### Issue 1: `spring-boot-starter-web` conflict

```
Failed to start bean 'documentationPluginsBootstrapper'
OR
Unable to start embedded Tomcat/Netty conflict
```

**Cause:** Added `spring-boot-starter-web` to the gateway's `pom.xml`. The gateway uses WebFlux (Netty), not Servlet (Tomcat). These two cannot coexist.

**Fix:** Remove `spring-boot-starter-web` from `api-gateway/pom.xml`. Only use `spring-cloud-starter-gateway`.

---

### Issue 2: 503 Service Unavailable

```json
{
  "timestamp": "...",
  "path": "/api/v1/auth/login",
  "status": 503,
  "error": "Service Unavailable",
  "message": "Unable to find instance for auth-service"
}
```

**Cause:** The gateway can't find `auth-service` in Eureka.

**Fix — check all three:**
1. Is `auth-service` actually running? Check its console.
2. Is Eureka running on port 8761? Open `http://localhost:8761`.
3. Is `auth-service` listed in the Eureka dashboard? If not, check its `application.yml` Eureka config.

---

### Issue 3: 401 on every request including login

```
401 Unauthorized on POST /api/v1/auth/login
```

**Cause:** `/api/v1/auth/login` should be in the public path list but your `JWT_SECRET` may not be set, causing the filter to crash on startup, or the public path check has a bug.

**Fix:** Verify the `PUBLIC_PATHS` list in `JwtAuthenticationFilter.java` includes `/api/v1/auth/login`. Also verify `JWT_SECRET` env variable is set.

---

### Issue 4: JWT validation failed — "secret too short"

```
JWT validation failed: The signing key's size is 248 bits...
```

**Cause:** Your `JWT_SECRET` value is shorter than 256 bits (32 characters).

**Fix:** Use a secret that is at least 32 characters long:
```
medibook-super-secret-key-must-be-at-least-256-bits-long
```

---

### Issue 5: CORS error in browser

```
Access to XMLHttpRequest at 'http://localhost:8080/api/...'
from origin 'http://localhost:3000' has been blocked by CORS policy
```

**Cause:** Either the gateway isn't running, or your frontend origin isn't in `allowedOrigins`.

**Fix:** Add your frontend origin to `application.yml`:
```yaml
allowedOrigins:
  - "http://localhost:3000"
  - "http://localhost:5173"
  - "http://your-new-frontend-port"
```

---

### Issue 6: Eureka registration shows "STARTING" for a long time

**Cause:** The gateway registered but Eureka hasn't completed the handshake.

**Fix:** This is normal for 30–60 seconds after startup. Eureka heartbeat interval is 30 seconds. Wait and the status will change to `UP`.

---

### Issue 7: `Cannot determine local hostname`

```
WARN Cannot determine local hostname
```

**This is NOT an error.** This is a known Eureka client warning on Windows when the machine hostname isn't resolvable. The service still starts and registers correctly. To silence it, add to `application.yml`:

```yaml
eureka:
  instance:
    hostname: localhost
    prefer-ip-address: true
    ip-address: 127.0.0.1
```

---

## 🌿 Git Branch Info

```
Repository  : MediBook-Microservices
Branch      : feature/api-gateway
Base Branch : develop
Merge Target: develop (PR required)
Start After : feature/eureka-server must be merged first
```

**Commit convention:**
```
feat(gateway): add JWT authentication global filter
feat(gateway): add CORS global configuration
feat(gateway): add route for notification-service
fix(gateway): handle null userId in JWT claims forwarding
refactor(gateway): extract public paths to constant list
docs(gateway): update README with route table
```

---

## 📝 Key Design Decisions

**Why Spring Cloud Gateway instead of Zuul?**
Zuul 1 is blocking/servlet-based and no longer actively developed. Spring Cloud Gateway is the modern replacement — reactive, non-blocking, and officially supported by the Spring team.

**Why validate JWT at the gateway and not in each service?**
Centralizing JWT validation at the gateway means: (1) each downstream service doesn't need the JWT secret or jjwt library, (2) one place to update security logic, (3) downstream services simply trust the `X-User-*` headers the gateway injects.

**Why `StripPrefix=0`?**
The downstream services expose their APIs under the same path prefix (e.g., `auth-service` listens on `/api/v1/auth/**`). Without `StripPrefix=0`, the gateway would strip the path prefix before forwarding, breaking the downstream route matching.

**Why `order = -1` for the JWT filter?**
Order `-1` ensures the JWT filter runs before Spring Cloud Gateway's built-in route filters (which have order 0 and above). This guarantees that unauthorized requests are rejected before any routing logic executes.

---

## 👨‍💻 Developer Notes

- The gateway has **no database** — it is completely stateless
- The gateway shares the **same JWT secret** as `auth-service` — keep them in sync via the same environment variable
- `discovery.locator.enabled: true` means any new service that registers with Eureka is automatically accessible at `http://localhost:8080/service-name/**` without adding a manual route entry
- For production, add **rate limiting** using Spring Cloud Gateway's `RequestRateLimiter` filter with Redis
- For production, add **circuit breaking** using Spring Cloud CircuitBreaker (Resilience4j) to handle downstream service failures gracefully

---

### Author👨‍💻

[Harshal Choudhary](https://github.com/Harshal-25C) - Software Developer👨‍💻 | Cloud Enthusiast  
B.Tech - `[Computer Science & Engineering]`  
Java | Spring Boot | Spring Cloud | JWT & Security | React.js | Clean Architecture

---

<div align="center">

**MediBook API Gateway** | Part of MediBook Microservices Platform

*Confidential | MediBook Platform | Internal Use Only*

</div>