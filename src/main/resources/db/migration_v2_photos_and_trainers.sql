-- FemFit — Migration v2: trainer/program photos + additional trainers
-- PostgreSQL, UTF-8
-- Run AFTER schema.sql/data.sql have already been applied once.
-- Safe to re-run: every statement is idempotent (IF NOT EXISTS / ON CONFLICT).
--
-- Run: psql -U postgres -d femfit -f migration_v2_photos_and_trainers.sql


-- ══════════════════════════════════════
--  1. NEW COLUMNS
-- ══════════════════════════════════════
ALTER TABLE trainers
    ADD COLUMN IF NOT EXISTS photo_url      VARCHAR(255),
    ADD COLUMN IF NOT EXISTS specialization VARCHAR(150);

ALTER TABLE training_cycles
    ADD COLUMN IF NOT EXISTS photo_url  VARCHAR(255),
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT NOW();


-- ══════════════════════════════════════
--  2. PHOTOS FOR EXISTING 3 TRAINERS
--     (Elena = Yoga/Pilates, Sofia = HIIT/Cardio, Maria = Strength)
-- ══════════════════════════════════════
UPDATE trainers SET photo_url = '/static/img/trainers/trainer-01-yoga.jpg',
                     specialization = 'Yoga & Pilates'
FROM members m WHERE trainers.id = m.id AND m.email = 'elena@femfit.kz';

UPDATE trainers SET photo_url = '/static/img/trainers/trainer-02-cardio.jpg',
                     specialization = 'HIIT & Cardio'
FROM members m WHERE trainers.id = m.id AND m.email = 'sofia@femfit.kz';

UPDATE trainers SET photo_url = '/static/img/trainers/trainer-03-strength.jpg',
                     specialization = 'Strength & Nutrition'
FROM members m WHERE trainers.id = m.id AND m.email = 'maria@femfit.kz';


-- ══════════════════════════════════════
--  3. FOUR NEW TRAINERS (Pilates, Dance, Post-Natal, Beginner/Weight-Loss)
--     password for all = trainer123 (same BCrypt hash as the existing seed trainers)
-- ══════════════════════════════════════
INSERT INTO members (first_name, last_name, email, phone, password_hash, birth_date, role_id)
VALUES
    ('Aizhan', 'Tulegenova', 'aizhan@femfit.kz', '+77078901234',
     '$2a$12$LJy7.upOdLnL4Lz9HX5.2.xHrG6L4cU8zK1zMwhV4mD8nQOZwF3.r', '1993-04-11',
     (SELECT id FROM roles WHERE name = 'TRAINER')),
    ('Dina',   'Ospanova',    'dina@femfit.kz',   '+77089012345',
     '$2a$12$LJy7.upOdLnL4Lz9HX5.2.xHrG6L4cU8zK1zMwhV4mD8nQOZwF3.r', '1990-09-27',
     (SELECT id FROM roles WHERE name = 'TRAINER')),
    ('Karina', 'Yermekova',   'karina@femfit.kz', '+77090123456',
     '$2a$12$LJy7.upOdLnL4Lz9HX5.2.xHrG6L4cU8zK1zMwhV4mD8nQOZwF3.r', '1987-02-19',
     (SELECT id FROM roles WHERE name = 'TRAINER')),
    ('Saltanat', 'Iskakova',  'saltanat@femfit.kz', '+77001239876',
     '$2a$12$LJy7.upOdLnL4Lz9HX5.2.xHrG6L4cU8zK1zMwhV4mD8nQOZwF3.r', '1996-06-30',
     (SELECT id FROM roles WHERE name = 'TRAINER'))
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
--  4. NOTE on specializations
--     Your schema.sql (source of truth) has no trainer_specializations table —
--     it only appeared in the old, stale data.sql and is not part of the
--     real schema. Specialization is instead a plain `specialization` column
--     on trainers (added in step 1 above) — one short label per trainer,
--     e.g. "Yoga & Pilates". Simpler, no extra join, and matches how the
--     about/trainers.html page displays it directly under each trainer's name.
-- ══════════════════════════════════════


-- ══════════════════════════════════════
--  5. PHOTOS FOR THE 9 TRAINING CYCLES
--     (matched to the cycle titles already in your database)
-- ══════════════════════════════════════
UPDATE training_cycles SET photo_url = '/static/img/programs/cycle-01-beginner-body-reset.jpg' WHERE title = 'Beginner Body Reset';
UPDATE training_cycles SET photo_url = '/static/img/programs/cycle-02-core-flexibility.jpg'    WHERE title = 'Core & Flexibility';
UPDATE training_cycles SET photo_url = '/static/img/programs/cycle-03-strength-foundation.jpg' WHERE title = 'Strength Foundation';
UPDATE training_cycles SET photo_url = '/static/img/programs/cycle-04-total-body.jpg'          WHERE title = 'Total Body Transformation';
UPDATE training_cycles SET photo_url = '/static/img/programs/cycle-05-beginner-fitness.jpg'    WHERE title = 'Beginner Fitness';
UPDATE training_cycles SET photo_url = '/static/img/programs/cycle-06-weight-loss.jpg'         WHERE title = 'Weight Loss Intensive';
UPDATE training_cycles SET photo_url = '/static/img/programs/cycle-07-yoga-flexibility.jpg'    WHERE title = 'Yoga & Flexibility';
UPDATE training_cycles SET photo_url = '/static/img/programs/cycle-08-strength-tone.jpg'       WHERE title = 'Strength & Tone';
UPDATE training_cycles SET photo_url = '/static/img/programs/cycle-09-post-natal.jpg'          WHERE title = 'Post-Natal Recovery';


-- ══════════════════════════════════════
--  Done. After running this script:
--    • trainers table has 7 rows, all with photo_url set
--    • training_cycles table still has its original 9 rows, all with photo_url set
--    • 4 new TRAINER-role members were added (password for all: trainer123)
-- ══════════════════════════════════════
