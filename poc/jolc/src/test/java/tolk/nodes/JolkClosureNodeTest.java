package tolk.nodes;

import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import org.junit.jupiter.api.Test;
import tolk.runtime.JolkClosure;
import com.oracle.truffle.api.CallTarget;

import static org.junit.jupiter.api.Assertions.*;

///
/// Verifies the behavior of the [JolkClosureNode].
///
/// These tests confirm that a closure node correctly creates a [JolkClosure]
/// object at runtime and that the resulting closure is executable and yields the
/// correct value.
///
public class JolkClosureNodeTest {

    ///
    /// Tests that executing a [JolkClosureNode] results in a non-null
    /// [JolkClosure] instance.
    ///
    @Test
    void testExecuteGenericReturnsClosure() {
        JolkNode body = new JolkLiteralNode("test");
        CallTarget target = new JolkRootNode(null, body, "closure", false).getCallTarget();
        JolkClosureNode closureNode = new JolkClosureNode(target, new String[0], false);

        Object result = execute(closureNode);

        assertNotNull(result);
        assertTrue(result instanceof JolkClosure, "Executing a closure node should yield a JolkClosure instance.");
    }

    ///
    /// Tests that the created [JolkClosure] can be executed and returns the
    /// value from its body. This test uses the Interop library to treat the closure
    /// as a generic executable Truffle object.
    ///
    @Test
    void testClosureExecution() throws UnsupportedMessageException, ArityException, UnsupportedTypeException {
        JolkNode body = new JolkLiteralNode(123);
        CallTarget target = new JolkRootNode(null, body, "closure", false).getCallTarget();
        JolkClosureNode closureNode = new JolkClosureNode(target, new String[0], false);

        Object closureObject = execute(closureNode);
        assertTrue(closureObject instanceof JolkClosure);

        InteropLibrary interop = InteropLibrary.getUncached();
        assertTrue(interop.isExecutable(closureObject), "JolkClosure must be an executable Truffle object.");

        Object result = interop.execute(closureObject);
        assertEquals(123, result, "Executing the closure should return the value of its body node.");
    }

    ///
    /// Tests that a closure can be defined with parameters and a variadic flag.
    /// In the PoC, this verifies the constructor path and ensures execution 
    /// still yields a valid, executable closure object.
    ///
    @Test
    void testClosureWithComplexMetadata() {
        JolkNode body = new JolkLiteralNode(true);
        String[] params = {"first", "second"};
        CallTarget target = new JolkRootNode(null, body, "closure", false).getCallTarget();
        JolkClosureNode node = new JolkClosureNode(target, params, true);

        Object result = execute(node);
        assertNotNull(result, "Should result in a non-null closure object even when parameters are defined.");
    }

    ///
    /// Helper method to execute a JolkNode within a proper Truffle context.
    /// This wraps the node in a [JolkRootNode] and calls it.
    ///
    /// @param node The node to execute.
    /// @return The result of the execution.
    ///
    private Object execute(JolkNode node) {
        // For testing JolkClosureNode, we need a JolkRootNode that is not a method.
        JolkRootNode root = new JolkRootNode(null, node, "testClosureNode", false);
        return root.getCallTarget().call();
    }
}
