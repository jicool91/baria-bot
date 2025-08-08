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
        _model = SentenceTransformer(EMB_MODEL_NAME)
    return _model

def embed_texts(texts: List[str]) -> List[List[float]]:
    return get_model().encode(texts, normalize_embeddings=True).tolist()

def connect():
    return psycopg2.connect(DB_URL)

def ensure_schema():
    """Создаем недостающие таблицы если их нет"""
    with connect() as con, con.cursor() as cur:
        cur.execute("CREATE EXTENSION IF NOT EXISTS vector;")
        # Используем таблицы из init.sql
        cur.execute("""
            CREATE TABLE IF NOT EXISTS rag_chunks (
                id BIGSERIAL PRIMARY KEY,
                doc_id BIGINT NOT NULL REFERENCES rag_documents(id) ON DELETE CASCADE,
                content TEXT NOT NULL,
                embedding vector(384)
            );
        """)
        cur.execute("""
            CREATE INDEX IF NOT EXISTS idx_rag_chunks_emb
            ON rag_chunks USING ivfflat (embedding vector_cosine_ops);
        """)
        con.commit()

def index_chunks(chunks: List[Dict[str, Any]]) -> int:
    texts = [c["content"] for c in chunks]
    embs = embed_texts(texts)
    inserted = 0

    with connect() as con, con.cursor() as cur:
        for chunk, emb in zip(chunks, embs):
            doc_id = chunk.get("doc_id", 1)  # default doc_id если не указан

            # Вставляем документ если не существует
            cur.execute("""
                INSERT INTO rag_documents(id, source, meta)
                VALUES (%s, %s, %s)
                ON CONFLICT (id) DO NOTHING
            """, (doc_id, chunk.get("source", "unknown"), {}))

            # Вставляем чанк
            cur.execute("""
                INSERT INTO rag_chunks(doc_id, content, embedding)
                VALUES (%s, %s, %s::vector)
            """, (doc_id, chunk["content"], emb))
            inserted += 1
        con.commit()
    return inserted

def search(query: str, top_k: int = 5) -> List[Dict[str, Any]]:
    q_emb = embed_texts([query])[0]

    with connect() as con, con.cursor() as cur:
        cur.execute("""
            SET LOCAL ivfflat.probes = 10;
            SELECT c.id, c.doc_id, d.source, c.content,
                   1 - (c.embedding <=> %s::vector) AS score
            FROM rag_chunks c
            JOIN rag_documents d ON c.doc_id = d.id
            ORDER BY c.embedding <=> %s::vector
            LIMIT %s
        """, (q_emb, q_emb, top_k))

        rows = cur.fetchall()
        return [
            {
                "id": r[0],
                "doc_id": r[1],
                "source": r[2],
                "content": r[3],
                "score": float(r[4])
            }
            for r in rows
        ]