# -*- coding: utf-8 -*-
"""
==============================================================================
FASE 3D COMPLETA: CIDADE PERDIDA DE ATLANTIS ("AS ÁGUAS DE APSU")
Script Procedural bpy para Blender 4.5+

Gera um cenário 3D completo de fase de jogo submersa integrando:
- Herói Adapa (com animação de nado contínuo, cauda/barbatanas ondulantes, disparo de bolhas)
- Boss Kullullû (com animação de respiração abissal e fendas vulcânicas ativas)
- Sábio Enki (no Santuário com cajado místico e orbe d'água giratório)
- Inimigos (Peixes Sombrios em patrulha, Medusas elétricas oscilando, Cardumes de peixes)
- Guardião Atlante (Estátua colossal com runas douradas e olhos acesos)
- Ruínas de Atlantis (Colunatas monumentais, arcos quebrados, obeliscos de cristal)
- Elementos de Cenário (Navio naufragado, Baús de tesouro abertos com joias e luz)
- Perigos e Obstáculos (Gêiseres hidrotermais com emissão de bolhas, minas de espinhos)
- Terreno Oceânico com relevo orgânico e florestas de corais/algas
- Iluminação Subaquática com Cáusticas / God Rays e Névoa Volumétrica
- Câmeras Cinemáticas e de Gameplay 2.5D com Parallax

Salva o arquivo .blend final e renderiza imagens de demonstração.
==============================================================================
"""

import bpy
import math
import random
import os

# Configuração de caminhos
BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
BLEND_OUTPUT_DIR = os.path.join(BASE_DIR, "blends", "fase_atlantis_apsu")
RENDERS_DIR = os.path.join(BASE_DIR, "renders", "fase_atlantis_apsu")
BLEND_FILE = os.path.join(BLEND_OUTPUT_DIR, "fase_atlantis_apsu_3d.blend")

os.makedirs(BLEND_OUTPUT_DIR, exist_ok=True)
os.makedirs(RENDERS_DIR, exist_ok=True)

# ============================================================================
# 1. SETUP DE CENA, MUNDO E RENDER
# ============================================================================
def setup_cena():
    bpy.ops.object.select_all(action='SELECT')
    bpy.ops.object.delete(use_global=False)
    for m in list(bpy.data.meshes): bpy.data.meshes.remove(m, do_unlink=True)
    for mat in list(bpy.data.materials): bpy.data.materials.remove(mat, do_unlink=True)
    for col in list(bpy.data.collections):
        if col.name != "Scene Collection":
            bpy.data.collections.remove(col)

    scene = bpy.context.scene
    try:
        scene.render.engine = 'BLENDER_EEVEE_NEXT'
    except:
        scene.render.engine = 'BLENDER_EEVEE'
        
    if hasattr(scene, 'eevee'):
        if hasattr(scene.eevee, 'taa_render_samples'): scene.eevee.taa_render_samples = 32
        if hasattr(scene.eevee, 'use_gtao'): scene.eevee.use_gtao = True
        if hasattr(scene.eevee, 'use_bloom'): scene.eevee.use_bloom = True
        if hasattr(scene.eevee, 'use_volumetric'): scene.eevee.use_volumetric = True

    scene.render.resolution_x = 1920
    scene.render.resolution_y = 1080
    scene.render.film_transparent = False
    scene.view_settings.view_transform = 'Filmic'
    scene.view_settings.look = 'High Contrast'
    
    # Timeline
    scene.frame_start = 1
    scene.frame_end = 240
    scene.render.fps = 30
    scene.frame_set(1)

    # Shading do Mundo (Oceano Profundo de Atlantis)
    world = bpy.context.scene.world
    if not world:
        world = bpy.data.worlds.new("World_Atlantis_Oceano")
        bpy.context.scene.world = world
    world.use_nodes = True
    wnodes = world.node_tree.nodes
    wlinks = world.node_tree.links
    wnodes.clear()

    w_out = wnodes.new('ShaderNodeOutputWorld')
    w_bg = wnodes.new('ShaderNodeBackground')
    w_grad = wnodes.new('ShaderNodeTexGradient')
    w_ramp = wnodes.new('ShaderNodeValToRGB')
    w_coord = wnodes.new('ShaderNodeTexCoord')
    w_map = wnodes.new('ShaderNodeMapping')

    w_map.inputs['Rotation'].default_value[0] = math.radians(90)
    w_ramp.color_ramp.elements[0].position = 0.0
    w_ramp.color_ramp.elements[0].color = (0.005, 0.02, 0.05, 1.0) # Abismo escuro inferior
    w_ramp.color_ramp.elements[1].position = 1.0
    w_ramp.color_ramp.elements[1].color = (0.02, 0.15, 0.28, 1.0) # Luz do mar superior

    w_bg.inputs['Strength'].default_value = 0.8
    wlinks.new(w_coord.outputs['Generated'], w_map.inputs['Vector'])
    wlinks.new(w_map.outputs['Vector'], w_grad.inputs['Vector'])
    wlinks.new(w_grad.outputs['Fac'], w_ramp.inputs['Fac'])
    wlinks.new(w_ramp.outputs['Color'], w_bg.inputs['Color'])
    wlinks.new(w_bg.outputs['Background'], w_out.inputs['Surface'])

def get_or_create_collection(nome, parent=None):
    if nome in bpy.data.collections:
        col = bpy.data.collections[nome]
    else:
        col = bpy.data.collections.new(nome)
        if parent is None:
            bpy.context.scene.collection.children.link(col)
        else:
            parent.children.link(col)
    return col

def smooth(obj):
    if obj and obj.type == 'MESH':
        for p in obj.data.polygons:
            p.use_smooth = True

# ============================================================================
# 2. BIBLIOTECA DE SHADERS PROCEDURAIS (60-30-10 & ESTILO ATLANTIS)
# ============================================================================
MATS = {}

def init_materiais():
    global MATS

    def mat_principled(nome, cor, metallic=0.1, roughness=0.4, em=False, em_cor=None, em_str=1.0, alpha=1.0):
        mat = bpy.data.materials.new(name=nome)
        mat.use_nodes = True
        bsdf = mat.node_tree.nodes.get("Principled BSDF")
        if bsdf:
            if 'Base Color' in bsdf.inputs: bsdf.inputs['Base Color'].default_value = cor
            if 'Metallic' in bsdf.inputs: bsdf.inputs['Metallic'].default_value = metallic
            if 'Roughness' in bsdf.inputs: bsdf.inputs['Roughness'].default_value = roughness
            if alpha < 1.0 and 'Alpha' in bsdf.inputs:
                bsdf.inputs['Alpha'].default_value = alpha
                mat.blend_method = 'BLEND'
            if em and em_cor:
                if 'Emission Color' in bsdf.inputs: bsdf.inputs['Emission Color'].default_value = em_cor
                if 'Emission Strength' in bsdf.inputs: bsdf.inputs['Emission Strength'].default_value = em_str
        return mat

    def mat_escamas_proc(nome, cor_a, cor_b, metallic=0.7, roughness=0.2):
        mat = bpy.data.materials.new(name=nome)
        mat.use_nodes = True
        nodes = mat.node_tree.nodes
        links = mat.node_tree.links
        nodes.clear()
        out = nodes.new('ShaderNodeOutputMaterial')
        bsdf = nodes.new('ShaderNodeBsdfPrincipled')
        coord = nodes.new('ShaderNodeTexCoord')
        vor = nodes.new('ShaderNodeTexVoronoi')
        ramp = nodes.new('ShaderNodeValToRGB')
        bump = nodes.new('ShaderNodeBump')
        vor.inputs['Scale'].default_value = 16.0
        ramp.color_ramp.elements[0].position = 0.2
        ramp.color_ramp.elements[0].color = cor_b
        ramp.color_ramp.elements[1].position = 0.8
        ramp.color_ramp.elements[1].color = cor_a
        bump.inputs['Strength'].default_value = 0.4
        if 'Metallic' in bsdf.inputs: bsdf.inputs['Metallic'].default_value = metallic
        if 'Roughness' in bsdf.inputs: bsdf.inputs['Roughness'].default_value = roughness
        links.new(coord.outputs['Object'], vor.inputs['Vector'])
        links.new(vor.outputs['Distance'], ramp.inputs['Fac'])
        links.new(ramp.outputs['Color'], bsdf.inputs['Base Color'])
        links.new(vor.outputs['Distance'], bump.inputs['Height'])
        links.new(bump.outputs['Normal'], bsdf.inputs['Normal'])
        links.new(bsdf.outputs['BSDF'], out.inputs['Surface'])
        return mat

    def mat_obsidiana_proc(nome, cor_base, cor_crack, escala=3.0, em_forca=3.0):
        mat = bpy.data.materials.new(name=nome)
        mat.use_nodes = True
        nodes = mat.node_tree.nodes
        links = mat.node_tree.links
        nodes.clear()
        out = nodes.new('ShaderNodeOutputMaterial')
        bsdf = nodes.new('ShaderNodeBsdfPrincipled')
        coord = nodes.new('ShaderNodeTexCoord')
        vor = nodes.new('ShaderNodeTexVoronoi')
        ramp_d = nodes.new('ShaderNodeValToRGB')
        ramp_e = nodes.new('ShaderNodeValToRGB')
        bump = nodes.new('ShaderNodeBump')
        vor.inputs['Scale'].default_value = escala
        vor.feature = 'DISTANCE_TO_EDGE'
        ramp_d.color_ramp.elements[0].position = 0.05
        ramp_d.color_ramp.elements[0].color = cor_crack
        ramp_d.color_ramp.elements[1].position = 0.16
        ramp_d.color_ramp.elements[1].color = cor_base
        ramp_e.color_ramp.elements[0].position = 0.03
        ramp_e.color_ramp.elements[0].color = (1.0, 0.25, 0.0, 1.0)
        ramp_e.color_ramp.elements[1].position = 0.08
        ramp_e.color_ramp.elements[1].color = (0,0,0,1)
        bump.inputs['Strength'].default_value = 0.6
        if 'Metallic' in bsdf.inputs: bsdf.inputs['Metallic'].default_value = 0.45
        if 'Roughness' in bsdf.inputs: bsdf.inputs['Roughness'].default_value = 0.28
        if 'Emission Strength' in bsdf.inputs: bsdf.inputs['Emission Strength'].default_value = em_forca
        links.new(coord.outputs['Object'], vor.inputs['Vector'])
        links.new(vor.outputs['Distance'], ramp_d.inputs['Fac'])
        links.new(ramp_d.outputs['Color'], bsdf.inputs['Base Color'])
        links.new(vor.outputs['Distance'], ramp_e.inputs['Fac'])
        if 'Emission Color' in bsdf.inputs: links.new(ramp_e.outputs['Color'], bsdf.inputs['Emission Color'])
        links.new(vor.outputs['Distance'], bump.inputs['Height'])
        links.new(bump.outputs['Normal'], bsdf.inputs['Normal'])
        links.new(bsdf.outputs['BSDF'], out.inputs['Surface'])
        return mat

    MATS['adapa_escama']  = mat_escamas_proc("M_Adapa_Escama", (0.04, 0.92, 0.75, 1), (0.01, 0.42, 0.52, 1))
    MATS['adapa_pele']    = mat_principled("M_Adapa_Pele", (0.86, 0.68, 0.52, 1), roughness=0.45)
    MATS['adapa_ouro']    = mat_principled("M_Adapa_Ouro", (0.98, 0.80, 0.20, 1), metallic=0.95, roughness=0.15)
    MATS['adapa_fin']     = mat_principled("M_Adapa_Fin", (0.95, 0.85, 0.30, 1), metallic=0.75, roughness=0.2)
    MATS['adapa_barba']   = mat_principled("M_Adapa_Barba", (0.07, 0.05, 0.04, 1), roughness=0.75)
    MATS['adapa_olho']    = mat_principled("M_Adapa_Olho", (1.0, 0.50, 0.10, 1), em=True, em_cor=(1.0, 0.5, 0.1, 1), em_str=6.0)
    MATS['adapa_bolha']   = mat_principled("M_Adapa_Bolha", (0.3, 0.95, 1.0, 0.85), roughness=0.02, em=True, em_cor=(0.2, 0.9, 1.0, 1), em_str=5.0, alpha=0.85)

    MATS['boss_corpo']    = mat_obsidiana_proc("M_Boss_Obsidiana", (0.04, 0.01, 0.07, 1), (1.0, 0.25, 0.02, 1), escala=2.8, em_forca=4.0)
    MATS['boss_espinho']  = mat_principled("M_Boss_Espinho", (0.42, 0.03, 0.05, 1), metallic=0.4, roughness=0.3)
    MATS['boss_magma']    = mat_principled("M_Boss_Magma", (1.0, 0.22, 0.0, 1), em=True, em_cor=(1.0, 0.25, 0.0, 1), em_str=8.0)

    MATS['enki_tunica']   = mat_escamas_proc("M_Enki_Tunica", (0.08, 0.25, 0.70, 1), (0.02, 0.08, 0.30, 1), metallic=0.8, roughness=0.2)
    MATS['enki_barba']    = mat_principled("M_Enki_Barba", (0.92, 0.95, 0.98, 1), roughness=0.3)
    MATS['enki_orbe']     = mat_principled("M_Enki_Orbe", (0.2, 0.9, 1.0, 0.9), em=True, em_cor=(0.1, 0.95, 1.0, 1), em_str=10.0, alpha=0.85)

    MATS['ruina_marmore'] = mat_principled("M_Ruina_Marmore", (0.55, 0.65, 0.72, 1), roughness=0.7)
    MATS['ruina_ouro']    = mat_principled("M_Ruina_Ouro", (0.85, 0.68, 0.22, 1), metallic=0.8, roughness=0.3)
    MATS['cristal_luz']   = mat_principled("M_Cristal_Luz", (0.1, 0.98, 1.0, 0.9), em=True, em_cor=(0.1, 0.98, 1.0, 1), em_str=9.0, alpha=0.9)

    MATS['chao_areia']    = mat_principled("M_Chao_Areia", (0.15, 0.25, 0.30, 1), roughness=0.85)
    MATS['rocha_abissal'] = mat_principled("M_Rocha_Abissal", (0.08, 0.12, 0.16, 1), roughness=0.8)
    MATS['navio_madeira'] = mat_principled("M_Navio_Madeira", (0.12, 0.16, 0.18, 1), roughness=0.85)
    MATS['navio_metal']   = mat_principled("M_Navio_Metal", (0.35, 0.15, 0.08, 1), metallic=0.6, roughness=0.5)

    MATS['coral_rosa']    = mat_principled("M_Coral_Rosa", (0.95, 0.25, 0.45, 1), roughness=0.5, em=True, em_cor=(0.95, 0.25, 0.45, 1), em_str=1.5)
    MATS['coral_amarelo'] = mat_principled("M_Coral_Amarelo", (0.98, 0.80, 0.10, 1), roughness=0.5, em=True, em_cor=(0.98, 0.80, 0.10, 1), em_str=1.8)
    MATS['coral_verde']   = mat_principled("M_Coral_Verde", (0.10, 0.92, 0.50, 1), roughness=0.4, em=True, em_cor=(0.10, 0.92, 0.50, 1), em_str=2.0)
    MATS['alga_kelp']     = mat_principled("M_Alga_Kelp", (0.03, 0.35, 0.22, 1), roughness=0.6)

    MATS['inimigo_corpo'] = mat_principled("M_Inimigo_Corpo", (0.05, 0.06, 0.12, 1), metallic=0.5, roughness=0.3)
    MATS['inimigo_olho']  = mat_principled("M_Inimigo_Olho", (1.0, 0.1, 0.1, 1), em=True, em_cor=(1.0, 0.1, 0.1, 1), em_str=7.0)
    MATS['medusa_corpo']  = mat_principled("M_Medusa_Corpo", (0.3, 0.9, 1.0, 0.75), em=True, em_cor=(0.2, 0.85, 1.0, 1), em_str=4.0, alpha=0.75)
    MATS['perigo_mina']   = mat_principled("M_Perigo_Mina", (0.18, 0.18, 0.20, 1), metallic=0.8, roughness=0.4)
    MATS['perigo_alerta'] = mat_principled("M_Perigo_Alerta", (1.0, 0.05, 0.05, 1), em=True, em_cor=(1.0, 0.05, 0.05, 1), em_str=8.0)

# ============================================================================
# 3. CONSTRUTOR DO TERRENO SUBMARINO DE ATLANTIS
# ============================================================================
def construir_terreno_oceano(col_cenario):
    # Base do fundo do mar (64x30 unidades com ondulações)
    bpy.ops.mesh.primitive_grid_add(x_subdivisions=64, y_subdivisions=32, size=70, location=(0, 4, -1.0))
    chao = bpy.context.active_object
    chao.name = "Terreno_Oceano_Atlantis"
    chao.data.materials.append(MATS['chao_areia'])
    smooth(chao)

    # Deformação com displace procedural
    mod_disp = chao.modifiers.new(name="Ondulacao_Areia", type='DISPLACE')
    tex_noise = bpy.data.textures.new("Tex_Noise_Chao", type='CLOUDS')
    tex_noise.noise_scale = 1.8
    tex_noise.noise_depth = 3
    mod_disp.texture = tex_noise
    mod_disp.strength = 1.4
    
    # Subsurf leve para suavizar
    mod_sub = chao.modifiers.new(name="Subsurf_Chao", type='SUBSURF')
    mod_sub.levels = 1
    mod_sub.render_levels = 1

    # Paredes de rochas ao fundo (Background Cliffs)
    for i, x_pos in enumerate([-28, -14, 0, 14, 28]):
        bpy.ops.mesh.primitive_cube_add(size=1.0, location=(x_pos, 18, 4))
        falésia = bpy.context.active_object
        falésia.name = f"Falesia_Fundo_{i+1}"
        falésia.scale = (16, 6, 12)
        falésia.rotation_euler = (math.radians(-10), math.radians(random.uniform(-5, 5)), 0)
        falésia.data.materials.append(MATS['rocha_abissal'])
        smooth(falésia)
        for c in col_cenario.objects: pass
        if falésia.name not in col_cenario.objects: col_cenario.objects.link(falésia)
        bpy.context.scene.collection.objects.unlink(falésia)

    if chao.name not in col_cenario.objects: col_cenario.objects.link(chao)
    bpy.context.scene.collection.objects.unlink(chao)

# ============================================================================
# 4. CONSTRUTORES DE RUÍNAS, TEMPLOS E PROPS DE ATLANTIS
# ============================================================================
def criar_coluna_atlantis(loc, altura=4.5, quebrada=False, inclinacao=0.0, parent=None, col_target=None):
    root_col = bpy.data.objects.new(f"Coluna_Atlantis_{loc[0]:.1f}", None)
    root_col.location = loc
    col_target.objects.link(root_col)
    if parent: root_col.parent = parent

    # Base
    bpy.ops.mesh.primitive_cylinder_add(radius=0.7, depth=0.4, vertices=16, location=(0, 0, 0.2))
    base = bpy.context.active_object
    base.data.materials.append(MATS['ruina_marmore'])
    base.parent = root_col
    smooth(base)

    # Fuste canelado
    h_fuste = altura * (0.6 if quebrada else 1.0)
    bpy.ops.mesh.primitive_cylinder_add(radius=0.48, depth=h_fuste, vertices=16, location=(0, 0, 0.4 + h_fuste/2))
    fuste = bpy.context.active_object
    fuste.rotation_euler = (math.radians(inclinacao), 0, 0)
    fuste.data.materials.append(MATS['ruina_marmore'])
    fuste.parent = root_col
    smooth(fuste)

    # Capitel Dourado (se não quebrada no topo)
    if not quebrada:
        bpy.ops.mesh.primitive_cube_add(size=1.0, location=(0, 0, 0.4 + h_fuste + 0.2))
        cap = bpy.context.active_object
        cap.scale = (1.2, 1.2, 0.35)
        cap.data.materials.append(MATS['ruina_ouro'])
        cap.parent = root_col
        smooth(cap)
    else:
        # Pedaço partido no chão
        bpy.ops.mesh.primitive_cylinder_add(radius=0.45, depth=1.2, vertices=12, location=(0.6, 0.5, 0.3))
        pedaço = bpy.context.active_object
        pedaço.rotation_euler = (math.radians(80), math.radians(25), 0)
        pedaço.data.materials.append(MATS['ruina_marmore'])
        pedaço.parent = root_col
        smooth(pedaço)

    return root_col

def construir_ruinas_colunatas(col_ruinas):
    # Grande Via Sacra de Atlantis (Zona 2: X = -12 a +2)
    colunas_coords = [
        # Linha de trás (Y = 4.5)
        (-12, 4.5, 5.0, False, 0),
        (-8,  4.5, 4.8, False, 0),
        (-4,  4.5, 2.8, True,  -8),
        (0,   4.5, 5.2, False, 0),
        (4,   4.5, 3.2, True,  12),
        # Linha da frente (Y = 1.0)
        (-10, 1.0, 4.2, False, 0),
        (-6,  1.0, 2.5, True,  10),
        (-2,  1.0, 4.6, False, 0),
        (2,   1.0, 4.4, False, 0),
    ]

    for x, y, h, q, inc in colunas_coords:
        criar_coluna_atlantis((x, y, 0), altura=h, quebrada=q, inclinacao=inc, col_target=col_ruinas)

    # Grande Arco Triunfal em Ruínas no centro (X = -5, Y = 3)
    bpy.ops.mesh.primitive_torus_add(major_radius=3.2, minor_radius=0.45, location=(-5, 3, 4.5))
    arco = bpy.context.active_object
    arco.name = "Arco_Atlantis_Monumental"
    arco.rotation_euler = (math.radians(90), 0, 0)
    arco.scale = (1.0, 0.8, 1.2)
    arco.data.materials.append(MATS['ruina_marmore'])
    smooth(arco)
    col_ruinas.objects.link(arco)
    bpy.context.scene.collection.objects.unlink(arco)

    # Obeliscos com Cristais de Energia Atlante Pulsantes
    for ox, oy in [(-9, 2.5), (1, 2.5)]:
        # Pedestal
        bpy.ops.mesh.primitive_cube_add(size=1.0, location=(ox, oy, 1.0))
        ped = bpy.context.active_object
        ped.scale = (1.2, 1.2, 1.8)
        ped.data.materials.append(MATS['ruina_ouro'])
        smooth(ped)
        col_ruinas.objects.link(ped)
        bpy.context.scene.collection.objects.unlink(ped)

        # Cristal Flutuante
        bpy.ops.mesh.primitive_cone_add(vertices=6, radius1=0.4, radius2=0.0, depth=1.6, location=(ox, oy, 2.8))
        cristal_top = bpy.context.active_object
        cristal_top.data.materials.append(MATS['cristal_luz'])
        smooth(cristal_top)

        bpy.ops.mesh.primitive_cone_add(vertices=6, radius1=0.4, radius2=0.0, depth=1.6, location=(ox, oy, 2.8))
        cristal_bot = bpy.context.active_object
        cristal_bot.rotation_euler = (math.radians(180), 0, 0)
        cristal_bot.data.materials.append(MATS['cristal_luz'])
        smooth(cristal_bot)

        # Luz pontual do cristal
        light_data = bpy.data.lights.new(name=f"Luz_Cristal_{ox}", type='POINT')
        light_data.color = (0.1, 0.95, 1.0)
        light_data.energy = 450.0
        light_data.shadow_soft_size = 0.2
        light_obj = bpy.data.objects.new(name=f"Luz_Cristal_Obj_{ox}", object_data=light_data)
        light_obj.location = (ox, oy, 2.8)
        col_ruinas.objects.link(light_obj)

        col_ruinas.objects.link(cristal_top)
        col_ruinas.objects.link(cristal_bot)
        bpy.context.scene.collection.objects.unlink(cristal_top)
        bpy.context.scene.collection.objects.unlink(cristal_bot)

def construir_navio_naufragado(col_props, loc=(-18, 5, 0)):
    # Galeão Naufragado na Zona 1 (Recifes)
    root_navio = bpy.data.objects.new("Navio_Naufragado_Root", None)
    root_navio.location = loc
    root_navio.rotation_euler = (math.radians(12), math.radians(-18), math.radians(-20))
    col_props.objects.link(root_navio)

    # Casco
    bpy.ops.mesh.primitive_cone_add(vertices=12, radius1=1.8, radius2=0.5, depth=7.0, location=(0, 0, 1.2))
    casco = bpy.context.active_object
    casco.rotation_euler = (0, math.radians(90), 0)
    casco.scale = (0.7, 1.1, 0.7)
    casco.data.materials.append(MATS['navio_madeira'])
    casco.parent = root_navio
    smooth(casco)

    # Mastro partido
    bpy.ops.mesh.primitive_cylinder_add(radius=0.18, depth=4.5, vertices=12, location=(-0.5, 0, 3.0))
    mastro = bpy.context.active_object
    mastro.rotation_euler = (math.radians(25), 0, math.radians(-15))
    mastro.data.materials.append(MATS['navio_madeira'])
    mastro.parent = root_navio
    smooth(mastro)

    # Âncora e Corrente enferrujada
    bpy.ops.mesh.primitive_torus_add(major_radius=0.45, minor_radius=0.1, location=(3.2, -0.6, 0.4))
    ancora = bpy.context.active_object
    ancora.data.materials.append(MATS['navio_metal'])
    ancora.parent = root_navio
    smooth(ancora)

def construir_bau_tesouro(col_props, loc=(-15, 0.5, 0.2), rot_z=20):
    root_bau = bpy.data.objects.new(f"Bau_Tesouro_{loc[0]:.0f}", None)
    root_bau.location = loc
    root_bau.rotation_euler = (0, 0, math.radians(rot_z))
    col_props.objects.link(root_bau)

    # Base do Baú
    bpy.ops.mesh.primitive_cube_add(size=1.0, location=(0, 0, 0.35))
    base = bpy.context.active_object
    base.scale = (0.9, 0.6, 0.5)
    base.data.materials.append(MATS['navio_madeira'])
    base.parent = root_bau
    smooth(base)

    # Tampa Aberta
    bpy.ops.mesh.primitive_cylinder_add(radius=0.3, depth=0.9, vertices=16, location=(0, -0.25, 0.75))
    tampa = bpy.context.active_object
    tampa.rotation_euler = (math.radians(-50), 0, math.radians(90))
    tampa.data.materials.append(MATS['navio_madeira'])
    tampa.parent = root_bau
    smooth(tampa)

    # Tesouro e Ouro reluzente no interior
    bpy.ops.mesh.primitive_cube_add(size=1.0, location=(0, 0, 0.45))
    ouro = bpy.context.active_object
    ouro.scale = (0.75, 0.45, 0.2)
    ouro.data.materials.append(MATS['adapa_ouro'])
    ouro.parent = root_bau
    smooth(ouro)

    # Luz de feixe mágico saindo do baú
    light_data = bpy.data.lights.new(name=f"Luz_Bau_{loc[0]:.0f}", type='POINT')
    light_data.color = (1.0, 0.85, 0.2)
    light_data.energy = 220.0
    light_data.shadow_soft_size = 0.15
    light_obj = bpy.data.objects.new(name=f"Luz_Bau_Obj_{loc[0]:.0f}", object_data=light_data)
    light_obj.location = (0, 0, 0.7)
    light_obj.parent = root_bau
    col_props.objects.link(light_obj)

# ============================================================================
# 5. CONSTRUTORES DE FLORA, CORAIS E PERIGOS ABISSAIS
# ============================================================================
def construir_recifes_e_flora(col_flora):
    random.seed(42)
    # Clusters de corais e kelp ao longo do cenário
    for i in range(24):
        cx = random.uniform(-28, 28)
        cy = random.uniform(-2, 8)
        cz = 0.0

        tipo = random.choice(['coral_chifre', 'coral_cerebro', 'kelp', 'anemona'])
        if tipo == 'coral_chifre':
            bpy.ops.mesh.primitive_cone_add(vertices=8, radius1=0.35, radius2=0.08, depth=1.8, location=(cx, cy, cz + 0.9))
            c = bpy.context.active_object
            c.scale = (random.uniform(0.6, 1.2), random.uniform(0.6, 1.2), random.uniform(0.8, 1.5))
            c.rotation_euler = (math.radians(random.uniform(-15, 15)), math.radians(random.uniform(-15, 15)), 0)
            c.data.materials.append(random.choice([MATS['coral_rosa'], MATS['coral_amarelo']]))
            smooth(c)
            col_flora.objects.link(c)
            bpy.context.scene.collection.objects.unlink(c)
        elif tipo == 'coral_cerebro':
            bpy.ops.mesh.primitive_uv_sphere_add(radius=random.uniform(0.4, 0.9), location=(cx, cy, cz + 0.4))
            c = bpy.context.active_object
            c.scale = (1.2, 1.1, 0.7)
            c.data.materials.append(MATS['coral_verde'])
            smooth(c)
            col_flora.objects.link(c)
            bpy.context.scene.collection.objects.unlink(c)
        elif tipo == 'kelp':
            # Tronco longo de alga
            bpy.ops.mesh.primitive_cylinder_add(radius=0.08, depth=4.0, vertices=8, location=(cx, cy, cz + 2.0))
            k = bpy.context.active_object
            k.rotation_euler = (math.radians(random.uniform(-10, 10)), math.radians(random.uniform(-10, 10)), 0)
            k.data.materials.append(MATS['alga_kelp'])
            smooth(k)
            col_flora.objects.link(k)
            bpy.context.scene.collection.objects.unlink(k)

def construir_perigos_abissais(col_perigos):
    # Zona 3: Desfiladeiro de Perigos (X = 6 a 16)
    # 1. Gêiseres Hidrotermais (Black Smokers) emitindo bolhas
    geiser_locs = [(8, 1.5), (12, 3.5), (15, 1.0)]
    for gx, gy in geiser_locs:
        bpy.ops.mesh.primitive_cone_add(vertices=12, radius1=0.9, radius2=0.35, depth=2.0, location=(gx, gy, 1.0))
        cone = bpy.context.active_object
        cone.data.materials.append(MATS['rocha_abissal'])
        smooth(cone)
        col_perigos.objects.link(cone)
        bpy.context.scene.collection.objects.unlink(cone)

        # Lava no topo do respiradouro
        bpy.ops.mesh.primitive_uv_sphere_add(radius=0.36, location=(gx, gy, 1.95))
        lava = bpy.context.active_object
        lava.data.materials.append(MATS['boss_magma'])
        smooth(lava)
        col_perigos.objects.link(lava)
        bpy.context.scene.collection.objects.unlink(lava)

        # Coluna de bolhas animada subindo
        for b_idx in range(6):
            bpy.ops.mesh.primitive_uv_sphere_add(radius=random.uniform(0.08, 0.18), location=(gx + random.uniform(-0.1, 0.1), gy + random.uniform(-0.1, 0.1), 2.2 + b_idx * 0.7))
            bolha = bpy.context.active_object
            bolha.data.materials.append(MATS['adapa_bolha'])
            smooth(bolha)
            
            # Animação de subida cíclica
            bolha.keyframe_insert(data_path="location", frame=1)
            bolha.location.z += 2.5
            bolha.keyframe_insert(data_path="location", frame=120)
            bolha.location.z -= 2.5
            bolha.keyframe_insert(data_path="location", frame=240)

            col_perigos.objects.link(bolha)
            bpy.context.scene.collection.objects.unlink(bolha)

    # 2. Minas de Espinhos Atlantes Flutuantes
    minas_coords = [(7, 0, 3.2), (10, -0.5, 2.5), (13, 0.5, 4.0)]
    for mx, my, mz in minas_coords:
        root_mina = bpy.data.objects.new(f"Mina_Espinhos_{mx}", None)
        root_mina.location = (mx, my, mz)
        col_perigos.objects.link(root_mina)

        # Núcleo da mina
        bpy.ops.mesh.primitive_uv_sphere_add(radius=0.55, location=(0, 0, 0))
        esfera = bpy.context.active_object
        esfera.data.materials.append(MATS['perigo_mina'])
        esfera.parent = root_mina
        smooth(esfera)

        # Espinhos cardeais
        for rot in [(0,0,0), (90,0,0), (0,90,0), (45,45,0), (-45,45,0)]:
            bpy.ops.mesh.primitive_cone_add(vertices=8, radius1=0.15, radius2=0.0, depth=1.1, location=(0, 0, 0))
            esp = bpy.context.active_object
            esp.rotation_euler = (math.radians(rot[0]), math.radians(rot[1]), math.radians(rot[2]))
            esp.data.materials.append(MATS['perigo_mina'])
            esp.parent = root_mina
            smooth(esp)

        # Luz de perigo pulsante
        bpy.ops.mesh.primitive_uv_sphere_add(radius=0.16, location=(0, -0.45, 0))
        alerta = bpy.context.active_object
        alerta.data.materials.append(MATS['perigo_alerta'])
        alerta.parent = root_mina
        smooth(alerta)

        # Animação de flutuação suave da mina
        root_mina.keyframe_insert(data_path="location", frame=1)
        root_mina.location.z += 0.4
        root_mina.keyframe_insert(data_path="location", frame=120)
        root_mina.location.z -= 0.4
        root_mina.keyframe_insert(data_path="location", frame=240)

# ============================================================================
# 6. CONSTRUTORES DE TODOS OS PERSONAGENS E NPCS
# ============================================================================

# --- HERÓI ADAPA COM ANIMAÇÃO COMPLETA DE NADO E DISPARO ---
def construir_adapa_heroi(col_heroi):
    root_adapa = bpy.data.objects.new("Heroi_Adapa_Root", None)
    root_adapa.location = (-22, 0, 3.0)
    col_heroi.objects.link(root_adapa)

    # 1. Cabeça com Barba Mesopotâmica e Olhos Emissivos
    bpy.ops.mesh.primitive_uv_sphere_add(segments=20, ring_count=16, radius=0.45, location=(0.75, 0, 0.15))
    cabeca = bpy.context.active_object
    cabeca.data.materials.append(MATS['adapa_pele'])
    cabeca.parent = root_adapa
    smooth(cabeca)

    # Barba e Cabelo
    bpy.ops.mesh.primitive_cone_add(vertices=12, radius1=0.35, radius2=0.08, depth=0.8, location=(0.95, 0, -0.2))
    barba = bpy.context.active_object
    barba.rotation_euler = (0, math.radians(65), 0)
    barba.data.materials.append(MATS['adapa_barba'])
    barba.parent = root_adapa
    smooth(barba)

    # Olhos Emissivos
    for sy in [-0.14, 0.14]:
        bpy.ops.mesh.primitive_uv_sphere_add(radius=0.08, location=(1.12, sy, 0.22))
        olho = bpy.context.active_object
        olho.data.materials.append(MATS['adapa_olho'])
        olho.parent = root_adapa
        smooth(olho)

    # Tiara Real de Ouro
    bpy.ops.mesh.primitive_torus_add(major_radius=0.46, minor_radius=0.06, location=(0.75, 0, 0.35))
    tiara = bpy.context.active_object
    tiara.rotation_euler = (0, math.radians(15), 0)
    tiara.data.materials.append(MATS['adapa_ouro'])
    tiara.parent = root_adapa
    smooth(tiara)

    # 2. Torso Hidrodinâmico com Peitoral Dourado
    bpy.ops.mesh.primitive_uv_sphere_add(segments=24, ring_count=18, radius=0.6, location=(0.1, 0, 0))
    torso = bpy.context.active_object
    torso.scale = (1.4, 0.9, 0.8)
    torso.data.materials.append(MATS['adapa_pele'])
    torso.parent = root_adapa
    smooth(torso)

    # Armadura / Peitoral Dourado
    bpy.ops.mesh.primitive_uv_sphere_add(radius=0.58, location=(0.2, 0, 0.05))
    armadura = bpy.context.active_object
    armadura.scale = (1.1, 0.85, 0.75)
    armadura.data.materials.append(MATS['adapa_ouro'])
    armadura.parent = root_adapa
    smooth(armadura)

    # 3. Braços e Tridente Reluzente
    bpy.ops.mesh.primitive_cylinder_add(radius=0.12, depth=1.2, vertices=12, location=(0.4, -0.6, -0.1))
    braco_r = bpy.context.active_object
    braco_r.rotation_euler = (math.radians(25), math.radians(65), 0)
    braco_r.data.materials.append(MATS['adapa_pele'])
    braco_r.parent = root_adapa
    smooth(braco_r)

    # Tridente de Atlantis
    bpy.ops.mesh.primitive_cylinder_add(radius=0.05, depth=2.6, vertices=8, location=(0.8, -0.6, -0.1))
    trid_cabo = bpy.context.active_object
    trid_cabo.rotation_euler = (0, math.radians(80), 0)
    trid_cabo.data.materials.append(MATS['adapa_ouro'])
    trid_cabo.parent = root_adapa
    smooth(trid_cabo)

    # Pontas do Tridente
    for tz in [-0.22, 0.0, 0.22]:
        bpy.ops.mesh.primitive_cone_add(vertices=8, radius1=0.08, radius2=0.0, depth=0.6, location=(2.0, -0.6, -0.1 + tz))
        ponta = bpy.context.active_object
        ponta.rotation_euler = (0, math.radians(90), 0)
        ponta.data.materials.append(MATS['adapa_ouro'])
        ponta.parent = root_adapa
        smooth(ponta)

    # 4. Cauda de Peixe Apkallu Segmentada em Escamas (Animação de Ondulação)
    cauda_segs = []
    seg_offsets = [-0.6, -1.3, -1.9, -2.4]
    seg_radii   = [0.48, 0.38, 0.26, 0.16]
    for idx, (so, sr) in enumerate(zip(seg_offsets, seg_radii)):
        bpy.ops.mesh.primitive_uv_sphere_add(radius=sr, location=(so, 0, 0))
        seg = bpy.context.active_object
        seg.name = f"Adapa_Cauda_Seg_{idx+1}"
        seg.scale = (1.3, 0.85, 0.85)
        seg.data.materials.append(MATS['adapa_escama'])
        seg.parent = root_adapa
        smooth(seg)
        cauda_segs.append(seg)

    # Nadadeira Caudal Bifurcada em Ouro/Barbatana
    bpy.ops.mesh.primitive_cone_add(vertices=8, radius1=0.55, radius2=0.05, depth=1.1, location=(-3.0, 0, 0))
    fin = bpy.context.active_object
    fin.name = "Adapa_Nadadeira_Caudal"
    fin.rotation_euler = (0, math.radians(-90), 0)
    fin.scale = (0.3, 1.4, 0.6)
    fin.data.materials.append(MATS['adapa_fin'])
    fin.parent = root_adapa
    smooth(fin)

    # 5. Disparo de Bolhas / Vórtice Mágico Aquático
    for bi, bx in enumerate([2.6, 3.5, 4.6]):
        bpy.ops.mesh.primitive_uv_sphere_add(radius=0.25 - bi*0.04, location=(bx, -0.5, 0))
        bolha_atk = bpy.context.active_object
        bolha_atk.data.materials.append(MATS['adapa_bolha'])
        bolha_atk.parent = root_adapa
        smooth(bolha_atk)

    # --- ANIMAÇÕES DO HERÓI (NADO, ONDULAÇÃO DA CAUDA E TRAVESSIA DA FASE) ---
    # 1. Travessia ao longo do cenário (X = -22 até X = +22) nos 240 frames
    for f, x_prog, z_wave in [(1, -22.0, 3.0), (60, -11.0, 3.6), (120, 0.0, 2.8), (180, 11.0, 3.8), (240, 22.0, 3.2)]:
        root_adapa.location = (x_prog, 0, z_wave)
        root_adapa.rotation_euler = (0, math.radians((z_wave - 3.0) * 8), 0)
        root_adapa.keyframe_insert(data_path="location", frame=f)
        root_adapa.keyframe_insert(data_path="rotation_euler", frame=f)

    # 2. Ondulação senoidal na cauda do herói
    for frame in range(1, 241, 15):
        t = frame / 10.0
        for s_idx, seg in enumerate(cauda_segs):
            angle = math.sin(t + s_idx * 0.9) * 0.25 * (s_idx + 1) * 0.5
            seg.rotation_euler = (0, 0, angle)
            seg.keyframe_insert(data_path="rotation_euler", frame=frame)
        
        fin.rotation_euler = (0, math.radians(-90), math.sin(t + 2.5) * 0.45)
        fin.keyframe_insert(data_path="rotation_euler", frame=frame)

    return root_adapa

# --- BOSS KULLULLÛ (O APKALLU CORROMPIDO DO ABISMO) ---
def construir_kullullu_boss(col_boss, loc=(23, 1.5, 2.5)):
    root_boss = bpy.data.objects.new("Boss_Kullullu_Root", None)
    root_boss.location = loc
    root_boss.rotation_euler = (0, 0, math.radians(-155)) # Encarando o herói que chega pela esquerda
    col_boss.objects.link(root_boss)

    # Torso Colossal em Obsidiana Rachada
    bpy.ops.mesh.primitive_uv_sphere_add(segments=28, ring_count=20, radius=1.35, location=(0, 0, 1.8))
    torso = bpy.context.active_object
    torso.scale = (1.1, 0.9, 1.3)
    torso.data.materials.append(MATS['boss_corpo'])
    torso.parent = root_boss
    smooth(torso)

    # Cabeça Demoníaca com Chifres de Obsidiana
    bpy.ops.mesh.primitive_uv_sphere_add(radius=0.75, location=(0, -0.4, 3.2))
    cabeca = bpy.context.active_object
    cabeca.scale = (0.95, 1.1, 1.0)
    cabeca.data.materials.append(MATS['boss_corpo'])
    cabeca.parent = root_boss
    smooth(cabeca)

    # Chifres Curvos
    for sx in [-1, 1]:
        bpy.ops.mesh.primitive_cone_add(vertices=12, radius1=0.22, radius2=0.04, depth=1.6, location=(sx * 0.65, -0.2, 4.0))
        chifre = bpy.context.active_object
        chifre.rotation_euler = (math.radians(-30), math.radians(sx * 35), 0)
        chifre.data.materials.append(MATS['boss_espinho'])
        chifre.parent = root_boss
        smooth(chifre)

    # Olhos e Fendas Vulcânicas de Magma
    for sx in [-0.25, 0.25]:
        bpy.ops.mesh.primitive_uv_sphere_add(radius=0.14, location=(sx, -1.05, 3.25))
        olho = bpy.context.active_object
        olho.data.materials.append(MATS['boss_magma'])
        olho.parent = root_boss
        smooth(olho)

    # Braços Massivos com Garras
    for sx in [-1, 1]:
        bpy.ops.mesh.primitive_cylinder_add(radius=0.35, depth=2.2, vertices=12, location=(sx * 1.5, -0.4, 1.8))
        braco = bpy.context.active_object
        braco.rotation_euler = (math.radians(35), math.radians(sx * 25), 0)
        braco.data.materials.append(MATS['boss_corpo'])
        braco.parent = root_boss
        smooth(braco)

    # Cauda Leviatã Abissal
    bpy.ops.mesh.primitive_cylinder_add(radius=0.7, depth=4.5, vertices=16, location=(0, 1.2, 0.2))
    cauda = bpy.context.active_object
    cauda.rotation_euler = (math.radians(-65), 0, 0)
    cauda.data.materials.append(MATS['boss_corpo'])
    cauda.parent = root_boss
    smooth(cauda)

    # Luz de Magma Intensa sob o Boss
    light_data = bpy.data.lights.new(name="Luz_Magma_Boss", type='POINT')
    light_data.color = (1.0, 0.22, 0.0)
    light_data.energy = 850.0
    light_data.shadow_soft_size = 0.5
    light_obj = bpy.data.objects.new(name="Luz_Magma_Boss_Obj", object_data=light_data)
    light_obj.location = (0, -0.5, 1.5)
    light_obj.parent = root_boss
    col_boss.objects.link(light_obj)

    # Animação de Idle Ameaçador e Respiração
    for frame in [1, 60, 120, 180, 240]:
        t = frame / 30.0
        root_boss.location.z = loc[2] + math.sin(t) * 0.25
        root_boss.keyframe_insert(data_path="location", frame=frame)

    return root_boss

# --- SÁBIO ENKI (NPC ANCESTRAL NO SANTUÁRIO) ---
def construir_enki_npc(col_npc, loc=(-16, 2.0, 1.5)):
    root_enki = bpy.data.objects.new("NPC_Enki_Root", None)
    root_enki.location = loc
    root_enki.rotation_euler = (0, 0, math.radians(-30))
    col_npc.objects.link(root_enki)

    # Altar de Pedra do Santuário
    bpy.ops.mesh.primitive_cylinder_add(radius=1.5, depth=0.8, vertices=16, location=(0, 0, -0.8))
    altar = bpy.context.active_object
    altar.data.materials.append(MATS['ruina_marmore'])
    altar.parent = root_enki
    smooth(altar)

    # Manto / Túnica Celestial de Enki
    bpy.ops.mesh.primitive_cone_add(vertices=16, radius1=0.8, radius2=0.35, depth=2.0, location=(0, 0, 0.4))
    manto = bpy.context.active_object
    manto.data.materials.append(MATS['enki_tunica'])
    manto.parent = root_enki
    smooth(manto)

    # Cabeça e Longa Barba Branca Ancestral
    bpy.ops.mesh.primitive_uv_sphere_add(radius=0.38, location=(0, -0.1, 1.6))
    cabeca = bpy.context.active_object
    cabeca.data.materials.append(MATS['adapa_pele'])
    cabeca.parent = root_enki
    smooth(cabeca)

    bpy.ops.mesh.primitive_cone_add(vertices=12, radius1=0.32, radius2=0.05, depth=1.3, location=(0, -0.3, 1.0))
    barba = bpy.context.active_object
    barba.data.materials.append(MATS['enki_barba'])
    barba.parent = root_enki
    smooth(barba)

    # Tiara de Chifres Sagrados
    bpy.ops.mesh.primitive_torus_add(major_radius=0.4, minor_radius=0.06, location=(0, -0.1, 1.85))
    tiara = bpy.context.active_object
    tiara.data.materials.append(MATS['adapa_ouro'])
    tiara.parent = root_enki
    smooth(tiara)

    # Cajado de Enki com Orbe Místico d'Água Giratório
    bpy.ops.mesh.primitive_cylinder_add(radius=0.06, depth=2.8, vertices=12, location=(0.75, -0.4, 0.8))
    cajado = bpy.context.active_object
    cajado.data.materials.append(MATS['adapa_ouro'])
    cajado.parent = root_enki
    smooth(cajado)

    bpy.ops.mesh.primitive_uv_sphere_add(radius=0.32, location=(0.75, -0.4, 2.35))
    orbe = bpy.context.active_object
    orbe.name = "Enki_Orbe_Mistico"
    orbe.data.materials.append(MATS['enki_orbe'])
    orbe.parent = root_enki
    smooth(orbe)

    # Luz do Orbe
    light_data = bpy.data.lights.new(name="Luz_Enki_Orbe", type='POINT')
    light_data.color = (0.2, 0.9, 1.0)
    light_data.energy = 380.0
    light_data.shadow_soft_size = 0.2
    light_obj = bpy.data.objects.new(name="Luz_Enki_Orbe_Obj", object_data=light_data)
    light_obj.location = (0.75, -0.4, 2.35)
    light_obj.parent = root_enki
    col_npc.objects.link(light_obj)

    # Animação do Orbe Flutuando e Girando
    for f in [1, 60, 120, 180, 240]:
        orbe.rotation_euler = (0, 0, math.radians(f * 3.0))
        orbe.keyframe_insert(data_path="rotation_euler", frame=f)

    return root_enki

# --- GUARDIÃO ATLANTE (ESTÁTUA COLOSSAL) ---
def construir_guardiao_atlante(col_npc, loc=(-3, 6.0, 0.0), rot_z=-20):
    root_guard = bpy.data.objects.new("Guardiao_Atlante_Root", None)
    root_guard.location = loc
    root_guard.rotation_euler = (0, 0, math.radians(rot_z))
    col_npc.objects.link(root_guard)

    # Pedestal
    bpy.ops.mesh.primitive_cube_add(size=1.0, location=(0, 0, 0.8))
    ped = bpy.context.active_object
    ped.scale = (2.2, 2.2, 1.6)
    ped.data.materials.append(MATS['ruina_marmore'])
    ped.parent = root_guard
    smooth(ped)

    # Corpo Colossal da Estátua
    bpy.ops.mesh.primitive_cube_add(size=1.0, location=(0, 0, 3.2))
    corpo = bpy.context.active_object
    corpo.scale = (1.6, 1.2, 3.4)
    corpo.data.materials.append(MATS['ruina_marmore'])
    corpo.parent = root_guard
    smooth(corpo)

    # Cabeça Esculpida com Barba Lamassu e Olhos Iluminados
    bpy.ops.mesh.primitive_uv_sphere_add(radius=0.9, location=(0, -0.3, 5.2))
    cabeca = bpy.context.active_object
    cabeca.scale = (0.9, 1.1, 1.1)
    cabeca.data.materials.append(MATS['ruina_marmore'])
    cabeca.parent = root_guard
    smooth(cabeca)

    for sx in [-0.35, 0.35]:
        bpy.ops.mesh.primitive_uv_sphere_add(radius=0.15, location=(sx, -1.15, 5.3))
        olho = bpy.context.active_object
        olho.data.materials.append(MATS['cristal_luz'])
        olho.parent = root_guard
        smooth(olho)

    # Frisos Dourados
    bpy.ops.mesh.primitive_torus_add(major_radius=0.95, minor_radius=0.1, location=(0, -0.3, 5.9))
    coroa = bpy.context.active_object
    coroa.data.materials.append(MATS['ruina_ouro'])
    coroa.parent = root_guard
    smooth(coroa)

    return root_guard

# --- INIMIGOS (PEIXES SOMBRIOS & MEDUSAS) ---
def construir_inimigos_fauna(col_inimigos):
    # 1. Peixes Sombrios em Patrulha (Zona 2 e Zona 3)
    peixes_locs = [(-6, 0.5, 2.2), (1, -0.5, 3.5), (9, 0, 2.0), (14, 0.5, 3.2)]
    for idx, (px, py, pz) in enumerate(peixes_locs):
        root_p = bpy.data.objects.new(f"Peixe_Sombrio_{idx+1}", None)
        root_p.location = (px, py, pz)
        col_inimigos.objects.link(root_p)

        # Corpo
        bpy.ops.mesh.primitive_uv_sphere_add(radius=0.5, location=(0, 0, 0))
        corpo = bpy.context.active_object
        corpo.scale = (1.5, 0.5, 0.7)
        corpo.data.materials.append(MATS['inimigo_corpo'])
        corpo.parent = root_p
        smooth(corpo)

        # Olho Vermelho Agressivo
        bpy.ops.mesh.primitive_uv_sphere_add(radius=0.1, location=(-0.5, 0.22, 0.1))
        olho = bpy.context.active_object
        olho.data.materials.append(MATS['inimigo_olho'])
        olho.parent = root_p
        smooth(olho)

        # Animação de Patrulha
        for frame in [1, 60, 120, 180, 240]:
            t = frame / 30.0
            root_p.location.x = px + math.sin(t + idx) * 1.5
            root_p.location.z = pz + math.cos(t * 1.5 + idx) * 0.3
            root_p.keyframe_insert(data_path="location", frame=frame)

    # 2. Medusas Bioluminescentes (Flutuando no topo da cena)
    medusas_locs = [(-11, 2.5, 5.2), (-2, 2.0, 5.8), (11, 2.0, 5.4)]
    for idx, (mx, my, mz) in enumerate(medusas_locs):
        root_m = bpy.data.objects.new(f"Medusa_Luz_{idx+1}", None)
        root_m.location = (mx, my, mz)
        col_inimigos.objects.link(root_m)

        # Cúpula da Medusa
        bpy.ops.mesh.primitive_uv_sphere_add(radius=0.7, location=(0, 0, 0))
        cupula = bpy.context.active_object
        cupula.scale = (1.0, 1.0, 0.55)
        cupula.data.materials.append(MATS['medusa_corpo'])
        cupula.parent = root_m
        smooth(cupula)

        # Tentáculos
        for ti in range(6):
            ang = ti * (math.pi / 3)
            bpy.ops.mesh.primitive_cylinder_add(radius=0.04, depth=1.8, vertices=6, location=(math.cos(ang)*0.4, math.sin(ang)*0.4, -0.9))
            tent = bpy.context.active_object
            tent.data.materials.append(MATS['medusa_corpo'])
            tent.parent = root_m
            smooth(tent)

        # Animação de Pulsação Vertical da Medusa
        for frame in [1, 60, 120, 180, 240]:
            t = frame / 20.0
            root_m.location.z = mz + math.sin(t + idx) * 0.4
            root_m.keyframe_insert(data_path="location", frame=frame)

# ============================================================================
# 7. ILUMINAÇÃO DE CENA, GOD RAYS E CÂMERAS
# ============================================================================
def setup_iluminacao_e_cameras(col_luz, col_cam, root_adapa):
    # 1. Luz Solar Superior (God Rays Oceânicos)
    sun_data = bpy.data.lights.new(name="Luz_Solar_Oceano", type='SUN')
    sun_data.color = (0.4, 0.85, 1.0)
    sun_data.energy = 4.5
    sun_obj = bpy.data.objects.new(name="Luz_Solar_Oceano_Obj", object_data=sun_data)
    sun_obj.rotation_euler = (math.radians(35), math.radians(-25), math.radians(45))
    col_luz.objects.link(sun_obj)

    # 2. Luz de Preenchimento Azul Abissal Frontal
    fill_data = bpy.data.lights.new(name="Luz_Fill_Azul", type='SUN')
    fill_data.color = (0.05, 0.25, 0.5)
    fill_data.energy = 1.8
    fill_obj = bpy.data.objects.new(name="Luz_Fill_Azul_Obj", object_data=fill_data)
    fill_obj.rotation_euler = (math.radians(-45), 0, 0)
    col_luz.objects.link(fill_obj)

    # 3. CÂMERA 1: Gameplay 2.5D Dinâmica que segue Adapa
    cam_game_data = bpy.data.cameras.new(name="Cam_Game_25D")
    cam_game_data.lens = 42
    cam_game_data.clip_start = 0.5
    cam_game_data.clip_end = 200.0
    cam_game_obj = bpy.data.objects.new(name="Camera_Gameplay_25D", object_data=cam_game_data)
    col_cam.objects.link(cam_game_obj)

    # Animação de Tracking da Câmera acompanhando o Herói
    for f in [1, 60, 120, 180, 240]:
        # Pega a posição interpolada de Adapa
        t = (f - 1) / 239.0
        x_target = -22.0 + t * 44.0
        cam_game_obj.location = (x_target + 1.5, -14.0, 3.8)
        cam_game_obj.rotation_euler = (math.radians(85), 0, math.radians(2))
        cam_game_obj.keyframe_insert(data_path="location", frame=f)
        cam_game_obj.keyframe_insert(data_path="rotation_euler", frame=f)

    # 4. CÂMERA 2: Panorâmica Geral da Fase (Overview Cinemático)
    cam_pano_data = bpy.data.cameras.new(name="Cam_Panoramica_Overview")
    cam_pano_data.lens = 24
    cam_pano_obj = bpy.data.objects.new(name="Camera_Panoramica_Overview", object_data=cam_pano_data)
    cam_pano_obj.location = (0, -22.0, 9.0)
    cam_pano_obj.rotation_euler = (math.radians(72), 0, 0)
    col_cam.objects.link(cam_pano_obj)

    # 5. CÂMERA 3: Confronto Final no Portal do Boss Kullullû
    cam_boss_data = bpy.data.cameras.new(name="Cam_Boss_Confronto")
    cam_boss_data.lens = 35
    cam_boss_obj = bpy.data.objects.new(name="Camera_Boss_Confronto", object_data=cam_boss_data)
    cam_boss_obj.location = (17.5, -9.0, 3.5)
    cam_boss_obj.rotation_euler = (math.radians(82), 0, math.radians(-15))
    col_cam.objects.link(cam_boss_obj)

    # Define a câmera ativa padrão
    bpy.context.scene.camera = cam_pano_obj

    return {
        'game': cam_game_obj,
        'pano': cam_pano_obj,
        'boss': cam_boss_obj
    }

# ============================================================================
# 8. FUNÇÃO MESTRE DE GERAÇÃO E RENDERIZAÇÃO
# ============================================================================
def gerar_fase_atlantis_completa():
    print("==================================================================")
    print("INICIANDO GERAÇÃO DA FASE 3D DE ATLANTIS (AS ÁGUAS DE APSU)")
    print("==================================================================")

    # 1. Setup
    setup_cena()
    init_materiais()

    # 2. Coleções organizadas no Outliner do Blender
    col_cenario  = get_or_create_collection("01_Terreno_Cenario")
    col_ruinas   = get_or_create_collection("02_Ruinas_Atlantis")
    col_flora    = get_or_create_collection("03_Flora_Recifes")
    col_props    = get_or_create_collection("04_Props_Tesouros")
    col_perigos  = get_or_create_collection("05_Perigos_Abismo")
    col_npc      = get_or_create_collection("06_NPCs_Guardioes")
    col_inimigos = get_or_create_collection("07_Inimigos_Fauna")
    col_heroi    = get_or_create_collection("08_Heroi_Adapa")
    col_boss     = get_or_create_collection("09_Boss_Kullullu")
    col_luz      = get_or_create_collection("10_Iluminacao_Mundo")
    col_cam      = get_or_create_collection("11_Cameras_Cinematicas")

    # 3. Construção do Mundo e Ambientes
    print("-> Construindo Terreno Oceânico...")
    construir_terreno_oceano(col_cenario)

    print("-> Erguendo Ruínas, Colunatas e Cristais de Atlantis...")
    construir_ruinas_colunatas(col_ruinas)

    print("-> Gerando Navio Naufragado e Baús de Tesouro...")
    construir_navio_naufragado(col_props, loc=(-19, 5, 0))
    construir_bau_tesouro(col_props, loc=(-14.5, 0.8, 0.2), rot_z=25)
    construir_bau_tesouro(col_props, loc=(5.0, 3.5, 0.8), rot_z=-15)

    print("-> Plantando Recifes de Corais e Florestas de Kelp...")
    construir_recifes_e_flora(col_flora)

    print("-> Criando Gêiseres Hidrotermais e Minas de Espinhos...")
    construir_perigos_abissais(col_perigos)

    # 4. Construção dos Personagens e Animações
    print("-> Convocando Sábio Enki no Santuário...")
    construir_enki_npc(col_npc, loc=(-16, 2.2, 1.5))

    print("-> Esculpindo Guardião Atlante Monumental...")
    construir_guardiao_atlante(col_npc, loc=(-3, 5.8, 0.0), rot_z=-25)
    construir_guardiao_atlante(col_npc, loc=(18, 5.8, 0.0), rot_z=25)

    print("-> Invocando Inimigos Abissais e Medusas...")
    construir_inimigos_fauna(col_inimigos)

    print("-> Modelando e Animando Boss Kullullû no Portal de Apsu...")
    construir_kullullu_boss(col_boss, loc=(23, 1.5, 2.5))

    print("-> Modelando, Rigando e Animando Nado/Disparo do Herói Adapa...")
    root_adapa = construir_adapa_heroi(col_heroi)

    # 5. Iluminação e Câmeras
    print("-> Configurando Iluminação Submersa, God Rays e Câmeras...")
    cameras = setup_iluminacao_e_cameras(col_luz, col_cam, root_adapa)

    # 6. Salva o Arquivo .blend Final
    print(f"-> Salvando arquivo .blend completo em: {BLEND_FILE}")
    bpy.ops.wm.save_as_mainfile(filepath=BLEND_FILE)

    # 7. Renderização de Imagens de Demonstração
    print("-> Renderizando Imagens de Demonstração da Fase 3D...")
    renders_info = [
        ("01_panoramica_geral_fase.png", cameras['pano'], 1),
        ("02_adapa_nadando_recife_enki.png", cameras['game'], 1),
        ("03_adapa_cidade_colunata_guardiao.png", cameras['game'], 120),
        ("04_adapa_desfiladeiro_perigos.png", cameras['game'], 180),
        ("05_boss_kullullu_confronto.png", cameras['boss'], 240),
    ]

    for fname, cam_obj, frame_num in renders_info:
        bpy.context.scene.camera = cam_obj
        bpy.context.scene.frame_set(frame_num)
        out_path = os.path.join(RENDERS_DIR, fname)
        bpy.context.scene.render.filepath = out_path
        print(f"   [RENDER] Frame {frame_num} com {cam_obj.name} -> {fname}...")
        bpy.ops.render.render(write_still=True)

    print("==================================================================")
    print("FASE 3D DE ATLANTIS GERADA E RENDERIZADA COM SUCESSO TOTAL!")
    print(f"Blend: {BLEND_FILE}")
    print(f"Renders: {RENDERS_DIR}")
    print("==================================================================")

if __name__ == "__main__":
    gerar_fase_atlantis_completa()
