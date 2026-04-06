package tolk.nodes;

import org.junit.jupiter.api.Test;
import tolk.runtime.JolkNothing;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ## JolkIdentityNodeTest
 *
 * Verifies the behavior of the {@link JolkIdentityNode}, which implements
 * Jolk's identity (`==`, `!=`) and equivalence (`~~`, `!~`) operators.
 */
public class JolkIdentityNodeTest {

    // Helper to create a simple executable node for testing
    private JolkNode createLiteralNode(Object value) {
        return new JolkLiteralNode(value);
    }

    /**
     * Tests the `==` operator for strict identity (reference equality).
     */
    @Test
    void testStrictIdentityEquals() {
        Object obj1 = new Object();
        Object obj2 = new Object();

        // Same instance
        JolkIdentityNode node1 = new JolkIdentityNode(createLiteralNode(obj1), createLiteralNode(obj1), false);
        assertTrue((Boolean) node1.executeGeneric(null), "Same object references should be identical (==).");

        // Different instances
        JolkIdentityNode node2 = new JolkIdentityNode(createLiteralNode(obj1), createLiteralNode(obj2), false);
        assertFalse((Boolean) node2.executeGeneric(null), "Different object references should not be identical (==).");

        // JolkNothing identity
        JolkIdentityNode node3 = new JolkIdentityNode(createLiteralNode(JolkNothing.INSTANCE), createLiteralNode(JolkNothing.INSTANCE), false);
        assertTrue((Boolean) node3.executeGeneric(null), "JolkNothing.INSTANCE should be identical to itself (==).");

        JolkIdentityNode node4 = new JolkIdentityNode(createLiteralNode(JolkNothing.INSTANCE), createLiteralNode(obj1), false);
        assertFalse((Boolean) node4.executeGeneric(null), "JolkNothing.INSTANCE should not be identical to other objects (==).");
    }

    /**
     * Tests the `!=` operator for strict non-identity (reference inequality).
     */
    @Test
    void testStrictIdentityNotEquals() {
        Object obj1 = new Object();
        Object obj2 = new Object();

        // Same instance
        JolkIdentityNode node1 = new JolkIdentityNode(createLiteralNode(obj1), createLiteralNode(obj1), true);
        assertFalse((Boolean) node1.executeGeneric(null), "Same object references should not be non-identical (!=).");

        // Different instances
        JolkIdentityNode node2 = new JolkIdentityNode(createLiteralNode(obj1), createLiteralNode(obj2), true);
        assertTrue((Boolean) node2.executeGeneric(null), "Different object references should be non-identical (!=).");
    }

    /**
     * Tests the `~~` operator for equivalence (structural comparison).
     * This relies on the `InteropLibrary.isIdenticalOrEqual` message, which typically
     * delegates to `equals()` for non-primitive types.
     */
    @Test
    void testEquivalenceEquals() {
        String s1 = new String("hello");
        String s2 = new String("hello");
        String s3 = new String("world");

        // Same content, different instances
        JolkNode left = new JolkMessageSendNode(createLiteralNode(s1), "~~", new JolkNode[]{createLiteralNode(s2)});
        assertTrue((Boolean) left.executeGeneric(null), "Objects with same content should be equivalent (~~).");

        // Different content
        JolkNode right = new JolkMessageSendNode(createLiteralNode(s1), "~~", new JolkNode[]{createLiteralNode(s3)});
        assertFalse((Boolean) right.executeGeneric(null), "Objects with different content should not be equivalent (~~).");

        // Primitives with same value
        JolkNode longEq = new JolkMessageSendNode(createLiteralNode(10L), "~~", new JolkNode[]{createLiteralNode(10L)});
        assertTrue((Boolean) longEq.executeGeneric(null), "Longs with same value should be equivalent (~~).");
    }

    /**
     * Tests the `!~` operator for non-equivalence (structural non-comparison).
     */
    @Test
    void testEquivalenceNotEquals() {
        String s1 = new String("hello");
        String s2 = new String("hello");
        String s3 = new String("world");

        JolkNode left = new JolkMessageSendNode(createLiteralNode(s1), "!~", new JolkNode[]{createLiteralNode(s2)});
        assertFalse((Boolean) left.executeGeneric(null), "Objects with same content should not be non-equivalent (!~).");

        JolkNode right = new JolkMessageSendNode(createLiteralNode(s1), "!~", new JolkNode[]{createLiteralNode(s3)});
        assertTrue((Boolean) right.executeGeneric(null), "Objects with different content should be non-equivalent (!~).");
    }

    @Test
    void testNumericEquivalence() {
        // Jolk treats 10 (Int) and 10L (Long) as equivalent (~~) and identical (==)
        JolkNode left = createLiteralNode(10);
        JolkNode right = createLiteralNode(10L);

        JolkIdentityNode identity = new JolkIdentityNode(left, right, true);
        assertFalse((Boolean) identity.executeGeneric(null), "Different numeric types are equal when their values are equal.");

        JolkMessageSendNode equiv = new JolkMessageSendNode(left, "~~", new JolkNode[]{right});
        assertTrue((Boolean) equiv.executeGeneric(null), "Same numeric values are equivalent.");
    }
}