"""Generate reusable low-poly 3D enemy source scenes for deep phase creatures."""
import bpy
import math
import os
import sys
from mathutils import Vector

args = sys.argv[sys.argv.index("--") + 1:] if "--" in sys.argv else []
root = args[0] if args else os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
destination = os.path.join(root, "tools", "personagens", "blends", "04_peixe_sombrio_inimigo")
os.makedirs(destination, exist_ok=True)


def clear():
    bpy.ops.object.select_all(action="SELECT")
    bpy.ops.object.delete(use_global=False)


def material(name, color, emission=0.0, metallic=0.0, roughness=.56):
    item = bpy.data.materials.new(name)
    item.diffuse_color = (*color, 1)
    item.use_nodes = True
    shader = item.node_tree.nodes.get("Principled BSDF")
    shader.inputs["Base Color"].default_value = (*color, 1)
    shader.inputs["Metallic"].default_value = metallic
    shader.inputs["Roughness"].default_value = roughness
    if "Emission Color" in shader.inputs:
        shader.inputs["Emission Color"].default_value = (*color, 1)
        shader.inputs["Emission Strength"].default_value = emission
    return item


def sphere(name, loc, scale, mat, rotation=None, subdivisions=2):
    bpy.ops.mesh.primitive_uv_sphere_add(segments=16, ring_count=8, location=loc)
    obj = bpy.context.object
    obj.name = name
    obj.scale = scale
    if rotation:
        obj.rotation_euler = rotation
    obj.data.materials.append(mat)
    for face in obj.data.polygons:
        face.use_smooth = True
    return obj


def fin(name, points, mat, depth=.05):
    mesh = bpy.data.meshes.new(name + "_mesh")
    mesh.from_pydata(points, [], [(0, 1, 2)])
    obj = bpy.data.objects.new(name, mesh)
    bpy.context.collection.objects.link(obj)
    obj.data.materials.append(mat)
    solid = obj.modifiers.new("thin fin", "SOLIDIFY")
    solid.thickness = depth
    bevel = obj.modifiers.new("soft fin edge", "BEVEL")
    bevel.width = .025
    bevel.segments = 1
    return obj


def tentacle(name, points, radius, mat):
    curve = bpy.data.curves.new(name + "_curve", "CURVE")
    curve.dimensions = "3D"
    curve.resolution_u = 2
    curve.bevel_depth = radius
    curve.bevel_resolution = 2
    spline = curve.splines.new("BEZIER")
    spline.bezier_points.add(len(points) - 1)
    for point, co in zip(spline.bezier_points, points):
        point.co = co
        point.handle_left_type = "AUTO"
        point.handle_right_type = "AUTO"
    obj = bpy.data.objects.new(name, curve)
    bpy.context.collection.objects.link(obj)
    obj.data.materials.append(mat)
    return obj


def save(name):
    curves = [obj for obj in bpy.context.scene.objects if obj.type == "CURVE"]
    if curves:
        bpy.ops.object.select_all(action="DESELECT")
        for obj in curves:
            obj.select_set(True)
        bpy.context.view_layer.objects.active = curves[0]
        bpy.ops.object.convert(target="MESH")
    meshes = [obj for obj in bpy.context.scene.objects if obj.type == "MESH"]
    if len(meshes) > 1:
        bpy.ops.object.select_all(action="DESELECT")
        for obj in meshes:
            obj.select_set(True)
        bpy.context.view_layer.objects.active = meshes[0]
        bpy.ops.object.join()
    bpy.ops.wm.save_as_mainfile(filepath=os.path.join(destination, name + ".blend"))


def make_octopus():
    clear()
    skin = material("octopus midnight mantle", (.08,.035,.19), .13, .12)
    underside = material("octopus violet underside", (.32,.06,.36), .22)
    sucker = material("cyan sucker glow", (.08,.72,.82), 1.15)
    eye = material("octopus amber eye", (1,.18,.035), 1.8)
    sphere("large_abyssal_mantle", (0,0,.35), (.63,.47,.66), skin)
    sphere("mantle_highlight", (.08,-.18,.56), (.43,.32,.39), underside)
    sphere("head_front", (.39,-.05,.08), (.40,.39,.43), skin)
    for side in (-1, 1):
        sphere("eye_ridge", (.52,side*.31,.24), (.18,.11,.21), underside)
        sphere("glowing_eye", (.57,side*.395,.25), (.075,.035,.09), eye, subdivisions=1)
    for arm in range(8):
        angle = math.pi*2*arm/8 + math.pi/8
        dx, dy = math.cos(angle), math.sin(angle)
        base = (.12, dy*.28, -.10)
        p1 = (base[0] + dx*.42, base[1] + dy*.44, -.26 + (arm%2)*.12)
        p2 = (base[0] + dx*.83, base[1] + dy*.77, -.50 + (arm%3)*.12)
        p3 = (base[0] + dx*1.15, base[1] + dy*1.08, -.30 + (arm%2)*.17)
        tentacle(f"curling_tentacle_{arm}", [base,p1,p2,p3], .09 if arm%2 else .105, skin)
        # A compact set of visible suckers gives the underside readable detail.
        for i in (1,2,3):
            t = i/4
            x = base[0]*(1-t) + p2[0]*t
            y = base[1]*(1-t) + p2[1]*t - dy*.06
            z = base[2]*(1-t) + p2[2]*t
            sphere("bioluminescent_sucker", (x,y,z), (.065,.035,.065), sucker, subdivisions=1)
    save("19_polvo_abissal_inimigo")


make_octopus()
print("[3D] Polvo abissal gerado.")
