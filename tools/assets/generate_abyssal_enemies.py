"""Generate reusable low-poly 3D enemy source scenes for deep phase creatures."""
import bpy
import math
import os
import sys
from mathutils import Vector

args = sys.argv[sys.argv.index("--") + 1:] if "--" in sys.argv else []
root = args[0] if args else os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
destination = os.path.join(root, "personagens", "blends", "04_peixe_sombrio_inimigo")
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


def make_dolphin():
    clear()
    slate = material("abyssal dolphin blue-black skin", (.035,.12,.20), .08, .16)
    belly = material("cold pale underbelly", (.28,.43,.48), .10)
    finmat = material("indigo fins", (.055,.12,.25), .08, .12)
    scar = material("bioluminescent scars", (.16,.82,.88), 1.1)
    eye = material("hostile magenta eye", (1,.025,.14), 2.0)
    # Body is aligned horizontally; the tapered rostrum points toward +X.
    sphere("sleek_dolphin_torso", (0,0,0), (1.05,.43,.42), slate, (0,0,0))
    sphere("dolphin_belly", (.05,-.018,-.20), (.79,.39,.22), belly)
    sphere("long_predatory_rostrum", (.91,-.01,-.035), (.55,.22,.20), slate, (0,.13,0))
    fin("curved_dorsal_fin", [(-.42,0,.31),(-.16,0,.86),(.23,0,.30)], finmat)
    fin("left_pectoral_fin", [(-.05,-.25,-.05),(.63,-.33,-.56),(.35,-.16,.02)], finmat)
    fin("right_pectoral_fin", [(-.05,.25,-.05),(.63,.33,-.56),(.35,.16,.02)], finmat)
    fin("tail_fluke_left", [(-.83,0,0),(-1.42,0,.45),(-1.16,0,-.05)], finmat)
    fin("tail_fluke_right", [(-.83,0,0),(-1.42,0,-.45),(-1.16,0,.05)], finmat)
    sphere("eye_socket", (.52,-.39,.035), (.14,.06,.14), finmat)
    sphere("glowing_malicious_eye", (.55,-.445,.04), (.073,.025,.074), eye, subdivisions=1)
    for i in range(3):
        sphere("glowing_lateral_stripe", (-.45+i*.20,-.425,.1-i*.055), (.07,.018,.018), scar, subdivisions=1)
    save("18_delfim_abissal_inimigo")


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


def make_vampire_squid():
    clear()
    mantle = material("vampire squid dark burgundy", (.105,.025,.12), .12, .16)
    web = material("scarlet webbing", (.39,.045,.12), .25)
    glow = material("violet photophores", (.25,.035,.9), 1.6)
    eye = material("cold cyan eye", (.045,.9,1), 2.1)
    sphere("vampire_squid_mantle", (0,0,.12), (.72,.48,.56), mantle)
    sphere("mantle_cape", (-.1,0,-.12), (.82,.50,.25), web)
    sphere("mouth", (.51,-.11,-.05), (.22,.20,.24), mantle)
    for side in (-1,1):
        sphere("eye_socket", (.36,side*.37,.24), (.20,.10,.19), mantle)
        sphere("glowing_cyan_eye", (.42,side*.445,.25), (.09,.035,.10), eye, subdivisions=1)
        # Two broad webbed arms frame six short trailing arms.
        tentacle("vampire_cape_arm", [(-.25,side*.20,-.02),(-.72,side*.52,-.48),(-1.05,side*.62,-.18)], .13, web)
        for i in range(3):
            x = -.18 - i*.08
            tentacle("trailing_luminous_arm", [(x,side*.2,-.12),(x-.16,side*.42,-.56),(x-.40,side*(.52+i*.08),-.80)], .047, mantle)
            sphere("photophore", (x-.39,side*(.51+i*.08),-.79), (.07,.055,.07), glow, subdivisions=1)
    save("20_lula_vampira_inimigo")


make_dolphin()
make_octopus()
make_vampire_squid()
print("[3D] Golfinho predador, polvo abissal e lula vampira gerados.")
