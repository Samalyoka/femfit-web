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
│   ├── TrainerProfileDto.java
│   ├── ClientOrderDto.java
│   └── BookingReminderDto.java
├── exception/
│   └── GlobalExceptionHandler.java
├── filter/
│   ├── EncodingFilter.java
│   └── LoggingFilter.java
├── service/
│   ├── EmailService.java
│   └── ReminderScheduler.java
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
git clone https://github.com/Samalyoka/femfit-web.git
cd femfit-web
```

### 2. Create the database

```sql
CREATE DATABASE femfit;
```

### 3. Run the schema and seed data

```bash
psql -U postgres -d femfit -f src/main/resources/db/schema.sql
psql -U postgres -d femfit -f src/main/resources/db/data.sql
psql -U postgres -d femfit -f src/main/resources/db/migration_v7_avatar_and_seed.sql
```

### 4. Configure database connection

Edit `src/main/resources/application.properties`:

```properties
db.url=jdbc:postgresql://localhost:5432/femfit
db.username=postgres
db.password=your_password
app.upload.base-path=/path/to/tomcat/webapps/femfit
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
- Upload and update profile avatar photo
- View class schedule with category filters (Yoga, Cardio, Strength, Pilates, Dance)
- Book and cancel classes
- Browse training programs
- Choose a trainer when purchasing a program
- View orders and assignments from trainer
- Request revision on assignment (exercises / equipment / nutrition / schedule)
- Change password from profile page
- View completed orders archive
- Leave reviews on completed orders
- Switch interface language (EN / RU / KZ)

### Trainer
- View assigned clients dashboard
- Upload and update profile avatar photo
- Create, edit, and delete training assignments
- Set assignment status (Active / Completed / Revision Requested)

### Admin
- View all clients and trainers with pagination
- Browse training programs with pagination
- Class schedule with pagination and category filters
- Activate and deactivate member accounts
- Set discount percentage for clients
- View and manage all orders (complete / cancel / reassign trainer) with pagination
- View and manage training cycles (create, edit, activate/deactivate)

---

## Roles & Test Credentials

| Role | Email | Password | Specialization |
|---|---|---|---|
| Admin | admin@femfit.kz | Admin123! | — |
| Trainer | elena@femfit.kz | Trainer123! | Yoga & Post-Natal |
| Trainer | sofia@femfit.kz | Trainer123! | HIIT & Cardio |
| Trainer | maria@femfit.kz | Trainer123! | Strength & Nutrition |
| Trainer | aizhan@femfit.kz | Trainer123! | Pilates & Barre |
| Trainer | dina@femfit.kz | Trainer123! | Dance & Cardio |
| Trainer | karina@femfit.kz | Trainer123! | Post-Natal Recovery |
| Trainer | saltanat@femfit.kz | Trainer123! | Beginner Coaching & Weight Loss |
| Trainer | zarina@femfit.kz | Trainer123! | Stretching & Mobility |
| Client | dana@mail.com | 12345678 | — |
| Client | sammy@mail.ru | 12345678 | — |
| Client | assem@mail.ru | 12345678 | — |

---

## Internationalisation

The application supports three languages switchable from the navigation bar:

- 🇬🇧 English (`messages_en.properties`)
- 🇷🇺 Russian (`messages_ru.properties`)
- 🇰🇿 Kazakh (`messages_kz.properties`)

All 29+ Thymeleaf templates are fully localised including flash messages, pagination labels, date formatting, and confirmation dialogs.

---

## Database Schema (key tables)

```
members           — id, first_name, last_name, email, password_hash, role_id, is_active, discount_percent, account_type, avatar_url
roles             — id, name (CLIENT | TRAINER | ADMIN)
trainers          — id (FK → members.id), bio, bio_ru, bio_kz, specialization, specialization_ru, specialization_kz, experience_years, certification, photo_url, availability_status
fitness_classes   — id, name, category, capacity
class_schedules   — id, class_id, trainer_id, start_time, room, is_active, is_cancelled
bookings          — id, member_id, schedule_id, booked_at, status
training_cycles   — id, title, title_ru, title_kz, description, duration_weeks, price, is_active, photo_url
orders            — id, member_id, cycle_id, trainer_id, status, paid_amount, created_at, completed_at
assignments       — id, order_id, exercises, equipment, nutrition_plan, schedule_info, status, revision_comment
reviews           — id, order_id, member_id, trainer_id, rating, comment, created_at
```

---

## Security

- Spring Security 6 with role-based access control
- BCrypt password hashing (strength 12)
- CSRF protection on all POST requests
- Custom 403 Access Denied and 500 Internal Server Error pages
- Session invalidation on logout
- Multipart file upload validation (type + size checks)

---

## Architecture Notes

- **No ORM** — all database access via Spring JDBC and manual `ResultSet` mapping
- **Connection Pool** — custom `ConnectionPool` implementation (10 connections), managed as a Spring singleton bean
- **Layered architecture** — Controller → Service → DAO → Database
- **Thymeleaf fragments** — shared `nav` and `footer` via `common/layout.html`
- **Filters** — `EncodingFilter` (UTF-8), `LoggingFilter` (request/response logging)
- **Global exception handling** — `GlobalExceptionHandler` (`@ControllerAdvice`) renders a custom 500 page for unexpected errors; 403 is handled by Spring Security's `accessDeniedPage`
- **Avatar upload** — files saved to `static/img/avatars/{memberId}.{ext}`, path stored in `members.avatar_url`

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
controller method but before the view is rendered, injecting `isAuthenticated`,
`userRole`, `currentUserName`, `currentUserInitials`, and `currentUserAvatar` into the model.

**Rationale**: centralizes a cross-cutting concern (authentication status for
navigation rendering) instead of duplicating `model.addAttribute(...)` calls in every
controller. `common/layout.html :: nav` reads these attributes to show or hide
Login / Logout / Admin Panel links and the user avatar for the current user.

### 3. Object Pool Pattern (`ConnectionPool`)

`ConnectionPool` (`datasource/ConnectionPool.java`) pre-creates a fixed set of JDBC
connections at startup and manages them via a `BlockingQueue<Connection>`. Callers
acquire a connection with `getConnection()` and return it with `releaseConnection()`.

**Rationale**: creating a new `Connection` per request is expensive (TCP handshake,
authentication, memory allocation). A pool reuses existing connections, bounding
resource usage and enabling safe concurrent access from multiple threads.

---

## SOLID Principles

### Single Responsibility Principle (SRP)
Every class has exactly one reason to change:
- `MemberServiceImpl` — member business logic only (registration, password, discount, avatar upload)
- `EmailService` — email sending only; called by `ReminderScheduler`, not by Services
- `LocalizedDateFormatter` — date formatting for Kazakh/Russian/English only
- `AuthInterceptor` — injecting auth model attributes only; no business logic
- `GlobalExceptionHandler` — HTTP error rendering only

### Open/Closed Principle (OCP)
All DAO and Service classes are accessed through interfaces (`MemberDao`, `OrderService`,
etc.). Adding a new implementation (e.g. a caching DAO) requires no changes to the
controllers or services that consume them — only a new class implementing the interface.

### Liskov Substitution Principle (LSP)
Every `*Impl` class fully honours its interface contract. Example: any code using
`MemberDao` can be given `MemberDaoImpl` without behaviour changes — all methods
return the same types, throw only documented exceptions, and never weaken preconditions.

### Interface Segregation Principle (ISP)
Each DAO interface exposes only the operations its clients need:
- `ReviewDao` — `save`, `findByOrderId`, `findByMemberId`, `findRecentForDisplay`, `existsByOrderId`
- `ClassScheduleDao` — schedule-specific queries; clients never see unrelated booking SQL
- No "fat" interfaces that force implementors to stub unused methods.

### Dependency Inversion Principle (DIP)
High-level modules depend on abstractions, not concretions:
- `AdminController` depends on `MemberService`, `OrderService`, `TrainerService` (interfaces)
- `MemberServiceImpl` depends on `MemberDao` (interface) and `PasswordEncoder` (interface)
- Spring wires the concrete implementations at runtime via `@Autowired` constructor injection

---

## Testing

Unit tests (JUnit 5 + Mockito + AssertJ) cover both the **Service** and **DAO** layers,
with 138+ `@Test` methods across 8 test classes. All tests run automatically via `mvn test`.
JaCoCo is configured to enforce ≥ 50% line coverage on `service.impl.*` and `dao.impl.*`.

**Service layer** — business logic isolated from JDBC with Mockito:
- `MemberServiceImplTest` (31) — registration, password change, discount validation (boundary values), role queries
- `OrderServiceImplTest` (28) — order placement, trainer assignment, status transitions
- `TrainerServiceImplTest` (16) — trainer listing, client assignment management
- `TrainingCycleServiceImplTest` (16) — cycle CRUD, activate/deactivate
- `ReviewServiceImplTest` (12) — review submission, duplicate detection
- `BookingServiceImplTest` (9) — booking delegation, exception propagation

**DAO layer** — JDBC mocked at `ConnectionPool / Connection / PreparedStatement / ResultSet`:
- `TrainingCycleDaoImplTest` (14) — findAll, findById, save (RETURNING mapping), setActive, update, exception wrapping, connection release
- `ReviewDaoImplTest` (11) — save (null trainerId → setNull), findByOrderId, findByMemberId, existsByOrderId, exception propagation

---

*Built as part of the EPAM Java Fundamentals program, 2026.*