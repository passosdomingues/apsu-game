"""Executado pelo Blender: renderiza 8 direcoes de cada Action do arquivo.

Convencao: use uma Camera chamada SpriteCamera. Cada Action (Idle, Swim, Attack)
vira uma pasta de frames PNG. Assim o GLB/Hunyuan pode ser retocado, riggado e
substituido no Blender sem qualquer alteracao no Java.
"""
import bpy
import os
import sys

args = sys.argv[sys.argv.index("--") + 1:]
output_root = args[0]
threads = int(args[1]) if len(args) > 1 else os.cpu_count() or 1
resolution = int(args[2]) if len(args) > 2 else 192
samples = int(args[3]) if len(args) > 3 else 8
directions = int(args[4]) if len(args) > 4 else 1
direction_start = int(args[5]) if len(args) > 5 else 0
direction_step = int(args[6]) if len(args) > 6 else 1
scene = bpy.context.scene
camera = bpy.data.objects.get("SpriteCamera") or scene.camera
if camera is None or camera.type != 'CAMERA':
    raise RuntimeError("Crie/selecione uma camera chamada SpriteCamera antes de exportar.")
scene.camera = camera
scene.render.engine = 'BLENDER_EEVEE_NEXT'
scene.render.resolution_x = resolution
scene.render.resolution_y = resolution
scene.render.resolution_percentage = 100
scene.render.image_settings.file_format = 'PNG'
scene.render.film_transparent = True
scene.render.threads_mode = 'FIXED'
scene.render.threads = max(1, threads)
# Perfil responsivo para sprite sheet: a resolucao final do heroi e' pequena e
# o jogador nao deve esperar minutos por oito direcoes. Aumente por ambiente:
# BLENDER_RESOLUTION=512 BLENDER_SAMPLES=64 make blender-sprites ASSET=adapa
scene.eevee.taa_render_samples = max(1, samples)
scene.eevee.use_raytracing = False
scene.eevee.volumetric_samples = 1

rigs = [obj for obj in bpy.context.scene.objects if obj.type == 'ARMATURE']
if not rigs:
    rigs = [None]  # cenario/objeto estatico: ainda gera os oito angulos
actions = list(bpy.data.actions) or [None]

for action in actions:
    action_name = action.name if action else "Static"
    action_dir = os.path.join(output_root, action_name)
    os.makedirs(action_dir, exist_ok=True)
    if action:
        for rig in rigs:
            if rig:
                if not rig.animation_data:
                    rig.animation_data_create()
                rig.animation_data.action = action
        start, end = map(int, action.frame_range)
    else:
        start = end = scene.frame_current
    for direction in range(direction_start, max(1, directions), max(1, direction_step)):
        # A camera permanece fixa; girar o rig preserva luz e enquadramento
        # idênticos em cada direção, ideal para um sprite sheet 2.5D.
        for rig in rigs:
            if rig:
                rig.rotation_euler[2] = direction * (2.0 * 3.141592653589793 / 8.0)
        for frame in range(start, end + 1):
            scene.frame_set(frame)
            scene.render.filepath = os.path.join(action_dir, f"d{direction}_f{frame:03d}.png")
            bpy.ops.render.render(write_still=True)
