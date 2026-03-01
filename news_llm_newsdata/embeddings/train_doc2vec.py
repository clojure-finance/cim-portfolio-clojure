"""Train a Doc2Vec model from collected articles.

Example:
	python embeddings/train_doc2vec.py \
		--input data/articles.jsonl \
		--output embeddings/model/doc2vec.bin \
		--vector-size 300 --epochs 30
"""

from __future__ import annotations

import argparse
import json
import os
from pathlib import Path
from typing import Iterable, List

from gensim.models.doc2vec import Doc2Vec, TaggedDocument
from gensim.utils import simple_preprocess


def read_corpus(path: Path) -> Iterable[TaggedDocument]:
	with path.open("r", encoding="utf-8") as f:
		for idx, line in enumerate(f):
			obj = json.loads(line)
			text = f"{obj.get('title', '')} {obj.get('content', '')}"
			tokens = simple_preprocess(text)
			if tokens:
				yield TaggedDocument(tokens, [idx])


def train_model(docs: List[TaggedDocument], vector_size: int, epochs: int, min_count: int, window: int, dm: int, output: Path):
	if not docs:
		raise ValueError("No documents to train on.")

	model = Doc2Vec(
		vector_size=vector_size,
		min_count=min_count,
		window=window,
		dm=dm,
		workers=os.cpu_count() or 2,
	)
	model.build_vocab(docs)
	model.train(docs, total_examples=model.corpus_count, epochs=epochs)

	output.parent.mkdir(parents=True, exist_ok=True)
	model.save(str(output))
	return output


def parse_args():
	p = argparse.ArgumentParser(description="Train Doc2Vec on collected news articles.")
	p.add_argument("--input", type=Path, default=Path("data/articles.jsonl"), help="JSONL file with title/content fields")
	p.add_argument("--output", type=Path, default=Path("embeddings/model/doc2vec.bin"), help="Where to save the model")
	p.add_argument("--vector-size", type=int, default=300)
	p.add_argument("--epochs", type=int, default=30)
	p.add_argument("--min-count", type=int, default=2)
	p.add_argument("--window", type=int, default=5)
	p.add_argument("--dm", type=int, default=1, help="1: Distributed Memory, 0: DBOW")
	return p.parse_args()


def main():
	args = parse_args()
	if not args.input.exists():
		raise SystemExit(f"Input file not found: {args.input}")

	docs = list(read_corpus(args.input))
	out = train_model(docs, args.vector_size, args.epochs, args.min_count, args.window, args.dm, args.output)
	print(f"Saved model to {out}")


if __name__ == "__main__":  # pragma: no cover
	main()
