-- FemFit Women's Fitness Club — Database Schema
-- PostgreSQL, UTF-8, 3NF normalized
-- Run: psql -U postgres -d femfit -f schema.sql



-- ══════════════════════════════════════
--  ROLES
-- ══════════════════════════════════════
CREATE TABLE IF NOT EXISTS roles (
                                     id      SERIAL PRIMARY KEY,
                                     name    VARCHAR(20) NOT NULL UNIQUE  -- CLIENT, TRAINER, ADMIN
    );

-- ══════════════════════════════════════
--  USERS  (key entity)
-- ══════════════════════════════════════
CREATE TABLE IF NOT EXISTS users (
                                     id                BIGSERIAL PRIMARY KEY,
                                     first_name        VARCHAR(100)        NOT NULL,
    last_name         VARCHAR(100)        NOT NULL,
    email             VARCHAR(255)        NOT NULL UNIQUE,
    phone             VARCHAR(20),
    password_hash     VARCHAR(255)        NOT NULL,   -- BCrypt
    birth_date        DATE,
    role_id           INT                 NOT NULL REFERENCES roles(id),
    is_active         BOOLEAN             NOT NULL DEFAULT TRUE,
    registration_date TIMESTAMP           NOT NULL DEFAULT NOW(),
    discount_percent  INT                 NOT NULL DEFAULT 0
    );

-- ══════════════════════════════════════
--  USER ADDRESSES  (multivalued attr)
-- ══════════════════════════════════════
CREATE TABLE IF NOT EXISTS user_addresses (
                                              id        BIGSERIAL PRIMARY KEY,
                                              user_id   BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    address   VARCHAR(300) NOT NULL
    );

-- ══════════════════════════════════════
--  TRAINERS  (key entity, extends users)
-- ══════════════════════════════════════
CREATE TABLE IF NOT EXISTS trainers (
                                        id               BIGINT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    bio              TEXT,
    experience_years INT          NOT NULL DEFAULT 0,
    certification    VARCHAR(200)
    );

-- ══════════════════════════════════════
--  TRAINER SPECIALIZATIONS (multivalued)
-- ══════════════════════════════════════
CREATE TABLE IF NOT EXISTS trainer_specializations (
                                                       id          BIGSERIAL PRIMARY KEY,
                                                       trainer_id  BIGINT NOT NULL REFERENCES trainers(id) ON DELETE CASCADE,
    name        VARCHAR(100) NOT NULL
    );

-- ══════════════════════════════════════
--  MEMBERSHIP PLANS  (key entity)
-- ══════════════════════════════════════
CREATE TABLE IF NOT EXISTS membership_plans (
                                                id               SERIAL PRIMARY KEY,
                                                name             VARCHAR(100)   NOT NULL,
    description      TEXT,
    price_per_month  DECIMAL(10,2)  NOT NULL,
    duration_months  INT            NOT NULL DEFAULT 1,
    access_level     VARCHAR(50)    NOT NULL,  -- BASIC, PREMIUM, ELITE
    is_active        BOOLEAN        NOT NULL DEFAULT TRUE
    );

-- ══════════════════════════════════════
--  SUBSCRIPTIONS  (associative entity)
-- ══════════════════════════════════════
CREATE TABLE IF NOT EXISTS subscriptions (
                                             id          BIGSERIAL PRIMARY KEY,
                                             user_id     BIGINT        NOT NULL REFERENCES users(id),
    plan_id     INT           NOT NULL REFERENCES membership_plans(id),
    start_date  DATE          NOT NULL,
    end_date    DATE          NOT NULL,
    status      VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE, EXPIRED, CANCELLED
    paid_amount DECIMAL(10,2) NOT NULL,
    created_at  TIMESTAMP     NOT NULL DEFAULT NOW()
    );

-- ══════════════════════════════════════
--  FITNESS CLASSES  (key entity)
-- ══════════════════════════════════════
CREATE TABLE IF NOT EXISTS fitness_classes (
                                               id               SERIAL PRIMARY KEY,
                                               name             VARCHAR(150)  NOT NULL,
    description      TEXT,
    capacity         INT           NOT NULL DEFAULT 20,
    duration_minutes INT           NOT NULL DEFAULT 60,
    difficulty_level VARCHAR(20)   NOT NULL DEFAULT 'BEGINNER', -- BEGINNER, INTERMEDIATE, ADVANCED
    is_active        BOOLEAN       NOT NULL DEFAULT TRUE
    );

-- ══════════════════════════════════════
--  CLASS CATEGORIES  (multivalued attr)
-- ══════════════════════════════════════
CREATE TABLE IF NOT EXISTS class_categories (
                                                id        SERIAL PRIMARY KEY,
                                                class_id  INT         NOT NULL REFERENCES fitness_classes(id) ON DELETE CASCADE,
    category  VARCHAR(50) NOT NULL  -- YOGA, CARDIO, STRENGTH, PILATES, DANCE
    );

-- ══════════════════════════════════════
--  CLASS SCHEDULES  (associative entity)
-- ══════════════════════════════════════
CREATE TABLE IF NOT EXISTS class_schedules (
                                               id           BIGSERIAL PRIMARY KEY,
                                               class_id     INT       NOT NULL REFERENCES fitness_classes(id),
    trainer_id   BIGINT    NOT NULL REFERENCES trainers(id),
    scheduled_at TIMESTAMP NOT NULL,
    room         VARCHAR(50),
    is_cancelled BOOLEAN   NOT NULL DEFAULT FALSE
    );

-- ══════════════════════════════════════
--  BOOKINGS  (weak entity)
-- ══════════════════════════════════════
CREATE TABLE IF NOT EXISTS bookings (
                                        id           BIGSERIAL PRIMARY KEY,
                                        user_id      BIGINT      NOT NULL REFERENCES users(id),
    schedule_id  BIGINT      NOT NULL REFERENCES class_schedules(id),
    booked_at    TIMESTAMP   NOT NULL DEFAULT NOW(),
    status       VARCHAR(20) NOT NULL DEFAULT 'CONFIRMED', -- CONFIRMED, CANCELLED, ATTENDED
    UNIQUE (user_id, schedule_id)
    );

-- ══════════════════════════════════════
--  TRAINING CYCLES  (key entity)
-- ══════════════════════════════════════
CREATE TABLE IF NOT EXISTS training_cycles (
                                               id          SERIAL PRIMARY KEY,
                                               title       VARCHAR(200)  NOT NULL,
    description TEXT,
    duration_weeks INT        NOT NULL,
    price       DECIMAL(10,2) NOT NULL,
    is_active   BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP     NOT NULL DEFAULT NOW()
    );

-- ══════════════════════════════════════
--  ORDERS  (associative entity)
-- ══════════════════════════════════════
CREATE TABLE IF NOT EXISTS orders (
                                      id              BIGSERIAL PRIMARY KEY,
                                      user_id         BIGINT        NOT NULL REFERENCES users(id),
    cycle_id        INT           NOT NULL REFERENCES training_cycles(id),
    trainer_id      BIGINT        REFERENCES trainers(id),
    status          VARCHAR(20)   NOT NULL DEFAULT 'PENDING', -- PENDING, ACTIVE, COMPLETED, CANCELLED
    paid_amount     DECIMAL(10,2) NOT NULL,
    created_at      TIMESTAMP     NOT NULL DEFAULT NOW(),
    completed_at    TIMESTAMP
    );

-- ══════════════════════════════════════
--  ASSIGNMENTS  (weak entity of order)
-- ══════════════════════════════════════
CREATE TABLE IF NOT EXISTS assignments (
                                           id              BIGSERIAL PRIMARY KEY,
                                           order_id        BIGINT      NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    exercises       TEXT,          -- workout routines
    equipment       TEXT,          -- required equipment
    nutrition_plan  TEXT,          -- dietary recommendations
    schedule_info   TEXT,          -- training schedule
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, COMPLETED, REVISION_REQUESTED
    created_at      TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP   NOT NULL DEFAULT NOW()
    );

-- ══════════════════════════════════════
--  REVIEWS  (weak entity of order)
-- ══════════════════════════════════════
CREATE TABLE IF NOT EXISTS reviews (
                                       id          BIGSERIAL PRIMARY KEY,
                                       order_id    BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    user_id     BIGINT NOT NULL REFERENCES users(id),
    trainer_id  BIGINT NOT NULL REFERENCES trainers(id),
    rating      INT    NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment     TEXT,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (order_id, user_id)
    );

-- ══════════════════════════════════════
--  INDEXES
-- ══════════════════════════════════════
CREATE INDEX IF NOT EXISTS idx_users_email        ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_role         ON users(role_id);
CREATE INDEX IF NOT EXISTS idx_subscriptions_user ON subscriptions(user_id);
CREATE INDEX IF NOT EXISTS idx_bookings_user      ON bookings(user_id);
CREATE INDEX IF NOT EXISTS idx_bookings_schedule  ON bookings(schedule_id);
CREATE INDEX IF NOT EXISTS idx_schedules_date     ON class_schedules(scheduled_at);
CREATE INDEX IF NOT EXISTS idx_orders_user        ON orders(user_id);
CREATE INDEX IF NOT EXISTS idx_orders_trainer     ON orders(trainer_id);
CREATE INDEX IF NOT EXISTS idx_orders_status      ON orders(status);