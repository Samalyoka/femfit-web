# FemFit — Women's Fitness Club

Capstone project for Java Development course.

## Tech Stack
- Java 17, Spring MVC, Spring Security, JDBC (no ORM)
- PostgreSQL, Thymeleaf, Bootstrap 5
- Maven, JUnit 5, Mockito, JaCoCo

## Setup
1. Create database: `createdb femfit`
2. Run schema: `psql -d femfit -f src/main/resources/db/schema.sql`
3. Run seed: `psql -d femfit -f src/main/resources/db/data.sql`
4. Copy `application.properties` and set your DB credentials
5. Run: `mvn tomcat7:run`
6. Open: http://localhost:8080/femfit

## Test Accounts
| Role    | Email               | Password    |
|---------|---------------------|-------------|
| Admin   | admin@femfit.kz     | admin123    |
| Trainer | elena@femfit.kz     | trainer123  |
| Client  | anna@mail.kz        | client123   |

## Run Tests
```bash
mvn test
mvn jacoco:report   # coverage report in target/site/jacoco/
```
```