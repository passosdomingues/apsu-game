#!/usr/bin/env python3
"""Valida a integridade do harness: settings, hooks, comandos, agentes, skills e sprints."""
import json
import os
import re
import subprocess
import sys

HERE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
errors = []


def fail(message):
    errors.append(message)


def frontmatter(path):
    text = open(path, encoding="utf-8").read()
    match = re.match(r"^---\n(.*?)\n---\n", text, re.S)
    if not match:
        return None
    data = {}
    for line in match.group(1).splitlines():
        if ":" in line:
            key, value = line.split(":", 1)
            data[key.strip()] = value.strip()
    return data


# settings.json
settings_path = os.path.join(HERE, "settings.json")
try:
    settings = json.load(open(settings_path, encoding="utf-8"))
except ValueError as exc:
    fail("settings.json invalido: %s" % exc)
    settings = {}

known_events = {"SessionStart", "SessionEnd", "UserPromptSubmit", "PreToolUse", "PostToolUse", "Notification", "Stop", "SubagentStop", "PreCompact"}
for event, groups in (settings.get("hooks") or {}).items():
    if event not in known_events:
        fail("evento de hook desconhecido: " + event)
    for group in groups:
        for hook in group.get("hooks", []):
            command = hook.get("command", "")
            match = re.search(r"\.claude/hooks/([\w.]+)", command)
            if not match:
                fail("hook sem script em .claude/hooks: " + command)
                continue
            script = os.path.join(HERE, "hooks", match.group(1))
            if not os.path.isfile(script):
                fail("script de hook ausente: " + script)
            elif not os.access(script, os.X_OK):
                fail("script de hook sem permissao de execucao: " + script)
status = (settings.get("statusLine") or {}).get("command", "")
match = re.search(r"\.claude/hooks/([\w.]+)", status)
if match and not os.path.isfile(os.path.join(HERE, "hooks", match.group(1))):
    fail("statusLine aponta para script ausente")

permissions = settings.get("permissions", {})
for rule in permissions.get("allow", []):
    if rule in permissions.get("deny", []):
        fail("regra em allow e deny: " + rule)
for key in ("allow", "ask", "deny"):
    if key not in permissions:
        fail("permissions.%s ausente" % key)

# compilacao dos scripts python
for folder in ("hooks", "tools"):
    for name in sorted(os.listdir(os.path.join(HERE, folder))):
        if name.endswith(".py"):
            source_path = os.path.join(HERE, folder, name)
            try:
                compile(open(source_path, encoding="utf-8").read(), source_path, "exec")
            except SyntaxError as exc:
                fail("erro de sintaxe em %s/%s: %s" % (folder, name, exc))

# comandos
for name in sorted(os.listdir(os.path.join(HERE, "commands"))):
    data = frontmatter(os.path.join(HERE, "commands", name))
    if data is None or not data.get("description"):
        fail("comando sem description: " + name)

# agentes
for name in sorted(os.listdir(os.path.join(HERE, "agents"))):
    data = frontmatter(os.path.join(HERE, "agents", name))
    if data is None:
        fail("agente sem frontmatter: " + name)
        continue
    if data.get("name") != name[:-3]:
        fail("nome do agente difere do arquivo: " + name)
    if not data.get("description") or not data.get("tools"):
        fail("agente sem description ou tools: " + name)

# skills
for name in sorted(os.listdir(os.path.join(HERE, "skills"))):
    path = os.path.join(HERE, "skills", name, "SKILL.md")
    data = frontmatter(path) if os.path.isfile(path) else None
    if data is None or data.get("name") != name or not data.get("description"):
        fail("skill invalida: " + name)

# comandos citados no CLAUDE.md existem
claude_md = open(os.path.join(HERE, "CLAUDE.md"), encoding="utf-8").read()
for slash in set(re.findall(r"`/([a-z-]+)(?: [^`]*)?`", claude_md)):
    if slash not in {n[:-3] for n in os.listdir(os.path.join(HERE, "commands"))}:
        fail("CLAUDE.md cita comando inexistente: /" + slash)

# estilo e pendencias
EMOJI = re.compile("[\U0001F300-\U0001FAFF\u2600-\u27BF\u2B50\u2B55\uFE0F]")
MARKER = re.compile(r"\b(TODO|FIXME|TBD|A DEFINIR|PENDENTE)\b")
for folder, _, files in os.walk(HERE):
    if os.sep + "state" in folder or "__pycache__" in folder:
        continue
    for name in files:
        if not name.endswith((".md", ".json", ".py", ".mk", "Makefile", "Dockerfile")):
            continue
        path = os.path.join(folder, name)
        text = open(path, encoding="utf-8").read()
        shown = os.path.relpath(path, HERE)
        if EMOJI.search(text):
            fail("emoji em " + shown)
        if "\u2014" in text:
            fail("travessao longo em " + shown)
        if name.endswith((".md", ".json")) and MARKER.search(text):
            fail("marcador de pendencia em " + shown)

# sprints
env = dict(os.environ, CLAUDE_PROJECT_DIR=os.path.dirname(HERE))
result = subprocess.run([sys.executable, os.path.join(HERE, "tools", "sprint_report.py"), "--quiet"], capture_output=True, text=True, env=env)
if result.returncode != 0:
    fail("sprints invalidas:\n" + result.stdout)

if errors:
    print("Harness com %d problema(s):" % len(errors))
    for item in errors:
        print(" - " + item)
    sys.exit(1)
print("Harness valido.")
