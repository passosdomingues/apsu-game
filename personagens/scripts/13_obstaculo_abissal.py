import bpy
import math
import os

# ============================================================
# 13 - OBSTACULO ABISSAL v1.0
# Elemento de cenário para Fase 3 (Correntes Abissais)
# Salva em: blends/10_perigos_e_obstaculos_cenario/13_obstaculo_abissal.blend
#
# 2026-08-16: faltava um asset 3D para o MOVING_OBSTACLE roxo da Fase
# 3 — antes era só um retangulo com contorno brilhante no Java (o
# mesmo "efeito portal" ja corrigido na Fase 4 com 12_obstaculo_
# vulcanico.py). Este script segue o MESMO padrao/escala daquele, so
# muda o tema: em vez de rocha vulcanica com fissuras de magma, um
# crescimento coralino/rochoso CORROMPIDO por Kullullu — rocha escura
# com veias toxicas roxo-magenta, coerente com a narrativa (o veneno
# de Kullullu contaminou as correntes abissais) e visualmente distinto
# do laranja/vermelho ja usado na Fase 4 (mesmo obstaculo generico,
# fase diferente, leitura de cor diferente).
# ============================================================

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

# --- Biblioteca compartilhada (rim light) ---
try:
    _SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
except NameError:
    _SCRIPT_DIR = os.getcwd()
_LIB_PATH = os.path.join(_SCRIPT_DIR, "_apsu_shared_lib.py")
if os.path.exists(_LIB_PATH):
    exec(compile(open(_LIB_PATH, encoding="utf-8").read(), _LIB_PATH, 'exec'))
else:
    print("[APSU] AVISO: _apsu_shared_lib.py nao encontrado em " + _LIB_PATH)
    def add_fresnel_rim(mat, *a, **kw): return mat


def setup_cena(res_x=1920, res_y=1080, look='High Contrast'):
    bpy.ops.object.select_all(action='SELECT')
    bpy.ops.object.delete(use_global=False)
    for mesh in list(bpy.data.meshes): bpy.data.meshes.remove(mesh, do_unlink=True)
    for mat in list(bpy.data.materials): bpy.data.materials.remove(mat, do_unlink=True)
    scene = bpy.context.scene
    try: scene.render.engine = 'BLENDER_EEVEE_NEXT'
    except: scene.render.engine = 'BLENDER_EEVEE'
    if hasattr(scene, 'eevee') and hasattr(scene.eevee, 'taa_render_samples'):
        scene.eevee.taa_render_samples = 16
    scene.render.resolution_x = res_x
    scene.render.resolution_y = res_y
    scene.render.film_transparent = True
    scene.view_settings.view_transform = 'Filmic'
    scene.view_settings.look = look


def criar_mat(nome, cor, metallic=0.2, roughness=0.35, em=False, em_cor=None, em_str=4.0):
    mat = bpy.data.materials.new(name=nome)
    mat.use_nodes = True
    bsdf = mat.node_tree.nodes.get("Principled BSDF")
    if bsdf:
        if 'Base Color' in bsdf.inputs: bsdf.inputs['Base Color'].default_value = cor
        if 'Metallic' in bsdf.inputs: bsdf.inputs['Metallic'].default_value = metallic
        if 'Roughness' in bsdf.inputs: bsdf.inputs['Roughness'].default_value = roughness
        if em and em_cor:
            if 'Emission Color' in bsdf.inputs: bsdf.inputs['Emission Color'].default_value = em_cor
            if 'Emission Strength' in bsdf.inputs: bsdf.inputs['Emission Strength'].default_value = em_str
    return mat


def smooth(obj):
    if obj and obj.type == 'MESH':
        for p in obj.data.polygons: p.use_smooth = True


def construir_obstaculo():
    setup_cena()
    m_rocha  = criar_mat("M_RochaAbissal", (0.03, 0.02, 0.05, 1), metallic=0.15, roughness=0.75)
    m_veias  = criar_mat("M_VeiaToxica", (0.85, 0.05, 0.55, 1), em=True, em_cor=(0.85, 0.05, 0.55, 1), em_str=7.5)
    m_esporo = criar_mat("M_EsporoToxico", (0.55, 0.15, 0.90, 1), em=True, em_cor=(0.55, 0.15, 0.90, 1), em_str=5.5)

    root = bpy.data.objects.new("ObstaculoAbissal_Root", None)
    bpy.context.scene.collection.objects.link(root)

    # Massa rochosa/coralina principal — formato irregular (ico deformado),
    # nao um pilar reto: leitura de "crescimento organico", nao "pedra".
    bpy.ops.mesh.primitive_ico_sphere_add(radius=0.85, subdivisions=2, location=(0, 0, 0))
    nucleo = bpy.context.active_object
    nucleo.scale = (1.0, 0.85, 1.3)
    nucleo.data.materials.append(m_rocha)
    nucleo.parent = root
    smooth(nucleo)

    # Protuberancias assimetricas ao redor do nucleo (galhos corrompidos)
    for i in range(5):
        a = i * 1.9
        r = 0.55 + (i % 3) * 0.08
        px, py, pz = math.sin(a) * r, math.cos(a) * r * 0.7, -0.5 + i * 0.35
        bpy.ops.mesh.primitive_cone_add(vertices=6, radius1=0.22 - i*0.02, depth=0.6, location=(px, py, pz))
        galho = bpy.context.active_object
        galho.rotation_euler = (math.radians(30 + i*10), math.radians(i*40), 0)
        galho.data.materials.append(m_rocha)
        galho.parent = root
        smooth(galho)

    # Veias toxicas incandescentes trincando a superficie
    for i in range(6):
        a = i * 1.3
        r = 0.70
        bpy.ops.mesh.primitive_ico_sphere_add(radius=0.09, subdivisions=1,
                                               location=(math.sin(a)*r, math.cos(a)*r*0.75, -0.5 + i*0.32))
        veia = bpy.context.active_object
        veia.data.materials.append(m_veias)
        veia.parent = root

    # Esporos flutuantes (particulas organicas ao redor, reforcam "vivo/
    # corrompido" em vez de "pedra morta")
    for i in range(4):
        a = i * 2.3 + 0.6
        r = 1.05
        bpy.ops.mesh.primitive_uv_sphere_add(radius=0.06, location=(math.sin(a)*r, math.cos(a)*r*0.6, 0.3 + i*0.25))
        esp = bpy.context.active_object
        esp.data.materials.append(m_esporo)
        esp.parent = root
        smooth(esp)

    # Camera e luz — mesmo enquadramento do obstaculo vulcanico (consistencia
    # entre os dois, pra ficarem no mesmo "tamanho de leitura" no jogo)
    bpy.ops.object.camera_add(location=(0, -5.5, 0))
    cam = bpy.context.active_object
    cam.rotation_euler = (math.radians(90), 0, 0)
    cam.data.lens = 45
    bpy.context.scene.camera = cam

    bpy.ops.object.light_add(type='POINT', location=(0, -2.0, 0))
    luz = bpy.context.active_object
    luz.data.energy = 550; luz.data.color = (0.6, 0.2, 0.9)
    bpy.ops.object.light_add(type='AREA', location=(-1.5, -3.5, 1.5))
    luz2 = bpy.context.active_object
    luz2.data.energy = 300; luz2.data.size = 2.5; luz2.data.color = (0.3, 0.7, 1.0)

    # Rim dourado (mesma logica de sempre: complementar da paleta medida
    # do bg3 — violeta escuro, ver PHASE_PALETTES em _apsu_shared_lib.py)
    for mat in (m_rocha,):
        add_fresnel_rim(mat, get_phase_rim("bg3") if 'get_phase_rim' in dir() else (1.0, 0.8, 0.2),
                         power=1.5, strength=5.5)


def salvar(nome):
    bdir = os.path.join(BASE_DIR, "blends", "10_perigos_e_obstaculos_cenario")
    rdir = os.path.join(BASE_DIR, "renders")
    os.makedirs(bdir, exist_ok=True); os.makedirs(rdir, exist_ok=True)
    try:
        bpy.ops.wm.save_as_mainfile(filepath=os.path.join(bdir, nome+".blend"))
        print("Blend salvo: " + nome)
    except Exception as e:
        print("Aviso: " + str(e))
    bpy.context.scene.render.filepath = os.path.join(rdir, nome+".png")
    bpy.ops.render.render(write_still=True)
    print("Render salvo: " + nome)


if __name__ == "__main__":
    construir_obstaculo()
    salvar("13_obstaculo_abissal")
    print("OK 13_obstaculo_abissal v1.0")
