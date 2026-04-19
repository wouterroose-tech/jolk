package tolk.nodes;

import org.junit.jupiter.api.Test;
import tolk.runtime.JolkNothing;
import tolk.JolcTestBase;
import tolk.language.JolkLanguage;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ## JolkIdentityNodeTest
 *
 * Verifies the behavior of the {@link JolkIdentityNode}, which implements
 * Jolk's identity (`==`, `!=`) and equivalence (`~~`, `!~`) operators.
 */
public class JolkIdentityNodeTest extends JolcTestBase {

    // Helper to create a simple executable node for testing
    private JolkNode createLiteralNode(Object value) {
        return new JolkLiteralNode(value);
    }

    /**
     * Tests the `==` operator for strict identity (reference equality).
     */
    @Test
    void testStrictIdentityEquals() {
        eval(""); // Initialize context
        context.enter();
        try {
            Object obj1 = new Object();
            Object obj2 = new Object();

            // Same instance
            JolkIdentityNode node1 = new JolkIdentityNode(createLiteralNode(obj1), createLiteralNode(obj1), false);
            assertTrue((Boolean) execute(node1), "Same object references should be identical (==).");

            // Different instances
            JolkIdentityNode node2 = new JolkIdentityNode(createLiteralNode(obj1), createLiteralNode(obj2), false);
            assertFalse((Boolean) execute(node2), "Different object references should not be identical (==).");

            // JolkNothing identity
            JolkIdentityNode node3 = new JolkIdentityNode(createLiteralNode(JolkNothing.INSTANCE), createLiteralNode(JolkNothing.INSTANCE), false);
            assertTrue((Boolean) execute(node3), "JolkNothing.INSTANCE should be identical to itself (==).");

            JolkIdentityNode node4 = new JolkIdentityNode(createLiteralNode(JolkNothing.INSTANCE), createLiteralNode(obj1), false);
            assertFalse((Boolean) execute(node4), "JolkNothing.INSTANCE should not be identical to other objects (==).");
        } finally {
            context.leave();
        }
    }

    /**
     * Tests the `!=` operator for strict non-identity (reference inequality).
     */
    @Test
    void testStrictIdentityNotEquals() throws Exception {
        eval(""); // Initialize context
        context.enter();
        try {
            Object obj1 = new Object();
            Object obj2 = new Object();

            // Same instance
            JolkIdentityNode node1 = new JolkIdentityNode(createLiteralNode(obj1), createLiteralNode(obj1), true);
            assertFalse((Boolean) execute(node1), "Same object references should not be non-identical (!=).");

            // Different instances
            JolkIdentityNode node2 = new JolkIdentityNode(createLiteralNode(obj1), createLiteralNode(obj2), true);
            assertTrue((Boolean) execute(node2), "Different object references should be non-identical (!=).");
        } finally {
            context.leave();
        }
    }

    /**
     * Tests the `~~` operator for equivalence (structural comparison).
     * This relies on the `InteropLibrary.isIdenticalOrEqual` message, which typically
     * delegates to `equals()` for non-primitive types.
     */
    @Test
    void testEquivalenceEquals() throws Exception {
        eval(""); // Initialize context
        context.enter();
        try {
            String s1 = new String("hello");
            String s2 = new String("hello");
            String s3 = new String("world");

            // Same content, different instances
            JolkNode left = JolkMessageSendNodeGen.create("~~", new JolkNode[]{createLiteralNode(s2)}, createLiteralNode(s1));
            assertTrue((Boolean) execute(left), "Objects with same content should be equivalent (~~).");

            // Different content
            JolkNode right = JolkMessageSendNodeGen.create("~~", new JolkNode[]{createLiteralNode(s3)}, createLiteralNode(s1));
            assertFalse((Boolean) execute(right), "Objects with different content should not be equivalent (~~).");

            // Primitives with same value
            JolkNode longEq = JolkMessageSendNodeGen.create("~~", new JolkNode[]{createLiteralNode(10L)}, createLiteralNode(10L));
            assertTrue((Boolean) execute(longEq), "Longs with same value should be equivalent (~~).");
        } finally {
            context.leave();
        }
    }

    /**
     * Tests the `!~` operator for non-equivalence (structural non-comparison).
     */
    @Test
    void testEquivalenceNotEquals() throws Exception {
        eval(""); // Initialize context
        context.enter();
        try {
            String s1 = new String("hello");
            String s2 = new String("hello");
            String s3 = new String("world");

            JolkNode left = JolkMessageSendNodeGen.create("!~", new JolkNode[]{createLiteralNode(s2)}, createLiteralNode(s1));
            assertFalse((Boolean) execute(left), "Objects with same content should not be non-equivalent (!~).");

            JolkNode right = JolkMessageSendNodeGen.create("!~", new JolkNode[]{createLiteralNode(s3)}, createLiteralNode(s1));
            assertTrue((Boolean) execute(right), "Objects with different content should be non-equivalent (!~).");
        } finally {
            context.leave();
        }
    }

    @Test
    void testNumericEquivalence() {
        eval(""); // Initialize context
        context.enter();
        try {
            // Jolk treats 10 (Int) and 10L (Long) as equivalent (~~) and identical (==)
            JolkNode left = createLiteralNode(10);
            JolkNode right = createLiteralNode(10L);

            JolkIdentityNode identity = new JolkIdentityNode(left, right, true);
            assertFalse((Boolean) execute(identity), "Different numeric types are equal when their values are equal.");

            JolkNode equiv = JolkMessageSendNodeGen.create("~~", new JolkNode[]{right}, left);
            assertTrue((Boolean) execute(equiv), "Same numeric values are equivalent.");
        } finally {
            context.leave();
        }
    }

    /**
     * Helper method to execute a JolkNode within a proper Truffle context,
     * wrapping it in a JolkRootNode and calling its CallTarget.
     *
     * @param node The node to execute.
     * @return The result of the execution.
     */
    private Object execute(JolkNode node) {
        // Wrap in RootNode to support Truffle DSL cachi
        JolkLanguage lang = com.oracle.truffle.api.TruffleLanguage.LanguageReference.create(JolkLanguage.class).get(null);
        JolkRootNode root = new JolkRootNode(lang, node);
        return root.getCallTarget().call();
    }
}