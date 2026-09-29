"""Create an irregular low-poly 3D lava basin used by phase four hazards."""
import bpy
import math
import os
import sys

args = sys.argv[sys.argv.index("--") + 1:] if "--" in sys.argv else []
root = args[0] if args else os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
destination = os.path.join(root, "personagens", "blends", "10_perigos_e_obstaculos_cenario")
os.makedirs(destination, exist_ok=True)

bpy.ops.object.select_all(action="SELECT")
bpy.ops.object.delete(use_global=False)

def material(name, color, emission=0.0, metallic=0.0):
    mat = bpy.data.materials.new(name)
    mat.diffuse_color = (*color, 1)
    mat.use_nodes = True
    shader = mat.node_tree.nodes.get("Principled BSDF")
    shader.inputs["Base Color"].default_value = (*color, 1)
    shader.inputs["Metallic"].default_value = metallic
    shader.inputs["Roughness"].default_value = .84
    if "Emission Color" in shader.inputs:
        shader.inputs["Emission Color"].default_value = (*color, 1)
        shader.inputs["Emission Strength"].default_value = emission
    return mat

basalt = material("basalt dark crust", (.045, .055, .075))
edge = material("warm basalt edges", (.17, .075, .045))
lava = material("molten orange core", (1.0, .12, .012), 2.2)
hot = material("molten gold fissures", (1.0, .47, .035), 3.0)

# Scalloped dark basalt bank, with a shallow recess around the molten center.
segments = 18
vertices, faces = [], []
for ring, (rx, rz) in enumerate(((1.0, .42), (.77, .29), (.55, .15))):
    for i in range(segments):
        a = 2 * math.pi * i / segments
        irregular = 1 + .09 * math.sin(i * 5.3) + .045 * math.cos(i * 2.7)
        x, z = math.cos(a) * rx * irregular, math.sin(a) * rz * irregular
        y = -.04 - .025 * math.sin(i * 1.9) if ring < 2 else .005
        vertices.append((x, y, z))
for ring in range(2):
    for i in range(segments):
        a = ring * segments + i
        b = ring * segments + (i + 1) % segments
        c = (ring + 1) * segments + (i + 1) % segments
        d = (ring + 1) * segments + i
        faces.append((a, b, c, d))
mesh = bpy.data.meshes.new("uneven_scalloped_basalt_rim")
mesh.from_pydata(vertices, [], faces)
rim = bpy.data.objects.new("irregular_basalt_lava_basin", mesh)
bpy.context.collection.objects.link(rim)
rim.data.materials.append(basalt)
rim.data.materials.append(edge)
for poly in mesh.polygons:
    poly.material_index = 1 if poly.index % 4 == 0 else 0

# Recessed, organic molten pool as a triangulated fan, no rectangular slab.
pool_verts = [(0, .01, 0)]
pool_faces = []
for i in range(segments):
    a = 2 * math.pi * i / segments
    irregular = .52 * (1 + .075 * math.sin(i * 5.3) + .04 * math.cos(i * 2.7))
    pool_verts.append((math.cos(a) * irregular, -.006, math.sin(a) * .14 * irregular))
for i in range(segments):
    pool_faces.append((0, i + 1, (i + 1) % segments + 1))
pool_mesh = bpy.data.meshes.new("faceted_molten_pool")
pool_mesh.from_pydata(pool_verts, [], pool_faces)
pool = bpy.data.objects.new("glowing_lava_inset", pool_mesh)
bpy.context.collection.objects.link(pool)
pool.data.materials.append(lava)

# A few thin, branching fissures glow through the basalt edge.
for index, angle in enumerate((.35, 2.15, 4.05, 5.25)):
    x1, z1 = math.cos(angle) * .56, math.sin(angle) * .16
    x2, z2 = math.cos(angle + .15) * .78, math.sin(angle + .15) * .28
    bpy.ops.mesh.primitive_cone_add(vertices=5, radius1=.012, radius2=.008,
                                    depth=math.hypot(x2-x1, z2-z1),
                                    location=((x1+x2)/2, -.03, (z1+z2)/2))
    fissure = bpy.context.object
    fissure.name = f"glowing_lava_fissure_{index}"
    fissure.rotation_euler[1] = -math.atan2(z2-z1, x2-x1)
    fissure.data.materials.append(hot)

bpy.ops.wm.save_as_mainfile(filepath=os.path.join(destination, "17_piscina_lava_3d.blend"))
print("[3D] Modelo 3D low-poly de piscina de lava criado.")
