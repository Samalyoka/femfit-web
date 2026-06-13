# FemFit — Women's Fitness Club Web Application

> **EPAM Java Fundamentals Capstone Project**  
> A full-stack web application for a premium women's fitness club, built with Spring MVC, JDBC, and Thymeleaf.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Core, Spring MVC, Spring Security 6 |
| Database access | Spring JDBC (no ORM) |
| Database | PostgreSQL 15 |
| View layer | Thymeleaf 3.1 |
| Build tool | Maven |
| Server | Apache Tomcat 10.1 |
| Security | BCrypt password hashing |
| Validation | Hibernate Validator 8 |
| Testing | JUnit 5 + Mockito + AssertJ |
| IDE | IntelliJ IDEA |

---

## Project Structure

```
com.femfit
├── config/
│   ├── AppConfig.java
│   ├── WebMvcConfig.java
│   ├── SecurityConfig.java
│   ├── WebAppInitializer.java
│   ├── AuthInterceptor.java
│   └── FemFitUserDetailsService.java
├── controller/
│   ├── HomeController.java
│   ├── AuthController.java
│   ├── ClientController.java
│   ├── TrainerController.java
│   ├── AdminController.java
│   └── ScheduleController.java
├── service/ + service/impl/
├── dao/ + dao/impl/
├── model/
│   ├── Member.java
│   ├── Role.java
│   ├── Booking.java
│   ├── Order.java
│   ├── Assignment.java
│   ├── ClassSchedule.java
│   └── TrainingCycle.java
├── dto/
│   ├── RegisterDto.java
│   ├── ChangePasswordDto.java
│   ├── PageDto.java
│   ├── TrainerDto.java
│   └── ClientOrderDto.java
├── exception/
│   └── GlobalExceptionHandler.java
├── filter/
│   ├── EncodingFilter.java
│   └── LoggingFilter.java
└── datasource/
    └── ConnectionPool.java
```

---

## Prerequisites

- Java 21+
- PostgreSQL 15+
- Apache Tomcat 10.1+
- Maven 3.8+

---

## How to Run

### 1. Clone the repository

```bash
git clone https://github.com/yourusername/femfit-web.git
cd femfit-web
```

### 2. Create the database

```sql
CREATE DATABASE femfit;
```

### 3. Run the schema and seed data

```bash
psql -U postgres -d femfit -f src/main/resources/schema.sql
psql -U postgres -d femfit -f src/main/resources/data.sql
```

### 4. Configure database connection

Edit `src/main/resources/application.properties`:

```properties
db.url=jdbc:postgresql://localhost:5432/femfit
db.username=postgres
db.password=your_password
```

### 5. Build and deploy

```bash
mvn clean package
```

Copy the generated `.war` file to Tomcat's `webapps/` directory, or use the SmartTomcat plugin in IntelliJ IDEA.

### 6. Access the application

```
http://localhost:8080/femfit
```

---

## Features

### Client
- Register and log in
- View class schedule with category filters (Yoga, Cardio, Strength, Pilates, Dance)
- Book and cancel classes
- Browse training programs
- Choose a trainer when purchasing a program
- View orders and assignments from trainer
- Request revision on assignment
- Change password from profile page
- Switch interface language (EN / RU / KZ)

### Trainer
- View assigned clients dashboard
- Create, edit, and delete training assignments
- Set assignment status (Active / Completed / Revision Requested)

### Admin
- View all clients and trainers with pagination
- Activate and deactivate member accounts
- Set discount percentage for clients
- View and manage all orders (complete / cancel) with pagination

---

## Roles & Test Credentials

| Role | Email | Password |
|---|---|---|
| Admin | admin@femfit.kz | *(set during registration)* |
| Trainer | elena@femfit.kz | Trainer123! |
| Trainer | sofia@femfit.kz | Trainer123! |
| Trainer | maria@femfit.kz | Trainer123! |
| Client | anna@mail.kz | client123 |

---

## Internationalisation

The application supports three languages switchable from the navigation bar:

- 🇬🇧 English (`messages.properties`)
- 🇷🇺 Russian (`messages_ru.properties`)
- 🇰🇿 Kazakh (`messages_kz.properties`)

---

## Database Schema (key tables)

```
members           — id, first_name, last_name, email, password_hash, role_id, is_active, discount_percent
roles             — id, name (CLIENT | TRAINER | ADMIN)
trainers          — id (FK → members.id), bio, experience_years, certification
fitness_classes   — id, name, category, capacity
class_schedules   — id, class_id, trainer_id, scheduled_at, room, is_cancelled
bookings          — id, member_id, schedule_id, booked_at, status
training_cycles   — id, title, description, duration_weeks, price, is_active
orders            — id, member_id, cycle_id, trainer_id, status, paid_amount, created_at, completed_at
assignments       — id, order_id, exercises, equipment, nutrition_plan, schedule_info, status
reviews           — id, order_id, member_id, trainer_id, rating, comment, created_at
```

---

## Security

- Spring Security 6 with role-based access control
- BCrypt password hashing (strength 12)
- CSRF protection on all POST requests
- Custom 403 Access Denied and 500 Internal Server Error pages
- Session invalidation on logout

---

## Architecture Notes

- **No ORM** — all database access via Spring JDBC and manual `ResultSet` mapping
- **Connection Pool** — custom `ConnectionPool` implementation (10 connections), managed as a Spring singleton bean
- **Layered architecture** — Controller → Service → DAO → Database
- **Thymeleaf fragments** — shared `nav` and `footer` via `common/layout.html`
- **Filters** — `EncodingFilter` (UTF-8), `LoggingFilter` (request/response logging)
- **Global exception handling** — `GlobalExceptionHandler` (`@ControllerAdvice`) renders a custom 500 page for unexpected errors; 403 is handled by Spring Security's `accessDeniedPage`

---

## Design Patterns

### 1. Builder Pattern (via Lombok `@Builder`)

Used throughout `model/` and `dto/` for entities with many fields, some optional or
derived from SQL joins (e.g. `Order.trainerId` can be `null`, `Booking.className`/
`trainerName` come from joined tables): `Member`, `Order`, `Booking`, `Assignment`,
`TrainingCycle`, `TrainerDto`, `ClientOrderDto`.

Example, from `OrderDaoImpl.mapRow()`:

```java
return Order.builder()
        .id(rs.getLong("id"))
        .userId(rs.getLong("member_id"))
        .trainerId(rs.getObject("trainer_id") != null ? rs.getLong("trainer_id") : null)
        .status(rs.getString("status"))
        .build();
```

**Rationale**: avoids telescoping constructors/setters for entities with 8+ fields,
makes nullable/optional fields explicit at construction time, and keeps row-mapping
code in DAOs concise and readable.

### 2. Interceptor Pattern (`AuthInterceptor`, Spring `HandlerInterceptor`)

Registered in `WebMvcConfig.addInterceptors()`. Its `postHandle()` runs after every
controller method but before the view is rendered, injecting `isAuthenticated` and
`userRole` into the model.

**Rationale**: centralizes a cross-cutting concern (authentication status for
navigation rendering) instead of duplicating `model.addAttribute(...)` calls in every
controller. `common/layout.html :: nav` reads these attributes to show or hide
Login / Logout / Admin Panel links for the current user.

---

## Testing

Unit tests (JUnit 5 + Mockito + AssertJ) cover the service layer, isolating business
logic from JDBC/DAO implementations:

- `MemberServiceImplTest` — registration, password change, discount validation
  (including boundary values), role-based queries
- `BookingServiceImplTest` — booking, capacity limits (including the
  last-available-slot boundary), cancellation, upcoming bookings, visit counts
- `TrainerServiceImplTest` — trainer listing (DTO projection), client assignment
  management

---

*Built as part of the EPAM Java Fundamentals program, 2026.*