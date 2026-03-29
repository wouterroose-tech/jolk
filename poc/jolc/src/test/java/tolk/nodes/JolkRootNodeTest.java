package tolk.nodes;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.Test;
import tolk.JolcTestBase;

import static org.junit.jupiter.api.Assertions.*;

///
/// Verifies the behavior of the root execution node for the Jolk language.
/// These tests confirm that the program's entry point correctly manages the
/// execution flow and returns the appropriate final value from a script.
///
public class JolkRootNodeTest extends JolcTestBase {

    @Test
    void testEmptyProgramReturnsNull() {
        Value result = eval("");
        assertEquals("null", result.toString(), "Executing an empty program should result in a null value.");
    }

    /**
     * Verifies that the root node correctly delegates execution to the body node
     * and returns the result in a script (non-method) context.
     */
    @Test
    void testScriptExecution() {
        JolkNode body = new JolkLiteralNode(100L);
        JolkRootNode rootNode = new JolkRootNode(null, body, "script", false);

        Object result = rootNode.getCallTarget().call();
        assertEquals(100L, result, "Script execution should return the literal value from the body.");
    }

    /**
     * Verifies that when a [JolkReturnException] is thrown with a target matching
     * the current activation's environment, the root node catches it and returns the value.
     */
    @Test
    void testMethodReturnUnwindsToCorrectTarget() {
        Object[] environment = new Object[]{"self", "arg1"};

        JolkNode body = new JolkNode() {
            @Override
            public Object executeGeneric(VirtualFrame frame) {
                // In Jolk, ^ targets the 'Home' method arguments array (the lexical environment)
                throw new JolkReturnException("ReturnValue", frame.getArguments());
            }
        };

        JolkRootNode rootNode = new JolkRootNode(null, new FrameDescriptor(), body, "testMethod", true);
        Object result = rootNode.getCallTarget().call(environment);

        assertEquals("ReturnValue", result, "RootNode should catch the ReturnException when the target matches the arguments.");
    }

    /**
     * Verifies that a [JolkReturnException] bubbles up through intermediate methods
     * if the target does not match the current method's environment.
     */
    @Test
    void testReturnBubblesUpOnMismatch() {
        Object[] currentEnv = new Object[]{"inner"};
        Object[] targetEnv = new Object[]{"outer"};

        JolkNode body = new JolkNode() {
            @Override
            public Object executeGeneric(VirtualFrame frame) {
                throw new JolkReturnException("exit", targetEnv);
            }
        };

        JolkRootNode rootNode = new JolkRootNode(null, new FrameDescriptor(), body, "innerMethod", true);

        // Should bubble up because currentEnv != targetEnv
        assertThrows(JolkReturnException.class, () -> rootNode.getCallTarget().call(currentEnv),
            "ReturnException should be rethrown if the target doesn't match the current frame arguments.");
    }

    @Test
    void testGetName() {
        JolkRootNode rootNode = new JolkRootNode(null, new JolkEmptyNode(), "testRoot", false);
        assertEquals("testRoot", rootNode.getName());
    }

}
