package tolk.nodes;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

///
/// Verifies the behavior of the [JolkFieldNode].
///
public class JolkFieldNodeTest {

    ///
    /// Tests that the field node correctly stores its name and initializer.
    ///
    @Test
    void testFieldMetadata() {
        JolkNode initializer = new JolkLiteralNode(123);
        JolkFieldNode node = new JolkFieldNode("age", initializer);

        assertEquals("age", node.getName());
        assertSame(initializer, node.getInitializer());
    }

    ///
    /// Tests that executing a field node returns the value of its initializer.
    ///
    @Test
    void testFieldExecution() {
        JolkFieldNode node = new JolkFieldNode("f", new JolkLiteralNode("val"));
        
        JolkRootNode root = new JolkRootNode(null, node);
        Object result = root.getCallTarget().call();
        
        assertEquals("val", result);
    }

    @Test
    void testFieldExecutionNoInitializer() {
        JolkFieldNode node = new JolkFieldNode("f", null);
        Object result = node.executeGeneric(null);
        
        assertNull(result, "Executing a field with no initializer should return null.");
    }
}
