#!/usr/bin/env python3
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
JAVA_ROOT = ROOT / "app" / "src" / "main" / "java"


def scan(path: Path):
    text = path.read_text(encoding="utf-8")
    state = "normal"
    escaped = False
    line = 1
    start_line = 1
    errors = []
    i = 0
    while i < len(text):
        c = text[i]
        n = text[i + 1] if i + 1 < len(text) else ""

        if state == "normal":
            if c == '/' and n == '/':
                state = "line_comment"
                i += 2
                continue
            if c == '/' and n == '*':
                state = "block_comment"
                start_line = line
                i += 2
                continue
            if c == '"':
                state = "string"
                escaped = False
                start_line = line
                i += 1
                continue
            if c == "'":
                state = "char"
                escaped = False
                start_line = line
                i += 1
                continue
        elif state == "line_comment":
            if c == '\n':
                state = "normal"
        elif state == "block_comment":
            if c == '*' and n == '/':
                state = "normal"
                i += 2
                continue
        elif state in ("string", "char"):
            if c == '\n' or c == '\r':
                kind = "string literal" if state == "string" else "character literal"
                errors.append(f"{path.relative_to(ROOT)}:{start_line}: raw newline inside Java {kind}")
                state = "normal"
                escaped = False
            elif escaped:
                escaped = False
            elif c == '\\':
                escaped = True
            elif (state == "string" and c == '"') or (state == "char" and c == "'"):
                state = "normal"

        if c == '\n':
            line += 1
        i += 1

    if state == "string":
        errors.append(f"{path.relative_to(ROOT)}:{start_line}: unclosed Java string literal")
    elif state == "char":
        errors.append(f"{path.relative_to(ROOT)}:{start_line}: unclosed Java character literal")
    elif state == "block_comment":
        errors.append(f"{path.relative_to(ROOT)}:{start_line}: unclosed Java block comment")
    return errors


def main():
    files = sorted(JAVA_ROOT.rglob("*.java"))
    if not files:
        print("ERROR: no Java source files found")
        return 1
    all_errors = []
    for path in files:
        all_errors.extend(scan(path))
    if all_errors:
        print("Java source literal check FAILED")
        for err in all_errors:
            print("ERROR:", err)
        return 1
    print(f"Java source literal check OK: {len(files)} Java files; no raw-newline or unclosed literals/comments found")
    return 0


if __name__ == "__main__":
    sys.exit(main())
