package br.apsu.core.spatial;

import java.util.ArrayList;
import java.util.List;

/**
 * QuadTree 2D para particionamento espacial de objetos com Bounding Box.
 * Otimiza a checagem de colisão e busca por vizinhos de O(N*M) para O(N log M).
 *
 * @param <T> Tipo de elemento armazenado na árvore.
 */
public class QuadTree<T> {

    /**
     * Provedor de coordenadas e dimensões para o elemento generico T.
     */
    public interface BoundingBoxProvider<T> {
        double getX(T item);
        double getY(T item);
        double getWidth(T item);
        double getHeight(T item);
    }

    /**
     * Representação imutável de limites retangulares.
     */
    public record Bounds(double x, double y, double width, double height) {
        public boolean intersects(Bounds other) {
            return !(x + width <= other.x || other.x + other.width <= x ||
                     y + height <= other.y || other.y + other.height <= y);
        }

        public boolean intersects(double qx, double qy, double qw, double qh) {
            return !(x + width <= qx || qx + qw <= x ||
                     y + height <= qy || qy + qh <= y);
        }
    }

    private static final int MAX_OBJECTS = 8;
    private static final int MAX_LEVELS = 6;

    private final int level;
    private final List<T> objects;
    private final Bounds bounds;
    private final QuadTree<T>[] nodes;
    private final BoundingBoxProvider<T> provider;

    @SuppressWarnings("unchecked")
    public QuadTree(int level, Bounds bounds, BoundingBoxProvider<T> provider) {
        this.level = level;
        this.bounds = bounds;
        this.provider = provider;
        this.objects = new ArrayList<>();
        this.nodes = new QuadTree[4];
    }

    public void clear() {
        objects.clear();
        for (int i = 0; i < nodes.length; i++) {
            if (nodes[i] != null) {
                nodes[i].clear();
                nodes[i] = null;
            }
        }
    }

    private void split() {
        double subWidth = bounds.width / 2.0;
        double subHeight = bounds.height / 2.0;
        double x = bounds.x;
        double y = bounds.y;

        nodes[0] = new QuadTree<>(level + 1, new Bounds(x + subWidth, y, subWidth, subHeight), provider); // NE
        nodes[1] = new QuadTree<>(level + 1, new Bounds(x, y, subWidth, subHeight), provider);            // NW
        nodes[2] = new QuadTree<>(level + 1, new Bounds(x, y + subHeight, subWidth, subHeight), provider); // SW
        nodes[3] = new QuadTree<>(level + 1, new Bounds(x + subWidth, y + subHeight, subWidth, subHeight), provider); // SE
    }

    private int getIndex(T item) {
        int index = -1;
        double midX = bounds.x + (bounds.width / 2.0);
        double midY = bounds.y + (bounds.height / 2.0);

        double ix = provider.getX(item);
        double iy = provider.getY(item);
        double iw = provider.getWidth(item);
        double ih = provider.getHeight(item);

        boolean topQuadrant = (iy < midY && iy + ih < midY);
        boolean bottomQuadrant = (iy > midY);

        if (ix < midX && ix + iw < midX) {
            if (topQuadrant) {
                index = 1; // NW
            } else if (bottomQuadrant) {
                index = 2; // SW
            }
        } else if (ix > midX) {
            if (topQuadrant) {
                index = 0; // NE
            } else if (bottomQuadrant) {
                index = 3; // SE
            }
        }

        return index;
    }

    public void insert(T item) {
        if (nodes[0] != null) {
            int index = getIndex(item);
            if (index != -1) {
                nodes[index].insert(item);
                return;
            }
        }

        objects.add(item);

        if (objects.size() > MAX_OBJECTS && level < MAX_LEVELS) {
            if (nodes[0] == null) {
                split();
            }

            int i = 0;
            while (i < objects.size()) {
                T obj = objects.get(i);
                int index = getIndex(obj);
                if (index != -1) {
                    nodes[index].insert(objects.remove(i));
                } else {
                    i++;
                }
            }
        }
    }

    public List<T> retrieve(List<T> returnObjects, Bounds returnArea) {
        return retrieve(returnObjects, returnArea.x, returnArea.y, returnArea.width, returnArea.height);
    }

    public List<T> retrieve(List<T> returnObjects, double qx, double qy, double qw, double qh) {
        if (nodes[0] != null) {
            double midX = bounds.x + (bounds.width / 2.0);
            double midY = bounds.y + (bounds.height / 2.0);

            boolean topQuadrant = (qy < midY && qy + qh < midY);
            boolean bottomQuadrant = (qy > midY);

            if (qx < midX && qx + qw < midX) {
                if (topQuadrant) {
                    nodes[1].retrieve(returnObjects, qx, qy, qw, qh);
                } else if (bottomQuadrant) {
                    nodes[2].retrieve(returnObjects, qx, qy, qw, qh);
                } else {
                    nodes[1].retrieve(returnObjects, qx, qy, qw, qh);
                    nodes[2].retrieve(returnObjects, qx, qy, qw, qh);
                }
            } else if (qx > midX) {
                if (topQuadrant) {
                    nodes[0].retrieve(returnObjects, qx, qy, qw, qh);
                } else if (bottomQuadrant) {
                    nodes[3].retrieve(returnObjects, qx, qy, qw, qh);
                } else {
                    nodes[0].retrieve(returnObjects, qx, qy, qw, qh);
                    nodes[3].retrieve(returnObjects, qx, qy, qw, qh);
                }
            } else {
                for (QuadTree<T> node : nodes) {
                    if (node.bounds.intersects(qx, qy, qw, qh)) {
                        node.retrieve(returnObjects, qx, qy, qw, qh);
                    }
                }
            }
        }

        for (T obj : objects) {
            double ox = provider.getX(obj);
            double oy = provider.getY(obj);
            double ow = provider.getWidth(obj);
            double oh = provider.getHeight(obj);
            if (ox + ow > qx && ox < qx + qw && oy + oh > qy && oy < qy + qh) {
                returnObjects.add(obj);
            }
        }
        return returnObjects;
    }

    public int getLevel() {
        return level;
    }

    public Bounds getBounds() {
        return bounds;
    }

    public int size() {
        int count = objects.size();
        if (nodes[0] != null) {
            for (QuadTree<T> node : nodes) {
                count += node.size();
            }
        }
        return count;
    }
}
