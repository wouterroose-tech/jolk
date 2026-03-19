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
        //assertFalse(result.hasMembers());
    }

    @Test
    @Disabled("Pending implementation of methods")
    void testCreationMethod_Default() {
        String className = "MyFirstJolkClass";
        String source = "final class " + className + " { self me() { ^ self; } }";
        Value result = eval(className, source);
        assertTrue(result.hasMembers());
        assertNotNull(result.getMember("me"));
        // TODO assert ...
    }

    @Test
    @Disabled("Pending implementation of methods")
    void testCreationMethod_basic() {
        String className = "MyFirstJolkClass";
        String source = "final class " + className + " { }";
        Value result = eval(className, source);
        assertFalse(result.hasMembers());
        // TODO assert ...
        // TODO assert #new
    }

    @Test
    @Disabled("Pending implementation of methods")
    void testCreationMethod_basic() {
        String className = "MyFirstJolkClass";
        String source = "final class " + className + " { meta Self new(Object arg) { ^ super #new }  }";
        Value result = eval(className, source);
        assertFalse(result.hasMembers());
        // TODO assert ...
        // TODO assert #new
    }

    @Test
    @Disabled("Pending implementation of methods")
    void testCreationMethod_variadic() {
        String className = "MyFirstJolkClass";
        String source = "final class " + className + " { meta Self new(Object ... args) { ^ super #new } }";
        Value result = eval(className, source);
        assertFalse(result.hasMembers());
        // TODO assert ...
        // TODO assert #new
    }

    @Test
    @Disabled("Pending implementation of methods")
    void testClassWithField() {
        String className = "MyFirstJolkClass";
        String source = "final class " + className + " { Self me() { ^ self; } }";
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
    @Test
    @Disabled("Pending implementation of the core protocol in JolkObject.") 
    void testIdentityOperators() {
        String source = """
            class TestObj {}
            x = TestObj #new;
            y = TestObj #new;
            #(
                "self_identity" -> (x == x),
                "other_identity" -> (x == y)
            )
        """;
        Value results = eval(source);
        assertTrue(results.getMember("self_identity").asBoolean(), "An object must be identical to itself.");
        assertFalse(results.getMember("other_identity").asBoolean(), "Two distinct objects should not be identical.");
    }

    @Test
    @Disabled("Pending implementation of the core protocol in JolkObject.") 
    void testDefaultEquivalenceIsIdentity() {
        String source = """
            class TestObj {}
            x = TestObj #new;
            y = TestObj #new;
            #(
                "self_equiv" -> (x ~~ x),
                "other_equiv" -> (x ~~ y)
            )
        """;
        Value results = eval(source);
        // By default, equivalence (~~) should fall back to identity (==).
        assertTrue(results.getMember("self_equiv").asBoolean(), "An object must be equivalent to itself.");
        assertFalse(results.getMember("other_equiv").asBoolean(), "Two distinct objects should not be equivalent by default.");
    }

    @Test
    @Disabled("Pending implementation of the core protocol in JolkObject.") 
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

            p1 = Point #new; p1 #x(10); p1 #y(20);
            p2 = Point #new; p2 #x(10); p2 #y(20);
            p3 = Point #new; p3 #x(0); p3 #y(0);

            #( "p1_vs_p2" -> (p1 ~~ p2), "p1_vs_p3" -> (p1 ~~ p3) )
        """;
        Value results = eval(source);
        assertTrue(results.getMember("p1_vs_p2").asBoolean(), "Two points with the same coordinates should be equivalent.");
        assertFalse(results.getMember("p1_vs_p3").asBoolean(), "Two points with different coordinates should not be equivalent.");
    }

    @Test
    @Disabled("Pending implementation of the core protocol in JolkObject.") 
    void testFlowControlMessagesOnObject() {
        // The #ifPresent block should execute for a valid object.
        Value ifPresentResult = eval("class O{} x = 1; obj = O #new; obj #ifPresent [ x = 2 ]; ^x");
        assertEquals(2, ifPresentResult.asInt(), "The #ifPresent block should execute on a non-null object.");

        // The #ifEmpty block should not execute.
        Value ifEmptyResult = eval("class O{} x = 1; obj = O #new; obj #ifEmpty [ x = 2 ]; ^x");
        assertEquals(1, ifEmptyResult.asInt(), "The #ifEmpty block should not execute on a non-null object.");
    }
    
}
