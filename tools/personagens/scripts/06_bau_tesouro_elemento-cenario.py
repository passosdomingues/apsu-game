import bpy
import math
import os

# --- Biblioteca compartilhada (2026-08-16: rim light) ---
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

# ============================================================
# 06 - BAÃš DO TESOURO & RELÃQUIA ANCESTRAL (PROP v3.1)
# Paleta 60-30-10:
#   60% Madeira Nobre Submersa / Carvalho Envelhecido (Casco do BaÃº)
#   30% Bronze / Metal Oxidado Verde-Teal (Cintas de ReforÃ§o, DobradiÃ§as)
#   10% Ouro MaciÃ§o, PÃ©rolas e FÃ³ssil da Tartaruga Ancestral Emissivo
# ============================================================

def setup_cena():
    bpy.ops.object.select_all(action='SELECT')
    bpy.ops.object.delete(use_global=False)
    for mesh in bpy.data.meshes:
        bpy.data.meshes.remove(mesh, do_unlink=True)
    for mat in bpy.data.materials:
        bpy.data.materials.remove(mat, do_unlink=True)

    scene = bpy.context.scene
    scene.render.engine = 'BLENDER_EEVEE_NEXT' if hasattr(bpy.types, 'RenderSettings') and 'BLENDER_EEVEE_NEXT' in bpy.types.RenderSettings.bl_rna.properties['engine'].enum_items.keys() else 'BLENDER_EEVEE'
    if hasattr(scene, 'eevee') and hasattr(scene.eevee, 'taa_render_samples'):
        scene.eevee.taa_render_samples = 16
    scene.render.resolution_x = 1920
    scene.render.resolution_y = 1080
    scene.render.film_transparent = True
    scene.view_settings.view_transform = 'Filmic'
    scene.view_settings.look = 'High Contrast'

def criar_material_madeira(nome, cor_a, cor_b):
    mat = bpy.data.materials.new(name=nome)
    mat.use_nodes = True
    nodes = mat.node_tree.nodes
    links = mat.node_tree.links
    nodes.clear()

    output = nodes.new('ShaderNodeOutputMaterial')
    bsdf = nodes.new('ShaderNodeBsdfPrincipled')
    tex_coord = nodes.new('ShaderNodeTexCoord')
    wave = nodes.new('ShaderNodeTexWave')
    ramp = nodes.new('ShaderNodeValToRGB')
    bump = nodes.new('ShaderNodeBump')

    wave.wave_type = 'BANDS'
    wave.inputs['Scale'].default_value = 8.0
    wave.inputs['Distortion'].default_value = 2.0

    ramp.color_ramp.elements[0].position = 0.2
    ramp.color_ramp.elements[0].color = cor_b
    ramp.color_ramp.elements[1].position = 0.8
    ramp.color_ramp.elements[1].color = cor_a

    bump.inputs['Strength'].default_value = 0.35

    if 'Metallic' in bsdf.inputs:
        bsdf.inputs['Metallic'].default_value = 0.05
    if 'Roughness' in bsdf.inputs:
        bsdf.inputs['Roughness'].default_value = 0.75

    links.new(tex_coord.outputs['Object'], wave.inputs['Vector'])
    links.new(wave.outputs['Fac'], ramp.inputs['Fac'])
    links.new(ramp.outputs['Color'], bsdf.inputs['Base Color'])
    links.new(wave.outputs['Fac'], bump.inputs['Height'])
    links.new(bump.outputs['Normal'], bsdf.inputs['Normal'])
    links.new(bsdf.outputs['BSDF'], output.inputs['Surface'])
    return mat

def criar_material_simples(nome, cor_rgba, metallic=0.1, roughness=0.3, emission=False, emission_cor=(1,1,1,1), emission_forca=3.0):
    mat = bpy.data.materials.new(name=nome)
    mat.use_nodes = True
    bsdf = mat.node_tree.nodes.get("Principled BSDF")
    if bsdf:
        if 'Base Color' in bsdf.inputs:
            bsdf.inputs['Base Color'].default_value = cor_rgba
        if 'Metallic' in bsdf.inputs:
            bsdf.inputs['Metallic'].default_value = metallic
        if 'Roughness' in bsdf.inputs:
            bsdf.inputs['Roughness'].default_value = roughness
        if emission:
            if 'Emission Color' in bsdf.inputs:
                bsdf.inputs['Emission Color'].default_value = emission_cor
            if 'Emission Strength' in bsdf.inputs:
                bsdf.inputs['Emission Strength'].default_value = emission_forca
    return mat

def smooth_object(obj):
    if obj.type == 'MESH':
        for p in obj.data.polygons:
            p.use_smooth = True

def construir_bau_tesouro():
    setup_cena()

    # --- MATERIAIS 60-30-10 ---
    mat_madeira = criar_material_madeira("Mat_Bau_Madeira_60", (0.24, 0.14, 0.08, 1.0), (0.12, 0.07, 0.04, 1.0))
    mat_metal_patina = criar_material_simples("Mat_Bau_Metal_30", (0.08, 0.35, 0.32, 1.0), metallic=0.75, roughness=0.35)
    mat_ouro = criar_material_simples("Mat_Bau_Ouro_10", (0.98, 0.80, 0.20, 1.0), metallic=0.95, roughness=0.15)
    mat_reliquia_brilho = criar_material_simples("Mat_Bau_Reliquia_10", (0.3, 0.95, 1.0, 0.8), emission=True, emission_cor=(0.3, 0.95, 1.0, 1.0), emission_forca=7.0)
    mat_rubi = criar_material_simples("Mat_Bau_Rubi", (1.0, 0.1, 0.2, 1.0), emission=True, emission_cor=(1.0, 0.1, 0.2, 1.0), emission_forca=5.0)

    root = bpy.data.objects.new("Bau_Root", None)
    bpy.context.scene.collection.objects.link(root)
    root.rotation_euler = (0, 0, math.radians(-15))

    # Base
    bpy.ops.mesh.primitive_cube_add(size=1.2, location=(0, 0, 0.45))
    base = bpy.context.active_object
    base.scale = (1.1, 0.75, 0.65)
    base.parent = root
    base.data.materials.append(mat_madeira)
    smooth_object(base)

    # Tampa
    tampa_root = bpy.data.objects.new("Tampa_Root", None)
    bpy.context.scene.collection.objects.link(tampa_root)
    tampa_root.parent = root
    tampa_root.location = (0, 0.45, 0.85)
    tampa_root.rotation_euler = (math.radians(-32), 0, 0)

    bpy.ops.mesh.primitive_cylinder_add(radius=0.68, depth=1.35, vertices=20, location=(0, -0.45, 0.15))
    tampa = bpy.context.active_object
    tampa.rotation_euler = (0, math.radians(90), 0)
    tampa.scale = (0.55, 1.0, 1.0)
    tampa.parent = tampa_root
    tampa.data.materials.append(mat_madeira)
    smooth_object(tampa)

    for px in [-0.55, 0, 0.55]:
        bpy.ops.mesh.primitive_cylinder_add(radius=0.71, depth=0.08, vertices=20, location=(px, -0.45, 0.15))
        cinta_t = bpy.context.active_object
        cinta_t.rotation_euler = (0, math.radians(90), 0)
        cinta_t.scale = (0.57, 1.0, 1.0)
        cinta_t.parent = tampa_root
        cinta_t.data.materials.append(mat_metal_patina)
        smooth_object(cinta_t)

    # Cintas da Base
    for px in [-0.6, 0, 0.6]:
        bpy.ops.mesh.primitive_cube_add(size=1.22, location=(px, 0, 0.45))
        cinta_b = bpy.context.active_object
        cinta_b.scale = (0.07, 0.77, 0.67)
        cinta_b.parent = root
        cinta_b.data.materials.append(mat_metal_patina)
        smooth_object(cinta_b)

    # Fecho
    bpy.ops.mesh.primitive_cube_add(size=0.28, location=(0, -0.48, 0.75))
    fecho = bpy.context.active_object
    fecho.scale = (1.2, 0.4, 1.2)
    fecho.parent = root
    fecho.data.materials.append(mat_ouro)
    smooth_object(fecho)

    # FÃ³ssil RelÃ­quia
    bpy.ops.mesh.primitive_uv_sphere_add(segments=20, ring_count=16, radius=0.32, location=(0, -0.05, 1.15))
    reliquia = bpy.context.active_object
    reliquia.scale = (1.1, 1.3, 0.6)
    reliquia.rotation_euler = (math.radians(15), math.radians(-10), 0)
    reliquia.parent = root
    reliquia.data.materials.append(mat_reliquia_brilho)
    smooth_object(reliquia)

    # Moedas e Gemas
    tesouros_dados = [
        (-0.35, -0.2, 0.95, 0.12, mat_ouro),
        (0.35, -0.15, 0.92, 0.10, mat_ouro),
        (-0.15, -0.25, 1.02, 0.08, mat_rubi),
        (0.2, -0.22, 1.05, 0.09, mat_ouro),
        (-0.45, -0.35, 0.35, 0.08, mat_ouro),
        (0.5, -0.38, 0.28, 0.07, mat_rubi),
    ]
    for tx, ty, tz, trad, tmat in tesouros_dados:
        bpy.ops.mesh.primitive_ico_sphere_add(radius=trad, subdivisions=3, location=(tx, ty, tz))
        gem = bpy.context.active_object
        gem.parent = root
        gem.data.materials.append(tmat)
        smooth_object(gem)

    # --- CÃ‚MERA AUTO-TRACKING ENQUADRADA PERFEITAMENTE ---
    cam_target = bpy.data.objects.new("Bau_Camera_Target", None)
    cam_target.location = (0.0, 0.0, 0.75)
    bpy.context.scene.collection.objects.link(cam_target)

    bpy.ops.object.camera_add(location=(2.5, -5.8, 2.8))
    camera = bpy.context.active_object
    camera.data.lens = 45
    bpy.context.scene.camera = camera

    track = camera.constraints.new(type='TRACK_TO')
    track.target = cam_target
    track.track_axis = 'TRACK_NEGATIVE_Z'
    track.up_axis = 'UP_Y'

    bpy.ops.object.light_add(type='AREA', location=(1.5, -3.5, 4.5))
    key = bpy.context.active_object
    key.data.energy = 550
    key.data.color = (0.6, 0.9, 1.0)

    bpy.ops.object.light_add(type='POINT', location=(0, -0.1, 1.2))
    t_light = bpy.context.active_object
    t_light.data.energy = 800
    t_light.data.color = (0.3, 1.0, 0.9)

    bpy.ops.object.light_add(type='AREA', location=(-2.5, 3.5, 3.0))
    rim = bpy.context.active_object
    rim.data.energy = 700
    rim.data.color = (1.0, 0.8, 0.3)

    # Rim light (2026-08-16) — dourado quente, mesma cor do próprio ouro
    # do baú (reforça o "tesouro reluzente" na silhueta)
    for mat in (mat_madeira, mat_metal_patina):
        add_fresnel_rim(mat, (0.98, 0.80, 0.20), power=1.6, strength=5.0)

    return root

def salvar_e_renderizar(nome_arquivo="06_bau_tesouro_elemento-cenario"):
    base_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    blends_dir = os.path.join(base_dir, "blends", "06_bau_tesouro_elemento-cenario")
    renders_dir = os.path.join(base_dir, "renders")
    os.makedirs(blends_dir, exist_ok=True)
    os.makedirs(renders_dir, exist_ok=True)

    blend_path = os.path.join(blends_dir, f"{nome_arquivo}.blend")
    render_path = os.path.join(renders_dir, f"{nome_arquivo}.png")

    try:
        bpy.ops.wm.save_as_mainfile(filepath=blend_path)
        print(f"ðŸ’¾ Blend salvo em: {blend_path}")
    except Exception as e:
        print(f"Aviso ao salvar blend: {e}")

    try:
        bpy.context.scene.render.filepath = render_path
        bpy.ops.render.render(write_still=True)
        print(f"ðŸ“¸ Render salvo em: {render_path}")
    except Exception as e:
        print(f"Aviso ao renderizar: {e}")

if __name__ == "__main__":
    construir_bau_tesouro()
    salvar_e_renderizar("06_bau_tesouro_elemento-cenario")
    print("âœ… BaÃº do Tesouro v3.1 gerado com sucesso!")

