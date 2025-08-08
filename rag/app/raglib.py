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

def index_chunks(chunks: List[Dict[str, Any]]) -> int:
    texts = [c["content"] for c in chunks]
    embs = embed_texts(texts)
    with connect() as con, con.cursor() as cur:
        for chunk, emb in zip(chunks, embs):
            cur.execute("""
                INSERT INTO chunks (doc_id, source, content, embedding, content_sha)
                VALUES (%s, %s, %s, (%s::real[])::vector, md5(%s))
                ON CONFLICT (doc_id, content_sha) DO NOTHING
            """, (chunk.get("doc_id"), chunk.get("source", "unknown"), chunk["content"], emb, chunk["content"]))
    return len(chunks)

def search(query: str, top_k: int = 5) -> List[Dict[str, Any]]:
    q_emb = embed_texts([query])[0]
    with connect() as con, con.cursor() as cur:
        cur.execute("""
            SET LOCAL ivfflat.probes = 10;
            WITH q AS (SELECT ((%s::real[])::vector) AS qvec)
            SELECT id, doc_id, source, content, 1 - (embedding <=> q.qvec) AS score
            FROM chunks, q
            ORDER BY embedding <=> q.qvec
            LIMIT %s
        """, (q_emb, top_k))
        rows = cur.fetchall()
        return [{"id": r[0], "doc_id": r[1], "source": r[2], "content": r[3], "score": float(r[4])} for r in rows]
