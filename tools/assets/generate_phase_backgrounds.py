"""Render five stylized, low-poly 3D underwater panoramas for runtime parallax."""
import bpy
import math
import os
import random
import sys
from mathutils import Vector


def project_root():
    args = sys.argv[sys.argv.index("--") + 1:] if "--" in sys.argv else []
    return args[0] if args else os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))


ROOT = project_root()
OUTPUT = os.path.join(ROOT, "src", "main", "resources", "backgrounds")
os.makedirs(OUTPUT, exist_ok=True)
WIDTH, HEIGHT = 3072, 768


def clear_scene():
    bpy.ops.object.select_all(action="SELECT")
    bpy.ops.object.delete(use_global=False)
    for datablocks in (bpy.data.materials, bpy.data.curves, bpy.data.meshes, bpy.data.cameras, bpy.data.lights):
        for block in list(datablocks):
            if block.users == 0:
                datablocks.remove(block)


def mat(name, color, emission=0.0, metallic=0.0, roughness=0.8):
    m = bpy.data.materials.new(name)
    m.diffuse_color = (*color, 1)
    m.use_nodes = True
    bsdf = m.node_tree.nodes.get("Principled BSDF")
    bsdf.inputs["Base Color"].default_value = (*color, 1)
    bsdf.inputs["Metallic"].default_value = metallic
    bsdf.inputs["Roughness"].default_value = roughness
    if "Emission Color" in bsdf.inputs:
        bsdf.inputs["Emission Color"].default_value = (*color, 1)
        bsdf.inputs["Emission Strength"].default_value = emission
    return m


def assign(obj, material):
    obj.data.materials.append(material)
    return obj


def ico(name, loc, scale, material, subdivisions=1):
    bpy.ops.mesh.primitive_ico_sphere_add(subdivisions=subdivisions, radius=1, location=loc)
    obj = bpy.context.object
    obj.name = name
    obj.scale = scale
    return assign(obj, material)


def rod(name, a, b, radius, material, vertices=6):
    a, b = Vector(a), Vector(b)
    delta = b - a
    bpy.ops.mesh.primitive_cone_add(vertices=vertices, radius1=radius, radius2=radius * 0.72,
                                    depth=delta.length, location=(a + b) / 2)
    obj = bpy.context.object
    obj.name = name
    obj.rotation_mode = "QUATERNION"
    obj.rotation_quaternion = delta.to_track_quat("Z", "Y")
    return assign(obj, material)


def rock_cluster(x, z, width, height, material, count=5, seed=0, y=0):
    rng = random.Random(seed)
    for i in range(count):
        sx = width * rng.uniform(.12, .29)
        sz = height * rng.uniform(.25, .66)
        ico("low_poly_reef_stone", (x + rng.uniform(-width*.48, width*.48), y,
                                     z + rng.uniform(-height*.05, height*.35)),
            (sx, rng.uniform(35, 80), sz), material, 1)


def coral(x, z, scale, trunk, tips, seed=0, y=-15):
    rng = random.Random(seed)
    points = [(x, z)]
    for branch in range(3):
        direction = -1 if branch == 0 else (1 if branch == 1 else rng.choice((-1, 1)))
        px, pz = x + direction * scale * rng.uniform(.15, .4), z + scale * rng.uniform(.38, .62)
        rod("coral_branch", (x, y, z), (px, y, pz), scale * .09, trunk, 5)
        points.append((px, pz))
        qx, qz = px + direction * scale * rng.uniform(.2, .45), pz + scale * rng.uniform(.22, .4)
        rod("coral_fork", (px, y, pz), (qx, y, qz), scale * .06, trunk, 5)
        points.append((qx, qz))
    for px, pz in points[1:]:
        ico("coral_bioluminescent_tip", (px, y - 2, pz), (scale*.07, scale*.055, scale*.09), tips, 1)


def pillar(x, z, height, stone, cap=None, y=0):
    bpy.ops.mesh.primitive_cylinder_add(vertices=7, radius=27, depth=height, location=(x, y, z + height/2))
    bpy.context.object.name = "broken_atlantean_column"
    assign(bpy.context.object, stone)
    for offset in (12, height * .84):
        bpy.ops.mesh.primitive_cylinder_add(vertices=7, radius=34, depth=13,
                                            location=(x, y, z + offset))
        bpy.context.object.name = "column_carved_ring"
        assign(bpy.context.object, cap or stone)
    if height > 120:
        ico("column_fragment", (x + 8, y, z + height + 22), (30, 35, 24), stone, 1)


def fish_school(x, z, count, material, seed=0, y=-50):
    rng = random.Random(seed)
    for i in range(count):
        px = x + rng.uniform(-220, 220)
        pz = z + rng.uniform(-48, 48)
        size = rng.uniform(10, 22)
        ico("distant_fish_silhouette", (px, y, pz), (size, 5, size*.42), material, 1)


def seabed(material, accent, seed):
    rng = random.Random(seed)
    verts, faces = [], []
    segments = 90
    for i in range(segments + 1):
        x = -1700 + i * 3400 / segments
        ridge = -305 + math.sin(i*.26 + seed) * 28 + math.sin(i*.073) * 26
        verts.extend(((x, 10, -410), (x, 10, ridge)))
        if i:
            a = (i-1) * 2
            faces.append((a, a+2, a+3, a+1))
    mesh = bpy.data.meshes.new("sculpted_seafloor_mesh")
    mesh.from_pydata(verts, [], faces)
    obj = bpy.data.objects.new("sculpted_seafloor", mesh)
    bpy.context.collection.objects.link(obj)
    assign(obj, material)
    for _ in range(100):
        x = rng.uniform(-1700, 1700)
        z = rng.uniform(-380, -300)
        ico("seafloor_scattered_stone", (x, rng.uniform(-5, 9), z),
            (rng.uniform(8, 34), rng.uniform(9, 28), rng.uniform(4, 14)), accent, 1)


def backdrop(top, middle, bottom):
    mesh = bpy.data.meshes.new("vertical_gradient_backdrop_mesh")
    mesh.from_pydata([(-1900, 180, -500), (1900, 180, -500),
                      (1900, 180, 500), (-1900, 180, 500)], [], [(0, 1, 2, 3)])
    plane = bpy.data.objects.new("painted_ocean_depth_backdrop", mesh)
    bpy.context.collection.objects.link(plane)
    material = bpy.data.materials.new("depth_gradient")
    material.use_nodes = True
    nodes, links = material.node_tree.nodes, material.node_tree.links
    nodes.clear()
    tex = nodes.new("ShaderNodeTexCoord")
    sep = nodes.new("ShaderNodeSeparateXYZ")
    ramp = nodes.new("ShaderNodeValToRGB")
    ramp.color_ramp.elements[0].position = 0.0
    ramp.color_ramp.elements[0].color = (*bottom, 1)
    ramp.color_ramp.elements[1].position = 1.0
    ramp.color_ramp.elements[1].color = (*top, 1)
    mid = ramp.color_ramp.elements.new(.55)
    mid.color = (*middle, 1)
    emission = nodes.new("ShaderNodeEmission")
    output = nodes.new("ShaderNodeOutputMaterial")
    links.new(tex.outputs["Generated"], sep.inputs["Vector"])
    links.new(sep.outputs["Z"], ramp.inputs["Fac"])
    links.new(ramp.outputs["Color"], emission.inputs["Color"])
    links.new(emission.outputs["Emission"], output.inputs["Surface"])
    assign(plane, material)


def setup_camera_and_render():
    bpy.ops.object.camera_add(location=(0, -1200, 0))
    camera = bpy.context.object
    camera.rotation_euler = (math.radians(90), 0, 0)
    camera.data.type = "ORTHO"
    camera.data.ortho_scale = WIDTH
    camera.data.clip_start = .1
    camera.data.clip_end = 5000
    bpy.context.scene.camera = camera
    bpy.context.scene.render.engine = "BLENDER_EEVEE_NEXT"
    scene = bpy.context.scene
    scene.eevee.taa_render_samples = 16
    scene.render.resolution_x, scene.render.resolution_y = WIDTH, HEIGHT
    scene.render.resolution_percentage = 100
    scene.render.image_settings.file_format = "PNG"
    scene.render.image_settings.color_mode = "RGB"
    scene.render.film_transparent = False
    scene.render.filepath = os.path.join(OUTPUT, "phase.png")
    scene.render.image_settings.compression = 20
    scene.view_settings.view_transform = "Standard"
    scene.view_settings.look = "Medium High Contrast"
    scene.view_settings.exposure = 0
    scene.view_settings.gamma = 1
    scene.world.color = (.004, .008, .02)
    if scene.world.use_nodes:
        scene.world.node_tree.nodes.get("Background").inputs["Color"].default_value = (.004, .008, .02, 1)
    scene.render.image_settings.color_depth = "8"
    scene.camera.data.lens = 50
    scene.render.engine = "BLENDER_EEVEE_NEXT"
    scene.render.resolution_percentage = 100
    scene.render.filepath = ""
    bpy.ops.object.light_add(type="AREA", location=(-600, -450, 550))
    key = bpy.context.object
    key.data.energy = 2800
    key.data.shape = "DISK"
    key.data.size = 900
    key.rotation_euler = (math.radians(28), 0, math.radians(-22))
    bpy.ops.object.light_add(type="AREA", location=(900, -320, 150))
    fill = bpy.context.object
    fill.data.energy = 950
    fill.data.color = (.25, .65, 1.0)
    fill.data.size = 700
    fill.rotation_euler = (math.radians(28), 0, math.radians(28))
    scene.render.filepath = os.path.join(OUTPUT, "phase.png")


def stage(phase):
    clear_scene()
    random.seed(9200 + phase)
    # Ambient scenery uses restrained values; interactive entities retain their
    # saturated runtime materials and occupy the strongest color contrast.
    palettes = {
        1: ((.025,.12,.19),(.02,.27,.34),(.015,.09,.14),(.12,.34,.35),(.09,.30,.27),(.06,.36,.36),(.24,.44,.38)),
        2: ((.014,.065,.13),(.045,.18,.27),(.01,.045,.10),(.09,.23,.28),(.13,.19,.24),(.20,.33,.30),(.24,.33,.32)),
        3: ((.009,.025,.075),(.035,.08,.16),(.008,.025,.065),(.055,.13,.19),(.10,.13,.21),(.16,.27,.31),(.19,.25,.31)),
        4: ((.035,.025,.07),(.12,.045,.09),(.025,.025,.07),(.16,.075,.09),(.17,.12,.16),(.55,.14,.035),(.24,.17,.17)),
        5: ((.018,.025,.09),(.055,.08,.18),(.012,.02,.065),(.08,.12,.22),(.13,.14,.20),(.13,.37,.45),(.19,.21,.29)),
    }
    top, mid, bottom, farc, rockc, glowc, floorc = palettes[phase]
    backdrop(top, mid, bottom)
    far = mat("distant_silhouette", farc, .08)
    rock = mat("ambient_rock_10_percent", rockc, .03)
    floor = mat("muted_seafloor", floorc, .025)
    glow = mat("interactable_accent_30_percent", glowc, .55, .05, .45)
    coralmat = mat("muted_coral", tuple(min(.85, c*1.16) for c in rockc), .06)
    seabed(floor, rock, phase * 31)

    # Distant ledges frame the play area without competing with the player.
    for side in (-1, 1):
        rock_cluster(side*1510, -75, 390, 470, far, 8, phase*5+side, 65)
    if phase in (1, 2):
        for i, x in enumerate((-1300,-890,-420,280,760,1190)):
            coral(x, -275 + (i%2)*15, 120 + (i%3)*18, coralmat, glow if i%3 == 1 else far,
                  phase*100+i, -30 - (i%2)*35)
            rock_cluster(x+100, -290, 230, 165, rock, 4, phase*50+i, 22)
        fish_school(-900, 185, 12, glow if phase == 1 else far, phase+4, -80)
        fish_school(600, 65, 10, far, phase+12, -100)
    elif phase == 3:
        # Deep trench: sparse silhouettes and broken columns establish scale.
        for i, x in enumerate((-1240,-650,20,690,1270)):
            pillar(x, -295, 170 + (i%3)*70, far, rock, 24)
        for i, x in enumerate((-1120,-320,550,1120)):
            rod("slow_abyss_current_ribbon", (x,-100,-130), (x+260,-100,80), 5, glow if i==1 else far, 5)
        fish_school(-950, 185, 9, far, phase+5, -100)
    elif phase == 4:
        ember = mat("distant_lava_veins", (.78,.13,.025), 1.0)
        for i, x in enumerate((-1280,-760,-150,470,1050,1430)):
            rock_cluster(x, -280, 350, 280 + (i%2)*80, far, 7, phase*20+i, 35)
            # Irregular glowing fissures sit inside the rock silhouettes.
            for j in range(3):
                xx = x + (j-1)*38
                rod("thin_lava_seam", (xx, -5, -330), (xx+random.uniform(-35,35), -5, -190),
                    random.uniform(3,6), ember, 5)
        for x in (-1000, 430, 1270):
            bpy.ops.mesh.primitive_cone_add(vertices=9, radius1=130, radius2=25, depth=260,
                                            location=(x, 15, -220))
            bpy.context.object.name = "subsea_volcanic_vent"
            assign(bpy.context.object, far)
            ico("vent_glow", (x, -4, -92), (34, 18, 19), ember, 1)
        fish_school(-850, 130, 8, far, phase+11, -100)
    else:
        for i, x in enumerate((-1330,-1010,-550,-40,520,1000,1370)):
            pillar(x, -300, 190 + ((i*3)%4)*45, far, rock, 40)
            if i in (1,4):
                rod("temple_arch", (x,-12,-80), (x+170,-12,-80), 18, rock, 7)
        # The sealed gate glows as a single restrained focal cue in the distance.
        for radius in (100, 79, 58):
            bpy.ops.mesh.primitive_torus_add(major_radius=radius, minor_radius=6,
                                             major_segments=32, minor_segments=6,
                                             location=(860,-65,-90), rotation=(math.pi/2,0,0))
            assign(bpy.context.object, glow if radius == 100 else far)
        fish_school(-970, 175, 8, far, phase+17, -100)

    # Sparse vertical kelp/bubble forms give a 2.5D sense of depth.
    rng = random.Random(phase*119)
    for i in range(17):
        x = rng.uniform(-1450, 1450)
        z = rng.uniform(-250, 170)
        if phase <= 2 and i % 3 == 0:
            rod("distant_kelp", (x,45,-300), (x+rng.uniform(-75,75),45,z),
                rng.uniform(3,7), far, 5)
        if i % 4 == 0:
            ico("small_background_bubble", (x,-75,z), (8,6,12), glow if phase == 1 else far, 1)

    setup_camera_and_render()
    path = os.path.join(OUTPUT, f"phase-{phase}.png")
    bpy.context.scene.render.filepath = path
    bpy.ops.render.render(write_still=True)
    print(f"[BG 3D] Fase {phase}: {path}")


for phase in range(1, 6):
    stage(phase)
print("[BG 3D] Panoramas 3D renderizados.")
