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
        // An empty class should still have the default members from Object, as well as the class-specific members.
        assertTrue(result.hasMembers());
        assertEquals(4, result.getMemberKeys().size());
        assertTrue(result.hasMember("new"));
        assertTrue(result.hasMember("name"));
        assertTrue(result.hasMember("superclass"));
        assertTrue(result.hasMember("isInstance"));
        
        Value instance = result.invokeMember("new");
        // An instance of an empty class should still have the default members from Object.
        assertTrue(instance.hasMembers());
        assertTrue(instance.hasMember("=="));
        assertTrue(instance.hasMember("!="));
        assertTrue(instance.hasMember("~~"));
        assertTrue(instance.hasMember("hash"));
        assertTrue(instance.hasMember("toString"));
        assertTrue(instance.hasMember("ifPresent"));
        assertTrue(instance.hasMember("ifEmpty"));  
        assertTrue(instance.hasMember("isPresent"));
        assertTrue(instance.hasMember("isEmpty"));
        assertTrue(instance.hasMember("class"));
        assertTrue(instance.hasMember("instanceOf"));
    }

    @Test
    void testClassWithMethod() {
        String className = "MyClass";
        String source = "final class " + className + " { Self me() { ^ self; } }";
        Value meta = eval(className, source);
        Value instance = meta.invokeMember("new");
        assertTrue(instance.hasMembers());
        assertTrue(instance.hasMember("me"));
        // TODO assert ...
    }

    @Test
    void testClassWithField() {
        String className = "MyClass";
        String source = "class " + className + " { String name; }";
        Value meta = eval(className, source);
        Value instance = meta.invokeMember("new");
        assertTrue(instance.hasMembers());
        assertTrue(instance.hasMember("name"), "Instance should have member 'name' from its field.");
        // TODO assert ...
    }

    @Test
    void testClassWithMethodAndField() {
        String className = "MyClass";
        String source = "final class " + className + " { String name; String name() { ^ name; } }";
        Value meta = eval(className, source);
        assertEquals(4, meta.getMemberKeys().size());

        Value instance = meta.invokeMember("new");
        assertTrue(instance.hasMember("name"));
        // TODO assert ...
    }

    @Test
    void testClassWithMethodAndField_2() {
        String className = "MyClass";
        String source = "final class " + className + " { String name; String myName() { ^ name; } }";
        Value meta = eval(className, source);
        Value instance = meta.invokeMember("new");
        assertTrue(instance.hasMembers());
        assertTrue(instance.hasMember("name"));
        assertTrue(instance.hasMember("myName"));
        // TODO assert ...
    }

    @Test
    void testCreationMethod_basic() {
        String className = "MyClass";
        String source = "final class " + className + " { meta Self new(Object arg) { ^ super #new }  }";
        Value result = eval(className, source);
        assertTrue(result.hasMembers());
        assertEquals(4, result.getMemberKeys().size());
        assertTrue(result.hasMember("new"));
    }

    @Test
    void testCreationMethod_variadic() {
        String className = "MyClass";
        String source = "final class " + className + " { meta Self new(Object ... args) { ^ super #new } }";
        Value result = eval(className, source);
        assertTrue(result.hasMembers());
        assertEquals(4, result.getMemberKeys().size());
        assertTrue(result.hasMember("new"));
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
        Value instance = meta.invokeMember("new");
        assertTrue(instance.hasMembers());
        assertTrue(instance.hasMember("~~"));
        assertTrue(instance.hasMember("x"));
        assertTrue(instance.hasMember("y"));
    }
}
