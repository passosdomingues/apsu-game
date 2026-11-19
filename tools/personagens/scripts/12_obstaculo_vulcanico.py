import bpy
import math
import os

# ============================================================
# 12 - OBSTACULO VULCANICO v1.0
# Elemento de cenário para Fase 4 e 5
# Salva em: blends/10_perigos_e_obstaculos_cenario/12_obstaculo_vulcanico.blend
# ============================================================

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

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

def smooth(obj):
    if obj and obj.type == 'MESH':
        for p in obj.data.polygons: p.use_smooth = True

def construir_obstaculo():
    setup_cena()
    m_rocha = criar_mat("M_RochaBasalto", (0.05, 0.03, 0.02, 1), metallic=0.1, roughness=0.8)
    m_magma = criar_mat("M_MagmaBrilho", (1.0, 0.25, 0.0, 1), em=True, em_cor=(1.0, 0.25, 0.0, 1), em_str=8.0)

    root = bpy.data.objects.new("ObstaculoVulcanico_Root", None)
    bpy.context.scene.collection.objects.link(root)

    # Pilar de rocha vulcânica principal
    bpy.ops.mesh.primitive_cylinder_add(vertices=7, radius=0.8, depth=2.5, location=(0, 0, 0))
    pilar = bpy.context.active_object
    pilar.data.materials.append(m_rocha)
    pilar.parent = root
    smooth(pilar)

    # Fissuras de lava
    for i in range(4):
        bpy.ops.mesh.primitive_ico_sphere_add(radius=0.18, subdivisions=2, location=(math.sin(i*1.5)*0.75, math.cos(i*1.5)*0.75, -0.6 + i*0.4))
        fiss = bpy.context.active_object
        fiss.data.materials.append(m_magma)
        fiss.parent = root

    # Câmera e luz
    bpy.ops.object.camera_add(location=(0, -5.5, 0))
    cam = bpy.context.active_object
    cam.rotation_euler = (math.radians(90), 0, 0)
    cam.data.lens = 45
    bpy.context.scene.camera = cam

    bpy.ops.object.light_add(type='POINT', location=(0, -2.0, 0))
    luz = bpy.context.active_object
    luz.data.energy = 600; luz.data.color = (1.0, 0.3, 0.0)

def salvar(nome):
    bdir = os.path.join(BASE_DIR, "blends", "10_perigos_e_obstaculos_cenario")
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
    construir_obstaculo()
    salvar("12_obstaculo_vulcanico")
