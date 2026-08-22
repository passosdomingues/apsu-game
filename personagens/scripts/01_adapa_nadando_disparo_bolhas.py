import bpy
import math
import os

# --- Biblioteca compartilhada (Sprint 6: rim light + ciclo de nado real) ---
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

HERO_RIM = (1.0, 0.55, 0.16)  # mesmo rim universal do 01_adapa_heroi.py

# ============================================================
# 01B - ADAPA: POSE DE ACAO - NADANDO E ATIRANDO BOLHAS (v4.0)
# Camera lateral 16:9 para capturar a pose horizontal dinamica
# Paleta 60-30-10: Turquesa / Dourado / Coral-Laranja Emissivo
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
    # Formato widescreen para cena de acao horizontal
    scene.render.resolution_x = 1920
    scene.render.resolution_y = 1080
    scene.render.film_transparent = True
    scene.view_settings.view_transform = 'Filmic'
    scene.view_settings.look = 'High Contrast'

def criar_mat_escamas(nome, cor_a, cor_b, metallic=0.68, roughness=0.18):
    mat = bpy.data.materials.new(name=nome)
    mat.use_nodes = True
    nodes = mat.node_tree.nodes
    links = mat.node_tree.links
    nodes.clear()
    out = nodes.new('ShaderNodeOutputMaterial')
    bsdf = nodes.new('ShaderNodeBsdfPrincipled')
    coord = nodes.new('ShaderNodeTexCoord')
    vor = nodes.new('ShaderNodeTexVoronoi')
    ramp = nodes.new('ShaderNodeValToRGB')
    bump = nodes.new('ShaderNodeBump')
    vor.feature = 'F1'
    vor.inputs['Scale'].default_value = 16.0
    ramp.color_ramp.elements[0].position = 0.2
    ramp.color_ramp.elements[0].color = cor_b
    ramp.color_ramp.elements[1].position = 0.8
    ramp.color_ramp.elements[1].color = cor_a
    bump.inputs['Strength'].default_value = 0.38
    if 'Metallic' in bsdf.inputs: bsdf.inputs['Metallic'].default_value = metallic
    if 'Roughness' in bsdf.inputs: bsdf.inputs['Roughness'].default_value = roughness
    if 'Subsurface Weight' in bsdf.inputs: bsdf.inputs['Subsurface Weight'].default_value = 0.30
    if 'Subsurface Radius' in bsdf.inputs: bsdf.inputs['Subsurface Radius'].default_value = (0.3, 0.5, 0.4)
    links.new(coord.outputs['Object'], vor.inputs['Vector'])
    links.new(vor.outputs['Distance'], ramp.inputs['Fac'])
    links.new(ramp.outputs['Color'], bsdf.inputs['Base Color'])
    links.new(vor.outputs['Distance'], bump.inputs['Height'])
    links.new(bump.outputs['Normal'], bsdf.inputs['Normal'])
    links.new(bsdf.outputs['BSDF'], out.inputs['Surface'])
    return mat

def criar_mat(nome, cor, metallic=0.1, roughness=0.3, em=False, em_cor=None, em_str=3.0):
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

def construir_adapa_nadando():
    setup_cena()

    # Materiais
    m_escama  = criar_mat_escamas("M_Esc",  (0.04, 0.92, 0.75, 1), (0.01, 0.42, 0.52, 1))
    m_pele    = criar_mat("M_Pele",  (0.86, 0.68, 0.52, 1), roughness=0.42)
    m_barba   = criar_mat("M_Barba", (0.07, 0.05, 0.04, 1), roughness=0.72)
    m_ouro    = criar_mat("M_Ouro",  (0.98, 0.80, 0.20, 1), metallic=0.96, roughness=0.12)
    m_barbat  = criar_mat("M_Fin",   (0.95, 0.85, 0.30, 1), metallic=0.78, roughness=0.16)
    m_olho    = criar_mat("M_Olho",  (1.0, 0.50, 0.10, 1), em=True, em_cor=(1.0, 0.50, 0.10, 1), em_str=8.0)
    m_joia    = criar_mat("M_Joia",  (1.0, 0.35, 0.08, 1), em=True, em_cor=(1.0, 0.35, 0.08, 1), em_str=5.0)
    m_bolha   = criar_mat("M_Bolha", (0.3, 0.95, 1.0, 1), roughness=0.02, em=True, em_cor=(0.2, 0.88, 1.0, 1), em_str=5.5)
    m_vortice = criar_mat("M_Vort",  (0.4, 0.95, 1.0, 1), roughness=0.05, em=True, em_cor=(0.35, 0.9, 1.0, 1), em_str=3.2)

    # Root - horizontalmente orientado para nado
    # Adapa horizontal: corpo no eixo X, cabeca para direita (+X), cauda para esquerda (-X)
    root = bpy.data.objects.new("Adapa_Nado_Root", None)
    bpy.context.scene.collection.objects.link(root)

    # === CABECA (direita, x=+2.8) ===
    bpy.ops.mesh.primitive_uv_sphere_add(segments=24, ring_count=20, radius=0.42, location=(2.8, 0, 0.3))
    cabeca = bpy.context.active_object
    cabeca.data.materials.append(m_pele)
    cabeca.parent = root
    smooth(cabeca)

    # Tiara
    bpy.ops.mesh.primitive_cylinder_add(radius=0.44, depth=0.14, location=(2.8, 0, 0.58))
    tiara = bpy.context.active_object
    tiara.data.materials.append(m_ouro)
    tiara.parent = root
    smooth(tiara)

    # Gema
    bpy.ops.mesh.primitive_uv_sphere_add(radius=0.08, location=(2.8, -0.43, 0.62))
    gm = bpy.context.active_object
    gm.data.materials.append(m_joia)
    gm.parent = root

    # Olhos
    for s in [-1, 1]:
        bpy.ops.mesh.primitive_uv_sphere_add(radius=0.06, location=(2.8, s*0.16, 0.32))
        ol = bpy.context.active_object
        ol.data.materials.append(m_olho)
        ol.parent = root

    # Barba curta (nado)
    for i in range(3):
        bpy.ops.mesh.primitive_torus_add(major_radius=0.22-i*0.04, minor_radius=0.06, location=(2.5-i*0.18, -0.2+i*0.02, 0.0-i*0.12))
        ab = bpy.context.active_object
        ab.rotation_euler = (0, math.radians(90), 0)
        ab.data.materials.append(m_barba)
        ab.parent = root
        smooth(ab)

    # === TORSO AERODINAMICO ===
    bpy.ops.mesh.primitive_uv_sphere_add(segments=28, ring_count=22, radius=0.7, location=(1.2, 0, 0.1))
    torso = bpy.context.active_object
    torso.scale = (1.35, 0.78, 1.05)
    torso.data.materials.append(m_pele)
    torso.parent = root
    smooth(torso)

    # Peitoral
    bpy.ops.mesh.primitive_torus_add(major_radius=0.65, minor_radius=0.12, location=(1.2, 0.05, 0.3))
    peit = bpy.context.active_object
    peit.rotation_euler = (math.radians(90), 0, 0)
    peit.scale = (1.0, 1.0, 0.72)
    peit.data.materials.append(m_ouro)
    peit.parent = root
    smooth(peit)

    # Broche
    bpy.ops.mesh.primitive_uv_sphere_add(radius=0.12, location=(1.2, -0.52, 0.35))
    brc = bpy.context.active_object
    brc.data.materials.append(m_joia)
    brc.parent = root

    # === BRACO DISPARANDO - esticado para frente (direita +X) ===
    bpy.ops.mesh.primitive_cylinder_add(radius=0.13, depth=1.1, location=(2.45, -0.55, -0.45))
    braco = bpy.context.active_object
    braco.rotation_euler = (math.radians(-30), math.radians(70), math.radians(20))
    braco.data.materials.append(m_pele)
    braco.parent = root
    smooth(braco)

    bpy.ops.mesh.primitive_cylinder_add(radius=0.19, depth=0.3, location=(2.72, -0.9, -0.65))
    brac_puls = bpy.context.active_object
    brac_puls.rotation_euler = (math.radians(-30), math.radians(70), math.radians(20))
    brac_puls.data.materials.append(m_ouro)
    brac_puls.parent = root
    smooth(brac_puls)

    bpy.ops.mesh.primitive_uv_sphere_add(radius=0.17, location=(2.92, -1.15, -0.78))
    mao = bpy.context.active_object
    mao.data.materials.append(m_pele)
    mao.parent = root
    smooth(mao)

    # === CAUDA SERPENTINA (horizontal para esquerda, -X) ===
    cauda_segs = []
    cauda = [
        (-0.2, 0,  0.0, 0.60, 1.0),
        (-1.1, 0.2, 0.2, 0.50, 0.9),
        (-2.0, 0.5, 0.35, 0.40, 0.85),
        (-2.8, 0.85, 0.2, 0.30, 0.80),
        (-3.5, 1.0, -0.1, 0.20, 0.72),
        (-4.1, 0.85, -0.55, 0.13, 0.65),
    ]
    for lx, ly, lz, r1, dep in cauda:
        bpy.ops.mesh.primitive_cone_add(vertices=22, radius1=r1, radius2=r1*0.82, depth=dep, location=(lx, ly, lz))
        seg = bpy.context.active_object
        seg.rotation_euler = (0, math.radians(90), 0)
        seg.data.materials.append(m_escama)
        seg.parent = root
        smooth(seg)
        cauda_segs.append(seg)

    # Barbatanas caudais (esquerda)
    for s in [-1, 1]:
        bpy.ops.mesh.primitive_cone_add(vertices=4, radius1=0.55, depth=1.1, location=(-4.4, 0.72+s*0.3, -0.75))
        fin = bpy.context.active_object
        fin.scale = (0.06, 0.85, 1.0)
        fin.rotation_euler = (math.radians(s*35), math.radians(80), math.radians(s*12))
        fin.data.materials.append(m_barbat)
        fin.parent = root
        smooth(fin)

    # Crista dorsal (em cima do corpo)
    for i, x in enumerate([2.0, 1.2, 0.4, -0.4, -1.2]):
        tam = 0.28 - i*0.03
        bpy.ops.mesh.primitive_cone_add(vertices=3, radius1=0.14, depth=tam*2.0, location=(x, -0.45+i*0.05, 0.75+i*0.05))
        dors = bpy.context.active_object
        dors.scale = (0.06, 0.6, 1.0)
        dors.rotation_euler = (math.radians(8), math.radians(-90), 0)
        dors.data.materials.append(m_barbat)
        dors.parent = root
        smooth(dors)

    # === ESFERA DE AGUA / VORTICE (frente, direita) ===
    # Esfera central
    bpy.ops.mesh.primitive_ico_sphere_add(radius=0.65, subdivisions=4, location=(4.2, -1.0, -0.5))
    esfera = bpy.context.active_object
    esfera.data.materials.append(m_bolha)
    esfera.parent = root
    smooth(esfera)

    # Aneis de vortice
    aneis = [(3.6,-0.8,-0.3,0.5,0.045),(4.2,-1.0,-0.5,0.9,0.065),(4.9,-1.2,-0.6,1.25,0.08),(5.6,-1.4,-0.75,1.65,0.1)]
    for ax,ay,az,rMaj,rMin in aneis:
        bpy.ops.mesh.primitive_torus_add(major_radius=rMaj, minor_radius=rMin, location=(ax,ay,az))
        anel = bpy.context.active_object
        anel.rotation_euler = (math.radians(75), math.radians(12), math.radians(-20))
        anel.data.materials.append(m_vortice)
        anel.parent = root
        smooth(anel)

    # Bolhas disparadas em linha
    bolhas_dados = [(4.8,-0.6,-0.2,0.30),(5.4,-0.8,-0.45,0.38),(6.0,-1.0,-0.3,0.25),(6.6,-0.7,-0.55,0.44),(7.2,-0.9,-0.3,0.32)]
    for bx,by,bz,br in bolhas_dados:
        bpy.ops.mesh.primitive_uv_sphere_add(segments=18, ring_count=14, radius=br, location=(bx,by,bz))
        bl = bpy.context.active_object
        bl.data.materials.append(m_bolha)
        bl.parent = root
        smooth(bl)

    # === CAMERA WIDESCREEN - lateral, enquadrando acao de ponta a ponta ===
    # Centro da acao: X=1.4, Z=0.0
    bpy.ops.object.camera_add(location=(1.4, -9.5, 0.0))
    cam = bpy.context.active_object
    cam.rotation_euler = (math.radians(90), 0, 0)
    cam.data.lens = 28
    bpy.context.scene.camera = cam

    # Luzes
    bpy.ops.object.light_add(type='AREA', location=(3.0, -6.0, 4.5))
    kl = bpy.context.active_object
    kl.data.energy = 750
    kl.data.size = 4.5
    kl.data.color = (0.5, 0.92, 1.0)
    kl.rotation_euler = (math.radians(42), 0, math.radians(-22))

    bpy.ops.object.light_add(type='POINT', location=(4.2, -1.5, 0.5))
    burst = bpy.context.active_object
    burst.data.energy = 1400
    burst.data.color = (0.25, 1.0, 0.92)

    bpy.ops.object.light_add(type='AREA', location=(-2.0, 4.5, 3.0))
    rl = bpy.context.active_object
    rl.data.energy = 950
    rl.data.size = 4.0
    rl.data.color = (1.0, 0.75, 0.3)
    rl.rotation_euler = (math.radians(-40), 0, math.radians(148))

    # === RIM LIGHT + CICLO DE NADO REAL (Sprint 6, mesmo padrão do herói) ===
    for mat in (m_escama, m_pele, m_ouro, m_barbat):
        add_fresnel_rim(mat, HERO_RIM, power=1.5, strength=6.0)

    swim_cycle_keyframes(root, cauda_segs, frame_count=8, amp_deg=14.0,
                          wavelength_segs=3.0, cycles=1.0, bob_amp=0.05,
                          root_sway_deg=2.5)

    return {"root": root, "cauda_segs": cauda_segs}

def salvar_render(nome):
    base = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    bdir = os.path.join(base, "blends", "01_adapa_heroi")
    rdir = os.path.join(base, "renders")
    os.makedirs(bdir, exist_ok=True)
    os.makedirs(rdir, exist_ok=True)
    try:
        bpy.ops.wm.save_as_mainfile(filepath=os.path.join(bdir, nome+".blend"))
        print("Blend salvo: " + nome)
    except Exception as e:
        print("Aviso: " + str(e))
    bpy.context.scene.render.filepath = os.path.join(rdir, nome+".png")
    bpy.ops.render.render(write_still=True)
    print("Render salvo: " + nome)

if __name__ == "__main__":
    construir_adapa_nadando()
    salvar_render("01_adapa_nadando_disparo_bolhas")
    print("OK 01_adapa_nadando_disparo_bolhas v5.0 (+rim, +ciclo de nado real)")
