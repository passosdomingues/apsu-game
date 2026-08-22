import bpy
import math
import os

# ============================================================
# 02B - KULLULLÛ BOSS: VARIAÇÕES ELEMENTAIS DO ABISMO (v3.1)
# Paleta 60-30-10
# ============================================================

PALETAS_BOSS = {
    "02_kullullu_var_toxico": {
        "nome_pt": "Kullullû Abissal Tóxico",
        "cor_base_60": (0.02, 0.04, 0.02, 1.0),
        "cor_rachadura_10": (0.4, 1.0, 0.1, 1.0),
        "espinho_30": (0.22, 0.02, 0.35, 1.0),
        "acento_10": (1.0, 0.6, 0.0, 1.0),
        "luz_key": (0.4, 1.0, 0.2),
        "luz_rim": (0.8, 0.2, 1.0)
    },
    "02_kullullu_var_glacial": {
        "nome_pt": "Kullullû Leviatã do Gelo Negro",
        "cor_base_60": (0.01, 0.03, 0.08, 1.0),
        "cor_rachadura_10": (0.2, 0.8, 1.0, 1.0),
        "espinho_30": (0.08, 0.18, 0.45, 1.0),
        "acento_10": (0.6, 0.95, 1.0, 1.0),
        "luz_key": (0.3, 0.8, 1.0),
        "luz_rim": (0.6, 0.9, 1.0)
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
    scene.view_settings.look = 'Very High Contrast'

def criar_material_obsidiana_rachada(nome, cor_base, cor_rachadura, escala=2.8, metallic=0.5, roughness=0.28, emissao_forca=7.0):
    mat = bpy.data.materials.new(name=nome)
    mat.use_nodes = True
    nodes = mat.node_tree.nodes
    links = mat.node_tree.links
    nodes.clear()

    output = nodes.new('ShaderNodeOutputMaterial')
    bsdf = nodes.new('ShaderNodeBsdfPrincipled')
    tex_coord = nodes.new('ShaderNodeTexCoord')
    voronoi = nodes.new('ShaderNodeTexVoronoi')
    noise = nodes.new('ShaderNodeTexNoise')
    ramp_diffuse = nodes.new('ShaderNodeValToRGB')
    ramp_emission = nodes.new('ShaderNodeValToRGB')
    bump = nodes.new('ShaderNodeBump')

    voronoi.inputs['Scale'].default_value = escala
    voronoi.feature = 'DISTANCE_TO_EDGE'
    noise.inputs['Scale'].default_value = escala * 2.0
    noise.inputs['Detail'].default_value = 5.0

    ramp_diffuse.color_ramp.elements[0].position = 0.05
    ramp_diffuse.color_ramp.elements[0].color = cor_rachadura
    ramp_diffuse.color_ramp.elements[1].position = 0.16
    ramp_diffuse.color_ramp.elements[1].color = cor_base

    ramp_emission.color_ramp.elements[0].position = 0.04
    ramp_emission.color_ramp.elements[0].color = cor_rachadura
    ramp_emission.color_ramp.elements[1].position = 0.10
    ramp_emission.color_ramp.elements[1].color = (0.0, 0.0, 0.0, 1.0)

    bump.inputs['Strength'].default_value = 0.6

    if 'Metallic' in bsdf.inputs:
        bsdf.inputs['Metallic'].default_value = metallic
    if 'Roughness' in bsdf.inputs:
        bsdf.inputs['Roughness'].default_value = roughness
    if 'Emission Strength' in bsdf.inputs:
        bsdf.inputs['Emission Strength'].default_value = emissao_forca

    links.new(tex_coord.outputs['Object'], voronoi.inputs['Vector'])
    links.new(tex_coord.outputs['Object'], noise.inputs['Vector'])
    links.new(voronoi.outputs['Distance'], ramp_diffuse.inputs['Fac'])
    links.new(ramp_diffuse.outputs['Color'], bsdf.inputs['Base Color'])
    links.new(voronoi.outputs['Distance'], ramp_emission.inputs['Fac'])
    if 'Emission Color' in bsdf.inputs:
        links.new(ramp_emission.outputs['Color'], bsdf.inputs['Emission Color'])
    links.new(noise.outputs['Fac'], bump.inputs['Height'])
    links.new(bump.outputs['Normal'], bsdf.inputs['Normal'])
    links.new(bsdf.outputs['BSDF'], output.inputs['Surface'])
    return mat

def criar_material_simples(nome, cor_rgba, metallic=0.2, roughness=0.35, emission=False, emission_cor=(1,0.2,0,1), emission_forca=5.0):
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

def construir_boss_variacao(var_id, dados):
    setup_cena()

    mat_corpo = criar_material_obsidiana_rachada(f"Mat_{var_id}_Corpo_60", dados["cor_base_60"], dados["cor_rachadura_10"], escala=2.8)
    mat_espinho = criar_material_simples(f"Mat_{var_id}_Espinho_30", dados["espinho_30"], metallic=0.5, roughness=0.3)
    mat_acento = criar_material_simples(f"Mat_{var_id}_Acento_10", dados["acento_10"], emission=True, emission_cor=dados["acento_10"], emission_forca=8.0)

    root = bpy.data.objects.new(f"Boss_{var_id}_Root", None)
    bpy.context.scene.collection.objects.link(root)
    root.rotation_euler = (math.radians(-15), math.radians(-5), math.radians(15))

    # Torso
    bpy.ops.mesh.primitive_uv_sphere_add(segments=32, ring_count=24, radius=1.45, location=(0, 0, 1.6))
    torso = bpy.context.active_object
    torso.scale = (1.35, 1.05, 1.5)
    torso.parent = root
    torso.data.materials.append(mat_corpo)
    smooth_object(torso)

    # Núcleo
    bpy.ops.mesh.primitive_ico_sphere_add(radius=0.35, subdivisions=3, location=(0, -0.92, 1.7))
    nucleo = bpy.context.active_object
    nucleo.parent = root
    nucleo.data.materials.append(mat_acento)
    smooth_object(nucleo)

    # Cabeça e Chifres
    bpy.ops.mesh.primitive_uv_sphere_add(segments=28, ring_count=24, radius=0.85, location=(0, -0.35, 3.2))
    cabeca = bpy.context.active_object
    cabeca.scale = (1.0, 1.15, 0.95)
    cabeca.parent = root
    cabeca.data.materials.append(mat_corpo)
    smooth_object(cabeca)

    for side in [-1, 1]:
        bpy.ops.mesh.primitive_cone_add(vertices=12, radius1=0.28, depth=1.6, location=(side * 0.75, -0.2, 4.0))
        chifre = bpy.context.active_object
        chifre.rotation_euler = (math.radians(-30), math.radians(side * 40), math.radians(side * 20))
        chifre.parent = root
        chifre.data.materials.append(mat_espinho)
        smooth_object(chifre)

    # Olhos
    for side in [-1, 1]:
        bpy.ops.mesh.primitive_uv_sphere_add(radius=0.12, location=(side * 0.35, -1.05, 3.35))
        olho = bpy.context.active_object
        olho.parent = root
        olho.data.materials.append(mat_acento)

    # Espinhos dorsais
    for i, y_pos in enumerate([-0.6, -0.1, 0.4, 0.9, 1.4]):
        tam = 0.85 - (i * 0.1)
        bpy.ops.mesh.primitive_cone_add(vertices=6, radius1=0.22, depth=tam*2.0, location=(0, y_pos, 2.7 - (i*0.35)))
        esp = bpy.context.active_object
        esp.rotation_euler = (math.radians(-65), 0, 0)
        esp.parent = root
        esp.data.materials.append(mat_espinho)
        smooth_object(esp)

    # Braços
    for side in [-1, 1]:
        bpy.ops.mesh.primitive_cylinder_add(radius=0.4, depth=1.4, location=(side * 1.5, -0.2, 1.8))
        braco = bpy.context.active_object
        braco.rotation_euler = (math.radians(20), math.radians(side * 35), math.radians(side * 20))
        braco.parent = root
        braco.data.materials.append(mat_corpo)
        smooth_object(braco)

    # Cauda
    cauda_boss = [
        ((0, 0.2, 0.4), (1.1, 0.95, 1.1), math.radians(-20)),
        ((0, 0.7, -0.5), (0.85, 0.75, 1.1), math.radians(-40)),
        ((0, 1.4, -1.3), (0.60, 0.55, 1.0), math.radians(-15)),
        ((0, 2.0, -1.9), (0.35, 0.30, 0.9), math.radians(25)),
    ]
    for pos, scale, rot_x in cauda_boss:
        bpy.ops.mesh.primitive_cone_add(vertices=20, radius1=scale[0], radius2=scale[0]*0.7, depth=scale[2], location=pos)
        seg = bpy.context.active_object
        seg.rotation_euler = (rot_x, 0, 0)
        seg.parent = root
        seg.data.materials.append(mat_corpo)
        smooth_object(seg)

    # Lança
    lance_root = bpy.data.objects.new(f"Lanca_{var_id}", None)
    bpy.context.scene.collection.objects.link(lance_root)
    lance_root.parent = root
    lance_root.location = (-1.5, -1.2, 1.8)
    lance_root.rotation_euler = (math.radians(55), math.radians(15), math.radians(25))

    bpy.ops.mesh.primitive_cylinder_add(radius=0.08, depth=5.2, location=(0, 0, 0))
    haste = bpy.context.active_object
    haste.parent = lance_root
    haste.data.materials.append(mat_espinho)
    smooth_object(haste)

    bpy.ops.mesh.primitive_cone_add(vertices=6, radius1=0.25, depth=1.6, location=(0, 0, 2.8))
    lamina = bpy.context.active_object
    lamina.parent = lance_root
    lamina.data.materials.append(mat_corpo)
    smooth_object(lamina)

    bpy.ops.mesh.primitive_ico_sphere_add(radius=0.3, subdivisions=3, location=(0, 0, 2.0))
    orbe = bpy.context.active_object
    orbe.parent = lance_root
    orbe.data.materials.append(mat_acento)
    smooth_object(orbe)

    # --- CÂMERA AUTO-TRACKING ENQUADRADA PERFEITA ---
    cam_target = bpy.data.objects.new("Boss_Camera_Target", None)
    cam_target.location = (0.0, 0.0, 1.8)
    bpy.context.scene.collection.objects.link(cam_target)

    # Camera Portrait Fixa
    bpy.ops.object.camera_add(location=(1.0, -8.5, 1.8), rotation=(math.radians(88), 0, math.radians(7)))
    camera = bpy.context.active_object
    camera.data.lens = 42
    bpy.context.scene.camera = camera

    bpy.ops.object.light_add(type='AREA', location=(2.5, -5.5, 5.0))
    key = bpy.context.active_object
    key.data.energy = 750
    key.data.color = dados["luz_key"]

    bpy.ops.object.light_add(type='AREA', location=(-3.5, 4.0, 3.5))
    rim = bpy.context.active_object
    rim.data.energy = 950
    rim.data.color = dados["luz_rim"]

    #def salvar_variacao(nome):
    base_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    blends_dir = os.path.join(base_dir, "blends", "02_kullullu_boss")
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
    for var_id, dados in PALETAS_BOSS.items():
        print(f"--- Gerando {dados['nome_pt']} ({var_id}) ---")
        construir_boss_variacao(var_id, dados)
    print("OK Variacoes de Kullullu geradas com sucesso!")
