#!/usr/bin/env python3
"""Le um log com linhas [PERF] do GameLoop e compara com o orcamento do CLAUDE.md.

Uso: python3 .claude/tools/perf_gate.py <log> [--warmup N] [--json] [--save]
O log pode ser a saida de `APSU_PERF=1 make run` redirecionada com tee.
"""
import argparse
import json
import os
import re
import statistics
import sys

LINE = re.compile(
    r"\[PERF\] (?P<state>\w+) P(?P<phase>\d+) \| (?P<fps>[\d.]+) fps \| "
    r"pulse p50/p95/max (?P<p50>[\d.]+)/(?P<p95>[\d.]+)/(?P<max>[\d.]+) ms \| "
    r">16ms (?P<o16>\d+) >33ms (?P<o33>\d+) \| update (?P<update>[\d.]+) ms \| "
    r"render (?P<render>[\d.]+) ms \| (?P<sim>[\d.]+) sim/frame \| "
    r"GC (?P<gcn>\d+)/(?P<gct>\d+)ms \| heap (?P<heap>\d+)/(?P<maxheap>\d+) MiB"
)
OBJ = re.compile(r"\[PERF\] OBJ async (?P<name>.+?): (?P<ms>[\d.]+) ms")

BUDGET = {
    "fps_mean_min": 55.0,
    "p95_max": 22.0,
    "over33_per_minute_max": 3,
    "update_mean_max": 4.0,
    "render_mean_max": 12.0,
    "gc_per_minute_max": 6,
    "sim_spread_max": 0.35,
}
TARGET = {"fps_mean": 60.0, "p95": 18.0, "update": 2.0, "render": 9.0}


def parse(path):
    samples, objs = [], []
    with open(path, encoding="utf-8", errors="replace") as handle:
        for text in handle:
            match = LINE.search(text)
            if match:
                samples.append({k: (float(v) if re.fullmatch(r"[\d.]+", v) else v) for k, v in match.groupdict().items()})
                continue
            match = OBJ.search(text)
            if match:
                objs.append((match.group("name"), float(match.group("ms"))))
    return samples, objs


def evaluate(samples, warmup):
    body = [s for s in samples if s["state"].startswith("P")][warmup:]
    if len(body) < 10:
        return None, ["amostras insuficientes: %d (minimo 10 depois do aquecimento de %d)" % (len(body), warmup)]
    minutes = len(body) / 60.0
    metrics = {
        "amostras": len(body),
        "fps_mean": statistics.fmean(s["fps"] for s in body),
        "fps_min": min(s["fps"] for s in body),
        "p95_worst": max(s["p95"] for s in body),
        "p95_mean": statistics.fmean(s["p95"] for s in body),
        "frame_max": max(s["max"] for s in body),
        "over33_per_minute": sum(s["o33"] for s in body) / minutes,
        "update_mean": statistics.fmean(s["update"] for s in body),
        "render_mean": statistics.fmean(s["render"] for s in body),
        "gc_per_minute": sum(s["gcn"] for s in body) / minutes,
        "sim_mean": statistics.fmean(s["sim"] for s in body),
        "sim_spread": max(s["sim"] for s in body) - min(s["sim"] for s in body),
    }
    failures = []
    if metrics["fps_mean"] < BUDGET["fps_mean_min"]:
        failures.append("FPS medio %.1f abaixo de %.0f" % (metrics["fps_mean"], BUDGET["fps_mean_min"]))
    if metrics["p95_worst"] > BUDGET["p95_max"]:
        failures.append("p95 do frame %.1f ms acima de %.0f ms" % (metrics["p95_worst"], BUDGET["p95_max"]))
    if metrics["over33_per_minute"] > BUDGET["over33_per_minute_max"]:
        failures.append("%.1f frames acima de 33 ms por minuto (maximo %d)" % (metrics["over33_per_minute"], BUDGET["over33_per_minute_max"]))
    if metrics["update_mean"] > BUDGET["update_mean_max"]:
        failures.append("update medio %.2f ms acima de %.0f ms" % (metrics["update_mean"], BUDGET["update_mean_max"]))
    if metrics["render_mean"] > BUDGET["render_mean_max"]:
        failures.append("render medio %.2f ms acima de %.0f ms" % (metrics["render_mean"], BUDGET["render_mean_max"]))
    if metrics["gc_per_minute"] > BUDGET["gc_per_minute_max"]:
        failures.append("%.1f coletas de GC por minuto (maximo %d)" % (metrics["gc_per_minute"], BUDGET["gc_per_minute_max"]))
    if metrics["sim_spread"] > BUDGET["sim_spread_max"]:
        failures.append("passos de simulacao por frame variam %.2f (judder; maximo %.2f)" % (metrics["sim_spread"], BUDGET["sim_spread_max"]))
    return metrics, failures


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("log")
    parser.add_argument("--warmup", type=int, default=10)
    parser.add_argument("--json", action="store_true")
    parser.add_argument("--save", action="store_true", help="grava o resultado em .claude/state/last-perf-gate")
    args = parser.parse_args()

    samples, objs = parse(args.log)
    metrics, failures = evaluate(samples, args.warmup)
    if metrics is None:
        print("REPROVADO: " + failures[0])
        return 2
    if any(ms > 50 for _, ms in objs):
        slow = ", ".join("%s %.0f ms" % item for item in objs if item[1] > 50)
        print("aviso: carga de modelo lenta (verifique se ocorre fora do thread FX): " + slow)
    verdict = "APROVADO" if not failures else "REPROVADO"
    if args.json:
        print(json.dumps({"verdict": verdict, "metrics": metrics, "failures": failures}, indent=2))
    else:
        print("%s | fps %.1f (min %.1f) | p95 %.1f ms | >33ms/min %.1f | update %.2f ms | render %.2f ms | GC/min %.1f | sim/frame %.2f (+-%.2f)" % (
            verdict, metrics["fps_mean"], metrics["fps_min"], metrics["p95_worst"], metrics["over33_per_minute"],
            metrics["update_mean"], metrics["render_mean"], metrics["gc_per_minute"], metrics["sim_mean"], metrics["sim_spread"]))
        for failure in failures:
            print("  - " + failure)
        print("Metas: fps %.0f, p95 %.0f ms, update %.0f ms, render %.0f ms." % (TARGET["fps_mean"], TARGET["p95"], TARGET["update"], TARGET["render"]))
    if args.save:
        root = os.environ.get("CLAUDE_PROJECT_DIR") or os.getcwd()
        os.makedirs(os.path.join(root, ".claude", "state"), exist_ok=True)
        with open(os.path.join(root, ".claude", "state", "last-perf-gate"), "w", encoding="utf-8") as handle:
            handle.write("%s fps %.1f p95 %.1f" % (verdict, metrics["fps_mean"], metrics["p95_worst"]))
    return 0 if not failures else 1


if __name__ == "__main__":
    sys.exit(main())
