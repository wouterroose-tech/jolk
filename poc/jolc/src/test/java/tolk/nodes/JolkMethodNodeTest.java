package tolk.nodes;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

///
/// Verifies the behavior of the [JolkMethodNode].
///
/// These tests ensure that method nodes correctly preserve their metadata
/// (name and body) and that executing the node correctly triggers the
/// execution of its body within the provided virtual frame.
///
public class JolkMethodNodeTest {

    ///
    /// Verifies that the method node correctly stores and exposes its
    /// identity and executable body.
    ///
    @Test
    void testMethodMetadata() {
        JolkNode body = new JolkEmptyNode();
        String[] params = {"arg1"};
        JolkMethodNode node = new JolkMethodNode("myMethod", body, params, false);

        assertEquals("myMethod", node.getName());
        assertSame(body, node.getBody(), "getBody should return the constructor-injected body node.");
        assertArrayEquals(params, node.getParameters(), "getParameters should return the constructor-injected parameters.");
        assertFalse(node.isVariadic(), "isVariadic should reflect the constructor-injected value.");
        assertEquals(0, node.getFrameSlots(), "Default frameSlots should be 0 for the 4-argument constructor.");
    }

    ///
    /// Verifies that the method node correctly stores frame slot information
    /// when provided via the 5-argument constructor.
    ///
    @Test
    void testMethodMetadataWithFrameSlots() {
        JolkNode body = new JolkEmptyNode();
        String[] params = {"a", "b"};
        int expectedFrameSlots = 5; // Example value
        JolkMethodNode node = new JolkMethodNode("complexMethod", body, params, true, expectedFrameSlots, false);

        assertEquals("complexMethod", node.getName());
        assertSame(body, node.getBody());
        assertArrayEquals(params, node.getParameters());
        assertTrue(node.isVariadic());
        assertEquals(expectedFrameSlots, node.getFrameSlots(), "getFrameSlots should return the constructor-injected value.");
    }
    
    ///
    /// Verifies that executing a [JolkMethodNode] delegates to its body
    /// and returns the evaluation result.
    ///
    @Test
    void testMethodExecution() {
        JolkNode body = new JolkLiteralNode("hello");
        JolkMethodNode node = new JolkMethodNode("greet", body, new String[0], false);

        Object result = execute(node);

        assertEquals("hello", result, "Method execution should return the result of its body evaluation.");
    }

    ///
    /// Helper method to execute a JolkNode within a proper Truffle context.
    ///
    /// @param node The node to execute.
    /// @return The result of the execution.
    ///
    private Object execute(JolkNode node) {
        JolkRootNode root = new JolkRootNode(null, node);
        return root.getCallTarget().call();
    }
}
