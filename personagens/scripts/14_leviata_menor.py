import bpy
import math
import os

# ============================================================
# 14 - LEVIATÃ MENOR v1.0
# Sub-boss da Fase 4 (Abismo Vulcânico) — move rápido em diagonal
# Salva em: blends/04_peixe_sombrio_inimigo/12_leviata_menor.blend
# (nome de SAÍDA "12_leviata_menor" bate com
# EnemyType.LEVIATA em Java — o número do ARQUIVO deste script
# é 14 só pra não colidir com 12_obstaculo_vulcanico.py na lista
# do gerador mestre, mas o nome do PNG/blend gerado é o que importa)
#
# 2026-08-16: achado em auditoria sistemática (comparei todo caminho
# sprites/ referenciado no Java contra os scripts existentes) — este
# inimigo tem tipo, tamanho, dano e posição na Fase 4 desde sempre,
# mas nunca teve NENHUM asset 3D. No jogo, sem sprite carregado, cai
# no fallback geométrico (retângulo roxo simples) — o "sub-boss" da
# fase mais visualmente exigente do jogo era, na prática, um
# placeholder invisível.
#
# Design: serpente marinha grande (maior que a enguia, corpo mais
# grosso, cabeça com mandíbula/dentes visíveis, crista dorsal de
# espinhos). Basalto escuro com veias de magma (mesma linguagem do
# boss Kullullû e do obstáculo vulcânico) — encaixa como "predador
# nativo do abismo vulcânico", visualmente distinto da enguia
# (ciano/elétrica) e do próprio Kullullû (obsidiana+magenta).
# ============================================================

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

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
    def swim_cycle_keyframes(*a, **kw): pass


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
        if 'Subsurface Weight' in bsdf.inputs: bsdf.inputs['Subsurface Weight'].default_value = 0.22
    return mat


def criar_mat_voronoi(nome, cor_a, cor_b, escala=6.0):
    """Pele de basalto rachado — mesma técnica do Kullullû (Voronoi
    definindo onde a pele escura cede lugar às veias de magma)."""
    mat = bpy.data.materials.new(name=nome)
    mat.use_nodes = True
    nodes, links = mat.node_tree.nodes, mat.node_tree.links
    bsdf = nodes.get("Principled BSDF")
    coord = nodes.new('ShaderNodeTexCoord')
    vor = nodes.new('ShaderNodeTexVoronoi')
    vor.inputs['Scale'].default_value = escala
    ramp = nodes.new('ShaderNodeValToRGB')
    ramp.color_ramp.elements[0].color = cor_b
    ramp.color_ramp.elements[1].color = cor_a
    ramp.color_ramp.elements[0].position = 0.35
    ramp.color_ramp.elements[1].position = 0.55
    links.new(coord.outputs['Object'], vor.inputs['Vector'])
    links.new(vor.outputs['Distance'], ramp.inputs['Fac'])
    links.new(ramp.outputs['Color'], bsdf.inputs['Base Color'])
    if 'Roughness' in bsdf.inputs: bsdf.inputs['Roughness'].default_value = 0.42
    if 'Emission Color' in bsdf.inputs: bsdf.inputs['Emission Color'].default_value = cor_a
    if 'Emission Strength' in bsdf.inputs:
        emit_mix = nodes.new('ShaderNodeMath')
        emit_mix.operation = 'MULTIPLY'
        emit_mix.inputs[1].default_value = 4.0
        links.new(ramp.outputs['Color'], emit_mix.inputs[0])
        # usa a luminosidade do proprio ramp como fator de emissao —
        # veias de magma (cor_a) brilham, pele escura (cor_b) nao
        links.new(emit_mix.outputs['Value'], bsdf.inputs['Emission Strength'])
    return mat


def smooth(obj):
    if obj and obj.type == 'MESH':
        for p in obj.data.polygons: p.use_smooth = True


def construir_leviata():
    setup_cena()
    m_corpo = criar_mat_voronoi("M_LevCorpo", (1.0, 0.35, 0.05, 1), (0.04, 0.02, 0.03, 1), escala=5.0)
    m_espinho = criar_mat("M_LevEspinho", (0.08, 0.05, 0.06, 1), metallic=0.3, roughness=0.5)
    m_olho = criar_mat("M_LevOlho", (1.0, 0.85, 0.1, 1), em=True, em_cor=(1.0, 0.85, 0.1, 1), em_str=9.0)
    m_dente = criar_mat("M_LevDente", (0.92, 0.90, 0.85, 1), roughness=0.15)

    root = bpy.data.objects.new("Leviata_Root", None)
    bpy.context.scene.collection.objects.link(root)

    # Corpo serpentino — 8 segmentos, mais grosso e mais longo que a
    # enguia (esta é um "sub-boss", precisa ler como maior/mais forte)
    segmentos = [
        (2.9, 0, 0.05, 0.62, 0.85),
        (2.1, 0, 0.10, 0.58, 0.80),
        (1.3, 0, 0.02, 0.52, 0.78),
        (0.5, 0, -0.10, 0.46, 0.76),
        (-0.3, 0, -0.15, 0.38, 0.72),
        (-1.05, 0, -0.08, 0.30, 0.68),
        (-1.75, 0, 0.05, 0.22, 0.62),
        (-2.35, 0, 0.15, 0.13, 0.55),
    ]
    leviata_segs = []
    for lx, ly, lz, r1, dep in segmentos:
        bpy.ops.mesh.primitive_cone_add(vertices=22, radius1=r1, radius2=r1*0.86, depth=dep, location=(lx, ly, lz))
        seg = bpy.context.active_object
        seg.rotation_euler = (0, math.radians(90), 0)
        seg.data.materials.append(m_corpo)
        seg.parent = root
        smooth(seg)
        leviata_segs.append(seg)

    # Cabeça — maior e mais angulosa que a da enguia, leitura de predador
    bpy.ops.mesh.primitive_uv_sphere_add(radius=0.72, location=(3.55, 0, 0.05))
    cabeca = bpy.context.active_object
    cabeca.scale = (1.35, 0.85, 0.78)
    cabeca.data.materials.append(m_corpo)
    cabeca.parent = root
    smooth(cabeca)

    # Mandíbula inferior — articulada, entreaberta (ameaçadora)
    bpy.ops.mesh.primitive_cone_add(vertices=4, radius1=0.42, depth=0.9, location=(3.75, 0, -0.42))
    mandibula = bpy.context.active_object
    mandibula.scale = (0.85, 1.0, 0.35)
    mandibula.rotation_euler = (0, math.radians(90), math.radians(18))
    mandibula.data.materials.append(m_espinho)
    mandibula.parent = root
    smooth(mandibula)

    # Dentes (2 fileiras simples, superior/inferior)
    for row_z, flip in [(0.18, 1), (-0.28, -1)]:
        for i in range(5):
            dx = 3.15 + i * 0.16
            bpy.ops.mesh.primitive_cone_add(vertices=4, radius1=0.05, depth=0.22, location=(dx, 0, row_z))
            d = bpy.context.active_object
            d.rotation_euler = (math.radians(90 * flip), 0, 0)
            d.data.materials.append(m_dente)
            d.parent = root

    # Olhos — pequenos, predatórios, bem separados na cabeça angulosa
    for s in [-1, 1]:
        bpy.ops.mesh.primitive_uv_sphere_add(radius=0.11, location=(3.75, s*0.48, 0.22))
        ol = bpy.context.active_object
        ol.data.materials.append(m_olho)
        ol.parent = root
        smooth(ol)

    # Crista dorsal de espinhos — ao longo de todo o corpo (silhueta
    # de "monstro", reforça leitura de sub-boss a distância)
    for i, (lx, ly, lz, r1, dep) in enumerate(segmentos):
        bpy.ops.mesh.primitive_cone_add(vertices=4, radius1=0.14 - i*0.01, depth=0.35 - i*0.02,
                                         location=(lx, 0, lz + r1*0.85))
        esp = bpy.context.active_object
        esp.rotation_euler = (math.radians(-15), 0, 0)
        esp.data.materials.append(m_espinho)
        esp.parent = root
        smooth(esp)

    # Nadadeiras laterais (par único, próximo à cabeça — impulsiona a
    # investida rápida em diagonal que a IA já faz em jogo)
    for s in [-1, 1]:
        bpy.ops.mesh.primitive_cone_add(vertices=3, radius1=0.55, depth=0.9, location=(2.5, s*0.55, -0.15))
        fin = bpy.context.active_object
        fin.rotation_euler = (math.radians(20), math.radians(s*70), 0)
        fin.scale = (0.06, 1.0, 0.85)
        fin.data.materials.append(m_espinho)
        fin.parent = root
        smooth(fin)

    # Cauda bifurcada (igual convenção da enguia, maior)
    cauda_bifurcada = []
    for s in [-1, 1]:
        bpy.ops.mesh.primitive_cone_add(vertices=4, radius1=0.44, depth=0.95, location=(-2.65, 0.10+s*0.35, 0.18+s*0.1))
        ct = bpy.context.active_object
        ct.scale = (0.05, 0.9, 0.95)
        ct.rotation_euler = (math.radians(s*22), math.radians(95), 0)
        ct.data.materials.append(m_espinho)
        ct.parent = root
        smooth(ct)
        cauda_bifurcada.append(ct)

    # Câmera / luz — enquadramento mais largo (é maior que os outros
    # inimigos, precisa de mais espaço no frame)
    bpy.ops.object.camera_add(location=(0.4, -8.5, 0))
    cam = bpy.context.active_object
    cam.rotation_euler = (math.radians(90), 0, 0)
    cam.data.lens = 32
    bpy.context.scene.camera = cam

    bpy.ops.object.light_add(type='AREA', location=(1.5, -5.5, 3.5))
    kl = bpy.context.active_object
    kl.data.energy = 700; kl.data.size = 4.5
    kl.data.color = (1.0, 0.5, 0.2)
    bpy.ops.object.light_add(type='AREA', location=(-2.5, 4.5, 2.5))
    rl = bpy.context.active_object
    rl.data.energy = 550; rl.data.size = 3.5
    rl.data.color = (0.3, 0.2, 0.9)
    bpy.ops.object.light_add(type='POINT', location=(3.5, 0, 0.3))
    pl = bpy.context.active_object
    pl.data.energy = 200; pl.data.color = (1.0, 0.8, 0.2)

    # Rim (cor dos próprios olhos — mesma regra de sempre)
    for mat in (m_corpo, m_espinho):
        add_fresnel_rim(mat, (1.0, 0.85, 0.1), power=1.4, strength=6.5)

    # Ciclo de nado — corpo + cauda bifurcada, mesmo padrão da enguia
    # (candidato natural, é literalmente uma serpente marinha)
    swim_cycle_keyframes(root, leviata_segs + cauda_bifurcada, frame_count=8,
                          amp_deg=15.0, wavelength_segs=3.5, cycles=1.0,
                          bob_amp=0.06, root_sway_deg=2.5)


def salvar(nome):
    bdir = os.path.join(BASE_DIR, "blends", "04_peixe_sombrio_inimigo")
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
    construir_leviata()
    salvar("12_leviata_menor")  # nome de saida bate com EnemyType.LEVIATA
    print("OK 14_leviata_menor v1.0")
