-- FemFit Women's Fitness Club — Database Schema
-- PostgreSQL, UTF-8
-- Run: psql -U postgres -d femfit -f schema.sql


-- ══════════════════════════════════════
--  ROLES
-- ══════════════════════════════════════
CREATE TABLE IF NOT EXISTS roles (
                                     id      SERIAL PRIMARY KEY,
                                     name    VARCHAR(20) NOT NULL UNIQUE  -- CLIENT, TRAINER, ADMIN
);

-- ══════════════════════════════════════
--  MEMBERS  (key entity)
-- ══════════════════════════════════════
CREATE TABLE IF NOT EXISTS members (
                                       id                BIGSERIAL PRIMARY KEY,
                                       first_name        VARCHAR(100)  NOT NULL,
                                       last_name         VARCHAR(100)  NOT NULL,
                                       email             VARCHAR(255)  NOT NULL UNIQUE,
                                       phone             VARCHAR(20),
                                       password_hash     VARCHAR(255)  NOT NULL,   -- BCrypt
                                       birth_date        DATE,
                                       role_id           INT           NOT NULL REFERENCES roles(id),
                                       is_active         BOOLEAN       NOT NULL DEFAULT TRUE,
                                       registration_date TIMESTAMP     NOT NULL DEFAULT NOW(),
                                       discount_percent  INT           NOT NULL DEFAULT 0
);

-- ══════════════════════════════════════
--  TRAINERS  (key entity, extends members)
-- ══════════════════════════════════════
CREATE TABLE IF NOT EXISTS trainers (
                                        id               BIGINT PRIMARY KEY REFERENCES members(id) ON DELETE CASCADE,
                                        bio              TEXT,
                                        experience_years INT          NOT NULL DEFAULT 0,
                                        certification    VARCHAR(200),
                                        photo_url        VARCHAR(255),
                                        specialization   VARCHAR(150),
                                        bio_ru            TEXT,
                                        bio_kz            TEXT,
                                        specialization_ru VARCHAR(150),
                                        specialization_kz VARCHAR(150),
                                        availability_status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE'
                                                             CHECK (availability_status IN ('AVAILABLE', 'UNAVAILABLE'))
);

-- ══════════════════════════════════════
--  FITNESS CLASSES  (key entity)
-- ══════════════════════════════════════
CREATE TABLE IF NOT EXISTS fitness_classes (
                                               id               SERIAL PRIMARY KEY,
                                               name             VARCHAR(150)  NOT NULL,
                                               description      TEXT,
                                               category         VARCHAR(50),  -- YOGA, CARDIO, STRENGTH, PILATES, DANCE
                                               capacity         INT           NOT NULL DEFAULT 20,
                                               duration_minutes INT           NOT NULL DEFAULT 60,
                                               difficulty_level VARCHAR(20)   NOT NULL DEFAULT 'BEGINNER', -- BEGINNER, INTERMEDIATE, ADVANCED
                                               is_active        BOOLEAN       NOT NULL DEFAULT TRUE,
                                               name_ru          VARCHAR(150),
                                               name_kz          VARCHAR(150),
                                               description_ru   TEXT,
                                               description_kz   TEXT
);

-- ══════════════════════════════════════
--  CLASS SCHEDULES  (associative entity)
-- ══════════════════════════════════════
-- ══════════════════════════════════════
--  CLASS SCHEDULES  (recurring weekly TEMPLATE — no specific date)
--  Each row = "this class, with this trainer, every <day_of_week> at <start_time>".
--  Concrete dated sessions are generated into class_occurrences below, so the
--  schedule never "runs out" — it repeats indefinitely until deactivated.
-- ══════════════════════════════════════
CREATE TABLE IF NOT EXISTS class_schedules (
                                               id           BIGSERIAL PRIMARY KEY,
                                               class_id     INT       NOT NULL REFERENCES fitness_classes(id),
                                               trainer_id   BIGINT    NOT NULL REFERENCES trainers(id),
                                               day_of_week  INT       NOT NULL CHECK (day_of_week BETWEEN 1 AND 7), -- ISO: 1=Monday .. 7=Sunday
                                               start_time   TIME      NOT NULL,
                                               room         VARCHAR(50),
                                               is_active    BOOLEAN   NOT NULL DEFAULT TRUE -- replaces is_cancelled: deactivates the whole recurring slot
);

-- ══════════════════════════════════════
--  CLASS OCCURRENCES  (a single dated instance of a recurring schedule)
--  Generated ahead of time (e.g. next N weeks) from class_schedules.
--  This is what members actually book — bookings.schedule_id points here.
-- ══════════════════════════════════════
CREATE TABLE IF NOT EXISTS class_occurrences (
                                                 id               BIGSERIAL PRIMARY KEY,
                                                 schedule_id      BIGINT    NOT NULL REFERENCES class_schedules(id) ON DELETE CASCADE,
                                                 occurrence_date  DATE      NOT NULL,
                                                 is_cancelled     BOOLEAN   NOT NULL DEFAULT FALSE,
                                                 UNIQUE (schedule_id, occurrence_date)
);

-- ══════════════════════════════════════
--  BOOKINGS  (weak entity)
-- ══════════════════════════════════════
CREATE TABLE IF NOT EXISTS bookings (
                                        id           BIGSERIAL PRIMARY KEY,
                                        member_id    BIGINT      NOT NULL REFERENCES members(id),
                                        schedule_id  BIGINT      NOT NULL REFERENCES class_occurrences(id), -- column name kept as "schedule_id" so app code (Booking.scheduleId) didn't need to change; it now points to a dated occurrence, not the recurring template
                                        booked_at    TIMESTAMP   NOT NULL DEFAULT NOW(),
                                        status       VARCHAR(20) NOT NULL DEFAULT 'CONFIRMED', -- CONFIRMED, CANCELLED, ATTENDED
                                        UNIQUE (member_id, schedule_id)
);

-- ══════════════════════════════════════
--  TRAINING CYCLES  (key entity)
-- ══════════════════════════════════════
CREATE TABLE IF NOT EXISTS training_cycles (
                                               id             SERIAL PRIMARY KEY,
                                               title          VARCHAR(200)  NOT NULL,
                                               description    TEXT,
                                               duration_weeks INT           NOT NULL,
                                               price          DECIMAL(10,2) NOT NULL,
                                               is_active      BOOLEAN       NOT NULL DEFAULT TRUE,
                                               created_at     TIMESTAMP     NOT NULL DEFAULT NOW(),
                                               photo_url      VARCHAR(255),
                                               title_ru       VARCHAR(200),
                                               title_kz       VARCHAR(200),
                                               description_ru TEXT,
                                               description_kz TEXT
);

-- ══════════════════════════════════════
--  ORDERS  (associative entity)
-- ══════════════════════════════════════
CREATE TABLE IF NOT EXISTS orders (
                                      id           BIGSERIAL PRIMARY KEY,
                                      member_id    BIGINT        NOT NULL REFERENCES members(id),
                                      cycle_id     INT           NOT NULL REFERENCES training_cycles(id),
                                      trainer_id   BIGINT        REFERENCES trainers(id),
                                      status       VARCHAR(20)   NOT NULL DEFAULT 'PENDING', -- PENDING, ACTIVE, COMPLETED, CANCELLED
                                      paid_amount  DECIMAL(10,2) NOT NULL,
                                      created_at   TIMESTAMP     NOT NULL DEFAULT NOW(),
                                      completed_at TIMESTAMP
);

-- ══════════════════════════════════════
--  ASSIGNMENTS  (weak entity of order)
-- ══════════════════════════════════════
CREATE TABLE IF NOT EXISTS assignments (
                                           id             BIGSERIAL PRIMARY KEY,
                                           order_id       BIGINT      NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
                                           exercises      TEXT,          -- workout routines
                                           equipment      TEXT,          -- required equipment
                                           nutrition_plan TEXT,          -- dietary recommendations
                                           schedule_info  TEXT,          -- training schedule
                                           status         VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, COMPLETED, REVISION_REQUESTED
                                           -- Per-item revision flags: which specific part(s) of the
                                           -- assignment the client asked the trainer to redo. status
                                           -- becomes REVISION_REQUESTED when any of these is true.
                                           revision_exercises_requested  BOOLEAN NOT NULL DEFAULT FALSE,
                                           revision_equipment_requested  BOOLEAN NOT NULL DEFAULT FALSE,
                                           revision_nutrition_requested  BOOLEAN NOT NULL DEFAULT FALSE,
                                           revision_schedule_requested   BOOLEAN NOT NULL DEFAULT FALSE,
                                           revision_comment              TEXT,
                                           created_at     TIMESTAMP   NOT NULL DEFAULT NOW(),
                                           updated_at     TIMESTAMP   NOT NULL DEFAULT NOW()
);

-- ══════════════════════════════════════
--  REVIEWS  (weak entity of order)
-- ══════════════════════════════════════
CREATE TABLE IF NOT EXISTS reviews (
                                       id         BIGSERIAL PRIMARY KEY,
                                       order_id   BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
                                       member_id  BIGINT NOT NULL REFERENCES members(id),
                                       trainer_id BIGINT NOT NULL REFERENCES trainers(id),
                                       rating     INT    NOT NULL CHECK (rating BETWEEN 1 AND 5),
                                       comment    TEXT,
                                       created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                                       UNIQUE (order_id, member_id)
);

-- ══════════════════════════════════════
--  INDEXES
-- ══════════════════════════════════════
CREATE INDEX IF NOT EXISTS idx_members_email     ON members(email);
CREATE INDEX IF NOT EXISTS idx_members_role      ON members(role_id);
CREATE INDEX IF NOT EXISTS idx_bookings_member   ON bookings(member_id);
CREATE INDEX IF NOT EXISTS idx_bookings_schedule ON bookings(schedule_id);
CREATE INDEX IF NOT EXISTS idx_schedules_day      ON class_schedules(day_of_week);
CREATE INDEX IF NOT EXISTS idx_occurrences_date   ON class_occurrences(occurrence_date);
CREATE INDEX IF NOT EXISTS idx_occurrences_schedule ON class_occurrences(schedule_id);
CREATE INDEX IF NOT EXISTS idx_orders_member     ON orders(member_id);
CREATE INDEX IF NOT EXISTS idx_orders_trainer    ON orders(trainer_id);
CREATE INDEX IF NOT EXISTS idx_orders_status     ON orders(status);