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
        assertEquals(className, result.getMetaQualifiedName());
        return result;
    }
    @Test
    public void testEmptyClass() {
        String className = "MyFirstJolkClass";
        String source = "class " + className + " { }";
        Value result = eval(className, source);
        // An empty, non-final class should have no members.
        // TODO Pending implementation of members
        // assertFalse(result.hasMembers());
    }

    @Test
    @Disabled("Pending implementation of methods")
    void testClassWithField() {
        String className = "MyFirstJolkClass";
        String source = "final class " + className + " { self me() { ^ self; } }";
        Value result = eval(className, source);
        assertFalse(result.hasMembers());
        // TODO assert ...
    }

    @Test
    @Disabled("Pending implementation of fields and methods")
    void testClassWithMethodAndField() {
        String className = "MyFirstJolkClass";
        String source = "final class " + className + " { String name; String me() { ^ name; } }";
        Value result = eval(className, source);
        assertFalse(result.hasMembers());
        // TODO assert ...
    }

    @Test
    @Disabled("Pending implementation final ")
    public void testEmptyFinalClass() {
        String className = "MyFirstJolkClass";
        String source = "final class " + className + " { }";
        Value result = eval(className, source);
        // A final class must have at least one method
        // this must result in a compilationerror?
        // TODO assert ...
    }

    @Test
    @Disabled("Pending implementation final ")
    public void testFinalClass() {
        String className = "MyFirstJolkClass";
        String source = "final class " + className + " { self me() { ^ self; } }";
        Value result = eval(className, source);
        // A final class should have a meta-object that indicates it's final.
        // TODO is this the way to test it?
        assertTrue(result.hasMember("isFinal"));
        assertTrue(result.getMember("isFinal").asBoolean());
        // TODO assert ...
    }
}
