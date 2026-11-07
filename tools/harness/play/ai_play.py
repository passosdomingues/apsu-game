#!/usr/bin/env python3
"""Joga o jogo em um display virtual, tira capturas e resume o desempenho.

Somente biblioteca padrao (sem venv). Requer no sistema: java, Xvfb, xdotool e
ImageMagick (comando import). Em Debian ou Ubuntu:
  sudo apt install xvfb xdotool imagemagick mesa-utils libgl1-mesa-dri

Uso:
  python3 tools/harness/play/ai_play.py --label es2 --prism es2
  python3 tools/harness/play/ai_play.py --label sw --prism sw --assert-3d

Saida em target/ai-play/<label>/: capturas .png, stdout.log, stderr.log,
summary.json e report.md. O script nao altera o projeto.
"""
import argparse
import json
import os
import re
import shutil
import signal
import statistics
import subprocess
import sys
import time
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
PERF = re.compile(
    r"\[PERF\] (\w+) P(\d) \| ([\d.]+) fps \| pulse p50/p95/max ([\d.]+)/([\d.]+)/([\d.]+) ms"
)


def need(tools):
    missing = [t for t in tools if shutil.which(t) is None]
    if missing:
        sys.exit("Faltam ferramentas: " + ", ".join(missing) +
                 "\nDebian ou Ubuntu: sudo apt install xvfb xdotool imagemagick mesa-utils libgl1-mesa-dri")


def classpath(arg):
    if arg:
        return arg
    jars = sorted((ROOT / "target").glob("*jar-with-dependencies.jar"))
    if jars:
        return str(jars[-1])
    classes = ROOT / "target" / "classes"
    deps = ROOT / "target" / "dependency"
    if classes.exists() and deps.exists():
        return os.pathsep.join([str(classes)] + [str(j) for j in sorted(deps.glob("*.jar"))])
    sys.exit("Build nao encontrado. Rode make package (gera o jar com dependencias) ou passe --classpath.")


def start_xvfb():
    for n in range(90, 120):
        if not Path(f"/tmp/.X11-unix/X{n}").exists():
            proc = subprocess.Popen(
                ["Xvfb", f":{n}", "-screen", "0", "1366x768x24", "+extension", "GLX", "+render", "-noreset"],
                stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
            time.sleep(2)
            return proc, f":{n}"
    sys.exit("Nenhum display livre entre :90 e :119.")


def run_steps(steps, display, out):
    env = dict(os.environ, DISPLAY=display)

    def sh(*cmd):
        subprocess.run(cmd, env=env, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, timeout=30)

    for step in steps:
        action = step["do"]
        if action == "wait":
            time.sleep(step["s"])
        elif action == "click":
            sh("xdotool", "mousemove", "683", "400", "click", "1")
            time.sleep(1)
        elif action == "key":
            sh("xdotool", "key", step["k"])
        elif action == "hold":
            sh("xdotool", "keydown", step["k"])
            time.sleep(step["s"])
            sh("xdotool", "keyup", step["k"])
        elif action == "shot":
            sh("import", "-window", "root", str(out / (step["nome"] + ".png")))
        else:
            sys.exit(f"Passo desconhecido: {action}")


def summarize(out):
    stdout = (out / "stdout.log").read_text(errors="replace") if (out / "stdout.log").exists() else ""
    stderr = (out / "stderr.log").read_text(errors="replace") if (out / "stderr.log").exists() else ""
    pipeline = re.findall(r"Prism pipeline name = (\S+)", stdout)
    renderer = re.findall(r"^\s+Renderer: (.+)$", stdout, re.M)
    perf = {}
    for m in PERF.finditer(stdout):
        perf.setdefault(f"{m[1]} P{m[2]}", []).append(tuple(float(x) for x in m.groups()[2:]))
    states = {}
    for key, rows in perf.items():
        rows = rows[1:] or rows  # descarta a primeira janela, que inclui o aquecimento
        states[key] = {
            "janelas": len(rows),
            "fps_mediana": round(statistics.median(r[0] for r in rows), 1),
            "pulse_p50_ms": round(statistics.median(r[1] for r in rows), 1),
            "pulse_p95_ms_pior": round(max(r[2] for r in rows), 1),
            "pulse_max_ms": round(max(r[3] for r in rows), 1),
        }
    return {
        "pipeline": pipeline[-1] if pipeline else "desconhecido",
        "renderer": renderer[-1].strip() if renderer else "desconhecido",
        "scene3d_avisos": stderr.count("can't support ConditionalFeature.SCENE3D"),
        "erros_3d": [l for l in stderr.splitlines() + stdout.splitlines() if l.startswith("[3D]")][:10],
        "excecoes": [l for l in stderr.splitlines() if "Exception" in l][:5],
        "estados": states,
    }


def write_report(out, label, args, summary):
    lines = [f"# Relatorio de jogo automatico: {label}", "",
             f"- Pipeline Prism: {summary['pipeline']}",
             f"- Renderizador GL: {summary['renderer']}",
             f"- Avisos SCENE3D: {summary['scene3d_avisos']}",
             f"- Opcoes: prism={args.prism}, quality={args.quality}, cenario={args.scenario}", ""]
    if summary["scene3d_avisos"]:
        lines.append("Resultado: o 3D esta indisponivel neste pipeline. Personagens em malha 3D nao sao desenhados.")
    if summary["erros_3d"]:
        lines += ["", "Erros de carga 3D:"] + [f"- {e}" for e in summary["erros_3d"]]
    lines += ["", "| Estado | Janelas | FPS (mediana) | Pulse p50 (ms) | Pulse p95 (pior, ms) | Pulse max (ms) |",
              "|---|---|---|---|---|---|"]
    for key, s in summary["estados"].items():
        lines.append(f"| {key} | {s['janelas']} | {s['fps_mediana']} | {s['pulse_p50_ms']} | "
                     f"{s['pulse_p95_ms_pior']} | {s['pulse_max_ms']} |")
    lines += ["", "Orcamento do projeto: pulse p95 de 16,7 ms e p99 de 25 ms no perfil balanced.",
              "Capturas de tela na mesma pasta; abra-as para conferir o que foi desenhado."]
    (out / "report.md").write_text("\n".join(lines) + "\n", encoding="utf-8")


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--label", default="jogo")
    ap.add_argument("--scenario", default="smoke_p1")
    ap.add_argument("--prism", default="auto", help="auto, es2, sw ou d3d")
    ap.add_argument("--quality", default="low", help="low, balanced ou high")
    ap.add_argument("--classpath")
    ap.add_argument("--java-args", default="", help="argumentos extras da JVM, por exemplo --module-path ...")
    ap.add_argument("--main", default="br.apsu.Launcher")
    ap.add_argument("--assert-3d", action="store_true", help="sai com codigo 1 se o 3D estiver indisponivel")
    args = ap.parse_args()

    need(["java", "Xvfb", "xdotool", "import"])
    scenario_path = Path(args.scenario)
    if not scenario_path.exists():
        scenario_path = Path(__file__).parent / "scenarios" / (args.scenario + ".json")
    steps = json.loads(scenario_path.read_text(encoding="utf-8"))["passos"]
    out = ROOT / "target" / "ai-play" / args.label
    shutil.rmtree(out, ignore_errors=True)
    out.mkdir(parents=True)

    xvfb, display = start_xvfb()
    jvm = ["java"] + args.java_args.split() + [
        "-Dapsu.perf=true", "-Dapsu.audio.enabled=false", "-Dprism.verbose=true",
        f"-Dapsu.quality={args.quality}", "-cp", classpath(args.classpath)]
    if args.prism != "auto":
        jvm.append(f"-Dprism.order={args.prism}")
    if args.prism in ("auto", "es2"):
        jvm.append("-Dprism.forceGPU=true")  # o Mesa em software falha na checagem de GPU do JavaFX
    jvm.append(args.main)
    game = subprocess.Popen(jvm, cwd=ROOT, env=dict(os.environ, DISPLAY=display),
                            stdout=(out / "stdout.log").open("w"), stderr=(out / "stderr.log").open("w"))
    try:
        run_steps(steps, display, out)
    finally:
        game.send_signal(signal.SIGTERM)
        try:
            game.wait(timeout=10)
        except subprocess.TimeoutExpired:
            game.kill()
        xvfb.terminate()

    summary = summarize(out)
    (out / "summary.json").write_text(json.dumps(summary, indent=2, ensure_ascii=False), encoding="utf-8")
    write_report(out, args.label, args, summary)
    print((out / "report.md").read_text(encoding="utf-8"))
    return 1 if args.assert_3d and summary["scene3d_avisos"] else 0


if __name__ == "__main__":
    sys.exit(main())
