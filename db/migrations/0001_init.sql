CREATE EXTENSION IF NOT EXISTS vector;

-- базовая таблица под чанки (RAG)
CREATE TABLE IF NOT EXISTS kb_chunk (
  id BIGSERIAL PRIMARY KEY,
  phase TEXT,
  content TEXT NOT NULL,
  source TEXT,
  last_reviewed DATE,
  embedding VECTOR(1024)
);
CREATE INDEX IF NOT EXISTS idx_kb_chunk_embedding ON kb_chunk USING ivfflat (embedding vector_cosine_ops);

CREATE TABLE IF NOT EXISTS users (
  id BIGSERIAL PRIMARY KEY,
  tg_id BIGINT UNIQUE,
  tz TEXT,
  surgery_date DATE,
  phase TEXT,
  created_at TIMESTAMP DEFAULT now()
);
