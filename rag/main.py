from fastapi import FastAPI
from pydantic import BaseModel
from typing import Optional, Dict, Any

app = FastAPI()

class AskRequest(BaseModel):
    q: str
    user: Optional[Dict[str, Any]] = None
    options: Optional[Dict[str, Any]] = None

@app.post("/ask")
async def ask(req: AskRequest):
    return {
        "answer": "stub",
        "sources": [],
        "used_chunks": [],
        "safety": {},
        "tokens": {"prompt": 0, "completion": 0}
    }
