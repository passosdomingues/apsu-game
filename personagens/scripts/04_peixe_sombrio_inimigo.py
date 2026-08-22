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
    def swim_cycle_keyframes(*a, **kw): pass

# ============================================================
# 04 - PEIXE-SOMBRIO DO ABISMO (INIMIGO COMUM v5.0)
# Paleta 60-30-10:
#   60% Verde-Doentio / Escamas Toxicas (Corpo Peixe)
#   30% Roxo-Venenoso / Carmesim (Barbatanas, Cauda, Espinhos)
#   10% Amarelo-Acido Emissivo (Olho, Isca Abissal, Dentes)
# Camara FIXA sem rotacao no root
# ============================================================

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
BLEND_DIR = os.path.join(BASE_DIR, "blends", "04_peixe_sombrio_inimigo")
RENDER_DIR = os.path.join(BASE_DIR, "renders")

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

def criar_mat_escamas_toxicas(nome):
    mat = bpy.data.materials.new(name=nome)
    mat.use_nodes = True
    nodes = mat.node_tree.nodes
    links = mat.node_tree.links
    nodes.clear()
    out   = nodes.new('ShaderNodeOutputMaterial')
    bsdf  = nodes.new('ShaderNodeBsdfPrincipled')
    coord = nodes.new('ShaderNodeTexCoord')
    vor   = nodes.new('ShaderNodeTexVoronoi')
    ramp  = nodes.new('ShaderNodeValToRGB')
    bump  = nodes.new('ShaderNodeBump')
    vor.feature = 'F1'
    vor.inputs['Scale'].default_value = 18.0
    ramp.color_ramp.elements[0].position = 0.2
    ramp.color_ramp.elements[0].color = (0.01, 0.22, 0.06, 1)
    ramp.color_ramp.elements[1].position = 0.8
    ramp.color_ramp.elements[1].color = (0.04, 0.55, 0.12, 1)
    bump.inputs['Strength'].default_value = 0.45
    if 'Metallic' in bsdf.inputs: bsdf.inputs['Metallic'].default_value = 0.35
    if 'Roughness' in bsdf.inputs: bsdf.inputs['Roughness'].default_value = 0.38
    links.new(coord.outputs['Object'], vor.inputs['Vector'])
    links.new(vor.outputs['Distance'], ramp.inputs['Fac'])
    links.new(ramp.outputs['Color'], bsdf.inputs['Base Color'])
    links.new(vor.outputs['Distance'], bump.inputs['Height'])
    links.new(bump.outputs['Normal'], bsdf.inputs['Normal'])
    links.new(bsdf.outputs['BSDF'], out.inputs['Surface'])
    return mat

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

def construir_peixe(variacao='padrao'):
    setup_cena(1920, 1080)

    if variacao == 'padrao':
        m_corpo = criar_mat_escamas_toxicas("M_Corpo")
        m_fin   = criar_mat("M_Fin",   (0.32, 0.04, 0.38, 1), metallic=0.42, roughness=0.32)
        m_olho  = criar_mat("M_Olho",  (0.7, 1.0, 0.08, 1), em=True, em_cor=(0.7, 1.0, 0.08, 1), em_str=9.0)
        m_dente = criar_mat("M_Dente", (0.88, 0.92, 0.78, 1), roughness=0.25)
    elif variacao == 'abissal':
        m_corpo = criar_mat("M_Corpo", (0.02, 0.02, 0.08, 1), metallic=0.3, roughness=0.5)
        m_fin   = criar_mat("M_Fin",   (0.06, 0.04, 0.22, 1), metallic=0.5, roughness=0.25)
        m_olho  = criar_mat("M_Olho",  (0.1, 0.4, 1.0, 1), em=True, em_cor=(0.1, 0.4, 1.0, 1), em_str=10.0)
        m_dente = criar_mat("M_Dente", (0.78, 0.88, 1.0, 1), roughness=0.2)
    else:  # coral
        m_corpo = criar_mat("M_Corpo", (0.55, 0.06, 0.06, 1), metallic=0.28, roughness=0.42)
        m_fin   = criar_mat("M_Fin",   (0.82, 0.12, 0.04, 1), metallic=0.4, roughness=0.28)
        m_olho  = criar_mat("M_Olho",  (1.0, 0.62, 0.0, 1), em=True, em_cor=(1.0, 0.62, 0.0, 1), em_str=8.0)
        m_dente = criar_mat("M_Dente", (0.95, 0.88, 0.75, 1), roughness=0.22)

    # === RIM LIGHT (auditoria 2026-08-15) ===
    # Reusa a propria cor do olho (ja e o "accent" desenhado pra cada
    # variacao) como cor de contorno — nao precisa escolher uma cor
    # nova nem duplicar literais: e sempre a identidade visual que o
    # personagem ja tinha, so agora tambem na borda da silhueta.
    _rim = tuple(m_olho.node_tree.nodes.get("Principled BSDF").inputs['Emission Color'].default_value)[:3]
    for mat in (m_corpo, m_fin):
        add_fresnel_rim(mat, _rim, power=1.4, strength=6.5)

    root = bpy.data.objects.new("Peixe_Root", None)
    bpy.context.scene.collection.objects.link(root)
    # SEM rotacao no root - camera fixa resolve

    # === CORPO OVOIDE ===
    bpy.ops.mesh.primitive_uv_sphere_add(segments=28, ring_count=22, radius=0.72, location=(0, 0, 0))
    corpo = bpy.context.active_object
    corpo.scale = (0.72, 1.38, 0.88)
    corpo.data.materials.append(m_corpo)
    corpo.parent = root
    smooth(corpo)

    # Mandibula
    bpy.ops.mesh.primitive_cube_add(size=0.5, location=(0, -0.88, -0.08))
    mand = bpy.context.active_object
    mand.scale = (0.72, 0.58, 0.32)
    mand.rotation_euler = (math.radians(12), 0, 0)
    mand.data.materials.append(m_corpo)
    mand.parent = root
    smooth(mand)

    # Dentes
    for i in range(5):
        bpy.ops.mesh.primitive_cone_add(vertices=4, radius1=0.032, depth=0.14, location=(-0.2+i*0.1, -1.08, 0.01))
        d = bpy.context.active_object
        d.data.materials.append(m_dente)
        d.parent = root

    # === OLHO CENTRAL ===
    bpy.ops.mesh.primitive_uv_sphere_add(radius=0.18, location=(0, -0.78, 0.18))
    olho = bpy.context.active_object
    olho.data.materials.append(m_olho)
    olho.parent = root
    smooth(olho)

    # Pupila
    bpy.ops.mesh.primitive_uv_sphere_add(radius=0.08, location=(0, -0.95, 0.18))
    pupila = bpy.context.active_object
    pupila.data.materials.append(criar_mat("M_Pupila", (0.01, 0.01, 0.01, 1), roughness=0.05))
    pupila.parent = root

    # === ANTENA BIOLUMINESCENTE ===
    bpy.ops.mesh.primitive_cylinder_add(radius=0.022, depth=0.75, location=(0, -0.4, 0.88))
    ant = bpy.context.active_object
    ant.rotation_euler = (math.radians(-42), 0, 0)
    ant.data.materials.append(m_fin)
    ant.parent = root
    smooth(ant)

    bpy.ops.mesh.primitive_ico_sphere_add(radius=0.14, subdivisions=3, location=(0, -0.72, 1.28))
    isca = bpy.context.active_object
    isca.data.materials.append(m_olho)
    isca.parent = root
    smooth(isca)

    # Glow ao redor da isca
    bpy.ops.mesh.primitive_uv_sphere_add(radius=0.22, location=(0, -0.72, 1.28))
    glow = bpy.context.active_object
    mg = criar_mat("M_Glow", (0.7, 1.0, 0.1, 1), roughness=0.05, em=True, em_cor=(0.7, 1.0, 0.1, 1), em_str=3.0)
    glow.data.materials.append(mg)
    glow.parent = root
    smooth(glow)

    # === CAUDA TRIANGULAR ===
    bpy.ops.mesh.primitive_cone_add(vertices=3, radius1=0.62, depth=1.05, location=(0, 1.02, 0))
    cauda = bpy.context.active_object
    cauda.rotation_euler = (math.radians(90), 0, 0)
    cauda.scale = (0.055, 1.0, 1.1)
    cauda.data.materials.append(m_fin)
    cauda.parent = root
    smooth(cauda)

    # === BARBATANAS PEITORAIS ===
    fins_peitorais = []
    for s in [-1, 1]:
        bpy.ops.mesh.primitive_cone_add(vertices=3, radius1=0.4, depth=0.7, location=(s*0.62, 0.12, 0))
        fin = bpy.context.active_object
        fin.rotation_euler = (math.radians(18), math.radians(s*58), 0)
        fin.scale = (0.048, 0.95, 0.72)
        fin.data.materials.append(m_fin)
        fin.parent = root
        smooth(fin)
        fins_peitorais.append(fin)

    # === CICLO DE NADO (2026-08-16, pedido do Rafa: "toda criatura que
    # nade" precisa de ciclo de verdade) ===
    # Peixe sem cauda articulada em segmentos — trata as 2 barbatanas
    # peitorais + a cauda triangular como uma "cadeia" curta (amplitude
    # cresce em direção à cauda, que é quem realmente propele o peixe).
    swim_cycle_keyframes(root, fins_peitorais + [cauda], frame_count=8,
                          amp_deg=22.0, wavelength_segs=2.0, cycles=1.2,
                          bob_amp=0.08, root_sway_deg=4.0)

    # === ESPINHOS DORSAIS ===
    for i, y in enumerate([-0.55, -0.18, 0.22, 0.58]):
        h = 0.42 - i*0.06
        bpy.ops.mesh.primitive_cone_add(vertices=4, radius1=0.065, depth=h*2.2, location=(0, y, 0.72))
        esp = bpy.context.active_object
        esp.rotation_euler = (math.radians(-28+i*5), 0, 0)
        esp.data.materials.append(m_fin)
        esp.parent = root
        smooth(esp)

    # === CAMARA FIXA (portrait, 1080x1920 nao - este e widescreen) ===
    # Peixe ocupa aprox x=-1.2 a +1.2, y=-1.2 a +1.5, z=-0.9 a +1.4
    # Camera frontal, ligeiramente de angulo
    bpy.ops.object.camera_add(location=(0.8, -6.5, 0.25))
    cam = bpy.context.active_object
    cam.rotation_euler = (math.radians(90), 0, math.radians(8))
    cam.data.lens = 44
    bpy.context.scene.camera = cam

    # Iluminacao
    bpy.ops.object.light_add(type='AREA', location=(1.5, -4.5, 2.8))
    kl = bpy.context.active_object
    kl.data.energy = 480
    kl.data.size = 3.5
    kl.data.color = (0.55, 1.0, 0.55)
    kl.rotation_euler = (math.radians(45), 0, math.radians(-25))

    bpy.ops.object.light_add(type='AREA', location=(-2.2, 3.5, 2.0))
    rl = bpy.context.active_object
    rl.data.energy = 650
    rl.data.size = 3.0
    rl.data.color = (0.72, 0.2, 1.0)
    rl.rotation_euler = (math.radians(-40), 0, math.radians(148))

    bpy.ops.object.light_add(type='POINT', location=(0, -0.72, 1.3))
    pl = bpy.context.active_object
    pl.data.energy = 220
    pl.data.color = (0.7, 1.0, 0.1)

def salvar(nome, subpasta="04_peixe_sombrio_inimigo"):
    bdir = os.path.join(BASE_DIR, "blends", subpasta)
    rdir = RENDER_DIR
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
    construir_peixe('padrao')
    salvar("04_peixe_sombrio_inimigo")
    print("OK 04_peixe_sombrio_inimigo v5.0")
