# Resource Booking System

A RESTful API built with **Spring Boot 3** for managing resources and reservations with JWT-based authentication and Role-Based Access Control (RBAC).

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 21 |
| Framework | Spring Boot 3.2.4 |
| Security | Spring Security 6 + JWT (JJWT) |
| Database | MySQL (prod) / H2 in-memory (dev/test) |
| ORM | Spring Data JPA / Hibernate |
| Docs | SpringDoc OpenAPI 3 (Swagger UI) |
| Build | Maven 3.9+ |
| Testing | JUnit 5 + MockMvc integration tests |

---

## Features

- **JWT Authentication** — Stateless login via `POST /auth/login`, returns signed JWT
- **BCrypt Password Hashing** — All passwords stored securely
- **RBAC** — Two roles: `ADMIN` (full CRUD) and `USER` (read resources, manage own reservations)
- **Resource CRUD** — Create, read, update, delete bookable resources
- **Reservation Management** — Create/update/cancel reservations with time-slot tracking
- **Ownership Enforcement** — Users can only view/modify their own reservations
- **Filtering & Pagination** — Filter by status, price range; paginate and sort results
- **Global Exception Handling** — Consistent JSON error responses
- **Input Validation** — Field-level validation with descriptive messages
- **Swagger UI** — Full interactive API documentation

---

## Getting Started

### Prerequisites

- Java 21+
- Maven 3.9+
- MySQL 8+ (optional — use H2 dev profile to skip)

### Run with H2 (no MySQL required)

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### Run with MySQL

1. Create the database:
```sql
CREATE DATABASE resource_booking_db;
```

2. Update `src/main/resources/application.properties` with your credentials.

3. Run:
```bash
mvn spring-boot:run
```

---

## API Endpoints

### Authentication

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| POST | `/auth/login` | Login and receive JWT token | Public |
| POST | `/auth/register` | Register new user | Public |

### Resources

| Method | Endpoint | Description | Role |
|--------|----------|-------------|------|
| GET | `/api/resources` | List all resources (paginated) | Any |
| GET | `/api/resources/{id}` | Get resource by ID | Any |
| POST | `/api/resources` | Create resource | ADMIN |
| PUT | `/api/resources/{id}` | Update resource | ADMIN |
| DELETE | `/api/resources/{id}` | Delete resource | ADMIN |

### Reservations

| Method | Endpoint | Description | Role |
|--------|----------|-------------|------|
| GET | `/api/reservations` | List reservations (ADMIN: all, USER: own) | Authenticated |
| GET | `/api/reservations/{id}` | Get reservation (ADMIN: any, USER: own) | Authenticated |
| POST | `/api/reservations` | Create reservation | Authenticated |
| PUT | `/api/reservations/{id}` | Update reservation | Authenticated |
| DELETE | `/api/reservations/{id}` | Cancel reservation | Authenticated |

---

## Default Users

Seeded automatically on first startup:

| Username | Password | Role |
|----------|----------|------|
| `admin` | `admin123` | ADMIN |
| `user` | `user123` | USER |

---

## Authentication Flow

```
POST /auth/login
Body: { "username": "admin", "password": "admin123" }
Response: { "token": "eyJhbGciOiJIUzM4NCJ9...", "type": "Bearer" }

Use token in subsequent requests:
Authorization: Bearer <token>
```

---

## Running Tests

```bash
mvn test
```

Tests use H2 in-memory database — no external setup required.

---

## API Documentation

Once running, open:
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **H2 Console** (dev profile): http://localhost:8080/h2-console

---

## License

MIT
