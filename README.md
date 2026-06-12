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
│   ├── User.java
│   ├── Role.java
│   ├── Booking.java
│   ├── Order.java
│   ├── Assignment.java
│   ├── ClassSchedule.java
│   └── TrainingCycle.java
├── dto/
│   ├── RegisterDto.java
│   ├── PageDto.java
│   └── ClientOrderDto.java
├── exception/
├── filter/
│   ├── EncodingFilter.java
│   └── LoggingFilter.java
└── util/pool/
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
- Switch interface language (EN / RU / KZ)

### Trainer
- View assigned clients dashboard
- Create, edit, and delete training assignments
- Set assignment status (Active / Completed / Revision Requested)

### Admin
- View all clients and trainers with pagination
- Activate and deactivate member accounts
- Set discount percentage for clients
- View and manage all orders (complete / cancel)

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
roles           — id, name (CLIENT | TRAINER | ADMIN)
trainers        — id (FK → members.id), bio, experience_years, certification
fitness_classes — id, name, category, capacity
class_schedules — id, class_id, trainer_id, scheduled_at, room
bookings        — id, user_id, schedule_id, booked_at
training_cycles — id, title, description, duration_weeks, price, is_active
orders          — id, user_id, cycle_id, trainer_id, status, paid_amount, created_at
assignments     — id, order_id, exercises, equipment, nutrition_plan, schedule_info, status
```

---

## Security

- Spring Security 6 with role-based access control
- BCrypt password hashing (strength 12)
- CSRF protection on all POST requests
- Custom 403 Access Denied page
- Session invalidation on logout

---

## Architecture Notes

- **No ORM** — all database access via Spring JDBC and manual `ResultSet` mapping
- **Connection Pool** — custom `ConnectionPool` implementation (10 connections)
- **Layered architecture** — Controller → Service → DAO → Database
- **Thymeleaf fragments** — shared `nav` and `footer` via `common/layout.html`
- **Filters** — `EncodingFilter` (UTF-8), `LoggingFilter` (request/response logging)

---

*Built as part of the EPAM Java Fundamentals program, 2026.*