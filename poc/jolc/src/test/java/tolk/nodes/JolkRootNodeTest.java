package tolk.nodes;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.frame.VirtualFrame;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ## JolkRootNodeTest
 *
 * Verifies the behavior of the {@link JolkRootNode}, which serves as the entry point
 * for Jolk methods and closures, managing their execution context and handling
 * non-local returns.
 */
public class JolkRootNodeTest {

    /**
     * Tests that a {@link JolkRootNode} correctly executes its body and returns the result.
     */
    @Test
    void testExecuteGeneric() {
        JolkNode body = new JolkLiteralNode(42);
        JolkRootNode rootNode = new JolkRootNode(null, body, "testMethod", true);
        CallTarget callTarget = rootNode.getCallTarget();

        Object result = callTarget.call();
        assertEquals(42, result, "The root node should execute its body and return the result.");
    }

    /**
     * Tests that a {@link JolkRootNode} configured as a method boundary
     * catches a {@link JolkReturnException} and returns its value.
     */
    @Test
    void testMethodRootNodeCatchesReturnException() {
        // A body that throws a JolkReturnException
        JolkNode throwingBody = new JolkNode() {
            @Override
            public Object executeGeneric(VirtualFrame frame) {
                // Jolk uses identity-based targets for non-local returns.
                // The target identity is the arguments array of the target activation.
                throw new JolkReturnException(42, frame.getArguments());
            }
        };

        // Root node for a method (isMethod = true)
        JolkRootNode methodRootNode = new JolkRootNode(null, throwingBody, "testMethod", true);
        CallTarget callTarget = methodRootNode.getCallTarget();

        Object result = callTarget.call();
        assertEquals(42, result, "A method root node should catch JolkReturnException and return its value.");
    }

    /**
     * Tests that a {@link JolkRootNode} configured as a closure (not a method boundary)
     * re-throws a {@link JolkReturnException} if it's not the target of the return.
     * This is crucial for Non-Local Returns.
     */
    @Test
    void testClosureRootNodeReThrowsReturnExceptionIfNotTarget() {
        // A body that throws a JolkReturnException with targetDepth > 0
        // We need a distinct Object[] to represent a "higher" frame's arguments.
        Object[] higherFrameArgs = new Object[]{"higher"};
        JolkNode throwingBody = new JolkNode() {
            @Override
            public Object executeGeneric(VirtualFrame frame) {
                // This return targets a frame *other* than the current closure's frame.
                // The JolkRootNode for the closure should re-throw it.
                throw new JolkReturnException(99, higherFrameArgs);
            }
        };

        // Root node for a closure (isMethod = false)
        JolkRootNode closureRootNode = new JolkRootNode(null, throwingBody, "testClosure", false);
        CallTarget callTarget = closureRootNode.getCallTarget(); // This will create a frame for the closure

        // Expect the JolkReturnException to be re-thrown
        JolkReturnException thrown = assertThrows(JolkReturnException.class, () -> callTarget.call(),
                "A closure root node should re-throw JolkReturnException if it's not the target.");

        assertEquals(99, thrown.getResult(), "The re-thrown exception should carry the correct result.");
        assertEquals(higherFrameArgs, thrown.getTarget(), "The re-thrown exception should carry the correct target identifier.");
    }

    /**
     * Tests that a {@link JolkRootNode} properly handles standard execution 
     * without exceptions.
     */
    @Test
    void testStandardExecutionFlow() {
        JolkNode body = new JolkNode() {
            @Override
            public Object executeGeneric(VirtualFrame frame) {
                return "success";
            }
        };
        JolkRootNode root = new JolkRootNode(null, body, "test", true);
        assertEquals("success", root.getCallTarget().call());
    }

    @Test
    void testClosureRootNodeAcceptsZeroDepthReturn() {
        // A closure root node should re-throw returns that target its own frame,
        // as it's not a method boundary.
        JolkNode body = new JolkNode() {
            @Override public Object executeGeneric(VirtualFrame frame) {
                // This return targets the current frame's arguments.
                throw new JolkReturnException("local", frame.getArguments());
            }
        };
        // Even if isMethod is false, if targetDepth is 0, it should be caught if the node is the target.
        // However, JolkRootNode logic usually catches ONLY if isMethod is true or depth matches.
        JolkRootNode root = new JolkRootNode(null, body, "closure", false);
        assertThrows(JolkReturnException.class, () -> root.getCallTarget().call());
    }
}