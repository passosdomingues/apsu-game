import bpy
import math
import os

# --- Biblioteca compartilhada (2026-08-16: rim light) ---
try:
    _SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
except NameError:
    _SCRIPT_DIR = os.getcwd()
_LIB_PATH = os.path.join(_SCRIPT_DIR, "_apsu_shared_lib.py")
if os.path.exists(_LIB_PATH):
    exec(compile(open(_LIB_PATH, encoding="utf-8").read(), _LIB_PATH, 'exec'))
else:
    print("[APSU] AVISO: _apsu_shared_lib.py nao encontrado em " + _LIB_PATH)
    def add_fresnel_rim(mat, *a, **kw): return mat

# ============================================================
# 07 - NAVIO NAUFRAGADO DO ABISMO (PROP DE CENÃRIO v3)
# Elemento de Parallax / AmbientaÃ§Ã£o (Fases 1, 2 e 3)
# Paleta 60-30-10 (Dessaturada para nÃ£o roubar foco dos personagens):
#   60% Madeira Apodrecida Azul-Acinzentada (Casco, ConvÃ©s Partido)
#   30% Metal & Corrente Enferrujada Marrom-Avermelhada (Costelas, Ã‚ncora, CanhÃµes)
#   10% Algas & AnÃªmonas Bioluminescentes (Vida Marinha Crescendo no NaufrÃ¡gio)
# ============================================================

def setup_cena():
    bpy.ops.object.select_all(action='SELECT')
    bpy.ops.object.delete(use_global=False)
    for mesh in bpy.data.meshes:
        bpy.data.meshes.remove(mesh, do_unlink=True)
    for mat in bpy.data.materials:
        bpy.data.materials.remove(mat, do_unlink=True)

    scene = bpy.context.scene
    scene.render.engine = 'BLENDER_EEVEE_NEXT' if hasattr(bpy.types, 'RenderSettings') and 'BLENDER_EEVEE_NEXT' in bpy.types.RenderSettings.bl_rna.properties['engine'].enum_items.keys() else 'BLENDER_EEVEE'
    if hasattr(scene, 'eevee') and hasattr(scene.eevee, 'taa_render_samples'):
        scene.eevee.taa_render_samples = 16
    scene.render.resolution_x = 1920
    scene.render.resolution_y = 1080
    scene.render.film_transparent = True
    scene.view_settings.view_transform = 'Filmic'
    scene.view_settings.look = 'Medium High Contrast'

def criar_material_simples(nome, cor_rgba, metallic=0.1, roughness=0.7, emission=False, emission_cor=(1,1,1,1), emission_forca=2.0):
    mat = bpy.data.materials.new(name=nome)
    mat.use_nodes = True
    bsdf = mat.node_tree.nodes.get("Principled BSDF")
    if bsdf:
        if 'Base Color' in bsdf.inputs:
            bsdf.inputs['Base Color'].default_value = cor_rgba
        if 'Metallic' in bsdf.inputs:
            bsdf.inputs['Metallic'].default_value = metallic
        if 'Roughness' in bsdf.inputs:
            bsdf.inputs['Roughness'].default_value = roughness
        if emission:
            if 'Emission Color' in bsdf.inputs:
                bsdf.inputs['Emission Color'].default_value = emission_cor
            if 'Emission Strength' in bsdf.inputs:
                bsdf.inputs['Emission Strength'].default_value = emission_forca
    return mat

def smooth_object(obj):
    if obj.type == 'MESH':
        for p in obj.data.polygons:
            p.use_smooth = True

def construir_navio_naufragado():
    setup_cena()

    # --- MATERIAIS 60-30-10 ---
    mat_casco = criar_material_simples("Mat_Navio_Casco_60", (0.12, 0.16, 0.20, 1.0), roughness=0.85)
    mat_ferrugem = criar_material_simples("Mat_Navio_Ferrugem_30", (0.35, 0.14, 0.08, 1.0), metallic=0.4, roughness=0.6)
    mat_alga_luz = criar_material_simples("Mat_Navio_Alga_10", (0.2, 0.95, 0.6, 1.0), emission=True, emission_cor=(0.2, 0.95, 0.6, 1.0), emission_forca=5.0)

    root = bpy.data.objects.new("Navio_Root", None)
    bpy.context.scene.collection.objects.link(root)
    root.rotation_euler = (math.radians(10), math.radians(-15), math.radians(-25)) # Navio tombado no leito

    # --- CASCO PRINCIPAL DO GALEÃƒO (60% Madeira) ---
    bpy.ops.mesh.primitive_cone_add(vertices=12, radius1=1.6, radius2=0.4, depth=6.5, location=(0, 0, 0.8))
    casco = bpy.context.active_object
    casco.rotation_euler = (0, math.radians(90), 0)
    casco.scale = (0.6, 1.0, 0.6)
    casco.parent = root
    casco.data.materials.append(mat_casco)
    smooth_object(casco)

    # ConvÃ©s Superior Partido
    bpy.ops.mesh.primitive_cube_add(size=1.0, location=(-0.6, 0.35, 1.45))
    deck = bpy.context.active_object
    deck.scale = (3.2, 1.2, 0.1)
    deck.rotation_euler = (0, 0, math.radians(12))
    deck.parent = root
    deck.data.materials.append(mat_casco)
    smooth_object(deck)

    # Castelo de Popa Elevado
    bpy.ops.mesh.primitive_cube_add(size=1.0, location=(-2.2, 0.2, 1.9))
    popa = bpy.context.active_object
    popa.scale = (1.4, 1.1, 1.0)
    popa.rotation_euler = (0, math.radians(10), 0)
    popa.parent = root
    popa.data.materials.append(mat_casco)
    smooth_object(popa)

    # --- MASTRO PRINCIPAL QUEBRADO ---
    bpy.ops.mesh.primitive_cylinder_add(radius=0.12, depth=3.8, location=(0.4, 0.3, 2.7))
    mastro = bpy.context.active_object
    mastro.rotation_euler = (math.radians(45), math.radians(15), 0)
    mastro.parent = root
    mastro.data.materials.append(mat_casco)
    smooth_object(mastro)

    # Verga / Trave Quebrada
    bpy.ops.mesh.primitive_cylinder_add(radius=0.07, depth=2.4, location=(0.2, 0.8, 3.4))
    verga = bpy.context.active_object
    verga.rotation_euler = (math.radians(10), math.radians(75), math.radians(20))
    verga.parent = root
    verga.data.materials.append(mat_casco)
    smooth_object(verga)

    # --- COSTELAS ESTRUTURAIS EXPOSTAS (30% Metal Enferrujado) ---
    for i in range(6):
        bpy.ops.mesh.primitive_torus_add(major_radius=1.1 - i*0.06, minor_radius=0.05, location=(-2.4 + i*0.6, 0.3, 0.9))
        costela = bpy.context.active_object
        costela.rotation_euler = (0, math.radians(90), 0)
        costela.scale = (1.0, 1.0, 0.65)
        costela.parent = root
        costela.data.materials.append(mat_ferrugem)
        smooth_object(costela)

    # --- GRANDE Ã‚NCORA DE FERRO ENFERRUJADO ---
    ancora_root = bpy.data.objects.new("Ancora_Root", None)
    bpy.context.scene.collection.objects.link(ancora_root)
    ancora_root.parent = root
    ancora_root.location = (2.2, 0.2, 0.3)
    # 2026-08-16 (pedido do Rafa: "dá mais vida... elementos do cenário"):
    # âncora levemente inclinada, como se estivesse presa numa corrente/
    # corda e balançando com a água — em vez de perfeitamente vertical
    # (mesmo raciocínio do baú/cardume: pose congelada com vida, não
    # animação, já que cenário só carrega imagem estática no jogo).
    ancora_root.rotation_euler = (0, 0, math.radians(9))

    bpy.ops.mesh.primitive_cylinder_add(radius=0.06, depth=1.8, location=(0, 0, 0.6))
    haste_ancora = bpy.context.active_object
    haste_ancora.parent = ancora_root
    haste_ancora.data.materials.append(mat_ferrugem)
    smooth_object(haste_ancora)

    bpy.ops.mesh.primitive_torus_add(major_radius=0.35, minor_radius=0.05, location=(0, 0, 1.5))
    anel_ancora = bpy.context.active_object
    anel_ancora.rotation_euler = (math.radians(90), 0, 0)
    anel_ancora.parent = ancora_root
    anel_ancora.data.materials.append(mat_ferrugem)
    smooth_object(anel_ancora)

    bpy.ops.mesh.primitive_torus_add(major_radius=0.6, minor_radius=0.06, location=(0, 0, 0.1))
    gancho_ancora = bpy.context.active_object
    gancho_ancora.parent = ancora_root
    gancho_ancora.data.materials.append(mat_ferrugem)
    smooth_object(gancho_ancora)

    # --- ALGAS E CORAIS BIOLUMINESCENTES (10% Acento de Vida) ---
    algas_dados = [
        (-1.2, -0.6, 1.1, 0.22),
        (0.6, -0.7, 1.2, 0.18),
        (-0.2, 0.8, 0.6, 0.20),
        (1.4, -0.3, 0.7, 0.16),
        (-2.4, -0.4, 1.8, 0.25),
        (0.4, 0.6, 2.2, 0.14),
    ]
    for ax, ay, az, arad in algas_dados:
        bpy.ops.mesh.primitive_ico_sphere_add(radius=arad, subdivisions=2, location=(ax, ay, az))
        alga = bpy.context.active_object
        alga.scale = (1.0, 1.0, 0.5)
        alga.parent = root
        alga.data.materials.append(mat_alga_luz)
        smooth_object(alga)

    # Camera widescreen - afastada para capturar o galeao inteiro de 7 metros e mastro
    bpy.ops.object.camera_add(location=(0.0, -12.0, 2.0))
    camera = bpy.context.active_object
    camera.rotation_euler = (math.radians(85), 0, 0)
    camera.data.lens = 32
    bpy.context.scene.camera = camera

    bpy.ops.object.light_add(type='AREA', location=(0.0, -4.5, 5.0))
    key = bpy.context.active_object
    key.data.energy = 550
    key.data.color = (0.4, 0.7, 1.0)

    bpy.ops.object.light_add(type='AREA', location=(-3.5, 4.0, 3.0))
    rim = bpy.context.active_object
    rim.data.energy = 700
    rim.data.color = (0.8, 0.5, 0.3)

    # Rim light — mesma cor da alga bioluminescente já usada como acento
    for mat in (mat_casco, mat_ferrugem):
        add_fresnel_rim(mat, (0.2, 0.95, 0.6), power=1.6, strength=4.5)

    return root

def salvar_e_renderizar(nome_arquivo="07_navio_naufragado_elemento-cenario"):
    # Nunca use um caminho absoluto de outra máquina: no Linux a string
    # Windows vira uma pasta literal dentro de scripts/. A raiz é sempre a
    # pasta personagens que contém este script.
    base_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    blends_dir = os.path.join(base_dir, "blends", "07_navio_naufragado_elemento-cenario")
    renders_dir = os.path.join(base_dir, "renders")
    os.makedirs(blends_dir, exist_ok=True)
    os.makedirs(renders_dir, exist_ok=True)

    blend_path = os.path.join(blends_dir, f"{nome_arquivo}.blend")
    render_path = os.path.join(renders_dir, f"{nome_arquivo}.png")

    try:
        bpy.ops.wm.save_as_mainfile(filepath=blend_path)
        print(f"ðŸ’¾ Blend salvo em: {blend_path}")
    except Exception as e:
        print(f"Aviso ao salvar blend: {e}")

    try:
        bpy.context.scene.render.filepath = render_path
        bpy.ops.render.render(write_still=True)
        print(f"ðŸ“¸ Render salvo em: {render_path}")
    except Exception as e:
        print(f"Aviso ao renderizar: {e}")

if __name__ == "__main__":
    construir_navio_naufragado()
    salvar_e_renderizar("07_navio_naufragado_elemento-cenario")
    print("âœ… Navio Naufragado v3 gerado com sucesso!")

