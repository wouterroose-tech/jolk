package tolk.nodes;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ## JolkReturnNodeTest
 *
 * Verifies the behavior of the {@link JolkReturnNode}, which implements
 * Jolk's non-local return mechanism ("Augmented Escaping").
 */
public class JolkReturnNodeTest {

    /**
     * Tests that a {@link JolkReturnNode} correctly throws a {@link JolkReturnException}
     * with the specified result and target depth.
     */
    @Test
    void testReturnNodeThrowsException() {
        JolkNode valueToReturn = new JolkLiteralNode("return_value");
        // Create a mock frame and its arguments to be the target
        Object[] mockFrameArgs = new Object[]{"mockArg"};
        VirtualFrame mockFrame = new VirtualFrameMock(null, mockFrameArgs);
        JolkNode targetFrame = new JolkReadEnvironmentNode(0); // This will resolve to mockFrameArgs
        JolkReturnNode returnNode = new JolkReturnNode(valueToReturn, targetFrame);

        JolkReturnException thrown = assertThrows(JolkReturnException.class, () -> returnNode.executeGeneric(mockFrame),
                "JolkReturnNode should throw JolkReturnException.");

        assertEquals("return_value", thrown.getResult(), "The exception should carry the correct return value.");
        assertEquals(mockFrameArgs, thrown.getTarget(), "The exception should carry the correct target identity (mockFrameArgs).");
    }

    /**
     * Tests a simple non-local return from a closure to its immediate parent method.
     */
    @Test
    void testNonLocalReturnToParentMethod() {
        // Method body: defines a closure and calls it
        JolkNode methodBody = new JolkNode() {
            @Child JolkNode closureNode;

            @Override
            public Object executeGeneric(VirtualFrame frame) {
                // Closure body: returns 99 to the method (targetDepth 1 relative to closure activation)
                JolkNode closureInnerBody = new JolkReturnNode(new JolkLiteralNode(99), new JolkReadEnvironmentNode(1));
                CallTarget closureTarget = new JolkRootNode(null, closureInnerBody, "innerClosure", false).getCallTarget();
                closureNode = JolkClosureNodeGen.create(closureTarget);

                Object closure = closureNode.executeGeneric(frame);
                return execute(closure);
            }
        };

        // Root node for the method (isMethod = true)
        JolkRootNode methodRootNode = new JolkRootNode(null, methodBody, "testMethod", true);
        CallTarget methodCallTarget = methodRootNode.getCallTarget();

        Object result = methodCallTarget.call();
        assertEquals(99, result, "Non-local return from closure should return to the parent method.");
    }

    /**
     * Tests a non-local return that skips an intermediate closure frame.
     * Method -> Outer Closure (depth 0) -> Inner Closure (depth 1) -> Return to Method (targetDepth 1 from Inner)
     */
    @Test
    void testNonLocalReturnSkipsIntermediateClosure() {
        // Innermost closure body: returns 123, targeting the method (depth 2 from here)
        JolkNode innermostClosureBody = new JolkReturnNode(new JolkLiteralNode(123), new JolkReadEnvironmentNode(2));
        CallTarget innermostClosureTarget = new JolkRootNode(null, innermostClosureBody, "innermostClosure", false).getCallTarget();
        JolkClosureNode innermostClosureNode = JolkClosureNodeGen.create(innermostClosureTarget);

        // Outer closure body: defines and executes the innermost closure
        JolkNode outerClosureBody = new JolkNode() {
            @Child JolkNode innerClosure = innermostClosureNode;
            @Override
            public Object executeGeneric(VirtualFrame frame) {
                // This statement should be skipped
                return execute(innerClosure.executeGeneric(frame));
            }
        };
        CallTarget outerClosureTarget = new JolkRootNode(null, outerClosureBody, "outerClosure", false).getCallTarget();
        JolkClosureNode outerClosureNode = JolkClosureNodeGen.create(outerClosureTarget);

        // Method body: defines and executes the outer closure
        JolkNode methodBody = new JolkNode() {
            @Child JolkNode outerClosure = outerClosureNode;
            @Override
            public Object executeGeneric(VirtualFrame frame) {
                // This statement should be skipped
                return execute(outerClosure.executeGeneric(frame));
            }
        };

        // Root node for the method (isMethod = true)
        JolkRootNode methodRootNode = new JolkRootNode(null, methodBody, "testMethod", true);
        CallTarget methodCallTarget = methodRootNode.getCallTarget();

        Object result = methodCallTarget.call();
        assertEquals(123, result, "Non-local return should skip intermediate closure frames.");
    }

    @Test
    void testReturnNodeWithLiteralDepth() {
        // Simulation of ^ value targeting depth 2
        JolkReturnNode node = new JolkReturnNode(new JolkLiteralNode("val"), new JolkReadEnvironmentNode(2));
        // Mock frame with environment chain (targetArgs[0] pattern)
        Object[] depth2 = {"target"};
        Object[] depth1 = {depth2};
        Object[] depth0 = {depth1};
        VirtualFrame frame = new VirtualFrameMock(null, depth0);
        
        JolkReturnException ex = assertThrows(JolkReturnException.class, () -> node.executeGeneric(frame));
        assertEquals(depth2, ex.getTarget(), "The exception target must match the resolved environment array at depth 2.");
    }

    private Object execute(Object receiver, Object... args) {
        try {
            return InteropLibrary.getUncached().execute(receiver, args);
        } catch (UnsupportedTypeException | ArityException | UnsupportedMessageException e) {
            throw new RuntimeException(e);
        }
    }
}