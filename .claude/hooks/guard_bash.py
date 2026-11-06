#!/usr/bin/env python3
"""PreToolUse(Bash): barra comandos perigosos e viola AGENTS.md."""
import re

from _common import block, read_payload

payload = read_payload()
cmd = (payload.get("tool_input") or {}).get("command", "") or ""
if not cmd.strip():
    raise SystemExit(0)

RULES = [
    (r"\bgit\s+push\b.*(--force|-f\b|--force-with-lease)", "Force push proibido. Use push normal em branch de feature."),
    (r"\bgit\s+reset\s+--hard", "git reset --hard descarta trabalho. Use git stash ou um commit de checkpoint."),
    (r"\bgit\s+clean\s+-[a-zA-Z]*f", "git clean -f apaga arquivos nao versionados. Liste com git status antes."),
    (r"\bgit\s+(checkout|restore)\s+(--\s+)?\.(\s|$)", "Restaurar a arvore inteira descarta trabalho. Restaure arquivos especificos."),
    (r"\bgit\s+add\s+(-A|--all|\.)(\s|$)", "AGENTS.md regra 7: adicione arquivos explicitamente (git add src/...). Nunca -A nem ponto."),
    (r"\bgit\s+commit\b.*--no-verify", "Pular verificacoes de commit e proibido."),
    (r"\bgit\s+add\b.*(target/|\.class\b|\.jar\b|\.log\b|\.jfr\b)", "AGENTS.md regra 7: nao versionar target, class, jar, log ou jfr."),
    (r"\brm\s+(-[a-zA-Z]*r[a-zA-Z]*f|-[a-zA-Z]*f[a-zA-Z]*r)\s+(/|~|\$HOME|\.\s|\*)", "rm -rf em raiz, home ou curinga proibido."),
    (r"\brm\b.*\b(src/main/resources|personagens|assets)\b", "AGENTS.md regra 8: nao apagar assets sem justificativa tecnica escrita em .claude/docs/decisions.md. Peça aprovacao ao usuario."),
    (r"\b(curl|wget)\b.*\|\s*(sh|bash|python3?)", "Executar script baixado direto no shell e proibido."),
    (r"\bsudo\b", "sudo nao e permitido neste projeto."),
    (r"\bchmod\s+-R\s+777", "chmod -R 777 proibido."),
    (r">\s*src/main/resources/audio/", "Audio versionado nao pode ser sobrescrito por redirecionamento."),
    (r"\bmvn\b.*\bdeploy\b", "mvn deploy nao se aplica a este projeto."),
]
for pattern, message in RULES:
    if re.search(pattern, cmd):
        block("[guard_bash] Bloqueado: " + message)

# Conventional Commits (regra 6)
match = re.search(r"\bgit\s+commit\b.*?(?:-m|--message)\s+(?:\"([^\"]*)\"|'([^']*)')", cmd, re.S)
if match:
    message = (match.group(1) or match.group(2) or "").strip().splitlines()[0] if (match.group(1) or match.group(2)) else ""
    if message and not re.match(r"^(feat|fix|perf|refactor|docs|test|build|chore|ci|style)(\([\w./-]+\))?!?: \S", message):
        block(
            "[guard_bash] Mensagem de commit fora do padrao. Use '<tipo>: <mensagem no imperativo>' com tipo em "
            "feat, fix, perf, refactor, docs, test, build, chore, ci ou style. Recebido: " + message[:80]
        )
    if message and re.search(r"[\U0001F300-\U0001FAFF\u2600-\u27BF]", message):
        block("[guard_bash] Mensagem de commit sem emojis.")
raise SystemExit(0)
