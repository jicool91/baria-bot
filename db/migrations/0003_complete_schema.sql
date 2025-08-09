-- Расширяем схему согласно полному ТЗ
-- pgvector уже должен быть установлен

-- Обновляем таблицу пользователей согласно ТЗ
ALTER TABLE users 
ADD COLUMN IF NOT EXISTS consent_at TIMESTAMP,
ADD COLUMN IF NOT EXISTS tz TEXT DEFAULT 'Europe/Moscow',
ADD COLUMN IF NOT EXISTS phase TEXT CHECK (phase IN ('liquid', 'pureed', 'soft', 'regular')),
ADD COLUMN IF NOT EXISTS phase_mode TEXT DEFAULT 'auto' CHECK (phase_mode IN ('auto', 'manual')),
ADD COLUMN IF NOT EXISTS height_cm INT,
ADD COLUMN IF NOT EXISTS weight_kg NUMERIC(5,2),
ADD COLUMN IF NOT EXISTS goal TEXT CHECK (goal IN ('lose', 'maintain', 'protein')),
ADD COLUMN IF NOT EXISTS restrictions JSONB DEFAULT '[]'::jsonb,
ADD COLUMN IF NOT EXISTS supplements JSONB DEFAULT '[]'::jsonb,
ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT now();

-- Таблица продуктов
CREATE TABLE IF NOT EXISTS product (
  id BIGSERIAL PRIMARY KEY,
  title TEXT NOT NULL,
  category TEXT NOT NULL CHECK (category IN ('sets', 'masks', 'shampoos', 'creams', 'supps')),
  description TEXT,
  price NUMERIC(10,2),
  url TEXT,
  allowed_phases TEXT[],
  allergens TEXT[],
  tags TEXT[],
  image_url TEXT,
  active BOOLEAN DEFAULT TRUE,
  created_at TIMESTAMP DEFAULT now(),
  updated_at TIMESTAMP DEFAULT now()
);

-- Таблица напоминаний (обновляем существующую)
ALTER TABLE reminders 
ADD COLUMN IF NOT EXISTS type TEXT,
ADD COLUMN IF NOT EXISTS rrule TEXT,
ADD COLUMN IF NOT EXISTS local_time TEXT,
ADD COLUMN IF NOT EXISTS meta JSONB DEFAULT '{}'::jsonb;

UPDATE reminders SET type = kind WHERE type IS NULL;
ALTER TABLE reminders DROP COLUMN IF EXISTS kind;
ALTER TABLE reminders DROP COLUMN IF EXISTS cron;

-- Логирование симптомов
CREATE TABLE IF NOT EXISTS symptom_log (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
  symptom TEXT NOT NULL,
  started_at TIMESTAMP DEFAULT now(),
  status TEXT DEFAULT 'active' CHECK (status IN ('active', 'resolved')),
  notes TEXT,
  created_at TIMESTAMP DEFAULT now()
);

-- KB chunks для RAG (обновляем существующую структуру)
CREATE TABLE IF NOT EXISTS kb_chunk (
  id BIGSERIAL PRIMARY KEY,
  phase TEXT,
  content TEXT NOT NULL,
  source TEXT,
  last_reviewed DATE,
  embedding VECTOR(1024),
  created_at TIMESTAMP DEFAULT now()
);

-- Индексы для поиска
CREATE INDEX IF NOT EXISTS idx_kb_chunk_embedding ON kb_chunk USING ivfflat (embedding vector_cosine_ops);
CREATE INDEX IF NOT EXISTS idx_kb_chunk_phase ON kb_chunk(phase);

-- Аудит RAG запросов
CREATE TABLE IF NOT EXISTS rag_audit (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT REFERENCES users(id),
  user_query TEXT,
  retrieved_ids BIGINT[],
  model TEXT,
  prompt_tokens INT,
  completion_tokens INT,
  answer TEXT,
  sources TEXT[],
  safety_flags TEXT[],
  created_at TIMESTAMP DEFAULT now()
);

-- Состояние FSM для пользователей
CREATE TABLE IF NOT EXISTS user_state (
  user_id BIGINT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
  state TEXT NOT NULL,
  payload JSONB DEFAULT '{}'::jsonb,
  updated_at TIMESTAMP DEFAULT now()
);

-- Red flags (обновляем существующую таблицу)
CREATE TABLE IF NOT EXISTS red_flags (
  id BIGSERIAL PRIMARY KEY,
  symptom TEXT NOT NULL,
  questions TEXT[],
  urgent_message TEXT,
  self_care_tips TEXT[],
  created_at TIMESTAMP DEFAULT now()
);

-- Планы питания
CREATE TABLE IF NOT EXISTS meal_plan (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
  day_number INT NOT NULL,
  phase TEXT NOT NULL,
  meals JSONB NOT NULL,
  created_at TIMESTAMP DEFAULT now()
);

-- Каталог просмотров (аналитика)
CREATE TABLE IF NOT EXISTS product_view (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT REFERENCES users(id),
  product_id BIGINT REFERENCES product(id),
  action TEXT CHECK (action IN ('view', 'buy_click')),
  created_at TIMESTAMP DEFAULT now()
);

-- Индексы для производительности
CREATE INDEX IF NOT EXISTS idx_users_tg_id ON users(tg_id);
CREATE INDEX IF NOT EXISTS idx_symptom_log_user_status ON symptom_log(user_id, status);
CREATE INDEX IF NOT EXISTS idx_product_category_active ON product(category, active);
CREATE INDEX IF NOT EXISTS idx_product_view_user_product ON product_view(user_id, product_id);
CREATE INDEX IF NOT EXISTS idx_rag_audit_user_created ON rag_audit(user_id, created_at);
