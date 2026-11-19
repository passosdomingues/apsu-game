import bpy
import math
import os

# ============================================================
# 11 - ARRAIAO ABISSAL v1.0
# Inimigo da Fase 4 (Abismo Vulcânico)
# Salva em: blends/04_peixe_sombrio_inimigo/11_arraiao_abissal.blend
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

def construir_arraiao():
    setup_cena()
    m_corpo = criar_mat("M_ArraiaCorpo", (0.02, 0.08, 0.22, 1), metallic=0.4, roughness=0.25)
    m_ventre = criar_mat("M_ArraiaVentre", (0.1, 0.2, 0.4, 1), metallic=0.2, roughness=0.3)
    m_neon  = criar_mat("M_ArraiaNeon",  (0.0, 0.85, 1.0, 1), em=True, em_cor=(0.0, 0.85, 1.0, 1), em_str=7.0)

    root = bpy.data.objects.new("Arraiao_Root", None)
    bpy.context.scene.collection.objects.link(root)

    # Corpo central discoidal achatado
    bpy.ops.mesh.primitive_uv_sphere_add(segments=32, ring_count=24, radius=1.0, location=(0, 0, 0))
    corpo = bpy.context.active_object
    corpo.scale = (1.8, 1.4, 0.22)
    corpo.data.materials.append(m_corpo)
    corpo.parent = root
    smooth(corpo)

    # Asas laterais (mantas)
    for s in [-1, 1]:
        bpy.ops.mesh.primitive_cone_add(vertices=4, radius1=1.2, depth=2.4, location=(0, s*1.3, 0))
        asa = bpy.context.active_object
        asa.rotation_euler = (math.radians(s*90), 0, math.radians(90))
        asa.scale = (0.12, 1.0, 0.8)
        asa.data.materials.append(m_corpo)
        asa.parent = root
        smooth(asa)

    # Cauda chicote
    bpy.ops.mesh.primitive_cylinder_add(radius=0.08, depth=2.8, location=(-2.0, 0, 0))
    cauda = bpy.context.active_object
    cauda.rotation_euler = (0, math.radians(90), 0)
    cauda.data.materials.append(m_corpo)
    cauda.parent = root
    smooth(cauda)

    # Olhos bioluminescentes
    for s in [-1, 1]:
        bpy.ops.mesh.primitive_uv_sphere_add(radius=0.12, location=(0.8, s*0.4, 0.15))
        ol = bpy.context.active_object
        ol.data.materials.append(m_neon)
        ol.parent = root

    # Marcações neon nas asas
    for s in [-1, 1]:
        for i in range(3):
            bpy.ops.mesh.primitive_ico_sphere_add(radius=0.08, subdivisions=2, location=(0.2 - i*0.4, s*(0.8 + i*0.3), 0.1))
            m = bpy.context.active_object
            m.data.materials.append(m_neon)
            m.parent = root

    # Câmera e luz
    bpy.ops.object.camera_add(location=(0, -6.5, 1.5))
    cam = bpy.context.active_object
    cam.rotation_euler = (math.radians(75), 0, 0)
    cam.data.lens = 40
    bpy.context.scene.camera = cam

    bpy.ops.object.light_add(type='AREA', location=(1.5, -4.5, 3.5))
    kl = bpy.context.active_object
    kl.data.energy = 500; kl.data.size = 3.5
    kl.data.color = (0.2, 0.7, 1.0)

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
    construir_arraiao()
    salvar("11_arraiao_abissal")
