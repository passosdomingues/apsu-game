package br.apsu.graphics;

import javafx.scene.Group;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.shape.VertexFormat;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.image.Image;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;

/** Carrega OBJ/MTL exportados pelo pipeline Blender para a cena JavaFX 3D. */
final class ObjModelLoader {
    @FunctionalInterface
    interface TextureBinder {
        void bind(PhongMaterial material, String resourcePath) throws IOException;
    }

    record Model(Group node, double width, double height) {}
    private record Face(int a, int b, int c, int na, int nb, int nc, int ta, int tb, int tc) {}
    private record MaterialAppearance(Color diffuse, String texture, Color specular, double shininess) {}
    private static final Map<String, Image> TEXTURES = new ConcurrentHashMap<>();
    private static final class MaterialFaces {
        final List<Face> faces = new ArrayList<>();
    }

    private ObjModelLoader() {}

    static Model load(String modelName, double targetWidth, double targetHeight) throws IOException {
        return load(modelName, targetWidth, targetHeight, ObjModelLoader::bindTexture);
    }

    static Model load(String modelName, double targetWidth, double targetHeight,
                      TextureBinder textureBinder) throws IOException {
        String resourceKey;
        if (modelName.startsWith("scenery/")) {
            String name = modelName.substring("scenery/".length());
            resourceKey = "scenery/" + name + "/" + name;
        } else if (modelName.startsWith("characters/")) {
            String name = modelName.substring("characters/".length());
            resourceKey = "characters/" + name + "/" + name;
        } else {
            resourceKey = modelName.contains("/") ? modelName : modelName + "/" + modelName;
        }
        String base = "/models/";
        if (resourceKey.startsWith("scenery/") || resourceKey.startsWith("characters/")) {
            base += resourceKey;
        } else {
            base += "characters/" + resourceKey;
        }
        List<double[]> vertices = new ArrayList<>();
        List<double[]> normals = new ArrayList<>();
        List<double[]> texCoords = new ArrayList<>();
        Map<String, MaterialFaces> facesByMaterial = new HashMap<>();
        Map<String, MaterialAppearance> materials = loadMaterials(base + ".mtl");
        String activeMaterial = "default";
        MaterialFaces active = facesByMaterial.computeIfAbsent(activeMaterial, ignored -> new MaterialFaces());
        try (InputStream stream = resource(base + ".obj");
             BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("v ")) {
                    StringTokenizer p = new StringTokenizer(line);
                    p.nextToken();
                    vertices.add(new double[]{Double.parseDouble(p.nextToken()), Double.parseDouble(p.nextToken()), Double.parseDouble(p.nextToken())});
                } else if (line.startsWith("vn ")) {
                    StringTokenizer p = new StringTokenizer(line);
                    p.nextToken();
                    normals.add(new double[]{Double.parseDouble(p.nextToken()), Double.parseDouble(p.nextToken()), Double.parseDouble(p.nextToken())});
                } else if (line.startsWith("vt ")) {
                    StringTokenizer p = new StringTokenizer(line);
                    p.nextToken();
                    texCoords.add(new double[]{Double.parseDouble(p.nextToken()), Double.parseDouble(p.nextToken())});
                } else if (line.startsWith("usemtl ")) {
                    activeMaterial = line.substring(7).trim();
                    active = facesByMaterial.computeIfAbsent(activeMaterial, ignored -> new MaterialFaces());
                } else if (line.startsWith("f ")) {
                    StringTokenizer p = new StringTokenizer(line);
                    p.nextToken();
                    String firstPoint = p.nextToken();
                    String previousPoint = p.nextToken();
                    int first = vertexIndex(firstPoint, vertices.size());
                    int previous = vertexIndex(previousPoint, vertices.size());
                    while (p.hasMoreTokens()) {
                        String nextPoint = p.nextToken();
                        int next = vertexIndex(nextPoint, vertices.size());
                        active.faces.add(new Face(first, previous, next,
                            normalIndex(firstPoint, normals.size()), normalIndex(previousPoint, normals.size()), normalIndex(nextPoint, normals.size()),
                            texCoordIndex(firstPoint, texCoords.size()), texCoordIndex(previousPoint, texCoords.size()), texCoordIndex(nextPoint, texCoords.size())));
                        previousPoint = nextPoint;
                        previous = next;
                    }
                }
            }
        }
        if (vertices.isEmpty()) throw new IOException("OBJ sem vértices: " + modelName);

        double minX = Double.POSITIVE_INFINITY, maxX = Double.NEGATIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY, maxZ = Double.NEGATIVE_INFINITY;
        for (double[] v : vertices) {
            minX = Math.min(minX, v[0]); maxX = Math.max(maxX, v[0]);
            minY = Math.min(minY, v[1]); maxY = Math.max(maxY, v[1]);
            minZ = Math.min(minZ, v[2]); maxZ = Math.max(maxZ, v[2]);
        }
        double modelW = Math.max(0.001, maxX - minX), modelH = Math.max(0.001, maxZ - minZ);
        double scale = Math.min(targetWidth / modelW, targetHeight / modelH);
        Group result = new Group();
        for (Map.Entry<String, MaterialFaces> entry : facesByMaterial.entrySet()) {
            if (entry.getValue().faces.isEmpty()) continue;
            TriangleMesh mesh = new TriangleMesh(VertexFormat.POINT_NORMAL_TEXCOORD);
            Map<Integer, Integer> pointIndices = new HashMap<>();
            Map<Integer, Integer> normalIndices = new HashMap<>();
            if (texCoords.isEmpty()) mesh.getTexCoords().addAll(0, 0);
            else for (double[] uv : texCoords) mesh.getTexCoords().addAll((float) uv[0], (float) (1.0 - uv[1]));
            int[] triangles = new int[entry.getValue().faces.size() * 9];
            int out = 0;
            int faceIndex = 0;
            for (Face face : entry.getValue().faces) {
                double[] faceNormal = transformedFaceNormal(vertices.get(face.a()), vertices.get(face.b()), vertices.get(face.c()));
                for (int i = 0; i < 3; i++) {
                    int index = i == 0 ? face.a() : i == 1 ? face.b() : face.c();
                    int sourceNormalId = i == 0 ? face.na() : i == 1 ? face.nb() : face.nc();
                    int uvId = i == 0 ? face.ta() : i == 1 ? face.tb() : face.tc();
                    Integer pointIndex = pointIndices.get(index);
                    if (pointIndex == null) {
                        double[] v = vertices.get(index);
                        mesh.getPoints().addAll(
                            (float) ((v[0] - (minX + maxX) * 0.5) * scale),
                            (float) (-(v[2] - (minZ + maxZ) * 0.5) * scale),
                            // Center Blender's depth axis too. Leaving the
                            // source object origin here creates large,
                            // model-specific parallax in the perspective view
                            // (the shipwreck was displaced hundreds of pixels).
                            (float) ((v[1] - (minY + maxY) * 0.5) * scale));
                        pointIndex = mesh.getPoints().size() / 3 - 1;
                        pointIndices.put(index, pointIndex);
                    }
                    int vertexOut = pointIndex;
                    int sourceNormal = sourceNormalId >= 0 ? sourceNormalId : -faceIndex - 1;
                    Integer mappedNormal = normalIndices.get(sourceNormal);
                    if (mappedNormal == null) {
                        double[] n = sourceNormalId >= 0 ? transformNormal(normals.get(sourceNormalId)) : faceNormal;
                        mesh.getNormals().addAll((float) n[0], (float) n[1], (float) n[2]);
                        mappedNormal = mesh.getNormals().size() / 3 - 1;
                        normalIndices.put(sourceNormal, mappedNormal);
                    }
                    int normalOut = mappedNormal;
                    triangles[out++] = vertexOut;
                    triangles[out++] = normalOut;
                    triangles[out++] = uvId >= 0 ? uvId : 0;
                }
                faceIndex++;
            }
            mesh.getFaces().addAll(triangles);
            MeshView view = new MeshView(mesh);
            MaterialAppearance appearance = materials.getOrDefault(entry.getKey(),
                new MaterialAppearance(Color.LIGHTGRAY, null, Color.color(0.15, 0.15, 0.15), 24));
            PhongMaterial material = new PhongMaterial(appearance.diffuse());
            material.setSpecularColor(appearance.specular());
            material.setSpecularPower(appearance.shininess());
            if (appearance.texture() != null) {
                textureBinder.bind(material, appearance.texture());
            }
            view.setMaterial(material);
            result.getChildren().add(view);
        }
        return new Model(result, modelW * scale, modelH * scale);
    }

    /** Creates an inexpensive instance that shares immutable mesh and material data. */
    static Model instance(Model template, double targetWidth, double targetHeight) {
        return instance(template, targetWidth, targetHeight, Color.WHITE);
    }

    /** Creates a tinted instance while sharing the source texture maps and meshes. */
    static Model instance(Model template, double targetWidth, double targetHeight, Color tint) {
        double scale = Math.min(targetWidth / template.width(), targetHeight / template.height());
        Group node = new Group();
        for (javafx.scene.Node child : template.node().getChildren()) {
            MeshView source = (MeshView) child;
            MeshView view = new MeshView(source.getMesh());
            PhongMaterial sourceMaterial = (PhongMaterial) source.getMaterial();
            PhongMaterial material = new PhongMaterial(multiply(sourceMaterial.getDiffuseColor(), tint));
            material.setDiffuseMap(sourceMaterial.getDiffuseMap());
            material.setSpecularColor(sourceMaterial.getSpecularColor());
            material.setSpecularPower(sourceMaterial.getSpecularPower());
            material.setSelfIlluminationMap(sourceMaterial.getSelfIlluminationMap());
            view.setMaterial(material);
            node.getChildren().add(view);
        }
        node.setScaleX(scale);
        node.setScaleY(scale);
        node.setScaleZ(scale);
        return new Model(node, template.width() * scale, template.height() * scale);
    }

    private static int vertexIndex(String facePoint, int vertexCount) {
        int slash = facePoint.indexOf('/');
        return parseIndex(facePoint, 0, slash < 0 ? facePoint.length() : slash, vertexCount);
    }

    private static int normalIndex(String facePoint, int normalCount) {
        int firstSlash = facePoint.indexOf('/');
        if (firstSlash < 0) return -1;
        int secondSlash = facePoint.indexOf('/', firstSlash + 1);
        if (secondSlash < 0 || secondSlash + 1 == facePoint.length()) return -1;
        return parseIndex(facePoint, secondSlash + 1, facePoint.length(), normalCount);
    }

    private static int texCoordIndex(String facePoint, int uvCount) {
        int firstSlash = facePoint.indexOf('/');
        if (firstSlash < 0) return -1;
        int secondSlash = facePoint.indexOf('/', firstSlash + 1);
        int end = secondSlash < 0 ? facePoint.length() : secondSlash;
        if (firstSlash + 1 == end) return -1;
        return parseIndex(facePoint, firstSlash + 1, end, uvCount);
    }

    private static int parseIndex(String token, int start, int end, int itemCount) {
        boolean negative = token.charAt(start) == '-';
        int value = 0;
        for (int i = negative ? start + 1 : start; i < end; i++) {
            value = value * 10 + token.charAt(i) - '0';
        }
        return negative ? itemCount - value : value - 1;
    }

    private static double[] transformNormal(double[] source) {
        return normalize(new double[]{source[0], -source[2], source[1]});
    }

    private static double[] transformedFaceNormal(double[] a, double[] b, double[] c) {
        double[] p = {a[0], -a[2], a[1]};
        double[] q = {b[0], -b[2], b[1]};
        double[] r = {c[0], -c[2], c[1]};
        double[] u = {q[0] - p[0], q[1] - p[1], q[2] - p[2]};
        double[] v = {r[0] - p[0], r[1] - p[1], r[2] - p[2]};
        return normalize(new double[]{u[1] * v[2] - u[2] * v[1], u[2] * v[0] - u[0] * v[2], u[0] * v[1] - u[1] * v[0]});
    }

    private static double[] normalize(double[] v) {
        double length = Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
        if (length < 1e-10) return new double[]{0, 0, -1};
        return new double[]{v[0] / length, v[1] / length, v[2] / length};
    }

    private static Map<String, MaterialAppearance> loadMaterials(String path) throws IOException {
        Map<String, MaterialAppearance> result = new HashMap<>();
        try (InputStream stream = resource(path);
             BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String material = "default", line, texture = null;
            Color diffuse = Color.LIGHTGRAY, specular = Color.color(0.15, 0.15, 0.15);
            double shininess = 24;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("newmtl ")) {
                    if (!material.equals("default")) result.put(material,
                        new MaterialAppearance(diffuse, texture, specular, shininess));
                    material = line.substring(7).trim();
                    diffuse = Color.LIGHTGRAY;
                    specular = Color.color(0.15, 0.15, 0.15);
                    shininess = 24;
                    texture = null;
                } else if (line.startsWith("Kd ")) {
                    String[] p = line.trim().split("\\s+");
                    diffuse = Color.color(clamp(Double.parseDouble(p[1])), clamp(Double.parseDouble(p[2])), clamp(Double.parseDouble(p[3])));
                } else if (line.startsWith("Ks ")) {
                    String[] p = line.trim().split("\\s+");
                    specular = Color.color(clamp(Double.parseDouble(p[1])), clamp(Double.parseDouble(p[2])), clamp(Double.parseDouble(p[3])));
                } else if (line.startsWith("Ns ")) {
                    shininess = Math.max(1, Math.min(128, Double.parseDouble(line.substring(3).trim())));
                } else if (line.startsWith("map_Kd ")) {
                    String file = line.substring(7).trim();
                    String directory = path.substring(0, path.lastIndexOf('/') + 1);
                    texture = normalizeResource(directory + file);
                }
            }
            result.put(material, new MaterialAppearance(diffuse, texture, specular, shininess));
        }
        return result;
    }

    private static String normalizeResource(String path) {
        java.util.ArrayDeque<String> components = new java.util.ArrayDeque<>();
        for (String part : path.split("/")) {
            if (part.isEmpty() || part.equals(".")) continue;
            if (part.equals("..")) { if (!components.isEmpty()) components.removeLast(); }
            else components.addLast(part);
        }
        return "/" + String.join("/", components);
    }

    private static double clamp(double c) { return Math.max(0, Math.min(1, c)); }

    private static Color multiply(Color source, Color tint) {
        return Color.color(clamp(source.getRed() * tint.getRed()),
            clamp(source.getGreen() * tint.getGreen()),
            clamp(source.getBlue() * tint.getBlue()), source.getOpacity());
    }

    private static void bindTexture(PhongMaterial material, String resourcePath) throws IOException {
        var textureUrl = ObjModelLoader.class.getResource(resourcePath);
        if (textureUrl == null) throw new IOException("Textura MTL não encontrada: " + resourcePath);
        Image texture = TEXTURES.computeIfAbsent(textureUrl.toExternalForm(), key -> new Image(key, false));
        material.setDiffuseMap(texture);
    }

    private static InputStream resource(String path) throws IOException {
        InputStream stream = ObjModelLoader.class.getResourceAsStream(path);
        if (stream == null) throw new IOException("Recurso 3D não encontrado: " + path);
        return stream;
    }
}
