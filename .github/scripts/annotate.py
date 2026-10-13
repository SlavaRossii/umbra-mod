"""Publish a text file as workflow notice annotations (readable through the checks API).

usage: annotate.py <title> <file> [max_chunks]
"""
import sys

title, path = sys.argv[1], sys.argv[2]
max_chunks = int(sys.argv[3]) if len(sys.argv) > 3 else 9
CHUNK = 60000

try:
    text = open(path, encoding="utf-8", errors="replace").read()
except FileNotFoundError:
    text = f"(no {path})"
if not text.strip():
    text = "(empty)"

chunks = [text[i:i + CHUNK] for i in range(0, len(text), CHUNK)][:max_chunks]
for n, chunk in enumerate(chunks, 1):
    esc = chunk.replace("%", "%25").replace("\r", "%0D").replace("\n", "%0A")
    print(f"::notice title={title} {n}/{len(chunks)}::{esc}")
