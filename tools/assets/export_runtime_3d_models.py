"""Exporta os personagens do Blender como malhas 3D consumíveis pelo jogo.

Os PNGs continuam servindo para elementos 2D de UI/compatibilidade. A cena
do jogo usa os OBJ/MTL daqui para personagens renderizados em tempo real.
"""
import bpy
import os
import sys


def args_after_separator():
    return sys.argv[sys.argv.index("--") + 1:] if "--" in sys.argv else []


args = args_after_separator()
project_root = args[0] if args else os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
only_models = None
fix_mtls_only = "--fix-mtls-only" in args
if "--only" in args:
    only_index = args.index("--only")
    if only_index + 1 < len(args):
        only_models = {name.strip() for name in args[only_index + 1].split(",") if name.strip()}
blend_root = os.path.join(project_root, "personagens", "blends")
output_root = os.path.join(project_root, "src", "main", "resources", "models")

exported = 0
baked_material_paths = {}


def export_obj(target):
    bpy.ops.wm.obj_export(
        filepath=target,
        forward_axis='NEGATIVE_Y',
        up_axis='Z',
        global_scale=1.0,
        apply_modifiers=True,
        export_eval_mode='DAG_EVAL_VIEWPORT',
        export_selected_objects=False,
        export_uv=True,
        export_normals=True,
        export_materials=True,
        export_pbr_extensions=False,
        path_mode='RELATIVE',
        export_triangulated_mesh=True,
        export_object_groups=False,
        export_material_groups=True,
        export_smooth_groups=True,
    )
    if not os.path.isfile(target) or os.path.getsize(target) < 100:
        raise RuntimeError("Export OBJ vazio: " + target)


def bake_material_textures(target_dir):
    """Bake procedural Blender materials to UV textures usable by JavaFX."""
    baked_material_paths.clear()
    scene = bpy.context.scene
    scene.render.engine = 'CYCLES'
    scene.cycles.samples = 8
    scene.cycles.bake_type = 'DIFFUSE'
    scene.render.bake.use_pass_color = True
    scene.render.bake.use_pass_direct = False
    scene.render.bake.use_pass_indirect = False
    scene.render.bake.margin = 8
    texture_dir = os.path.join(target_dir, "textures")
    os.makedirs(texture_dir, exist_ok=True)

    meshes = [obj for obj in scene.objects if obj.type == 'MESH' and obj.data.polygons]
    for obj_index, obj in enumerate(meshes):
        if not obj.data.uv_layers:
            bpy.ops.object.select_all(action='DESELECT')
            obj.select_set(True)
            bpy.context.view_layer.objects.active = obj
            bpy.ops.object.mode_set(mode='EDIT')
            bpy.ops.mesh.select_all(action='SELECT')
            bpy.ops.uv.smart_project(island_margin=0.025)
            bpy.ops.object.mode_set(mode='OBJECT')

        image_nodes = []
        for slot_index, slot in enumerate(obj.material_slots):
            if slot.material is None:
                continue
            material = slot.material.copy()
            slot.material = material
            material.use_nodes = True
            nodes = material.node_tree.nodes
            image = bpy.data.images.new(
                f"{obj_index:03d}_{slot_index:02d}_{obj.name}_{material.name}",
                width=256, height=256, alpha=True,
            )
            image.filepath_raw = os.path.join(texture_dir, f"{obj_index:03d}_{slot_index:02d}.png")
            image.file_format = 'PNG'
            image_node = nodes.new('ShaderNodeTexImage')
            image_node.image = image
            for node in nodes:
                node.select = False
            image_node.select = True
            nodes.active = image_node
            image_nodes.append((material, image_node, image))
            baked_material_paths[material.name] = image.filepath_raw

        if not image_nodes:
            continue
        bpy.ops.object.select_all(action='DESELECT')
        obj.select_set(True)
        bpy.context.view_layer.objects.active = obj
        bpy.ops.object.bake(type='DIFFUSE', pass_filter={'COLOR'}, use_clear=True, margin=8)

        for material, image_node, image in image_nodes:
            image.save()
            base_color = material.node_tree.nodes.get('Principled BSDF')
            if base_color is None:
                base_color = next((node for node in material.node_tree.nodes if node.type == 'BSDF_PRINCIPLED'), None)
            if base_color is None:
                continue
            socket = base_color.inputs.get('Base Color')
            if socket is None:
                continue
            for link in list(socket.links):
                material.node_tree.links.remove(link)
            material.node_tree.links.new(image_node.outputs['Color'], socket)


def ensure_mtl_textures(mtl_path):
    """Keep every baked material mapped, including shaders unsupported by OBJ export."""
    if not os.path.isfile(mtl_path):
        raise RuntimeError("MTL não encontrado após exportação: " + mtl_path)
    with open(mtl_path, "r", encoding="utf-8") as source:
        lines = source.readlines()

    result = []
    current_material = None
    has_texture = False
    for line in lines:
        if line.startswith("newmtl "):
            if current_material and not has_texture and current_material in baked_material_paths:
                image_path = baked_material_paths[current_material]
                relative = os.path.relpath(image_path, os.path.dirname(mtl_path)).replace(os.sep, "/")
                result.append("map_Kd " + relative + "\n")
            current_material = line[7:].strip()
            has_texture = False
        elif line.startswith("map_Kd "):
            has_texture = True
        result.append(line)
    if current_material and not has_texture and current_material in baked_material_paths:
        image_path = baked_material_paths[current_material]
        relative = os.path.relpath(image_path, os.path.dirname(mtl_path)).replace(os.sep, "/")
        result.append("map_Kd " + relative + "\n")
    with open(mtl_path, "w", encoding="utf-8") as target:
        target.writelines(result)


def collect_existing_texture_paths(target_dir):
    """Rebuild the deterministic Blender material-to-bake filename mapping."""
    baked_material_paths.clear()
    meshes = [obj for obj in bpy.context.scene.objects if obj.type == 'MESH' and obj.data.polygons]
    for obj_index, obj in enumerate(meshes):
        for slot_index, slot in enumerate(obj.material_slots):
            if slot.material is None:
                continue
            material = slot.material.copy()
            slot.material = material
            baked_material_paths[material.name] = os.path.join(
                target_dir, "textures", f"{obj_index:03d}_{slot_index:02d}.png")


def simplify_runtime_scenery(model_name, scene):
    """Decimate busy environmental meshes before they enter the 60 FPS scene."""
    ratios = {
        "08_recifes_e_cardume_elemento-cenario": 0.16,
        "16_corrente_abissal_3d": 0.12,
        "17_piscina_lava_3d": 0.45,
        "07_navio_naufragado_elemento-cenario": 0.38,
        "06_bau_tesouro_elemento-cenario": 0.55,
        "15_portal_atlantica_3d": 0.55,
        "13_obstaculo_abissal": 0.55,
        "12_obstaculo_vulcanico": 0.70,
    }
    ratio = ratios.get(model_name)
    if ratio is None:
        return
    for obj in scene.objects:
        if obj.type != "MESH" or len(obj.data.polygons) < 80:
            continue
        modifier = obj.modifiers.new("Apsu Runtime LOD", "DECIMATE")
        modifier.ratio = ratio
        bpy.ops.object.select_all(action="DESELECT")
        obj.select_set(True)
        bpy.context.view_layer.objects.active = obj
        bpy.ops.object.modifier_apply(modifier=modifier.name)


for root, _dirs, files in os.walk(blend_root):
    for filename in sorted(files):
        if not filename.endswith(".blend"):
            continue
        name = os.path.splitext(filename)[0]
        if only_models is not None and name not in only_models:
            continue
        is_character = any(key in name.lower() for key in (
            "adapa", "kullullu", "enki", "guardiao", "peixe", "inimigo",
            "arraiao", "leviata",
        ))
        is_scenery = any(key in name.lower() for key in (
            "bau_tesouro", "navio_naufragado", "recifes_e_cardume",
            "ruinas_e_colunas", "obstaculo_abissal", "obstaculo_vulcanico",
            "corrente_abissal", "portal_atlantica", "geiser",
            "piscina_lava",
        ))
        if not is_character and not is_scenery:
            continue

        source = os.path.join(root, filename)
        category = "characters" if is_character else "scenery"
        target_dir = os.path.join(output_root, category, name)
        os.makedirs(target_dir, exist_ok=True)
        target = os.path.join(target_dir, name + ".obj")
        bpy.ops.wm.open_mainfile(filepath=source)
        scene = bpy.context.scene
        scene.frame_set(scene.frame_start)
        if fix_mtls_only:
            collect_existing_texture_paths(target_dir)
            for current_root, _dirs, current_files in os.walk(target_dir):
                for current_file in current_files:
                    if current_file.endswith(".mtl"):
                        ensure_mtl_textures(os.path.join(current_root, current_file))
            print("[3D] MTL texturas revisados: " + name)
            exported += 1
            continue
        if is_scenery:
            simplify_runtime_scenery(name, scene)
        bake_material_textures(target_dir)
        export_obj(target)
        ensure_mtl_textures(os.path.splitext(target)[0] + ".mtl")
        print("[3D] " + name + " -> " + target)
        exported += 1

if exported == 0 and not fix_mtls_only:
    raise RuntimeError("Nenhum modelo de personagem .blend foi encontrado em " + blend_root)
print("[3D] Exportados " + str(exported) + " modelos OBJ/MTL para runtime.")
