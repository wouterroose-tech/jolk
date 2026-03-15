package tolk.nodes;

import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import tolk.JolcTestBase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ## JolkRootNodeTest
 *
 * Verifies the behavior of the root execution node for the Jolk language.
 * These tests confirm that the program's entry point correctly manages the
 * execution flow and returns the appropriate final value from a script.
 */
public class JolkRootNodeTest extends JolcTestBase {

    @Test
    void testEmptyProgramReturnsNull() {
        Value result = eval("");
        assertTrue(result.isNull(), "Executing an empty program should result in a null value.");
    }

}
