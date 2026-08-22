import bpy
import math
import os

# ============================================================
# 01 - ADAPA: O HEROI APKALLU (v5.0 - Rim Light + Ciclo de Nado Real)
# Paleta 60-30-10:
#   60% Turquesa / Verde-Agua Vibrante (Corpo, Cauda, Escamas)
#   30% Dourado Mesopotamico / Bronze Real (Ombreiras, Tiara, Tridente)
#   10% Coral / Laranja Solar Emissivo (Olhos, Joias, Nucleo do Tridente)
#
# v6.0 (auditoria 2026-08-15/16):
#   - Rim light (Fresnel) nas superficies grandes (escama/pele/ouro),
#     pra silhueta vencer o fundo pintado (bg1..bg5) em vez de se
#     confundir com ele. Cor quente universal (funciona bem contra 4
#     das 5 paletas medidas; ver _apsu_shared_lib.py PHASE_PALETTES).
#   - Subsurface Scattering da escama subiu de 0.12 (quase zero) para
#     algo realmente visivel, com raio e cor proprios.
#   - Ciclo de nado real: a cauda agora tem keyframes (onda
#     anguiliforme, amplitude crescendo da cabeca pra ponta) + bob de
#     flutuacao no root. Antes, os 6 frames renderizados eram a MESMA
#     pose (ciclo de nado nao existia — so animacao no lado Java).
#   - construir_adapa() agora RETORNA um dict com os objetos-chave
#     (root, torso, tr_root, cauda_segs, materiais) pra ser reusado
#     por outros scripts (ex.: variações de ataque) sem duplicar codigo.
# ============================================================

# --- Carrega a biblioteca compartilhada (mesma pasta deste script) ---
try:
    _SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
except NameError:
    _SCRIPT_DIR = os.getcwd()  # fallback se colado direto na area de Scripting
_LIB_PATH = os.path.join(_SCRIPT_DIR, "_apsu_shared_lib.py")
if os.path.exists(_LIB_PATH):
    exec(compile(open(_LIB_PATH, encoding="utf-8").read(), _LIB_PATH, 'exec'))
else:
    print("[APSU] AVISO: _apsu_shared_lib.py nao encontrado em " + _LIB_PATH)
    print("[APSU] Rim light e ciclo de nado real NAO serao aplicados nesta rodada.")

    def add_fresnel_rim(mat, *a, **kw): return mat
    def swim_cycle_keyframes(*a, **kw): pass

# Rim quente universal: funciona contra bg1 (coral), bg3 (dourado),
# bg4 (laranja-lava) e bg5 (dourado divino). So bg2 (caverna quase
# preta) preferiria ciano-bio, mas la QUALQUER cor clara ja pop contra
# o preto — entao um so tom serve pro heroi (ele aparece nas 5 fases).
HERO_RIM = (1.0, 0.55, 0.16)

def setup_cena():
    bpy.ops.object.select_all(action='SELECT')
    bpy.ops.object.delete(use_global=False)
    for mesh in list(bpy.data.meshes):
        bpy.data.meshes.remove(mesh, do_unlink=True)
    for mat in list(bpy.data.materials):
        bpy.data.materials.remove(mat, do_unlink=True)

    scene = bpy.context.scene
    try:
        scene.render.engine = 'BLENDER_EEVEE_NEXT'
    except:
        scene.render.engine = 'BLENDER_EEVEE'
    if hasattr(scene, 'eevee') and hasattr(scene.eevee, 'taa_render_samples'):
        scene.eevee.taa_render_samples = 16
    scene.render.resolution_x = 1080
    scene.render.resolution_y = 1920
    scene.render.film_transparent = True
    scene.view_settings.view_transform = 'Filmic'
    scene.view_settings.look = 'Medium High Contrast'

def criar_mat_escamas(nome, cor_a, cor_b, metallic=0.65, roughness=0.22):
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
    vor.feature = 'F1'
    vor.inputs['Scale'].default_value = 14.0
    ramp.color_ramp.elements[0].position = 0.2
    ramp.color_ramp.elements[0].color = cor_b
    ramp.color_ramp.elements[1].position = 0.8
    ramp.color_ramp.elements[1].color = cor_a
    bump.inputs['Strength'].default_value = 0.4
    if 'Metallic' in bsdf.inputs: bsdf.inputs['Metallic'].default_value = metallic
    if 'Roughness' in bsdf.inputs: bsdf.inputs['Roughness'].default_value = roughness
    # SSS subiu de 0.12 (quase imperceptivel) pra um valor que realmente
    # se ve — escama debaixo d'agua sem SSS lê como plastico opaco.
    if 'Subsurface Weight' in bsdf.inputs: bsdf.inputs['Subsurface Weight'].default_value = 0.30
    if 'Subsurface Radius' in bsdf.inputs: bsdf.inputs['Subsurface Radius'].default_value = (0.3, 0.5, 0.4)
    if 'Subsurface Color' in bsdf.inputs: bsdf.inputs['Subsurface Color'].default_value = cor_a
    links.new(coord.outputs['Object'], vor.inputs['Vector'])
    links.new(vor.outputs['Distance'], ramp.inputs['Fac'])
    links.new(ramp.outputs['Color'], bsdf.inputs['Base Color'])
    links.new(vor.outputs['Distance'], bump.inputs['Height'])
    links.new(bump.outputs['Normal'], bsdf.inputs['Normal'])
    links.new(bsdf.outputs['BSDF'], out.inputs['Surface'])
    return mat

def criar_mat(nome, cor, metallic=0.1, roughness=0.3, em=False, em_cor=None, em_str=3.0):
    mat = bpy.data.materials.new(name=nome)
    mat.use_nodes = True
    bsdf = mat.node_tree.nodes.get("Principled BSDF")
    if bsdf:
        if 'Base Color' in bsdf.inputs: bsdf.inputs['Base Color'].default_value = cor
        if 'Metallic' in bsdf.inputs: bsdf.inputs['Metallic'].default_value = metallic
        if 'Roughness' in bsdf.inputs: bsdf.inputs['Roughness'].default_value = roughness
        if em and em_cor:
            if 'Emission Color' in bsdf.inputs: bsdf.inputs['Emission Color'].default_value = em_cor
            if 'Emission Strength' in bsdf.inputs: bsdf.inputs['Emission Strength'].default_value = em_str
    return mat

def smooth(obj):
    if obj and obj.type == 'MESH':
        for p in obj.data.polygons: p.use_smooth = True

def add_obj(prim_fn, mat, parent, **kwargs):
    prim_fn(**kwargs)
    obj = bpy.context.active_object
    obj.data.materials.append(mat)
    obj.parent = parent
    smooth(obj)
    return obj

def construir_adapa():
    setup_cena()

    # Materiais
    m_escama   = criar_mat_escamas("M_Escama", (0.05, 0.88, 0.72, 1), (0.01, 0.38, 0.48, 1))
    m_pele     = criar_mat("M_Pele",     (0.86, 0.68, 0.52, 1), roughness=0.5)
    m_barba    = criar_mat("M_Barba",    (0.08, 0.05, 0.04, 1), roughness=0.75)
    m_ouro     = criar_mat("M_Ouro",     (0.98, 0.78, 0.18, 1), metallic=0.95, roughness=0.12)
    m_barbat   = criar_mat("M_Barbat",   (0.95, 0.82, 0.25, 1), metallic=0.75, roughness=0.18)
    m_olho     = criar_mat("M_Olho",     (1.0, 0.45, 0.1, 1),   em=True, em_cor=(1.0, 0.45, 0.1, 1), em_str=7.0)
    m_joia     = criar_mat("M_Joia",     (1.0, 0.3, 0.05, 1),   metallic=0.8, roughness=0.1, em=True, em_cor=(1.0, 0.35, 0.1, 1), em_str=5.0)
    m_agua     = criar_mat("M_Agua",     (0.3, 0.9, 1.0, 1),    roughness=0.05, em=True, em_cor=(0.2, 0.85, 1.0, 1), em_str=5.0)

    # Root - sem rotacao para que bounding box fique previsivel
    root = bpy.data.objects.new("Adapa_Root", None)
    bpy.context.scene.collection.objects.link(root)

    # === CORPO SUPERIOR ===
    # Torso
    bpy.ops.mesh.primitive_uv_sphere_add(segments=32, ring_count=24, radius=0.72, location=(0, 0, 2.8))
    torso = bpy.context.active_object
    torso.scale = (1.0, 0.78, 1.1)
    torso.data.materials.append(m_pele)
    torso.parent = root
    smooth(torso)

    # Peitoral dourado
    bpy.ops.mesh.primitive_torus_add(major_radius=0.72, minor_radius=0.13, location=(0, 0.1, 3.1))
    peit = bpy.context.active_object
    peit.rotation_euler = (math.radians(80), 0, 0)
    peit.scale = (1.0, 0.7, 1.0)
    peit.data.materials.append(m_ouro)
    peit.parent = root
    smooth(peit)

    # Broche
    bpy.ops.mesh.primitive_uv_sphere_add(radius=0.14, location=(0, -0.55, 3.15))
    br = bpy.context.active_object
    br.data.materials.append(m_joia)
    br.parent = root

    # Bracadeiras
    for s in [-1, 1]:
        bpy.ops.mesh.primitive_cylinder_add(radius=0.24, depth=0.38, location=(s*0.9, 0.1, 2.65))
        brac = bpy.context.active_object
        brac.rotation_euler = (0, math.radians(s*20), 0)
        brac.data.materials.append(m_ouro)
        brac.parent = root
        smooth(brac)

    # === CABECA ===
    bpy.ops.mesh.primitive_uv_sphere_add(segments=28, ring_count=24, radius=0.44, location=(0, -0.05, 4.05))
    cabeca = bpy.context.active_object
    cabeca.data.materials.append(m_pele)
    cabeca.parent = root
    smooth(cabeca)

    # Tiara
    bpy.ops.mesh.primitive_cylinder_add(radius=0.47, depth=0.16, location=(0, -0.05, 4.32))
    tiara = bpy.context.active_object
    tiara.data.materials.append(m_ouro)
    tiara.parent = root
    smooth(tiara)

    # Chifre central da tiara
    bpy.ops.mesh.primitive_cone_add(vertices=8, radius1=0.08, depth=0.55, location=(0, -0.05, 4.72))
    chifre_c = bpy.context.active_object
    chifre_c.data.materials.append(m_ouro)
    chifre_c.parent = root
    smooth(chifre_c)

    # Gema frontal da tiara
    bpy.ops.mesh.primitive_uv_sphere_add(radius=0.09, location=(0, -0.5, 4.36))
    gem_t = bpy.context.active_object
    gem_t.data.materials.append(m_joia)
    gem_t.parent = root

    # Olhos
    for s in [-1, 1]:
        bpy.ops.mesh.primitive_uv_sphere_add(radius=0.07, location=(s*0.18, -0.44, 4.1))
        olho = bpy.context.active_object
        olho.data.materials.append(m_olho)
        olho.parent = root

    # Barba em aneis
    for i in range(5):
        z = 3.62 - i*0.2
        r = 0.30 - i*0.04
        bpy.ops.mesh.primitive_torus_add(major_radius=r, minor_radius=0.07, location=(0, -0.3+i*0.03, z))
        ab = bpy.context.active_object
        ab.rotation_euler = (math.radians(80), 0, 0)
        ab.data.materials.append(m_barba)
        ab.parent = root
        smooth(ab)

    # === CAUDA SERPENTINA (desce de z=2.1 ate z=-2.5) ===
    # BUGFIX 2026-08-16: a lista original tinha os ângulos rx escolhidos um
    # a um à mão (0,20,35,20,-10,-30,-45) — o salto 20->35->20 é um "kink"
    # visual real, confirmado em render + crop ampliado (Rafa). Posição/
    # raio/profundidade continuam os mesmos (já aprovados, controlam a
    # silhueta) — só rx/rz agora vêm de uma curva suave (smoothstep),
    # âncorada nos mesmos ângulos inicial/final da lista original.
    cauda_base = [
        # (loc_x, loc_y, loc_z, radius1, depth, rot_x, rot_z)
        (0,    0,  1.9,  0.58, 0.9,  math.radians(0),   0),
        (0,    0.2, 1.05, 0.52, 0.9,  math.radians(20),  0),
        (0.1,  0.45, 0.2, 0.44, 0.9,  math.radians(35),  math.radians(8)),
        (0.15, 0.65,-0.6, 0.35, 0.85, math.radians(20),  math.radians(5)),
        (0.1,  0.7,-1.35, 0.26, 0.8,  math.radians(-10), math.radians(-5)),
        (0,    0.55,-2.05, 0.18, 0.75, math.radians(-30), math.radians(-8)),
        (-0.1, 0.3,-2.65, 0.12, 0.65, math.radians(-45), math.radians(-12)),
    ]
    cauda = rebuild_chain_angles(cauda_base, rx_start_deg=0, rx_end_deg=-48,
                                  rz_start_deg=0, rz_end_deg=-12,
                                  rx_wobble_deg=3.0, rz_wobble_deg=2.0)
    cauda_segs = []  # cabeca->cauda, usado no ciclo de nado (onda anguiliforme)
    for lx, ly, lz, r1, dep, rx, rz in cauda:
        bpy.ops.mesh.primitive_cone_add(vertices=24, radius1=r1, radius2=r1*0.82, depth=dep, location=(lx, ly, lz))
        seg = bpy.context.active_object
        seg.rotation_euler = (rx, 0, rz)
        seg.data.materials.append(m_escama)
        seg.parent = root
        smooth(seg)
        cauda_segs.append(seg)

    # Barbatanas caudais
    for s in [-1, 1]:
        bpy.ops.mesh.primitive_cone_add(vertices=4, radius1=0.65, depth=1.4, location=(s*0.35, 0.1, -2.85))
        fin = bpy.context.active_object
        fin.scale = (0.06, 0.9, 1.1)
        fin.rotation_euler = (math.radians(-50), math.radians(s*30), math.radians(s*12))
        fin.data.materials.append(m_barbat)
        fin.parent = root
        smooth(fin)

    # Crista Dorsal
    for i, z in enumerate([2.5, 2.1, 1.7, 1.3, 0.9]):
        tam = 0.32 - i*0.04
        bpy.ops.mesh.primitive_cone_add(vertices=3, radius1=0.18, depth=tam*2.0, location=(0, -0.55+i*0.1, z))
        dors = bpy.context.active_object
        dors.scale = (0.06, 0.6, 1.0)
        dors.rotation_euler = (math.radians(-100), 0, 0)
        dors.data.materials.append(m_barbat)
        dors.parent = root
        smooth(dors)

    # === TRIDENTE (mao direita) ===
    tr_root = bpy.data.objects.new("Tridente", None)
    bpy.context.scene.collection.objects.link(tr_root)
    tr_root.parent = root
    tr_root.location = (0.9, -0.45, 2.9)
    tr_root.rotation_euler = (math.radians(-5), math.radians(15), math.radians(20))

    bpy.ops.mesh.primitive_cylinder_add(radius=0.04, depth=3.6, location=(0, 0, 0))
    haste = bpy.context.active_object
    haste.data.materials.append(m_ouro)
    haste.parent = tr_root
    smooth(haste)

    bpy.ops.mesh.primitive_torus_add(major_radius=0.22, minor_radius=0.055, location=(0, 0, 1.55))
    grd = bpy.context.active_object
    grd.data.materials.append(m_ouro)
    grd.parent = tr_root
    smooth(grd)

    bpy.ops.mesh.primitive_ico_sphere_add(radius=0.16, subdivisions=3, location=(0, 0, 1.6))
    orb = bpy.context.active_object
    orb.data.materials.append(m_agua)
    orb.parent = tr_root
    smooth(orb)

    for px, pz, dep in [(0,2.3,0.85), (-0.3,2.1,0.7), (0.3,2.1,0.7)]:
        bpy.ops.mesh.primitive_cone_add(vertices=6, radius1=0.055, depth=dep, location=(px, 0, pz))
        pt = bpy.context.active_object
        pt.data.materials.append(m_ouro)
        pt.parent = tr_root
        smooth(pt)

    # Bolhas decorativas
    for bx, by, bz, br in [(0.7,-0.8,4.5,0.22),(1.0,-1.1,4.9,0.16),(0.4,-0.6,5.1,0.12),(0.85,-1.4,5.3,0.28)]:
        bpy.ops.mesh.primitive_uv_sphere_add(segments=16, ring_count=12, radius=br, location=(bx, by, bz))
        bl = bpy.context.active_object
        bl.data.materials.append(m_agua)
        bl.parent = root
        smooth(bl)

    # === CAMERA (posicao fixa calculada para capturar z=-3.5 ate z=5.2) ===
    # Centro vertical ~ z=0.85, distancia lateral para ver tudo
    bpy.ops.object.camera_add(location=(0, -8.5, 1.0))
    cam = bpy.context.active_object
    cam.rotation_euler = (math.radians(90), 0, 0)
    cam.data.lens = 40
    bpy.context.scene.camera = cam

    # Iluminacao
    bpy.ops.object.light_add(type='AREA', location=(2.5, -5.0, 5.0))
    kl = bpy.context.active_object
    kl.data.energy = 700
    kl.data.size = 3.5
    kl.data.color = (0.55, 0.92, 1.0)
    kl.rotation_euler = (math.radians(40), 0, math.radians(-30))

    bpy.ops.object.light_add(type='AREA', location=(-3.0, 4.5, 3.5))
    rl = bpy.context.active_object
    rl.data.energy = 900
    rl.data.size = 4.0
    rl.data.color = (1.0, 0.78, 0.35)
    rl.rotation_euler = (math.radians(-45), 0, math.radians(150))

    bpy.ops.object.light_add(type='POINT', location=(-1.5, -3.0, 1.0))
    fl = bpy.context.active_object
    fl.data.energy = 280
    fl.data.color = (0.1, 0.3, 0.8)

    # === RIM LIGHT — silhueta vencendo o fundo (auditoria 2026-08-15) ===
    # Aplica so nas superficies grandes (escama, pele, ouro, barbatana);
    # olho/joia/agua ja sao emissivos e broche/gemas sao pequenos demais
    # pra importar pro contorno.
    for mat in (m_escama, m_pele, m_ouro, m_barbat):
        add_fresnel_rim(mat, HERO_RIM, power=1.5, strength=6.0)

    # === CICLO DE NADO REAL (onda anguiliforme + bob de flutuacao) ===
    # Sem isso, os 6-8 frames renderizados eram identicos (verificado
    # por diff de pixel) — o motor Java so movia/rodava o sprite
    # INTEIRO, nunca mudava a pose da cauda.
    swim_cycle_keyframes(root, cauda_segs, frame_count=8, amp_deg=14.0,
                          wavelength_segs=3.0, cycles=1.0, bob_amp=0.05,
                          root_sway_deg=2.5)

    return {
        "root": root, "torso": torso, "cabeca": cabeca, "tr_root": tr_root,
        "cauda_segs": cauda_segs,
        "materiais": {"escama": m_escama, "pele": m_pele, "barba": m_barba,
                      "ouro": m_ouro, "barbat": m_barbat, "olho": m_olho,
                      "joia": m_joia, "agua": m_agua},
    }

def salvar_render(nome):
    base = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    bdir = os.path.join(base, "blends", "01_adapa_heroi")
    rdir = os.path.join(base, "renders")
    os.makedirs(bdir, exist_ok=True)
    os.makedirs(rdir, exist_ok=True)
    try:
        bpy.ops.wm.save_as_mainfile(filepath=os.path.join(bdir, nome+".blend"))
        print("Blend salvo: " + nome)
    except Exception as e:
        print("Aviso blend: " + str(e))
    bpy.context.scene.render.filepath = os.path.join(rdir, nome+".png")
    bpy.ops.render.render(write_still=True)
    print("Render salvo: " + nome)

if __name__ == "__main__":
    construir_adapa()
    salvar_render("01_adapa_heroi")
    print("OK 01_adapa_heroi v6.0")
