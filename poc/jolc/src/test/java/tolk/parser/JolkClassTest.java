package tolk.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import tolk.JolcTestBase;

/// 
/// Verifies the language's behavior when defining classes.
///
public class JolkClassTest extends JolcTestBase {

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
        // 4 base meta-members (new, name, superclass, isInstance) + 18 Jolk Object Protocol intrinsics
        assertEquals(21, result.getMemberKeys().size());
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
    void testClassWithMethodWithExplicitSelfReturn() {
        String className = "MyClass";
        String source = "final class " + className + " { Self me() { ^ self } }";
        Value meta = eval(className, source);
        Value instance = meta.invokeMember("new");
        assertTrue(instance.hasMembers());
        assertTrue(instance.hasMember("me"));
        assertEquals(instance, instance.invokeMember("me"), "The explicit method 'me' should return 'self'.");
    }

    @Test
    void testClassWithMethodWithSynthesizedSelfReturn() {
        String className = "MyClass";
        String source = "final class " + className + " { Self me() {  } }";
        Value meta = eval(className, source);
        Value instance = meta.invokeMember("new");
        assertTrue(instance.hasMembers());
        assertTrue(instance.hasMember("me"));
    }

    @Test
    void testClassWithMethodWithSynthesizedSelfReturn_TODO() {
        String className = "MyClass";
        String source = "final class " + className + " { me() {  } }";
        Value meta = eval(className, source);
        Value instance = meta.invokeMember("new");
        assertTrue(instance.hasMembers());
        assertTrue(instance.hasMember("me"));
        assertEquals(instance, instance.invokeMember("me"), "The explicit method 'me' should return 'self'.");
    }

    @Test
    void testMethods() {
        String source = """
            class MyClass {
                Self me() { ^ self }
                Boolean exist() { ^ true }
                Long x() { ^ 42 } 
                Long y(Long y) { ^ y } 
            }""";
        Value meta = eval(source);
        Value instance = meta.invokeMember("new");
        assertTrue(instance.hasMember("me")); 
        assertTrue(instance.hasMember("exist"));
        assertTrue(instance.hasMember("x"));
        assertTrue(instance.hasMember("y"));
        
        assertEquals(instance, instance.invokeMember("me"));
        assertTrue(instance.invokeMember("exist").asBoolean());
        assertEquals(42L, instance.invokeMember("x").asLong());
        assertEquals(42L, instance.invokeMember("y", 42L).asLong());
    }

    @Test
    void testClassWithField() {
        String className = "MyClass";
        String source = "class " + className + " { String myField; }";
        Value meta = eval(className, source);
        Value instance = meta.invokeMember("new");
        assertTrue(instance.hasMembers());
        assertTrue(instance.hasMember("myField"), "Instance should have member 'myField' from its field.");
        
        // Test synthesized setter and getter
        instance.invokeMember("myField", "Jolk");
        assertEquals("Jolk", instance.invokeMember("myField").asString(), "Synthesized accessor should store and retrieve value.");
    }

    @Test
    void testClassWithMethodAndField() {
        String className = "MyClass";
        String source = "final class " + className + " { String name; String myName() { ^ name; } }";
        Value meta = eval(className, source);
        Value instance = meta.invokeMember("new");
        assertTrue(instance.hasMembers());
        assertTrue(instance.hasMember("name"));
        assertTrue(instance.hasMember("myName"));
        
        // Use synthesized setter on the field
        instance.invokeMember("name", "Jolk");
        assertEquals("Jolk", instance.invokeMember("myName").asString(), "Synthesized accessor should store and retrieve value.");
    }

    @Test
    void testCreationMethod_basic() {
        String className = "MyClass";
        String source = "final class " + className + " { meta Self new(Object arg) { ^ super #new }  }";
        Value result = eval(className, source);
        assertTrue(result.hasMembers());
        // 4 base meta-members (new, name, superclass, isInstance) + 18 Jolk Object Protocol intrinsics
        assertEquals(21, result.getMemberKeys().size());
        assertTrue(result.hasMember("new"));
    }

    @Test
    void testCreationMethod_variadic() {
        String className = "MyClass";
        String source = "final class " + className + " { meta Self new(Object ... args) { ^ super #new } }";
        Value result = eval(className, source);
        assertTrue(result.hasMembers());
        // 4 base meta-members (new, name, superclass, isInstance) + 18 Jolk Object Protocol intrinsics
        assertEquals(21, result.getMemberKeys().size());
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
    
    @Test
    void testSynthesizedAccessors() {
        // Define a class with a field 'x'.
        String source = "class MyClass { Object x; }";
        Value meta = eval(source);
        Value instance = meta.invokeMember("new");
        // The instance should have synthesized accessors for the field 'x'.
        assertTrue(instance.hasMember("x"));
        
        // Verify Setter: #x(value) -> returns self
        Value result = instance.invokeMember("x", "testValue");
        assertEquals(instance, result, "The synthesized setter should return 'self' for fluent chaining.");

        // Verify Getter: #x-> returns value
        Value value = instance.invokeMember("x");
        assertEquals("testValue", value.asString(), "The synthesized getter should return the stored value.");
    }

    @Test
    void testSingleFieldCanonicalNew() {
        // Verify that #new(arg) initializes the field 'val'
        String source = "class Container { Object val; }";
        Value meta = eval(source);
        
        // Canonical #new
        Value instance = meta.invokeMember("new", "initial");
        
        assertTrue(instance.hasMember("val"));
        assertEquals("initial", instance.invokeMember("val").asString(), "Canonical #new should initialize fields in order.");
    }
    
    @Test
    void testMultiFieldCanonicalNew() {
        // Verify that #new(arg1, arg2, ...) initializes fields in definition order
        String source = "class Point3D { Object x; Object y; Object z; }";
        Value meta = eval(source);
        
        // Canonical #new with 3 arguments
        Value instance = meta.invokeMember("new", 10, 20, 30);
        
        assertEquals(10, instance.invokeMember("x").asInt(), "Field 'x' should be initialized to first argument.");
        assertEquals(20, instance.invokeMember("y").asInt(), "Field 'y' should be initialized to second argument.");
        assertEquals(30, instance.invokeMember("z").asInt(), "Field 'z' should be initialized to third argument.");
    }

}
