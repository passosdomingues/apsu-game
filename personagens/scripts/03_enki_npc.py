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
# 03 - ENKI: O ANCIAO DAS AGUAS & DEUS DA SABEDORIA (NPC v4.0)
# Paleta 60-30-10:
#   60% Azul-Royal Profundo Celestial (Manto das Aguas)
#   30% Marfim / Prata Reluzente (Barba Longa, Xale)
#   10% Dourado Mesopotamico & Ciano Astral (Coroa, Orbe, Runas)
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
    scene.view_settings.look = 'Medium High Contrast'

def criar_mat_manto(nome, cor_a, cor_b):
    mat = bpy.data.materials.new(name=nome)
    mat.use_nodes = True
    nodes = mat.node_tree.nodes
    links = mat.node_tree.links
    nodes.clear()
    out = nodes.new('ShaderNodeOutputMaterial')
    bsdf = nodes.new('ShaderNodeBsdfPrincipled')
    coord = nodes.new('ShaderNodeTexCoord')
    wave = nodes.new('ShaderNodeTexWave')
    ramp = nodes.new('ShaderNodeValToRGB')
    bump = nodes.new('ShaderNodeBump')
    wave.wave_type = 'RINGS'
    wave.inputs['Scale'].default_value = 5.0
    wave.inputs['Distortion'].default_value = 3.5
    ramp.color_ramp.elements[0].position = 0.25
    ramp.color_ramp.elements[0].color = cor_b
    ramp.color_ramp.elements[1].position = 0.8
    ramp.color_ramp.elements[1].color = cor_a
    bump.inputs['Strength'].default_value = 0.3
    if 'Metallic' in bsdf.inputs: bsdf.inputs['Metallic'].default_value = 0.35
    if 'Roughness' in bsdf.inputs: bsdf.inputs['Roughness'].default_value = 0.35
    links.new(coord.outputs['Object'], wave.inputs['Vector'])
    links.new(wave.outputs['Fac'], ramp.inputs['Fac'])
    links.new(ramp.outputs['Color'], bsdf.inputs['Base Color'])
    links.new(wave.outputs['Fac'], bump.inputs['Height'])
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

def construir_enki():
    setup_cena()

    m_manto  = criar_mat_manto("M_Manto", (0.04, 0.15, 0.58, 1), (0.01, 0.05, 0.28, 1))
    m_pele   = criar_mat("M_Pele",   (0.84, 0.67, 0.50, 1), roughness=0.55)
    m_barba  = criar_mat("M_Barba",  (0.94, 0.95, 0.98, 1), metallic=0.2, roughness=0.5)
    m_pedra  = criar_mat("M_Pedra",  (0.35, 0.32, 0.28, 1), roughness=0.8)
    m_ouro   = criar_mat("M_Ouro",   (0.98, 0.80, 0.20, 1), metallic=0.95, roughness=0.15)
    m_runa   = criar_mat("M_Runa",   (0.3, 0.9, 1.0, 1), em=True, em_cor=(0.3, 0.9, 1.0, 1), em_str=6.0)
    m_orbe   = criar_mat("M_Orbe",   (0.2, 0.85, 1.0, 1), roughness=0.05, em=True, em_cor=(0.2, 0.85, 1.0, 1), em_str=5.5)

    root = bpy.data.objects.new("Enki_Root", None)
    bpy.context.scene.collection.objects.link(root)

    # === MANTO / CORPO ===
    bpy.ops.mesh.primitive_cone_add(vertices=24, radius1=1.05, radius2=0.4, depth=3.0, location=(0, 0, 1.5))
    manto = bpy.context.active_object
    manto.scale = (1.0, 0.78, 1.0)
    manto.data.materials.append(m_manto)
    manto.parent = root
    smooth(manto)

    # Xale sobre ombros
    bpy.ops.mesh.primitive_torus_add(major_radius=0.72, minor_radius=0.16, location=(0, 0.05, 3.0))
    xale = bpy.context.active_object
    xale.rotation_euler = (math.radians(82), 0, 0)
    xale.scale = (0.95, 0.72, 1.0)
    xale.data.materials.append(m_barba)
    xale.parent = root
    smooth(xale)

    # Broche
    bpy.ops.mesh.primitive_ico_sphere_add(radius=0.13, subdivisions=3, location=(0.28, -0.5, 3.08))
    brc = bpy.context.active_object
    brc.data.materials.append(m_ouro)
    brc.parent = root
    smooth(brc)

    # === CABECA ===
    bpy.ops.mesh.primitive_uv_sphere_add(segments=28, ring_count=24, radius=0.44, location=(0, -0.05, 3.78))
    cabeca = bpy.context.active_object
    cabeca.data.materials.append(m_pele)
    cabeca.parent = root
    smooth(cabeca)

    # Olhos
    for s in [-1, 1]:
        bpy.ops.mesh.primitive_uv_sphere_add(radius=0.065, location=(s*0.16, -0.44, 3.82))
        ol = bpy.context.active_object
        ol.data.materials.append(m_runa)
        ol.parent = root

    # === COROA TRIPLA MESOPOTAMICA ===
    bpy.ops.mesh.primitive_cylinder_add(radius=0.47, depth=0.42, location=(0, -0.05, 4.12))
    coroa = bpy.context.active_object
    coroa.data.materials.append(m_ouro)
    coroa.parent = root
    smooth(coroa)

    # Chifres da coroa (3 pares)
    chifre_dados = [(0.42, 4.28, 0.12, 0.45), (0.38, 4.48, 0.10, 0.38), (0.30, 4.65, 0.08, 0.30)]
    for s in [-1, 1]:
        for xd, z, r, dep in chifre_dados:
            bpy.ops.mesh.primitive_cone_add(vertices=8, radius1=r, depth=dep, location=(s*xd, -0.05, z))
            ch = bpy.context.active_object
            ch.rotation_euler = (0, 0, math.radians(s*30))
            ch.data.materials.append(m_ouro)
            ch.parent = root
            smooth(ch)

    # Halo de luz
    bpy.ops.mesh.primitive_torus_add(major_radius=1.2, minor_radius=0.032, location=(0, -0.05, 4.35))
    halo = bpy.context.active_object
    halo.rotation_euler = (math.radians(12), math.radians(8), 0)
    halo.data.materials.append(m_runa)
    halo.parent = root
    smooth(halo)

    # === BARBA EM CAMADAS LONGAS ===
    for i in range(7):
        z = 3.38 - i*0.28
        r = 0.36 - i*0.038
        if r < 0.08: r = 0.08
        bpy.ops.mesh.primitive_cone_add(vertices=14, radius1=r, depth=0.45, location=(0, -0.28+i*0.02, z))
        cb = bpy.context.active_object
        cb.rotation_euler = (math.radians(12), 0, 0)
        cb.data.materials.append(m_barba)
        cb.parent = root
        smooth(cb)

    # === TABULETA CUNEIFORME ===
    tab_root = bpy.data.objects.new("Tab_Root", None)
    bpy.context.scene.collection.objects.link(tab_root)
    tab_root.parent = root
    tab_root.location = (0.78, -0.55, 2.85)
    tab_root.rotation_euler = (math.radians(28), math.radians(-22), math.radians(18))

    bpy.ops.mesh.primitive_cube_add(size=0.65, location=(0, 0, 0))
    tab = bpy.context.active_object
    tab.scale = (0.78, 0.12, 1.18)
    tab.data.materials.append(m_pedra)
    tab.parent = tab_root
    smooth(tab)

    for row in range(3):
        for col in range(3):
            bpy.ops.mesh.primitive_cube_add(size=0.058, location=(-0.16+col*0.16, -0.08, -0.22+row*0.22))
            rn = bpy.context.active_object
            rn.scale = (1.0, 0.4, 0.55)
            rn.data.materials.append(m_runa)
            rn.parent = tab_root

    # === CAJADO ===
    caj_root = bpy.data.objects.new("Caj_Root", None)
    bpy.context.scene.collection.objects.link(caj_root)
    caj_root.parent = root
    caj_root.location = (-0.85, -0.35, 1.5)
    caj_root.rotation_euler = (math.radians(4), 0, math.radians(-8))

    bpy.ops.mesh.primitive_cylinder_add(radius=0.038, depth=3.4, location=(0, 0, 0))
    haste = bpy.context.active_object
    haste.data.materials.append(m_ouro)
    haste.parent = caj_root
    smooth(haste)

    bpy.ops.mesh.primitive_torus_add(major_radius=0.33, minor_radius=0.055, location=(0, 0, 1.7))
    lua = bpy.context.active_object
    lua.rotation_euler = (0, math.radians(90), 0)
    lua.data.materials.append(m_ouro)
    lua.parent = caj_root
    smooth(lua)

    bpy.ops.mesh.primitive_ico_sphere_add(radius=0.21, subdivisions=3, location=(0, 0, 1.7))
    orb = bpy.context.active_object
    orb.data.materials.append(m_orbe)
    orb.parent = caj_root
    smooth(orb)

    # Particulas de agua ao redor do orbe
    for bx, by, bz, br in [(0.35,0,1.9,0.08),(0.28,-0.18,1.95,0.06),(-0.22,0.15,2.02,0.07)]:
        bpy.ops.mesh.primitive_uv_sphere_add(radius=br, location=(bx,by,bz+caj_root.location[2]-1.7))
        p = bpy.context.active_object
        p.data.materials.append(m_orbe)
        p.parent = caj_root
        smooth(p)

    # === CAMERA PORTRAIT FIXA ===
    # Enki ocupa z=0 ate z=5.0 (cajado extende mais)
    bpy.ops.object.camera_add(location=(0, -8.5, 2.4))
    cam = bpy.context.active_object
    cam.rotation_euler = (math.radians(90), 0, 0)
    cam.data.lens = 42
    bpy.context.scene.camera = cam

    # Luzes
    bpy.ops.object.light_add(type='AREA', location=(1.5, -5.0, 6.0))
    kl = bpy.context.active_object
    kl.data.energy = 650
    kl.data.size = 4.0
    kl.data.color = (0.55, 0.82, 1.0)
    kl.rotation_euler = (math.radians(40), 0, math.radians(-20))

    bpy.ops.object.light_add(type='AREA', location=(-3.5, 4.5, 4.5))
    rl = bpy.context.active_object
    rl.data.energy = 800
    rl.data.size = 4.5
    rl.data.color = (1.0, 0.85, 0.4)
    rl.rotation_euler = (math.radians(-40), 0, math.radians(150))

    bpy.ops.object.light_add(type='POINT', location=(0, -2.0, 1.8))
    fl = bpy.context.active_object
    fl.data.energy = 180
    fl.data.color = (0.2, 0.5, 1.0)

    # Enki e o mentor divino, aparece em todas as fases — reusa o
    # dourado "divino" medido pra fase 5 (tema Enki/templo), consistente
    # em qualquer fundo em que seu dialogo apareca.
    ENKI_RIM = (1.0, 0.85, 0.35)
    for mat in (m_manto, m_pele, m_pedra):
        add_fresnel_rim(mat, ENKI_RIM, power=1.5, strength=6.0)

    # === VIDA IDLE (2026-08-16, pedido do Rafa: "dá mais vida, mais
    # personalidade" pros NPCs de manto) === Enki não tem cauda — usa
    # balanço de manto (como tecido boiando) + respiração leve no
    # próprio manto (dobra como torso) + inclinação de cabeça defasada,
    # em vez do ciclo de nado anguiliforme (que não se aplica aqui).
    add_idle_life_keyframes(root, manto_obj=manto, torso_obj=manto,
                             cabeca_obj=cabeca, frame_count=8,
                             sway_deg=3.0, breathe_amount=0.018,
                             head_tilt_deg=2.2, cycles=1.0)

def salvar_render(nome):
    base = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    bdir = os.path.join(base, "blends", "03_enki_npc")
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
    construir_enki()
    salvar_render("03_enki_npc")
    print("OK 03_enki_npc v5.0 (+ rim light)")
