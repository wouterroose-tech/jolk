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
                "Boolean ne(Long a, Long b) { ^ a !~ b } " +
                "Boolean present(Long i) { ^ i #isPresent } " +
                "Boolean empty(Long i) { ^ i #isEmpty } }";
        Value instance = eval(source).invokeMember("new");
        assertEquals("42", instance.invokeMember("str", 42L).asString());
        assertEquals(42L, instance.invokeMember("h", 42L).asLong());
        assertTrue(instance.invokeMember("eq", 42L, 42L).asBoolean());
        assertFalse(instance.invokeMember("ne", 42L, 42L).asBoolean());
        assertTrue(instance.invokeMember("present", 42L).asBoolean());
        assertFalse(instance.invokeMember("empty", 42L).asBoolean());
    }

    @Test
    @Disabled("Default field values are not yet supported.")
    void testLongFieldWithDefault() {
        String source = "class Container { Long long = 42; Int int = 42;}";
        Value meta = eval(source);
        Value instance = meta.invokeMember("new");  
        assertEquals(42L, instance.invokeMember("long").asLong());
        assertEquals(42L, instance.invokeMember("int").asLong());
    }

    @Test
    void testMinMax_tmp() {
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
        assertEquals(Long.MIN_VALUE, instance.invokeMember("wrap").asLong(), "Long overflow should wrap around.");
    }

    @Test
    @Disabled("Re-enable once constant folding is implemented and the PoC supports it")
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
        assertEquals(Long.MAX_VALUE, instance.invokeMember("getMaxConst").asLong());
        assertEquals(Long.MIN_VALUE, instance.invokeMember("getMinConst").asLong());
        assertEquals(Long.MIN_VALUE, instance.invokeMember("wrap").asLong(), "Long overflow should wrap around.");
    }

    @Test
    void testExpression() {
        String source = """
            class MinMaxTest {
            Long val() { ^ 42 }
            Long val2() { ^ 40 + 2 }
            Long val3() { ^ true ? 42 : 0 }
            Long val4() { ^ false ? 42 : 0 }
            Long val5() { ^ null ?? 42 }
            Long val6() { ^ (2 * 1) + (20 * 2)}
            Long val6() { ^ (2 ** 2) + (19 * 2)}
            Int int() { ^ 42 }
            
            }""";
        Value meta = eval(source);
        Value instance = meta.invokeMember("new");  
        assertEquals(42L, instance.invokeMember("val").asLong());
        assertEquals(42L, instance.invokeMember("val2").asLong());
        assertEquals(42L, instance.invokeMember("val3").asLong());
        assertEquals(0L, instance.invokeMember("val4").asLong());
        assertEquals(42L, instance.invokeMember("val5").asLong());
        assertEquals(42L, instance.invokeMember("val6").asLong());
        assertEquals(42L, instance.invokeMember("val6").asLong());
        assertEquals(42L, instance.invokeMember("int").asLong());
    }

    @Test
    @Disabled("Pending implementation of the core protocol in JolkNothing.") 
    void testFlowControlMessages() {
        // #ifPresent should not execute its closure for a null receiver.
        Value ifPresentResult = eval("x = 1; null #ifPresent [ x = 2 ]; ^x");
        assertEquals(1L, ifPresentResult.asLong(), "The #ifPresent block should not execute on null.");

        // #ifEmpty should execute its closure for a null receiver.
        Value ifEmptyResult = eval("x = 1; null #ifEmpty [ x = 2 ]; ^x");
        assertEquals(2L, ifEmptyResult.asLong(), "The #ifEmpty block should execute on null.");
    }
}
