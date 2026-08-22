import bpy
import math
import os

"""Corrente abissal 3D: fitas de água em espiral, sem placa retangular."""

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

def material(name, color, strength):
    mat = bpy.data.materials.new(name)
    mat.use_nodes = True
    node = mat.node_tree.nodes.get("Principled BSDF")
    node.inputs["Base Color"].default_value = color
    node.inputs["Roughness"].default_value = 0.22
    node.inputs["Emission Color"].default_value = color
    node.inputs["Emission Strength"].default_value = strength
    return mat

def ribbon(points, mat, bevel):
    curve = bpy.data.curves.new("Fita de corrente", "CURVE")
    curve.dimensions = "3D"
    curve.bevel_depth = bevel
    curve.bevel_resolution = 3
    spline = curve.splines.new("NURBS")
    spline.points.add(len(points) - 1)
    for point, coord in zip(spline.points, points): point.co = (*coord, 1)
    spline.order_u = min(3, len(points))
    spline.use_endpoint_u = True
    obj = bpy.data.objects.new("Fita de água", curve)
    bpy.context.collection.objects.link(obj)
    obj.data.materials.append(mat)

def build():
    bpy.ops.object.select_all(action="SELECT")
    bpy.ops.object.delete(use_global=False)
    scene = bpy.context.scene
    scene.render.engine = "BLENDER_EEVEE_NEXT"
    scene.render.resolution_x = 512
    scene.render.resolution_y = 512
    scene.render.film_transparent = True
    scene.render.image_settings.file_format = "PNG"

    cyan = material("Água ciano", (0.02, 0.72, 1.0, 1), 5.5)
    blue = material("Água azul profunda", (0.04, 0.18, 0.95, 1), 3.2)
    foam = material("Espuma bioluminescente", (0.55, 0.96, 1.0, 1), 8.0)

    # Três fitas helicoidais verticais, com silhueta orgânica e leitura de fluxo.
    for strand, mat in enumerate((blue, cyan, foam)):
        points = []
        for i in range(18):
            z = -1.75 + i * 0.205
            angle = i * 0.78 + strand * 2.09
            radius = 0.42 + math.sin(i * 0.42 + strand) * 0.12
            points.append((math.cos(angle) * radius, math.sin(angle) * radius * 0.35, z))
        ribbon(points, mat, 0.055 if strand < 2 else 0.032)

    for i in range(12):
        angle = i * 2.4
        bpy.ops.mesh.primitive_uv_sphere_add(radius=0.045 + (i % 3) * 0.018,
            location=(math.sin(angle) * (0.35 + (i % 2) * 0.16), -0.08, -1.55 + i * 0.28))
        bubble = bpy.context.object
        bubble.data.materials.append(foam)

    bpy.ops.object.camera_add(location=(0, -6.4, 0))
    camera = bpy.context.object
    camera.rotation_euler = (math.radians(90), 0, 0)
    camera.data.lens = 52
    scene.camera = camera
    bpy.ops.object.light_add(type="AREA", location=(0, -3, 2))
    bpy.context.object.data.energy = 850
    bpy.context.object.data.color = (0.25, 0.75, 1.0)
    bpy.context.object.data.shape = "DISK"
    bpy.context.object.data.size = 4

def save():
    name = "16_corrente_abissal_3d"
    blend_dir = os.path.join(BASE_DIR, "blends", "10_perigos_e_obstaculos_cenario")
    render_dir = os.path.join(BASE_DIR, "renders")
    os.makedirs(blend_dir, exist_ok=True)
    os.makedirs(render_dir, exist_ok=True)
    bpy.ops.wm.save_as_mainfile(filepath=os.path.join(blend_dir, name + ".blend"))
    bpy.context.scene.render.filepath = os.path.join(render_dir, name + ".png")
    bpy.ops.render.render(write_still=True)

if __name__ == "__main__":
    build()
    save()
