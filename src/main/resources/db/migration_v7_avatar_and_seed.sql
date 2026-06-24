-- ══════════════════════════════════════
-- Migration v7: avatar_url for members + rich seed data
-- Run once on existing database
-- ══════════════════════════════════════

-- 1. Add avatar_url column to members (safe, idempotent)
ALTER TABLE members
    ADD COLUMN IF NOT EXISTS avatar_url VARCHAR(255);

-- 2. New client accounts for demo (rich data)
-- Password: 12345678  (hash verified)
INSERT INTO members (first_name, last_name, email, phone, password_hash, birth_date, role_id, is_active, registration_date, discount_percent)
VALUES
    ('Aliya',    'Nurlanova',  'aliya@mail.kz',    '+77011112233', '$2a$12$MKz8.vqPeLpM5Mb0IY6.3.yIsMh7M5dV9aL2aNxiW5nEoPQawG4.s', '1995-03-15', 1, true, NOW(), 0),
    ('Madina',   'Bekova',     'madina@mail.kz',   '+77022223344', '$2a$12$MKz8.vqPeLpM5Mb0IY6.3.yIsMh7M5dV9aL2aNxiW5nEoPQawG4.s', '1999-07-22', 1, true, NOW(), 5),
    ('Dinara',   'Sultanova',  'dinara@mail.kz',   '+77033334455', '$2a$12$MKz8.vqPeLpM5Mb0IY6.3.yIsMh7M5dV9aL2aNxiW5nEoPQawG4.s', '1993-11-08', 1, true, NOW(), 10),
    ('Gulnara',  'Ospanova',   'gulnara@mail.kz',  '+77044445566', '$2a$12$MKz8.vqPeLpM5Mb0IY6.3.yIsMh7M5dV9aL2aNxiW5nEoPQawG4.s', '2001-01-30', 1, true, NOW(), 0),
    ('Kamila',   'Akhmetova',  'kamila@mail.kz',   '+77055556677', '$2a$12$MKz8.vqPeLpM5Mb0IY6.3.yIsMh7M5dV9aL2aNxiW5nEoPQawG4.s', '1997-06-14', 1, true, NOW(), 0),
    ('Tolkyn',   'Seitkali',   'tolkyn@mail.kz',   '+77066667788', '$2a$12$MKz8.vqPeLpM5Mb0IY6.3.yIsMh7M5dV9aL2aNxiW5nEoPQawG4.s', '1990-09-03', 1, true, NOW(), 15)
ON CONFLICT (email) DO NOTHING;

-- 3. Orders for new clients — assign to various trainers
INSERT INTO orders (member_id, cycle_id, trainer_id, status, paid_amount, created_at)
SELECT m.id, 5, t.id, 'ACTIVE', 29900.00, NOW() - INTERVAL '10 days'
FROM members m, members t
WHERE m.email = 'aliya@mail.kz' AND t.email = 'aizhan@femfit.kz'
ON CONFLICT DO NOTHING;

INSERT INTO orders (member_id, cycle_id, trainer_id, status, paid_amount, created_at)
SELECT m.id, 7, t.id, 'ACTIVE', 39900.00, NOW() - INTERVAL '8 days'
FROM members m, members t
WHERE m.email = 'madina@mail.kz' AND t.email = 'elena@femfit.kz'
ON CONFLICT DO NOTHING;

INSERT INTO orders (member_id, cycle_id, trainer_id, status, paid_amount, created_at)
SELECT m.id, 8, t.id, 'ACTIVE', 59900.00, NOW() - INTERVAL '14 days'
FROM members m, members t
WHERE m.email = 'dinara@mail.kz' AND t.email = 'maria@femfit.kz'
ON CONFLICT DO NOTHING;

INSERT INTO orders (member_id, cycle_id, trainer_id, status, paid_amount, created_at)
SELECT m.id, 6, t.id, 'ACTIVE', 49900.00, NOW() - INTERVAL '5 days'
FROM members m, members t
WHERE m.email = 'gulnara@mail.kz' AND t.email = 'sofia@femfit.kz'
ON CONFLICT DO NOTHING;

INSERT INTO orders (member_id, cycle_id, trainer_id, status, paid_amount, created_at)
SELECT m.id, 9, t.id, 'ACTIVE', 44900.00, NOW() - INTERVAL '7 days'
FROM members m, members t
WHERE m.email = 'kamila@mail.kz' AND t.email = 'karina@femfit.kz'
ON CONFLICT DO NOTHING;

INSERT INTO orders (member_id, cycle_id, trainer_id, status, paid_amount, created_at)
SELECT m.id, 3, t.id, 'ACTIVE', 99900.00, NOW() - INTERVAL '20 days'
FROM members m, members t
WHERE m.email = 'tolkyn@mail.kz' AND t.email = 'dina@femfit.kz'
ON CONFLICT DO NOTHING;

-- 4. Assignments for ALL active orders that don't have one yet
-- (заполняем существующие orders 10,11,12,13,15,16,17,18,20,21 + новые)

-- Order 10: Әйгерім - yoga (trainer elena)
INSERT INTO assignments (order_id, exercises, equipment, nutrition_plan, schedule_info, status, created_at, updated_at)
SELECT 10,
'Vinyasa Flow:\nSun Salutation A: 3 rounds\nWarrior I & II: 5 breaths per side\nDownward Dog: 45 sec hold\nChild''s Pose: 1 min rest\nTree Pose: 30 sec per leg',
'Non-slip yoga mat\nTwo yoga blocks\nYoga strap\nComfortable stretchy clothing',
'Pre-practice: light snack 1h before (banana or oat crackers)\nPost-practice: protein smoothie\nHydration: 2L water daily\nAnti-inflammatory foods: ginger tea, berries',
'3 times/week: Monday, Wednesday, Friday\nBest time: morning or evening\nDuration: 45-60 min per session',
'ACTIVE', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM assignments WHERE order_id = 10);

-- Order 11: Әйгерім - strength (trainer maria)
INSERT INTO assignments (order_id, exercises, equipment, nutrition_plan, schedule_info, status, created_at, updated_at)
SELECT 11,
'Strength & Sculpt Circuit (3 sets x 12 reps):\nRomanian Deadlifts: 4x10\nPush-Ups: 3x12\nReverse Lunges with Dumbbells: 3x10 per leg\nDumbbell Bent-Over Rows: 4x12\nCore Finisher: Plank Shoulder Taps 3x30 sec',
'Medium-Heavy Dumbbells\nLong Resistance Band\nHigh-quality Fitness Mat',
'Protein: 1.6-2.0g per kg bodyweight\nComplex carbs post-workout: sweet potato or brown rice\nMagnesium-rich foods: leafy greens, seeds\nHydration: 2.5L water daily',
'3-4 times/week: Mon/Wed/Fri or Tue/Thu/Sat\nBest time: early evening\nDuration: 50-60 min per session',
'ACTIVE', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM assignments WHERE order_id = 11);

-- Order 12: Әйгерім - weight loss (trainer sofia)
INSERT INTO assignments (order_id, exercises, equipment, nutrition_plan, schedule_info, status, created_at, updated_at)
SELECT 12,
'HIIT Metabolic Circuit (3 rounds, 45 sec work / 15 sec rest):\nGoblet Squats\nDumbbell Thrusters\nMountain Climbers\nKettlebell Swings\nCardio: 10 min treadmill intervals (30s sprint / 30s walk)',
'Medium-weight Dumbbells\nKettlebell\nFitness Mat\nWater Bottle',
'Caloric deficit: 300-500 kcal\nHigh protein: chicken, fish, tofu\nComplex carbs: quinoa, oats\nNo liquid calories\nPost-workout protein snack within 45 min',
'4 times/week: Tuesday, Wednesday, Friday, Saturday\nBest time: late morning or early evening\nDuration: 45-55 min per session',
'ACTIVE', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM assignments WHERE order_id = 12);

-- Order 13: Әйгерім - post-natal (trainer elena)
INSERT INTO assignments (order_id, exercises, equipment, nutrition_plan, schedule_info, status, created_at, updated_at)
SELECT 13,
'Post-Natal Recovery:\nDiaphragmatic Breathing: 2-3 min\nCat-Cow Stretch: 10 slow reps\nPelvic Tilts: 3x12\nGlute Bridges: 3x10\nSupported Bird-Dog: 2x8 per side\nChest Opener Stretch: 30-45 sec',
'Soft Fitness Mat\nSmall Pilates Ball\nSturdy Chair',
'Hydration: 2.5-3L water daily (vital for recovery)\nCollagen-rich foods: bone broth, lean proteins\nCalcium & Vitamin D: dairy, eggs\nAvoid restrictive diets',
'2-3 times/week\nBest time: baby''s morning nap\nDuration: 20-30 min sessions (consistency over length)',
'ACTIVE', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM assignments WHERE order_id = 13);

-- Order 15: Dana - weight loss (trainer maria)
INSERT INTO assignments (order_id, exercises, equipment, nutrition_plan, schedule_info, status, created_at, updated_at)
SELECT 15,
'Full Body HIIT Circuit (3 rounds):\nBurpees: 10 reps\nJump Squats: 15 reps\nPush-Ups: 12 reps\nHigh Knees: 30 sec\nPlank: 45 sec\nRest: 60 sec between rounds',
'Fitness Mat\nTimer\nWater Bottle\nRunning Shoes',
'Caloric deficit: 400 kcal\nProtein-first meals\nCut sugar and processed foods\nGreen tea for metabolism\nMeal prep on Sundays',
'4 times/week\nMorning workouts preferred\nDuration: 40-50 min',
'ACTIVE', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM assignments WHERE order_id = 15);

-- Order 16: Dana - total body transformation (trainer maria)
INSERT INTO assignments (order_id, exercises, equipment, nutrition_plan, schedule_info, status, created_at, updated_at)
SELECT 16,
'12-Week Total Transformation Program:\nWeeks 1-4: Foundation strength (3x10-12)\nWeeks 5-8: Progressive overload (4x8-10)\nWeeks 9-12: Power & conditioning (5x6-8)\nCardio: 20 min LISS after every session',
'Full gym equipment\nBarbell & plates\nDumbbells set\nResistance bands\nPull-up bar',
'Periodized nutrition matching training phases\nBulk phase (W1-4): +200 kcal surplus\nMaintenance (W5-8): TDEE\nCut phase (W9-12): -300 kcal\nProtein: 2g per kg throughout',
'5 times/week (Mon-Fri)\nWeekend: active recovery (walking, stretching)\nDuration: 60-75 min sessions',
'ACTIVE', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM assignments WHERE order_id = 16);

-- Order 18: Assem - yoga (trainer maria)
INSERT INTO assignments (order_id, exercises, equipment, nutrition_plan, schedule_info, status, created_at, updated_at)
SELECT 18,
'Yoga Flow for Flexibility:\nOpening meditation: 5 min\nSun Salutations: 5 rounds\nStanding poses: Warrior I, II, III\nBalance: Tree, Eagle\nBackbends: Bridge, Camel\nSavasana: 10 min',
'Yoga mat\nBlocks x2\nBolster\nBlanket',
'Plant-based protein sources\nAnti-inflammatory diet\nHerbal teas: turmeric, ginger\nIntermittent hydration during practice\nLight meals 2h before',
'Daily practice possible\nIntense flow: 3x/week\nRestorative: 2x/week\nDuration: 60 min',
'ACTIVE', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM assignments WHERE order_id = 18);

-- Order 20: Sammy - core & flexibility (trainer elena)
-- (already has assignment 4, skip)

-- Order 21: Sammy - strength & tone (trainer maria)
-- (already has assignment 9, skip)

-- New orders for new clients
-- We need to get the new order IDs dynamically
-- Aliya order
INSERT INTO assignments (order_id, exercises, equipment, nutrition_plan, schedule_info, status, created_at, updated_at)
SELECT o.id,
'Beginner Fitness Plan:\nWarm-up: 10 min walk\nBodyweight Squats: 3x15\nWall Push-Ups: 3x12\nGlute Bridges: 3x15\nBird-Dog: 3x10 per side\nCool-down stretching: 10 min',
'Comfortable shoes\nYoga mat\nWater bottle',
'Balanced meals with protein and vegetables\nSnack 1h before workout\nHydration: 2L daily\nLimit processed foods',
'3 times/week\nMorning or afternoon\nDuration: 30-40 min',
'ACTIVE', NOW(), NOW()
FROM orders o
JOIN members m ON o.member_id = m.id
WHERE m.email = 'aliya@mail.kz' AND o.status = 'ACTIVE'
AND NOT EXISTS (SELECT 1 FROM assignments a WHERE a.order_id = o.id);

-- Madina order - yoga
INSERT INTO assignments (order_id, exercises, equipment, nutrition_plan, schedule_info, status, created_at, updated_at)
SELECT o.id,
'Yoga & Flexibility Flow:\nBreathwork: 5 min pranayama\nSun Salutation B: 3 rounds\nHip Openers: Pigeon, Lizard\nSpinal Twists: 1 min per side\nInversions: Legs-up-wall 5 min\nSavasana: 10 min',
'Yoga mat\nTwo blocks\nYoga strap\nBolster pillow',
'Pre-practice: light fruit snack\nPost-practice: protein shake\nAnti-inflammatory: turmeric, ginger\nHydration: 2-2.5L daily',
'4-5 times/week (yoga is gentle enough)\nEarly morning or evening\nDuration: 45-60 min',
'ACTIVE', NOW(), NOW()
FROM orders o
JOIN members m ON o.member_id = m.id
WHERE m.email = 'madina@mail.kz' AND o.status = 'ACTIVE'
AND NOT EXISTS (SELECT 1 FROM assignments a WHERE a.order_id = o.id);

-- Dinara order - strength & tone
INSERT INTO assignments (order_id, exercises, equipment, nutrition_plan, schedule_info, status, created_at, updated_at)
SELECT o.id,
'Strength & Tone Program:\nCompound Lifts (primary):\nDeadlift: 4x6-8\nBarbell Squat: 4x8\nBench Press: 3x10\nPull-Ups: 3x max\nIsolation Work:\nBicep Curls: 3x12\nTricep Dips: 3x12\nCore: Plank variations 3x45sec',
'Barbell & plates\nDumbbells (various weights)\nPull-up bar\nBench\nFitness mat',
'Protein priority: 130-150g daily\nMeal timing: protein within 30 min post-workout\nCreatine: 5g daily (optional)\nCarbs around workouts\nAvoid training on empty stomach',
'4 times/week: Mon/Tue/Thu/Fri\nPush/Pull/Legs split\nDuration: 60-70 min',
'ACTIVE', NOW(), NOW()
FROM orders o
JOIN members m ON o.member_id = m.id
WHERE m.email = 'dinara@mail.kz' AND o.status = 'ACTIVE'
AND NOT EXISTS (SELECT 1 FROM assignments a WHERE a.order_id = o.id);

-- Gulnara order - weight loss intensive
INSERT INTO assignments (order_id, exercises, equipment, nutrition_plan, schedule_info, status, created_at, updated_at)
SELECT o.id,
'Weight Loss Intensive Circuit:\nCardio Blast (20 min):\nJump Rope: 3x3 min\nBox Steps: 3x2 min\nStrength Circuit (3 rounds):\nDumbbell Squats x15\nPush-Ups x12\nDumbbell Rows x15\nCore Finisher:\nRussian Twists: 3x20\nBicycle Crunches: 3x30 sec',
'Jump Rope\nStep box\nLight-medium dumbbells\nMat',
'Calorie tracking: 1400-1600 kcal/day\nHigh volume foods: salads, soups\nProtein with every meal\nNo alcohol or sugary drinks\nCheat meal once per week maximum',
'5 times/week\nMornings preferred (boosted metabolism)\nDuration: 45 min',
'ACTIVE', NOW(), NOW()
FROM orders o
JOIN members m ON o.member_id = m.id
WHERE m.email = 'gulnara@mail.kz' AND o.status = 'ACTIVE'
AND NOT EXISTS (SELECT 1 FROM assignments a WHERE a.order_id = o.id);

-- Kamila order - post-natal
INSERT INTO assignments (order_id, exercises, equipment, nutrition_plan, schedule_info, status, created_at, updated_at)
SELECT o.id,
'Post-Natal Gentle Recovery:\nBreathing & Activation:\nDiaphragmatic breathing: 5 min\nPelvic floor activation: 10 reps\nGentle Strength:\nKneeling Push-Ups: 2x10\nWall Squats: 2x10\nGlute Bridges: 3x12\nStretching:\nHip flexor stretch: 30 sec each\nUpper back release: 1 min',
'Soft yoga mat\nSmall pilates ball\nLight resistance band',
'Breastfeeding nutrition: +500 kcal if nursing\nIron-rich foods: spinach, lentils, red meat\nCalcium: dairy or fortified alternatives\nOmega-3: salmon, flaxseeds\nHydration: 3L daily',
'2-3 times/week (listen to your body)\nDuring baby''s nap time\nDuration: 20-30 min\nRest day between sessions',
'ACTIVE', NOW(), NOW()
FROM orders o
JOIN members m ON o.member_id = m.id
WHERE m.email = 'kamila@mail.kz' AND o.status = 'ACTIVE'
AND NOT EXISTS (SELECT 1 FROM assignments a WHERE a.order_id = o.id);

-- Tolkyn order - strength foundation
INSERT INTO assignments (order_id, exercises, equipment, nutrition_plan, schedule_info, status, created_at, updated_at)
SELECT o.id,
'8-Week Strength Foundation:\nWeek 1-2 (Adaptation):\nGoblet Squat: 3x12\nDumbbell Press: 3x12\nSeated Cable Row: 3x12\nWeek 3-4 (Load):\nBarbell Squat: 4x10\nBench Press: 4x10\nBent-Over Row: 4x10\nWeek 5-8 (Strength):\nAll lifts: 5x5 progressive\nAccessory work: 3x12',
'Barbell + plates\nDumbbells full set\nCable machine\nBench\nSquat rack',
'Protein: 1.8g per kg\nCarbohydrate cycling (more on training days)\nPre-workout: oats + banana 1h before\nPost-workout: whey protein + fruit\nBedtime: cottage cheese (casein protein)',
'3 times/week: Mon/Wed/Fri\nFull-body each session\nDuration: 55-65 min',
'ACTIVE', NOW(), NOW()
FROM orders o
JOIN members m ON o.member_id = m.id
WHERE m.email = 'tolkyn@mail.kz' AND o.status = 'ACTIVE'
AND NOT EXISTS (SELECT 1 FROM assignments a WHERE a.order_id = o.id);

-- 5. Add reviews from new clients
INSERT INTO reviews (order_id, member_id, trainer_id, rating, comment, created_at)
SELECT o.id, o.member_id, o.trainer_id, 5,
       'Incredible trainer! My flexibility has improved so much in just 3 weeks. Highly recommend!',
       NOW() - INTERVAL '2 days'
FROM orders o JOIN members m ON o.member_id = m.id
WHERE m.email = 'madina@mail.kz' AND o.status = 'ACTIVE'
ON CONFLICT DO NOTHING;

INSERT INTO reviews (order_id, member_id, trainer_id, rating, comment, created_at)
SELECT o.id, o.member_id, o.trainer_id, 5,
       'The strength program is perfectly designed. I can see real results already. Maria is amazing!',
       NOW() - INTERVAL '3 days'
FROM orders o JOIN members m ON o.member_id = m.id
WHERE m.email = 'dinara@mail.kz' AND o.status = 'ACTIVE'
ON CONFLICT DO NOTHING;

INSERT INTO reviews (order_id, member_id, trainer_id, rating, comment, created_at)
SELECT o.id, o.member_id, o.trainer_id, 4,
       'Great program and very supportive trainer. The workouts are challenging but fun!',
       NOW() - INTERVAL '1 day'
FROM orders o JOIN members m ON o.member_id = m.id
WHERE m.email = 'tolkyn@mail.kz' AND o.status = 'ACTIVE'
ON CONFLICT DO NOTHING;
