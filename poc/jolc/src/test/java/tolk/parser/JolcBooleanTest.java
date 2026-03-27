package tolk.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import tolk.JolcTestBase;

///
/// Verifies the language's behavior when defining Boolean fields.
///
public class JolcBooleanTest extends JolcTestBase {

    @Test
    void testBooleanField() {
        String source = "class Container { Boolean val; }";
        Value meta = eval(source);

        Value instance = meta.invokeMember("new");
        assertFalse(instance.invokeMember("val").isNull(), "Boolean fields should default to false.");
        assertFalse(instance.invokeMember("val").asBoolean());

        instance.invokeMember("val", true);
        assertTrue(instance.invokeMember("val").asBoolean());
        instance.invokeMember("val", false);
        assertFalse(instance.invokeMember("val").asBoolean());

        // Canonical #new
        instance = meta.invokeMember("new", true);
        assertTrue(instance.invokeMember("val").asBoolean(), "Canonical #new should initialize Boolean fields.");
    }

    @Test
    void testLogicExpression() {
        String source = "class ExprTest { Boolean run() { ^ true && false || !false } }";
        Value result = eval(source).invokeMember("new").invokeMember("run");
        assertTrue(result.asBoolean(), "The logical expression should evaluate correctly.");
    }

    @Test
    void testLogicOperations() {
        String source = """
            class LogicTest {
                Boolean and(Boolean a, Boolean b) { ^ a && b }
                Boolean or(Boolean a, Boolean b) { ^ a || b }
                Boolean not(Boolean a) { ^ !a }
            }
            """;
        Value instance = eval(source).invokeMember("new");
        assertTrue(instance.invokeMember("and", true, true).asBoolean());
        assertFalse(instance.invokeMember("and", true, false).asBoolean());
        assertTrue(instance.invokeMember("or", false, true).asBoolean());
        assertFalse(instance.invokeMember("or", false, false).asBoolean());
        assertFalse(instance.invokeMember("not", true).asBoolean());
        assertTrue(instance.invokeMember("not", false).asBoolean());
    }

    @Test
    void testEquality() {
        String source = """
            class EqualityTest {
                Boolean eq(Boolean a, Boolean b) { ^ a == b }
                Boolean ne(Boolean a, Boolean b) { ^ a != b }
            }
            """;
        Value instance = eval(source).invokeMember("new");
        assertTrue(instance.invokeMember("eq", true, true).asBoolean());
        assertFalse(instance.invokeMember("eq", true, false).asBoolean());
        assertTrue(instance.invokeMember("ne", true, false).asBoolean());
    }

    @Test
    //@Disabled("Message dispatch not yet implemented for the PoC.")
    void testObjectProtocol() {
        String source = """
            class ProtoTest {
                String str(Boolean b) { ^ b #toString }
                Long h(Boolean b) { ^ b #hash }
                Boolean eq(Boolean a, Boolean b) { ^ a ~~ b }
            }
            """;
        Value instance = eval(source).invokeMember("new");
        assertEquals("true", instance.invokeMember("str", true).asString());
        assertEquals((long)Boolean.TRUE.hashCode(), instance.invokeMember("h", true).asLong());
        assertTrue(instance.invokeMember("eq", true, true).asBoolean());
    }

}
