-- FemFit — Seed data (matches the live database snapshot taken 2026-06-20)
-- PostgreSQL, UTF-8
-- Run after schema.sql: psql -U postgres -d femfit -f data.sql
--
-- Notes:
--  • member ids 5-23 mirror the real, in-use development data (test client
--    accounts the developer logged in with, plus the 3 original seed trainers,
--    admin, and Anna Kim). Nothing here is fabricated — these rows, and every
--    order/booking/assignment/review below, are an exact copy of the live DB.
--  • 4 additional trainers (Aizhan, Dina, Karina, Saltanat) are appended at
--    the end so every training direction (Yoga, Cardio, Strength, Pilates,
--    Dance, Post-Natal, Beginner) has a dedicated trainer — see the
--    "ADDITIONAL TRAINERS" section below.
--  • photo_url / specialization columns are part of schema.sql (added in the
--    trainers/training_cycles tables) — seeded directly here rather than via
--    a separate patch, so a fresh `schema.sql` + `data.sql` run produces the
--    full, photo-complete site with no extra steps.
--  • Passwords: trainer accounts use trainer123, admin uses admin123,
--    client test accounts use client123 (all BCrypt-hashed, unchanged from
--    the live DB).


-- ══════════════════════════════════════
--  ROLES
-- ══════════════════════════════════════
INSERT INTO roles (id, name) VALUES (1, 'CLIENT'), (2, 'TRAINER'), (3, 'ADMIN')
ON CONFLICT (id) DO NOTHING;
SELECT setval('roles_id_seq', (SELECT MAX(id) FROM roles));


-- members
INSERT INTO members (id, first_name, last_name, email, phone, password_hash, birth_date, role_id, is_active, registration_date, discount_percent) VALUES
                                                                                                                                                      (5, 'Anna', 'Kim', 'anna@mail.kz', '+77045678901', '$2a$12$MKz8.vqPeLpM5Mb0IY6.3.yIsMh7M5dV9aL2aNxiW5nEoPQawG4.s', '1998-05-20', 1, true, '2026-06-03 17:15:07.996', 0),
                                                                                                                                                      (6, 'Aigerim', 'Bekova', 'aigerim@mail.kz', '+77056789012', '$2a$12$MKz8.vqPeLpM5Mb0IY6.3.yIsMh7M5dV9aL2aNxiW5nEoPQawG4.s', '2000-09-14', 1, true, '2026-06-03 17:15:07.996', 0),
                                                                                                                                                      (7, 'Zarina', 'Seitkali', 'zarina@mail.kz', '+77067890123', '$2a$12$MKz8.vqPeLpM5Mb0IY6.3.yIsMh7M5dV9aL2aNxiW5nEoPQawG4.s', '1996-12-03', 1, true, '2026-06-03 17:15:07.996', 0),
                                                                                                                                                      (8, 'Samal', 'Sam', 'samal@mail.ru', '+77780304512', '$2a$12$KexPiHKqUDnUUq8cPfXiRO2q1R9FW1P70Lgn9U7YUMBHwQ3pTPnMW', '1988-02-18', 1, true, '2026-06-05 11:16:59.756', 0),
                                                                                                                                                      (9, 'Sam', 'Sam', 'sam@mail.ru', '+77780304512', '$2a$12$eSBm140PSagO0D2t2qz5fOjUsG5v5X6o980kyCt.hYZhoMgi5mzKS', '1988-02-18', 1, true, '2026-06-05 11:21:01.627', 0),
                                                                                                                                                      (10, 'Sama', 'Sam', 'sam@mail.com', '+77780304512', '$2a$12$HSu7QCpe1Vvu7vVw.xXz8.LxwG/4QkBKKHDQ9lsaYpTJWnep05VMm', '1988-02-18', 1, true, '2026-06-05 11:41:15.917', 0),
                                                                                                                                                      (11, 'Samal', 'Sss', 'samal@mail.com', '+77780304512', '$2a$12$gjLwUYcPX8KJt3.Ijb6/VOEzOtShCGjqrCfI3SM2o0K6e7p/CgUMy', '2000-01-12', 1, true, '2026-06-05 12:37:13.929', 0),
                                                                                                                                                      (12, 'Sammy', 'Sammy', 'sammy@mail.ru', '+77780304512', '$2a$12$LizY.7y9rdL4pPwNDz6dBuHi30PFAYKoFwVbOkAUfzE27nH74oEcO', '1998-12-12', 1, true, '2026-06-05 12:58:41.066', 0),
                                                                                                                                                      (13, 'Assem', 'Abc', 'assem@mail.ru', '+77771234567', '$2a$12$BbA8bc23wWAGMIbxS62p.OwiadIINHp0GFcyo9Ml8hQ97K3FHPHUW', '2000-01-01', 1, true, '2026-06-05 14:21:09.305', 5),
                                                                                                                                                      (14, 'Samal', 'Bai', 'samal@gmail.com', '+77780304512', '$2a$12$OaBznkBPAJyGAQIcRWKSGuET.3864CJvr/SyIen4xznhqqQtw5b6m', '1998-12-03', 1, true, '2026-06-05 15:07:17.305', 10),
                                                                                                                                                      (15, 'Samal', 'Sam', 'samal@mail.kz', '+77780304512', '$2a$12$ox9ifVMHMuA6IIWsldpTUuy54P..9z5YYNCnoQ/XMS4uKI9rYGiba', '1988-01-01', 1, true, '2026-06-05 18:16:19.547', 0),
                                                                                                                                                      (16, 'Samal', 'Baiturina', 'samalb@mail.com', '87780304512', '$2a$12$F4DY59bHUGVJgqLFJ2AI9ep0.QXux1hPlRdp6iKf.jNhM7.g3xE0G', '1988-02-18', 1, true, '2026-06-05 19:06:15.547', 0),
                                                                                                                                                      (17, 'Dana', 'Dana', 'dana@mail.com', '87781234567', '$2a$12$IgNHtUKpHIeOjOKb/SUjF.8BYPsjsDCeJG45sG5y10qQInLti60Ka', NULL, 1, true, '2026-06-06 11:21:00.721', 0),
                                                                                                                                                      (18, 'Elena', 'Morozova', 'elena@femfit.kz', NULL, '$2a$12$bq3Kf2TLrBr/T80mCLscheAXtP4p/k/mUAA5BSCPr5fZUCW8CtLoO', NULL, 2, true, '2026-06-06 17:50:54.927', 0),
                                                                                                                                                      (19, 'Admin', 'FemFit', 'admin@femfit.kz', NULL, '$2a$12$HWfeHT0ZpiB7INFxvEPvLe6Gd5Io7yjzH4I5RPt4E0OP2iSJZraUS', NULL, 3, true, '2026-06-06 17:52:22.451', 0),
                                                                                                                                                      (20, 'Sofia', 'Romanova', 'sofia@femfit.kz', NULL, '$2a$12$bq3Kf2TLrBr/T80mCLscheAXtP4p/k/mUAA5BSCPr5fZUCW8CtLoO', NULL, 2, true, '2026-06-06 19:47:41.868', 0),
                                                                                                                                                      (21, 'Maria', 'Volkova', 'maria@femfit.kz', NULL, '$2a$12$EH4ItWvrBTSsx3GtaJmMD.66Bl6.kXwuoaeyFNVdZHP/extf22p3C', NULL, 2, true, '2026-06-06 19:48:06.114', 0),
                                                                                                                                                      (22, 'qwerty', 'qwerty', 'qwerty@mail.kz', NULL, '$2a$12$QLHLz6hs.dd6YkKMQ9YBXOuKHVcicGdE6Sf3V03hFDJCmhNa/5TRe', NULL, 1, true, '2026-06-13 12:23:48.942', 0),
                                                                                                                                                      (23, 'Әйгерім', 'Ааа', 'aika@mail.kz', NULL, '$2a$12$TDQD2UTzBzjx0UnvTNi15eC7nF4rYLF9D9.xKFbGDrNOi8d0WVhDm', NULL, 1, true, '2026-06-13 12:41:29.320', 0)
ON CONFLICT (id) DO NOTHING;

-- trainers (base row; photo_url/specialization filled by migration_v2)
INSERT INTO trainers (id, bio, experience_years, certification) VALUES
                                                                    (18, 'Certified yoga and pilates instructor.', 8, 'RYT-500'),
                                                                    (20, 'HIIT and cardio specialist. Former athletics coach.', 6, 'ACE-CPT'),
                                                                    (21, 'Strength training and nutrition expert.', 5, 'NSCA-CSCS')
ON CONFLICT (id) DO NOTHING;

-- training_cycles
INSERT INTO training_cycles (id, title, description, duration_weeks, price, is_active, created_at) VALUES
                                                                                                       (1, 'Beginner Body Reset', '4-week full-body program for beginners.', 4, 49900.00, true, '2026-06-03'),
                                                                                                       (2, 'Core & Flexibility', '6-week pilates and yoga focused program.', 6, 79900.00, true, '2026-06-03'),
                                                                                                       (3, 'Strength Foundation', '8-week progressive strength training.', 8, 99900.00, true, '2026-06-03'),
                                                                                                       (4, 'Total Body Transformation', '12-week comprehensive fitness program.', 12, 149900.00, true, '2026-06-03'),
                                                                                                       (5, 'Beginner Fitness', 'Perfect starter program for women new to fitness. Focuses on building strength and endurance through light cardio and bodyweight exercises.', 4, 29900.00, true, '2026-06-06'),
                                                                                                       (6, 'Weight Loss Intensive', 'High-intensity program combining cardio and strength training designed to maximize calorie burn and accelerate fat loss.', 8, 49900.00, true, '2026-06-06'),
                                                                                                       (7, 'Yoga & Flexibility', 'A holistic program combining yoga flows, stretching routines, and mindfulness practices for body and mind balance.', 6, 39900.00, true, '2026-06-06'),
                                                                                                       (8, 'Strength & Tone', 'Progressive resistance training program focusing on building lean muscle and toning the body with free weights and machines.', 10, 59900.00, true, '2026-06-06'),
                                                                                                       (9, 'Post-Natal Recovery', 'Gentle recovery program designed specifically for new mothers, focusing on core rehabilitation and gradual strength rebuilding.', 6, 44900.00, true, '2026-06-06')
ON CONFLICT (id) DO NOTHING;

-- fitness_classes
INSERT INTO fitness_classes (id, name, description, capacity, duration_minutes, difficulty_level, is_active, category) VALUES
                                                                                                                           (1, 'Morning Yoga', 'Gentle flow to start your day with intention.', 15, 60, 'BEGINNER', true, 'YOGA'),
                                                                                                                           (2, 'HIIT Cardio', 'High-intensity interval training for maximum burn.', 20, 45, 'ADVANCED', true, 'CARDIO'),
                                                                                                                           (3, 'Pilates Flow', 'Core strengthening and flexibility.', 12, 60, 'INTERMEDIATE', true, 'PILATES'),
                                                                                                                           (4, 'Power Strength', 'Full-body resistance training with weights.', 15, 60, 'INTERMEDIATE', true, 'STRENGTH'),
                                                                                                                           (5, 'Zumba Dance', 'Fun Latin-inspired cardio dance workout.', 25, 60, 'BEGINNER', true, 'DANCE'),
                                                                                                                           (6, 'Yoga Restore', 'Deep stretching and relaxation.', 12, 75, 'BEGINNER', true, 'YOGA'),
                                                                                                                           (7, 'Body Pump', 'Barbell workout for all major muscle groups.', 20, 60, 'INTERMEDIATE', true, 'STRENGTH'),
                                                                                                                           (8, 'Evening Cardio', 'Energizing cardio session to end the day.', 20, 45, 'BEGINNER', true, 'CARDIO'),
                                                                                                                           (9, 'Stretching', 'Full-body flexibility and mobility work.', 15, 45, 'BEGINNER', true, 'PILATES')
ON CONFLICT (id) DO NOTHING;

-- class_schedules: recurring weekly templates (no specific dates — see
-- migration_v4_recurring_schedule.sql for why). Each class meets 2-3 times
-- a week; concrete bookable sessions are generated into class_occurrences
-- by ScheduleOccurrenceInitializer on every application startup.
INSERT INTO class_schedules (class_id, trainer_id, day_of_week, start_time, room, is_active)
SELECT t.class_id, m.id, t.day_of_week, t.start_time::time, t.room, TRUE
FROM (VALUES
          (1, 'elena@femfit.kz', 1, '07:00', 'Studio A'),
          (1, 'elena@femfit.kz', 3, '07:00', 'Studio A'),
          (1, 'elena@femfit.kz', 5, '07:00', 'Studio A'),
          (2, 'sofia@femfit.kz', 2, '08:30', 'Studio B'),
          (2, 'sofia@femfit.kz', 4, '08:30', 'Studio B'),
          (3, 'aizhan@femfit.kz', 1, '09:00', 'Studio A'),
          (3, 'aizhan@femfit.kz', 3, '09:00', 'Studio A'),
          (4, 'maria@femfit.kz', 2, '10:00', 'Gym Hall'),
          (4, 'maria@femfit.kz', 4, '10:00', 'Gym Hall'),
          (4, 'maria@femfit.kz', 6, '10:00', 'Gym Hall'),
          (5, 'dina@femfit.kz', 3, '12:00', 'Studio B'),
          (5, 'dina@femfit.kz', 5, '12:00', 'Studio B'),
          (6, 'elena@femfit.kz', 2, '08:00', 'Studio A'),
          (6, 'elena@femfit.kz', 6, '08:00', 'Studio A'),
          (7, 'maria@femfit.kz', 1, '17:00', 'Gym Hall'),
          (7, 'maria@femfit.kz', 4, '17:00', 'Gym Hall'),
          (8, 'sofia@femfit.kz', 1, '18:30', 'Studio B'),
          (8, 'sofia@femfit.kz', 3, '18:30', 'Studio B'),
          (8, 'sofia@femfit.kz', 5, '18:30', 'Studio B'),
          (9, 'karina@femfit.kz', 2, '09:30', 'Studio A'),
          (9, 'karina@femfit.kz', 7, '09:30', 'Studio A')
     ) AS t(class_id, trainer_email, day_of_week, start_time, room)
         JOIN members m ON m.email = t.trainer_email;

-- class_occurrences: generate the next 8 weeks of bookable dated sessions
-- from the templates above. ScheduleOccurrenceInitializer re-runs this same
-- logic on every app startup (idempotent via the UNIQUE constraint), so this
-- seed is really just "day one" coverage.
INSERT INTO class_occurrences (schedule_id, occurrence_date)
SELECT cs.id, d::date
FROM class_schedules cs
         CROSS JOIN generate_series(CURRENT_DATE, CURRENT_DATE + INTERVAL '56 days', INTERVAL '1 day') AS d
WHERE cs.is_active = TRUE
  AND EXTRACT(ISODOW FROM d)::int = cs.day_of_week
ON CONFLICT (schedule_id, occurrence_date) DO NOTHING;

-- orders
INSERT INTO orders (id, member_id, cycle_id, trainer_id, status, paid_amount, created_at, completed_at) VALUES
                                                                                                            (1, 17, 7, NULL, 'CANCELLED', 39900.00, '2026-06-06', NULL),
                                                                                                            (2, 17, 9, NULL, 'CANCELLED', 44900.00, '2026-06-06', NULL),
                                                                                                            (3, 17, 9, NULL, 'CANCELLED', 44900.00, '2026-06-06', NULL),
                                                                                                            (4, 5, 1, 18, 'CANCELLED', 0.00, '2026-06-06', NULL),
                                                                                                            (5, 17, 5, 18, 'COMPLETED', 29900.00, '2026-06-06', '2026-06-06 19:38:23.052329'),
                                                                                                            (6, 17, 8, 20, 'CANCELLED', 59900.00, '2026-06-06', NULL),
                                                                                                            (7, 17, 1, 21, 'COMPLETED', 49900.00, '2026-06-17', '2026-06-18 19:24:43.428952'),
                                                                                                            (8, 17, 3, 21, 'COMPLETED', 99900.00, '2026-06-17', '2026-06-18 19:17:08.033638'),
                                                                                                            (9, 17, 2, 18, 'CANCELLED', 79900.00, '2026-06-17', NULL),
                                                                                                            (10, 23, 7, 18, 'ACTIVE', 39900.00, '2026-06-18', NULL),
                                                                                                            (11, 23, 8, 21, 'ACTIVE', 59900.00, '2026-06-18', NULL),
                                                                                                            (12, 23, 6, 20, 'ACTIVE', 49900.00, '2026-06-18', NULL),
                                                                                                            (13, 23, 9, 18, 'ACTIVE', 44900.00, '2026-06-18', NULL),
                                                                                                            (14, 23, 5, 20, 'COMPLETED', 29900.00, '2026-06-18', '2026-06-18 22:57:28.581981'),
                                                                                                            (15, 17, 6, 21, 'ACTIVE', 49900.00, '2026-06-18', NULL),
                                                                                                            (16, 17, 4, 21, 'ACTIVE', 149900.00, '2026-06-18', NULL),
                                                                                                            (17, 13, 9, 18, 'ACTIVE', 44900.00, '2026-06-18', NULL),
                                                                                                            (18, 13, 7, 21, 'ACTIVE', 39900.00, '2026-06-18', NULL),
                                                                                                            (19, 13, 5, 20, 'COMPLETED', 29900.00, '2026-06-18', '2026-06-18 22:57:22.092447'),
                                                                                                            (20, 12, 2, 18, 'ACTIVE', 79900.00, '2026-06-18', NULL),
                                                                                                            (21, 12, 8, 21, 'ACTIVE', 59900.00, '2026-06-18', NULL),
                                                                                                            (22, 12, 4, 20, 'COMPLETED', 149900.00, '2026-06-18', '2026-06-18 22:57:17.720874')
ON CONFLICT (id) DO NOTHING;

-- bookings
-- NOTE: the 2 original test bookings (member 17, old schedule_id 14/33) are
-- intentionally omitted here. They pointed at one-off dated class_schedules
-- rows that no longer exist now that the schedule is a recurring weekly
-- template (see migration_v4_recurring_schedule.sql). bookings.schedule_id
-- now references class_occurrences, which are generated fresh at startup —
-- there is no fixed occurrence id to seed a historical booking against on a
-- brand-new database. Live data from migration_v4 is unaffected.

-- assignments
INSERT INTO assignments (id, order_id, exercises, equipment, nutrition_plan, schedule_info, status, created_at, updated_at) VALUES
                                                                                                                                (2, 6, 'qwqe', 'ewe', 'qeqwe', 'qeqwe', 'ACTIVE', '2026-06-06 20:48:22.243382', '2026-06-06 20:48:22.243382'),
                                                                                                                                (3, 9, 'Warm-up: 5 min dynamic stretching
Goblet Squats: 3 x 12
Glute Bridges: 3 x 15
Plank: 3 x 45 sec
Bird-Dog: 3 x 10 per side

', 'Yoga mat
Resistance bands
2kg dumbbells
Water bottle', 'Pre-workout: 1 banana
Post-workout: Protein shake or Greek yogurt
Hydration: 2L water/day
Lunch: Lean chicken & broccoli', 'Monday: Full Body Strength
Tuesday: Active Recovery (Yoga)
Wednesday: Core & Stability
Thursday: Rest Day
Friday: Full Body Strength', 'ACTIVE', '2026-06-17 21:21:09.17287', '2026-06-18 22:50:23.241356'),
                                                                                                                                (4, 20, 'Warm-up: 5 mins of Cat-Cow stretch & Child’s Pose to mobilize the spine.
Core Engagement:
Dead Bug: 3 sets × 12 reps (focus on lower back pressed to the mat).
Forearm Plank: 3 sets × 45 seconds (maintain a straight line from head to heels).
Bird-Dog: 3 sets × 10 reps per side (controlled, slow extensions).
Flexibility Finish:
Seated Forward Fold & Cobra Stretch: Hold each for 30 seconds (deep, calm breathing).', 'Comfortable Fitness Mat
Yoga Block (optional, for deeper/supported stretching)
Resistance Band (light, for optional core activation)', 'Pre-workout (45-60 mins before): Light carbohydrate snack for steady energy (e.g., a banana or a small toast with almond butter).
Post-workout hydration: Drink at least 500ml of pure water within an hour after the session.
Daily focus: Ensure adequate protein intake to support core muscle recovery, and include magnesium-rich foods (spinach, nuts) to assist with muscle flexibility and relaxation.', 'Frequency: 3 times a week (e.g., Monday / Wednesday / Friday).
Best Time: Morning or early evening, ideally before heavy meals.
Duration: 30–40 minutes per session.', 'ACTIVE', '2026-06-18 22:42:18.16052', '2026-06-18 22:42:18.16052'),
                                                                                                                                (5, 17, '1. EXERCISE PLAN
Warm-up: 5 mins of Cat-Cow stretch & Child’s Pose to mobilize the spine.
Core Engagement:
Dead Bug: 3 sets × 12 reps (focus on lower back pressed to the mat).
Forearm Plank: 3 sets × 45 seconds (maintain a straight line from head to heels).
Bird-Dog: 3 sets × 10 reps per side (controlled, slow extensions).
Flexibility Finish:
Seated Forward Fold & Cobra Stretch: Hold each for 30 seconds (deep, calm breathing).', 'Comfortable Fitness Mat
Yoga Block (optional, for deeper/supported stretching)
Resistance Band (light, for optional core activation)', 'Pre-workout (45-60 mins before): Light carbohydrate snack for steady energy (e.g., a banana or a small toast with almond butter).
Post-workout hydration: Drink at least 500ml of pure water within an hour after the session.
Daily focus: Ensure adequate protein intake to support core muscle recovery, and include magnesium-rich foods (spinach, nuts) to assist with muscle flexibility and relaxation.', 'Frequency: 3 times a week (e.g., Monday / Wednesday / Friday).
Best Time: Morning or early evening, ideally before heavy meals.
Duration: 30–40 minutes per session.', 'ACTIVE', '2026-06-18 22:43:37.874289', '2026-06-18 22:43:37.874289'),
                                                                                                                                (6, 13, 'Warm-up & Breathing:
Diaphragmatic Breathing: 2-3 mins (focus on ribcage expansion and gentle pelvic floor activation on exhale).
Cat-Cow Stretch: 10 slow reps (mobilizing the spine without over-stretching the abdomen).
Safe Core & Pelvic Floor Re-education:
Pelvic Tilts: 3 sets × 12 reps (gentle activation of lower abs, completely safe for diastasis recti).
Glute Bridges (Bodyweight): 3 sets × 10 reps (focus on squeezing glutes, keeping hips level).
Supported Bird-Dog: 2 sets × 8 reps per side (keeping knees and hands firmly grounded, minimal extension).
Postural Flexibility:
Chest Opener Stretch: Hold for 30-45 seconds (restoring posture after holding/feeding the baby).', 'Soft Fitness Mat
Small Pilates Ball (or a firm pillow between knees for adductor/pelvic floor feedback)
Sturdy Chair (for supported movements or sitting alignment)', 'Hydration: Vital for recovery and lactation. Aim for 2.5–3 liters of water daily, keeping a glass nearby during workouts.
Nutrient Density: Prioritize collagen-rich foods (bone broths), lean proteins (chicken, fish), and healthy fats (avocado, eggs) to support tissue healing and hormonal balance.
Micronutrients: Ensure adequate iron, calcium, and vitamin D intake. Avoid restrictive low-calorie diets; focus on whole, energy-sustaining foods.', 'Frequency: 2–3 times a week, listening closely to the body''s signals.
Best Time: During the baby''s first morning nap, when energy levels are typically highest.
Duration: Short, high-quality sessions of 20–30 minutes (consistency over length).', 'ACTIVE', '2026-06-18 22:46:44.012368', '2026-06-18 22:46:44.012368'),
                                                                                                                                (7, 16, 'Warm-up: 5 min dynamic stretching
Goblet Squats: 3 x 12
Glute Bridges: 3 x 15
Plank: 3 x 45 sec
Bird-Dog: 3 x 10 per side

', 'Yoga mat
Resistance bands
2kg dumbbells
Water bottle', 'Pre-workout: 1 banana
Post-workout: Protein shake or Greek yogurt
Hydration: 2L water/day
Lunch: Lean chicken & broccoli', 'Monday: Full Body Strength
Tuesday: Active Recovery (Yoga)
Wednesday: Core & Stability
Thursday: Rest Day
Friday: Full Body Strength', 'ACTIVE', '2026-06-18 22:50:29.935469', '2026-06-18 22:50:29.935469'),
                                                                                                                                (8, 12, 'Warm-up: 5-7 mins of dynamic stretching (Arm circles, Leg swings, and light jumping jacks to elevate heart rate).
High-Intensity Metabolic Circuit:
Complete 3 rounds with 45 seconds of work and 15 seconds of rest per exercise:
Goblet Squats: Keep core tight, explode up on the exhale.
Dumbbell Thrusters: Full body movement combining a squat and an overhead press.
Mountain Climbers: Maintain a strong plank position, drive knees to chest rapidly.
Kettlebell Swings: Focus on the hip hinge and glute power.
Cardio & Burn:
10 mins of intervals on the treadmill or rowing machine (30s sprint / 30s walk).
Cooldown: 5 mins of full-body static stretching (Hamstrings, hip flexors, and lower back).', 'Medium-weight Dumbbells (suited to your fitness level)
Kettlebell (for dynamic swings)
Fitness Mat & Towel
Water Bottle (essential for staying hydrated during high-sweat sessions)', 'Caloric Deficit: Stick to your personalized daily caloric target, focusing on a moderate 300–500 kcal deficit.
Macronutrient Balance: Prioritize high protein (lean chicken, fish, tofu) to protect muscle mass, combined with complex carbohydrates (quinoa, oats) for sustained training energy.
Hydration & Recovery: Drink at least 2.5 liters of water daily. Eliminate liquid calories (juices, sugary coffee drinks).
Post-Workout: Consume a lean protein snack or shake within 45 minutes after the circuit to kickstart muscle recovery.', 'Frequency: 4 times a week (e.g., Tuesday, Wednesday, Friday, Saturday), allowing adequate rest days for muscle recovery.
Best Time: Late morning or early evening. Avoid intense circuits within 2 hours of going to sleep.
Duration: 45–55 minutes per session (including warm-up and cooldown).', 'ACTIVE', '2026-06-19 17:47:04.819299', '2026-06-19 17:47:04.819299'),
                                                                                                                                (9, 21, 'Warm-up: 5 mins of dynamic mobility (Shoulder rolls, bodyweight squats, and inchworms to prime the joints).
Strength & Sculpt Circuit:
Complete 3-4 sets of each exercise with a focus on slow, controlled execution (tempo 2-0-2):
Romanian Deadlifts (Dumbbells): 4 sets × 10 reps (focus on hamstrings and glutes activation).
Push-Ups (or Incline Modification): 3 sets × 12 reps (chest, shoulders, and core stability).
Reverse Lunges with Dumbbells: 3 sets × 10 reps per leg (quads and glutes focus).
Dumbbell Bent-Over Rows: 4 sets × 12 reps (sculpting the upper back and improving posture).
Core Finisher:
Plank Shoulder Taps: 3 sets × 30 seconds (maintaining perfectly still hips).
Cooldown: 5 mins of static stretching focusing on major muscle groups (Quads, glutes, chest, and lats).', 'Set of Medium to Heavy Dumbbells (appropriate for progressive overload)
Long Resistance Band (for back exercises or warm-up activation)
High-quality Fitness Mat', 'Protein Priority: Consume 1.6–2.0g of protein per kg of body weight daily (chicken breast, turkey, eggs, Greek yogurt) to support lean muscle building and toning.
Smart Recovery: Fuel your body with a balanced meal containing protein and complex carbohydrates (e.g., sweet potato or brown rice) within 1-2 hours post-workout.
Micronutrients for Muscles: Keep magnesium and potassium levels steady through leafy greens and seeds to prevent cramping and optimize performance.
Hydration: Drink 2.5 liters of clean water daily to assist metabolic processes and muscle repair.', 'Frequency: 3–4 times a week (alternating training days with full rest days, e.g., Mon/Wed/Fri).
Best Time: Early evening or whenever your strength levels feel most peaked.
Duration: 50–60 minutes per session (including a proper warm-up and deep cooldown).', 'ACTIVE', '2026-06-19 17:48:23.511239', '2026-06-19 17:48:23.511239'),
                                                                                                                                (10, 18, 'Centering & Breathwork:
Pranayama (Deep Belly Breathing): 3 mins (focus on lengthening inhalations and exhalations to calm the nervous system).
Vinyasa Flow & Spinal Mobility:
Sun Salutation A (Surya Namaskar): 3 fluid rounds (connecting movement with breath to warm up the entire body).
Warrior II to Reverse Warrior: Hold for 5 breaths per side (building lower body strength while opening the hips and side body).
Downward-Facing Dog: Hold for 45 seconds (focusing on a long spine and pushing heels toward the mat).
Deep Flexibility & Release:
Pigeon Pose (or Figure-4 modification): Hold for 1–2 minutes per side (deep release for tight hips and glutes).
Seated Forward Fold (Paschimottanasana): Hold for 1 minute (stretching the entire posterior chain and hamstrings).
Relaxation:
Savasana (Corpse Pose): 5 mins of total stillness to integrate the practice.', 'Non-slip Yoga Mat
Two Yoga Blocks (highly recommended to bring the floor closer and maintain proper alignment)
Yoga Strap (or a long towel to assist with deep hamstring stretches)
Comfortable, stretchable clothing', 'Pre-Practice Window: Avoid heavy meals at least 2 hours before yoga to ensure comfort during twists and inversions. If needed, have a light piece of fruit 45 minutes prior.
Mindful Hydration: Sip water before and after the session. Avoid drinking large amounts during practice to maintain internal heat.
Anti-inflammatory Focus: Incorporate herbal teas (like ginger or chamomile) and foods rich in antioxidants (berries, green leafy vegetables) to support joint health and muscle recovery.', 'Frequency: 3–5 times a week (can be practiced daily as a recovery session).
Best Time: Excellent for early morning to awaken the body, or late evening to relieve accumulated stress and prepare for deep sleep.
Duration: 45–60 minutes per session.', 'ACTIVE', '2026-06-19 17:49:30.8876', '2026-06-19 17:49:30.8876'),
                                                                                                                                (11, 11, 'Warm-up: 5 mins of upper body and hip mobility (Arm swings, torso twists, and bodyweight cosmic squats).
Sculpt & Define Circuit:
Complete 3 sets of 12 reps for each exercise, keeping a steady, controlled pace:
Goblet Squats with Pulse: 3 sets × 12 reps (adding a small pulse at the bottom to increase time under tension).
Dumbbell Shoulder Press: 3 sets × 12 reps (focus on strong posture, extending arms fully without shrugging).
Bicep Curl to Overhead Extension: 3 sets × 10 reps (combining two movements to tone arms and shoulders).
Glute Bridge Marching: 3 sets × 12 reps per leg (unilateral stability for glutes and lower back).
Core Power Finish:
Bicycle Crunches: 3 sets × 40 seconds (slow, intentional elbow-to-knee twists).
Cooldown: 5 mins of chest-opening and hamstring stretches (using deep, relaxing breaths).', 'Pair of Light to Medium Dumbbells (e.g., 2kg to 5kg depending on fitness level)
Loop Resistance Band (medium tension, for activation)
Standard Fitness Mat', 'Lean Tone Support: Aim for a high-protein breakfast or post-workout meal to kickstart muscle definition. Include clean options like egg whites, avocado, and spinach.
Complex Fuels: Swap simple carbs for complex ones (sweet potatoes, oats, quinoa) to maintain stable glycogen levels during strength intervals.
Hydration Rule: Keep a water bottle nearby and drink at least 500ml during the workout, totaling 2.5 liters throughout the day.
Recovery Window: Consume a small, easily digestible protein/carb snack (like a rice cake with turkey or protein shake) within 30 minutes after training.', 'Frequency: 3 times a week with mandatory 48-hour rest windows between identical muscle groups (e.g., Tuesday, Thursday, Saturday).
Best Time: Late afternoon or early evening when muscle temperature and strength naturally peak.
Duration: 45–50 minutes per session.', 'ACTIVE', '2026-06-19 17:50:40.175445', '2026-06-19 17:50:40.175445')
ON CONFLICT (id) DO NOTHING;

-- reviews
INSERT INTO reviews (id, order_id, member_id, trainer_id, rating, comment, created_at) VALUES
                                                                                           (1, 5, 17, 18, 5, 'This class was amazing!', '2026-06-17'),
                                                                                           (2, 19, 13, 20, 5, 'The perfect balance of challenge and comfort for beginners. No stress, highly professional trainers, and a beautiful community. It completely changed my perception of working out!', '2026-06-19'),
                                                                                           (3, 22, 12, 20, 5, 'Incredible energy, intense but rewarding workouts, and a massive boost in my strength and stamina. This program pushed me out of my comfort zone and gave me the best shape of my life!', '2026-06-19')
ON CONFLICT (id) DO NOTHING;

-- Re-sync sequences so future application INSERTs continue after the highest seeded id
SELECT setval('users_id_seq', (SELECT MAX(id) FROM members));
SELECT setval('training_cycles_id_seq', (SELECT MAX(id) FROM training_cycles));
SELECT setval('fitness_classes_id_seq', (SELECT MAX(id) FROM fitness_classes));
SELECT setval('class_schedules_id_seq', (SELECT MAX(id) FROM class_schedules));
SELECT setval('class_occurrences_id_seq', COALESCE((SELECT MAX(id) FROM class_occurrences), 1));
SELECT setval('orders_id_seq', (SELECT MAX(id) FROM orders));
SELECT setval('bookings_id_seq', COALESCE((SELECT MAX(id) FROM bookings), 1));
SELECT setval('assignments_id_seq', (SELECT MAX(id) FROM assignments));
SELECT setval('reviews_id_seq', (SELECT MAX(id) FROM reviews));

-- ══════════════════════════════════════
--  PHOTOS + SPECIALIZATION for the 3 original trainers
-- ══════════════════════════════════════
UPDATE trainers SET photo_url = '/static/img/trainers/trainer-01-yoga.jpg',
                    specialization = 'Yoga & Pilates'
WHERE id = 18;

UPDATE trainers SET photo_url = '/static/img/trainers/trainer-02-cardio.jpg',
                    specialization = 'HIIT & Cardio'
WHERE id = 20;

UPDATE trainers SET photo_url = '/static/img/trainers/trainer-03-strength.jpg',
                    specialization = 'Strength & Nutrition'
WHERE id = 21;


-- ══════════════════════════════════════
--  ADDITIONAL TRAINERS (Pilates, Dance, Post-Natal, Beginner/Weight-Loss)
--  password for all = trainer123 (same BCrypt hash as the original 3 trainers)
-- ══════════════════════════════════════
INSERT INTO members (first_name, last_name, email, phone, password_hash, birth_date, role_id)
VALUES
    ('Aizhan',   'Tulegenova', 'aizhan@femfit.kz',   '+77078901234',
     '$2a$12$bq3Kf2TLrBr/T80mCLscheAXtP4p/k/mUAA5BSCPr5fZUCW8CtLoO', '1993-04-11', 2),
    ('Dina',     'Ospanova',   'dina@femfit.kz',     '+77089012345',
     '$2a$12$bq3Kf2TLrBr/T80mCLscheAXtP4p/k/mUAA5BSCPr5fZUCW8CtLoO', '1990-09-27', 2),
    ('Karina',   'Yermekova',  'karina@femfit.kz',   '+77090123456',
     '$2a$12$bq3Kf2TLrBr/T80mCLscheAXtP4p/k/mUAA5BSCPr5fZUCW8CtLoO', '1987-02-19', 2),
    ('Saltanat', 'Iskakova',   'saltanat@femfit.kz', '+77001239876',
     '$2a$12$bq3Kf2TLrBr/T80mCLscheAXtP4p/k/mUAA5BSCPr5fZUCW8CtLoO', '1996-06-30', 2)
ON CONFLICT (email) DO NOTHING;

INSERT INTO trainers (id, bio, experience_years, certification, photo_url, specialization)
SELECT id, 'Pilates and barre specialist focused on core control and posture.', 7, 'PMA-CPT',
       '/static/img/trainers/trainer-04-pilates.jpg', 'Pilates & Barre'
FROM members WHERE email = 'aizhan@femfit.kz' ON CONFLICT DO NOTHING;

INSERT INTO trainers (id, bio, experience_years, certification, photo_url, specialization)
SELECT id, 'Dance and cardio choreographer. Former professional dancer.', 9, 'ZIN-INSTRUCTOR',
       '/static/img/trainers/trainer-05-dance.jpg', 'Dance & Cardio'
FROM members WHERE email = 'dina@femfit.kz' ON CONFLICT DO NOTHING;

INSERT INTO trainers (id, bio, experience_years, certification, photo_url, specialization)
SELECT id, 'Pre- and post-natal fitness specialist, gentle recovery programmes.', 11, 'PNFC-CERT',
       '/static/img/trainers/trainer-06-postnatal.jpg', 'Post-Natal Recovery'
FROM members WHERE email = 'karina@femfit.kz' ON CONFLICT DO NOTHING;

INSERT INTO trainers (id, bio, experience_years, certification, photo_url, specialization)
SELECT id, 'Beginner-friendly coaching and sustainable weight-loss programmes.', 4, 'ACE-CPT',
       '/static/img/trainers/trainer-07-beginner.jpg', 'Beginner Coaching & Weight Loss'
FROM members WHERE email = 'saltanat@femfit.kz' ON CONFLICT DO NOTHING;


-- ══════════════════════════════════════
--  PHOTOS for the 9 training cycles
-- ══════════════════════════════════════
UPDATE training_cycles SET photo_url = '/static/img/programs/cycle-01-beginner-body-reset.jpg' WHERE id = 1;
UPDATE training_cycles SET photo_url = '/static/img/programs/cycle-02-core-flexibility.jpg'    WHERE id = 2;
UPDATE training_cycles SET photo_url = '/static/img/programs/cycle-03-strength-foundation.jpg' WHERE id = 3;
UPDATE training_cycles SET photo_url = '/static/img/programs/cycle-04-total-body.jpg'          WHERE id = 4;
UPDATE training_cycles SET photo_url = '/static/img/programs/cycle-05-beginner-fitness.jpg'    WHERE id = 5;
UPDATE training_cycles SET photo_url = '/static/img/programs/cycle-06-weight-loss.jpg'         WHERE id = 6;
UPDATE training_cycles SET photo_url = '/static/img/programs/cycle-07-yoga-flexibility.jpg'    WHERE id = 7;
UPDATE training_cycles SET photo_url = '/static/img/programs/cycle-08-strength-tone.jpg'       WHERE id = 8;
UPDATE training_cycles SET photo_url = '/static/img/programs/cycle-09-post-natal.jpg'          WHERE id = 9;


-- ══════════════════════════════════════
--  Re-sync members sequence again (4 new trainer members were added above)
-- ══════════════════════════════════════
SELECT setval('users_id_seq', (SELECT MAX(id) FROM members));

-- ══════════════════════════════════════
--  RU/KZ TRANSLATIONS for training_cycles, fitness_classes, trainers
-- ══════════════════════════════════════
-- training_cycles translations
UPDATE training_cycles SET title_ru = 'Перезагрузка тела для начинающих', title_kz = 'Бастаушыларға арналған қалпына келтіру', description_ru = '4-недельная программа для всего тела для начинающих.', description_kz = 'Бастаушыларға арналған 4 апталық дене бойынша бағдарлама.' WHERE id = 1;
UPDATE training_cycles SET title_ru = 'Кор и гибкость', title_kz = 'Дене тірегі мен икемділік', description_ru = '6-недельная программа на основе пилатеса и йоги.', description_kz = 'Пилатес пен йогаға негізделген 6 апталық бағдарлама.' WHERE id = 2;
UPDATE training_cycles SET title_ru = 'Основы силы', title_kz = 'Күш негіздері', description_ru = '8-недельная программа прогрессивных силовых тренировок.', description_kz = '8 апталық үдемелі күш жаттығулары бағдарламасы.' WHERE id = 3;
UPDATE training_cycles SET title_ru = 'Полная трансформация тела', title_kz = 'Дененің толық өзгеруі', description_ru = '12-недельная комплексная фитнес-программа.', description_kz = '12 апталық кешенді фитнес бағдарламасы.' WHERE id = 4;
UPDATE training_cycles SET title_ru = 'Фитнес для начинающих', title_kz = 'Бастаушыларға арналған фитнес', description_ru = 'Идеальная стартовая программа для женщин, новых в фитнесе. Развивает силу и выносливость через лёгкое кардио и упражнения с собственным весом.', description_kz = 'Фитнеспен жаңадан айналысатын әйелдерге арналған тамаша бастапқы бағдарлама. Жеңіл кардио мен жеке салмақпен жасалатын жаттығулар арқылы күш пен төзімділікті дамытады.' WHERE id = 5;
UPDATE training_cycles SET title_ru = 'Интенсивное похудение', title_kz = 'Қарқынды салмақ жоғалту', description_ru = 'Высокоинтенсивная программа, сочетающая кардио и силовые тренировки для максимального сжигания калорий и ускоренного похудения.', description_kz = 'Калорияны барынша жағу және салмақты тез жоғалту үшін кардио мен күш жаттығуларын біріктіретін жоғары қарқынды бағдарлама.' WHERE id = 6;
UPDATE training_cycles SET title_ru = 'Йога и гибкость', title_kz = 'Йога және икемділік', description_ru = 'Целостная программа, сочетающая йога-флоу, растяжку и практики осознанности для баланса тела и разума.', description_kz = 'Дене мен ақыл-ой тепе-теңдігі үшін йога ағындарын, созылу жаттығуларын және зейінділік тәжірибелерін біріктіретін біртұтас бағдарлама.' WHERE id = 7;
UPDATE training_cycles SET title_ru = 'Сила и тонус', title_kz = 'Күш және тонус', description_ru = 'Прогрессивная программа силовых тренировок, направленная на наращивание мышечной массы и тонизирование тела с помощью свободных весов и тренажёров.', description_kz = 'Бос салмақтар мен тренажёрлар көмегімен бұлшықет массасын арттыруға және денені тонизациялауға бағытталған үдемелі күш жаттығулары бағдарламасы.' WHERE id = 8;
UPDATE training_cycles SET title_ru = 'Восстановление после родов', title_kz = 'Босанғаннан кейінгі қалпына келтіру', description_ru = 'Мягкая восстановительная программа, разработанная специально для молодых мам, направленная на реабилитацию кора и постепенное восстановление силы.', description_kz = 'Жас аналарға арнайы әзірленген, дене тірегін қалпына келтіруге және күшті бірте-бірте қалпына келтіруге бағытталған жұмсақ қалпына келтіру бағдарламасы.' WHERE id = 9;

-- fitness_classes translations
UPDATE fitness_classes SET name_ru = 'Утренняя йога', name_kz = 'Таңғы йога', description_ru = 'Мягкий флоу, чтобы осознанно начать день.', description_kz = 'Күнді саналы түрде бастауға арналған жұмсақ жаттығу ағыны.' WHERE id = 1;
UPDATE fitness_classes SET name_ru = 'HIIT кардио', name_kz = 'HIIT кардио', description_ru = 'Высокоинтенсивная интервальная тренировка для максимального сжигания калорий.', description_kz = 'Калорияны барынша жағуға арналған жоғары қарқынды интервалды жаттығу.' WHERE id = 2;
UPDATE fitness_classes SET name_ru = 'Пилатес-флоу', name_kz = 'Пилатес ағыны', description_ru = 'Укрепление кора и развитие гибкости.', description_kz = 'Дене тірегін нығайту және икемділікті дамыту.' WHERE id = 3;
UPDATE fitness_classes SET name_ru = 'Силовая тренировка', name_kz = 'Күш жаттығуы', description_ru = 'Силовая тренировка всего тела с отягощениями.', description_kz = 'Салмақпен жасалатын дененің барлық бөлігіне арналған күш жаттығуы.' WHERE id = 4;
UPDATE fitness_classes SET name_ru = 'Зумба-данс', name_kz = 'Зумба би', description_ru = 'Энергичная кардио-тренировка в стиле латинских танцев.', description_kz = 'Латын билері стиліндегі қарқынды кардио жаттығу.' WHERE id = 5;
UPDATE fitness_classes SET name_ru = 'Восстановительная йога', name_kz = 'Қалпына келтіруші йога', description_ru = 'Глубокая растяжка и расслабление.', description_kz = 'Терең созылу және демалу.' WHERE id = 6;
UPDATE fitness_classes SET name_ru = 'Боди-памп', name_kz = 'Боди-памп', description_ru = 'Тренировка со штангой на все основные группы мышц.', description_kz = 'Барлық негізгі бұлшықет топтарына арналған штангамен жаттығу.' WHERE id = 7;
UPDATE fitness_classes SET name_ru = 'Вечернее кардио', name_kz = 'Кешкі кардио', description_ru = 'Энергичная кардио-сессия для завершения дня.', description_kz = 'Күнді аяқтауға арналған қарқынды кардио сессиясы.' WHERE id = 8;
UPDATE fitness_classes SET name_ru = 'Стретчинг', name_kz = 'Созылу жаттығулары', description_ru = 'Растяжка и мобильность для всего тела.', description_kz = 'Дененің барлық бөлігіне арналған созылу және қозғалғыштық жаттығулары.' WHERE id = 9;

-- trainers translations
UPDATE trainers SET bio_ru = 'Сертифицированный инструктор по йоге и пилатесу с 8-летним опытом.', bio_kz = '8 жылдық тәжірибесі бар йога және пилатес бойынша сертификатталған нұсқаушы.', specialization_ru = 'Йога и пилатес', specialization_kz = 'Йога және пилатес' FROM members m WHERE trainers.id = m.id AND m.email = 'elena@femfit.kz';
UPDATE trainers SET bio_ru = 'Специалист по HIIT и кардио. Бывший тренер по лёгкой атлетике.', bio_kz = 'HIIT және кардио бойынша маман. Бұрынғы жеңіл атлетика бойынша жаттықтырушы.', specialization_ru = 'HIIT и кардио', specialization_kz = 'HIIT және кардио' FROM members m WHERE trainers.id = m.id AND m.email = 'sofia@femfit.kz';
UPDATE trainers SET bio_ru = 'Эксперт по силовым тренировкам и питанию.', bio_kz = 'Күш жаттығулары мен тамақтану бойынша сарапшы.', specialization_ru = 'Сила и питание', specialization_kz = 'Күш және тамақтану' FROM members m WHERE trainers.id = m.id AND m.email = 'maria@femfit.kz';
UPDATE trainers SET bio_ru = 'Специалист по пилатесу и барре, фокус на контроле кора и осанке.', bio_kz = 'Пилатес және барре бойынша маман, дене тірегін бақылау мен дұрыс осанкаға назар аударады.', specialization_ru = 'Пилатес и барре', specialization_kz = 'Пилатес және барре' FROM members m WHERE trainers.id = m.id AND m.email = 'aizhan@femfit.kz';
UPDATE trainers SET bio_ru = 'Хореограф по танцам и кардио. Бывшая профессиональная танцовщица.', bio_kz = 'Би және кардио бойынша хореограф. Бұрынғы кәсіби биші.', specialization_ru = 'Танцы и кардио', specialization_kz = 'Би және кардио' FROM members m WHERE trainers.id = m.id AND m.email = 'dina@femfit.kz';
UPDATE trainers SET bio_ru = 'Специалист по фитнесу до и после родов, программы мягкого восстановления.', bio_kz = 'Босануға дейінгі және босанғаннан кейінгі фитнес бойынша маман, жұмсақ қалпына келтіру бағдарламалары.', specialization_ru = 'Восстановление после родов', specialization_kz = 'Босанғаннан кейінгі қалпына келтіру' FROM members m WHERE trainers.id = m.id AND m.email = 'karina@femfit.kz';
UPDATE trainers SET bio_ru = 'Коучинг для начинающих и устойчивые программы похудения.', bio_kz = 'Бастаушыларға арналған коучинг және тұрақты салмақ жоғалту бағдарламалары.', specialization_ru = 'Коучинг для начинающих и похудение', specialization_kz = 'Бастаушыларға арналған коучинг және салмақ жоғалту' FROM members m WHERE trainers.id = m.id AND m.email = 'saltanat@femfit.kz';