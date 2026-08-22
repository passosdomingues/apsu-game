# -*- coding: utf-8 -*-
"""
============================================================================
 _apsu_shared_lib.py — Biblioteca compartilhada de shading + animação
 As Aguas de Apsu — pipeline procedural Blender 4.5 LTS

 Cole ESTE arquivo em uma nova área de Scripting ANTES dos scripts de
 personagem (01_*, 02_*, etc.) e rode-o primeiro (ou dê "Run Script"),
 OU use-o como módulo: copie a pasta `personagens/scripts` para o
 site-packages do Blender e faça `from _apsu_shared_lib import *`.
 Para uso simples via "colar na área de Scripting", cada script de
 personagem já faz `exec(open(...).read())` apontando pra este arquivo
 (ver bloco no fim de cada script atualizado).

 Por que este arquivo existe (auditoria 2026-08-15):
   1) Os renders atuais (personagens/renders/*.png) mostram personagens
      lendo mal contra os fundos pintados (bg1..bg5.png): a paleta de
      cada personagem foi escolhida "no olho", sem medir a paleta real
      do fundo. Aqui a paleta 60/30/10 de cada fundo foi MEDIDA (PIL,
      quantização MedianCut) e as cores de destaque (rim light) foram
      derivadas por teoria da cor (complementar/quente) a partir dela.
   2) Os frames de "ciclo de nado" em src/main/resources/sprites/**/
      frame_00N.png são, hoje, RENDERS IDENTICOS (5 de 6 frames com
      diff=0 pixel a pixel — confirmado por script) porque nenhum
      script de personagem chama keyframe_insert(). O motor Java já
      faz animação procedural (tail wave, pitch, offsets de ataque)
      em cima dos frames — mas os frames em si nunca mudam de pose.
      As funções `swim_cycle_keyframes` e `attack_pose_keyframes`
      abaixo resolvem isso na origem (Blender), gerando poses reais
      por frame para tools/render_all_2d5_sprites.py capturar.
============================================================================
"""
import bpy
import math
import random

# ----------------------------------------------------------------------
# 1) PALETAS MEDIDAS DOS FUNDOS (bg1.png .. bg5.png)
#    Extraídas por quantização MedianCut (6 cores, PIL) em 2026-08-15.
#    Cada entrada: (nome, dominante_60%, secundaria_30%, hsv_dominante)
#    "rim_accent" = cor de contorno/emissivo recomendada para o
#    personagem SEMPRE vencer o contraste contra aquele fundo
#    específico (calculada por deslocamento de matiz + alto valor).
# ----------------------------------------------------------------------
PHASE_PALETTES = {
    # Fase 1 — Aguas Claras: ciano/turquesa MUITO saturado e claro.
    # Um personagem na mesma família ciano se PERDE. Rim precisa ser
    # quente (laranja/coral) — o oposto do H~195 do fundo.
    "bg1": dict(
        dominant=(0.055, 0.62, 0.86),      # #0e9edb medido
        secondary=(0.64, 0.66, 0.50),      # #a3a980 medido (pedra/musgo)
        rim_accent=(1.00, 0.42, 0.10),     # laranja-coral (complementar)
        mood="claro e saturado — precisa de rim quente forte",
    ),
    # Fase 2 — Cavernas de Coral: quase preto-azulado (V 9-30%).
    # Personagem PRECISA de auto-iluminacao para nao sumir no escuro.
    "bg2": dict(
        dominant=(0.024, 0.09, 0.12),      # #06171f medido
        secondary=(0.086, 0.26, 0.30),     # #16434d medido
        rim_accent=(0.25, 0.95, 0.85),     # bioluminescencia ciano-verde
        mood="quase preto — priorizar emissao propria, nao so rim",
    ),
    # Fase 3 — Correntes Abissais: indigo/violeta escuro (H~255-270).
    "bg3": dict(
        dominant=(0.055, 0.047, 0.094),    # #0e0c18 medido
        secondary=(0.180, 0.125, 0.259),   # #2e2342 medido
        rim_accent=(1.00, 0.80, 0.20),     # dourado quente (oposto do violeta)
        mood="violeta escuro — rim dourado ou ciano puro",
    ),
    # Fase 4 — Abismo Vulcanico: azul-cinza escuro + vermelho de lava distante.
    "bg4": dict(
        dominant=(0.024, 0.051, 0.086),    # #06131c medido
        secondary=(0.165, 0.090, 0.094),   # #2a1718 medido (glow de lava)
        rim_accent=(1.00, 0.35, 0.05),     # laranja-lava
        mood="deve competir com brilho de lava — rim laranja intenso",
    ),
    # Fase 5 — Templo de Apsu: azul profundo + pedra neutra clara.
    "bg5": dict(
        dominant=(0.031, 0.075, 0.145),    # #081324 medido
        secondary=(0.561, 0.576, 0.580),   # #8f9394 medido (pedra do templo)
        rim_accent=(1.00, 0.85, 0.35),     # dourado divino (tema Enki)
        mood="tema divino — rim dourado puro, alto brilho",
    ),
}


def get_phase_rim(phase_key, fallback=(1.0, 0.5, 0.15)):
    """Retorna (r,g,b) do rim_accent medido para a fase, ou fallback."""
    p = PHASE_PALETTES.get(phase_key)
    return p["rim_accent"] if p else fallback


# ----------------------------------------------------------------------
# 2) RIM LIGHT / FRESNEL — faz o personagem "vencer" o fundo
#    Injeta, em cima do material já existente (Principled BSDF),
#    uma emissão mascarada por Fresnel (mais forte nas bordas =
#    silhueta), somada (Add Shader) ao shading normal. É o principal
#    motivo dos personagens da fase 2/3/4 (fundo quase preto) ficarem
#    ilegíveis: nunca houve emissão de borda, só cor plana.
# ----------------------------------------------------------------------
def add_fresnel_rim(mat, rim_rgb, power=1.5, strength=6.0, ior=1.33):
    """
    mat: bpy.types.Material (precisa use_nodes=True e ter um
         Principled BSDF já ligado ao Material Output).
    rim_rgb: tupla (r,g,b) 0..1 — cor do brilho de contorno.
    power: quao estreita a faixa de borda é (MAIOR = mais fina/rente
           à silhueta; MENOR = cobre mais da superfície visível).
           2026-08-16: default caiu de 2.6 pra 1.5 — no primeiro render
           real (Rafa, Blender 4.5), o efeito só aparecia em 1-2 pixels
           bem na borda, imperceptível a olho. Testado por amostragem
           de pixel no render (ver auditoria) — a faixa antiga só
           começava a acender depois de 61% do Fac do Fresnel; agora
           começa aos 33%, cobrindo bem mais silhueta.
    strength: intensidade da emissao no contorno (subiu de 3.2 pra 6.0
           pelo mesmo motivo — precisa competir com as luzes de área/
           ponto já fortes da cena + compressão do Filmic).
    """
    if mat is None or not mat.use_nodes:
        return mat
    nt = mat.node_tree
    nodes, links = nt.nodes, nt.links

    if nodes.get("APSU_RimAdd"):
        return mat  # ja aplicado, idempotente

    out = next((n for n in nodes if n.type == 'OUTPUT_MATERIAL'), None)
    bsdf = next((n for n in nodes if n.type == 'BSDF_PRINCIPLED'), None)
    if out is None or bsdf is None:
        return mat

    fres = nodes.new('ShaderNodeFresnel')
    fres.name = "APSU_Fresnel"
    fres.inputs['IOR'].default_value = ior

    ramp = nodes.new('ShaderNodeValToRGB')
    ramp.name = "APSU_RimRamp"
    ramp.color_ramp.elements[0].position = 1.0 - (1.0 / max(power, 1.0))
    ramp.color_ramp.elements[1].position = 1.0

    emit = nodes.new('ShaderNodeEmission')
    emit.name = "APSU_RimEmit"
    emit.inputs['Color'].default_value = (*rim_rgb, 1.0)
    emit.inputs['Strength'].default_value = strength

    add = nodes.new('ShaderNodeAddShader')
    add.name = "APSU_RimAdd"

    links.new(fres.outputs['Fac'], ramp.inputs['Fac'])
    # ramp e preto->branco (default) = mascara 0..1 da borda; a COR do
    # rim fica fixa em rim_rgb (setada acima em emit.inputs['Color']),
    # so a INTENSIDADE (Strength) e modulada pela mascara via mix_str.
    mix_str = nodes.new('ShaderNodeMath')
    mix_str.name = "APSU_RimStrengthMix"
    mix_str.operation = 'MULTIPLY'
    mix_str.inputs[1].default_value = strength
    links.new(ramp.outputs['Color'], mix_str.inputs[0])
    links.new(mix_str.outputs['Value'], emit.inputs['Strength'])

    # religa a saida do BSDF original -> Add Shader -> Output
    old_link = next((l for l in bsdf.outputs['BSDF'].links), None)
    links.new(bsdf.outputs['BSDF'], add.inputs[0])
    links.new(emit.outputs['Emission'], add.inputs[1])
    links.new(add.outputs['Shader'], out.inputs['Surface'])

    # organiza posicao visual dos nos novos (nao afeta shading)
    fres.location = (bsdf.location.x - 380, bsdf.location.y - 260)
    ramp.location = (bsdf.location.x - 200, bsdf.location.y - 260)
    mix_str.location = (bsdf.location.x - 40, bsdf.location.y - 260)
    emit.location = (bsdf.location.x + 140, bsdf.location.y - 260)
    add.location = (bsdf.location.x + 340, bsdf.location.y)
    out.location = (add.location.x + 220, out.location.y)
    return mat


# ----------------------------------------------------------------------
# 3) PBR ORGANICO (pele/escamas com subsurface de verdade)
#    O material de escamas original (`criar_mat_escamas`) usa
#    Subsurface Weight = 0.12, praticamente imperceptivel. Pele/escama
#    debaixo d'agua precisa de SSS visivel para nao parecer plastico.
# ----------------------------------------------------------------------
def make_organic_pbr(name, base_color, sss_weight=0.30,
                      sss_radius=(0.35, 0.16, 0.10),
                      roughness=0.30, metallic=0.0, clearcoat=0.15):
    mat = bpy.data.materials.new(name=name)
    mat.use_nodes = True
    nodes = mat.node_tree.nodes
    bsdf = nodes.get("Principled BSDF")
    if not bsdf:
        return mat
    b = bsdf.inputs
    if 'Base Color' in b: b['Base Color'].default_value = (*base_color, 1.0)
    if 'Roughness' in b: b['Roughness'].default_value = roughness
    if 'Metallic' in b: b['Metallic'].default_value = metallic
    if 'Subsurface Weight' in b: b['Subsurface Weight'].default_value = sss_weight
    if 'Subsurface Radius' in b: b['Subsurface Radius'].default_value = sss_radius
    if 'Subsurface Color' in b: b['Subsurface Color'].default_value = (*base_color, 1.0)
    if 'Coat Weight' in b: b['Coat Weight'].default_value = clearcoat
    if 'Coat Roughness' in b: b['Coat Roughness'].default_value = 0.15
    return mat


# ----------------------------------------------------------------------
# 3b) GEOMETRIA PROCEDURAL DE CADEIA — curva suave, não ângulos soltos
#     Achado 2026-08-16: a cauda do Adapa era uma lista de 7 tuplas com
#     ângulos escolhidos um a um à mão (0°,20°,35°,20°,-10°,-30°,-45°) —
#     o salto 20°→35°→20° não é uma curva, é um "kink" visual (confirmado
#     em render real + crop ampliado). Regra geral do projeto agora:
#     cadeias articuladas (cauda, tentáculo, barbatana) usam uma curva
#     C1-contínua (smoothstep) com ondulação opcional, nunca uma lista
#     de ângulos arbitrários.
# ----------------------------------------------------------------------
def smooth_angle_chain(n, start_deg, end_deg, wobble_deg=0.0, wobble_cycles=1.0):
    """
    Gera n ângulos (radianos) interpolando start_deg -> end_deg com
    smoothstep (1a derivada contínua nas pontas — sem saltos bruscos
    entre vizinhos) e uma ondulação senoidal opcional SOBREPOSTA que
    se anula nas duas pontas (via sin(pi*t)), então nunca introduz
    descontinuidade nova, só variação orgânica no meio da cadeia.
    """
    out = []
    for i in range(n):
        t = i / max(n - 1, 1)
        s = t * t * (3 - 2 * t)  # smoothstep
        base = start_deg + (end_deg - start_deg) * s
        wobble = wobble_deg * math.sin(wobble_cycles * math.pi * t) * math.sin(math.pi * t)
        out.append(math.radians(base + wobble))
    return out


def rebuild_chain_angles(cauda_original, rx_start_deg=None, rx_end_deg=None,
                          rz_start_deg=None, rz_end_deg=None,
                          rx_wobble_deg=4.0, rz_wobble_deg=3.0):
    """
    Recebe a lista original de tuplas (lx,ly,lz,r1,dep,rx,rz) — preserva
    POSIÇÃO/RAIO/PROFUNDIDADE (já aprovados, controlam silhueta geral)
    e SUBSTITUI só rx/rz por uma curva suave. Se rx_start/end não forem
    passados, usa o primeiro/último ângulo da lista original como âncora
    (mantém a pose "esticada" e "curvada" nas pontas, só remove o kink
    do meio).
    """
    n = len(cauda_original)
    if rx_start_deg is None: rx_start_deg = math.degrees(cauda_original[0][5])
    if rx_end_deg is None: rx_end_deg = math.degrees(cauda_original[-1][5])
    if rz_start_deg is None: rz_start_deg = math.degrees(cauda_original[0][6])
    if rz_end_deg is None: rz_end_deg = math.degrees(cauda_original[-1][6])

    rx_list = smooth_angle_chain(n, rx_start_deg, rx_end_deg, rx_wobble_deg, 1.3)
    rz_list = smooth_angle_chain(n, rz_start_deg, rz_end_deg, rz_wobble_deg, 1.6)

    out = []
    for i, (lx, ly, lz, r1, dep, _rx, _rz) in enumerate(cauda_original):
        out.append((lx, ly, lz, r1, dep, rx_list[i], rz_list[i]))
    return out


# ----------------------------------------------------------------------
# 4) ANIMACAO PROCEDURAL — CICLO DE NADO ANGUILIFORME
#
#    Sistema de coordenadas confirmado a partir do rig de camera usado
#    em todos os scripts de personagem (`cam.rotation_euler=(90°,0,0)`,
#    camera em Y negativo olhando para +Y):
#        tela horizontal (esquerda/direita) = eixo mundo X
#        tela vertical   (cima/baixo)       = eixo mundo Z
#        profundidade (nao visivel)         = eixo mundo Y
#    Logo, para um "aceno" lateral de cauda LEGIVEL na tela (o efeito
#    de nado estilo Donkey Kong Country), a rotacao que precisa oscilar
#    e a de eixo Y (rotation_euler[1]) de cada segmento — ela varre a
#    ponta do segmento pelo plano X-Z, que e exatamente o plano da tela.
#    Isso NAO foi testado em Blender real (sandbox sem Blender/GPU) —
#    a logica de eixos foi derivada por calculo, nao por preview visual.
#    Rode e confira visualmente antes de aceitar (ver checklist no
#    doc de auditoria).
# ----------------------------------------------------------------------
def swim_cycle_keyframes(root_obj, spine_segments, frame_count=8,
                          amp_deg=15.0, wavelength_segs=3.2, cycles=1.0,
                          bob_amp=0.05, root_sway_deg=3.0, wave_axis=1,
                          bob_axis=2, frame_start=1):
    """
    root_obj: objeto raiz do personagem (Empty pai) — recebe bob de
              flutuacao (empuxo) + sway leve.
    spine_segments: lista ORDENADA cabeca->cauda dos objetos da
              coluna/cauda (ex.: os 7 segmentos de cone da cauda do
              Adapa). A amplitude cresce da cabeca (pouco) pra ponta
              da cauda (muito), como nado real de peixe (anguiliforme).
    frame_count: quantos frames o ciclo completo ocupa (a ferramenta
              de render usa frame_set(1..N) — mantenha compativel com
              tools/render_all_2d5_sprites.py, hoje N=8).
    amp_deg: amplitude MAXIMA (na ponta da cauda) em graus.
    wavelength_segs: quantos segmentos cabem em 1 comprimento de onda
              (controla a defasagem entre segmentos consecutivos).
    cycles: quantos ciclos completos de onda cabem em frame_count
              (1.0 = um ciclo completo de nado no loop).
    bob_amp: amplitude do bob de flutuacao vertical do root, em
              unidades Blender (metros).
    wave_axis/bob_axis: indices de eixo (0=X,1=Y,2=Z) — trocar aqui
              se a camera do personagem-alvo tiver outra orientacao.
    """
    base_rot = {}
    for i, seg in enumerate(spine_segments):
        base_rot[seg.name] = tuple(seg.rotation_euler)
    base_root_rot = tuple(root_obj.rotation_euler) if root_obj else None
    base_root_loc = tuple(root_obj.location) if root_obj else None

    # BUGFIX 2026-08-16 (achado por render real do Rafa — cauda em zigue-zague):
    # sin(phase_global - phase_offset) com phase_global=0 no frame_start NÃO
    # é zero pra todo segmento (só pra quem tem phase_offset=0) — é fisicamente
    # correto pra uma onda viajante contínua (não existe "frame neutro" dentro
    # de um ciclo real), mas isso fazia o PRIMEIRO frame renderizado (usado
    # como imagem estática de referência) sair com cada segmento num ponto
    # diferente da onda, em vez da pose desenhada à mão original — lido como
    # zigue-zague entre segmentos no render.
    # Fix: grava a pose BASE (sem nenhuma oscilação) como uma âncora no frame
    # (frame_start - 1), fora do intervalo 1..frame_count usado pelo tool de
    # sprite sheet — e deixa o frame atual da cena nessa âncora ao final, pra
    # qualquer render "estático" (salvar_render) pegar a pose limpa e não um
    # instante do ciclo.
    anchor_frame = frame_start - 1
    for seg in spine_segments:
        seg.rotation_euler = base_rot[seg.name]
        seg.keyframe_insert(data_path="rotation_euler", frame=anchor_frame)
    if root_obj is not None:
        root_obj.location = base_root_loc
        root_obj.keyframe_insert(data_path="location", frame=anchor_frame)
        root_obj.rotation_euler = base_root_rot
        root_obj.keyframe_insert(data_path="rotation_euler", frame=anchor_frame)

    n_seg = max(len(spine_segments), 1)
    for f in range(frame_start, frame_start + frame_count):
        t = (f - frame_start) / float(frame_count)
        phase_global = 2.0 * math.pi * cycles * t

        for i, seg in enumerate(spine_segments):
            envelope = (i + 1) / n_seg  # 0->pouco (cabeca), 1->muito (cauda)
            phase_offset = (i / max(wavelength_segs, 0.01)) * 2.0 * math.pi
            delta = math.radians(amp_deg) * envelope * math.sin(phase_global - phase_offset)

            rx, ry, rz = base_rot[seg.name]
            new_rot = [rx, ry, rz]
            new_rot[wave_axis] = new_rot[wave_axis] + delta
            seg.rotation_euler = new_rot
            seg.keyframe_insert(data_path="rotation_euler", frame=f)

        if root_obj is not None:
            # bob de flutuacao (empuxo variando ao longo do ciclo) + sway leve
            bob = bob_amp * math.sin(phase_global)
            sway = math.radians(root_sway_deg) * math.sin(phase_global + math.pi / 2.0)

            loc = list(base_root_loc)
            loc[bob_axis] = loc[bob_axis] + bob
            root_obj.location = loc
            root_obj.keyframe_insert(data_path="location", frame=f)

            rot = list(base_root_rot)
            rot[wave_axis] = rot[wave_axis] + sway
            root_obj.rotation_euler = rot
            root_obj.keyframe_insert(data_path="rotation_euler", frame=f)

    # restaura estado base e fecha o loop — o frame ATUAL da cena fica ancorado
    # em anchor_frame (pose limpa), não em frame_start (que já tem oscilação),
    # então salvar_render() (chamado logo depois disso, sem trocar de frame)
    # captura a pose de referência, não um instante do ciclo de nado.
    for seg in spine_segments:
        seg.rotation_euler = base_rot[seg.name]
    if root_obj is not None:
        root_obj.location = base_root_loc
        root_obj.rotation_euler = base_root_rot

    # interpolacao suave (nao-linear) em todas as curvas recem-criadas
    _set_smooth_interpolation(spine_segments + ([root_obj] if root_obj else []))
    bpy.context.scene.frame_start = anchor_frame
    bpy.context.scene.frame_end = frame_start + frame_count - 1
    bpy.context.scene.frame_set(anchor_frame)


def _set_smooth_interpolation(objs, interp='SINE'):
    for obj in objs:
        if obj is None or obj.animation_data is None or obj.animation_data.action is None:
            continue
        for fc in obj.animation_data.action.fcurves:
            for kp in fc.keyframe_points:
                kp.interpolation = interp


# ----------------------------------------------------------------------
# 5) VARIACOES DE POSE DE ATAQUE (thrust / slash / spin / charge)
#    Gera poses-chave para o braço/arma (ex.: tridente do Adapa) —
#    usa o mesmo mapeamento de eixos do item 4 (Y = arco visivel na
#    tela, X = avanço/recuo pra frente do personagem).
# ----------------------------------------------------------------------
ATTACK_STYLES = ("thrust", "slash", "spin", "charge")


def attack_pose_keyframes(weapon_root, torso_obj, style="thrust",
                           frame_start=1, frame_count=6, arc_axis=1,
                           forward_axis=0, facing=1):
    """
    weapon_root: Empty pai da arma (ex.: `tr_root` do tridente).
    torso_obj: objeto do torso, para aplicar squash-and-stretch de
               impacto (opcional, pode ser None).
    style: 'thrust' (estocada reta), 'slash' (arco lateral amplo),
           'spin' (giro completo), 'charge' (recuo + salto pra frente).
    facing: 1 ou -1 — lado para o qual o personagem esta olhando
            (multiplica o eixo forward_axis).
    """
    if style not in ATTACK_STYLES:
        raise ValueError(f"style deve ser um de {ATTACK_STYLES}")

    base_rot = tuple(weapon_root.rotation_euler)
    base_loc = tuple(weapon_root.location)
    base_torso_scale = tuple(torso_obj.scale) if torso_obj else None

    def kf_weapon(f, d_rot=(0, 0, 0), d_loc=(0, 0, 0)):
        weapon_root.rotation_euler = (base_rot[0] + d_rot[0],
                                       base_rot[1] + d_rot[1],
                                       base_rot[2] + d_rot[2])
        weapon_root.location = (base_loc[0] + d_loc[0],
                                 base_loc[1] + d_loc[1],
                                 base_loc[2] + d_loc[2])
        weapon_root.keyframe_insert(data_path="rotation_euler", frame=f)
        weapon_root.keyframe_insert(data_path="location", frame=f)

    def kf_squash(f, sx, sy, sz):
        if torso_obj is None:
            return
        torso_obj.scale = (base_torso_scale[0] * sx,
                            base_torso_scale[1] * sy,
                            base_torso_scale[2] * sz)
        torso_obj.keyframe_insert(data_path="scale", frame=f)

    f0, f1, f2, f3 = (frame_start, frame_start + max(1, frame_count // 3),
                       frame_start + max(2, 2 * frame_count // 3),
                       frame_start + frame_count - 1)

    if style == "thrust":
        d_fwd = [0.0, 0.0, 0.0]
        kf_weapon(f0)                                   # repouso
        d_fwd[forward_axis] = -0.15 * facing
        kf_weapon(f1, d_rot=(0, 0, 0), d_loc=tuple(d_fwd))       # carga (recuo)
        kf_squash(f1, 1.08, 0.94, 0.94)
        d_fwd[forward_axis] = 0.55 * facing
        kf_weapon(f2, d_rot=(0, 0, 0), d_loc=tuple(d_fwd))       # estocada
        kf_squash(f2, 0.90, 1.08, 1.08)
        kf_weapon(f3)                                    # retorno
        kf_squash(f3, 1.0, 1.0, 1.0)

    elif style == "slash":
        arc = [0.0, 0.0, 0.0]
        arc[arc_axis] = math.radians(-35) * facing
        kf_weapon(f0, d_rot=tuple(arc))
        arc[arc_axis] = math.radians(55) * facing
        kf_weapon(f1, d_rot=tuple(arc))
        kf_squash(f1, 1.05, 0.95, 1.0)
        arc[arc_axis] = math.radians(120) * facing
        kf_weapon(f2, d_rot=tuple(arc))
        arc[arc_axis] = math.radians(70) * facing
        kf_weapon(f3, d_rot=tuple(arc))

    elif style == "spin":
        for i in range(frame_count):
            f = frame_start + i
            ang = [0.0, 0.0, 0.0]
            ang[arc_axis] = 2.0 * math.pi * (i / float(frame_count)) * facing
            kf_weapon(f, d_rot=tuple(ang))
        kf_squash(frame_start + frame_count // 2, 0.92, 1.10, 1.10)

    elif style == "charge":
        back = [0.0, 0.0, 0.0]
        back[forward_axis] = -0.30 * facing
        kf_weapon(f0)
        kf_weapon(f1, d_loc=tuple(back))
        kf_squash(f1, 1.15, 0.88, 0.88)
        fwd = [0.0, 0.0, 0.0]
        fwd[forward_axis] = 0.85 * facing
        kf_weapon(f2, d_loc=tuple(fwd))
        kf_squash(f2, 0.85, 1.15, 1.15)
        kf_weapon(f3)
        kf_squash(f3, 1.0, 1.0, 1.0)

    weapon_root.rotation_euler = base_rot
    weapon_root.location = base_loc
    if torso_obj:
        torso_obj.scale = base_torso_scale

    _set_smooth_interpolation([weapon_root, torso_obj] if torso_obj else [weapon_root], interp='BEZIER')
    bpy.context.scene.frame_start = frame_start
    bpy.context.scene.frame_end = frame_start + frame_count - 1


# ----------------------------------------------------------------------
# 6) Utilidade: aplica rim de fase em uma lista (nome_mat -> objeto)
# ----------------------------------------------------------------------
def apply_phase_rim_to_materials(materials, phase_key, power=1.5, strength=6.0):
    rim = get_phase_rim(phase_key)
    for mat in materials:
        add_fresnel_rim(mat, rim, power=power, strength=strength)


# ----------------------------------------------------------------------
# 7) BOLHAS DE ATAQUE (2026-08-16) — Rafa pediu pra usar os 4 estilos de
#    ataque (thrust/slash/spin/charge) no lugar das 2 poses antigas do
#    tiro de bolha. As poses antigas tinham bolha modelada JUNTO da
#    pose (residuo visual de "acabei de atirar"); os 4 estilos novos
#    (reusam construir_adapa(), corpo puro) não tinham. Esta função
#    resolve isso — cria um material glassy-emissivo (mesma receita da
#    pose antiga) e posiciona bolhas num padrão que varia por estilo,
#    sempre perto da ponta do tridente e PARENTEADAS a ele (se o
#    tridente girar/avançar na animação, as bolhas acompanham).
# ----------------------------------------------------------------------
def create_bubble_material(name="M_BolhaAtaque"):
    mat = bpy.data.materials.new(name=name)
    mat.use_nodes = True
    bsdf = mat.node_tree.nodes.get("Principled BSDF")
    if bsdf:
        b = bsdf.inputs
        if 'Base Color' in b: b['Base Color'].default_value = (0.3, 0.95, 1.0, 1)
        if 'Roughness' in b: b['Roughness'].default_value = 0.02
        if 'Transmission Weight' in b: b['Transmission Weight'].default_value = 0.6
        if 'Emission Color' in b: b['Emission Color'].default_value = (0.2, 0.88, 1.0, 1)
        if 'Emission Strength' in b: b['Emission Strength'].default_value = 5.5
    return mat


def add_attack_bubbles(parent_obj, tip_local=(0, 0, 2.0), style="thrust",
                        material=None, seed=0):
    """
    Cria um pequeno grupo de bolhas perto da ponta do tridente,
    parenteadas a `parent_obj` (normalmente tr_root), com um padrão de
    espalhamento diferente por estilo de ataque — reforça a leitura de
    cada golpe em vez de ser sempre o mesmo punhado de bolhas:
      thrust -> trilha estreita, alongada na direção do golpe (X)
      slash  -> arco espalhado lateralmente (leque)
      spin   -> anel ao redor da ponta (bolhas em círculo)
      charge -> aglomerado denso na ponta (carga concentrada)
    """
    rnd = random.Random(seed)
    mat = material or create_bubble_material()
    tx, ty, tz = tip_local
    pts = []
    if style == "thrust":
        for i in range(6):
            t = i / 5.0
            pts.append((tx + 0.15 + t*0.9, ty + rnd.uniform(-0.08, 0.08),
                        tz + t*0.25 + rnd.uniform(-0.05, 0.05), 0.10 + 0.16*(1-t)))
    elif style == "slash":
        for i in range(7):
            a = math.radians(-40 + 100 * i / 6.0)
            r = 0.55 + rnd.uniform(-0.05, 0.08)
            pts.append((tx + math.cos(a)*r, ty + rnd.uniform(-0.06, 0.06),
                        tz + math.sin(a)*r*0.6, 0.09 + rnd.uniform(0, 0.12)))
    elif style == "spin":
        for i in range(8):
            a = 2*math.pi*i/8.0
            r = 0.5
            pts.append((tx + math.cos(a)*r, ty + math.sin(a)*r*0.4,
                        tz + rnd.uniform(-0.1, 0.1), 0.08 + rnd.uniform(0, 0.10)))
    else:  # charge
        for i in range(9):
            pts.append((tx + rnd.uniform(-0.28, 0.28), ty + rnd.uniform(-0.2, 0.1),
                        tz + rnd.uniform(-0.28, 0.28), 0.09 + rnd.uniform(0, 0.18)))

    bubbles = []
    for bx, by, bz, br in pts:
        bpy.ops.mesh.primitive_uv_sphere_add(segments=14, ring_count=10, radius=br, location=(bx, by, bz))
        bl = bpy.context.active_object
        bl.data.materials.append(mat)
        bl.parent = parent_obj
        _shade_smooth(bl)
        bubbles.append(bl)
    return bubbles


def _shade_smooth(obj):
    try:
        bpy.context.view_layer.objects.active = obj
        bpy.ops.object.shade_smooth()
    except Exception:
        pass


# ----------------------------------------------------------------------
# 8) VIDA IDLE PARA NPCs DE MANTO (Enki, Guardião Atlante) — 2026-08-16
#    Esses personagens não têm cauda articulada (são humanoides de
#    manto/robe), então swim_cycle_keyframes não se aplica direto. Em
#    vez disso: leve balanço do manto (como um tecido boiando na
#    água), "respiração" no torso (pulso de escala sutil) e um pouco
#    de deriva/inclinação na cabeça — o suficiente pra parecerem vivos
#    parados, sem exigir uma cadeia de segmentos.
# ----------------------------------------------------------------------
def add_idle_life_keyframes(root_obj, manto_obj=None, torso_obj=None,
                             cabeca_obj=None, frame_count=8,
                             sway_deg=3.5, breathe_amount=0.02,
                             head_tilt_deg=2.0, cycles=1.0, frame_start=1):
    """
    root_obj: objeto raiz do personagem — recebe deriva vertical leve
              (mesma ideia do bob de flutuação do herói, mas mais sutil,
              já que estes personagens ficam parados/flutuando no lugar).
    manto_obj: capa/manto/robe — balança como tecido embaixo d'água
               (rotação em Y, mesmo eixo "visível na tela" usado em
               swim_cycle_keyframes).
    torso_obj: pulso de escala leve (respiração).
    cabeca_obj: leve inclinação/olhar, defasada do manto pra não ficar
               tudo em fase (movimento mais orgânico).
    """
    targets = [o for o in (root_obj, manto_obj, torso_obj, cabeca_obj) if o is not None]
    base = {o.name: {"rot": tuple(o.rotation_euler), "loc": tuple(o.location),
                      "scale": tuple(o.scale)} for o in targets}

    anchor_frame = frame_start - 1
    for f in list(range(anchor_frame, frame_start)) + list(range(frame_start, frame_start + frame_count)):
        t = 0.0 if f == anchor_frame else (f - frame_start) / float(frame_count)
        phase = 2.0 * math.pi * cycles * t

        if root_obj is not None:
            bob = 0.04 * math.sin(phase) if f != anchor_frame else 0.0
            loc = list(base[root_obj.name]["loc"]); loc[2] += bob
            root_obj.location = loc
            root_obj.keyframe_insert(data_path="location", frame=f)

        if manto_obj is not None:
            sway = math.radians(sway_deg) * math.sin(phase) if f != anchor_frame else 0.0
            rot = list(base[manto_obj.name]["rot"]); rot[1] += sway
            manto_obj.rotation_euler = rot
            manto_obj.keyframe_insert(data_path="rotation_euler", frame=f)

        if torso_obj is not None:
            breathe = breathe_amount * math.sin(phase * 0.5) if f != anchor_frame else 0.0
            sc = base[torso_obj.name]["scale"]
            torso_obj.scale = (sc[0]*(1+breathe*0.5), sc[1]*(1+breathe*0.5), sc[2]*(1-breathe*0.3))
            torso_obj.keyframe_insert(data_path="scale", frame=f)

        if cabeca_obj is not None:
            tilt = math.radians(head_tilt_deg) * math.sin(phase + math.pi/3) if f != anchor_frame else 0.0
            rot = list(base[cabeca_obj.name]["rot"]); rot[1] += tilt
            cabeca_obj.rotation_euler = rot
            cabeca_obj.keyframe_insert(data_path="rotation_euler", frame=f)

    for o in targets:
        o.rotation_euler = base[o.name]["rot"]
        o.location = base[o.name]["loc"]
        o.scale = base[o.name]["scale"]

    _set_smooth_interpolation(targets)
    bpy.context.scene.frame_start = anchor_frame
    bpy.context.scene.frame_end = frame_start + frame_count - 1
    bpy.context.scene.frame_set(anchor_frame)


if __name__ == "__main__":
    print("_apsu_shared_lib.py carregado — funcoes disponiveis:",
          "get_phase_rim, add_fresnel_rim, make_organic_pbr, smooth_angle_chain,",
          "rebuild_chain_angles, swim_cycle_keyframes, attack_pose_keyframes,",
          "add_attack_bubbles, add_idle_life_keyframes, apply_phase_rim_to_materials")
