from fastapi import FastAPI
from pydantic import BaseModel
from typing import List, Optional, Any
from raglib import ensure_schema, index_chunks, search_similar

app = FastAPI(title="baria-bot RAG API")

class Chunk(BaseModel):
    content: str
    phase: Optional[str] = None
    source: Optional[str] = None
    last_reviewed: Optional[str] = None  # ISO8601

class IndexRequest(BaseModel):
    chunks: List[Chunk]

class AskRequest(BaseModel):
    query: str
    top_k: int = 5
    min_score: Optional[float] = None

@app.on_event("startup")
def _startup():
    ensure_schema()

@app.get("/health")
def health():
    return {"status": "ok"}

@app.post("/index_chunks")
def index_chunks_ep(req: IndexRequest):
    cnt = index_chunks([c.model_dump() for c in req.chunks])
    return {"inserted": cnt}

@app.post("/ask")
def ask_ep(req: AskRequest):
    hits = search_similar(req.query, top_k=req.top_k, min_score=req.min_score)
    return {"results": hits}
