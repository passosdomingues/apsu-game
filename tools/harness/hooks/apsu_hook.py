#!/usr/bin/env python3
"""Hooks do harness APSU para Claude Code.

Somente biblioteca padrao (sem venv, sem pip). Subcomandos:
  session-start | pre-bash | pre-edit | post-edit | stop

Convencoes do Claude Code usadas aqui:
  exit 2            bloqueia e devolve stderr ao agente
  JSON "ask"        pede confirmacao ao humano (PreToolUse)
  exit 0 com stdout contexto adicional (SessionStart) ou JSON
Os guardrails aplicam as 13 regras do AGENTS.md, sem substitui-las.
"""
import json
import os
import re
import shlex
import subprocess
import sys
from pathlib import Path

ROOT = Path(os.environ.get("CLAUDE_PROJECT_DIR") or os.getcwd()).resolve()
STATE = ROOT / ".claude" / "state"
DIRTY = STATE / "dirty-java"
sys.path.insert(0, str(ROOT / "tools" / "harness"))

CONVENTIONAL = re.compile(
    r"^(feat|fix|docs|style|refactor|perf|test|build|ci|chore|revert)(\([^)]+\))?!?: \S.*"
)
PROTECTED = ("assets/", "personagens/", "src/main/resources/", "mpi/", "docs/diagrams/", "docs/audio/")
SAFE_RECURSIVE = ("target", "logs", ".claude/state", "/tmp")
GOD_OBJECTS = {
    "GameContext.java": "regra 10 (modularidade) do AGENTS.md",
    "RenderEngine.java": "regra 10 (modularidade) do AGENTS.md",
    "GameLoop.java": "regra 11 (frame-time do loop de 60 FPS) do AGENTS.md",
}
LOOP_FILES = {"GameLoop.java", "GameContext.java", "RenderEngine.java", "Runtime3DLayer.java"}
EMOJI = re.compile("[\U0001F000-\U0001FAFF\u2600-\u27BF\uFE0F]")
EM_DASH = "\u2014"


def read_input():
    try:
        return json.loads(sys.stdin.read() or "{}")
    except json.JSONDecodeError:
        return {}


def block(message):
    sys.stderr.write(message.rstrip() + "\n")
    sys.exit(2)


def ask(reason):
    print(json.dumps({"hookSpecificOutput": {
        "hookEventName": "PreToolUse",
        "permissionDecision": "ask",
        "permissionDecisionReason": reason,
    }}))
    sys.exit(0)


def context(event, text):
    print(json.dumps({"hookSpecificOutput": {"hookEventName": event, "additionalContext": text}}))
    sys.exit(0)


def rel(path_str):
    p = Path(path_str)
    if not p.is_absolute():
        p = ROOT / p
    p = Path(os.path.normpath(p))
    try:
        return p.relative_to(ROOT).as_posix(), True
    except ValueError:
        return p.as_posix(), False


def is_artifact(path):
    p = path.lstrip("./")
    return bool(
        re.match(r"^(target|build|out|logs)(/|$)", p)
        or "/target/" in p
        or p.endswith((".class", ".jar", ".log", ".blend1"))
    )


# ---------------------------------------------------------------- pre-bash

def segment_tokens(seg):
    try:
        toks = shlex.split(seg)
    except ValueError:
        toks = seg.split()
    while toks and re.match(r"^[A-Za-z_][A-Za-z0-9_]*=", toks[0]):
        toks = toks[1:]
    return toks


def pre_bash():
    data = read_input()
    cmd = (data.get("tool_input") or {}).get("command", "")
    blocks, asks = [], []

    if re.search(r"\bgit\s+commit\b", cmd):
        if "--no-verify" in cmd or re.search(r"\s-n(\s|$)", cmd):
            blocks.append("git commit sem verificacao e proibido (regra 06 do AGENTS.md).")
        try:
            toks = shlex.split(cmd)
        except ValueError:
            toks = []
        if "-m" in toks and "$(" not in cmd:
            msg = toks[toks.index("-m") + 1] if toks.index("-m") + 1 < len(toks) else ""
            if msg and not CONVENTIONAL.match(msg.splitlines()[0]):
                blocks.append(
                    "Mensagem de commit fora do padrao Conventional Commits (regra 06). "
                    "Formato: tipo(escopo): descricao. Tipos: feat, fix, docs, style, refactor, perf, test, build, ci, chore, revert."
                )

    for seg in re.split(r"\s*(?:&&|\|\||;|\|)\s*", cmd):
        t = segment_tokens(seg)
        if not t:
            continue
        head, args = t[0], t[1:]

        if head == "git" and args:
            sub = args[0]
            rest = args[1:]
            if sub == "push" and any(a in ("-f", "--force", "--force-with-lease") for a in rest):
                blocks.append("git push forcado e proibido.")
            if sub == "reset" and "--hard" in rest:
                blocks.append("git reset --hard descarta trabalho (regra 01). Use git stash ou peca ao usuario.")
            if sub == "clean" and any(a.startswith("-") and "f" in a for a in rest):
                blocks.append("git clean -f remove arquivos nao rastreados (regra 08). Peca ao usuario.")
            if sub == "add":
                paths = [a for a in rest if not a.startswith("-") or a in ("-A", "--all")]
                if any(a in ("-A", "--all", ".", "*") for a in paths):
                    blocks.append(
                        "git add amplo e proibido (regras 01, 06 e 07). Adicione caminhos explicitos, um commit atomico por vez."
                    )
                for a in paths:
                    if is_artifact(a):
                        blocks.append(f"Artefato de build nao pode ser commitado (regra 07): {a}")
            if sub == "rm":
                for a in rest:
                    if not a.startswith("-") and a.startswith(PROTECTED):
                        asks.append(f"git rm em asset protegido ({a}). Regra 08 exige justificativa tecnica explicita.")

        if head in ("rm", "unlink"):
            recursive = any(a.startswith("-") and ("r" in a or "R" in a) for a in args)
            for a in [x for x in args if not x.startswith("-")]:
                if a in ("/", "~", ".", "..", "*", "./*"):
                    blocks.append(f"rm em alvo perigoso: {a}")
                    continue
                path, inside = rel(a)
                if path.startswith(PROTECTED):
                    asks.append(f"Exclusao de asset protegido ({path}). Regra 08 exige justificativa tecnica explicita.")
                elif recursive and not path.startswith(SAFE_RECURSIVE):
                    asks.append(f"Exclusao recursiva fora de target/ ({path}).")

        if head == "make":
            targets = [a for a in args if not a.startswith("-") and "=" not in a]
            if not targets or any(x in ("run", "all", "docker-run") for x in targets):
                blocks.append(
                    "make run, make all e make sem alvo abrem a janela JavaFX e nao retornam. "
                    "Use make test, make build ou make coverage; peca ao usuario para jogar e validar."
                )
            if any(x in ("assets-regenerate", "generate-characters") for x in targets):
                asks.append("Este alvo sobrescreve fontes .blend em personagens/blends (regra 08).")

        if head in ("mvn", "./mvnw", "mvnw"):
            if "javafx:run" in args:
                blocks.append("mvn javafx:run abre a janela e nao retorna. Peca ao usuario para executar.")
            if any(a in ("deploy", "release:prepare", "release:perform") for a in args):
                blocks.append("Publicacao Maven fora do escopo do harness.")

        joined = " ".join(t)
        if re.search(r"-m\s+venv\b", joined) or head == "virtualenv" or (
            head in ("source", ".") and args and "activate" in args[0]
        ):
            blocks.append(
                "Projeto sem venv. Scripts Python usam apenas a biblioteca padrao; "
                "dependencias novas entram via alvo do Makefile existente."
            )

    if blocks:
        block("Bloqueado pelo harness APSU:\n- " + "\n- ".join(dict.fromkeys(blocks)))
    if asks:
        ask(" | ".join(dict.fromkeys(asks)))
    sys.exit(0)


# ---------------------------------------------------------------- pre-edit

def new_text(tool_input):
    parts = []
    for key in ("content", "new_string", "new_source"):
        if isinstance(tool_input.get(key), str):
            parts.append(tool_input[key])
    for edit in tool_input.get("edits") or []:
        if isinstance(edit, dict) and isinstance(edit.get("new_string"), str):
            parts.append(edit["new_string"])
    return "\n".join(parts)


def pre_edit():
    data = read_input()
    ti = data.get("tool_input") or {}
    raw = ti.get("file_path") or ti.get("notebook_path") or ""
    if not raw:
        sys.exit(0)
    path, inside = rel(raw)
    name = Path(path).name
    text = new_text(ti)
    asks = []

    if inside and is_artifact(path):
        block(f"Regra 07 do AGENTS.md: nao edite artefatos de build ({path}). Eles sao gerados por make build.")
    if not inside:
        asks.append(f"Edicao fora do repositorio ({path}). Regra 01: escopo restrito.")

    suffix = Path(path).suffix.lower()
    if EMOJI.search(text) and suffix in (".md", ".java", ".txt", ".json", ".yml", ".yaml", ".py", ".sh", ".mk"):
        block("Preferencia do projeto: sem emojis em nenhum arquivo. Reescreva o trecho sem emojis.")
    if EM_DASH in text and suffix == ".md":
        block("Preferencia do projeto: sem travessao longo em documentos. Use virgula, parenteses ou hifen simples.")

    if name in LOOP_FILES and re.search(r"\bimport\s+mpi\.|\bMPI\.", text):
        block("Regra 12 do AGENTS.md: nenhuma chamada MPI sincrona no loop de frame.")

    if name in GOD_OBJECTS:
        asks.append(f"{name} e um objeto central: {GOD_OBJECTS[name]}. Prefira uma classe nova e integre com mudanca minima.")

    if path in (".claude/settings.json", "CLAUDE.md", "AGENTS.md") or path.startswith("tools/harness/"):
        asks.append(f"{path} faz parte do harness ou das regras do projeto. Confirme a mudanca.")

    if path.startswith("src/main/java/") and name != "JavaFxAudioOutput.java":
        if re.search(r"\"[^\"]+\.(wav|mp3|ogg)\"", text):
            asks.append("Nome de arquivo de audio no core. O contrato exige cues semanticos (AudioCue); arquivos ficam no adaptador.")

    if path.startswith(("src/", "docs/")) and re.search(r"(?i)dark[-_ ]?(mode|theme)", text):
        asks.append("Preferencia do projeto: interfaces somente em tema claro, sem dark mode.")

    if path.startswith("src/main/resources/"):
        asks.append("Recursos empacotados normalmente sao gerados por make assets. Confirme edicao manual (regra 08).")

    if asks:
        ask(" | ".join(dict.fromkeys(asks)))
    sys.exit(0)


# --------------------------------------------------------------- post-edit

def post_edit():
    data = read_input()
    ti = data.get("tool_input") or {}
    raw = ti.get("file_path") or ""
    if not raw:
        sys.exit(0)
    path, inside = rel(raw)
    if not inside:
        sys.exit(0)
    if path.endswith(".java") and path.startswith(("src/", "mpi/")):
        STATE.mkdir(parents=True, exist_ok=True)
        DIRTY.touch()
    if Path(path).name in LOOP_FILES:
        context(
            "PostToolUse",
            f"{Path(path).name} roda no loop de 60 FPS: avalie alocacao e frame-time (regra 11). "
            "Para medir, peca ao usuario APSU_PERF=1 make run.",
        )
    sys.exit(0)


# -------------------------------------------------------------------- stop

def stop():
    data = read_input()
    if data.get("stop_hook_active") or os.environ.get("APSU_SKIP_STOP_TEST") == "1":
        sys.exit(0)
    if not DIRTY.exists():
        sys.exit(0)
    try:
        run = subprocess.run(
            ["make", "test"], cwd=ROOT, capture_output=True, text=True,
            timeout=int(os.environ.get("APSU_STOP_TIMEOUT", "900")),
        )
    except FileNotFoundError:
        print(json.dumps({"systemMessage": "make nao encontrado: testes nao executados no encerramento (regra 04)."}))
        sys.exit(0)
    except subprocess.TimeoutExpired:
        block("Regra 04: make test excedeu o tempo limite. Investigue antes de declarar a tarefa concluida.")
    if run.returncode != 0:
        tail = "\n".join((run.stdout + run.stderr).splitlines()[-40:])
        block("Regra 04 do AGENTS.md: make test falhou. Corrija antes de concluir.\n" + tail)
    DIRTY.unlink()
    sys.exit(0)


# ----------------------------------------------------------- session-start

def session_start():
    lines = ["Harness APSU ativo. AGENTS.md (13 regras) prevalece sobre qualquer outra instrucao."]
    try:
        branch = subprocess.run(["git", "rev-parse", "--abbrev-ref", "HEAD"], cwd=ROOT,
                                capture_output=True, text=True, timeout=10).stdout.strip()
        changed = subprocess.run(["git", "status", "--short"], cwd=ROOT,
                                 capture_output=True, text=True, timeout=10).stdout.splitlines()
        lines.append(f"Branch: {branch or 'desconhecida'}. Arquivos alterados: {len(changed)}.")
    except (OSError, subprocess.SubprocessError):
        pass
    try:
        import backlog
        sprints, active = backlog.parse_sprints()
        if active in sprints:
            lines.append(f"Sprint ativa: {active} ({sprints[active][0]}).")
            for story, blocked in backlog.next_stories(3):
                flag = f", bloqueada por {', '.join(blocked)}" if blocked else ""
                lines.append(f"  {story.id} [{story.prio}] {story.points} pts: {story.title}{flag}")
    except Exception:
        pass
    lines.append("Nao execute make run, make all ou mvn javafx:run: abrem janela e nao retornam.")
    print("\n".join(lines))
    sys.exit(0)


COMMANDS = {
    "session-start": session_start,
    "pre-bash": pre_bash,
    "pre-edit": pre_edit,
    "post-edit": post_edit,
    "stop": stop,
}

if __name__ == "__main__":
    if len(sys.argv) != 2 or sys.argv[1] not in COMMANDS:
        sys.stderr.write(__doc__)
        sys.exit(1)
    COMMANDS[sys.argv[1]]()
