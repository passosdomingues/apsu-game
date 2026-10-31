import bpy
import os
import sys

# Script automatizado executado pelo Blender no modo background
project_root = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
blends_dir = os.path.join(project_root, "personagens", "blends")
output_res_dir = os.path.join(project_root, "personagens", "renders", "previews")

print("=== Gerador de previews renderizados de cenas 3D Blender ===")
print(f"Buscando modelos .blend em: {blends_dir}")

# Cada PNG é uma captura renderizada de uma cena .blend 3D, não arte 2D desenhada.
# Uso seletivo: blender --background --python tools/assets/render_3d_previews.py
# -- --only nome1,nome2. Evita re-renderizar todo o catálogo quando só uma
# família de assets foi modificada.
only_names = None
only_source_paths = None
if "--" in sys.argv:
    args = sys.argv[sys.argv.index("--") + 1:]
    if "--only" in args:
        index = args.index("--only")
        if index + 1 < len(args):
            only_names = {name.strip() for name in args[index + 1].split(",") if name.strip()}
    if "--only-path" in args:
        index = args.index("--only-path")
        if index + 1 < len(args):
            only_source_paths = {
                os.path.normcase(os.path.abspath(path if os.path.isabs(path) else os.path.join(project_root, path)))
                for path in args[index + 1].split(",") if path.strip()
            }

blend_files = []
for root, dirs, files in os.walk(blends_dir):
    for f in files:
        if f.endswith(".blend"):
            if only_names is not None and os.path.splitext(f)[0] not in only_names:
                continue
            blend_path = os.path.join(root, f)
            if only_source_paths is not None and os.path.normcase(os.path.abspath(blend_path)) not in only_source_paths:
                continue
            blend_files.append(blend_path)

print(f"Encontrados {len(blend_files)} modelos 3D para renderizar.")
if not blend_files and (only_names is not None or only_source_paths is not None):
    raise SystemExit("Nenhum .blend corresponde ao filtro solicitado; verifique MODEL/--only-path.")

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
    if only_names is None and only_source_paths is None and category != "scenery":
        print(f"Ignorando preview de personagem: {basename} (o jogo usa o modelo 3D)")
        continue

    out_folder = os.path.join(output_res_dir, category, basename)
    os.makedirs(out_folder, exist_ok=True)

    print(f"Renderizando cena 3D: {basename} -> {category}")
    try:
        bpy.ops.wm.open_mainfile(filepath=blend_path)
        scene = bpy.context.scene

        # PNG transparente da cena 3D, com câmera e enquadramento consistentes.
        scene.render.film_transparent = True
        scene.render.image_settings.file_format = 'PNG'
        scene.render.image_settings.color_mode = 'RGBA'
        scene.render.resolution_x = 256
        scene.render.resolution_y = 256
        if scene.render.engine == 'BLENDER_EEVEE_NEXT':
            scene.eevee.taa_render_samples = min(scene.eevee.taa_render_samples, 8)

        # Garante câmeras e luzes
        if not scene.camera:
            cams = [o for o in scene.objects if o.type == 'CAMERA']
            if cams: scene.camera = cams[0]

        # Mantém a pose vertical do personagem e afasta a câmera para incluir
        # tridente e nadadeira em todos os quadros do sprite.
        if (basename == "01_adapa_heroi" or basename.startswith("01_adapa_ataque_")) and scene.camera:
            scene.camera.location *= 1.45

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
                import shutil
                shutil.copyfile(out_file, static_file)
            print(f"  Frame {frame} salvo em {out_file}")

    except Exception as e:
        print(f"  Aviso ao renderizar {basename}: {e}")

print("=== Previews das cenas 3D finalizados ===")
