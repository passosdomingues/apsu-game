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
# 04B - INIMIGOS VARIACOES v5.0
# Peixe Sombrio + Enguia Abissal + Caranguejo Blindado + Medusa Eletrica
# Cada variacao usa a mesma logica de construcao mas com paleta propria
# Salva em: blends/04_peixe_sombrio_inimigo/
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

def criar_mat_voronoi(nome, cor_a, cor_b, escala=16.0, metallic=0.3, roughness=0.38):
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
    vor.inputs['Scale'].default_value = escala
    ramp.color_ramp.elements[0].position = 0.2
    ramp.color_ramp.elements[0].color = cor_b
    ramp.color_ramp.elements[1].position = 0.8
    ramp.color_ramp.elements[1].color = cor_a
    bump.inputs['Strength'].default_value = 0.42
    if 'Metallic' in bsdf.inputs: bsdf.inputs['Metallic'].default_value = metallic
    if 'Roughness' in bsdf.inputs: bsdf.inputs['Roughness'].default_value = roughness
    links.new(coord.outputs['Object'], vor.inputs['Vector'])
    links.new(vor.outputs['Distance'], ramp.inputs['Fac'])
    links.new(ramp.outputs['Color'], bsdf.inputs['Base Color'])
    links.new(vor.outputs['Distance'], bump.inputs['Height'])
    links.new(bump.outputs['Normal'], bsdf.inputs['Normal'])
    links.new(bsdf.outputs['BSDF'], out.inputs['Surface'])
    return mat

def smooth(obj):
    if obj and obj.type == 'MESH':
        for p in obj.data.polygons: p.use_smooth = True

def add_camara_iluminacao_peixe():
    bpy.ops.object.camera_add(location=(0.8, -6.5, 0.25))
    cam = bpy.context.active_object
    cam.rotation_euler = (math.radians(90), 0, math.radians(8))
    cam.data.lens = 44
    bpy.context.scene.camera = cam

    bpy.ops.object.light_add(type='AREA', location=(1.5, -4.5, 2.8))
    kl = bpy.context.active_object
    kl.data.energy = 480; kl.data.size = 3.5
    kl.rotation_euler = (math.radians(45), 0, math.radians(-25))

    bpy.ops.object.light_add(type='AREA', location=(-2.2, 3.5, 2.0))
    rl = bpy.context.active_object
    rl.data.energy = 650; rl.data.size = 3.0

# ----------------------------------------------------------------
# ENGUIA ABISSAL — Proporção Anatômica & Enquadramento Aprimorado
# ----------------------------------------------------------------
def construir_enguia():
    setup_cena()
    m_corpo = criar_mat_voronoi("M_Eng", (0.02, 0.04, 0.18, 1), (0.0, 0.01, 0.08, 1), escala=8.0)
    m_gelo  = criar_mat("M_Gelo", (0.3, 0.65, 1.0, 1), metallic=0.55, roughness=0.12)
    m_elec  = criar_mat("M_Elec", (0.2, 0.8, 1.0, 1), em=True, em_cor=(0.2, 0.8, 1.0, 1), em_str=8.0)
    # Rim = cor eletrica propria (reforca o accent bioluminescente ja
    # desenhado, agora tambem na borda do corpo/nadadeiras).
    for mat in (m_corpo, m_gelo):
        add_fresnel_rim(mat, (0.2, 0.8, 1.0), power=1.4, strength=6.5)

    root = bpy.data.objects.new("Enguia_Root", None)
    bpy.context.scene.collection.objects.link(root)

    # Segmentos serpentinos mais robustos e anatomicamente coerentes
    segmentos = [
        (2.4,   0.0,  0.0,  0.55, 0.95),
        (1.5,   0.12, 0.12, 0.48, 0.95),
        (0.65,  0.35, 0.18, 0.42, 0.95),
        (-0.2,  0.60, 0.08, 0.35, 0.90),
        (-1.0,  0.80, -0.1, 0.26, 0.85),
        (-1.75, 0.90, -0.25, 0.18, 0.78),
    ]
    enguia_segs = []
    for lx, ly, lz, r1, dep in segmentos:
        bpy.ops.mesh.primitive_cone_add(vertices=20, radius1=r1, radius2=r1*0.85, depth=dep, location=(lx, ly, lz))
        seg = bpy.context.active_object
        seg.rotation_euler = (0, math.radians(90), 0)
        seg.data.materials.append(m_corpo)
        seg.parent = root
        smooth(seg)
        enguia_segs.append(seg)

    # Cabeça robusta de predador abissal
    bpy.ops.mesh.primitive_uv_sphere_add(radius=0.58, location=(3.15, -0.05, 0.02))
    cab = bpy.context.active_object
    cab.scale = (1.2, 0.82, 0.85)
    cab.data.materials.append(m_corpo)
    cab.parent = root
    smooth(cab)

    # Olhos bioluminescentes (2026-08-16: aumentados e empurrados pra fora —
    # Rafa relatou "parece que ficou sem olho" depois do ciclo de nado; o
    # olho existia mas era pequeno (raio 0.10) e ficava quase encostado na
    # superfície da cabeça, fácil de sumir visualmente/em resoluções baixas)
    for s in [-1, 1]:
        bpy.ops.mesh.primitive_uv_sphere_add(radius=0.15, location=(3.35, s*0.46, 0.20))
        ol = bpy.context.active_object
        ol.data.materials.append(m_elec)
        ol.parent = root
        smooth(ol)

    # Dentes afiados
    for i in range(5):
        bpy.ops.mesh.primitive_cone_add(vertices=4, radius1=0.035, depth=0.16, location=(3.2+0.08, -0.65+i*0.12*(-1 if i%2 else 1), -0.08+i*0.04))
        d = bpy.context.active_object
        d.rotation_euler = (math.radians(25), 0, 0)
        d.data.materials.append(criar_mat("M_Den", (0.85, 0.95, 1.0, 1), roughness=0.18))
        d.parent = root

    # Barbatanas peitorais desenvolvidas
    for s in [-1, 1]:
        bpy.ops.mesh.primitive_cone_add(vertices=4, radius1=0.35, depth=0.68, location=(1.5, s*0.28, 0))
        fin = bpy.context.active_object
        fin.rotation_euler = (0, math.radians(s*45), 0)
        fin.scale = (0.05, 0.95, 0.85)
        fin.data.materials.append(m_gelo)
        fin.parent = root
        smooth(fin)

    # Cauda bifurcada
    cauda_bifurcada = []
    for s in [-1, 1]:
        bpy.ops.mesh.primitive_cone_add(vertices=4, radius1=0.38, depth=0.82, location=(-1.95, 0.85+s*0.3, -0.3+s*0.1))
        ct = bpy.context.active_object
        ct.scale = (0.05, 0.9, 0.95)
        ct.rotation_euler = (math.radians(s*22), math.radians(95), 0)
        ct.data.materials.append(m_gelo)
        ct.parent = root
        smooth(ct)
        cauda_bifurcada.append(ct)

    # Orbes elétricos na crista
    for ox, oy, oz, r in [(3.6, -0.05, 0.02, 0.15), (0.5, 0.4, 0.25, 0.10), (-0.8, 0.72, -0.05, 0.09)]:
        bpy.ops.mesh.primitive_uv_sphere_add(radius=r, location=(ox, oy, oz))
        orb = bpy.context.active_object
        orb.data.materials.append(m_elec)
        orb.parent = root
        smooth(orb)

    # Câmera ajustada para preenchimento natural do enquadramento
    bpy.ops.object.camera_add(location=(0.65, -4.8, 0.1))
    cam = bpy.context.active_object
    cam.rotation_euler = (math.radians(90), 0, 0)
    cam.data.lens = 42
    bpy.context.scene.camera = cam

    bpy.ops.object.light_add(type='AREA', location=(1.0, -5.0, 3.0))
    kl = bpy.context.active_object
    kl.data.energy = 550; kl.data.size = 4.0
    kl.data.color = (0.3, 0.7, 1.0)
    bpy.ops.object.light_add(type='AREA', location=(-2.0, 4.0, 2.0))
    rl = bpy.context.active_object
    rl.data.energy = 700; rl.data.size = 3.5
    rl.data.color = (0.2, 0.5, 1.0)
    bpy.ops.object.light_add(type='POINT', location=(0.65, 0.45, 0.1))
    pl = bpy.context.active_object
    pl.data.energy = 280; pl.data.color = (0.2, 0.8, 1.0)

    # === CICLO DE NADO (2026-08-16, pedido do Rafa) ===
    # Corpo (6 segs, cabeça->cauda) + os 2 lóbulos da cauda bifurcada no
    # fim da cadeia (maior amplitude, igual ponta de cauda de peixe de
    # verdade). Enguia é o candidato mais natural pra onda anguiliforme
    # de todo o elenco — é literalmente a criatura que dá nome à técnica.
    swim_cycle_keyframes(root, enguia_segs + cauda_bifurcada, frame_count=8,
                          amp_deg=16.0, wavelength_segs=3.2, cycles=1.0,
                          bob_amp=0.05, root_sway_deg=2.0)


# ----------------------------------------------------------------
# CARANGUEJO BLINDADO
# ----------------------------------------------------------------
def construir_caranguejo():
    setup_cena()
    m_casco  = criar_mat_voronoi("M_Casco", (0.15, 0.08, 0.28, 1), (0.06, 0.02, 0.12, 1), escala=6.0, metallic=0.65, roughness=0.25)
    m_garra  = criar_mat("M_Garra", (0.55, 0.12, 0.06, 1), metallic=0.72, roughness=0.18)
    m_nucleo = criar_mat("M_Nucleo", (1.0, 0.38, 0.0, 1), em=True, em_cor=(1.0, 0.38, 0.0, 1), em_str=7.0)
    for mat in (m_casco, m_garra):
        add_fresnel_rim(mat, (1.0, 0.38, 0.0), power=1.4, strength=6.5)

    root = bpy.data.objects.new("Caran_Root", None)
    bpy.context.scene.collection.objects.link(root)

    # Carapaca central
    bpy.ops.mesh.primitive_uv_sphere_add(segments=24, ring_count=18, radius=0.88, location=(0, 0, 0))
    carap = bpy.context.active_object
    carap.scale = (1.25, 1.0, 0.65)
    carap.data.materials.append(m_casco)
    carap.parent = root
    smooth(carap)

    # Nucleo emissivo no centro
    bpy.ops.mesh.primitive_ico_sphere_add(radius=0.22, subdivisions=3, location=(0, -0.62, 0.12))
    nucleo = bpy.context.active_object
    nucleo.data.materials.append(m_nucleo)
    nucleo.parent = root
    smooth(nucleo)

    # Olhos em pedunculos
    for s in [-1, 1]:
        bpy.ops.mesh.primitive_cylinder_add(radius=0.045, depth=0.35, location=(s*0.4, -0.68, 0.45))
        ped = bpy.context.active_object
        ped.rotation_euler = (math.radians(-12), 0, math.radians(s*18))
        ped.data.materials.append(m_casco)
        ped.parent = root
        smooth(ped)
        bpy.ops.mesh.primitive_uv_sphere_add(radius=0.1, location=(s*0.46, -0.82, 0.65))
        ol = bpy.context.active_object
        ol.data.materials.append(m_nucleo)
        ol.parent = root
        smooth(ol)

    # Garras (2 grandes frontais)
    for s in [-1, 1]:
        # Braco
        bpy.ops.mesh.primitive_cylinder_add(radius=0.16, depth=0.95, location=(s*1.05, -0.3, 0.05))
        brc = bpy.context.active_object
        brc.rotation_euler = (math.radians(12), math.radians(s*35), 0)
        brc.data.materials.append(m_garra)
        brc.parent = root
        smooth(brc)
        # Garra superior
        bpy.ops.mesh.primitive_cone_add(vertices=4, radius1=0.08, depth=0.55, location=(s*1.62, -0.68, 0.28))
        gt = bpy.context.active_object
        gt.rotation_euler = (math.radians(-35), math.radians(s*25), 0)
        gt.data.materials.append(m_garra)
        gt.parent = root
        smooth(gt)
        # Garra inferior
        bpy.ops.mesh.primitive_cone_add(vertices=4, radius1=0.065, depth=0.42, location=(s*1.58, -0.58, 0.08))
        gb = bpy.context.active_object
        gb.rotation_euler = (math.radians(22), math.radians(s*25), 0)
        gb.data.materials.append(m_garra)
        gb.parent = root
        smooth(gb)

    # Pernas (3 pares laterais)
    for i in range(3):
        for s in [-1, 1]:
            bpy.ops.mesh.primitive_cylinder_add(radius=0.048, depth=0.88, location=(s*(0.92+i*0.22), 0.35-i*0.2, -0.32))
            perna = bpy.context.active_object
            perna.rotation_euler = (math.radians(-12+i*5), math.radians(s*(55+i*12)), 0)
            perna.data.materials.append(m_casco)
            perna.parent = root
            smooth(perna)

    # Camera para caranguejo (widescreen baixo, vista ligeiramente de cima)
    bpy.ops.object.camera_add(location=(0, -6.8, 1.8))
    cam = bpy.context.active_object
    cam.rotation_euler = (math.radians(75), 0, 0)
    cam.data.lens = 40
    bpy.context.scene.camera = cam

    bpy.ops.object.light_add(type='AREA', location=(1.5, -4.5, 3.5))
    kl = bpy.context.active_object
    kl.data.energy = 520; kl.data.size = 3.8
    kl.data.color = (0.75, 0.55, 1.0)
    bpy.ops.object.light_add(type='AREA', location=(-2.5, 3.5, 2.5))
    rl = bpy.context.active_object
    rl.data.energy = 680; rl.data.size = 3.2
    rl.data.color = (1.0, 0.4, 0.2)
    bpy.ops.object.light_add(type='POINT', location=(0, -0.62, 0.3))
    pl = bpy.context.active_object
    pl.data.energy = 350; pl.data.color = (1.0, 0.38, 0.0)

# ----------------------------------------------------------------
# MEDUSA ELETRICA
# ----------------------------------------------------------------
def construir_medusa():
    setup_cena()
    m_corpo  = criar_mat("M_MedCorpo", (0.05, 0.02, 0.22, 1), metallic=0.2, roughness=0.12)
    m_tent   = criar_mat("M_MedTent",  (0.28, 0.08, 0.55, 1), metallic=0.3, roughness=0.25)
    m_neon   = criar_mat("M_MedNeon",  (0.0, 0.9, 1.0, 1), em=True, em_cor=(0.0, 0.9, 1.0, 1), em_str=8.5)
    m_anel   = criar_mat("M_MedAnel",  (0.3, 0.1, 0.65, 1), metallic=0.4, roughness=0.18, em=True, em_cor=(0.5, 0.2, 1.0, 1), em_str=3.5)
    for mat in (m_corpo, m_tent):
        add_fresnel_rim(mat, (0.0, 0.9, 1.0), power=1.4, strength=6.5)

    root = bpy.data.objects.new("Medusa_Root", None)
    bpy.context.scene.collection.objects.link(root)

    # Campana translucida
    bpy.ops.mesh.primitive_uv_sphere_add(segments=24, ring_count=20, radius=0.92, location=(0, 0, 0))
    camp = bpy.context.active_object
    camp.scale = (1.0, 1.0, 0.72)
    camp.data.materials.append(m_corpo)
    camp.parent = root
    smooth(camp)

    # Aneis bioluminescentes concentricos
    for i, r in enumerate([0.28, 0.52, 0.72]):
        bpy.ops.mesh.primitive_torus_add(major_radius=r, minor_radius=0.032+i*0.008, location=(0, 0, -0.12+i*0.08))
        anel = bpy.context.active_object
        anel.data.materials.append(m_anel)
        anel.parent = root
        smooth(anel)

    # Nucleo central
    bpy.ops.mesh.primitive_uv_sphere_add(radius=0.28, location=(0, 0, -0.05))
    nucleo = bpy.context.active_object
    nucleo.data.materials.append(m_neon)
    nucleo.parent = root
    smooth(nucleo)

    # Tentaculos longos (6)
    tentaculos = [
        (0,    -0.7, -1.0, -0.55, -1.85),
        (0.5,  -0.5, -0.85, -0.35, -1.7),
        (0.8,   0.0, -0.72, 0.0,  -1.55),
        (-0.5, -0.5, -0.85, -0.35, -1.7),
        (-0.8,  0.0, -0.72, 0.0,  -1.55),
        (0.0,   0.6, -0.85, 0.45, -1.7),
    ]
    for i, (tx, ty, tz1, mid_x, tz2) in enumerate(tentaculos):
        # Segmento superior
        bpy.ops.mesh.primitive_cylinder_add(radius=0.048, depth=0.92, location=(tx*0.4, ty*0.5, tz1))
        t1 = bpy.context.active_object
        t1.data.materials.append(m_tent if i%2==0 else m_anel)
        t1.parent = root
        smooth(t1)
        # Segmento inferior
        bpy.ops.mesh.primitive_cylinder_add(radius=0.030, depth=0.9, location=(tx*0.65, ty*0.8, tz2))
        t2 = bpy.context.active_object
        t2.data.materials.append(m_neon if i%3==0 else m_tent)
        t2.parent = root
        smooth(t2)

    # Camera centrada (widescreen, captura cupula + tentaculos completos ate z=-2.3)
    bpy.ops.object.camera_add(location=(0, -8.0, -0.85))
    cam = bpy.context.active_object
    cam.rotation_euler = (math.radians(90), 0, 0)
    cam.data.lens = 38
    bpy.context.scene.camera = cam

    bpy.ops.object.light_add(type='AREA', location=(1.2, -5.0, 3.5))
    kl = bpy.context.active_object
    kl.data.energy = 450; kl.data.size = 3.5
    kl.data.color = (0.4, 0.8, 1.0)
    bpy.ops.object.light_add(type='AREA', location=(-2.0, 3.8, 2.5))
    rl = bpy.context.active_object
    rl.data.energy = 580; rl.data.size = 3.0
    rl.data.color = (0.6, 0.2, 1.0)
    bpy.ops.object.light_add(type='POINT', location=(0, 0, -0.2))
    pl = bpy.context.active_object
    pl.data.energy = 450; pl.data.color = (0.0, 0.9, 1.0)

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
    construir_enguia()
    salvar("04_inimigo_enguia_abissal")

    construir_caranguejo()
    salvar("04_inimigo_caranguejo_blindado")

    construir_medusa()
    salvar("04_inimigo_medusa_eletrica")

    print("OK 04_inimigos_variacoes v5.0")
