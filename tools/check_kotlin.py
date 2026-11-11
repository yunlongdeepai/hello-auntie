# -*- coding: utf-8 -*-
import re
from pathlib import Path

root = Path(__file__).resolve().parent.parent / "android"
files = sorted(root.rglob("*.kt"))
print("kotlin files:", len(files))

for f in files:
    s = f.read_text(encoding="utf-8")
    s = re.sub(r"/\*.*?\*/", "", s, flags=re.S)
    s = re.sub(r"//[^\n]*", "", s)
    s = re.sub(r'""".*?"""', '""', s, flags=re.S)
    s = re.sub(r'"(?:\\.|[^"\\])*"', '""', s)
    s = re.sub(r"'(?:\\.|[^'\\])*'", "''", s)
    counts = {c: s.count(c) for c in "{}()[]"}
    bad = (
        counts["{"] != counts["}"]
        or counts["("] != counts[")"]
        or counts["["] != counts["]"]
    )
    if bad:
        print("IMBALANCE", f.relative_to(root), counts)

print("done")
