import bpy
import math
import os

# ============================================================
# 01b - ADAPA: VARIACOES DE ATAQUE (thrust / slash / spin / charge)
#
# Novo em 2026-08-15 (auditoria): antes so existia UMA pose de
# ataque estatica sendo compensada por deslocamentos de camera no
# lado Java (HeroEntity: charge/impulso/firing/recuperacao mudam
# a POSICAO na tela, nunca a POSE 3D do tridente). Este script gera
# 4 variacoes reais de pose pra cada estilo de ataque, reusando o
# corpo do Adapa (mesma malha/material de 01_adapa_heroi.py) e
# animando so o tridente (tr_root) + leve squash-and-stretch no
# torso, via attack_pose_keyframes() da biblioteca compartilhada.
#
# 2026-08-16: Rafa decidiu SUBSTITUIR as 2 poses antigas de tiro de
# bolha (disparo_bolhas/atirando_bolhas — "a horizontal é a pior")
# por estes 4 estilos. Adicionadas bolhas (add_attack_bubbles) perto
# da ponta do tridente, com padrão de espalhamento diferente por
# estilo, pra não perder o residuo visual de "acabei de atirar" que
# as poses antigas tinham. Ver wiring em HeroEntity.java/RenderEngine
# (ATTACK_STYLES cicla a cada tiro em vez de alternar so 2 poses).
# ============================================================

def _find_script_dir():
    """
    Acha a pasta personagens/scripts mesmo se o Blender foi chamado com
    caminho relativo estranho ou cwd fora do projeto (foi exatamente o
    que aconteceu no primeiro teste: __file__ virou "/01_adapa_..." —
    Blender rodado com o nome nu do arquivo a partir de outra pasta).
    """
    candidatos = []
    try:
        candidatos.append(os.path.dirname(os.path.abspath(__file__)))
    except NameError:
        pass
    cwd = os.getcwd()
    candidatos.append(cwd)
    candidatos.append(os.path.join(cwd, "personagens", "scripts"))
    candidatos.append(os.path.join(cwd, "..", "personagens", "scripts"))
    candidatos.append(os.path.join(cwd, "Game", "personagens", "scripts"))
    for c in candidatos:
        if os.path.exists(os.path.join(c, "_apsu_shared_lib.py")):
            return os.path.abspath(c)
    return candidatos[0] if candidatos else os.getcwd()

_SCRIPT_DIR = _find_script_dir()
_LIB_PATH = os.path.join(_SCRIPT_DIR, "_apsu_shared_lib.py")
_HEROI_PATH = os.path.join(_SCRIPT_DIR, "01_adapa_heroi.py")

for _p in (_LIB_PATH, _HEROI_PATH):
    if not os.path.exists(_p):
        raise FileNotFoundError(
            "[APSU] Necessario para gerar variacoes de ataque: " + _p +
            " — rode com: /opt/blender-4.5.5-lts/blender --background "
            "--python personagens/scripts/01_adapa_ataque_variacoes.py "
            "(a partir da pasta Game/, igual ao 01_adapa_heroi.py)."
        )

# exec do heroi TAMBEM já executa exec do _apsu_shared_lib (o proprio
# 01_adapa_heroi.py carrega a lib) — nao precisa carregar duas vezes,
# mas nao ha problema se carregar (funcoes so ficam redefinidas).
exec(compile(open(_HEROI_PATH, encoding="utf-8").read(), _HEROI_PATH, 'exec'))


def construir_e_animar_ataque(estilo):
    """
    estilo: 'thrust' | 'slash' | 'spin' | 'charge'
    Constroi o Adapa do zero (corpo + rim light + ciclo de nado ja
    aplicados por construir_adapa()) e ENCIMA disso anima o tridente
    para o estilo de ataque pedido, nos mesmos frames 1..8 — o corpo
    continua ondulando (nado) enquanto o braço ataca, como no jogo
    real (o jogador nada e ataca ao mesmo tempo).
    """
    partes = construir_adapa()
    tr_root = partes["tr_root"]
    torso = partes["torso"]

    attack_pose_keyframes(
        weapon_root=tr_root,
        torso_obj=torso,
        style=estilo,
        frame_start=1,
        frame_count=8,
        arc_axis=1,       # Y = arco visivel no plano da tela (ver lib)
        forward_axis=0,   # X = avanco/recuo (heroi olha p/ +X)
        facing=1,
    )

    # BOLHAS (2026-08-16, pedido do Rafa): estes 4 estilos substituem as
    # 2 poses antigas de tiro de bolha — precisavam do mesmo residuo
    # visual de "acabei de atirar". Parenteadas ao tridente, entao
    # acompanham a arma pela animação (bolha "sai" durante o golpe).
    add_attack_bubbles(tr_root, tip_local=(0, 0, 2.0), style=estilo, seed=hash(estilo) % 1000)

    return partes


def salvar_render_ataque(estilo):
    base = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    nome = "01_adapa_ataque_" + estilo
    bdir = os.path.join(base, "blends", "01_adapa_heroi")
    rdir = os.path.join(base, "renders")
    os.makedirs(bdir, exist_ok=True)
    os.makedirs(rdir, exist_ok=True)
    try:
        bpy.ops.wm.save_as_mainfile(filepath=os.path.join(bdir, nome + ".blend"))
        print("Blend salvo: " + nome)
    except Exception as e:
        print("Aviso blend: " + str(e))
    bpy.context.scene.render.filepath = os.path.join(rdir, nome + ".png")
    bpy.ops.render.render(write_still=True)
    print("Render salvo: " + nome)


if __name__ == "__main__":
    for estilo in ATTACK_STYLES:  # thrust, slash, spin, charge (da lib)
        construir_e_animar_ataque(estilo)
        salvar_render_ataque(estilo)
    print("OK 01_adapa_ataque_variacoes — 4 estilos gerados: " + ", ".join(ATTACK_STYLES))
