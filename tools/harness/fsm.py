#!/usr/bin/env python3
"""Maquina de Estados Finitos (FSM) Deterministica do Harness APSU.

Implementa a filosofia: "Letting the model mediate instead of generate".
O loop central e um automato deterministico executando atomos verificaveis
(build, test, aiplay, lint, verify).

O modelo de IA entra em apenas um ponto: quando um atomo falha, solicitando
um patch minimo de reparo. Se o verificador protegido falhar, o patch e revertido
imediatamente via git.

Funcao de Lyapunov:
  L = (Criterios de Aceitacao Pendentes) + (Orcamento Restante de Reparos)
  L e estritamente decrescente a cada ciclo. Se o orcamento esgotar sem satisfazer
  todos os criterios, a FSM forca o desdobramento (split) da User Story.
"""
import argparse
import hashlib
import json
import os
import subprocess
import sys
import time
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
STATE_DIR = ROOT / ".claude" / "state"
TELEMETRY_FILE = STATE_DIR / "fsm_telemetry.json"
CHECKSUM_FILE = ROOT / "tools" / "harness" / "core_checksums.sha256"


class AtomResult:
    def __init__(self, name, success, returncode, stdout="", stderr="", duration=0.0):
        self.name = name
        self.success = success
        self.returncode = returncode
        self.stdout = stdout
        self.stderr = stderr
        self.duration = duration


def log(msg):
    timestamp = time.strftime("%Y-%m-%d %H:%M:%S")
    print(f"[FSM {timestamp}] {msg}", flush=True)


def load_telemetry():
    if TELEMETRY_FILE.exists():
        try:
            return json.loads(TELEMETRY_FILE.read_text(encoding="utf-8"))
        except Exception:
            pass
    return {
        "total_transitions": 0,
        "deterministic_transitions": 0,
        "model_repairs_requested": 0,
        "model_repairs_accepted": 0,
        "model_repairs_rejected": 0,
        "sessions": []
    }


def save_telemetry(data):
    STATE_DIR.mkdir(parents=True, exist_ok=True)
    data["fraction_deterministic"] = (
        data["deterministic_transitions"] / max(1, data["total_transitions"])
    )
    TELEMETRY_FILE.write_text(json.dumps(data, indent=2), encoding="utf-8")


def verify_core_checksums():
    """Verifica se os genes e verificador protegido nao foram adulterados."""
    if not CHECKSUM_FILE.exists():
        return True, "Arquivo de checksums nao encontrado (modo permissivo inicial)."
    
    lines = CHECKSUM_FILE.read_text(encoding="utf-8").splitlines()
    for line in lines:
        line = line.strip()
        if not line or line.startswith("#"):
            continue
        parts = line.split(maxsplit=1)
        if len(parts) != 2:
            continue
        expected_sha, rel_path = parts[0], parts[1].strip()
        target_path = ROOT / rel_path
        if not target_path.exists():
            return False, f"Arquivo protegido ausente: {rel_path}"
        actual_sha = hashlib.sha256(target_path.read_bytes()).hexdigest()
        if actual_sha != expected_sha:
            return False, f"Adulteracao detectada no nucleo protegido: {rel_path}"
    return True, "Nucleo protegido integro e verificado."


def update_core_checksums():
    """Atualiza a base de checksums do nucleo (executado por humano ou instalador)."""
    files_to_protect = [
        "AGENTS.md",
        "tools/harness/verify.py",
        "tools/harness/hooks/apsu_hook.py",
        "tools/harness/fsm.py",
    ]
    lines = ["# SHA-256 do nucleo protegido do harness APSU"]
    for rel_path in files_to_protect:
        p = ROOT / rel_path
        if p.exists():
            sha = hashlib.sha256(p.read_bytes()).hexdigest()
            lines.append(f"{sha}  {rel_path}")
    CHECKSUM_FILE.write_text("\n".join(lines) + "\n", encoding="utf-8")
    log(f"Checksums protegidos atualizados em {CHECKSUM_FILE.relative_to(ROOT)}")


def run_command(cmd, timeout=300):
    start = time.time()
    try:
        proc = subprocess.run(
            cmd,
            shell=True,
            cwd=ROOT,
            capture_output=True,
            text=True,
            timeout=timeout,
        )
        duration = time.time() - start
        return AtomResult(
            name=cmd,
            success=(proc.returncode == 0),
            returncode=proc.returncode,
            stdout=proc.stdout,
            stderr=proc.stderr,
            duration=duration,
        )
    except subprocess.TimeoutExpired:
        return AtomResult(
            name=cmd,
            success=False,
            returncode=-1,
            stdout="",
            stderr=f"Timeout apos {timeout} segundos",
            duration=timeout,
        )


class HarnessFSM:
    """Automato Finito de execucao deterministica para User Stories."""

    def __init__(self, story_id):
        self.story_id = story_id
        self.story = None
        self.budget = 0
        self.repairs_done = 0
        self.pending_criteria = 0
        self.telemetry = load_telemetry()
        self.session_record = {
            "story_id": story_id,
            "start_time": time.strftime("%Y-%m-%dT%H:%M:%SZ"),
            "transitions": [],
            "status": "RUNNING"
        }

    def transition(self, from_state, to_state, is_deterministic=True):
        self.telemetry["total_transitions"] += 1
        if is_deterministic:
            self.telemetry["deterministic_transitions"] += 1
        record = {
            "from": from_state,
            "to": to_state,
            "deterministic": is_deterministic,
            "timestamp": time.time(),
        }
        self.session_record["transitions"].append(record)
        log(f"Transicao [{from_state}] -> [{to_state}] (Deterministica={is_deterministic})")
        save_telemetry(self.telemetry)

    def atom_build(self):
        log("Atomo 1/5: Build do projeto...")
        return run_command("mvn compile -q")

    def atom_test(self):
        log("Atomo 2/5: Suite de testes unitarios e integracao...")
        return run_command("mvn test")

    def atom_backlog_lint(self):
        log("Atomo 3/5: Lint do backlog e criterios...")
        return run_command("python3 tools/harness/backlog.py lint")

    def atom_harness_verify(self):
        log("Atomo 4/5: Verificacao do harness e hooks...")
        ok, msg = verify_core_checksums()
        if not ok:
            return AtomResult("core_checksum_verify", False, 1, stderr=msg)
        return run_command("python3 tools/harness/verify.py")

    def rollback_patch(self):
        log("REVERSAO: Revertendo patch nao validado via git restore...")
        subprocess.run(["git", "restore", "src/", "docs/"], cwd=ROOT, check=False)
        self.telemetry["model_repairs_rejected"] += 1
        save_telemetry(self.telemetry)

    def load_story(self):
        from backlog import parse_backlog
        stories, _ = parse_backlog()
        for s in stories:
            if s.id == self.story_id:
                self.story = s
                break
        if not self.story:
            raise ValueError(f"Story {self.story_id} nao encontrada no backlog.")
        
        # Orcamento calibrado pelos pontos Fibonacci da story (ex: 2 pts = 3 tentativas max)
        self.budget = max(2, int(self.story.points * 1.5))
        self.pending_criteria = sum(1 for checked, _ in self.story.criteria if not checked)
        log(f"Story {self.story.id} ({self.story.points} pts): {self.pending_criteria} criterios pendentes. Orcamento de reparo: {self.budget} iteracoes.")

    def run_cycle(self):
        self.load_story()
        state = "ST_INIT"
        
        while True:
            # Funcao de Lyapunov L = pending_criteria + remaining_budget
            remaining_budget = max(0, self.budget - self.repairs_done)
            lyapunov_L = self.pending_criteria + remaining_budget
            log(f"Estado: {state} | Medida Lyapunov L = {lyapunov_L} (Criterios={self.pending_criteria}, BudgetRestante={remaining_budget})")

            if state == "ST_INIT":
                self.transition("ST_INIT", "ST_CHECK_CORE", is_deterministic=True)
                state = "ST_CHECK_CORE"

            elif state == "ST_CHECK_CORE":
                ok, msg = verify_core_checksums()
                if not ok:
                    log(f"FALHA CRITICA: {msg}")
                    self.session_record["status"] = "BLOCKED_CORE_TAMPERED"
                    save_telemetry(self.telemetry)
                    return 1
                self.transition("ST_CHECK_CORE", "ST_EXEC_ATOMS", is_deterministic=True)
                state = "ST_EXEC_ATOMS"

            elif state == "ST_EXEC_ATOMS":
                # Executa sequencia de atomos deterministas
                atoms = [
                    ("build", self.atom_build),
                    ("test", self.atom_test),
                    ("backlog_lint", self.atom_backlog_lint),
                    ("harness_verify", self.atom_harness_verify),
                ]
                failed_atom = None
                for atom_name, atom_fn in atoms:
                    res = atom_fn()
                    if not res.success:
                        failed_atom = (atom_name, res)
                        break

                if failed_atom is None:
                    # Todos atomos passaram
                    if self.pending_criteria == 0:
                        self.transition("ST_EXEC_ATOMS", "ST_SUCCESS", is_deterministic=True)
                        state = "ST_SUCCESS"
                    else:
                        self.transition("ST_EXEC_ATOMS", "ST_AWAIT_CRITERIA_IMPLEMENTATION", is_deterministic=False)
                        state = "ST_AWAIT_CRITERIA_IMPLEMENTATION"
                else:
                    atom_name, res = failed_atom
                    log(f"Atomo '{atom_name}' falhou (code {res.returncode}).")
                    if remaining_budget <= 0:
                        self.transition("ST_EXEC_ATOMS", "ST_BUDGET_EXHAUSTED", is_deterministic=True)
                        state = "ST_BUDGET_EXHAUSTED"
                    else:
                        self.transition("ST_EXEC_ATOMS", "ST_MODEL_REPAIR_NEEDED", is_deterministic=False)
                        state = "ST_MODEL_REPAIR_NEEDED"
                        self.last_failure = (atom_name, res)

            elif state == "ST_MODEL_REPAIR_NEEDED":
                atom_name, res = self.last_failure
                self.repairs_done += 1
                self.telemetry["model_repairs_requested"] += 1
                save_telemetry(self.telemetry)
                log(f"SOLICITANDO REPARO AO MODELO (Tentativa {self.repairs_done}/{self.budget}).")
                log(f"Falha em {atom_name}:\n{res.stderr or res.stdout}")
                
                # Gera arquivo descritor de reparo para o agente
                repair_prompt_file = STATE_DIR / "current_repair_request.json"
                repair_data = {
                    "story_id": self.story_id,
                    "failed_atom": atom_name,
                    "returncode": res.returncode,
                    "error_output": (res.stderr or res.stdout)[-2000:],
                    "budget_remaining": self.budget - self.repairs_done,
                    "instruction": "Gere apenas o patch minimo em src/ ou docs/ para corrigir a falha. Nao altere o verificador."
                }
                repair_prompt_file.write_text(json.dumps(repair_data, indent=2), encoding="utf-8")
                
                # Neste ponto a FSM entrega a solicitacao de reparo mediada
                print("\n=======================================================")
                print(f" [FSM] ACAO REQUERIDA DO AGENTE / MODELO:")
                print(f" Atomo com falha: {atom_name}")
                print(f" Detalhes gravados em: {repair_prompt_file.relative_to(ROOT)}")
                print(f" Execute a correcao e rode 'make fsm-verify' para testar o patch.")
                print("=======================================================\n")
                return 2

            elif state == "ST_AWAIT_CRITERIA_IMPLEMENTATION":
                print("\n=======================================================")
                print(f" [FSM] TODOS OS ATOMOS BASE ESTAO VERDES.")
                print(f" Criterios pendentes para {self.story_id}: {self.pending_criteria}")
                print(f" Implemente os criterios pendentes e marque [x] em BACKLOG.md.")
                print(f" Em seguida execute 'make fsm-cycle STORY={self.story_id}'.")
                print("=======================================================\n")
                return 0

            elif state == "ST_BUDGET_EXHAUSTED":
                log(f"ORCAMENTO ESGOTADO para a Story {self.story_id}.")
                log("Teorema de Lyapunov: L nao convergiu para 0 dentro do limite calibrado.")
                log("Recomendacao formal: SPLIT da User Story em subtarefas menores.")
                self.rollback_patch()
                self.session_record["status"] = "SPLIT_RECOMMENDED"
                save_telemetry(self.telemetry)
                return 3

            elif state == "ST_SUCCESS":
                log(f"SUCESSO FORMAL: Todos os atomos e criterios de {self.story_id} foram verificados e aprovados!")
                self.session_record["status"] = "COMPLETED"
                self.telemetry["sessions"].append(self.session_record)
                save_telemetry(self.telemetry)
                fraction = self.telemetry.get("fraction_deterministic", 0.0)
                log(f"Telemetria Global: {self.telemetry['deterministic_transitions']}/{self.telemetry['total_transitions']} transicoes deterministicas ({fraction:.1%}).")
                return 0


def main():
    parser = argparse.ArgumentParser(description="Harness FSM Deterministico APSU")
    subparsers = parser.add_subparsers(dest="subcommand")

    cycle_parser = subparsers.add_parser("cycle", help="Executa um ciclo da FSM para uma story")
    cycle_parser.add_argument("story_id", help="Ex: US-014 ou US-025")

    subparsers.add_parser("verify-patch", help="Valida patch proposto pelo modelo; reverte se falhar")
    subparsers.add_parser("checksums", help="Atualiza checksums protegidos do nucleo")
    subparsers.add_parser("telemetry", help="Exibe metricas de determinismo do harness")

    args = parser.parse_args()

    # Adiciona tools/harness ao path para importar backlog se necessario
    sys.path.insert(0, str(ROOT / "tools" / "harness"))

    if args.subcommand == "checksums":
        update_core_checksums()
        return 0
    elif args.subcommand == "telemetry":
        t = load_telemetry()
        print(json.dumps(t, indent=2))
        return 0
    elif args.subcommand == "verify-patch":
        log("Validando patch proposto...")
        ok, msg = verify_core_checksums()
        if not ok:
            log(f"FALHA NO PATCH: {msg}")
            log("Revertendo...")
            subprocess.run(["git", "restore", "."], cwd=ROOT, check=False)
            return 1
        test_res = run_command("mvn test")
        if not test_res.success:
            log("FALHA: Testes falharam apos o patch. Revertendo...")
            subprocess.run(["git", "restore", "src/", "docs/"], cwd=ROOT, check=False)
            return 1
        log("Patch aprovado pelo verificador protegido!")
        return 0
    elif args.subcommand == "cycle":
        fsm = HarnessFSM(args.story_id)
        return fsm.run_cycle()
    else:
        parser.print_help()
        return 0


if __name__ == "__main__":
    sys.exit(main())
