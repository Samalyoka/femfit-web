-- FemFit — Initial seed data
-- Run after schema.sql: psql -U postgres -d femfit -f data.sql
-- Passwords are BCrypt hashes of: admin123, trainer123, client123


-- ── ROLES ──
INSERT INTO roles (name) VALUES ('CLIENT'), ('TRAINER'), ('ADMIN')
ON CONFLICT (name) DO NOTHING;

-- ── USERS ──
-- Admin: password = admin123
INSERT INTO users (first_name, last_name, email, phone, password_hash, birth_date, role_id)
VALUES ('Admin', 'FemFit', 'admin@femfit.kz', '+77001234567',
        '$2a$12$KIx6.tqNcKmK3Kz8GW4.1.wGqF5K3bT7yJ0yLvhU3lC7mPNYvE2.q',
        '1990-01-01',
        (SELECT id FROM roles WHERE name = 'ADMIN'))
ON CONFLICT (email) DO NOTHING;

-- Trainers: password = trainer123
INSERT INTO users (first_name, last_name, email, phone, password_hash, birth_date, role_id)
VALUES
    ('Elena',  'Morozova', 'elena@femfit.kz',  '+77012345678',
     '$2a$12$LJy7.upOdLnL4Lz9HX5.2.xHrG6L4cU8zK1zMwhV4mD8nQOZwF3.r', '1992-03-15',
     (SELECT id FROM roles WHERE name = 'TRAINER')),
    ('Sofia',  'Romanova',  'sofia@femfit.kz',  '+77023456789',
     '$2a$12$LJy7.upOdLnL4Lz9HX5.2.xHrG6L4cU8zK1zMwhV4mD8nQOZwF3.r', '1988-07-22',
     (SELECT id FROM roles WHERE name = 'TRAINER')),
    ('Maria',  'Volkova',   'maria@femfit.kz',  '+77034567890',
     '$2a$12$LJy7.upOdLnL4Lz9HX5.2.xHrG6L4cU8zK1zMwhV4mD8nQOZwF3.r', '1995-11-08',
     (SELECT id FROM roles WHERE name = 'TRAINER'))
ON CONFLICT (email) DO NOTHING;

-- Clients: password = client123
INSERT INTO users (first_name, last_name, email, phone, password_hash, birth_date, role_id)
VALUES
    ('Anna',    'Kim',      'anna@mail.kz',    '+77045678901',
     '$2a$12$MKz8.vqPeLpM5Mb0IY6.3.yIsMh7M5dV9aL2aNxiW5nEoPQawG4.s', '1998-05-20',
     (SELECT id FROM roles WHERE name = 'CLIENT')),
    ('Aigerim', 'Bekova',   'aigerim@mail.kz', '+77056789012',
     '$2a$12$MKz8.vqPeLpM5Mb0IY6.3.yIsMh7M5dV9aL2aNxiW5nEoPQawG4.s', '2000-09-14',
     (SELECT id FROM roles WHERE name = 'CLIENT')),
    ('Zarina',  'Seitkali', 'zarina@mail.kz',  '+77067890123',
     '$2a$12$MKz8.vqPeLpM5Mb0IY6.3.yIsMh7M5dV9aL2aNxiW5nEoPQawG4.s', '1996-12-03',
     (SELECT id FROM roles WHERE name = 'CLIENT'))
ON CONFLICT (email) DO NOTHING;

-- ── TRAINERS ──
INSERT INTO trainers (id, bio, experience_years, certification)
SELECT id, 'Certified yoga and pilates instructor with 8 years experience.', 8, 'RYT-500'
FROM users WHERE email = 'elena@femfit.kz' ON CONFLICT DO NOTHING;

INSERT INTO trainers (id, bio, experience_years, certification)
SELECT id, 'HIIT and cardio specialist. Former athletics coach.', 6, 'ACE-CPT'
FROM users WHERE email = 'sofia@femfit.kz' ON CONFLICT DO NOTHING;

INSERT INTO trainers (id, bio, experience_years, certification)
SELECT id, 'Strength training and nutrition expert.', 5, 'NSCA-CSCS'
FROM users WHERE email = 'maria@femfit.kz' ON CONFLICT DO NOTHING;

-- ── TRAINER SPECIALIZATIONS ──
INSERT INTO trainer_specializations (trainer_id, name)
SELECT t.id, s.name FROM trainers t
                             JOIN users u ON t.id = u.id
                             JOIN (VALUES
                                       ('elena@femfit.kz', 'Yoga'),
                                       ('elena@femfit.kz', 'Pilates'),
                                       ('sofia@femfit.kz', 'HIIT'),
                                       ('sofia@femfit.kz', 'Cardio'),
                                       ('maria@femfit.kz', 'Strength Training'),
                                       ('maria@femfit.kz', 'Nutrition')
) AS s(email, name) ON u.email = s.email;

-- ── MEMBERSHIP PLANS ──
INSERT INTO membership_plans (name, description, price_per_month, duration_months, access_level)
VALUES
    ('Basic',   'Gym access and 4 group classes per month.', 49900.00, 1, 'BASIC'),
    ('Premium', 'Unlimited classes, 2 personal sessions, sauna access.', 89900.00, 1, 'PREMIUM'),
    ('Elite',   'Everything in Premium plus unlimited personal sessions and nutrition consulting.', 139900.00, 1, 'ELITE')
ON CONFLICT DO NOTHING;

-- ── FITNESS CLASSES ──
INSERT INTO fitness_classes (name, description, capacity, duration_minutes, difficulty_level)
VALUES
    ('Morning Yoga',    'Gentle flow to start your day with intention.',          15, 60, 'BEGINNER'),
    ('HIIT Cardio',     'High-intensity interval training for maximum burn.',     20, 45, 'ADVANCED'),
    ('Pilates Flow',    'Core strengthening and flexibility.',                    12, 60, 'INTERMEDIATE'),
    ('Power Strength',  'Full-body resistance training with weights.',            15, 60, 'INTERMEDIATE'),
    ('Zumba Dance',     'Fun Latin-inspired cardio dance workout.',               25, 60, 'BEGINNER'),
    ('Yoga Restore',    'Deep stretching and relaxation.',                        12, 75, 'BEGINNER'),
    ('Body Pump',       'Barbell workout for all major muscle groups.',           20, 60, 'INTERMEDIATE'),
    ('Evening Cardio',  'Energizing cardio session to end the day.',              20, 45, 'BEGINNER'),
    ('Stretching',      'Full-body flexibility and mobility work.',               15, 45, 'BEGINNER')
ON CONFLICT DO NOTHING;

-- ── CLASS CATEGORIES ──
INSERT INTO class_categories (class_id, category)
SELECT id, cat FROM fitness_classes
                        JOIN (VALUES
                                  ('Morning Yoga',   'YOGA'),
                                  ('HIIT Cardio',    'CARDIO'),
                                  ('Pilates Flow',   'PILATES'),
                                  ('Power Strength', 'STRENGTH'),
                                  ('Zumba Dance',    'DANCE'),
                                  ('Yoga Restore',   'YOGA'),
                                  ('Body Pump',      'STRENGTH'),
                                  ('Evening Cardio', 'CARDIO'),
                                  ('Stretching',     'PILATES')
) AS c(name, cat) ON fitness_classes.name = c.name;

-- ── CLASS SCHEDULES (next 7 days) ──
INSERT INTO class_schedules (class_id, trainer_id, scheduled_at, room)
SELECT
    fc.id,
    u.id,
    sched.scheduled_at,
    sched.room
FROM (VALUES
          ('Morning Yoga',    'elena@femfit.kz', NOW()::date + INTERVAL '1 day' + TIME '07:00', 'Studio A'),
          ('HIIT Cardio',     'sofia@femfit.kz', NOW()::date + INTERVAL '1 day' + TIME '08:30', 'Studio B'),
          ('Pilates Flow',    'elena@femfit.kz', NOW()::date + INTERVAL '1 day' + TIME '09:00', 'Studio A'),
          ('Power Strength',  'maria@femfit.kz', NOW()::date + INTERVAL '1 day' + TIME '10:30', 'Gym Hall'),
          ('Zumba Dance',     'sofia@femfit.kz', NOW()::date + INTERVAL '2 day' + TIME '12:00', 'Studio B'),
          ('Yoga Restore',    'elena@femfit.kz', NOW()::date + INTERVAL '2 day' + TIME '13:30', 'Studio A'),
          ('Body Pump',       'maria@femfit.kz', NOW()::date + INTERVAL '3 day' + TIME '17:00', 'Gym Hall'),
          ('Evening Cardio',  'sofia@femfit.kz', NOW()::date + INTERVAL '3 day' + TIME '18:30', 'Studio B'),
          ('Stretching',      'elena@femfit.kz', NOW()::date + INTERVAL '4 day' + TIME '19:30', 'Studio A')
     ) AS sched(class_name, trainer_email, scheduled_at, room)
         JOIN fitness_classes fc ON fc.name = sched.class_name
         JOIN users u ON u.email = sched.trainer_email;

-- ── TRAINING CYCLES ──
INSERT INTO training_cycles (title, description, duration_weeks, price)
VALUES
    ('Beginner Body Reset',    '4-week full-body program for beginners.',     4,  49900.00),
    ('Core & Flexibility',     '6-week pilates and yoga focused program.',    6,  79900.00),
    ('Strength Foundation',    '8-week progressive strength training.',       8,  99900.00),
    ('Total Body Transformation', '12-week comprehensive fitness program.',  12, 149900.00)
ON CONFLICT DO NOTHING;

-- ── SUBSCRIPTIONS (sample) ──
INSERT INTO subscriptions (user_id, plan_id, start_date, end_date, paid_amount)
SELECT
    u.id,
    mp.id,
    CURRENT_DATE,
    CURRENT_DATE + INTERVAL '1 month',
    mp.price_per_month
FROM users u, membership_plans mp
WHERE u.email = 'anna@mail.kz' AND mp.name = 'Premium'
ON CONFLICT DO NOTHING;