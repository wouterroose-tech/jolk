package tolk.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import tolk.JolcTestBase;

/// 
/// Verifies the language's behavior when defining Long fields.
///
public class JolcLongTest extends JolcTestBase {

    @Test
    void testLongField() {
        // Verify that #new(arg) initializes the field 'val'
        String source = "class Container { Long val; }";
        Value meta = eval(source);
        
        Value instance = meta.invokeMember("new");
        assertEquals(0L, instance.invokeMember("val").asLong(), "Uninitialized Long fields should default to 0L.");
        instance.invokeMember("val", 1L);
        assertEquals(1L, instance.invokeMember("val").asLong(), "The getter should return the long value stored in the Object field.");

        // Canonical #new
        instance = meta.invokeMember("new", 42L);
        assertEquals(42L, instance.invokeMember("val").asLong(), "Canonical #new should initialize fields in order.");
    }

    /// Verify that Long fields can be initialized with expressions and that they evaluate correctly.
     @Test
     void testLongExpression() {
        String source = "class ExprTest { Long run() { ^ 40 + 1 ** 2  * -2 * -1 } }";
        Value result = eval(source).invokeMember("new").invokeMember("run");
        assertEquals(42L, result.asLong(), "The expression should evaluate to the correct long value.");
     }

    @Test
    void testArithmetic() {
        String source = "class MathTest { " +
                "Long add(Long a, Long b) { ^ a + b } " +
                "Long sub(Long a, Long b) { ^ a - b } " +
                "Long mul(Long a, Long b) { ^ a * b } " +
                "Long div(Long a, Long b) { ^ a / b } " +
                "Long mod(Long a, Long b) { ^ a % b } " +
                "Long pow(Long a, Long b) { ^ a ** b } }";
        Value instance = eval(source).invokeMember("new");
        assertEquals(30L, instance.invokeMember("add", 10L, 20L).asLong());
        assertEquals(10L, instance.invokeMember("sub", 30L, 20L).asLong());
        assertEquals(200L, instance.invokeMember("mul", 10L, 20L).asLong());
        assertEquals(5L, instance.invokeMember("div", 10L, 2L).asLong());
        assertEquals(1L, instance.invokeMember("mod", 10L, 3L).asLong());
        assertEquals(8L, instance.invokeMember("pow", 2L, 3L).asLong());
    }

    @Test
    void testComparison() {
        String source = "class CompTest { " +
                "Boolean gt(Long a, Long b) { ^ a > b } " +
                "Boolean lt(Long a, Long b) { ^ a < b } " +
                "Boolean ge(Long a, Long b) { ^ a >= b } " +
                "Boolean le(Long a, Long b) { ^ a <= b } " +
                "Boolean eq(Long a, Long b) { ^ a == b } " +
                "Boolean ne(Long a, Long b) { ^ a != b } }";
        Value instance = eval(source).invokeMember("new");
        assertTrue(instance.invokeMember("gt", 10L, 5L).asBoolean());
        assertTrue(instance.invokeMember("lt", 5L, 10L).asBoolean());
        assertTrue(instance.invokeMember("ge", 10L, 10L).asBoolean());
        assertTrue(instance.invokeMember("le", 10L, 10L).asBoolean());
        assertTrue(instance.invokeMember("eq", 10L, 10L).asBoolean());
        assertTrue(instance.invokeMember("ne", 10L, 5L).asBoolean());
    }

    @Test
    void testObjectProtocol() {
        String source = "class ProtoTest { " +
                "String str(Long i) { ^ i #toString } " +
                "Long h(Long i) { ^ i #hash } " +
                "Boolean eq(Long a, Long b) { ^ a ~~ b } " +
                "Boolean present(Long i) { ^ i #isPresent } " +
                "Boolean empty(Long i) { ^ i #isEmpty } }";
        Value instance = eval(source).invokeMember("new");
        assertEquals("42", instance.invokeMember("str", 42L).asString());
        assertEquals(42L, instance.invokeMember("h", 42L).asLong());
        assertTrue(instance.invokeMember("eq", 42L, 42L).asBoolean());
        assertTrue(instance.invokeMember("present", 42L).asBoolean());
        assertFalse(instance.invokeMember("empty", 42L).asBoolean());
    }

    @Test
    @Disabled("Default field values are not yet supported.")
    void testLongFieldWithDefault() {
        String source = "class Container { Long val = 42; }";
        Value meta = eval(source);
        Value instance = meta.invokeMember("new");  
        assertEquals(42L, instance.invokeMember("val").asLong(), "The field should be initialized to the default value.");
    }

    @Test
    void testMinMax() {
        String source = """
            class MinMaxTest {
            Long getMax() { ^ 9223372036854775807 }
            Long getMin() { ^ -9223372036854775808 }
            Long getMaxConst() { ^ Long #MAX }
            Long getMinConst() { ^ Long #MIN }
            Long wrap() { ^ 9223372036854775807 + 1 } }
            """;
        Value instance = eval(source).invokeMember("new");
        assertEquals(Long.MAX_VALUE, instance.invokeMember("getMax").asLong());
        assertEquals(Long.MIN_VALUE, instance.invokeMember("getMin").asLong());
        // TODO: Re-enable once constant folding is implemented and the PoC supports it.
        // assertEquals(Long.MAX_VALUE, instance.invokeMember("getMaxConst").asLong());
        // assertEquals(Long.MIN_VALUE, instance.invokeMember("getMinConst").asLong());
        assertEquals(Long.MIN_VALUE, instance.invokeMember("wrap").asLong(), "Long overflow should wrap around.");
    }
}
