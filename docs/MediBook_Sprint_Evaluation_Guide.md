# MediBook — Complete Sprint Evaluation Guide
### Everything You Need for Your Capgemini Virtual Evaluation

---

## TABLE OF CONTENTS
1. [Project Overview — How to Explain It](#1-project-overview)
2. [Full Architecture — Backend + Frontend](#2-full-architecture)
3. [Frontend Basics (HTML, CSS, JavaScript, React) — Zero to Hero](#3-frontend-basics)
4. [React Concepts with YOUR Code](#4-react-concepts-with-your-code)
5. [Backend — Spring Boot, Annotations, Layers](#5-backend-concepts)
6. [Service-to-Service Communication — Feign Client + RabbitMQ](#6-service-communication)
7. [Login Credentials Flow — Step by Step](#7-login-flow)
8. [Exception Handling — Your Actual Code](#8-exception-handling)
9. [JUnit + Mockito Testing — Your Actual Code](#9-junit-testing)
10. [API Gateway + Eureka — What & Why](#10-api-gateway-eureka)
11. [Why Passwords Are Hashed, Not Encrypted](#11-password-hashing)
12. [Redis, RabbitMQ — Extra Technologies](#12-redis-rabbitmq)
13. [Common Interview Questions with Answers](#13-interview-qa)

---

## 1. Project Overview

### How to Introduce MediBook (Say This!)

> "MediBook is a full-stack Online Appointment Booking System. It allows patients to search for doctors, view available time slots, and book appointments online. The system has three roles — Patient, Provider (Doctor), and Admin — each with their own dashboard and features.
>
> The backend is built with Java Spring Boot as a Microservices architecture — 8 independent services like auth-service, appointment-service, payment-service, etc. — all communicating through an API Gateway. The frontend is built with React.js using Axios for API calls. Services communicate synchronously via Feign Client and asynchronously via RabbitMQ for notifications."

### Three Roles in MediBook

| Role | What They Do |
|------|-------------|
| **Patient** | Register, search doctors, book/cancel/reschedule appointments, view medical records, make payments, submit reviews |
| **Provider (Doctor)** | Manage availability slots, view appointments, mark as complete, create medical records, view earnings |
| **Admin** | Verify providers, manage all users, view platform analytics, moderate reviews, generate revenue reports |

---

## 2. Full Architecture

### Big Picture Flow

```
Browser (React App)
       |
       | HTTP Requests (Axios)
       ↓
  API Gateway (port 8080)     ← Single entry point for ALL requests
       |
  Eureka Server               ← Service Registry (knows where each service runs)
       |
  ┌────┬────┬─────┬──────┬──────┬──────┬──────┬────────┐
  │auth│prov│sched│appt  │pay   │review│notif │record  │
  │8081│8082│8083 │8084  │8085  │8086  │8087  │8088    │
  └────┴────┴─────┴──────┴──────┴──────┴──────┴────────┘
       |                    |              |
      MySQL              RabbitMQ       Email/SMS
   (per service DB)    (async messages) (notifications)
```

### Each Microservice Has These 5 Layers

```
REST Resource (Controller)    ← Receives HTTP requests
      ↓
Service Interface             ← Contract / Blueprint
      ↓
ServiceImpl                   ← Business Logic
      ↓
Repository Interface          ← Database queries
      ↓
Entity (POJO)                 ← Database table mapping
```

### Frontend Structure

```
src/
├── main.jsx          ← Entry point (like main() in Java)
├── App.jsx           ← All routes defined here
├── utils/api.js      ← All API calls to backend
├── context/
│   └── ThemeContext.jsx  ← Global state (dark/light mode)
├── components/
│   └── Layout.jsx    ← Shared components (Sidebar, Topbar, Icons)
└── pages/
    ├── auth/         ← Login, Register, OTP pages
    ├── patient/      ← Patient Dashboard, Appointments, etc.
    ├── provider/     ← Provider Dashboard, Schedule, etc.
    └── admin/        ← Admin Dashboard, Users, etc.
```

---

## 3. Frontend Basics (Zero to Hero)

### HTML — The Structure

HTML (HyperText Markup Language) defines the **structure** of a web page. Think of it as the skeleton.

```html
<!-- Basic HTML structure -->
<!DOCTYPE html>
<html>
  <head>
    <title>MediBook</title>
  </head>
  <body>
    <h1>Welcome to MediBook</h1>
    <p>Book your appointments online</p>
    <button>Click Me</button>
    <input type="text" placeholder="Enter email" />
  </body>
</html>
```

**Common HTML Tags:**
- `<div>` — a container (like a box)
- `<h1>` to `<h6>` — headings
- `<p>` — paragraph
- `<button>` — clickable button
- `<input>` — text field
- `<form>` — form container
- `<img>` — image
- `<a href="...">` — link

### CSS — The Styling

CSS (Cascading Style Sheets) makes things **look good**. Think of it as the skin and clothes.

```css
/* MediBook's global.css uses CSS variables for theming */
:root {
  --primary: #2563eb;       /* blue color */
  --bg: #ffffff;            /* background */
  --text: #1e293b;          /* text color */
}

/* Dark mode — just change the variables! */
[data-theme="dark"] {
  --bg: #0f172a;
  --text: #f1f5f9;
}

.btn {
  background: var(--primary);
  color: white;
  padding: 10px 20px;
  border-radius: 8px;
}
```

**In MediBook**, CSS variables are used everywhere so dark/light mode works by changing just one attribute on `<html>` — see `ThemeContext.jsx`.

### JavaScript — The Behavior

JavaScript makes things **interactive**. Think of it as the muscles.

```javascript
// Variables
const name = "MediBook";        // cannot be reassigned
let count = 0;                  // can be reassigned

// Arrow function (modern way)
const greet = (name) => {
  return `Hello, ${name}!`;
};

// Async/Await (how MediBook calls APIs)
const fetchDoctors = async () => {
  try {
    const response = await api.get('/providers/all');
    console.log(response.data);
  } catch (error) {
    console.error('Failed:', error);
  }
};

// Array methods used in MediBook
const appointments = [...];
const completed = appointments.filter(a => a.status === 'COMPLETED');
const totalSpent = payments.reduce((sum, p) => sum + p.amount, 0);
```

**Key JS Concepts Used in MediBook:**

| Concept | Example in MediBook |
|---------|---------------------|
| Arrow functions | `const load = async () => { ... }` |
| Destructuring | `const { token, userId, role } = res.data;` |
| Template literals | `` `Bearer ${token}` `` |
| Optional chaining | `err.response?.data?.message` |
| Spread operator | `[...new Set(providerIds)]` |
| Promise.all | Load multiple APIs at same time |
| localStorage | Store JWT token in browser |

---

## 4. React Concepts with Your Code

### What is React?

React is a JavaScript **library** for building User Interfaces. It breaks the UI into **components** — reusable pieces of code.

**Old way (HTML + JS separately):** Hard to manage, spaghetti code  
**React way:** Everything is a component, state is managed, UI updates automatically

### What is a Component?

A component is a **JavaScript function that returns HTML-like code (called JSX)**.

```jsx
// Simplest possible component
function HelloWorld() {
  return <h1>Hello World</h1>;
}

// Export so other files can use it
export default HelloWorld;
```

**In MediBook**, every page is a component:
- `LoginPage` is a component
- `PatientDashboard` is a component
- `AdminDashboard` is a component

### JSX — HTML inside JavaScript

JSX looks like HTML but it IS JavaScript. Rules:
- Use `className` instead of `class`
- Use `{expression}` to put JavaScript inside JSX
- Every element must be closed (`<br />` not `<br>`)

```jsx
// From MediBook's LoginPage.jsx
function LoginPage() {
  const [error, setError] = useState('');

  return (
    <div className="auth-page">         {/* className, not class */}
      <h2>Sign in to continue care</h2>
      {error && <div className="alert">{error}</div>}  {/* JS expression */}
      <button onClick={() => console.log('clicked')}>
        Login
      </button>
    </div>
  );
}
```

### useState Hook — Managing Data in Components

`useState` lets a component **remember data** and **re-render** when it changes.

**Syntax:** `const [value, setValue] = useState(initialValue);`

```jsx
// FROM YOUR LoginPage.jsx
import React, { useState } from 'react';

export default function LoginPage() {
  // useState creates a variable AND a function to update it
  const [form, setForm] = useState({ email: '', password: '' });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [showPass, setShowPass] = useState(false);

  // When user types in email input
  const handle = (e) => {
    const { name, value } = e.target;
    setForm((prev) => ({ ...prev, [name]: value }));
    // This updates form.email or form.password
    // React automatically re-renders the component
  };

  return (
    <input
      type="email"
      name="email"
      value={form.email}        // controlled: React owns this value
      onChange={handle}         // fires when user types
    />
  );
}
```

**Rule:** Never modify state directly. Always use the setter.
```jsx
// WRONG
form.email = 'test';

// CORRECT
setForm({ ...form, email: 'test' });
```

### useEffect Hook — Side Effects (API Calls, Timers)

`useEffect` runs code **after the component renders**. Used for:
- Fetching data from API
- Setting up timers
- Listening to events

**Syntax:**
```jsx
useEffect(() => {
  // code to run
  return () => { /* cleanup */ }; // optional
}, [dependencies]); // when to run
```

**When does it run?**
- `[]` — only once when component first loads (like `@PostConstruct`)
- `[id]` — runs when `id` changes
- No array — runs after EVERY render (usually bad)

**FROM YOUR PatientDashboard.jsx:**
```jsx
import React, { useState, useEffect } from 'react';

export default function PatientDashboard() {
  const user = getUser(); // get logged in user from localStorage
  const [upcoming, setUpcoming] = useState([]);
  const [records, setRecords] = useState([]);
  const [loading, setLoading] = useState(true);

  // useEffect runs ONCE when dashboard first loads
  useEffect(() => {
    const load = async () => {
      try {
        // Promise.all fetches 4 APIs at the same time (parallel!)
        const [ua, rec, pay, allAppts] = await Promise.all([
          appointmentAPI.getUpcoming(user.userId),
          recordAPI.getByPatient(user.userId),
          paymentAPI.getByPatient(user.userId),
          appointmentAPI.getByPatient(user.userId),
        ]);
        setUpcoming(ua.data || []);
        setRecords(rec.data || []);
      } catch (err) {
        console.error(err);
      } finally {
        setLoading(false); // hide spinner regardless
      }
    };
    load();
  }, []); // empty array = run only once
  
  return <div>...</div>;
}
```

**FROM YOUR ThemeContext.jsx — useEffect with dependency:**
```jsx
// Runs every time 'theme' changes
useEffect(() => {
  document.documentElement.setAttribute('data-theme', theme); // apply to HTML
  localStorage.setItem('medibook-theme', theme); // save to browser
}, [theme]); // dependency: only runs when theme value changes
```

**FROM YOUR Layout.jsx — useEffect with cleanup (interval):**
```jsx
// Poll for new notifications every 30 seconds
useEffect(() => {
  if (!user) return;
  const interval = setInterval(fetchNotifs, 30000);
  return () => clearInterval(interval); // CLEANUP: stop polling when component unmounts
}, []);
```

### useState vs useEffect — Quick Comparison

| Feature | useState | useEffect |
|---------|----------|-----------|
| Purpose | Store data | Run side effects |
| When it runs | When setter is called | After render |
| Example | `setLoading(true)` | API calls, timers |
| Updates UI? | Yes (triggers re-render) | Not directly |

### Props — Passing Data Between Components

Props are how a **parent component passes data to a child component**. Like method parameters.

```jsx
// Parent component
function PatientDashboard() {
  return <Topbar title="Patient Dashboard" />;
  //              ^^^^ this is a prop
}

// Child component (from Layout.jsx)
function Topbar({ title }) {  // receives props
  return <h1>{title}</h1>;
}
```

**Props vs State:**

| Feature | Props | State |
|---------|-------|-------|
| Where it lives | Passed from parent | Lives inside component |
| Who can change it | Parent only | Component itself |
| Example | `<Topbar title="..." />` | `const [count, setCount] = useState(0)` |

### Routing — How Navigation Works in MediBook

**FROM YOUR App.jsx:**
```jsx
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';

export default function App() {
  return (
    <BrowserRouter>  {/* enables browser URL routing */}
      <Routes>
        {/* Public routes — anyone can access */}
        <Route path="/" element={<LandingPage />} />
        <Route path="/login" element={<LoginPage />} />
        
        {/* Protected routes — must be logged in as Patient */}
        <Route 
          path="/patient" 
          element={
            <PrivateRoute role="Patient">
              <PatientDashboard />
            </PrivateRoute>
          } 
        />
      </Routes>
    </BrowserRouter>
  );
}

// This is the guard — checks if user is logged in
function PrivateRoute({ children, role }) {
  const user = getUser(); // from localStorage
  if (!user) return <Navigate to="/login" replace />;  // redirect to login
  if (role && user.role !== role) return <Navigate to="/" replace />;
  return children; // render the page
}
```

**`useNavigate`** — Navigate programmatically in code:
```jsx
// FROM LoginPage.jsx
const navigate = useNavigate();

// After successful login:
if (user.role === 'Patient') navigate('/patient', { replace: true });
else if (user.role === 'Provider') navigate('/provider', { replace: true });
else if (user.role === 'Admin') navigate('/admin', { replace: true });
```

### Lazy Loading — How MediBook Loads Pages

**FROM YOUR App.jsx:**
```jsx
import React, { lazy, Suspense } from 'react';

// LAZY LOADING: don't load this file until user actually visits /patient
const PatientDashboard = lazy(() => import('./pages/patient/PatientDashboard'));
const AdminDashboard   = lazy(() => import('./pages/admin/AdminDashboard'));
// ... all pages are lazy loaded
```

**What is Lazy Loading?**
- Without lazy loading: ALL page code is downloaded when user opens the website (slow!)
- With lazy loading: Only the page the user is visiting is downloaded (fast!)

**Suspense** shows a loading spinner while the page is being downloaded:
```jsx
<Suspense fallback={<AppFallback />}>
  <Routes>...</Routes>
</Suspense>
```

**Eager Loading** = load everything upfront (opposite of lazy)

### Context API — Global State

**FROM YOUR ThemeContext.jsx:**
```jsx
import { createContext, useContext, useState, useEffect } from 'react';

// 1. Create context (like a global variable container)
const ThemeContext = createContext();

// 2. Provider wraps the whole app
export function ThemeProvider({ children }) {
  const [theme, setTheme] = useState(() => {
    return localStorage.getItem('medibook-theme') || 'light';
  });

  useEffect(() => {
    document.documentElement.setAttribute('data-theme', theme);
    localStorage.setItem('medibook-theme', theme);
  }, [theme]);

  const toggleTheme = () => setTheme(t => t === 'light' ? 'dark' : 'light');

  return (
    <ThemeContext.Provider value={{ theme, toggleTheme }}>
      {children}   {/* everything inside can access theme */}
    </ThemeContext.Provider>
  );
}

// 3. Any component can use it
export function useTheme() {
  return useContext(ThemeContext);
}
```

**Used in Layout.jsx:**
```jsx
import { useTheme } from '../context/ThemeContext';

export function ThemeToggle() {
  const { theme, toggleTheme } = useTheme(); // access global theme
  return (
    <button onClick={toggleTheme}>
      {theme === 'light' ? <Moon size={16} /> : <Sun size={16} />}
    </button>
  );
}
```

### Where Icons Come From

**Answer:** Icons come from the `lucide-react` library (installed via npm).

**FROM YOUR LoginPage.jsx:**
```jsx
import {
  Mail,        // envelope icon
  Lock,        // padlock icon
  Eye,         // show password
  EyeOff,      // hide password
  HeartPulse,  // heart with pulse
  ArrowRight,  // → arrow
} from 'lucide-react';

// Used like HTML elements:
<Mail size={16} className="auth-input-icon" />
<Lock size={16} className="auth-input-icon" />
```

**FROM Layout.jsx:**
```jsx
import {
  LayoutDashboard,  // dashboard grid icon
  Calendar,         // calendar icon
  Bell,             // notification bell
  LogOut,           // logout icon
  Users,            // people icon
  Sun, Moon,        // theme toggle
} from 'lucide-react';
```

**In `package.json`:**
```json
{
  "dependencies": {
    "lucide-react": "^0.344.0"
  }
}
```

### First File in Frontend — Where Does React Start?

**The flow is:**
1. `index.html` — has `<div id="root"></div>`
2. `main.jsx` — React mounts itself here
3. `App.jsx` — defines all routes
4. Individual page components load

**FROM YOUR main.jsx:**
```jsx
import React from 'react';
import ReactDOM from 'react-dom/client';
import App from './App';
import { ThemeProvider } from './context/ThemeContext';

// Find the <div id="root"> in index.html and put React inside it
ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <ThemeProvider>  {/* wrap everything in theme context */}
      <App />        {/* App has all the routes */}
    </ThemeProvider>
  </React.StrictMode>
);
```

---

## 5. Backend Concepts

### Spring Boot Stereotype Annotations

These annotations tell Spring what role a class plays.

| Annotation | Meaning | Example in MediBook |
|-----------|---------|---------------------|
| `@RestController` | REST API controller (returns JSON) | `AuthResource`, `AppointmentResource` |
| `@Controller` | MVC controller (returns HTML views) | Not used (we use React frontend) |
| `@Service` | Business logic layer | `AuthServiceImpl`, `AppointmentServiceImpl` |
| `@Repository` | Database access layer | `UserRepository`, `AppointmentRepository` |
| `@Component` | Generic Spring bean | `JwtUtil`, `JwtFilter` |

**They are all just `@Component` underneath:**
```
@Component
├── @Controller → @RestController
├── @Service
└── @Repository
```

**FROM YOUR AuthResource.java:**
```java
@RestController          // This is a REST controller
@RequestMapping("/auth") // All URLs start with /auth
public class AuthResource {

    @Autowired
    private AuthService authService;  // inject dependency

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        // ...
    }
}
```

**FROM YOUR AuthServiceImpl.java:**
```java
@Service  // This is a service bean
public class AuthServiceImpl implements AuthService {

    @Autowired
    private UserRepository userRepository; // inject repository

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public User register(RegisterRequest request) {
        // business logic here
    }
}
```

**FROM YOUR UserRepository.java:**
```java
@Repository  // This is a repository bean
public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    List<User> findAllByRole(String role);
}
```

### @RestController vs @Controller

| Feature | @Controller | @RestController |
|---------|-------------|-----------------|
| Returns | HTML view (Thymeleaf) | JSON data |
| Used for | MVC (server-side rendering) | REST APIs |
| Used in MediBook | Not used | Every service uses this |

`@RestController = @Controller + @ResponseBody`

### Dependency Injection + @Autowired

**Dependency Injection (DI)** = Spring creates objects and injects them where needed. You don't use `new` keyword.

**Why?** Loose coupling. Easy testing. Single instances (Singleton by default).

**Two ways to inject:**

**1. @Autowired (field injection) — Used in MediBook:**
```java
@Service
public class AuthServiceImpl implements AuthService {

    @Autowired  // Spring injects UserRepository automatically
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;
}
```

**2. Constructor Injection (preferred by Spring docs):**
```java
@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // Spring sees this constructor and injects dependencies
    public AuthServiceImpl(UserRepository userRepository, 
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }
}
```

**Why constructor injection is better:**
- Fields are `final` (immutable)
- Easier to test (pass mocks directly)
- No circular dependency issues

### Layered Architecture — MVC Flow

When a request comes in, it flows like this:

```
HTTP Request
     ↓
@RestController (AuthResource)
     ↓ calls
@Service Interface (AuthService)
     ↓ implemented by
@Service Impl (AuthServiceImpl)
     ↓ calls
@Repository (UserRepository)
     ↓ talks to
Database (MySQL)
     ↓ returns data back up
HTTP Response (JSON)
```

### JPA / Hibernate

**JPA** = Java Persistence API — a standard for mapping Java objects to database tables.
**Hibernate** = the actual implementation of JPA that Spring uses.

**FROM YOUR User.java entity:**
```java
@Entity            // this class maps to a database table
@Table(name = "users")  // table name
public class User {

    @Id                                           // primary key
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // auto-increment
    private int userId;

    @Column(nullable = false, unique = true)  // NOT NULL + UNIQUE constraint
    private String email;

    @Column(nullable = false)
    private String role;

    @PrePersist  // runs before INSERT
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
```

**JpaRepository gives you free methods:**
```java
userRepository.save(user);            // INSERT or UPDATE
userRepository.findById(id);          // SELECT by ID
userRepository.findAll();             // SELECT all
userRepository.delete(user);          // DELETE
userRepository.existsByEmail(email);  // custom query method
```

**Custom Query Methods** — Spring creates SQL from method names:
```java
Optional<User> findByEmail(String email);
// → SELECT * FROM users WHERE email = ?

List<User> findAllByRole(String role);
// → SELECT * FROM users WHERE role = ?

boolean existsByEmail(String email);
// → SELECT COUNT(*) > 0 FROM users WHERE email = ?
```

### Lombok Annotations

**FROM YOUR User.java:**
```java
@Data           // generates getters, setters, equals, hashCode, toString
@NoArgsConstructor  // generates empty constructor
@AllArgsConstructor // generates constructor with all fields
@Builder        // enables builder pattern: User.builder().email("x").build()
public class User {
    // ...
}
```

Without Lombok, you'd write 100+ lines of boilerplate. Lombok generates it for you.

### @Transactional

```java
@Override
@Transactional  // FROM AppointmentServiceImpl.bookAppointment()
public Appointment bookAppointment(AppointmentRequest request) {
    // If ANY line throws an exception, ALL database changes are ROLLED BACK
    // Ensures atomicity — either everything succeeds or nothing does
    
    Appointment saved = appointmentRepository.save(appointment);
    slotClient.bookSlot(request.getSlotId()); // book the slot too
    
    return saved;
}
```

`@Transactional(readOnly = true)` — used for read-only queries for performance optimization.

### Java Stream API

Used for processing collections:
```java
// FROM PatientDashboard logic (in backend)
List<Appointment> completed = appointments.stream()
    .filter(a -> a.getStatus().equals("COMPLETED"))
    .collect(Collectors.toList());

// Sum total revenue
double total = payments.stream()
    .filter(p -> p.getStatus().equals("SUCCESS"))
    .mapToDouble(Payment::getAmount)
    .sum();

// Get unique provider IDs
Set<Integer> providerIds = appointments.stream()
    .map(Appointment::getProviderId)
    .collect(Collectors.toSet());
```

### Interface + Implementation Pattern (Why We Do It)

**FROM YOUR code:**
```java
// AuthService.java — INTERFACE (contract)
public interface AuthService {
    User register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    void logout(String token);
}

// AuthServiceImpl.java — IMPLEMENTATION
@Service
public class AuthServiceImpl implements AuthService {
    @Override
    public User register(RegisterRequest request) {
        // actual logic
    }
}
```

**Why?**
1. **Loose coupling** — controller depends on interface, not implementation
2. **Easy testing** — can mock the interface in JUnit tests
3. **Multiple implementations** — can swap implementation without changing controller

---

## 6. Service Communication

### Feign Client (Synchronous)

When `appointment-service` needs to book a slot, it calls `schedule-service` using Feign Client.

**FROM YOUR SlotClient.java:**
```java
package com.medibook.appointment.client;

import org.springframework.cloud.openfeign.FeignClient;

// name = "schedule-service" → Eureka finds this service
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

**How Feign Client works:**
1. You define an interface with the URL patterns
2. Feign generates the actual HTTP client code automatically
3. Eureka resolves `schedule-service` to the actual IP:port

**Used in AppointmentServiceImpl.java:**
```java
@Autowired
private SlotClient slotClient; // inject like any other dependency

// When booking appointment:
SlotDto slot = slotClient.getSlotById(request.getSlotId()); // HTTP GET
slotClient.bookSlot(request.getSlotId()); // HTTP PUT
```

### RabbitMQ (Asynchronous)

When an appointment is booked, notification-service needs to send emails. Instead of calling it directly (slow), appointment-service publishes a message to RabbitMQ. notification-service picks it up and sends the email.

**FROM AppointmentEventPublisher.java logic:**
```
appointment-service → publishes "BOOKED" event → RabbitMQ queue
                                                        ↓
                              notification-service → picks up → sends email/SMS
```

**Why async (RabbitMQ) vs sync (Feign)?**

| Scenario | Use |
|---------|-----|
| Need immediate response | Feign Client (sync) |
| Fire and forget (notifications) | RabbitMQ (async) |
| Don't want to block the main flow | RabbitMQ |

---

## 7. Login Flow

### Step-by-Step Login in MediBook

```
1. User enters email + password in LoginPage.jsx
           ↓
2. React calls: authAPI.login({ email, password })
           ↓
3. Axios sends POST /auth/login to API Gateway (port 8080)
           ↓
4. API Gateway routes to auth-service (port 8081)
           ↓
5. AuthResource.login() receives the request
           ↓
6. Calls authService.login(request)
           ↓
7. AuthServiceImpl.login():
   - Find user by email in DB
   - Check if account is active
   - Verify password using BCrypt: passwordEncoder.matches(input, hash)
           ↓
8. If password OK → check phone number
   If no phone → return { requiresPhone: true }
           ↓
9. If phone exists → sendOtp(email) → return { otpSent: true }
           ↓
10. React shows OTP page
           ↓
11. User enters OTP → POST /auth/verify-otp
           ↓
12. JwtUtil.generateToken(email, role, userId) → creates JWT
           ↓
13. Return { token, userId, role, fullName }
           ↓
14. React: saveAuth(token, user) → stores in localStorage
           ↓
15. Navigate to /patient or /provider or /admin based on role
```

**JWT Token explained:**
```
eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJqb2huQGV4LmNvbSIsInJvbGUiOiJQYXRpZW50IiwidXNlcklkIjoxfQ.signature
     ↑ Header                    ↑ Payload (email, role, userId)                          ↑ Signature
```

**FROM JwtUtil.java:**
```java
@Component
public class JwtUtil {

    @Value("${jwt.secret}")    // read from application.properties
    private String secret;

    public String generateToken(String email, String role, int userId) {
        return Jwts.builder()
                .setSubject(email)           // who this token is for
                .claim("role", role)         // custom claim: role
                .claim("userId", userId)     // custom claim: userId
                .setIssuedAt(new Date())     // when issued
                .setExpiration(new Date(System.currentTimeMillis() + expiration)) // expires in 24h
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }
}
```

**Every subsequent request sends JWT:**
```javascript
// FROM api.js — axios interceptor
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('medibook_token');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});
```

### Why Passwords Are Hashed (Not Encrypted)

**Encryption** is reversible — if the key is stolen, passwords are exposed.  
**Hashing** is one-way — you cannot reverse it to get the original password.

```
Password: "password123"
     ↓ BCrypt hash
Hash: "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy"

To verify: BCrypt.matches("password123", hash) → true
           BCrypt.matches("wrongpassword", hash) → false
```

**Even if database is hacked, attacker cannot recover passwords.**

**FROM AuthServiceImpl.java:**
```java
// On register: hash the password
.passwordHash(passwordEncoder.encode(request.getPassword()))

// On login: verify
if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
    throw new UnauthorizedException("Invalid email or password.");
}
```

---

## 8. Exception Handling

### Your GlobalExceptionHandler

**FROM GlobalExceptionHandler.java:**
```java
@RestControllerAdvice  // catches exceptions from ALL controllers
public class GlobalExceptionHandler {

    // Handle "resource not found" (404)
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            ResourceNotFoundException ex,
            HttpServletRequest request) {

        ErrorResponse error = new ErrorResponse(
            404,                    // HTTP status code
            "Not Found",            // error type
            ex.getMessage(),        // specific message
            request.getRequestURI(), // which URL caused it
            LocalDateTime.now()     // when it happened
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    // Handle duplicate resource (409)
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicate(...) { ... }

    // Handle bad request (400)
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(...) { ... }

    // Catch-all for unexpected errors (500)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex, ...) {
        // Never expose internal error details to client!
        return ResponseEntity.status(500).body("Something went wrong");
    }
}
```

### Custom Exceptions

```java
// ResourceNotFoundException.java
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String resource, String field, Object value) {
        super(resource + " not found with " + field + ": " + value);
        // "User not found with email: john@example.com"
    }
}

// DuplicateResourceException.java
public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String resource, String field, Object value) {
        super(resource + " already exists with " + field + ": " + value);
    }
}

// BadRequestException.java
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}

// UnauthorizedException.java
public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }
}
```

### Exception Propagation (Important Interview Question!)

> "Class A calls Class B, Class B calls Class C, Class C throws exception. If not handled in C or B, where does it go?"

**Answer:** It propagates UP the call stack until something catches it.

```
ClassC.methodC() → throws ResourceNotFoundException
         ↑ propagates
ClassB.methodB() → not handled here
         ↑ propagates
ClassA.methodA() → not handled here
         ↑ propagates
GlobalExceptionHandler → catches it HERE! → returns 404 JSON response
```

In MediBook: AuthResource calls AuthServiceImpl, AuthServiceImpl calls UserRepository. If user not found, ResourceNotFoundException goes all the way up to GlobalExceptionHandler.

### How to Write a Custom Exception (For Evaluation)

```java
// Step 1: Create the exception class
public class AppointmentNotFoundException extends RuntimeException {
    public AppointmentNotFoundException(int id) {
        super("Appointment not found with id: " + id);
    }
}

// Step 2: Throw it in service
public Appointment getById(int id) {
    return appointmentRepository.findById(id)
        .orElseThrow(() -> new AppointmentNotFoundException(id));
}

// Step 3: Handle in GlobalExceptionHandler
@ExceptionHandler(AppointmentNotFoundException.class)
public ResponseEntity<?> handleAppointmentNotFound(AppointmentNotFoundException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(Map.of("error", ex.getMessage()));
}
```

---

## 9. JUnit Testing

### Test Structure in MediBook

**FROM AuthServiceImplTest.java:**
```java
@ExtendWith(MockitoExtension.class)  // use Mockito for mocking
class AuthServiceImplTest {

    // @Mock creates a fake version of these classes
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtil jwtUtil;

    private AuthServiceImpl authService; // what we're testing

    @BeforeEach  // runs before EACH test
    void setUp() {
        authService = new AuthServiceImpl();
        // inject mocks manually
        injectField(authService, "userRepository", userRepository);
        injectField(authService, "passwordEncoder", passwordEncoder);
    }

    @Test  // mark this as a test method
    void register_success_savesUserWithEncodedPassword() {
        // ARRANGE — set up mock behavior
        RegisterRequest req = new RegisterRequest();
        req.setEmail("alice@x.com");
        req.setPassword("pwd");
        req.setRole("Patient");

        when(userRepository.existsByEmail("alice@x.com")).thenReturn(false);
        when(passwordEncoder.encode("pwd")).thenReturn("encoded_hash");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        // ACT — call the method
        User result = authService.register(req);

        // ASSERT — verify results
        assertThat(result.getEmail()).isEqualTo("alice@x.com");
        assertThat(result.getPasswordHash()).isEqualTo("encoded_hash");
        assertThat(result.isActive()).isTrue();
        verify(userRepository).save(any(User.class)); // verify save was called
    }

    @Test
    void register_duplicateEmail_throwsDuplicateResourceException() {
        // ARRANGE
        RegisterRequest req = new RegisterRequest();
        req.setEmail("existing@medibook.com");

        when(userRepository.existsByEmail("existing@medibook.com")).thenReturn(true);

        // ACT + ASSERT — expect exception
        assertThatThrownBy(() -> authService.register(req))
            .isInstanceOf(DuplicateResourceException.class);
        verify(userRepository, never()).save(any()); // verify save was NOT called
    }
}
```

### Key JUnit Annotations

| Annotation | Purpose |
|-----------|---------|
| `@Test` | Marks a test method |
| `@BeforeEach` | Runs before each test (setup) |
| `@AfterEach` | Runs after each test (cleanup) |
| `@ExtendWith(MockitoExtension.class)` | Enable Mockito in tests |
| `@Mock` | Create a mock object |
| `@InjectMocks` | Inject mocks into the class under test |

### Key Mockito Methods

```java
// Mock behavior — "when X is called, return Y"
when(userRepository.findByEmail("test@x.com")).thenReturn(Optional.of(user));
when(passwordEncoder.encode("pwd")).thenReturn("hashed");
when(userRepository.save(any(User.class))).thenReturn(savedUser);

// Verify — "check that this method was called"
verify(userRepository).save(any(User.class));        // called once
verify(userRepository, never()).save(any());          // never called
verify(userRepository, times(2)).findByEmail(any()); // called twice

// Expect exception
assertThatThrownBy(() -> authService.register(req))
    .isInstanceOf(DuplicateResourceException.class)
    .hasMessageContaining("already exists");
```

### How to Write a JUnit Test for Class A that Uses Class B (Interview Question)

```
Question: Class A uses Class B. Write JUnit test for Class A.

Answer:
1. Use @Mock to create a fake (mock) of Class B
2. Use @InjectMocks to inject the mock into Class A
3. Use when(...).thenReturn(...) to define mock behavior
4. Call the method on Class A
5. Assert the result with assertThat(...)
6. Use verify(...) to check Class B was called correctly
```

---

## 10. API Gateway + Eureka

### API Gateway — The Single Entry Point

**What is it?** API Gateway is like the **reception desk** of a hospital. Every visitor (request) goes through reception first, then gets directed to the right department (service).

**FROM application.yml (api-gateway):**
```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: auth-service
          uri: lb://auth-service        # lb = load balanced
          predicates:
            - Path=/auth/**             # /auth/login goes to auth-service

        - id: appointment-service
          uri: lb://appointment-service
          predicates:
            - Path=/appointments/**     # /appointments/book goes to appointment-service

        - id: payment-service
          uri: lb://payment-service
          predicates:
            - Path=/payments/**
```

**Benefits of API Gateway:**
- Single entry point (frontend only needs to know port 8080)
- CORS handled in one place
- Load balancing
- JWT validation can be centralized

### Eureka Server — Service Registry

**What is it?** Eureka is like a **phone directory**. Every service registers itself with Eureka saying "I'm `auth-service`, running at `localhost:8081`". When one service wants to call another, it asks Eureka for the address.

```
auth-service starts → registers with Eureka: "I'm auth-service at port 8081"
schedule-service starts → registers with Eureka: "I'm schedule-service at port 8083"

appointment-service wants to call schedule-service:
  → asks Eureka: "Where is schedule-service?"
  → Eureka says: "localhost:8083"
  → appointment-service calls it
```

**Why Eureka?** Services don't hardcode each other's URLs. Flexible, scalable.

---

## 11. Password Hashing

**Already covered in section 7. Quick summary:**

| | Encryption | Hashing (BCrypt) |
|--|-----------|-----------------|
| Reversible? | Yes (with key) | No |
| Used for | Transmitting data | Storing passwords |
| If DB hacked? | Decrypt with key → exposed | Cannot reverse → safe |
| In MediBook | Not used for passwords | `passwordEncoder.encode()` |

---

## 12. Redis + RabbitMQ

### Redis (mentioned in non-functional requirements)

**What is Redis?** An in-memory key-value store used as a **cache**.

**Why in MediBook?**
- Slot availability is read very frequently
- Instead of querying MySQL every time, cache the result in Redis
- Redis returns data in microseconds vs MySQL in milliseconds

```
First request: MySQL → fetch slots → save in Redis
Next 100 requests: Redis → return cached slots (super fast!)
When slot is booked: invalidate Redis cache
```

**Session store:** JWT tokens that are "logged out" (blacklisted) are stored in Redis.

### RabbitMQ (Used in appointment-service)

**What is RabbitMQ?** A **message broker** — allows services to communicate asynchronously.

**FROM AppointmentEventPublisher.java:**
```java
// When appointment is booked, publish a message
// appointment-service doesn't wait for notification-service
// It just puts the message in a queue and continues

public void publishBooked(Appointment appointment) {
    AppointmentEventDto event = new AppointmentEventDto(...);
    rabbitTemplate.convertAndSend(exchange, "appointment.booked", event);
}
// notification-service picks this up and sends email/SMS
```

---

## 13. Interview Q&A (Sample Questions from Your Friends)

### Q: Explain your whole project
> See Section 1 — say the 3-role explanation and mention microservices architecture.

### Q: Write a React component for "Hello World"
```jsx
import React from 'react';

function HelloWorld() {
  return <h1>Hello World</h1>;
}

export default HelloWorld;
```

### Q: Write a component with one input
```jsx
import React, { useState } from 'react';

function NameInput() {
  const [name, setName] = useState('');

  return (
    <div>
      <input
        type="text"
        value={name}
        onChange={(e) => setName(e.target.value)}
        placeholder="Enter your name"
      />
      <p>You typed: {name}</p>
    </div>
  );
}

export default NameInput;
```

### Q: What is the first file in frontend?
> `index.html` → `main.jsx` → `App.jsx`

### Q: What is the first file in backend?
> `AuthServiceApplication.java` (or whichever service) — it has `@SpringBootApplication` and the `main()` method.

```java
@SpringBootApplication
public class AuthServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}
```

### Q: What is @Autowired? What is Dependency Injection?
> DI = Spring creates and manages objects. @Autowired tells Spring to inject a dependency automatically. See Section 5.

### Q: What is @RestControllerAdvice and @ExceptionHandler?
```
@RestControllerAdvice = catches exceptions from ALL @RestController classes globally
@ExceptionHandler(SomeException.class) = handles a specific exception type
```

### Q: What are hooks? How used in your project?
> Hooks are special functions in React that let functional components use state and side effects.
> - `useState` — store component data (`const [form, setForm] = useState({})`)
> - `useEffect` — run code after render (API calls, timers)
> - `useNavigate` — programmatic navigation
> - `useContext` — access global context (theme)

### Q: Show where collection framework is used
**Backend (Java):**
```java
// List — ordered collection of appointments
List<Appointment> appointments = appointmentRepository.findByPatientId(id);

// Optional — may or may not have a user
Optional<User> user = userRepository.findByEmail(email);

// Map — store providers by ID in frontend logic
Map<Integer, Provider> providerMap = new HashMap<>();

// Set — unique provider IDs
Set<Integer> uniqueIds = new HashSet<>();
```

### Q: Show where abstraction/interface is used
```java
// Interface = abstraction
public interface AuthService {
    User register(RegisterRequest request);  // what, not how
}

// Concrete class = implementation
@Service
public class AuthServiceImpl implements AuthService {
    @Override
    public User register(RegisterRequest request) {
        // actual logic
    }
}
// Controller uses AuthService (interface), not AuthServiceImpl
// This is abstraction — controller doesn't know the details
```

### Q: What is @Entity, @Id, @GeneratedValue, @Table?
```java
@Entity  // this class = database table
@Table(name = "users")  // specific table name (optional, defaults to class name)
public class User {

    @Id  // primary key
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // AUTO_INCREMENT in MySQL
    private int userId;

    @Column(nullable = false, unique = true)  // NOT NULL + UNIQUE in DB
    private String email;
}
```

### Q: What is @Builder (Lombok)?
```java
// Without @Builder:
User user = new User();
user.setEmail("x@x.com");
user.setRole("Patient");

// With @Builder: (cleaner)
User user = User.builder()
    .email("x@x.com")
    .role("Patient")
    .isActive(true)
    .build();
```

### Q: Props vs State?
See table in Section 4. 
Props = passed from parent, read-only in child.  
State = owned by the component, can be changed.

### Q: How does backend communicate with frontend?
```
Frontend (React) → HTTP Request (Axios) → API Gateway (port 8080) → Microservice → MySQL
                ← HTTP Response (JSON) ←                           ←             ←
```
JWT token is included in every request header: `Authorization: Bearer <token>`

### Q: Explain Razorpay logic
```
1. Patient clicks "Pay with Razorpay" in BookAppointmentPage.jsx
2. React loads Razorpay script dynamically
3. Calls /payments/initiate → backend creates Razorpay order → returns order_id
4. Razorpay checkout opens in browser (handles payment UI)
5. On success → Razorpay calls backend /payments/verify with payment_id
6. Backend verifies signature → marks payment as SUCCESS
7. React shows confirmation
```

### Q: How does lazy loading improve performance?
> Without lazy loading: All 20+ page components are downloaded at once (slow, large bundle). With lazy loading (`lazy()` + `Suspense`), only the page the user visits is downloaded. Initial load is much faster.

### Q: Create a REST API (example)
```java
@RestController
@RequestMapping("/doctors")
public class DoctorResource {

    @Autowired
    private DoctorService doctorService;

    @GetMapping("/{id}")
    public ResponseEntity<Doctor> getDoctor(@PathVariable int id) {
        return ResponseEntity.ok(doctorService.getById(id));
    }

    @PostMapping("/add")
    public ResponseEntity<Doctor> addDoctor(@RequestBody Doctor doctor) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(doctorService.save(doctor));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Doctor> updateDoctor(@PathVariable int id, @RequestBody Doctor doctor) {
        return ResponseEntity.ok(doctorService.update(id, doctor));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDoctor(@PathVariable int id) {
        doctorService.delete(id);
        return ResponseEntity.ok(Map.of("message", "Deleted"));
    }
}
```

### Q: Create Feign Client (example)
```java
@FeignClient(name = "provider-service")
public interface ProviderClient {

    @GetMapping("/providers/{id}")
    Provider getProviderById(@PathVariable("id") int id);

    @PutMapping("/providers/{id}/verify")
    void verifyProvider(@PathVariable("id") int id);
}
```

### Q: Write JUnit test (example)
```java
@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void login_success() {
        User user = User.builder()
            .email("test@x.com")
            .passwordHash("hashed")
            .role("Patient")
            .isActive(true)
            .build();

        when(userRepository.findByEmail("test@x.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "hashed")).thenReturn(true);

        // Act
        AuthResponse result = authService.login(new LoginRequest("test@x.com", "password"));

        // Assert
        assertNotNull(result);
        verify(userRepository).findByEmail("test@x.com");
    }
}
```

---

## Quick Reference Cheat Sheet

### Annotations Summary

| Annotation | Layer | Description |
|-----------|-------|-------------|
| `@SpringBootApplication` | Main | Starts Spring Boot |
| `@RestController` | Controller | REST API endpoints |
| `@RequestMapping` | Controller | Base URL mapping |
| `@GetMapping` `@PostMapping` `@PutMapping` `@DeleteMapping` | Controller | HTTP methods |
| `@PathVariable` | Controller | URL parameter `/users/{id}` |
| `@RequestBody` | Controller | Parse JSON from request body |
| `@Service` | Service | Business logic |
| `@Repository` | Repository | Database access |
| `@Autowired` | Any | Inject dependency |
| `@Transactional` | Service | DB transaction |
| `@Entity` | Entity | Maps to DB table |
| `@Id` | Entity | Primary key |
| `@GeneratedValue` | Entity | Auto-increment |
| `@Column` | Entity | Column constraints |
| `@RestControllerAdvice` | Exception | Global exception handler |
| `@ExceptionHandler` | Exception | Handle specific exception |
| `@FeignClient` | Client | Inter-service HTTP calls |
| `@Data` `@Builder` | Lombok | Reduce boilerplate |
| `@Test` `@Mock` `@BeforeEach` | JUnit | Testing |
| `@ExtendWith(MockitoExtension.class)` | JUnit | Enable Mockito |

### React Hooks Summary

| Hook | Purpose | Example |
|------|---------|---------|
| `useState` | Store component data | `const [name, setName] = useState('')` |
| `useEffect` | Run after render | API calls, timers |
| `useNavigate` | Navigate to route | `navigate('/patient')` |
| `useParams` | Get URL params | `const { providerId } = useParams()` |
| `useContext` | Access context | `const { theme } = useTheme()` |

---

*Good luck with your Capgemini evaluation! You've built something impressive — MediBook is a complete, production-grade healthcare platform. Walk in with confidence!*
GUIDE_EOF