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
# 05 - GUARDIAO ATLANTE: ESTATUA VIVA & NPC SECUNDARIO (v5.0)
# Paleta 60-30-10:
#   60% Arenito / Pedra Ancestral Envelhecida (Corpo Colunar)
#   30% Patina de Cobre / Verde-Teal Oxidado (Faixas, Musgo)
#   10% Dourado Solar Emissivo (Olhos, Runas Cuneiformes)
# Camera PORTRAIT 1080x1920 - personagem ocupa z=0 ate z=4.8
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

def criar_mat_pedra(nome, cor_a, cor_b, rugosidade=0.82):
    mat = bpy.data.materials.new(name=nome)
    mat.use_nodes = True
    nodes = mat.node_tree.nodes
    links = mat.node_tree.links
    nodes.clear()
    out   = nodes.new('ShaderNodeOutputMaterial')
    bsdf  = nodes.new('ShaderNodeBsdfPrincipled')
    coord = nodes.new('ShaderNodeTexCoord')
    noise = nodes.new('ShaderNodeTexNoise')
    ramp  = nodes.new('ShaderNodeValToRGB')
    bump  = nodes.new('ShaderNodeBump')
    noise.inputs['Scale'].default_value = 12.0
    noise.inputs['Detail'].default_value = 7.0
    noise.inputs['Roughness'].default_value = 0.68
    ramp.color_ramp.elements[0].position = 0.3
    ramp.color_ramp.elements[0].color = cor_b
    ramp.color_ramp.elements[1].position = 0.75
    ramp.color_ramp.elements[1].color = cor_a
    bump.inputs['Strength'].default_value = 0.55
    if 'Metallic' in bsdf.inputs: bsdf.inputs['Metallic'].default_value = 0.08
    if 'Roughness' in bsdf.inputs: bsdf.inputs['Roughness'].default_value = rugosidade
    links.new(coord.outputs['Object'], noise.inputs['Vector'])
    links.new(noise.outputs['Fac'], ramp.inputs['Fac'])
    links.new(ramp.outputs['Color'], bsdf.inputs['Base Color'])
    links.new(noise.outputs['Fac'], bump.inputs['Height'])
    links.new(bump.outputs['Normal'], bsdf.inputs['Normal'])
    links.new(bsdf.outputs['BSDF'], out.inputs['Surface'])
    return mat

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

def smooth(obj):
    if obj and obj.type == 'MESH':
        for p in obj.data.polygons: p.use_smooth = True

def construir_guardiao(variacao='padrao'):
    setup_cena()

    if variacao == 'padrao':
        m_pedra = criar_mat_pedra("M_Ped", (0.68, 0.60, 0.48, 1), (0.44, 0.36, 0.28, 1))
        m_patina = criar_mat("M_Pat", (0.10, 0.38, 0.34, 1), metallic=0.38, roughness=0.52)
        m_runa   = criar_mat("M_Run", (0.96, 0.76, 0.16, 1), metallic=0.75, roughness=0.18, em=True, em_cor=(0.96, 0.76, 0.16, 1), em_str=6.5)
    elif variacao == 'coral':
        m_pedra  = criar_mat_pedra("M_Ped", (0.48, 0.18, 0.12, 1), (0.28, 0.06, 0.04, 1))
        m_patina = criar_mat("M_Pat", (1.0, 0.32, 0.12, 1), metallic=0.22, roughness=0.45)
        m_runa   = criar_mat("M_Run", (1.0, 0.82, 0.0, 1), em=True, em_cor=(1.0, 0.82, 0.0, 1), em_str=7.0)
    else:  # lamassu
        m_pedra  = criar_mat_pedra("M_Ped", (0.72, 0.68, 0.52, 1), (0.50, 0.46, 0.32, 1))
        m_patina = criar_mat("M_Pat", (0.55, 0.75, 0.85, 1), metallic=0.55, roughness=0.32)
        m_runa   = criar_mat("M_Run", (0.25, 0.9, 1.0, 1), em=True, em_cor=(0.25, 0.9, 1.0, 1), em_str=6.0)

    # Rim = cor da runa da propria variante (mesma logica das outras
    # criaturas: reforca o accent que o design ja tinha, na silhueta).
    _rim = tuple(m_runa.node_tree.nodes.get("Principled BSDF").inputs['Emission Color'].default_value)[:3]
    for mat in (m_pedra, m_patina):
        add_fresnel_rim(mat, _rim, power=1.5, strength=6.0)

    root = bpy.data.objects.new("Guard_Root", None)
    bpy.context.scene.collection.objects.link(root)

    # === BASE PEDESTAL ===
    bpy.ops.mesh.primitive_cylinder_add(radius=1.05, depth=0.52, vertices=12, location=(0, 0, 0.26))
    base = bpy.context.active_object
    base.data.materials.append(m_pedra)
    base.parent = root
    smooth(base)

    # Degrau da base
    bpy.ops.mesh.primitive_cylinder_add(radius=0.88, depth=0.28, vertices=12, location=(0, 0, 0.66))
    deg = bpy.context.active_object
    deg.data.materials.append(m_pedra)
    deg.parent = root
    smooth(deg)

    # === CORPO COLUNAR (60%) ===
    bpy.ops.mesh.primitive_cylinder_add(radius=0.62, depth=2.6, vertices=14, location=(0, 0, 2.1))
    corpo = bpy.context.active_object
    corpo.scale = (1.0, 0.78, 1.0)
    corpo.data.materials.append(m_pedra)
    corpo.parent = root
    smooth(corpo)

    # Faixa de Patina central (30%)
    bpy.ops.mesh.primitive_torus_add(major_radius=0.64, minor_radius=0.115, location=(0, 0, 2.15))
    faixa = bpy.context.active_object
    faixa.scale = (1.0, 0.78, 1.0)
    faixa.data.materials.append(m_patina)
    faixa.parent = root
    smooth(faixa)

    # Segunda faixa
    bpy.ops.mesh.primitive_torus_add(major_radius=0.62, minor_radius=0.075, location=(0, 0, 1.5))
    faixa2 = bpy.context.active_object
    faixa2.scale = (1.0, 0.78, 1.0)
    faixa2.data.materials.append(m_patina)
    faixa2.parent = root
    smooth(faixa2)

    # === CABECA TOTEM MESOPOTAMICA ===
    bpy.ops.mesh.primitive_cube_add(size=0.72, location=(0, 0, 3.65))
    cabeca = bpy.context.active_object
    cabeca.scale = (1.0, 0.82, 1.12)
    cabeca.data.materials.append(m_pedra)
    cabeca.parent = root
    smooth(cabeca)

    # Olhos Emissivos (10%)
    for s in [-1, 1]:
        bpy.ops.mesh.primitive_uv_sphere_add(radius=0.082, location=(s*0.20, -0.48, 3.72))
        ol = bpy.context.active_object
        ol.data.materials.append(m_runa)
        ol.parent = root
        smooth(ol)

    # Boca / Inscricao
    bpy.ops.mesh.primitive_cube_add(size=0.18, location=(0, -0.48, 3.44))
    boca = bpy.context.active_object
    boca.scale = (1.6, 0.35, 0.45)
    boca.data.materials.append(m_patina)
    boca.parent = root

    # === ELMO / TIARA (30% Patina, topo completo) ===
    bpy.ops.mesh.primitive_cylinder_add(radius=0.55, depth=0.40, vertices=12, location=(0, 0, 4.16))
    elmo = bpy.context.active_object
    elmo.data.materials.append(m_patina)
    elmo.parent = root
    smooth(elmo)

    # Chifres da tiara
    for s in [-1, 1]:
        bpy.ops.mesh.primitive_cone_add(vertices=8, radius1=0.12, depth=0.52, location=(s*0.42, 0, 4.5))
        ch = bpy.context.active_object
        ch.rotation_euler = (0, 0, math.radians(s*28))
        ch.data.materials.append(m_patina)
        ch.parent = root
        smooth(ch)

    # Gema central da tiara (10%)
    bpy.ops.mesh.primitive_ico_sphere_add(radius=0.10, subdivisions=3, location=(0, -0.48, 4.22))
    gem = bpy.context.active_object
    gem.data.materials.append(m_runa)
    gem.parent = root
    smooth(gem)

    # === BRACOS SENTINELA ===
    for s in [-1, 1]:
        bpy.ops.mesh.primitive_cylinder_add(radius=0.20, depth=1.45, location=(s*0.78, -0.08, 2.0))
        brc = bpy.context.active_object
        brc.rotation_euler = (math.radians(32), math.radians(s*48), 0)
        brc.data.materials.append(m_pedra)
        brc.parent = root
        smooth(brc)
        # Bracadeiras de patina
        bpy.ops.mesh.primitive_cylinder_add(radius=0.24, depth=0.20, location=(s*1.08, -0.22, 1.55))
        brac = bpy.context.active_object
        brac.rotation_euler = (math.radians(32), math.radians(s*48), 0)
        brac.data.materials.append(m_patina)
        brac.parent = root
        smooth(brac)

    # === RUNAS CUNEIFORMES NO PEITO (10% - maiores que v3) ===
    runas = [
        (-0.28, -0.50, 2.55), (0.0, -0.50, 2.55), (0.28, -0.50, 2.55),
        (-0.14, -0.50, 2.30), (0.14, -0.50, 2.30),
        (0.0,   -0.50, 2.05),
    ]
    for rx, ry, rz in runas:
        bpy.ops.mesh.primitive_cube_add(size=0.095, location=(rx, ry, rz))
        runa = bpy.context.active_object
        runa.scale = (1.0, 0.28, 0.68)
        runa.data.materials.append(m_runa)
        runa.parent = root

    # === CAMERA PORTRAIT FIXA ===
    # Personagem: z=0 ate z=4.85 (chifres da tiara), centro z=2.4
    bpy.ops.object.camera_add(location=(0, -9.2, 2.3))
    cam = bpy.context.active_object
    cam.rotation_euler = (math.radians(90), 0, 0)
    cam.data.lens = 42
    bpy.context.scene.camera = cam

    # Iluminacao
    bpy.ops.object.light_add(type='AREA', location=(1.5, -6.0, 5.5))
    kl = bpy.context.active_object
    kl.data.energy = 620; kl.data.size = 4.5
    kl.data.color = (0.72, 0.95, 0.88)
    kl.rotation_euler = (math.radians(42), 0, math.radians(-18))

    bpy.ops.object.light_add(type='AREA', location=(-2.8, 4.5, 4.0))
    rl = bpy.context.active_object
    rl.data.energy = 780; rl.data.size = 4.0
    rl.data.color = (1.0, 0.82, 0.38)
    rl.rotation_euler = (math.radians(-40), 0, math.radians(148))

    bpy.ops.object.light_add(type='POINT', location=(0, -2.5, 2.4))
    fl = bpy.context.active_object
    fl.data.energy = 160; fl.data.color = (0.95, 0.75, 0.15)

    # === VIDA IDLE (2026-08-16, pedido do Rafa) === Guardião é um totem
    # de pedra animado, sem manto — usa "respiração" (pulso bem sutil no
    # corpo colunar) + leve inclinação de cabeça, sem balanço de tecido.
    # Amplitude menor que o Enki (é pedra, não carne) — vivo mas rígido.
    add_idle_life_keyframes(root, manto_obj=None, torso_obj=corpo,
                             cabeca_obj=cabeca, frame_count=8,
                             sway_deg=0.0, breathe_amount=0.010,
                             head_tilt_deg=1.4, cycles=0.8)

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
    construir_guardiao('padrao')
    salvar("05_guardiao_atlante_npc")
    print("OK 05_guardiao_atlante_npc v5.0")
