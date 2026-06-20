-- FemFit — Migration v3: RU/KZ translations for training_cycles, fitness_classes, trainers
-- PostgreSQL, UTF-8
-- Run AFTER migration_v2_photos_and_trainers.sql has already been applied.
-- Safe to re-run: every UPDATE is keyed by id/email, idempotent by nature.
--
-- Run: psql -U postgres -d femfit -f migration_v3_translations.sql


-- ══════════════════════════════════════
--  1. NEW COLUMNS
-- ══════════════════════════════════════
ALTER TABLE training_cycles
    ADD COLUMN IF NOT EXISTS title_ru       VARCHAR(200),
    ADD COLUMN IF NOT EXISTS title_kz       VARCHAR(200),
    ADD COLUMN IF NOT EXISTS description_ru TEXT,
    ADD COLUMN IF NOT EXISTS description_kz TEXT;

ALTER TABLE fitness_classes
    ADD COLUMN IF NOT EXISTS is_active      BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS name_ru        VARCHAR(150),
    ADD COLUMN IF NOT EXISTS name_kz        VARCHAR(150),
    ADD COLUMN IF NOT EXISTS description_ru TEXT,
    ADD COLUMN IF NOT EXISTS description_kz TEXT;

ALTER TABLE trainers
    ADD COLUMN IF NOT EXISTS bio_ru             TEXT,
    ADD COLUMN IF NOT EXISTS bio_kz             TEXT,
    ADD COLUMN IF NOT EXISTS specialization_ru  VARCHAR(150),
    ADD COLUMN IF NOT EXISTS specialization_kz  VARCHAR(150);


-- ══════════════════════════════════════
--  2. TRANSLATIONS
-- ══════════════════════════════════════
-- training_cycles translations
UPDATE training_cycles SET title_ru = 'Сброс для начинающих', title_kz = 'Бастаушыларға арналған қалпына келтіру', description_ru = '4-недельная программа для всего тела для начинающих.', description_kz = 'Бастаушыларға арналған 4 апталық дене бойынша бағдарлама.' WHERE id = 1;
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
