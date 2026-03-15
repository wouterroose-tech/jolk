package tolk.parser;

import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import tolk.JolcTestBase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// 
/// Verifies the language's behavior when defining classes.
///
public class JolcClassTest extends JolcTestBase {


    private Value eval(String className, String source) {
        // Value is a polyglot representation of the result of evaluating the class definition.
        Value result = eval(source);
        // The result should not be null, it should be a meta-object representing the class.
        assertFalse(result.isNull());
        // A class definition should evaluate to a meta-object representing the class, not a host object.
        assertTrue(result.isMetaObject());
        assertFalse(result.isHostObject());
        // The class should have no members since it's empty.
        assertFalse(result.hasMembers());
        assertEquals(className, result.getMetaQualifiedName());
        return result;
    }
    @Test
    public void testEmptyClassDefinitionEvaluatesToClassObject() {
        String className = "MyFirstJolkClass";
        String source = "class " + className + " { }";
        eval(className, source);
    }

    @Test
    public void testEmptyFinalClassDefinitionEvaluatesToClassObject() {
        String className = "MyFirstJolkClass";
        String source = "final class " + className + " { }";
        Value result = eval(className, source);
        // A final class should have a meta-object that indicates it's final.
        // is this the way to test it?
        assertTrue(result.hasMember("isFinal"));
        assertTrue(result.getMember("isFinal").asBoolean());
    }

    @Test
    @Disabled("Pending implementation of the parser and class declaration nodes.")
    void testVisitClassWithMethodAndField() {
        // This test is a placeholder for when fields and methods are implemented.
        eval("class MyClass { String name; Int calculate(Int a, Int b) { ^ a + b; } }");
    }
}
