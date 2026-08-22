# -*- coding: utf-8 -*-
import os
import sys
import subprocess
import shutil

# ============================================================
# GERADOR MESTRE PROCEDURAL - AS AGUAS DE APSU
# Orquestrador de todos os scripts bpy do projeto.
# Executa a geração procedural de modelos 3D, texturas, iluminação
# e salva automaticamente os arquivos .blend e os renders .png.
# ============================================================

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SCRIPTS_DIR = os.path.join(BASE_DIR, "scripts")


def localizar_blender():
    """Resolve o Blender local sem embutir caminhos de uma máquina específica."""
    configurado = os.environ.get("BLENDER_BIN")
    if configurado:
        return configurado
    encontrado = shutil.which("blender")
    if encontrado:
        return encontrado
    for candidato in (
        "/opt/blender-4.5.5-lts/blender",
        "/usr/bin/blender",
        r"C:\\Program Files\\Blender Foundation\\Blender 4.5\\blender.exe",
    ):
        if os.path.exists(candidato):
            return candidato
    return None


BLENDER_PATH = localizar_blender()

SCRIPTS = [
    "01_adapa_heroi.py",
    "01_adapa_nadando_disparo_bolhas.py",
    "01_adapa_variacoes.py",
    "01_adapa_ataque_variacoes.py",
    "01_adapa_variacoes_ataque.py",
    "02_kullullu_boss.py",
    "02_kullullu_boss_variacoes.py",
    "03_enki_npc.py",
    "03_enki_npc_variacoes.py",
    "04_peixe_sombrio_inimigo.py",
    "04_inimigos_variacoes.py",
    "05_guardiao_atlante_npc.py",
    "05_guardioes_variacoes.py",
    "06_bau_tesouro_elemento-cenario.py",
    "07_navio_naufragado_elemento-cenario.py",
    "08_recifes_e_cardume_elemento-cenario.py",
    "09_ruinas_e_colunas_atlantis.py",
    "10_perigos_e_obstaculos_cenario.py",
    "11_arraiao_abissal.py",
    "12_obstaculo_vulcanico.py",
    "13_obstaculo_abissal.py",
    "14_leviata_menor.py",
    "15_portal_atlantica_3d.py",
    "16_corrente_abissal_3d.py"
]

def executar_script(script_name):
    if not BLENDER_PATH:
        print("[ERRO] Blender não encontrado. Defina BLENDER_BIN ou instale o executável.")
        return False
    script_path = os.path.join(SCRIPTS_DIR, script_name)
    if not os.path.exists(script_path):
        print("[ERRO] Arquivo não encontrado: " + script_path)
        return False

    print("\n=======================================================")
    print("[RENDER] Executando: " + script_name + "...")
    print("=======================================================")

    cmd = [BLENDER_PATH, "--background", "--python", script_path]
    res = subprocess.run(cmd, capture_output=True, text=True, encoding="utf-8", errors="replace")
    if res.returncode == 0:
        print("[OK] " + script_name + " finalizado com sucesso!")
        return True
    else:
        print("[FALHA] Erro ao executar " + script_name)
        stderr_lines = (res.stderr or "").strip().splitlines()
        stdout_lines = (res.stdout or "").strip().splitlines()
        for line in stderr_lines[-30:]:
            print("  STDERR:", line)
        for line in stdout_lines[-10:]:
            print("  STDOUT:", line)
        return False

def gerar_tudo():
    print("Iniciando Geração Completa de Assets 3D de 'As Águas de Apsu'...")
    sucessos = 0
    falhas = []
    for s in SCRIPTS:
        if executar_script(s):
            sucessos += 1
        else:
            falhas.append(s)
    print("\n=======================================================")
    print("Processo Concluído: {}/{} scripts executados com sucesso!".format(sucessos, len(SCRIPTS)))
    if falhas:
        print("Scripts com falha:")
        for f in falhas:
            print("  - " + f)
    print("=======================================================")

if __name__ == "__main__":
    # Filtra argumentos do Blender (como --background, --python, etc)
    user_args = [a for a in sys.argv[1:] if not a.startswith("-") and not a.endswith(".py")]
    if user_args:
        alvo = user_args[0]
        executar_script(alvo)
    else:
        gerar_tudo()
