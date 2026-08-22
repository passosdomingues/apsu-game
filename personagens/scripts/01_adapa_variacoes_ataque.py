"""Gera poses de ataque exclusivas para Abissal, Deus Dourado e Recife."""
import bpy
import os

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
VARIACOES_PATH = os.path.join(SCRIPT_DIR, "01_adapa_variacoes.py")
if not os.path.exists(VARIACOES_PATH):
    raise FileNotFoundError("01_adapa_variacoes.py não encontrado: " + VARIACOES_PATH)

# A variação já importa a biblioteca comum (nado, rim, ataque e bolhas).
# Executamos com nome isolado para não disparar seu bloco principal.
variacoes_ns = {"__file__": VARIACOES_PATH, "__name__": "apsu_variacoes_base"}
exec(compile(open(VARIACOES_PATH, encoding="utf-8").read(), VARIACOES_PATH, "exec"), variacoes_ns)
PALETAS_ADAPA = variacoes_ns["PALETAS_ADAPA"]
construir_variacao_adapa = variacoes_ns["construir_variacao_adapa"]
attack_pose_keyframes = variacoes_ns["attack_pose_keyframes"]
add_attack_bubbles = variacoes_ns["add_attack_bubbles"]
ATTACK_STYLES = variacoes_ns["ATTACK_STYLES"]


def salvar_ataque(var_id, estilo):
    partes = construir_variacao_adapa(var_id, PALETAS_ADAPA[var_id])
    attack_pose_keyframes(partes["tr_root"], partes["torso"], style=estilo,
                          frame_start=1, frame_count=8, arc_axis=1,
                          forward_axis=0, facing=1)
    add_attack_bubbles(partes["tr_root"], tip_local=(0, 0, 2.0), style=estilo,
                       seed=(hash(var_id + estilo) & 0xffff))

    base_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    nome = var_id + "_ataque_" + estilo
    blends_dir = os.path.join(base_dir, "blends", "01_adapa_heroi")
    renders_dir = os.path.join(base_dir, "renders")
    os.makedirs(blends_dir, exist_ok=True)
    os.makedirs(renders_dir, exist_ok=True)
    bpy.ops.wm.save_as_mainfile(filepath=os.path.join(blends_dir, nome + ".blend"))
    bpy.context.scene.render.filepath = os.path.join(renders_dir, nome + ".png")
    bpy.ops.render.render(write_still=True)
    print("Ataque de variante salvo: " + nome)


if __name__ == "__main__":
    for var_id in PALETAS_ADAPA:
        for estilo in ATTACK_STYLES:
            salvar_ataque(var_id, estilo)
    print("OK — 12 poses de ataque de variantes geradas")
