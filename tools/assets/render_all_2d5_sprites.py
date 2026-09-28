import bpy
import os
import sys

# Script automatizado executado pelo Blender no modo background
project_root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
blends_dir = os.path.join(project_root, "personagens", "blends")
output_res_dir = os.path.join(project_root, "src", "main", "resources", "sprites")

print(f"=== Renderizador 2.5D de Sprites Animados ===")
print(f"Buscando modelos .blend em: {blends_dir}")

# Uso seletivo: blender --background --python tools/render_all_2d5_sprites.py
# -- --only nome1,nome2. Evita re-renderizar todo o catálogo quando só uma
# família de assets foi modificada.
only_names = None
if "--" in sys.argv:
    args = sys.argv[sys.argv.index("--") + 1:]
    if "--only" in args:
        index = args.index("--only")
        if index + 1 < len(args):
            only_names = {name.strip() for name in args[index + 1].split(",") if name.strip()}

blend_files = []
for root, dirs, files in os.walk(blends_dir):
    for f in files:
        if f.endswith(".blend"):
            if only_names is not None and os.path.splitext(f)[0] not in only_names:
                continue
            blend_files.append(os.path.join(root, f))

print(f"Encontrados {len(blend_files)} modelos 3D para renderizar.")

for blend_path in blend_files:
    basename = os.path.splitext(os.path.basename(blend_path))[0]
    category = "scenery"
    if "adapa" in basename: category = "adapa"
    elif "kullullu" in basename: category = "kullullu"
    elif "enki" in basename: category = "enki"
    elif "inimigo" in basename or "peixe" in basename or "arraiao" in basename or "leviata" in basename: category = "inimigos"
    elif "guardiao" in basename: category = "guardioes"

    # No fluxo completo, personagens são malhas 3D de runtime (OBJ/MTL),
    # nunca PNGs usados como representação no jogo. `--only` segue disponível
    # para renders de preview/ataque solicitados explicitamente.
    if only_names is None and category != "scenery":
        print(f"Ignorando preview 2D de personagem: {basename} (o jogo usa o modelo 3D)")
        continue

    out_folder = os.path.join(output_res_dir, category, basename)
    os.makedirs(out_folder, exist_ok=True)

    print(f"Renderizando 2.5D: {basename} -> {category}")
    try:
        bpy.ops.wm.open_mainfile(filepath=blend_path)
        scene = bpy.context.scene

        # Configurar renderizador transparente 2.5D
        scene.render.film_transparent = True
        scene.render.image_settings.file_format = 'PNG'
        scene.render.resolution_x = 256
        scene.render.resolution_y = 256

        # Garante câmeras e luzes
        if not scene.camera:
            cams = [o for o in scene.objects if o.type == 'CAMERA']
            if cams: scene.camera = cams[0]

        # Renderiza 8 quadros de ciclo de animação (auditoria 2026-08-15:
        # subiu de 6 para 8 para acompanhar swim_cycle_keyframes() da
        # _apsu_shared_lib.py, que agora baka poses REAIS nesses frames
        # — antes eram 6 renders idênticos porque nenhum script inseria
        # keyframes; scene.frame_set(N) não fazia nada sem keyframes).
        static_file = os.path.join(output_res_dir, category, f"{basename}.png")
        for frame in range(1, 9):
            scene.frame_set(frame)
            out_file = os.path.join(out_folder, f"frame_{frame:03d}.png")
            scene.render.filepath = out_file
            bpy.ops.render.render(write_still=True)
            if frame == 1:
                scene.render.filepath = static_file
                bpy.ops.render.render(write_still=True)
            print(f"  Frame {frame} salvo em {out_file}")

    except Exception as e:
        print(f"  Aviso ao renderizar {basename}: {e}")

print("=== Renderização 2.5D Finalizada ===")
