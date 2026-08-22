import bpy
import math
import os

# ============================================================
# 15 - PORTAL ATLANTE 3D v1.0
# Pórtico de Obsidiana com Rúnico Dourado e Vórtice Bioluminescente
# Salva em: blends/10_perigos_e_obstaculos_cenario/15_portal_atlantica_3d.blend
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

def construir_portal():
    setup_cena()
    m_obsidiana = criar_mat("M_ObsidianaPórtico", (0.04, 0.06, 0.09, 1), metallic=0.7, roughness=0.15)
    m_ouro = criar_mat("M_OuroRúnico", (1.0, 0.84, 0.0, 1), metallic=0.9, roughness=0.2, em=True, em_cor=(1.0, 0.8, 0.2, 1), em_str=5.0)
    m_vortice = criar_mat("M_VórticeBioluminescente", (0.0, 0.7, 1.0, 1), em=True, em_cor=(0.0, 0.8, 1.0, 1), em_str=10.0)

    root = bpy.data.objects.new("PortalAtlante_Root", None)
    bpy.context.scene.collection.objects.link(root)

    # Anel externo do pórtico (Torus)
    bpy.ops.mesh.primitive_torus_add(major_radius=1.3, minor_radius=0.22, location=(0, 0, 0))
    anel = bpy.context.active_object
    anel.data.materials.append(m_obsidiana)
    anel.parent = root
    smooth(anel)

    # Anel de vórtice bioluminescente interno
    bpy.ops.mesh.primitive_circle_add(radius=1.1, fill_type='NGON', location=(0, 0.02, 0))
    vortice = bpy.context.active_object
    vortice.rotation_euler = (math.radians(90), 0, 0)
    vortice.data.materials.append(m_vortice)
    vortice.parent = root

    # Inscrições rúnicas douradas ornamentais
    for i in range(8):
        ang = i * (math.pi / 4)
        rx = math.cos(ang) * 1.3
        rz = math.sin(ang) * 1.3
        bpy.ops.mesh.primitive_ico_sphere_add(radius=0.09, subdivisions=2, location=(rx, -0.05, rz))
        runa = bpy.context.active_object
        runa.data.materials.append(m_ouro)
        runa.parent = root

    # Câmera e Iluminação
    bpy.ops.object.camera_add(location=(0, -4.5, 0))
    cam = bpy.context.active_object
    cam.rotation_euler = (math.radians(90), 0, 0)
    cam.data.lens = 45
    bpy.context.scene.camera = cam

    bpy.ops.object.light_add(type='POINT', location=(0, -1.5, 0))
    luz = bpy.context.active_object
    luz.data.energy = 800; luz.data.color = (0.2, 0.8, 1.0)

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
    construir_portal()
    salvar("15_portal_atlantica_3d")
