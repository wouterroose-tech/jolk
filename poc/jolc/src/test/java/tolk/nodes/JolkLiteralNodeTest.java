package tolk.nodes;

import org.junit.jupiter.api.Test;
import tolk.runtime.JolkNothing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

public class JolkLiteralNodeTest {

    @Test
    void testIntegerLiteral() {
        JolkLiteralNode node = new JolkLiteralNode(42);
        assertEquals(42, node.executeGeneric(null));
    }

    @Test
    void testStringLiteral() {
        JolkLiteralNode node = new JolkLiteralNode("Hello Jolk");
        assertEquals("Hello Jolk", node.executeGeneric(null));
    }

    @Test
    void testBooleanLiteral() {
        JolkLiteralNode node = new JolkLiteralNode(true);
        assertEquals(true, node.executeGeneric(null));
    }

    @Test
    void testNothingLiteral() {
        JolkLiteralNode node = new JolkLiteralNode(JolkNothing.INSTANCE);
        assertSame(JolkNothing.INSTANCE, node.executeGeneric(null));
    }
}
