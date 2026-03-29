package tolk.nodes;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * ### JolkReadArgumentNodeTest
 *
 * Verifies the behavior of the [JolkReadArgumentNode]. This node is responsible
 * for retrieving values from the Truffle frame arguments, supporting both 
 * direct local arguments and captured lexical environments.
 */
public class JolkReadArgumentNodeTest {

    /**
     * Verifies that the node can read a direct argument from the current frame (depth 0).
     */
    @Test
    void testReadDirectArgument() {
        // We want to read index 1. 
        // (In Jolk, index 0 is usually 'self' or the captured '<env>')
        JolkReadArgumentNode node = new JolkReadArgumentNode(1, 0);
        JolkRootNode root = new JolkRootNode(null, node);

        Object[] args = {"self", "targetValue"};
        Object result = root.getCallTarget().call(args);

        assertEquals("targetValue", result, "Should read argument at index 1 from the current frame.");
    }

    /**
     * Verifies that the node can read an argument from a captured environment (depth 1).
     * In Jolk's closure model, the parent environment is passed as the first argument (index 0).
     */
    @Test
    void testReadCapturedArgument() {
        // Read index 1 from the environment one level up
        JolkReadArgumentNode node = new JolkReadArgumentNode(1, 1);
        JolkRootNode root = new JolkRootNode(null, node);

        Object[] parentEnv = {"parentSelf", "capturedValue"};
        Object[] currentArgs = {parentEnv, "localValue"};

        Object result = root.getCallTarget().call(currentArgs);

        assertEquals("capturedValue", result, "Should navigate to the parent environment to read the argument.");
    }

    /**
     * Verifies reading from a deeply nested environment (depth 2).
     */
    @Test
    void testReadDeeplyNestedArgument() {
        JolkReadArgumentNode node = new JolkReadArgumentNode(1, 2);
        JolkRootNode root = new JolkRootNode(null, node);

        Object[] grandParentEnv = {"gpSelf", "deepValue"};
        Object[] parentEnv = {grandParentEnv, "pLocal"};
        Object[] currentArgs = {parentEnv, "local"};

        Object result = root.getCallTarget().call(currentArgs);

        assertEquals("deepValue", result, "Should navigate multiple environment levels to find the value.");
    }
}
