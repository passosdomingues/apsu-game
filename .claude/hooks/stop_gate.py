#!/usr/bin/env python3
"""Stop: se houve mudanca em Java e os testes nao passaram nesse estado, bloqueia o encerramento."""
import hashlib
import shutil
import subprocess

from _common import PROJECT, block, git, read_payload, read_state, write_state

payload = read_payload()
if payload.get("stop_hook_active"):
    raise SystemExit(0)

changed = git("diff", "HEAD", "--name-only")
untracked = git("ls-files", "--others", "--exclude-standard")
relevant = [
    name for name in (changed + "\n" + untracked).splitlines()
    if name.startswith(("src/main/java", "src/test/java", "pom.xml"))
]
if not relevant:
    raise SystemExit(0)

fingerprint = hashlib.sha256(
    (git("diff", "HEAD", "--", "src", "pom.xml") + "\n".join(sorted(relevant))).encode("utf-8")
).hexdigest()
if read_state("last-green-hash") == fingerprint:
    raise SystemExit(0)
if not shutil.which("mvn"):
    print("[stop_gate] Maven nao encontrado; testes nao executados.")
    raise SystemExit(0)

try:
    result = subprocess.run(
        ["mvn", "-o", "-B", "-q", "test"], cwd=PROJECT, capture_output=True, text=True, timeout=270
    )
except subprocess.TimeoutExpired:
    block("[stop_gate] mvn test excedeu 270 s. Rode make test manualmente e reporte o resultado antes de concluir.")
if result.returncode != 0:
    tail = (result.stdout + result.stderr).strip().splitlines()[-40:]
    block("[stop_gate] mvn test falhou. Corrija antes de concluir (AGENTS.md regra 4).\n" + "\n".join(tail))
write_state("last-green-hash", fingerprint)
raise SystemExit(0)
