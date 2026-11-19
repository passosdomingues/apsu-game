import bpy
import math
import os

# --- Biblioteca compartilhada (2026-08-16: rim light + vida idle) ---
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
    def add_idle_life_keyframes(*a, **kw): pass

# ============================================================
# 05B - GUARDIOES VARIACOES v6.0
# Coral Vivo + Lamassu Aquatico + Oraculo das Chamas Abissais
# Salva em: blends/05_guardiao_atlante_npc/
# ============================================================

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
BLEND_DIR = os.path.join(BASE_DIR, "blends", "05_guardiao_atlante_npc")
RENDER_DIR = os.path.join(BASE_DIR, "renders")

def setup_cena():
    bpy.ops.object.select_all(action='SELECT')
    bpy.ops.object.delete(use_global=False)
    for mesh in list(bpy.data.meshes): bpy.data.meshes.remove(mesh, do_unlink=True)
    for mat in list(bpy.data.materials): bpy.data.materials.remove(mat, do_unlink=True)
    scene = bpy.context.scene
    try: scene.render.engine = 'BLENDER_EEVEE_NEXT'
    except: scene.render.engine = 'BLENDER_EEVEE'
    if hasattr(scene, 'eevee') and hasattr(scene.eevee, 'taa_render_samples'):
        scene.eevee.taa_render_samples = 16
    scene.render.resolution_x = 1080
    scene.render.resolution_y = 1920
    scene.render.film_transparent = True
    scene.view_settings.view_transform = 'Filmic'
    scene.view_settings.look = 'Medium High Contrast'

def criar_mat(nome, cor, metallic=0.2, roughness=0.45, em=False, em_cor=None, em_str=5.0):
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

def criar_mat_pedra(nome, cor_a, cor_b, escala=12.0):
    mat = bpy.data.materials.new(name=nome)
    mat.use_nodes = True
    nodes = mat.node_tree.nodes; links = mat.node_tree.links; nodes.clear()
    out = nodes.new('ShaderNodeOutputMaterial')
    bsdf = nodes.new('ShaderNodeBsdfPrincipled')
    coord = nodes.new('ShaderNodeTexCoord')
    noise = nodes.new('ShaderNodeTexNoise')
    ramp = nodes.new('ShaderNodeValToRGB')
    bump = nodes.new('ShaderNodeBump')
    noise.inputs['Scale'].default_value = escala
    noise.inputs['Detail'].default_value = 6.0
    ramp.color_ramp.elements[0].position = 0.3; ramp.color_ramp.elements[0].color = cor_b
    ramp.color_ramp.elements[1].position = 0.75; ramp.color_ramp.elements[1].color = cor_a
    bump.inputs['Strength'].default_value = 0.5
    if 'Roughness' in bsdf.inputs: bsdf.inputs['Roughness'].default_value = 0.78
    links.new(coord.outputs['Object'], noise.inputs['Vector'])
    links.new(noise.outputs['Fac'], ramp.inputs['Fac'])
    links.new(ramp.outputs['Color'], bsdf.inputs['Base Color'])
    links.new(noise.outputs['Fac'], bump.inputs['Height'])
    links.new(bump.outputs['Normal'], bsdf.inputs['Normal'])
    links.new(bsdf.outputs['BSDF'], out.inputs['Surface'])
    return mat

def smooth(obj):
    if obj and obj.type == 'MESH':
        for p in obj.data.polygons: p.use_smooth = True

def add_camera_portrait(y=-9.2, z=2.3):
    bpy.ops.object.camera_add(location=(0, y, z))
    cam = bpy.context.active_object
    cam.rotation_euler = (math.radians(90), 0, 0)
    cam.data.lens = 42
    bpy.context.scene.camera = cam

def construir_coral_vivo():
    setup_cena()
    m_coral = criar_mat_pedra("M_CoralPed", (0.72, 0.15, 0.08, 1), (0.42, 0.05, 0.03, 1))
    m_musgo = criar_mat("M_CoralMusgo", (0.08, 0.48, 0.22, 1), metallic=0.12, roughness=0.65)
    m_bio   = criar_mat("M_CoralBio",   (1.0, 0.72, 0.0, 1), em=True, em_cor=(1.0, 0.72, 0.0, 1), em_str=7.0)

    root = bpy.data.objects.new("CoralGuard_Root", None)
    bpy.context.scene.collection.objects.link(root)

    # Base
    bpy.ops.mesh.primitive_cylinder_add(radius=1.05, depth=0.52, vertices=12, location=(0, 0, 0.26))
    base = bpy.context.active_object; base.data.materials.append(m_coral); base.parent = root; smooth(base)
    bpy.ops.mesh.primitive_cylinder_add(radius=0.88, depth=0.28, vertices=12, location=(0, 0, 0.66))
    deg = bpy.context.active_object; deg.data.materials.append(m_coral); deg.parent = root; smooth(deg)

    # Corpo colunar coralino
    bpy.ops.mesh.primitive_cylinder_add(radius=0.62, depth=2.6, vertices=14, location=(0, 0, 2.1))
    corpo = bpy.context.active_object; corpo.scale = (1.0, 0.78, 1.0)
    corpo.data.materials.append(m_coral); corpo.parent = root; smooth(corpo)

    # Musgo e algas nas faixas
    for z_pos in [1.5, 2.15, 2.8]:
        bpy.ops.mesh.primitive_torus_add(major_radius=0.64, minor_radius=0.115, location=(0, 0, z_pos))
        fa = bpy.context.active_object; fa.scale = (1.0, 0.78, 1.0)
        fa.data.materials.append(m_musgo); fa.parent = root; smooth(fa)

    # Cabeça
    bpy.ops.mesh.primitive_cube_add(size=0.72, location=(0, 0, 3.65))
    cab = bpy.context.active_object; cab.scale = (1.0, 0.82, 1.12)
    cab.data.materials.append(m_coral); cab.parent = root; smooth(cab)

    # Olhos bio
    for s in [-1, 1]:
        bpy.ops.mesh.primitive_uv_sphere_add(radius=0.082, location=(s*0.20, -0.48, 3.72))
        ol = bpy.context.active_object; ol.data.materials.append(m_bio); ol.parent = root; smooth(ol)

    # Elmo de coral vivo
    bpy.ops.mesh.primitive_cylinder_add(radius=0.55, depth=0.40, vertices=12, location=(0, 0, 4.16))
    elmo = bpy.context.active_object; elmo.data.materials.append(m_musgo); elmo.parent = root; smooth(elmo)

    # Cornos de coral ramificados
    for s in [-1, 1]:
        bpy.ops.mesh.primitive_cone_add(vertices=6, radius1=0.12, depth=0.58, location=(s*0.42, 0, 4.52))
        ch = bpy.context.active_object; ch.rotation_euler = (0, 0, math.radians(s*28))
        ch.data.materials.append(m_coral); ch.parent = root; smooth(ch)
        # Ramificacao lateral
        bpy.ops.mesh.primitive_cone_add(vertices=4, radius1=0.06, depth=0.32, location=(s*0.62, 0, 4.68))
        cr = bpy.context.active_object; cr.rotation_euler = (0, 0, math.radians(s*52))
        cr.data.materials.append(m_bio); cr.parent = root; smooth(cr)

    # Gema bio
    bpy.ops.mesh.primitive_ico_sphere_add(radius=0.10, subdivisions=3, location=(0, -0.48, 4.22))
    gem = bpy.context.active_object; gem.data.materials.append(m_bio); gem.parent = root; smooth(gem)

    # Bracos
    for s in [-1, 1]:
        bpy.ops.mesh.primitive_cylinder_add(radius=0.20, depth=1.45, location=(s*0.78, -0.08, 2.0))
        brc = bpy.context.active_object
        brc.rotation_euler = (math.radians(32), math.radians(s*48), 0)
        brc.data.materials.append(m_coral); brc.parent = root; smooth(brc)
        bpy.ops.mesh.primitive_cylinder_add(radius=0.24, depth=0.20, location=(s*1.08, -0.22, 1.55))
        brac = bpy.context.active_object
        brac.rotation_euler = (math.radians(32), math.radians(s*48), 0)
        brac.data.materials.append(m_musgo); brac.parent = root; smooth(brac)

    # Runas bio
    runas = [(-0.28, -0.50, 2.55), (0.0, -0.50, 2.55), (0.28, -0.50, 2.55),
             (-0.14, -0.50, 2.30), (0.14, -0.50, 2.30), (0.0, -0.50, 2.05)]
    for rx, ry, rz in runas:
        bpy.ops.mesh.primitive_cube_add(size=0.095, location=(rx, ry, rz))
        runa = bpy.context.active_object; runa.scale = (1.0, 0.28, 0.68)
        runa.data.materials.append(m_bio); runa.parent = root

    add_camera_portrait()
    bpy.ops.object.light_add(type='AREA', location=(1.5, -6.0, 5.5))
    kl = bpy.context.active_object; kl.data.energy = 580; kl.data.size = 4.5
    kl.data.color = (1.0, 0.72, 0.52); kl.rotation_euler = (math.radians(42), 0, math.radians(-18))
    bpy.ops.object.light_add(type='AREA', location=(-2.8, 4.5, 4.0))
    rl = bpy.context.active_object; rl.data.energy = 750; rl.data.size = 4.0
    rl.data.color = (0.5, 1.0, 0.4); rl.rotation_euler = (math.radians(-40), 0, math.radians(148))

def construir_lamassu():
    setup_cena()
    m_pedra  = criar_mat_pedra("M_LamPed", (0.72, 0.68, 0.52, 1), (0.50, 0.46, 0.32, 1))
    m_metal  = criar_mat("M_LamMetal", (0.55, 0.75, 0.85, 1), metallic=0.62, roughness=0.28)
    m_runa   = criar_mat("M_LamRuna",  (0.25, 0.90, 1.0, 1), em=True, em_cor=(0.25, 0.9, 1.0, 1), em_str=6.0)

    root = bpy.data.objects.new("Lamassu_Root", None)
    bpy.context.scene.collection.objects.link(root)

    # Base
    bpy.ops.mesh.primitive_cylinder_add(radius=1.05, depth=0.52, vertices=12, location=(0, 0, 0.26))
    base = bpy.context.active_object; base.data.materials.append(m_pedra); base.parent = root; smooth(base)
    bpy.ops.mesh.primitive_cylinder_add(radius=0.88, depth=0.28, vertices=12, location=(0, 0, 0.66))
    deg = bpy.context.active_object; deg.data.materials.append(m_pedra); deg.parent = root; smooth(deg)

    # Corpo colunar
    bpy.ops.mesh.primitive_cylinder_add(radius=0.62, depth=2.6, vertices=14, location=(0, 0, 2.1))
    corpo = bpy.context.active_object; corpo.scale = (1.0, 0.78, 1.0)
    corpo.data.materials.append(m_pedra); corpo.parent = root; smooth(corpo)

    # Faixas metalicas
    for z_pos in [1.5, 2.15]:
        bpy.ops.mesh.primitive_torus_add(major_radius=0.64, minor_radius=0.115, location=(0, 0, z_pos))
        fa = bpy.context.active_object; fa.scale = (1.0, 0.78, 1.0)
        fa.data.materials.append(m_metal); fa.parent = root; smooth(fa)

    # Cabeca com barba mesopotamica
    bpy.ops.mesh.primitive_cube_add(size=0.72, location=(0, 0, 3.65))
    cab = bpy.context.active_object; cab.scale = (1.0, 0.82, 1.12)
    cab.data.materials.append(m_pedra); cab.parent = root; smooth(cab)

    # Barba em camadas
    for i, zb in enumerate([3.28, 3.1, 2.92]):
        bpy.ops.mesh.primitive_cube_add(size=0.55-i*0.06, location=(0, -0.38, zb))
        barb = bpy.context.active_object; barb.scale = (1.0+i*0.1, 0.38, 0.38)
        barb.data.materials.append(m_pedra); barb.parent = root; smooth(barb)

    # Olhos
    for s in [-1, 1]:
        bpy.ops.mesh.primitive_uv_sphere_add(radius=0.082, location=(s*0.20, -0.48, 3.72))
        ol = bpy.context.active_object; ol.data.materials.append(m_runa); ol.parent = root; smooth(ol)

    # Elmo Lamassu com 5 chifres
    bpy.ops.mesh.primitive_cylinder_add(radius=0.55, depth=0.40, vertices=12, location=(0, 0, 4.16))
    elmo = bpy.context.active_object; elmo.data.materials.append(m_metal); elmo.parent = root; smooth(elmo)
    for i in range(5):
        a = -40 + i*20
        bpy.ops.mesh.primitive_cone_add(vertices=6, radius1=0.10, depth=0.42, location=(math.sin(math.radians(a))*0.35, 0, 4.52+i*0.04))
        ch = bpy.context.active_object; ch.rotation_euler = (0, 0, math.radians(a))
        ch.data.materials.append(m_metal); ch.parent = root; smooth(ch)

    # Gema
    bpy.ops.mesh.primitive_ico_sphere_add(radius=0.10, subdivisions=3, location=(0, -0.48, 4.22))
    gem = bpy.context.active_object; gem.data.materials.append(m_runa); gem.parent = root; smooth(gem)

    # Bracos
    for s in [-1, 1]:
        bpy.ops.mesh.primitive_cylinder_add(radius=0.20, depth=1.45, location=(s*0.78, -0.08, 2.0))
        brc = bpy.context.active_object
        brc.rotation_euler = (math.radians(32), math.radians(s*48), 0)
        brc.data.materials.append(m_pedra); brc.parent = root; smooth(brc)
        bpy.ops.mesh.primitive_cylinder_add(radius=0.24, depth=0.20, location=(s*1.08, -0.22, 1.55))
        brac = bpy.context.active_object
        brac.rotation_euler = (math.radians(32), math.radians(s*48), 0)
        brac.data.materials.append(m_metal); brac.parent = root; smooth(brac)

    # Runas
    runas = [(-0.28, -0.50, 2.55), (0.0, -0.50, 2.55), (0.28, -0.50, 2.55),
             (-0.14, -0.50, 2.30), (0.14, -0.50, 2.30), (0.0, -0.50, 2.05)]
    for rx, ry, rz in runas:
        bpy.ops.mesh.primitive_cube_add(size=0.095, location=(rx, ry, rz))
        runa = bpy.context.active_object; runa.scale = (1.0, 0.28, 0.68)
        runa.data.materials.append(m_runa); runa.parent = root

    add_camera_portrait()
    bpy.ops.object.light_add(type='AREA', location=(1.5, -6.0, 5.5))
    kl = bpy.context.active_object; kl.data.energy = 620; kl.data.size = 4.5
    kl.data.color = (0.72, 0.95, 0.88); kl.rotation_euler = (math.radians(42), 0, math.radians(-18))
    bpy.ops.object.light_add(type='AREA', location=(-2.8, 4.5, 4.0))
    rl = bpy.context.active_object; rl.data.energy = 780; rl.data.size = 4.0
    rl.data.color = (0.3, 0.7, 1.0); rl.rotation_euler = (math.radians(-40), 0, math.radians(148))

def construir_oraculo_chamas():
    """
    ORACULO DAS CHAMAS ABISSAIS — Guardiao da Fase 4 (Abismo Vulcanico).
    2026-08-16: faltava um 4o guardiao (GuardianEntity type=3, usado na
    Fase 4) — so existia o nome/dialogo em Java, nenhum modelo 3D.
    Renomeado de "Oraculo das Correntes" (nome que nao batia com uma
    fase vulcanica) para algo que sugere a propria fase: chamas,
    profecia, magma. Visual: mesma linguagem "totem colunar" dos
    outros guardioes, mas com terceiro-olho profetico (gema no lugar
    da testa), coroa de brasas flutuantes (em vez de chifres/galhos)
    e um braseiro erguido nos bracos.
    """
    setup_cena()
    m_obsidiana = criar_mat_pedra("M_OracPed", (0.05, 0.03, 0.04, 1), (0.02, 0.01, 0.015, 1))
    m_bronze    = criar_mat("M_OracBronze", (0.55, 0.28, 0.10, 1), metallic=0.75, roughness=0.30)
    m_magma     = criar_mat("M_OracMagma", (1.0, 0.35, 0.05, 1), em=True, em_cor=(1.0, 0.35, 0.05, 1), em_str=8.0)

    root = bpy.data.objects.new("OraculoGuard_Root", None)
    bpy.context.scene.collection.objects.link(root)

    # Base — pedestal vulcanico rachado
    bpy.ops.mesh.primitive_cylinder_add(radius=1.05, depth=0.52, vertices=12, location=(0, 0, 0.26))
    base = bpy.context.active_object; base.data.materials.append(m_obsidiana); base.parent = root; smooth(base)
    bpy.ops.mesh.primitive_cylinder_add(radius=0.88, depth=0.28, vertices=12, location=(0, 0, 0.66))
    deg = bpy.context.active_object; deg.data.materials.append(m_obsidiana); deg.parent = root; smooth(deg)

    # Corpo colunar de obsidiana
    bpy.ops.mesh.primitive_cylinder_add(radius=0.60, depth=2.6, vertices=14, location=(0, 0, 2.1))
    corpo = bpy.context.active_object; corpo.scale = (1.0, 0.78, 1.0)
    corpo.data.materials.append(m_obsidiana); corpo.parent = root; smooth(corpo)

    # Faixas de bronze (cinturoes) — mesma posicao das faixas de musgo do coral
    for z_pos in [1.5, 2.15, 2.8]:
        bpy.ops.mesh.primitive_torus_add(major_radius=0.62, minor_radius=0.10, location=(0, 0, z_pos))
        fa = bpy.context.active_object; fa.scale = (1.0, 0.78, 1.0)
        fa.data.materials.append(m_bronze); fa.parent = root; smooth(fa)

    # Cabeca
    bpy.ops.mesh.primitive_cube_add(size=0.72, location=(0, 0, 3.65))
    cab = bpy.context.active_object; cab.scale = (1.0, 0.82, 1.12)
    cab.data.materials.append(m_obsidiana); cab.parent = root; smooth(cab)

    # Olhos (pequenos, quase apagados — o "ver" de verdade vem do terceiro olho)
    for s in [-1, 1]:
        bpy.ops.mesh.primitive_uv_sphere_add(radius=0.06, location=(s*0.20, -0.48, 3.72))
        ol = bpy.context.active_object; ol.data.materials.append(m_magma); ol.parent = root; smooth(ol)

    # TERCEIRO OLHO — gema profetica na testa, feature de assinatura do Oraculo
    bpy.ops.mesh.primitive_ico_sphere_add(radius=0.16, subdivisions=3, location=(0, -0.50, 4.02))
    terceiro_olho = bpy.context.active_object
    terceiro_olho.data.materials.append(m_magma); terceiro_olho.parent = root; smooth(terceiro_olho)

    # Elmo/coroa base
    bpy.ops.mesh.primitive_cylinder_add(radius=0.53, depth=0.36, vertices=12, location=(0, 0, 4.14))
    elmo = bpy.context.active_object; elmo.data.materials.append(m_bronze); elmo.parent = root; smooth(elmo)

    # COROA DE BRASAS FLUTUANTES — em vez de chifres/galhos, brasas em orbita
    brasas = []
    for i in range(6):
        a = 2 * math.pi * i / 6.0
        bx, by = math.cos(a) * 0.46, math.sin(a) * 0.46 * 0.6
        bz = 4.55 + 0.10 * math.sin(a * 2)
        bpy.ops.mesh.primitive_ico_sphere_add(radius=0.075 + 0.02*(i % 2), subdivisions=2, location=(bx, by - 0.15, bz))
        br = bpy.context.active_object
        br.data.materials.append(m_magma); br.parent = root; smooth(br)
        brasas.append(br)

    # Bracos erguendo um braseiro
    for s in [-1, 1]:
        bpy.ops.mesh.primitive_cylinder_add(radius=0.19, depth=1.35, location=(s*0.72, -0.05, 2.15))
        brc = bpy.context.active_object
        brc.rotation_euler = (math.radians(-38), math.radians(s*30), 0)
        brc.data.materials.append(m_obsidiana); brc.parent = root; smooth(brc)

    # Braseiro (tigela de bronze com chama)
    bpy.ops.mesh.primitive_torus_add(major_radius=0.30, minor_radius=0.09, location=(0, -0.65, 3.35))
    braseiro = bpy.context.active_object
    braseiro.rotation_euler = (math.radians(90), 0, 0)
    braseiro.data.materials.append(m_bronze); braseiro.parent = root; smooth(braseiro)
    bpy.ops.mesh.primitive_cone_add(vertices=8, radius1=0.22, depth=0.42, location=(0, -0.65, 3.55))
    chama = bpy.context.active_object
    chama.data.materials.append(m_magma); chama.parent = root; smooth(chama)

    # Runas de magma (mesma disposicao dos outros guardioes)
    runas = [(-0.28, -0.48, 2.55), (0.0, -0.48, 2.55), (0.28, -0.48, 2.55),
             (-0.14, -0.48, 2.30), (0.14, -0.48, 2.30), (0.0, -0.48, 2.05)]
    for rx, ry, rz in runas:
        bpy.ops.mesh.primitive_cube_add(size=0.095, location=(rx, ry, rz))
        runa = bpy.context.active_object; runa.scale = (1.0, 0.28, 0.68)
        runa.data.materials.append(m_magma); runa.parent = root

    add_camera_portrait()
    bpy.ops.object.light_add(type='AREA', location=(1.5, -6.0, 5.5))
    kl = bpy.context.active_object; kl.data.energy = 550; kl.data.size = 4.5
    kl.data.color = (1.0, 0.55, 0.30); kl.rotation_euler = (math.radians(42), 0, math.radians(-18))
    bpy.ops.object.light_add(type='AREA', location=(-2.8, 4.5, 4.0))
    rl = bpy.context.active_object; rl.data.energy = 700; rl.data.size = 4.0
    rl.data.color = (0.9, 0.3, 0.1); rl.rotation_euler = (math.radians(-40), 0, math.radians(148))

    # Rim quente-magma (cor do proprio terceiro olho — mesma regra usada nos
    # outros guardioes/inimigos: reforca o accent que o design ja tem)
    ORAC_RIM = (1.0, 0.35, 0.05)
    for mat in (m_obsidiana, m_bronze):
        add_fresnel_rim(mat, ORAC_RIM, power=1.5, strength=6.5)

    # Vida idle — estatua viva, sem manto, respiracao bem sutil (obsidiana,
    # nao carne) + brasas da coroa ficam paradas (fazem parte da geometria
    # do elmo, nao entram na cadeia — orbita delas seria um passo futuro)
    add_idle_life_keyframes(root, manto_obj=None, torso_obj=corpo,
                             cabeca_obj=cab, frame_count=8,
                             sway_deg=0.0, breathe_amount=0.008,
                             head_tilt_deg=1.6, cycles=0.7)


def salvar(nome):
    os.makedirs(BLEND_DIR, exist_ok=True); os.makedirs(RENDER_DIR, exist_ok=True)
    try:
        bpy.ops.wm.save_as_mainfile(filepath=os.path.join(BLEND_DIR, nome+".blend"))
        print("Blend salvo: " + nome)
    except Exception as e:
        print("Aviso: " + str(e))
    bpy.context.scene.render.filepath = os.path.join(RENDER_DIR, nome+".png")
    bpy.ops.render.render(write_still=True)
    print("Render salvo: " + nome)

if __name__ == "__main__":
    construir_coral_vivo()
    salvar("05_guardiao_coral_vivo")

    construir_lamassu()
    salvar("05_guardiao_lamassu_aquatico")

    construir_oraculo_chamas()
    salvar("05_guardiao_oraculo_correntes")  # nome de arquivo mantido —
    # bate com GuardianEntity.SPRITE_PATHS[3] em Java, so o NOME EXIBIDO
    # (NAMES[3]) e o dialogo de abertura mudaram pra "Oraculo das Chamas
    # Abissais", mais coerente com a Fase 4 (vulcanica) do que o antigo
    # "Oraculo das Correntes" (nome de agua numa fase de lava).

    print("OK 05_guardioes_variacoes v6.0")
