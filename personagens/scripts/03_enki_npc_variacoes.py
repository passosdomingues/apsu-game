import bpy
import math
import os

# ============================================================
# 03B - ENKI NPC: VARIAÇÕES DE LORE E AVATARES (v3.1)
# Paleta 60-30-10
# ============================================================

PALETAS_ENKI = {
    "03_enki_var_eremita": {
        "nome_pt": "Enki Eremita das Cavernas Abissais",
        "manto_60_a": (0.05, 0.28, 0.18, 1.0),
        "manto_60_b": (0.02, 0.12, 0.08, 1.0),
        "barba_30": (0.75, 0.72, 0.65, 1.0),
        "ouro_10": (0.80, 0.55, 0.20, 1.0),     # Bronze Envelhecido
        "runa_10": (0.2, 1.0, 0.8, 1.0),        # Verde Aqua Neon
        "luz_key": (0.4, 0.9, 0.7)
    },
    "03_enki_var_celestial": {
        "nome_pt": "Enki Espírito Astral das Águas Primordiais",
        "manto_60_a": (0.88, 0.92, 0.98, 1.0),
        "manto_60_b": (0.45, 0.65, 0.85, 1.0),
        "barba_30": (0.98, 0.98, 1.0, 1.0),
        "ouro_10": (1.0, 0.82, 0.25, 1.0),      # Ouro Solar
        "runa_10": (1.0, 0.60, 0.1, 1.0),       # Fogo Divino
        "luz_key": (0.9, 0.95, 1.0)
    }
}

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
    scene.render.resolution_x = 1080
    scene.render.resolution_y = 1920
    scene.render.film_transparent = True
    scene.view_settings.view_transform = 'Filmic'
    scene.view_settings.look = 'Medium High Contrast'

def criar_material_manto(nome, cor_primaria, cor_secundaria):
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

    wave.wave_type = 'RINGS'
    wave.inputs['Scale'].default_value = 5.0
    wave.inputs['Distortion'].default_value = 3.5

    ramp.color_ramp.elements[0].position = 0.25
    ramp.color_ramp.elements[0].color = cor_secundaria
    ramp.color_ramp.elements[1].position = 0.8
    ramp.color_ramp.elements[1].color = cor_primaria

    bump.inputs['Strength'].default_value = 0.3

    if 'Metallic' in bsdf.inputs:
        bsdf.inputs['Metallic'].default_value = 0.35
    if 'Roughness' in bsdf.inputs:
        bsdf.inputs['Roughness'].default_value = 0.35

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

def construir_enki_variacao(var_id, dados):
    setup_cena()

    mat_manto = criar_material_manto(f"Mat_{var_id}_Manto_60", dados["manto_60_a"], dados["manto_60_b"])
    mat_pele = criar_material_simples(f"Mat_{var_id}_Pele", (0.84, 0.67, 0.50, 1.0), metallic=0.05, roughness=0.55)
    mat_barba = criar_material_simples(f"Mat_{var_id}_Barba_30", dados["barba_30"], metallic=0.2, roughness=0.5)
    mat_ouro = criar_material_simples(f"Mat_{var_id}_Ouro_10", dados["ouro_10"], metallic=0.92, roughness=0.18)
    mat_runa = criar_material_simples(f"Mat_{var_id}_Runa_10", dados["runa_10"], emission=True, emission_cor=dados["runa_10"], emission_forca=6.0)

    root = bpy.data.objects.new(f"Enki_{var_id}_Root", None)
    bpy.context.scene.collection.objects.link(root)
    root.rotation_euler = (0, 0, math.radians(10))

    # Manto
    bpy.ops.mesh.primitive_cone_add(vertices=24, radius1=1.1, radius2=0.45, depth=2.8, location=(0, 0, 1.4))
    manto = bpy.context.active_object
    manto.scale = (1.0, 0.75, 1.0)
    manto.parent = root
    manto.data.materials.append(mat_manto)
    smooth_object(manto)

    # Xale
    bpy.ops.mesh.primitive_torus_add(major_radius=0.7, minor_radius=0.15, location=(0, -0.05, 2.0))
    xale = bpy.context.active_object
    xale.rotation_euler = (math.radians(35), math.radians(20), 0)
    xale.scale = (0.9, 0.7, 1.1)
    xale.parent = root
    xale.data.materials.append(mat_barba)
    smooth_object(xale)

    # Cabeça
    bpy.ops.mesh.primitive_uv_sphere_add(segments=28, ring_count=24, radius=0.46, location=(0, -0.08, 2.75))
    cabeca = bpy.context.active_object
    cabeca.parent = root
    cabeca.data.materials.append(mat_pele)
    smooth_object(cabeca)

    # Olhos
    for side in [-1, 1]:
        bpy.ops.mesh.primitive_uv_sphere_add(radius=0.065, location=(side * 0.16, -0.46, 2.78))
        olho = bpy.context.active_object
        olho.parent = root
        olho.data.materials.append(mat_runa)

    # Coroa
    bpy.ops.mesh.primitive_cylinder_add(radius=0.48, depth=0.45, location=(0, -0.08, 3.12))
    coroa = bpy.context.active_object
    coroa.parent = root
    coroa.data.materials.append(mat_ouro)
    smooth_object(coroa)

    # Chifres
    chifres_dados = [
        (0.32, 0.45, 3.25, 0.1, 0.4),
        (0.28, 0.40, 3.42, 0.08, 0.35),
    ]
    for side in [-1, 1]:
        for ang_mult, x_dist, z_pos, r_ch, d_ch in chifres_dados:
            bpy.ops.mesh.primitive_cone_add(vertices=8, radius1=r_ch, depth=d_ch, location=(side * x_dist, -0.08, z_pos))
            c = bpy.context.active_object
            c.rotation_euler = (0, 0, math.radians(side * 28))
            c.parent = root
            c.data.materials.append(mat_ouro)
            smooth_object(c)

    # Barba
    for i in range(5):
        z_pos = 2.35 - (i * 0.26)
        raio_torus = 0.38 - (i * 0.04)
        bpy.ops.mesh.primitive_cone_add(vertices=12, radius1=raio_torus, depth=0.42, location=(0, -0.32 + (i*0.02), z_pos))
        cam = bpy.context.active_object
        cam.rotation_euler = (math.radians(15), 0, 0)
        cam.parent = root
        cam.data.materials.append(mat_barba)
        smooth_object(cam)

    # Tabuleta
    tab_root = bpy.data.objects.new(f"Tab_{var_id}", None)
    bpy.context.scene.collection.objects.link(tab_root)
    tab_root.parent = root
    tab_root.location = (0.75, -0.6, 2.0)
    tab_root.rotation_euler = (math.radians(25), math.radians(-20), math.radians(15))

    bpy.ops.mesh.primitive_cube_add(size=0.7, location=(0, 0, 0))
    tab = bpy.context.active_object
    tab.scale = (0.8, 0.12, 1.2)
    tab.parent = tab_root
    tab.data.materials.append(mat_barba)
    smooth_object(tab)

    # Cajado
    cajado_root = bpy.data.objects.new(f"Caj_{var_id}", None)
    bpy.context.scene.collection.objects.link(cajado_root)
    cajado_root.parent = root
    cajado_root.location = (-0.85, -0.4, 1.6)
    cajado_root.rotation_euler = (math.radians(5), 0, math.radians(-10))

    bpy.ops.mesh.primitive_cylinder_add(radius=0.04, depth=3.2, location=(0, 0, 0))
    haste = bpy.context.active_object
    haste.parent = cajado_root
    haste.data.materials.append(mat_ouro)
    smooth_object(haste)

    bpy.ops.mesh.primitive_ico_sphere_add(radius=0.22, subdivisions=3, location=(0, 0, 1.6))
    orbe = bpy.context.active_object
    orbe.parent = cajado_root
    orbe.data.materials.append(mat_runa)
    smooth_object(orbe)

    # --- CÂMERA AUTO-TRACKING ENQUADRADA PERFEITAMENTE ---
    cam_target = bpy.data.objects.new("Enki_Camera_Target", None)
    cam_target.location = (0.0, -0.2, 1.9)
    bpy.context.scene.collection.objects.link(cam_target)

    # Camera Portrait Fixa
    bpy.ops.object.camera_add(location=(1.0, -8.0, 1.8), rotation=(math.radians(88), 0, math.radians(7)))
    camera = bpy.context.active_object
    camera.data.lens = 42
    bpy.context.scene.camera = camera

    bpy.ops.object.light_add(type='AREA', location=(0.0, -4.5, 5.0))
    key = bpy.context.active_object
    key.data.energy = 600
    key.data.color = dados["luz_key"]

    bpy.ops.object.light_add(type='AREA', location=(-3.0, 4.0, 3.5))
    rim = bpy.context.active_object
    rim.data.energy = 750
    rim.data.color = (1.0, 0.85, 0.4)

    # Salva e Renderiza
    base_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    blends_dir = os.path.join(base_dir, "blends", "03_enki_npc")
    renders_dir = os.path.join(base_dir, "renders")
    os.makedirs(blends_dir, exist_ok=True)
    os.makedirs(renders_dir, exist_ok=True)

    blend_path = os.path.join(blends_dir, f"{var_id}.blend")
    render_path = os.path.join(renders_dir, f"{var_id}.png")

    try:
        bpy.ops.wm.save_as_mainfile(filepath=blend_path)
        print("Blend salvo: " + blend_path)
    except Exception as e:
        print("Aviso ao salvar blend: " + str(e))

    try:
        bpy.context.scene.render.filepath = render_path
        bpy.ops.render.render(write_still=True)
        print("Render salvo: " + render_path)
    except Exception as e:
        print("Aviso ao renderizar: " + str(e))

if __name__ == "__main__":
    for var_id, dados in PALETAS_ENKI.items():
        print(f"--- Gerando {dados['nome_pt']} ({var_id}) ---")
        construir_enki_variacao(var_id, dados)
    print("OK Variacoes de Enki geradas com sucesso!")
