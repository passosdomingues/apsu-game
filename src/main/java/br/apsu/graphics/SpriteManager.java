package br.apsu.graphics;

import javafx.scene.image.Image;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Gerenciador e cache de imagens e sequências de frames.
 */
public class SpriteManager {
    private static final SpriteManager INSTANCE = new SpriteManager();
    private final Map<String, Image> imageCache = new HashMap<>();
    /** Frames imutáveis compartilhados por todas as entidades da mesma animação. */
    private final Map<String, List<Image>> sequenceCache = new HashMap<>();

    public record FrameBlend(Image current, Image next, double nextAlpha) {}

    public record FrameSeq(List<Image> frames, double fps) {
        public FrameSeq(double fps) { this(new ArrayList<>(), fps); }
        public Image get(double timeSeconds) {
            if (frames.isEmpty()) return null;
            return frames.get((int)(timeSeconds * fps) % frames.size());
        }
        /** Transição suave entre frames, evitando cortes visuais em sprites 3D. */
        public FrameBlend blend(double timeSeconds) {
            if (frames.isEmpty()) return new FrameBlend(null, null, 0);
            if (frames.size() == 1) return new FrameBlend(frames.getFirst(), null, 0);
            double position = timeSeconds * fps;
            int index = Math.floorMod((int) Math.floor(position), frames.size());
            double fraction = position - Math.floor(position);
            double smooth = fraction * fraction * (3.0 - 2.0 * fraction);
            return new FrameBlend(frames.get(index), frames.get((index + 1) % frames.size()), smooth);
        }
        public boolean isEmpty() { return frames.isEmpty(); }
    }

    private SpriteManager() {}

    public static SpriteManager getInstance() {
        return INSTANCE;
    }

    public Image getImage(String resourcePath) {
        if (resourcePath == null) return null;
        if (imageCache.containsKey(resourcePath)) {
            return imageCache.get(resourcePath);
        }

        Image img = null;
        try {
            InputStream is = getClass().getResourceAsStream("/" + resourcePath);
            if (is != null) {
                img = new Image(is);
            } else {
                File file = new File("src/main/resources/" + resourcePath);
                if (file.exists()) {
                    img = new Image(file.toURI().toString());
                }
            }
        } catch (Exception e) {
            System.out.println("[WARN] Falha ao carregar imagem: " + resourcePath);
        }

        // Registrar sempre a chave no cache (mesmo se img for null), evitando
        // tentativas repetidas de I/O em disco a cada frame para caminhos inexistentes.
        imageCache.put(resourcePath, img);
        return img;
    }

    public FrameSeq loadSequence(String dirPath, double fps, Image fallback) {
        if (dirPath == null) return fallback == null
            ? new FrameSeq(fps)
            : new FrameSeq(List.of(fallback), fps);

        List<Image> frames = sequenceCache.computeIfAbsent(dirPath, this::readSequence);
        if (frames.isEmpty() && fallback != null) {
            return new FrameSeq(List.of(fallback), fps);
        }
        return new FrameSeq(frames, fps);
    }

    private List<Image> readSequence(String dirPath) {
        List<Image> frames = new ArrayList<>();
        for (int i = 1; i <= 8; i++) {
            Image frame = getImage(dirPath + String.format("/frame_%03d.png", i));
            if (frame != null) frames.add(frame);
        }
        return List.copyOf(frames);
    }
}
