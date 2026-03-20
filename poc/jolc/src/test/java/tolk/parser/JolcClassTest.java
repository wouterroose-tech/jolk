package tolk.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import tolk.JolcTestBase;

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
    
    // TODO test compilation errors for invalid class definitions
    // -> test here or in the truffle LSP layer? 
    // Myclass { } // missing class keyword
    // class Myclass // missing body
    // clazz Myclass { } // typo in class keyword

    @Test
    public void testEmptyClass() {
        String className = "MyFirstJolkClass";
        String source = "class " + className + " { }";
        Value result = eval(className, source);
        // An empty, non-final class should have no members.
        // TODO Pending implementation of members
        assertTrue(result.hasMembers());
    }

    @Test
    void testClassWithMethod() {
        String className = "MyClass";
        String source = "final class " + className + " { Self me() { ^ self; } }";
        Value result = eval(className, source);
        assertTrue(result.hasMembers());
        assertNotNull(result.getMember("me"));
        // TODO assert ...
    }

    @Test
    void testClassWithField() {
        String className = "MyClass";
        String source = "class " + className + " { String name; }";
        Value result = eval(className, source);
        assertTrue(result.hasMembers());
        assertNotNull(result.getMember("name"));
        // TODO assert ...
    }

    @Test
    void testClassWithMethodAndField() {
        String className = "MyClass";
        String source = "final class " + className + " { String name; String name() { ^ name; } }";
        Value result = eval(className, source);
        assertTrue(result.hasMembers());
        assertEquals(2, result.getMemberKeys().size());
        assertNotNull(result.getMember("name"));
        // TODO assert ...
    }

    @Test
    void testClassWithMethodAndField_2() {
        String className = "MyClass";
        String source = "final class " + className + " { String name; String myName() { ^ name; } }";
        Value result = eval(className, source);
        assertTrue(result.hasMembers());
        assertEquals(3, result.getMemberKeys().size());
        assertNotNull(result.getMember("name"));
        assertNotNull(result.getMember("myName"));
        // TODO assert ...
    }

    @Test
    @Disabled("Pending implementation of meta new methods")
    void testCreationMethod_basic() {
        String className = "MyClass";
        String source = "final class " + className + " { meta Self new(Object arg) { ^ super #new }  }";
        Value result = eval(className, source);
        assertTrue(result.hasMembers());
        // TODO assert ...
        // TODO assert #new
    }

    @Test
    @Disabled("Pending implementation of meta new methods")
    void testCreationMethod_variadic() {
        String className = "MyClass";
        String source = "final class " + className + " { meta Self new(Object ... args) { ^ super #new } }";
        Value result = eval(className, source);
        assertTrue(result.hasMembers());
        // TODO assert ...
        // TODO assert #new
    }

    @Test
    @Disabled("Pending implementation final ")
    public void testEmptyFinalClass() {
        String className = "MyClass";
        String source = "final class " + className + " { }";
        Value result = eval(className, source);
        // A final class must have at least one method
        // this must result in a compilationerror?
        // TODO assert ...
    }

    @Test
    @Disabled("Pending implementation final ")
    public void testFinalClass() {
        String className = "MyClass";
        String source = "final class " + className + " { self me() { ^ self; } }";
        Value result = eval(className, source);
        // A final class should have a meta-object that indicates it's final.
        // TODO is this the way to test it?
        assertTrue(result.hasMember("isFinal"));
        assertTrue(result.getMember("isFinal").asBoolean());
        // TODO assert ...
    }

    @Test
    void testOverriddenEquivalence() {
        // Define a class that overrides the equivalence operator '~~'.
        String source = """
            class Point {
                Int x;
                Int y;

                Boolean ~~(Object other) {
                    (self == other) ? [ ^true ];
                    other #as(Point) #ifPresent [ p ->
                        ^ (self #x == p #x) && (self #y == p #y)
                    ];
                    ^ false
                }
            }
        """;
        Value meta = eval(source);
        assertTrue(meta.hasMembers());
        assertEquals(4, meta.getMemberKeys().size());
        assertTrue(meta.hasMember("~~"));
        assertTrue(meta.hasMember("x"));
        assertTrue(meta.hasMember("y"));
        // TODO assert that the operator behaves as expected
    }
}
