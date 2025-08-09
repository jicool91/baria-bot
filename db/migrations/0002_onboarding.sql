-- Дополнительные поля для онбординга в таблице users
ALTER TABLE users ADD COLUMN IF NOT EXISTS height_cm INTEGER;
ALTER TABLE users ADD COLUMN IF NOT EXISTS weight_kg NUMERIC(6,2);
ALTER TABLE users ADD COLUMN IF NOT EXISTS current_phase TEXT;
ALTER TABLE users ADD COLUMN IF NOT EXISTS dietary_restrictions TEXT;
ALTER TABLE users ADD COLUMN IF NOT EXISTS doctor_code TEXT;
ALTER TABLE users ADD COLUMN IF NOT EXISTS consent_given BOOLEAN DEFAULT FALSE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS onboarding_completed BOOLEAN DEFAULT FALSE;

-- Индекс для поиска по коду врача
CREATE INDEX IF NOT EXISTS idx_users_doctor_code ON users(doctor_code);

-- Таблица для red-flags симптомов
CREATE TABLE IF NOT EXISTS red_flags (
                                         id BIGSERIAL PRIMARY KEY,
                                         user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    symptoms TEXT NOT NULL,
    severity INTEGER CHECK (severity BETWEEN 1 AND 10),
    auto_escalated BOOLEAN DEFAULT FALSE,
    doctor_contacted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
    );

-- Таблица для кодов врачей
CREATE TABLE IF NOT EXISTS doctor_codes (
                                            id BIGSERIAL PRIMARY KEY,
                                            code TEXT UNIQUE NOT NULL,
                                            doctor_id BIGINT NOT NULL REFERENCES doctors(id) ON DELETE CASCADE,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
    );

-- Сначала создаем тестового врача если его нет
INSERT INTO users (id, tg_id, username, full_name, role)
VALUES (1, 999999999, 'test_doctor', 'Тестовый Врач', 'doctor')
    ON CONFLICT (tg_id) DO NOTHING;

INSERT INTO doctors (id, user_id, specialty)
VALUES (1, 1, 'Бариатрический хирург')
    ON CONFLICT (user_id) DO NOTHING;

-- Добавляем тестовый код врача
INSERT INTO doctor_codes (code, doctor_id, active)
VALUES ('DOC001', 1, TRUE)
    ON CONFLICT (code) DO NOTHING;