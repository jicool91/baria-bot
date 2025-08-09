import os
import psycopg2
from typing import List, Dict, Any
from sentence_transformers import SentenceTransformer

DB_URL = os.getenv("DB_URL", "postgresql://app:app@db:5432/baria")
EMB_MODEL_NAME = os.getenv("EMB_MODEL_NAME", "sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2")

_model = None

def get_model():
    global _model
    if _model is None:
        print("Загружаем модель embeddings...")
        _model = SentenceTransformer(EMB_MODEL_NAME)
        print("Модель загружена")
    return _model

def embed_texts(texts: List[str]) -> List[List[float]]:
    return get_model().encode(texts, normalize_embeddings=True).tolist()

def connect():
    return psycopg2.connect(DB_URL)

def vector_to_string(vector: List[float]) -> str:
    """Конвертирует список в строку для pgvector"""
    return '[' + ','.join(map(str, vector)) + ']'

def ensure_schema():
    """Создаем недостающие таблицы если их нет"""
    with connect() as con, con.cursor() as cur:
        cur.execute("CREATE EXTENSION IF NOT EXISTS vector;")

        # Создаем таблицы если их нет (упрощенная версия)
        cur.execute("""
            CREATE TABLE IF NOT EXISTS rag_documents (
                id BIGSERIAL PRIMARY KEY,
                source TEXT,
                created_at TIMESTAMPTZ NOT NULL DEFAULT now()
            );
        """)

        cur.execute("""
            CREATE TABLE IF NOT EXISTS rag_chunks (
                id BIGSERIAL PRIMARY KEY,
                doc_id BIGINT NOT NULL REFERENCES rag_documents(id) ON DELETE CASCADE,
                content TEXT NOT NULL,
                embedding vector(384)
            );
        """)

        # Проверяем есть ли индекс, создаем если нет
        cur.execute("""
            SELECT 1 FROM pg_indexes
            WHERE tablename = 'rag_chunks' AND indexname = 'idx_rag_chunks_emb'
        """)
        if not cur.fetchone():
            cur.execute("""
                CREATE INDEX idx_rag_chunks_emb
                ON rag_chunks USING ivfflat (embedding vector_cosine_ops)
                WITH (lists = 100);
            """)

        con.commit()
        print("Схема БД готова")

def get_chunks_count() -> int:
    """Возвращает количество чанков в базе"""
    try:
        with connect() as con, con.cursor() as cur:
            cur.execute("SELECT COUNT(*) FROM rag_chunks")
            result = cur.fetchone()
            return result[0] if result else 0
    except Exception as e:
        print(f"Ошибка подсчета чанков: {e}")
        return 0

def clear_data():
    """Очищает все данные"""
    try:
        with connect() as con, con.cursor() as cur:
            cur.execute("DELETE FROM rag_chunks")
            cur.execute("DELETE FROM rag_documents")
            con.commit()
            print("Данные очищены")
    except Exception as e:
        print(f"Ошибка очистки данных: {e}")

def add_sample_data():
    """Добавляет базовые данные для тестирования"""
    print("Начинаем добавление тестовых данных...")

    sample_chunks = [
        {
            "doc_id": 1,
            "source": "Протокол бариатрии v1.0",
            "content": "После бариатрической операции важно соблюдать диету по фазам: жидкая (1-2 недели), пюре (2-4 недели), мягкая (4-8 недель), обычная (после 8 недель)."
        },
        {
            "doc_id": 1,
            "source": "Протокол бариатрии v1.0",
            "content": "На жидкой фазе разрешены: вода, бульоны, протеиновые коктейли без сахара, травяные чаи. Порции 30-60 мл каждые 30 минут."
        },
        {
            "doc_id": 1,
            "source": "Протокол бариатрии v1.0",
            "content": "Красные флаги после операции: сильная боль >7/10, рвота с кровью, температура >38.5°C, черный стул, боль в груди. При любом из этих симптомов - немедленно к врачу."
        },
        {
            "doc_id": 1,
            "source": "FAQ по питанию",
            "content": "Кофе можно вводить осторожно после 4-6 недель, начинать с небольших количеств. Избегать кофеина натощак."
        },
        {
            "doc_id": 1,
            "source": "Витамины после бариатрии",
            "content": "Обязательные витамины: B12, железо, кальций с витамином D, мультивитамины. Принимать пожизненно под контролем анализов."
        }
    ]

    try:
        # Сначала добавляем документ
        with connect() as con, con.cursor() as cur:
            cur.execute("""
                INSERT INTO rag_documents(id, source)
                VALUES (1, 'Медицинские протоколы тестовые')
                ON CONFLICT (id) DO NOTHING
            """)
            con.commit()

        # Создаем embeddings
        texts = [chunk["content"] for chunk in sample_chunks]
        print(f"Создаем embeddings для {len(texts)} текстов...")
        embeddings = embed_texts(texts)
        print("Embeddings созданы")

        # Добавляем чанки по одному
        with connect() as con, con.cursor() as cur:
            for i, (chunk, embedding) in enumerate(zip(sample_chunks, embeddings)):
                emb_str = vector_to_string(embedding)

                cur.execute("""
                    INSERT INTO rag_chunks(doc_id, content, embedding)
                    VALUES (%s, %s, %s::vector)
                """, (chunk["doc_id"], chunk["content"], emb_str))

                print(f"Добавлен чанк {i+1}/{len(sample_chunks)}")

            con.commit()
            print(f"Успешно добавлено {len(sample_chunks)} чанков")
            return len(sample_chunks)

    except Exception as e:
        print(f"Ошибка при добавлении данных: {e}")
        import traceback
        traceback.print_exc()
        return 0

def search(query: str, top_k: int = 5) -> List[Dict[str, Any]]:
    """Поиск похожих чанков"""
    if not query.strip():
        return []

    try:
        q_emb = embed_texts([query])[0]
        q_emb_str = vector_to_string(q_emb)

        with connect() as con, con.cursor() as cur:
            cur.execute("""
                SELECT c.id, c.doc_id, d.source, c.content,
                       1 - (c.embedding <=> %s::vector) AS score
                FROM rag_chunks c
                JOIN rag_documents d ON c.doc_id = d.id
                ORDER BY c.embedding <=> %s::vector
                LIMIT %s
            """, (q_emb_str, q_emb_str, top_k))

            rows = cur.fetchall()
            results = [
                {
                    "id": r[0],
                    "doc_id": r[1],
                    "source": r[2],
                    "content": r[3],
                    "score": float(r[4])
                }
                for r in rows
            ]

            print(f"Найдено {len(results)} результатов для запроса: {query[:50]}...")
            return results

    except Exception as e:
        print(f"Ошибка поиска: {e}")
        return []

# Инициализация при импорте
if __name__ == "__main__":
    ensure_schema()
    if get_chunks_count() == 0:
        add_sample_data()