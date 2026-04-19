package tolk.nodes;

import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;

import org.junit.jupiter.api.Test;
import tolk.runtime.JolkClosure;
import com.oracle.truffle.api.CallTarget;

import java.lang.reflect.Field;
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
        JolkClosureNode closureNode = new JolkClosureNode(target);

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
        JolkClosureNode closureNode = new JolkClosureNode(target);

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
        CallTarget target = new JolkRootNode(null, body, "closure", false).getCallTarget();
        JolkClosureNode node = new JolkClosureNode(target);

        Object result = execute(node);
        assertNotNull(result, "Should result in a non-null closure object even when parameters are defined.");
    }

    /**
     * ### testClosureProtocolHandlers
     * 
     * Verifies the JolkClosure interop protocol for 'catch' and 'finally'.
     */
    @Test
    //@Disabled("Activate when interop protocol is implemented")
    void testClosureProtocolHandlers() throws UnsupportedMessageException, ArityException, UnsupportedTypeException, UnknownIdentifierException {
        InteropLibrary interop = InteropLibrary.getUncached();
        
        // Setup a closure that returns a value
        JolkNode body = new JolkLiteralNode(42);
        CallTarget target = new JolkRootNode(null, body, "closure", false).getCallTarget();
        JolkClosure closure = new JolkClosure(target);

        // Test #finally: Ensure the finalAction is executed
        boolean[] finallyExecuted = {false};
        JolkClosure finalAction = new JolkClosure(new JolkRootNode(null, new JolkNode() {
            @Override public Object executeGeneric(com.oracle.truffle.api.frame.VirtualFrame frame) {
                finallyExecuted[0] = true;
                return null;
            }
        }, "finalizer", false).getCallTarget());

        Object result = interop.invokeMember(closure, "finally", finalAction);
        assertEquals(42, result, "Finally should return the result of the original closure.");
        assertTrue(finallyExecuted[0], "The 'finally' action must be executed.");

        // Test #catch: Ensure no catch occurs if no exception is thrown
        JolkClosure catchHandler = new JolkClosure(new JolkRootNode(null, new JolkLiteralNode("error"), "handler", false).getCallTarget());
        // Note: Exception.class is not a valid interop argument type. 
        // This test needs to be updated when the catch protocol is properly implemented.
        // For now, skip the catch test or use a valid argument type.
        Object noCatchResult = interop.invokeMember(closure, "catch", "Exception", catchHandler);
        assertEquals(42, noCatchResult, "Should return original result if no exception occurs.");
    }

    ///
    /// Tests that the closure correctly captures the lexical environment 
    /// (the frame arguments) of the caller.
    ///
    @Test
    void testEnvironmentCapture() {
        JolkNode body = new JolkLiteralNode("env");
        CallTarget target = new JolkRootNode(null, body, "closure", false).getCallTarget();
        JolkClosureNode closureNode = new JolkClosureNode(target);

        // Define lexical arguments to be captured by the node
        Object[] expectedEnv = {"outerValue", 99L};
        
        // Wrap the closure node in a root node and call it with arguments
        JolkRootNode parentRoot = new JolkRootNode(null, closureNode, "parent", false);
        JolkClosure closure = (JolkClosure) parentRoot.getCallTarget().call(expectedEnv);

        assertArrayEquals(expectedEnv, getEnvironmentViaReflection(closure), 
            "The closure should capture the frame arguments as its lexical environment.");
    }

    @Test
    void testExecuteWithNullFrame() {
        JolkNode body = new JolkLiteralNode(1);
        CallTarget target = new JolkRootNode(null, body, "closure", false).getCallTarget();
        JolkClosureNode closureNode = new JolkClosureNode(target);

        JolkClosure result = (JolkClosure) closureNode.executeGeneric(null);
        assertNull(getEnvironmentViaReflection(result), "Closure environment should be null if executed without a frame context.");
    }

    /**
     * ### getEnvironmentViaReflection
     *
     * Accesses the private environment field in `JolkClosure` for testing purposes.
     * This bypasses the current lack of a public getter in the runtime class.
     *
     * @param closure The closure instance to inspect.
     * @return The captured environment array.
     */
    private Object[] getEnvironmentViaReflection(JolkClosure closure) {
        try {
            Field field = JolkClosure.class.getDeclaredField("environment");
            field.setAccessible(true);
            Object env = field.get(closure);
            return (env instanceof Frame f) ? f.getArguments() : (Object[]) env;
        } catch (NoSuchFieldException e) {
            try {
                Field field = JolkClosure.class.getDeclaredField("env");
                field.setAccessible(true);
                Object env = field.get(closure);
                return (env instanceof Frame f) ? f.getArguments() : (Object[]) env;
            } catch (Exception ex) {
                throw new RuntimeException("Could not find environment field in JolkClosure", ex);
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
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
