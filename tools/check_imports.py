# -*- coding: utf-8 -*-
import re
from pathlib import Path

root = Path(__file__).resolve().parent.parent / "android"
decls = {}
for f in root.rglob("*.kt"):
    text = f.read_text(encoding="utf-8")
    pkg = re.search(r"^package\s+([\w.]+)", text, re.M)
    if not pkg:
        continue
    pkg = pkg.group(1)
    for m in re.finditer(
        r"^(?:@\w+\s+)*(?:(?:data|sealed|data\s+sealed)\s+)?(?:class|object|interface|enum\s+class|fun)\s+(\w+)",
        text,
        re.M,
    ):
        decls.setdefault(pkg + "." + m.group(1), []).append(str(f.relative_to(root)))

missing = []
for f in root.rglob("*.kt"):
    text = f.read_text(encoding="utf-8")
    for line in text.splitlines():
        line = line.strip()
        if not line.startswith("import "):
            continue
        name = line[len("import "):].strip()
        if name.startswith("com.helloauntie.") and not name.endswith("*"):
            if name not in decls:
                missing.append((str(f.relative_to(root)), name))

print("declared:", len(decls))
print("missing local imports:", len(missing))
for item in sorted(set(missing)):
    print(item[0], "->", item[1])
