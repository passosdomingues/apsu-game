import bpy
import math
import os

# ============================================================
# 10 - PERIGOS E OBSTÁCULOS SUBMARINOS (HAZARDS v1)
# Mecânicas de Fase (Gêiseres Termais, Minas de Espinho e Ouriços Abissais)
# Paleta 60-30-10:
#   60% Rocha Basáltica Negra / Ferro Fundido Escuro (Corpo das Minas e Chaminés)
#   30% Vermelho Sangue / Laranja Ácido (Espinhos e Correntes de Fixação)
#   10% Magma Submarino & Gases Superaquecidos Emissivos (Vapor e Alertas)
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
    scene.view_settings.look = 'Very High Contrast'

def criar_material_simples(nome, cor_rgba, metallic=0.2, roughness=0.4, emission=False, emission_cor=(1,1,1,1), emission_forca=3.0):
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

def construir_perigos_cenario():
    setup_cena()

    # --- MATERIAIS 60-30-10 ---
    mat_basalto = criar_material_simples("Mat_Hazard_Basalto_60", (0.06, 0.04, 0.08, 1.0), metallic=0.4, roughness=0.7)
    mat_metal_perigo = criar_material_simples("Mat_Hazard_Metal_30", (0.55, 0.08, 0.12, 1.0), metallic=0.7, roughness=0.3)
    mat_magma_gas = criar_material_simples("Mat_Hazard_Magma_10", (1.0, 0.35, 0.0, 1.0), emission=True, emission_cor=(1.0, 0.35, 0.0, 1.0), emission_forca=8.0)
    mat_bolha_gas = criar_material_simples("Mat_Hazard_Bolha", (0.3, 0.9, 1.0, 0.5), metallic=0.1, roughness=0.05, emission=True, emission_cor=(0.2, 0.85, 1.0, 1.0), emission_forca=4.0)

    root = bpy.data.objects.new("Perigos_Submarinos_Root", None)
    bpy.context.scene.collection.objects.link(root)

    # --- 1. CHAMINÉ HIDROTERMAL / GÊISER SUBMARINO (Esquerda) ---
    root_geiser = bpy.data.objects.new("Geiser_Root", None)
    bpy.context.scene.collection.objects.link(root_geiser)
    root_geiser.parent = root
    root_geiser.location = (-1.6, 0.2, 0)

    bpy.ops.mesh.primitive_cone_add(vertices=12, radius1=0.75, radius2=0.28, depth=2.4, location=(0, 0, 1.2))
    chamine = bpy.context.active_object
    chamine.parent = root_geiser
    chamine.data.materials.append(mat_basalto)
    smooth_object(chamine)

    # Borda Magmática do Gêiser
    bpy.ops.mesh.primitive_torus_add(major_radius=0.3, minor_radius=0.06, location=(0, 0, 2.4))
    borda_g = bpy.context.active_object
    borda_g.parent = root_geiser
    borda_g.data.materials.append(mat_magma_gas)
    smooth_object(borda_g)

    # Coluna de Bolhas e Gases Superaquecidos Subindo
    geiser_bolhas = [
        (0.0, 0.0, 2.7, 0.22),
        (0.08, -0.05, 3.1, 0.28),
        (-0.06, 0.04, 3.6, 0.35),
        (0.05, -0.08, 4.2, 0.42),
    ]
    for bx, by, bz, brad in geiser_bolhas:
        bpy.ops.mesh.primitive_uv_sphere_add(segments=16, ring_count=12, radius=brad, location=(bx, by, bz))
        b = bpy.context.active_object
        b.parent = root_geiser
        b.data.materials.append(mat_bolha_gas)
        smooth_object(b)

    # --- 2. MINA SUBMARINA DE ESPINHOS / OURIÇO DO ABISMO (Direita) ---
    root_mina = bpy.data.objects.new("Mina_Root", None)
    bpy.context.scene.collection.objects.link(root_mina)
    root_mina.parent = root
    root_mina.location = (1.4, -0.3, 1.8)

    # Esfera Central da Mina (60% Ferro Basáltico)
    bpy.ops.mesh.primitive_uv_sphere_add(segments=24, ring_count=18, radius=0.75, location=(0, 0, 0))
    corpo_mina = bpy.context.active_object
    corpo_mina.parent = root_mina
    corpo_mina.data.materials.append(mat_basalto)
    smooth_object(corpo_mina)

    # Núcleo de Alerta Pulsante (10% Magma/Alerta)
    bpy.ops.mesh.primitive_ico_sphere_add(radius=0.25, subdivisions=3, location=(0, -0.65, 0.2))
    olho_mina = bpy.context.active_object
    olho_mina.parent = root_mina
    olho_mina.data.materials.append(mat_magma_gas)

    # 12 Espinhos Cônicos em Todas as Direções (30% Vermelho)
    angulos = [
        (0, 0), (180, 0), (90, 0), (-90, 0),
        (45, 45), (-45, 45), (45, -45), (-45, -45),
        (0, 90), (0, -90), (90, 90), (-90, -90)
    ]
    for rx, rz in angulos:
        bpy.ops.mesh.primitive_cone_add(vertices=6, radius1=0.12, depth=0.7, location=(0, 0, 0))
        esp = bpy.context.active_object
        esp.rotation_euler = (math.radians(rx), 0, math.radians(rz))
        # Move para fora do raio
        esp.location = (
            math.sin(math.radians(rz)) * math.cos(math.radians(rx)) * 0.95,
            math.sin(math.radians(rx)) * 0.95,
            math.cos(math.radians(rz)) * math.cos(math.radians(rx)) * 0.95
        )
        esp.parent = root_mina
        esp.data.materials.append(mat_metal_perigo)
        smooth_object(esp)

    # Corrente de Fixação ao Fundo
    bpy.ops.mesh.primitive_cylinder_add(radius=0.035, depth=1.8, location=(0, 0, -1.2))
    corrente = bpy.context.active_object
    corrente.parent = root_mina
    corrente.data.materials.append(mat_metal_perigo)

    # Câmera e Luz
    bpy.ops.object.camera_add(location=(0.0, -8.2, 2.0), rotation=(math.radians(88), 0, 0))
    camera = bpy.context.active_object
    camera.data.lens = 35
    bpy.context.scene.camera = camera

    bpy.ops.object.light_add(type='AREA', location=(0.0, -3.5, 4.5))
    key = bpy.context.active_object
    key.data.energy = 550
    key.data.color = (0.5, 0.85, 1.0)

    bpy.ops.object.light_add(type='POINT', location=(-1.6, 0.2, 2.8))
    g_light = bpy.context.active_object
    g_light.data.energy = 900
    g_light.data.color = (1.0, 0.35, 0.0)

    bpy.ops.object.light_add(type='AREA', location=(-3.0, 3.5, 2.5))
    rim = bpy.context.active_object
    rim.data.energy = 700
    rim.data.color = (0.9, 0.2, 0.3)

    return root

def salvar_e_renderizar(nome_arquivo="10_perigos_e_obstaculos_cenario"):
    # Derivado do local do script; evita recriar caminhos Windows literais.
    base_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    blends_dir = os.path.join(base_dir, "blends", "10_perigos_e_obstaculos_cenario")
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
    construir_perigos_cenario()
    salvar_e_renderizar("10_perigos_e_obstaculos_cenario")
    print("OK 10_perigos_e_obstaculos_cenario v2.0")
