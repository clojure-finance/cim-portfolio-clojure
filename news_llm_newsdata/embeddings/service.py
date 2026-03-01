"""Lightweight Sentence Transformer Inference Service.

Uses pre-trained models from HuggingFace (via sentence-transformers) 
to provide embeddings to the Clojure app.

Usage:
    uvicorn embeddings.service:app --host 0.0.0.0 --port 8000

Environment Variables:
    MODEL_NAME: Name of the sentence-transformer model to use.
                Default: 'all-MiniLM-L6-v2' (Small, fast, good quality)
"""

from __future__ import annotations

import os
from typing import Optional

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from sentence_transformers import SentenceTransformer


# Use a small, efficient model by default
MODEL_NAME = os.environ.get("MODEL_NAME", "all-MiniLM-L6-v2")

app = FastAPI(title="Embedding Service", version="0.2.0")
_model: Optional[SentenceTransformer] = None


def load_model() -> SentenceTransformer:
    """Lazily load the SentenceTransformer model."""
    global _model
    if _model is None:
        print(f"Loading model: {MODEL_NAME} ...")
        _model = SentenceTransformer(MODEL_NAME)
        print("Model loaded.")
    return _model


class InferRequest(BaseModel):
    text: str


@app.get("/health")
def health():
    try:
        model = load_model()
        return {"status": "ok", "model": MODEL_NAME, "device": str(model.device)}
    except Exception as e:
        return {"status": "error", "detail": str(e)}


@app.post("/infer")
def infer(req: InferRequest):
    if not req.text.strip():
        raise HTTPException(status_code=400, detail="text is required")

    model = load_model()
    
    # SentenceTransformer allows encoding directly
    # It returns a numpy array, convert to list for JSON serialization
    vector = model.encode(req.text)
    
    return {"vector": vector.tolist(), "dim": len(vector)}


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=int(os.environ.get("PORT", 8000)))
