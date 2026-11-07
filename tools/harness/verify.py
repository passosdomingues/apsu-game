#!/usr/bin/env python3
"""Verifica a instalacao do harness APSU. Somente biblioteca padrao.

Confere arquivos, JSON, backlog e o comportamento real dos hooks com cargas de exemplo.
Sai com codigo 0 apenas se tudo passar.
"""
import json
import os
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
HOOK = ROOT / "tools" / "harness" / "hooks" / "apsu_hook.py"
failures = []


def check(name, ok, detail=""):
    print(("ok     " if ok else "FALHOU ") + name + (f" ({detail})" if detail and not ok else ""))
    if not ok:
        failures.append(name)


def run_hook(event, payload, env_extra=None):
    env = dict(os.environ, CLAUDE_PROJECT_DIR=str(ROOT))
    if env_extra:
        env.update(env_extra)
    return subprocess.run([sys.executable, str(HOOK), event], input=json.dumps(payload),
                          capture_output=True, text=True, env=env, timeout=60)


def bash(cmd):
    return run_hook("pre-bash", {"tool_name": "Bash", "tool_input": {"command": cmd}})


def edit(path, content="x", key="content"):
    return run_hook("pre-edit", {"tool_name": "Write", "tool_input": {"file_path": path, key: content}})


def is_ask(res):
    return res.returncode == 0 and '"permissionDecision": "ask"' in res.stdout


def main():
    required = [
        "CLAUDE.md", ".claude/settings.json", "tools/harness/hooks/apsu_hook.py",
        "tools/harness/backlog.py", "tools/harness/harness.mk", "AGENTS.md",
        "tools/harness/play/ai_play.py", "tools/harness/play/scenarios/smoke_p1.json",
        "docs/product/BACKLOG.md", "docs/product/SPRINTS.md", "docs/product/DEFINITION_OF_DONE.md",
    ]
    for rel in required:
        check(f"arquivo {rel}", (ROOT / rel).exists())
    for d in ("commands", "agents", "rules"):
        check(f".claude/{d} com arquivos", any((ROOT / ".claude" / d).glob("*.md")))
    check("skill apsu-user-story", (ROOT / ".claude/skills/apsu-user-story/SKILL.md").exists())

    try:
        settings = json.loads((ROOT / ".claude/settings.json").read_text(encoding="utf-8"))
        check("settings.json valido", True)
        cmds = [h["command"] for g in sum(settings["hooks"].values(), []) for h in g["hooks"]]
        check("hooks apontam para apsu_hook.py", all("apsu_hook.py" in c for c in cmds if "apsu" in c))
        check("deny bloqueia make run", "Bash(make run)" in settings["permissions"]["deny"])
    except Exception as exc:  # noqa: BLE001
        check("settings.json valido", False, str(exc))

    back = subprocess.run([sys.executable, str(ROOT / "tools/harness/backlog.py"), "lint"],
                          capture_output=True, text=True)
    helpres = subprocess.run([sys.executable, str(ROOT / "tools/harness/play/ai_play.py"), "--help"], capture_output=True, text=True)
    check("ai_play.py --help", helpres.returncode == 0)
    check("backlog lint", back.returncode == 0, back.stdout.strip()[-200:])

    makefile = ROOT / "Makefile"
    if makefile.exists():
        check("Makefile inclui harness.mk", "-include tools/harness/harness.mk" in makefile.read_text(encoding="utf-8"))

    # Comportamento dos hooks
    check("pre-bash bloqueia make run", bash("make run").returncode == 2)
    check("pre-bash bloqueia make sem alvo", bash("APSU_PERF=1 make").returncode == 2)
    check("pre-bash bloqueia mvn javafx:run", bash("mvn javafx:run").returncode == 2)
    check("pre-bash permite make test", bash("make test").returncode == 0)
    check("pre-bash bloqueia git add -A", bash("git add -A").returncode == 2)
    check("pre-bash permite git add explicito", bash("git add src/main/java/br/apsu/X.java").returncode == 0)
    check("pre-bash bloqueia add de target", bash("git add target/classes/X.class").returncode == 2)
    check("pre-bash bloqueia reset --hard", bash("git reset --hard HEAD~1").returncode == 2)
    check("pre-bash bloqueia push forcado", bash("git push --force origin main").returncode == 2)
    check("pre-bash bloqueia commit fora do padrao", bash('git commit -m "ajustes"').returncode == 2)
    check("pre-bash permite commit convencional", bash('git commit -m "fix(p2): exibir corais"').returncode == 0)
    check("pre-bash bloqueia venv", bash("python3 -m venv .venv").returncode == 2)
    check("pre-bash pergunta antes de apagar asset", is_ask(bash("rm -rf assets/characters")))
    check("pre-bash permite apagar target", bash("rm -rf target").returncode == 0)
    check("pre-bash pergunta antes de regenerar blend", is_ask(bash("make assets-regenerate")))

    check("pre-edit bloqueia artefato de build", edit("target/classes/A.class").returncode == 2)
    check("pre-edit bloqueia emoji", edit("docs/a.md", "ok \U0001F600").returncode == 2)
    check("pre-edit bloqueia travessao em md", edit("docs/a.md", "a \u2014 b").returncode == 2)
    check("pre-edit permite texto limpo", edit("docs/a.md", "texto simples").returncode == 0)
    check("pre-edit pergunta em GameContext", is_ask(edit("src/main/java/br/apsu/core/GameContext.java", "int a;")))
    check("pre-edit bloqueia MPI no loop", edit("src/main/java/br/apsu/core/GameLoop.java", "MPI.Init(args);").returncode == 2)
    check("pre-edit permite classe nova", edit("src/main/java/br/apsu/echo/EchoAbility.java", "class EchoAbility {}").returncode == 0)

    stop = run_hook("stop", {"stop_hook_active": True})
    check("stop nao entra em laco", stop.returncode == 0)
    start = run_hook("session-start", {})
    check("session-start informa sprint", start.returncode == 0 and "Sprint ativa" in start.stdout)

    mk = subprocess.run(["make", "-n", "-f", str(ROOT / "tools/harness/harness.mk"), "backlog-lint"],
                        capture_output=True, text=True) if shutil_which("make") else None
    if mk is not None:
        check("harness.mk valido para make", mk.returncode == 0, mk.stderr.strip()[-200:])

    print(f"\n{len(failures)} falha(s)")
    return 1 if failures else 0


def shutil_which(name):
    import shutil
    return shutil.which(name)


if __name__ == "__main__":
    sys.exit(main())
