package br.apsu.core.spatial;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testes de Unidade — QuadTree 2D")
public class QuadTreeTest {

    private record TestItem(double x, double y, double w, double h, String name) {}

    private QuadTree<TestItem> quadTree;

    @BeforeEach
    void setUp() {
        QuadTree.Bounds bounds = new QuadTree.Bounds(0, 0, 4000, 768);
        quadTree = new QuadTree<>(0, bounds, new QuadTree.BoundingBoxProvider<>() {
            @Override
            public double getX(TestItem item) { return item.x(); }
            @Override
            public double getY(TestItem item) { return item.y(); }
            @Override
            public double getWidth(TestItem item) { return item.w(); }
            @Override
            public double getHeight(TestItem item) { return item.h(); }
        });
    }

    @Test
    @DisplayName("Inserção e consulta em área delimitada")
    void testInsertAndRetrieve() {
        TestItem item1 = new TestItem(100, 100, 50, 50, "Item1");
        TestItem item2 = new TestItem(3000, 500, 50, 50, "Item2");

        quadTree.insert(item1);
        quadTree.insert(item2);

        assertEquals(2, quadTree.size());

        List<TestItem> foundNearItem1 = quadTree.retrieve(new ArrayList<>(), 50, 50, 200, 200);
        assertTrue(foundNearItem1.contains(item1));
        assertFalse(foundNearItem1.contains(item2));
    }

    @Test
    @DisplayName("Divisão automática em nós-filhos (split) quando excede capacidade")
    void testSubdivision() {
        for (int i = 0; i < 15; i++) {
            quadTree.insert(new TestItem(i * 100, i * 20, 40, 40, "Item" + i));
        }

        assertEquals(15, quadTree.size());

        quadTree.clear();
        assertEquals(0, quadTree.size());
    }

    @Test
    @DisplayName("Validação de interseção da classe Bounds")
    void testBoundsIntersects() {
        QuadTree.Bounds b1 = new QuadTree.Bounds(100, 100, 50, 50);
        QuadTree.Bounds b2 = new QuadTree.Bounds(120, 120, 50, 50);
        QuadTree.Bounds b3 = new QuadTree.Bounds(500, 500, 50, 50);

        assertTrue(b1.intersects(b2));
        assertFalse(b1.intersects(b3));
    }
}
