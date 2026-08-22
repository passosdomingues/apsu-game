import bpy
import math
import os

# --- Biblioteca compartilhada (Sprint 6: rim light + ciclo de nado real) ---
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
    def swim_cycle_keyframes(*a, **kw): pass
    def smooth_angle_chain(n, a, b, *args, **kw): return [((b-a)*i/max(n-1,1)+a)*math.pi/180 for i in range(n)]

# ============================================================
# 01C - ADAPA HERÓI: GERADOR DE VARIAÇÕES E SKINS (v3.1)
# Paleta 60-30-10
# ============================================================

PALETAS_ADAPA = {
    # 2026-08-16 (pedido do Rafa): "só muda a cor... poderia variar
    # melhor... não só aparência mas altura, gordura". Os 3 tipos já
    # têm identidade de FÍSICA bem diferente em HeroType.java (ABISSAL
    # = tanque pesado/muita inércia, DEUS = velocista, RECIFE = ágil/
    # freio rápido) — escala_corpo/escala_altura fazem a SILHUETA
    # bater com essa identidade em vez de só recolorir o mesmo corpo.
    "01_adapa_var_abissal": {
        "nome_pt": "Adapa Abissal das Profundezas",
        "corpo_60_a": (0.02, 0.08, 0.22, 1.0),
        "corpo_60_b": (0.01, 0.03, 0.12, 1.0),
        "pele": (0.75, 0.70, 0.78, 1.0),
        "barba": (0.05, 0.05, 0.10, 1.0),
        "metal_30": (0.80, 0.85, 0.95, 1.0),     # Platina/Prata reluzente
        "barbatana_30": (0.45, 0.75, 0.95, 0.85),
        "acento_10": (0.1, 0.95, 1.0, 1.0),      # Ciano Neon
        "luz_cor": (0.3, 0.7, 1.0),
        # Tanque pesado — mais baixo e ATARRACADO, torso/cauda mais
        # grossos (mais "gordura"/massa), cauda proporcionalmente mais
        # curta (menos alongamento = menos velocidade, mais força bruta).
        "escala_altura": 0.90, "escala_corpo": 1.24, "escala_cauda_len": 0.90,
    },
    "01_adapa_var_deus_dourado": {
        "nome_pt": "Adapa Ascendido / Deus Solar de Apsu",
        "corpo_60_a": (0.95, 0.75, 0.15, 1.0),
        "corpo_60_b": (0.60, 0.40, 0.05, 1.0),
        "pele": (0.92, 0.78, 0.60, 1.0),
        "barba": (0.25, 0.15, 0.05, 1.0),
        "metal_30": (0.35, 0.08, 0.45, 1.0),     # Púrpura Real imperial
        "barbatana_30": (1.0, 0.85, 0.30, 0.9),
        "acento_10": (0.1, 1.0, 0.5, 1.0),       # Esmeralda Divina
        "luz_cor": (1.0, 0.85, 0.5),
        # Velocista — mais ALTO e ESGUIO, cauda alongada (mais
        # propulsão, silhueta de nadador de alta velocidade).
        "escala_altura": 1.10, "escala_corpo": 0.86, "escala_cauda_len": 1.18,
    },
    "01_adapa_var_recife": {
        "nome_pt": "Adapa Guardião dos Recifes de Coral",
        "corpo_60_a": (0.05, 0.75, 0.40, 1.0),
        "corpo_60_b": (0.02, 0.35, 0.20, 1.0),
        "pele": (0.85, 0.65, 0.48, 1.0),
        "barba": (0.10, 0.08, 0.06, 1.0),
        "metal_30": (0.88, 0.50, 0.15, 1.0),     # Cobre / Âmbar
        "barbatana_30": (0.95, 0.60, 0.20, 0.85),
        "acento_10": (1.0, 0.15, 0.55, 1.0),      # Magenta Rubi
        "luz_cor": (0.4, 0.9, 0.7),
        # Tático ágil — compacto, o mais baixo dos 4 (menor massa pra
        # girar/frear rápido), cauda proporção padrão (agilidade vem
        # de resposta, não de alongamento).
        "escala_altura": 0.93, "escala_corpo": 0.96, "escala_cauda_len": 0.98,
    }
}

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
    scene.render.resolution_x = 1080
    scene.render.resolution_y = 1920
    scene.render.film_transparent = True
    scene.view_settings.view_transform = 'Filmic'
    scene.view_settings.look = 'Medium High Contrast'

def criar_material_escamas(nome, cor_a, cor_b, metallic=0.65, roughness=0.22):
    mat = bpy.data.materials.new(name=nome)
    mat.use_nodes = True
    nodes = mat.node_tree.nodes
    links = mat.node_tree.links
    nodes.clear()

    output = nodes.new('ShaderNodeOutputMaterial')
    bsdf = nodes.new('ShaderNodeBsdfPrincipled')
    tex_coord = nodes.new('ShaderNodeTexCoord')
    voronoi = nodes.new('ShaderNodeTexVoronoi')
    noise = nodes.new('ShaderNodeTexNoise')
    ramp = nodes.new('ShaderNodeValToRGB')
    bump = nodes.new('ShaderNodeBump')

    voronoi.feature = 'F1'
    voronoi.inputs['Scale'].default_value = 14.0
    noise.inputs['Scale'].default_value = 25.0

    ramp.color_ramp.elements[0].position = 0.15
    ramp.color_ramp.elements[0].color = cor_b
    ramp.color_ramp.elements[1].position = 0.85
    ramp.color_ramp.elements[1].color = cor_a

    bump.inputs['Strength'].default_value = 0.4

    if 'Metallic' in bsdf.inputs:
        bsdf.inputs['Metallic'].default_value = metallic
    if 'Roughness' in bsdf.inputs:
        bsdf.inputs['Roughness'].default_value = roughness
    if 'Subsurface Weight' in bsdf.inputs:
        bsdf.inputs['Subsurface Weight'].default_value = 0.30
    if 'Subsurface Radius' in bsdf.inputs:
        bsdf.inputs['Subsurface Radius'].default_value = (0.3, 0.5, 0.4)
    if 'Subsurface Color' in bsdf.inputs:
        bsdf.inputs['Subsurface Color'].default_value = cor_a

    links.new(tex_coord.outputs['Object'], voronoi.inputs['Vector'])
    links.new(tex_coord.outputs['Object'], noise.inputs['Vector'])
    links.new(voronoi.outputs['Distance'], ramp.inputs['Fac'])
    links.new(ramp.outputs['Color'], bsdf.inputs['Base Color'])
    links.new(voronoi.outputs['Distance'], bump.inputs['Height'])
    links.new(bump.outputs['Normal'], bsdf.inputs['Normal'])
    links.new(bsdf.outputs['BSDF'], output.inputs['Surface'])
    return mat

def criar_material_simples(nome, cor_rgba, metallic=0.1, roughness=0.3, emission=False, emission_cor=(1,1,1,1), emission_forca=3.0):
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

def construir_variacao_adapa(var_id, dados):
    setup_cena()

    mat_corpo = criar_material_escamas(f"Mat_{var_id}_Corpo_60", dados["corpo_60_a"], dados["corpo_60_b"])
    mat_pele = criar_material_simples(f"Mat_{var_id}_Pele", dados["pele"], metallic=0.05, roughness=0.45)
    mat_barba = criar_material_simples(f"Mat_{var_id}_Barba", dados["barba"], roughness=0.75)
    mat_metal = criar_material_simples(f"Mat_{var_id}_Metal_30", dados["metal_30"], metallic=0.92, roughness=0.18)
    mat_barbatana = criar_material_simples(f"Mat_{var_id}_Barbatana_30", dados["barbatana_30"], metallic=0.75, roughness=0.2)
    mat_acento = criar_material_simples(f"Mat_{var_id}_Acento_10", dados["acento_10"], emission=True, emission_cor=dados["acento_10"], emission_forca=6.0)

    root = bpy.data.objects.new(f"Adapa_{var_id}_Root", None)
    bpy.context.scene.collection.objects.link(root)
    root.rotation_euler = (math.radians(-10), 0, math.radians(12))
    # Diversidade fisica real (2026-08-16): altura/gordura por variante,
    # nao so cor — ver comentario em PALETAS_ADAPA. escala_corpo cobre
    # X/Y (largura/gordura), escala_altura cobre Z (altura geral).
    _esc_corpo = dados.get("escala_corpo", 1.0)
    _esc_altura = dados.get("escala_altura", 1.0)
    _esc_cauda = dados.get("escala_cauda_len", 1.0)
    root.scale = (_esc_corpo, _esc_corpo, _esc_altura)

    # Torso
    bpy.ops.mesh.primitive_uv_sphere_add(segments=32, ring_count=24, radius=0.85, location=(0, 0, 1.4))
    torso = bpy.context.active_object
    torso.scale = (0.85, 0.65, 1.15)
    torso.parent = root
    torso.data.materials.append(mat_pele)
    smooth_object(torso)

    # Peitoral
    bpy.ops.mesh.primitive_torus_add(major_radius=0.78, minor_radius=0.14, location=(0, -0.05, 1.65))
    peitoral = bpy.context.active_object
    peitoral.rotation_euler = (math.radians(35), math.radians(15), 0)
    peitoral.scale = (0.9, 0.7, 1.0)
    peitoral.parent = root
    peitoral.data.materials.append(mat_metal)
    smooth_object(peitoral)

    # Broche
    bpy.ops.mesh.primitive_uv_sphere_add(radius=0.16, location=(0.15, -0.65, 1.8))
    broche = bpy.context.active_object
    broche.parent = root
    broche.data.materials.append(mat_acento)

    # Cabeça
    bpy.ops.mesh.primitive_uv_sphere_add(segments=28, ring_count=24, radius=0.48, location=(0, -0.15, 2.5))
    cabeca = bpy.context.active_object
    cabeca.parent = root
    cabeca.data.materials.append(mat_pele)
    smooth_object(cabeca)

    # Tiara
    bpy.ops.mesh.primitive_cylinder_add(radius=0.51, depth=0.18, location=(0, -0.15, 2.78))
    tiara = bpy.context.active_object
    tiara.parent = root
    tiara.data.materials.append(mat_metal)
    smooth_object(tiara)

    # Gema da Tiara
    bpy.ops.mesh.primitive_uv_sphere_add(radius=0.1, location=(0, -0.64, 2.82))
    gema = bpy.context.active_object
    gema.parent = root
    gema.data.materials.append(mat_acento)

    # Olhos
    for side in [-1, 1]:
        bpy.ops.mesh.primitive_uv_sphere_add(radius=0.075, location=(side * 0.18, -0.58, 2.55))
        olho = bpy.context.active_object
        olho.parent = root
        olho.data.materials.append(mat_acento)

    # Barba
    for i in range(4):
        z_pos = 2.15 - (i * 0.22)
        raio = 0.32 - (i * 0.05)
        bpy.ops.mesh.primitive_torus_add(major_radius=raio, minor_radius=0.08, location=(0, -0.42 + (i*0.04), z_pos))
        b = bpy.context.active_object
        b.rotation_euler = (math.radians(20), 0, 0)
        b.parent = root
        b.data.materials.append(mat_barba)
        smooth_object(b)

    # Cauda
    # BUGFIX 2026-08-16: rot_x original (-10,-30,-15,20,45) tem o mesmo
    # problema achado no 01_adapa_heroi.py — o salto -30->-15 é um kink,
    # não uma curva. Substituído por smooth_angle_chain (mesmas âncoras
    # inicial/final, meio suavizado).
    cauda_segmentos_base = [
        ((0, 0.05, 0.55), (0.75, 0.65, 0.95), math.radians(-10)),
        ((0, 0.32, -0.25), (0.62, 0.55, 0.95), math.radians(-30)),
        ((0, 0.85, -0.95), (0.48, 0.42, 0.95), math.radians(-15)),
        ((0, 1.35, -1.55), (0.32, 0.28, 0.90), math.radians(20)),
        ((0, 1.70, -2.05), (0.18, 0.16, 0.80), math.radians(45)),
    ]
    # Alonga/encolhe SÓ a cauda (não a cabeça/torso) — multiplica a
    # extensão espacial (y,z) e a profundidade de cada segmento por
    # escala_cauda_len. Velocista fica com propulsão visivelmente
    # maior; tanque fica com cauda mais curta e grossa.
    if abs(_esc_cauda - 1.0) > 1e-6:
        cauda_segmentos_base = [
            ((px, py * _esc_cauda, pz * _esc_cauda), (sx, sy, sz * _esc_cauda), rx)
            for (px, py, pz), (sx, sy, sz), rx in cauda_segmentos_base
        ]
    _rx_smooth = smooth_angle_chain(len(cauda_segmentos_base), -10, 45, wobble_deg=4.0, wobble_cycles=1.2)
    cauda_segmentos = [(pos, scale, _rx_smooth[i]) for i, (pos, scale, _rot_x_old) in enumerate(cauda_segmentos_base)]
    cauda_segs = []
    for pos, scale, rot_x in cauda_segmentos:
        bpy.ops.mesh.primitive_cone_add(vertices=24, radius1=scale[0], radius2=scale[0]*0.75, depth=scale[2], location=pos)
        seg = bpy.context.active_object
        seg.rotation_euler = (rot_x, 0, 0)
        seg.parent = root
        seg.data.materials.append(mat_corpo)
        smooth_object(seg)
        cauda_segs.append(seg)

    # Barbatanas Caudais
    for side in [-1, 1]:
        bpy.ops.mesh.primitive_cone_add(vertices=4, radius1=0.75, depth=1.6,
                                         location=(side * 0.45, 2.1 * _esc_cauda, -2.1 * _esc_cauda))
        caudal_fin = bpy.context.active_object
        caudal_fin.scale = (0.06, 0.85, 1.2)
        caudal_fin.rotation_euler = (math.radians(65), math.radians(side * 35), math.radians(side * 15))
        caudal_fin.parent = root
        caudal_fin.data.materials.append(mat_barbatana)
        smooth_object(caudal_fin)

    # Crista Dorsal
    for i, y_pos in enumerate([0.1, 0.5, 0.9, 1.3, 1.7]):
        tam = 0.35 - (i * 0.05)
        bpy.ops.mesh.primitive_cone_add(vertices=3, radius1=0.22, depth=tam*2.2, location=(0, y_pos, 1.6 - (i*0.4)))
        dorsal = bpy.context.active_object
        dorsal.scale = (0.06, 0.7, 1.0)
        dorsal.rotation_euler = (math.radians(-65), 0, 0)
        dorsal.parent = root
        dorsal.data.materials.append(mat_barbatana)
        smooth_object(dorsal)

    # Tridente
    trident_root = bpy.data.objects.new(f"Tridente_{var_id}", None)
    bpy.context.scene.collection.objects.link(trident_root)
    trident_root.parent = root
    trident_root.location = (0.85, -0.65, 1.3)
    trident_root.rotation_euler = (math.radians(70), math.radians(-10), math.radians(-15))

    bpy.ops.mesh.primitive_cylinder_add(radius=0.045, depth=3.8, location=(0, 0, 0))
    haste = bpy.context.active_object
    haste.parent = trident_root
    haste.data.materials.append(mat_metal)
    smooth_object(haste)

    bpy.ops.mesh.primitive_ico_sphere_add(radius=0.18, subdivisions=3, location=(0, 0, 1.65))
    orbe = bpy.context.active_object
    orbe.parent = trident_root
    orbe.data.materials.append(mat_acento)

    pontas = [(0.0, 0.0, 2.4, 0.9), (-0.35, 0.0, 2.15, 0.75), (0.35, 0.0, 2.15, 0.75)]
    for px, py, pz, comp in pontas:
        bpy.ops.mesh.primitive_cone_add(vertices=8, radius1=0.06, depth=comp, location=(px, py, pz))
        ponta = bpy.context.active_object
        ponta.parent = trident_root
        ponta.data.materials.append(mat_metal)
        smooth_object(ponta)

    # --- CÂMERA COM AUTO-TRACKING ENQUADRADA PERFEITAMENTE ---
    cam_target = bpy.data.objects.new("Camera_Target", None)
    cam_target.location = (0.2, 0.0, 0.7)
    bpy.context.scene.collection.objects.link(cam_target)

    # Camera Portrait Fixa
    bpy.ops.object.camera_add(location=(1.0, -8.5, 1.8), rotation=(math.radians(88), 0, math.radians(7)))
    camera = bpy.context.active_object
    camera.data.lens = 42
    bpy.context.scene.camera = camera

    bpy.ops.object.light_add(type='AREA', location=(2.5, -5.5, 4.5))
    key = bpy.context.active_object
    key.data.energy = 650
    key.data.color = dados["luz_cor"]

    bpy.ops.object.light_add(type='AREA', location=(-3.0, 4.0, 3.0))
    rim = bpy.context.active_object
    rim.data.energy = 850
    rim.data.color = (1.0, 0.9, 0.7)

    # === RIM LIGHT + CICLO DE NADO REAL (Sprint 6) ===
    # Rim = cor de acento (10%) já definida na paleta de cada variação —
    # mesma regra usada nos inimigos/guardião: reforça a identidade que a
    # paleta já tinha, sem inventar cor nova por variante.
    rim_color = tuple(dados["acento_10"][:3])
    for mat in (mat_corpo, mat_pele, mat_metal, mat_barbatana):
        add_fresnel_rim(mat, rim_color, power=1.5, strength=6.0)

    swim_cycle_keyframes(root, cauda_segs, frame_count=8, amp_deg=13.0,
                          wavelength_segs=2.4, cycles=1.0, bob_amp=0.05,
                          root_sway_deg=2.5)

    #def salvar_variacao(nome):
    base_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    blends_dir = os.path.join(base_dir, "blends", "01_adapa_heroi")
    renders_dir = os.path.join(base_dir, "renders")
    os.makedirs(blends_dir, exist_ok=True)
    os.makedirs(renders_dir, exist_ok=True)

    blend_path = os.path.join(blends_dir, f"{var_id}.blend")
    render_path = os.path.join(renders_dir, f"{var_id}.png")

    try:
        bpy.ops.wm.save_as_mainfile(filepath=blend_path)
        print("Blend salvo: " + blend_path)
    except Exception as e:
        print("Aviso ao salvar blend: " + str(e))

    try:
        bpy.context.scene.render.filepath = render_path
        bpy.ops.render.render(write_still=True)
        print("Render salvo: " + render_path)
    except Exception as e:
        print("Aviso ao renderizar: " + str(e))

    # Contrato reutilizável: o gerador de ataques das variantes usa estes
    # objetos reais, evitando duplicar (e desalinhar) a modelagem da skin.
    return {"root": root, "torso": torso, "tr_root": trident_root}

if __name__ == "__main__":
    for var_id, dados in PALETAS_ADAPA.items():
        print(f"--- Gerando {dados['nome_pt']} ({var_id}) ---")
        construir_variacao_adapa(var_id, dados)
    print("OK Variacoes de Adapa geradas com sucesso!")
