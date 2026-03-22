package tolk.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import tolk.JolcTestBase;

/// 
/// Verifies the language's behavior when defining Int fields.
///
public class JolcIntTest extends JolcTestBase {

    @Test
    void testIntField() {
        // Verify that #new(arg) initializes the field 'val'
        String source = "class Container { Int val; }";
        Value meta = eval(source);
        
        // getter for Object field
        Value instance = meta.invokeMember("new");
        assertEquals(0, instance.invokeMember("val").asInt(), "The getter should return the integer value stored in the Object field.");
        instance.invokeMember("val", 1);
        assertEquals(1, instance.invokeMember("val").asInt(), "The getter should return the integer value stored in the Object field.");

        // Canonical #new
        instance = meta.invokeMember("new", 42);
        assertEquals(42, instance.invokeMember("val").asInt(), "Canonical #new should initialize fields in order.");
    }

    /// Verify that Int fields can be initialized with expressions and that they evaluate correctly.
     @Test
     void testIntExpression() {
        String source = "class ExprTest { Int run() { ^ 40 + 1 ** 2  * -2 * -1 } }";
        Value result = eval(source).invokeMember("new").invokeMember("run");
        assertEquals(42, result.asInt(), "The expression should evaluate to the correct integer value.");
     }

    @Test
    void testArithmetic() {
        String source = "class MathTest { " +
                "Int add(Int a, Int b) { ^ a + b } " +
                "Int sub(Int a, Int b) { ^ a - b } " +
                "Int mul(Int a, Int b) { ^ a * b } " +
                "Int div(Int a, Int b) { ^ a / b } " +
                "Int mod(Int a, Int b) { ^ a % b } " +
                "Int pow(Int a, Int b) { ^ a ** b } }";
        Value instance = eval(source).invokeMember("new");
        assertEquals(30, instance.invokeMember("add", 10, 20).asInt());
        assertEquals(10, instance.invokeMember("sub", 30, 20).asInt());
        assertEquals(200, instance.invokeMember("mul", 10, 20).asInt());
        assertEquals(5, instance.invokeMember("div", 10, 2).asInt());
        assertEquals(1, instance.invokeMember("mod", 10, 3).asInt());
        assertEquals(8, instance.invokeMember("pow", 2, 3).asInt());
    }

    @Test
    void testComparison() {
        String source = "class CompTest { " +
                "Boolean gt(Int a, Int b) { ^ a > b } " +
                "Boolean lt(Int a, Int b) { ^ a < b } " +
                "Boolean ge(Int a, Int b) { ^ a >= b } " +
                "Boolean le(Int a, Int b) { ^ a <= b } " +
                "Boolean eq(Int a, Int b) { ^ a == b } " +
                "Boolean ne(Int a, Int b) { ^ a != b } }";
        Value instance = eval(source).invokeMember("new");
        assertTrue(instance.invokeMember("gt", 10, 5).asBoolean());
        assertTrue(instance.invokeMember("lt", 5, 10).asBoolean());
        assertTrue(instance.invokeMember("ge", 10, 10).asBoolean());
        assertTrue(instance.invokeMember("le", 10, 10).asBoolean());
        assertTrue(instance.invokeMember("eq", 10, 10).asBoolean());
        assertTrue(instance.invokeMember("ne", 10, 5).asBoolean());
    }

    @Test
    void testObjectProtocol() {
        String source = "class ProtoTest { " +
                "String str(Int i) { ^ i #toString } " +
                "Int h(Int i) { ^ i #hash } " +
                "Boolean eq(Int a, Int b) { ^ a ~~ b } " +
                "Boolean present(Int i) { ^ i #isPresent } " +
                "Boolean empty(Int i) { ^ i #isEmpty } }";
        Value instance = eval(source).invokeMember("new");
        assertEquals("42", instance.invokeMember("str", 42).asString());
        assertEquals(42, instance.invokeMember("h", 42).asInt());
        assertTrue(instance.invokeMember("eq", 42, 42).asBoolean());
        assertTrue(instance.invokeMember("present", 42).asBoolean());
        assertFalse(instance.invokeMember("empty", 42).asBoolean());
    }

    @Test
    @Disabled("Default field values are not yet supported.")
    void testIntFieldWithDefault() {
        String source = "class Container { Int val = 42; }";
        Value meta = eval(source);
        Value instance = meta.invokeMember("new");  
        assertEquals(42, instance.invokeMember("val").asInt(), "The field should be initialized to the default value.");
    }
}
