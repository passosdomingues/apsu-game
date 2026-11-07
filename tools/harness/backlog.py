#!/usr/bin/env python3
"""Leitura, validacao e relatorio do backlog em docs/product.

Somente biblioteca padrao (sem venv, sem pip).
Uso: python3 tools/harness/backlog.py lint | report | next | show US-001
"""
import re
import sys
from dataclasses import dataclass, field
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
BACKLOG = ROOT / "docs" / "product" / "BACKLOG.md"
SPRINTS = ROOT / "docs" / "product" / "SPRINTS.md"
FIB = (1, 2, 3, 5, 8, 13)

HEAD = re.compile(
    r"^### (US-\d{3}) \| (.+?) \| (\d+) pts \| (P[0-3]) \| (S\d+) \| (E\d+) \| (todo|doing|done)\s*$"
)
SPRINT = re.compile(r"^## (S\d+) \| (.+?) \| capacidade (\d+)\s*$")
EPIC = re.compile(r"^\| (E\d+) \| (.+?) \|\s*$")
ACTIVE = re.compile(r"^Sprint ativa: (S\d+)\s*$", re.M)
AC = re.compile(r"^- \[( |x)\] (.+)$")
AC_FORM = re.compile(r"(?i)^dado .+, quando .+, ent[aã]o .+")
DEP = re.compile(r"^- Depende de: (.+)$")


@dataclass
class Story:
    id: str
    title: str
    points: int
    prio: str
    sprint: str
    epic: str
    status: str
    body: list = field(default_factory=list)

    @property
    def criteria(self):
        out = []
        for line in self.body:
            m = AC.match(line)
            if m:
                out.append((m.group(1) == "x", m.group(2)))
        return out

    @property
    def deps(self):
        for line in self.body:
            m = DEP.match(line)
            if m:
                raw = m.group(1).strip()
                return [] if raw.lower().startswith("nenhuma") else re.findall(r"US-\d{3}", raw)
        return None

    @property
    def has_story_line(self):
        text = "\n".join(self.body)
        return all(k in text for k in ("**Como**", "**quero**", "**para**"))


def parse_backlog():
    stories, epics, current = [], {}, None
    for line in BACKLOG.read_text(encoding="utf-8").splitlines():
        m = HEAD.match(line)
        if m:
            current = Story(m[1], m[2], int(m[3]), m[4], m[5], m[6], m[7])
            stories.append(current)
            continue
        if line.startswith("## "):
            current = None
        if current is not None:
            current.body.append(line)
        else:
            e = EPIC.match(line)
            if e:
                epics[e[1]] = e[2]
    return stories, epics


def parse_sprints():
    sprints, active = {}, None
    text = SPRINTS.read_text(encoding="utf-8")
    for line in text.splitlines():
        m = SPRINT.match(line)
        if m:
            sprints[m[1]] = (m[2], int(m[3]))
    m = ACTIVE.search(text)
    if m:
        active = m[1]
    return sprints, active


def num(sprint_id):
    return int(sprint_id[1:])


def lint():
    errors = []
    stories, epics = parse_backlog()
    sprints, active = parse_sprints()
    ids = {}
    for s in stories:
        if s.id in ids:
            errors.append(f"{s.id}: id duplicado")
        ids[s.id] = s
    if not stories:
        errors.append("nenhuma user story encontrada")
    if active not in sprints:
        errors.append(f"Sprint ativa invalida: {active}")
    for s in stories:
        if s.points not in FIB:
            errors.append(f"{s.id}: {s.points} pts fora de Fibonacci {FIB} (acima de 13, dividir)")
        if s.sprint not in sprints:
            errors.append(f"{s.id}: sprint {s.sprint} inexistente")
        if s.epic not in epics:
            errors.append(f"{s.id}: epico {s.epic} inexistente")
        if not s.has_story_line:
            errors.append(f"{s.id}: falta a linha **Como** ... **quero** ... **para** ...")
        crit = s.criteria
        if len(crit) < 3:
            errors.append(f"{s.id}: {len(crit)} criterios de aceitacao (minimo 3)")
        for _, text in crit:
            if not AC_FORM.match(text):
                errors.append(f"{s.id}: criterio fora do formato Dado/quando/entao: {text[:60]}")
        if s.status == "done" and not all(c for c, _ in crit):
            errors.append(f"{s.id}: status done com criterios pendentes")
        deps = s.deps
        if deps is None:
            errors.append(f"{s.id}: falta a linha '- Depende de:'")
        else:
            for d in deps:
                if d not in ids:
                    errors.append(f"{s.id}: dependencia {d} inexistente")
                elif s.sprint in sprints and ids[d].sprint in sprints and num(ids[d].sprint) > num(s.sprint):
                    errors.append(f"{s.id}: depende de {d}, que esta em sprint posterior")
    for sid, (_, cap) in sprints.items():
        total = sum(s.points for s in stories if s.sprint == sid)
        if total > cap:
            errors.append(f"{sid}: {total} pts excedem a capacidade {cap}")
    return errors


def report():
    stories, epics = parse_backlog()
    sprints, active = parse_sprints()
    print(f"Sprint ativa: {active}")
    print(f"{'Sprint':<7}{'Tema':<42}{'Cap':>4}{'Plan':>6}{'Feito':>6}{'Hist':>6}")
    for sid in sorted(sprints, key=num):
        theme, cap = sprints[sid]
        mine = [s for s in stories if s.sprint == sid]
        plan = sum(s.points for s in mine)
        done = sum(s.points for s in mine if s.status == "done")
        print(f"{sid:<7}{theme[:40]:<42}{cap:>4}{plan:>6}{done:>6}{len(mine):>6}")
    total = sum(s.points for s in stories)
    done = sum(s.points for s in stories if s.status == "done")
    print(f"Total: {total} pts em {len(stories)} stories, {done} pts concluidos")
    print("Por epico:")
    for eid, name in epics.items():
        mine = [s for s in stories if s.epic == eid]
        print(f"  {eid} {name}: {sum(s.points for s in mine)} pts, {len(mine)} stories")


def next_stories(limit=3):
    stories, _ = parse_backlog()
    _, active = parse_sprints()
    by_id = {s.id: s for s in stories}
    todo = [s for s in stories if s.sprint == active and s.status != "done"]
    todo.sort(key=lambda s: (s.prio, s.id))
    out = []
    for s in todo:
        blocked = [d for d in (s.deps or []) if by_id.get(d) and by_id[d].status != "done"]
        out.append((s, blocked))
    return out[:limit] if limit else out


def show(story_id):
    stories, _ = parse_backlog()
    for s in stories:
        if s.id == story_id:
            print(f"### {s.id} | {s.title} | {s.points} pts | {s.prio} | {s.sprint} | {s.epic} | {s.status}")
            print("\n".join(s.body).strip())
            return 0
    print(f"{story_id} nao encontrada", file=sys.stderr)
    return 1


def main(argv):
    cmd = argv[1] if len(argv) > 1 else "report"
    if cmd == "lint":
        errs = lint()
        for e in errs:
            print("ERRO", e)
        stories, _ = parse_backlog()
        print(f"backlog: {len(stories)} stories, {len(errs)} erros")
        return 1 if errs else 0
    if cmd == "report":
        report()
        return 0
    if cmd == "next":
        for s, blocked in next_stories(0):
            flag = f" (bloqueada por {', '.join(blocked)})" if blocked else ""
            print(f"{s.id} [{s.prio}] {s.points} pts {s.title}{flag}")
        return 0
    if cmd == "show" and len(argv) > 2:
        return show(argv[2])
    print(__doc__)
    return 2


if __name__ == "__main__":
    sys.exit(main(sys.argv))
