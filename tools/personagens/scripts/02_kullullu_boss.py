import bpy
import math
import os

# --- Biblioteca compartilhada (auditoria 2026-08-15: rim light) ---
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
# 02 - KULLULLU: O APKALLU CORROMPIDO DO ABISMO (BOSS v4.0)
# Paleta 60-30-10:
#   60% Obsidiana Roxa-Negra com Rachaduras Tectônicas
#   30% Vermelho Sangue / Carmesim Vulcânico
#   10% Magma Laranja Emissivo (Rachaduras, Olhos, Núcleo)
# ============================================================

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
    scene.view_settings.look = 'Very High Contrast'

def criar_mat_obsidiana(nome, cor_base, cor_crack, escala=2.8, em_forca=1.5):
    mat = bpy.data.materials.new(name=nome)
    mat.use_nodes = True
    nodes = mat.node_tree.nodes
    links = mat.node_tree.links
    nodes.clear()
    out = nodes.new('ShaderNodeOutputMaterial')
    bsdf = nodes.new('ShaderNodeBsdfPrincipled')
    coord = nodes.new('ShaderNodeTexCoord')
    vor = nodes.new('ShaderNodeTexVoronoi')
    noise = nodes.new('ShaderNodeTexNoise')
    ramp_d = nodes.new('ShaderNodeValToRGB')
    ramp_e = nodes.new('ShaderNodeValToRGB')
    bump = nodes.new('ShaderNodeBump')

    vor.inputs['Scale'].default_value = escala
    vor.feature = 'DISTANCE_TO_EDGE'
    noise.inputs['Scale'].default_value = escala * 2.0
    noise.inputs['Detail'].default_value = 5.0

    ramp_d.color_ramp.elements[0].position = 0.05
    ramp_d.color_ramp.elements[0].color = cor_crack
    ramp_d.color_ramp.elements[1].position = 0.16
    ramp_d.color_ramp.elements[1].color = cor_base
    ramp_e.color_ramp.elements[0].position = 0.03
    ramp_e.color_ramp.elements[0].color = (1.0, 0.25, 0.0, 1.0)
    ramp_e.color_ramp.elements[1].position = 0.08
    ramp_e.color_ramp.elements[1].color = (0,0,0,1)

    bump.inputs['Strength'].default_value = 0.6
    if 'Metallic' in bsdf.inputs: bsdf.inputs['Metallic'].default_value = 0.5
    if 'Roughness' in bsdf.inputs: bsdf.inputs['Roughness'].default_value = 0.28
    if 'Emission Strength' in bsdf.inputs: bsdf.inputs['Emission Strength'].default_value = em_forca
    if 'Roughness' in bsdf.inputs: bsdf.inputs['Roughness'].default_value = 0.35

    links.new(coord.outputs['Object'], vor.inputs['Vector'])
    links.new(coord.outputs['Object'], noise.inputs['Vector'])
    links.new(vor.outputs['Distance'], ramp_d.inputs['Fac'])
    links.new(ramp_d.outputs['Color'], bsdf.inputs['Base Color'])
    links.new(vor.outputs['Distance'], ramp_e.inputs['Fac'])
    if 'Emission Color' in bsdf.inputs: links.new(ramp_e.outputs['Color'], bsdf.inputs['Emission Color'])
    links.new(noise.outputs['Fac'], bump.inputs['Height'])
    links.new(bump.outputs['Normal'], bsdf.inputs['Normal'])
    links.new(bsdf.outputs['BSDF'], out.inputs['Surface'])
    return mat

def criar_mat(nome, cor, metallic=0.2, roughness=0.35, em=False, em_cor=None, em_str=5.0):
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

def construir_boss():
    setup_cena()

    m_corpo  = criar_mat_obsidiana("M_Corpo",  (0.04, 0.01, 0.07, 1), (1.0, 0.25, 0.02, 1))
    m_espinho = criar_mat("M_Espinho", (0.38, 0.02, 0.04, 1), metallic=0.4, roughness=0.32)
    m_magma  = criar_mat("M_Magma",   (1.0, 0.22, 0.0, 1), em=True, em_cor=(1.0, 0.22, 0.0, 1), em_str=5.0)
    m_couro  = criar_mat("M_Couro",   (0.02, 0.01, 0.03, 1), roughness=0.7)

    root = bpy.data.objects.new("Boss_Root", None)
    bpy.context.scene.collection.objects.link(root)

    # === TORSO COLOSSAL ===
    bpy.ops.mesh.primitive_uv_sphere_add(segments=32, ring_count=24, radius=1.35, location=(0, 0, 2.4))
    torso = bpy.context.active_object
    torso.scale = (1.15, 0.95, 1.25)
    torso.data.materials.append(m_corpo)
    torso.parent = root
    smooth(torso)

    # Placas peitorais
    for s in [-1, 1]:
        bpy.ops.mesh.primitive_uv_sphere_add(radius=0.52, location=(s*0.55, -0.55, 2.75))
        placa = bpy.context.active_object
        placa.scale = (0.85, 0.4, 0.75)
        placa.data.materials.append(m_espinho)
        placa.parent = root
        smooth(placa)

    # Nucleo de magma central
    bpy.ops.mesh.primitive_ico_sphere_add(radius=0.32, subdivisions=3, location=(0, -0.85, 2.55))
    nucleo = bpy.context.active_object
    nucleo.data.materials.append(m_magma)
    nucleo.parent = root
    smooth(nucleo)

    # === CABECA DEMONIACA ===
    bpy.ops.mesh.primitive_uv_sphere_add(segments=28, ring_count=24, radius=0.82, location=(0, -0.2, 4.25))
    cabeca = bpy.context.active_object
    cabeca.scale = (1.0, 1.1, 0.92)
    cabeca.data.materials.append(m_corpo)
    cabeca.parent = root
    smooth(cabeca)

    # Mandibula
    bpy.ops.mesh.primitive_cube_add(size=0.55, location=(0, -0.82, 3.78))
    mand = bpy.context.active_object
    mand.scale = (0.95, 0.55, 0.48)
    mand.data.materials.append(m_couro)
    mand.parent = root
    smooth(mand)

    # Dentes
    for i in range(5):
        bpy.ops.mesh.primitive_cone_add(vertices=4, radius1=0.045, depth=0.18, location=(-0.28+i*0.14, -1.05, 3.78))
        d = bpy.context.active_object
        d.data.materials.append(m_magma)
        d.parent = root

    # Chifres diabolicos curvos
    for s in [-1, 1]:
        bpy.ops.mesh.primitive_cone_add(vertices=12, radius1=0.26, depth=1.8, location=(s*0.85, -0.05, 5.1))
        chif = bpy.context.active_object
        chif.rotation_euler = (math.radians(-20), math.radians(s*38), math.radians(s*18))
        chif.data.materials.append(m_espinho)
        chif.parent = root
        smooth(chif)

    # Olhos multiplos
    olhos = [(-0.32,-1.0,4.4,0.12),(0.32,-1.0,4.4,0.12),(-0.5,-0.92,4.62,0.09),(0.5,-0.92,4.62,0.09)]
    for ox,oy,oz,orad in olhos:
        bpy.ops.mesh.primitive_uv_sphere_add(radius=orad, location=(ox,oy,oz))
        ol = bpy.context.active_object
        ol.data.materials.append(m_magma)
        ol.parent = root

    # === ESPINHOS DORSAIS ===
    for i, z in enumerate([3.6, 3.1, 2.6, 2.1, 1.7]):
        h = 1.0 - i*0.12
        bpy.ops.mesh.primitive_cone_add(vertices=6, radius1=0.2, depth=h*2.2, location=(0, -0.75+i*0.08, z))
        esp = bpy.context.active_object
        esp.rotation_euler = (math.radians(-78), 0, 0)
        esp.data.materials.append(m_espinho)
        esp.parent = root
        smooth(esp)

    # === BRACOS GIGANTESCOS ===
    for s in [-1, 1]:
        bpy.ops.mesh.primitive_cylinder_add(radius=0.38, depth=1.5, location=(s*1.6, 0.0, 2.5))
        brc = bpy.context.active_object
        brc.rotation_euler = (math.radians(15), math.radians(s*32), math.radians(s*18))
        brc.data.materials.append(m_corpo)
        brc.parent = root
        smooth(brc)
        # Garra
        bpy.ops.mesh.primitive_uv_sphere_add(radius=0.35, location=(s*2.45, 0.4, 1.65))
        garra = bpy.context.active_object
        garra.scale = (0.9, 0.85, 0.75)
        garra.data.materials.append(m_corpo)
        garra.parent = root
        smooth(garra)
        for gi in range(3):
            bpy.ops.mesh.primitive_cone_add(vertices=4, radius1=0.06, depth=0.35, location=(s*(2.45+gi*0.18*s), 0.15+gi*0.1, 1.3-gi*0.05))
            gar_pt = bpy.context.active_object
            gar_pt.rotation_euler = (math.radians(35), 0, math.radians(s*gi*15))
            gar_pt.data.materials.append(m_espinho)
            gar_pt.parent = root

    # === CAUDA MONSTRUOSA ===
    cauda = [
        (0,    0.3,  1.4,  1.0, 0.95),
        (0,    0.75, 0.45, 0.80, 0.85),
        (0.1,  1.15,-0.45, 0.62, 0.80),
        (0.2,  1.45,-1.25, 0.45, 0.75),
        (0.1,  1.55,-2.0,  0.30, 0.68),
        (-0.1, 1.40,-2.65, 0.18, 0.6),
    ]
    for i,(lx,ly,lz,r1,dep) in enumerate(cauda):
        bpy.ops.mesh.primitive_cone_add(vertices=20, radius1=r1, radius2=r1*0.78, depth=dep, location=(lx,ly,lz))
        seg = bpy.context.active_object
        seg.data.materials.append(m_corpo)
        seg.parent = root
        smooth(seg)

    # Nadadeiras caudais
    for s in [-1, 1]:
        bpy.ops.mesh.primitive_cone_add(vertices=4, radius1=0.6, depth=1.2, location=(s*0.4, 1.35, -2.9))
        nf = bpy.context.active_object
        nf.scale = (0.06, 0.9, 1.0)
        nf.rotation_euler = (math.radians(-48), math.radians(s*28), math.radians(s*12))
        nf.data.materials.append(m_espinho)
        nf.parent = root
        smooth(nf)

    # === LANCA DO ABISMO ===
    lance = bpy.data.objects.new("Lance", None)
    bpy.context.scene.collection.objects.link(lance)
    lance.parent = root
    lance.location = (-1.7, -0.9, 2.4)
    lance.rotation_euler = (math.radians(48), math.radians(12), math.radians(28))

    bpy.ops.mesh.primitive_cylinder_add(radius=0.075, depth=5.5, location=(0,0,0))
    hl = bpy.context.active_object
    hl.data.materials.append(m_espinho)
    hl.parent = lance
    smooth(hl)

    bpy.ops.mesh.primitive_cone_add(vertices=6, radius1=0.22, depth=1.5, location=(0,0,2.85))
    lam = bpy.context.active_object
    lam.data.materials.append(m_corpo)
    lam.parent = lance
    smooth(lam)

    bpy.ops.mesh.primitive_ico_sphere_add(radius=0.28, subdivisions=3, location=(0,0,2.05))
    ol = bpy.context.active_object
    ol.data.materials.append(m_magma)
    ol.parent = lance
    smooth(ol)

    # === CAMERA PORTRAIT FIXA ===
    # Personagem ocupa z=-3.2 ate z=6.5 (incluindo chifres e lanca)
    bpy.ops.object.camera_add(location=(0, -11.5, 1.8))
    cam = bpy.context.active_object
    cam.rotation_euler = (math.radians(90), 0, 0)
    cam.data.lens = 38
    bpy.context.scene.camera = cam

    # Luzes - frio azul dramatico para contrastar obsidiana escura
    bpy.ops.object.light_add(type='AREA', location=(2.0, -8.5, 5.5))
    kl = bpy.context.active_object
    kl.data.energy = 900
    kl.data.size = 5.5
    kl.data.color = (0.3, 0.55, 1.0)
    kl.rotation_euler = (math.radians(40), 0, math.radians(-15))

    bpy.ops.object.light_add(type='AREA', location=(-3.5, 5.0, 4.5))
    rl = bpy.context.active_object
    rl.data.energy = 700
    rl.data.size = 5.0
    rl.data.color = (0.15, 0.4, 1.0)
    rl.rotation_euler = (math.radians(-35), 0, math.radians(145))

    bpy.ops.object.light_add(type='POINT', location=(0, -1.5, -0.5))
    ul = bpy.context.active_object
    ul.data.energy = 600
    ul.data.color = (1.0, 0.12, 0.0)

    # === RIM LIGHT (auditoria 2026-08-15) ===
    # O corpo (obsidiana) e os espinhos ja tem trinca de magma laranja
    # emissiva — um rim NA MESMA cor ficaria redundante e reduziria a
    # leitura entre heroi (rim quente dourado) e vilao na mesma cena de
    # boss fight. Rim escolhido: magenta-violeta "corrompido", que
    # contrasta tanto com o laranja da magma quanto com o dourado do
    # heroi, reforçando "isto e o antagonista" so pela cor de contorno.
    BOSS_RIM = (0.95, 0.12, 0.55)
    for mat in (m_corpo, m_espinho, m_couro):
        add_fresnel_rim(mat, BOSS_RIM, power=1.4, strength=7.0)

    # === VIDA IDLE (2026-08-16) === Boss sem manto — respiração mais forte
    # e mais rápida que os NPCs (é um predador vivo, não estátua/pedra),
    # cabeça com leve deriva ameaçadora ("observando" o jogador).
    add_idle_life_keyframes(root, manto_obj=None, torso_obj=torso,
                             cabeca_obj=cabeca, frame_count=8,
                             sway_deg=0.0, breathe_amount=0.035,
                             head_tilt_deg=3.0, cycles=1.1)

def salvar_render(nome):
    base = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    bdir = os.path.join(base, "blends", "02_kullullu_boss")
    rdir = os.path.join(base, "renders")
    os.makedirs(bdir, exist_ok=True)
    os.makedirs(rdir, exist_ok=True)
    try:
        bpy.ops.wm.save_as_mainfile(filepath=os.path.join(bdir, nome+".blend"))
        print("Blend salvo: " + nome)
    except Exception as e:
        print("Aviso blend: " + str(e))
    bpy.context.scene.render.filepath = os.path.join(rdir, nome+".png")
    bpy.ops.render.render(write_still=True)
    print("Render salvo: " + nome)

if __name__ == "__main__":
    construir_boss()
    salvar_render("02_kullullu_boss")
    print("OK 02_kullullu_boss v5.0 (+ rim light)")
