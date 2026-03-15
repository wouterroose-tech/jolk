package tolk.nodes;

import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import tolk.JolcTestBase;

import static org.junit.jupiter.api.Assertions.assertEquals;

///
/// Verifies the fundamental behavior of expression nodes in the Jolk AST.
/// These tests ensure that basic literals and simple expressions are evaluated
/// correctly, forming the foundation of the execution tree.
///
public class JolkNodeTest extends JolcTestBase {

    @Test
    @Disabled("Pending implementation of literal nodes.")
    void testIntegerLiteralNode() {
        Value result = eval("42");
        assertEquals(42, result.asInt(), "An integer literal node should evaluate to its primitive value.");
    }

    @Test
    @Disabled("Pending implementation of binary operation nodes.")
    void testSimpleAdditionNode() {
        Value result = eval("10 + 32");
        assertEquals(42, result.asInt(), "An addition node should correctly sum the values of its children.");
    }

}
