"""Render all phase panoramas from the layered phase-one Blender scene.

The source scene keeps the same camera and Fundo/Meio/Frente depth planes in
every output. Later phases selectively remove decorative silhouettes and grade
the rendered image darker and less saturated to suggest greater ocean depth.
"""
import os
import sys

import bpy


def project_root():
    args = sys.argv[sys.argv.index("--") + 1:] if "--" in sys.argv else []
    return args[0] if args else os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))


SCRIPT_ARGS = sys.argv[sys.argv.index("--") + 1:] if "--" in sys.argv else []
LAYERS_ONLY = "--layers-only" in SCRIPT_ARGS
PHASE_ONLY = None
if "--phase-only" in SCRIPT_ARGS:
    phase_arg = SCRIPT_ARGS.index("--phase-only") + 1
    if phase_arg < len(SCRIPT_ARGS):
        PHASE_ONLY = int(SCRIPT_ARGS[phase_arg])
ROOT = project_root()
SOURCE = os.path.join(ROOT, "personagens", "fases", "fase1.blend")
OUTPUT = os.path.join(ROOT, "src", "main", "resources", "backgrounds")
WIDTH, HEIGHT = 3072, 768

# Saturation and exposure step down with each deeper region. The mild color
# transform is composited after rendering so image textures are treated too.
PHASE_GRADE = {
    1: (1.00, 0.00),
    2: (0.88, -0.32),
    3: (0.72, -0.68),
    4: (0.58, -1.02),
    5: (0.48, -1.34),
}


def layer_of(obj):
    for collection in obj.users_collection:
        if collection.name in ("Fundo", "Meio", "Frente"):
            return collection.name
    return ""


def omit_from_phase(obj, phase):
    """Cull decorative layers in a stable way while retaining scene depth."""
    if phase == 1 or obj.type not in {"MESH", "CURVE"}:
        return False

    name = obj.name.casefold()
    layer = layer_of(obj)
    decorative = any(token in name for token in (
        "alga", "amarelo", "laranja", "rosa", "roxo", "turquesa",
        "vermelho", "bolha", "raio",
    ))
    if phase >= 2 and layer == "Frente" and decorative:
        return True
    if phase >= 3 and layer == "Meio" and decorative:
        return True
    if phase >= 4 and layer == "Fundo" and decorative:
        return True
    if phase == 5 and layer == "Meio" and ("boca" in name or "pedra" in name):
        return True
    # From phase 3 onward, keep only roughly half the foreground silhouettes;
    # later regions expose more negative space and make the ruins feel sparse.
    if layer == "Frente" and phase >= 3:
        if phase == 5:
            return "rocha" not in name
        if phase == 4:
            return "rocha" not in name
        if phase == 3:
            return sum(ord(ch) for ch in obj.name) % 2 == 0
        return sum(ord(ch) for ch in obj.name) % (3 if phase == 4 else 5) != 0
    return False


def set_depth_grade(scene, phase):
    nodes = scene.node_tree.nodes
    nodes.clear()
    render = nodes.new("CompositorNodeRLayers")
    grade = nodes.new("CompositorNodeHueSat")
    grade.inputs["Saturation"].default_value = PHASE_GRADE[phase][0]
    grade.inputs["Value"].default_value = 2.0 ** PHASE_GRADE[phase][1]
    composite = nodes.new("CompositorNodeComposite")
    scene.node_tree.links.new(render.outputs["Image"], grade.inputs["Image"])
    scene.node_tree.links.new(grade.outputs["Image"], composite.inputs["Image"])


def render_phase(phase):
    bpy.ops.wm.open_mainfile(filepath=SOURCE)
    scene = bpy.context.scene
    camera = bpy.data.objects.get("CamCenario")
    if camera is None or camera.type != "CAMERA":
        raise RuntimeError("A cena fase1.blend precisa da câmera CamCenario.")
    expected_layers = {"Fundo", "Meio", "Frente"}
    present_layers = {collection.name for collection in bpy.data.collections}
    if not expected_layers.issubset(present_layers):
        raise RuntimeError("A cena fase1.blend precisa das coleções Fundo, Meio e Frente.")

    scene.camera = camera
    scene.render.engine = "BLENDER_EEVEE_NEXT"
    scene.eevee.taa_render_samples = 4 if LAYERS_ONLY else 8
    scene.render.resolution_x = WIDTH
    scene.render.resolution_y = HEIGHT
    scene.render.resolution_percentage = 100
    scene.render.image_settings.file_format = "PNG"
    scene.render.image_settings.color_depth = "8"
    scene.render.image_settings.compression = 20
    scene.render.film_transparent = False
    scene.view_settings.view_transform = "Standard"
    scene.view_settings.look = "Medium High Contrast"
    scene.use_nodes = True
    set_depth_grade(scene, phase)

    hidden = 0
    for obj in scene.objects:
        obj.hide_render = omit_from_phase(obj, phase)
        hidden += int(obj.hide_render)
    phase_visibility = {obj.name: obj.hide_render for obj in scene.objects}

    os.makedirs(OUTPUT, exist_ok=True)
    path = os.path.join(OUTPUT, f"phase-{phase}.png")
    if not LAYERS_ONLY:
        scene.render.image_settings.color_mode = "RGB"
        scene.render.film_transparent = False
        scene.render.filepath = path
        bpy.ops.render.render(write_still=True)
        print(f"[PANORAMA 3D] Fase {phase}: {path} ({hidden} elementos decorativos ocultos)", flush=True)

    # Export matching transparent planes so the game can apply independent
    # parallax speeds without re-rendering or changing the camera composition.
    scene.render.image_settings.color_mode = "RGBA"
    scene.render.film_transparent = True
    for layer in ("Fundo", "Meio", "Frente"):
        for obj in scene.objects:
            obj.hide_render = phase_visibility[obj.name] or (obj.type in {"MESH", "CURVE"} and layer_of(obj) != layer)
        layer_path = os.path.join(OUTPUT, f"phase-{phase}-{layer.casefold()}.png")
        scene.render.filepath = layer_path
        bpy.ops.render.render(write_still=True)
        print(f"[PANORAMA 3D] Fase {phase} / {layer}: {layer_path}", flush=True)


if not os.path.isfile(SOURCE):
    raise FileNotFoundError("Cena-base não encontrada: " + SOURCE)

for phase in ([PHASE_ONLY] if PHASE_ONLY is not None else range(1, 6)):
    render_phase(phase)

print("[PANORAMA 3D] Panoramas derivados do render da cena fase1.blend.")
