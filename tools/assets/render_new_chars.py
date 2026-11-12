"""render_new_chars.py -- Blender background script.
Renders 2D sprite sheets (8 frames, 256x256 PNG, transparent) and exports
OBJ/MTL runtime models for:
  - 02_kullullu_boss       (boss atualizado)
  - 11_arraiao_abissal     (arraiao abissal atualizado)
  - 12_boto-do-mal         -> 12_boto_do_mal_inimigo
  - 13_golfinho-do-mal     -> 13_golfinho_do_mal_inimigo

Uso:
  /opt/blender-4.5.5-lts/blender --background --python tools/assets/render_new_chars.py
"""
import bpy
import math
import os
import shutil
import sys

PROJECT_ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
ENEMY_SPRITE_OUT = os.path.join(PROJECT_ROOT, "src", "main", "resources", "sprites", "inimigos")
KULLULLU_SPRITE_OUT = os.path.join(PROJECT_ROOT, "src", "main", "resources", "sprites", "kullullu")
MODEL_OUT = os.path.join(PROJECT_ROOT, "src", "main", "resources", "models", "characters")
BLEND_ROOT = os.path.join(PROJECT_ROOT, "personagens", "blends")

# (blend_path, output_name, is_boss)
JOBS = [
    (os.path.join(BLEND_ROOT, "02_kullullu_boss", "02_kullullu_boss.blend"),
     "02_kullullu_boss", True),
    (os.path.join(BLEND_ROOT, "11_arraiao_abissal", "11_arraiao_abissal.blend"),
     "11_arraiao_abissal", False),
    (os.path.join(BLEND_ROOT, "12_boto-do-mal", "boto do mal.blend"),
     "12_boto_do_mal_inimigo", False),
    (os.path.join(BLEND_ROOT, "13_golfinho-do-mal", "golfinho do mal.blend"),
     "13_golfinho_do_mal_inimigo", False),
]


def setup_camera_lateral(scene, is_boss=False):
    cam = None
    for obj in scene.objects:
        if obj.type == 'CAMERA':
            cam = obj
            break
    if cam is None:
        bpy.ops.object.camera_add()
        cam = bpy.context.object
    scene.camera = cam
    dist = 8.0 if is_boss else 6.0
    cam.location = (0, dist, 0.1)
    cam.rotation_euler = (math.radians(90), 0, 0)
    cam.data.lens = 55


def ensure_lighting(scene):
    lights = [o for o in scene.objects if o.type == 'LIGHT']
    if lights:
        return
    for loc, strength in [((3, -4, 5), 400), ((-3, -2, 3), 150), ((0, 5, -1), 80)]:
        bpy.ops.object.light_add(type='POINT', location=loc)
        bpy.context.object.data.energy = strength


def render_sprites(blend_path, output_name, is_boss=False):
    sprite_dir = KULLULLU_SPRITE_OUT if is_boss else ENEMY_SPRITE_OUT
    frames_dir = os.path.join(sprite_dir, output_name)
    os.makedirs(frames_dir, exist_ok=True)
    bpy.ops.wm.open_mainfile(filepath=blend_path)
    scene = bpy.context.scene
    setup_camera_lateral(scene, is_boss)
    ensure_lighting(scene)
    scene.render.film_transparent = True
    scene.render.image_settings.file_format = 'PNG'
    scene.render.image_settings.color_mode = 'RGBA'
    scene.render.resolution_x = 256
    scene.render.resolution_y = 256
    scene.render.resolution_percentage = 100
    if scene.render.engine in ('BLENDER_EEVEE', 'BLENDER_EEVEE_NEXT'):
        scene.eevee.taa_render_samples = 8
    static_out = os.path.join(sprite_dir, output_name + ".png")
    for frame in range(1, 9):
        scene.frame_set(frame)
        out = os.path.join(frames_dir, f"frame_{frame:03d}.png")
        scene.render.filepath = out
        bpy.ops.render.render(write_still=True)
        print(f"  [sprite] frame {frame} -> {out}")
        if frame == 1:
            shutil.copyfile(out, static_out)
    print(f"[sprite] {output_name} -> {static_out}")


def export_obj(blend_path, output_name, is_boss=False):
    target_dir = os.path.join(MODEL_OUT, output_name)
    os.makedirs(target_dir, exist_ok=True)
    target = os.path.join(target_dir, output_name + ".obj")
    bpy.ops.wm.open_mainfile(filepath=blend_path)
    scene = bpy.context.scene
    scene.frame_set(scene.frame_start)
    lod_ratio = 0.42 if is_boss else 0.55
    for obj in scene.objects:
        if obj.type != 'MESH' or len(obj.data.polygons) < 80 or obj.data.shape_keys:
            continue
        mod = obj.modifiers.new("LOD", "DECIMATE")
        mod.ratio = lod_ratio
        bpy.ops.object.select_all(action='DESELECT')
        obj.select_set(True)
        bpy.context.view_layer.objects.active = obj
        try:
            bpy.ops.object.modifier_apply(modifier=mod.name)
        except RuntimeError as e:
            obj.modifiers.remove(mod)
            print(f"  [3D] LOD ignorado em {obj.name}: {e}")
    bpy.ops.wm.obj_export(
        filepath=target, forward_axis='NEGATIVE_Y', up_axis='Z',
        global_scale=1.0, apply_modifiers=True,
        export_eval_mode='DAG_EVAL_VIEWPORT', export_selected_objects=False,
        export_uv=True, export_normals=True, export_materials=True,
        export_pbr_extensions=False, path_mode='RELATIVE',
        export_triangulated_mesh=True, export_object_groups=False,
        export_material_groups=True, export_smooth_groups=True,
    )
    print(f"[3D] {output_name} -> {target}")


for blend_path, output_name, is_boss in JOBS:
    if not os.path.isfile(blend_path):
        print(f"[SKIP] blend nao encontrado: {blend_path}")
        continue
    print(f"\n=== Processando {output_name} ===")
    try:
        render_sprites(blend_path, output_name, is_boss)
    except Exception as e:
        print(f"[WARN] render_sprites falhou: {e}")
    try:
        export_obj(blend_path, output_name, is_boss)
    except Exception as e:
        print(f"[WARN] export_obj falhou: {e}")

print("\n=== render_new_chars.py concluido ===")
