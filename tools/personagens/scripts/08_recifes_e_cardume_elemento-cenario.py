import bpy
import math
import random
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
# 08 - RECIFES DE CORAL & CARDUME DINÃ‚MICO (PROPS v3)
# Elemento de Preenchimento & Parallax Submarino (Fases 1 e 2)
# Paleta 60-30-10 (Profundidade & Camada de Fundo):
#   60% Teal-Azulado Profundo (Corais CÃ©rebro, Silhuetas dos Peixes)
#   30% Magenta-Coral / Roxo Acinzentado (Corais em Leque e Galhos)
#   10% Amarelo-Solar & Ciano Bioluminescente (Pontas dos Corais, Olhos do Cardume)
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

def criar_material_simples(nome, cor_rgba, metallic=0.05, roughness=0.55, emission=False, emission_cor=(1,1,1,1), emission_forca=2.0):
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

def construir_recifes_e_cardume():
    setup_cena()
    random.seed(42) # Semente determinÃ­stica para geraÃ§Ã£o procedural impecÃ¡vel

    # --- MATERIAIS 60-30-10 ---
    mat_coral_teal = criar_material_simples("Mat_Coral_Teal_60", (0.05, 0.28, 0.35, 1.0), roughness=0.6)
    mat_coral_magenta = criar_material_simples("Mat_Coral_Magenta_30", (0.55, 0.15, 0.35, 1.0), roughness=0.5)
    mat_coral_laranja = criar_material_simples("Mat_Coral_Laranja_30", (0.75, 0.35, 0.10, 1.0), roughness=0.55)
    mat_ponta_luz = criar_material_simples("Mat_Coral_Ponta_10", (0.95, 0.90, 0.25, 1.0), emission=True, emission_cor=(0.95, 0.90, 0.25, 1.0), emission_forca=6.0)
    
    mat_peixe_corpo = criar_material_simples("Mat_Cardume_Corpo_60", (0.08, 0.38, 0.52, 1.0), metallic=0.6, roughness=0.2)
    mat_peixe_barbatana = criar_material_simples("Mat_Cardume_Barbatana_30", (0.95, 0.75, 0.20, 1.0), metallic=0.7, roughness=0.2)
    mat_peixe_olho = criar_material_simples("Mat_Cardume_Olho_10", (0.2, 1.0, 0.9, 1.0), emission=True, emission_cor=(0.2, 1.0, 0.9, 1.0), emission_forca=7.0)

    root = bpy.data.objects.new("Cenario_Recife_Cardume_Root", None)
    bpy.context.scene.collection.objects.link(root)

    # --- CLUSTER DE CORAIS (Primeiro Plano e Plano MÃ©dio) ---
    root_recife = bpy.data.objects.new("Recife_Cluster_Root", None)
    bpy.context.scene.collection.objects.link(root_recife)
    root_recife.parent = root

    coral_coords = [
        (-2.2, 0.2, "galho", mat_coral_magenta, 1.4),
        (-1.4, -0.4, "leque", mat_coral_teal, 0.9),
        (-0.7, 0.5, "tubo", mat_coral_laranja, 1.1),
        (0.0, -0.2, "cerebro", mat_coral_teal, 0.8),
        (0.8, 0.4, "galho", mat_coral_laranja, 1.5),
        (1.6, -0.3, "leque", mat_coral_magenta, 1.0),
        (2.3, 0.3, "tubo", mat_coral_teal, 1.3),
        (-1.8, 0.9, "cerebro", mat_coral_laranja, 0.7),
        (1.2, 1.1, "galho", mat_coral_magenta, 1.2),
        (-0.2, 1.3, "tubo", mat_coral_teal, 1.0),
    ]

    for cx, cy, ctipo, cmat, caltura in coral_coords:
        if ctipo == "galho":
            bpy.ops.mesh.primitive_cone_add(vertices=8, radius1=0.18, radius2=0.04, depth=caltura, location=(cx, cy, caltura/2))
            c = bpy.context.active_object
            c.rotation_euler = (math.radians(random.uniform(-12, 12)), math.radians(random.uniform(-12, 12)), random.uniform(0, 3.14))
            c.parent = root_recife
            c.data.materials.append(cmat)
            smooth_object(c)

            # Pontas bioluminescentes
            bpy.ops.mesh.primitive_uv_sphere_add(radius=0.08, location=(cx, cy, caltura))
            p = bpy.context.active_object
            p.parent = root_recife
            p.data.materials.append(mat_ponta_luz)

        elif ctipo == "leque":
            bpy.ops.mesh.primitive_torus_add(major_radius=0.55 * caltura, minor_radius=0.07, location=(cx, cy, 0.5 * caltura))
            c = bpy.context.active_object
            c.rotation_euler = (math.radians(90), 0, random.uniform(0, 3.14))
            c.scale = (1.0, 1.0, 0.6)
            c.parent = root_recife
            c.data.materials.append(cmat)
            smooth_object(c)

        elif ctipo == "tubo": # Esponjas Tubulares
            for ti in range(3):
                ox = cx + (ti - 1) * 0.16
                oy = cy + random.uniform(-0.1, 0.1)
                t_alt = caltura * (0.8 + ti*0.2)
                bpy.ops.mesh.primitive_cylinder_add(radius=0.09, depth=t_alt, vertices=12, location=(ox, oy, t_alt/2))
                c = bpy.context.active_object
                c.parent = root_recife
                c.data.materials.append(cmat)
                smooth_object(c)

                # Anel de luz na borda do tubo
                bpy.ops.mesh.primitive_torus_add(major_radius=0.09, minor_radius=0.02, location=(ox, oy, t_alt))
                an = bpy.context.active_object
                an.parent = root_recife
                an.data.materials.append(mat_ponta_luz)

        else: # Coral CÃ©rebro
            bpy.ops.mesh.primitive_ico_sphere_add(subdivisions=3, radius=0.45 * caltura, location=(cx, cy, 0.35 * caltura))
            c = bpy.context.active_object
            c.scale = (1.2, 1.0, 0.75)
            c.parent = root_recife
            c.data.materials.append(cmat)
            smooth_object(c)

    # --- CARDUME DINÃ‚MICO EM FORMAÃ‡ÃƒO DE VÃ“RTICE (Fundo / Parallax) ---
    root_cardume = bpy.data.objects.new("Cardume_Vortice_Root", None)
    bpy.context.scene.collection.objects.link(root_cardume)
    root_cardume.parent = root

    CARDUME_QTD = 18
    for i in range(CARDUME_QTD):
        t = i / CARDUME_QTD
        ang = t * (2 * math.pi * 1.5) # Espiral
        raio_curva = 2.2 + math.sin(t * 4) * 0.6
        px = math.cos(ang) * raio_curva
        py = 2.0 + math.sin(ang) * 1.2  # Ao fundo atrÃ¡s dos corais
        pz = 1.2 + (t * 1.8)           # Subindo em espiral
        escala_p = 0.18

        # Peixe individual com corpo, cauda e olho emissivo
        peixe_obj = bpy.data.objects.new(f"Peixe_{i}", None)
        bpy.context.scene.collection.objects.link(peixe_obj)
        peixe_obj.parent = root_cardume
        peixe_obj.location = (px, py, pz)
        peixe_obj.rotation_euler = (0, 0, ang + math.pi/2)

        # 2026-08-16 (pedido do Rafa: "dá mais vida... elementos do
        # cenário"): variação de ângulo por índice do peixe, não
        # animação — este asset só é usado como imagem estática no
        # jogo hoje (RenderEngine carrega scenery via getImage, não
        # loadSequence), então a "vida" precisa estar congelada na
        # pose, não em frames que ninguém vai ler. Cada peixe fica
        # num ponto diferente do próprio ciclo de nado — é isso que
        # faz o cardume ler como vivo, não sincronizado feito boneco.
        _wag = math.radians(20) * math.sin(i * 1.35)

        # Corpo do peixinho — leve inclinação correlacionada à cauda
        # (corpo "no meio de uma virada" lê como natação real, corpo
        # sempre reto lê como enfileirado)
        bpy.ops.mesh.primitive_uv_sphere_add(segments=12, ring_count=8, radius=escala_p, location=(0, 0, 0))
        p_corpo = bpy.context.active_object
        p_corpo.scale = (0.5, 1.4, 0.7)
        p_corpo.rotation_euler = (0, _wag * 0.35, 0)
        p_corpo.parent = peixe_obj
        p_corpo.data.materials.append(mat_peixe_corpo)
        smooth_object(p_corpo)

        # Cauda
        bpy.ops.mesh.primitive_cone_add(vertices=3, radius1=escala_p*0.7, depth=escala_p*1.2, location=(0, -escala_p*1.2, 0))
        p_cauda = bpy.context.active_object
        p_cauda.rotation_euler = (math.radians(-90), _wag, 0)
        p_cauda.scale = (0.05, 1.0, 0.8)
        p_cauda.parent = peixe_obj
        p_cauda.data.materials.append(mat_peixe_barbatana)
        smooth_object(p_cauda)



        # Olho de Luz
        bpy.ops.mesh.primitive_uv_sphere_add(radius=escala_p*0.2, location=(escala_p*0.35, escala_p*0.7, 0))
        p_olho = bpy.context.active_object
        p_olho.parent = peixe_obj
        p_olho.data.materials.append(mat_peixe_olho)

    # Camera widescreen - capturando todo o recife e a espiral do cardume subindo
    bpy.ops.object.camera_add(location=(0.0, -8.5, 2.0))
    camera = bpy.context.active_object
    camera.rotation_euler = (math.radians(84), 0, 0)
    camera.data.lens = 36
    bpy.context.scene.camera = camera

    # Key Light (Azul/Ciano Profundo)
    bpy.ops.object.light_add(type='AREA', location=(0.0, -3.5, 5.0))
    key = bpy.context.active_object
    key.data.energy = 600
    key.data.color = (0.4, 0.85, 1.0)

    # Fill Light (Rosa/Magenta Coral)
    bpy.ops.object.light_add(type='POINT', location=(-2.5, -1.0, 1.5))
    fill = bpy.context.active_object
    fill.data.energy = 400
    fill.data.color = (0.9, 0.4, 0.7)

    # Backlight do Cardume (Ciano Luminoso)
    bpy.ops.object.light_add(type='AREA', location=(0.0, 4.5, 3.5))
    back = bpy.context.active_object
    back.data.energy = 700
    back.data.color = (0.2, 0.8, 1.0)

    # Rim (2026-08-16) — cada material de coral usa sua própria ponta
    # bioluminescente como cor de contorno; peixes usam o próprio olho.
    add_fresnel_rim(mat_coral_teal, (0.95, 0.90, 0.25), power=1.6, strength=5.0)
    add_fresnel_rim(mat_coral_magenta, (0.95, 0.90, 0.25), power=1.6, strength=5.0)
    add_fresnel_rim(mat_coral_laranja, (0.95, 0.90, 0.25), power=1.6, strength=5.0)
    add_fresnel_rim(mat_peixe_corpo, (0.2, 1.0, 0.9), power=1.8, strength=5.5)

    return root

def salvar_e_renderizar(nome_arquivo="08_recifes_e_cardume_elemento-cenario"):
    # Mantém blends e renders na árvore do projeto, em qualquer SO.
    base_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    blends_dir = os.path.join(base_dir, "blends", "08_recifes_e_cardume_elemento-cenario")
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
    construir_recifes_e_cardume()
    salvar_e_renderizar("08_recifes_e_cardume_elemento-cenario")
    print("âœ… Recifes e Cardume v3 gerados com sucesso!")

