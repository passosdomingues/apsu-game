"""Cria o modelo low-poly de gêiser 3D usado nas fases subaquáticas."""
import bpy
import os
import sys


def project_root_arg():
    args = sys.argv[sys.argv.index("--") + 1:] if "--" in sys.argv else []
    return args[0] if args else os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))


def material(name, color, metallic=0.0, roughness=0.65):
    mat = bpy.data.materials.new(name)
    mat.diffuse_color = (*color, 1.0)
    mat.use_nodes = True
    bsdf = mat.node_tree.nodes.get("Principled BSDF")
    bsdf.inputs["Base Color"].default_value = (*color, 1.0)
    bsdf.inputs["Metallic"].default_value = metallic
    bsdf.inputs["Roughness"].default_value = roughness
    return mat


def ico(name, location, scale, subdivisions, mat):
    bpy.ops.mesh.primitive_ico_sphere_add(subdivisions=subdivisions, radius=1, location=location)
    obj = bpy.context.object
    obj.name = name
    obj.scale = scale
    obj.data.materials.append(mat)


def main():
    root = project_root_arg()
    bpy.ops.object.select_all(action="SELECT")
    bpy.ops.object.delete(use_global=False)

    rock = material("Geyser Basalt", (0.055, 0.16, 0.22), 0.18)
    rim = material("Geyser Cyan Rim", (0.08, 0.72, 0.86), 0.36, 0.28)
    water = material("Geyser Water Jet", (0.16, 0.48, 0.75), 0.08, 0.3)
    foam = material("Geyser Bubbles", (0.48, 0.9, 1.0), 0.0, 0.25)

    ico("Vent Basalt Base", (0, 0, 0.16), (0.9, 0.62, 0.32), 2, rock)
    bpy.ops.mesh.primitive_torus_add(major_segments=20, minor_segments=6,
        major_radius=0.42, minor_radius=0.095, location=(0, 0, 0.4))
    bpy.context.object.name = "Bioluminescent Vent Rim"
    bpy.context.object.data.materials.append(rim)

    bpy.ops.mesh.primitive_cone_add(vertices=12, radius1=0.32, radius2=0.075,
        depth=1.55, location=(0, 0, 1.36))
    bpy.context.object.name = "Hydrothermal Water Jet"
    bpy.context.object.data.materials.append(water)

    bubbles = (
        (-0.22, 0.02, 1.0, 0.105), (0.19, -0.04, 1.42, 0.085),
        (-0.12, -0.02, 1.87, 0.075), (0.17, 0.02, 2.2, 0.06),
    )
    for index, (x, y, z, size) in enumerate(bubbles):
        ico(f"Bubble {index + 1}", (x, y, z), (size, size, size), 1, foam)

    destination = os.path.join(root, "personagens", "blends", "10_perigos_e_obstaculos_cenario")
    os.makedirs(destination, exist_ok=True)
    bpy.ops.wm.save_as_mainfile(filepath=os.path.join(destination, "14_geiser_hidrotermal_3d.blend"))
    print("[3D] Modelo low-poly do gêiser salvo.")


main()
