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

    //TODO Visitor
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

    @Test
    @Disabled("Default field values are not yet supported.")
    void testIntFieldWithDefault() {
        String source = "class Container { Int val = 42; }";
        Value meta = eval(source);
        Value instance = meta.invokeMember("new");  
        assertEquals(42, instance.invokeMember("val").asInt(), "The field should be initialized to the default value.");
    }

    ///TODO test  expressions
    /// 
    /// Verify that Int fields can be initialized with expressions and that they evaluate correctly.
     @Test
     @Disabled("Expression evaluation in field initializers is not yet supported.")
     void testIntExpression() {
        String source = "40 + 2";
        Value meta = eval(source);
        Value result = meta.invokeMember("new");
        assertEquals(42, result, "The field should be initialized to the result of the expression.");

     }

}
