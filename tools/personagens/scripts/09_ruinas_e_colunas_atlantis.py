import bpy
import math
import os

# ============================================================
# 09 - RUÍNAS E COLUNAS DE ATLANTIS (PROP DE CENÁRIO v1)
# Templos Submersos & Plataformas de Fase (Fase 3 - Templo de Apsu)
# Paleta 60-30-10:
#   60% Mármore / Calcário Submerso Azulado (Colunas Caneladas e Arcos Partidos)
#   30% Pátina de Cobre & Ouro Ancestral (Capitéis Mesopotâmicos e Frisos)
#   10% Cristal Energético de Atlantis Emissivo (Pedestais e Runas de Ativação)
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
    scene.view_settings.look = 'Medium High Contrast'

def criar_material_simples(nome, cor_rgba, metallic=0.1, roughness=0.5, emission=False, emission_cor=(1,1,1,1), emission_forca=3.0):
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

def construir_ruinas_atlantis():
    setup_cena()

    # --- MATERIAIS 60-30-10 ---
    mat_marmore = criar_material_simples("Mat_Ruina_Marmore_60", (0.58, 0.65, 0.70, 1.0), roughness=0.75)
    mat_friso_ouro = criar_material_simples("Mat_Ruina_Ouro_30", (0.85, 0.68, 0.22, 1.0), metallic=0.7, roughness=0.35)
    mat_cristal_luz = criar_material_simples("Mat_Ruina_Cristal_10", (0.1, 0.95, 1.0, 0.8), metallic=0.2, roughness=0.1, emission=True, emission_cor=(0.1, 0.95, 1.0, 1.0), emission_forca=8.0)

    root = bpy.data.objects.new("Ruinas_Atlantis_Root", None)
    bpy.context.scene.collection.objects.link(root)

    # --- PLATAFORMA / BASE DE PEDRA ---
    bpy.ops.mesh.primitive_cube_add(size=1.0, location=(0, 0, 0.3))
    base_ruina = bpy.context.active_object
    base_ruina.scale = (4.5, 2.2, 0.6)
    base_ruina.parent = root
    base_ruina.data.materials.append(mat_marmore)
    smooth_object(base_ruina)

    # --- 3 COLUNAS TEMPLARES (Inteira, Quebrada e Inclinada) ---
    colunas_dados = [
        (-1.5, 0.0, 3.2, 0.0, 0.0),            # Coluna Íntegra
        (0.0, 0.0, 1.8, 0.0, 0.0),             # Coluna Quebrada
        (1.5, 0.0, 2.6, math.radians(12), 0.0) # Coluna Tombada
    ]

    for cx, cy, calt, rot_x, rot_y in colunas_dados:
        # Fuste Canelado
        bpy.ops.mesh.primitive_cylinder_add(radius=0.4, depth=calt, vertices=16, location=(cx, cy, 0.6 + calt/2))
        col = bpy.context.active_object
        col.rotation_euler = (rot_x, rot_y, 0)
        col.parent = root
        col.data.materials.append(mat_marmore)
        smooth_object(col)

        # Capitel Dourado Mesopotâmico no Topo
        bpy.ops.mesh.primitive_cube_add(size=0.9, location=(cx, cy, 0.6 + calt))
        capitel = bpy.context.active_object
        capitel.scale = (1.1, 1.1, 0.3)
        capitel.rotation_euler = (rot_x, rot_y, 0)
        capitel.parent = root
        capitel.data.materials.append(mat_friso_ouro)
        smooth_object(capitel)

    # --- ARQUITRAVE / VIGA DE PEDRA SOBRE AS COLUNAS ---
    bpy.ops.mesh.primitive_cube_add(size=1.0, location=(-0.8, 0.0, 4.0))
    viga = bpy.context.active_object
    viga.scale = (2.2, 0.7, 0.5)
    viga.parent = root
    viga.data.materials.append(mat_marmore)
    smooth_object(viga)

    # --- ALTAR CENTRAL COM CRISTAL DE ENERGIA DE ATLANTIS (10% Acento) ---
    bpy.ops.mesh.primitive_cylinder_add(radius=0.55, depth=0.9, vertices=8, location=(0.0, -0.6, 1.05))
    altar = bpy.context.active_object
    altar.parent = root
    altar.data.materials.append(mat_friso_ouro)
    smooth_object(altar)

    # Grande Cristal Octaédrico Flutuando
    bpy.ops.mesh.primitive_cone_add(vertices=6, radius1=0.35, depth=0.8, location=(0.0, -0.6, 2.0))
    cristal_topo = bpy.context.active_object
    cristal_topo.parent = root
    cristal_topo.data.materials.append(mat_cristal_luz)
    smooth_object(cristal_topo)

    bpy.ops.mesh.primitive_cone_add(vertices=6, radius1=0.35, depth=0.8, location=(0.0, -0.6, 1.4))
    cristal_base = bpy.context.active_object
    cristal_base.rotation_euler = (math.radians(180), 0, 0)
    cristal_base.parent = root
    cristal_base.data.materials.append(mat_cristal_luz)
    smooth_object(cristal_base)

    # Camera widescreen - recuada para ver topo das colunas altas (z=4.55)
    bpy.ops.object.camera_add(location=(0.0, -10.5, 2.5))
    camera = bpy.context.active_object
    camera.rotation_euler = (math.radians(90), 0, 0)
    camera.data.lens = 30
    bpy.context.scene.camera = camera

    bpy.ops.object.light_add(type='AREA', location=(0.0, -4.0, 5.0))
    key = bpy.context.active_object
    key.data.energy = 600
    key.data.color = (0.5, 0.85, 1.0)

    bpy.ops.object.light_add(type='POINT', location=(0.0, -0.6, 2.2))
    crystal_light = bpy.context.active_object
    crystal_light.data.energy = 800
    crystal_light.data.color = (0.1, 1.0, 0.9)

    bpy.ops.object.light_add(type='AREA', location=(-3.0, 3.5, 3.0))
    rim = bpy.context.active_object
    rim.data.energy = 650
    rim.data.color = (1.0, 0.8, 0.4)

    return root

def salvar_e_renderizar(nome_arquivo="09_ruinas_e_colunas_atlantis"):
    # Derivado do local do script; não depende da máquina que o gerou.
    base_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    blends_dir = os.path.join(base_dir, "blends", "09_ruinas_e_colunas_atlantis")
    renders_dir = os.path.join(base_dir, "renders")
    os.makedirs(blends_dir, exist_ok=True)
    os.makedirs(renders_dir, exist_ok=True)

    blend_path = os.path.join(blends_dir, nome_arquivo+".blend")
    render_path = os.path.join(renders_dir, nome_arquivo+".png")

    try:
        bpy.ops.wm.save_as_mainfile(filepath=blend_path)
        print("Blend salvo: " + blend_path)
    except Exception as e:
        print("Aviso ao salvar blend: " + str(e))

    bpy.context.scene.render.filepath = render_path
    bpy.ops.render.render(write_still=True)
    print("Render salvo: " + render_path)

if __name__ == "__main__":
    construir_ruinas_atlantis()
    salvar_e_renderizar("09_ruinas_e_colunas_atlantis")
    print("OK 09_ruinas_e_colunas_atlantis v2.0")
