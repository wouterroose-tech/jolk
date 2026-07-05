package tolk.runtime;

import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.Test;
import tolk.JolcTestBase;
import static org.junit.jupiter.api.Assertions.*;

/**
 * ## JolkDoubleTest
 *
 * Verifies the intrinsic behavior of the Jolk `Double` type.
 * This includes arithmetic operations, comparisons, and access to boundary constants.
 */
public class JolkDoubleTest extends JolcTestBase {

    @Test
    void testArithmetic() {
        String source = """
            class MathTest {
                Double add(Double a, Double b) { ^ a + b }
                Double sub(Double a, Double b) { ^ a - b }
                Double mul(Double a, Double b) { ^ a * b }
                Double div(Double a, Double b) { ^ a / b }
                Double pow(Double a, Double b) { ^ a ** b }
            }""";
        Value instance = eval(source).invokeMember("new");
        assertEquals(3.5, instance.invokeMember("add", 1.5, 2.0).asDouble());
        assertEquals(1.0, instance.invokeMember("sub", 3.0, 2.0).asDouble());
        assertEquals(6.0, instance.invokeMember("mul", 2.0, 3.0).asDouble());
        assertEquals(2.5, instance.invokeMember("div", 5.0, 2.0).asDouble());
        assertEquals(8.0, instance.invokeMember("pow", 2.0, 3.0).asDouble());
    }

    @Test
    void testComparison() {
        String source = """
            class CompTest {
                Double value = 3.14;
                Boolean gt(Double a, Double b) { ^ a > b }
                Boolean lt(Double a, Double b) { ^ a < b }
                Boolean ge(Double a, Double b) { ^ a >= b }
                Boolean le(Double a, Double b) { ^ a <= b }
                Boolean eq(Double a, Double b) { ^ a == b }
            }""";
        Value instance = eval(source).invokeMember("new");
        assertEquals(3.14, instance.invokeMember("value").asDouble());
        assertTrue(instance.invokeMember("gt", 10.5, 5.5).asBoolean());
        assertTrue(instance.invokeMember("lt", 5.5, 10.5).asBoolean());
        assertTrue(instance.invokeMember("ge", 10.5, 10.5).asBoolean());
        assertTrue(instance.invokeMember("le", 10.5, 10.5).asBoolean());
        assertTrue(instance.invokeMember("eq", 10.5, 10.5).asBoolean());
    }

    @Test
    void testConstants() {
        // Verify access to Meta-Object constants projected from java.lang.Double
        String source = """
            class ConstantTest {
                Double max() { ^ Double #MAX_VALUE }
                Double min() { ^ Double #MIN_VALUE }
                Double nan() { ^ Double #NaN }
                Double inf() { ^ Double #POSITIVE_INFINITY }
                Double ninf() { ^ Double #NEGATIVE_INFINITY }
                Double pi() { ^ Double #PI }
            }""";
        Value instance = eval(source).invokeMember("new");
        assertEquals(Double.MAX_VALUE, instance.invokeMember("max").asDouble());
        assertEquals(Double.MIN_VALUE, instance.invokeMember("min").asDouble());
        assertTrue(Double.isNaN(instance.invokeMember("nan").asDouble()));
        assertEquals(Double.POSITIVE_INFINITY, instance.invokeMember("inf").asDouble());
        assertEquals(Double.NEGATIVE_INFINITY, instance.invokeMember("ninf").asDouble());
        assertEquals(Math.PI, instance.invokeMember("pi").asDouble());
    }

    @Test
    void testObjectProtocol() {
        String source = """
            class ProtoTest {
                String str(Double d) { ^ d #toString }
                Long h(Double d) { ^ d #hash }
                Boolean present(Double d) { ^ d #isPresent }
            }""";
        Value instance = eval(source).invokeMember("new");
        assertEquals("42.5", instance.invokeMember("str", 42.5).asString());
        assertEquals((long)Double.valueOf(42.5).hashCode(), instance.invokeMember("h", 42.5).asLong());
        assertTrue(instance.invokeMember("present", 42.5).asBoolean());
    }

    @Test
    void testDoubleField() {
        String source = "class Container { Double val = 3.14; }";
        Value meta = eval(source);
        Value instance = meta.invokeMember("new");
        assertEquals(3.14, instance.invokeMember("val").asDouble());
        
        instance.invokeMember("val", 2.718);
        assertEquals(2.718, instance.invokeMember("val").asDouble());
    }

    @Test
    void testInstanceOf() {
        String source = "class TypeTest { Boolean isDouble(Object o) { ^ o #instanceOf(Double) #isPresent } }";
        Value instance = eval(source).invokeMember("new");
        
        // In the Tolk Engine, Double receivers resolve to the Number MetaClass archetype
        assertTrue(instance.invokeMember("isDouble", 3.14).asBoolean());
        assertFalse(instance.invokeMember("isDouble", "not a double").asBoolean());
    }

    @Test
    void testMathMethods() {
        String source = """
            class MathTest {
                Double random() { ^ Double #random }
                Long round(Double d) { ^ d #round }
            }""";
        Value instance = eval(source).invokeMember("new");
        
        // Random check: Verify range [0, 1)
        double rand = instance.invokeMember("random").asDouble();
        assertTrue(rand >= 0.0 && rand < 1.0, "Random should be in [0, 1)");
        
        // Round check: Verify instance message dispatch
        assertEquals(3L, instance.invokeMember("round", 3.14).asLong());
        assertEquals(4L, instance.invokeMember("round", 3.5).asLong());
    }
}
