package tolk.runtime;

import com.oracle.truffle.api.interop.InteropLibrary;
import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import tolk.JolcTestBase;
import java.util.Collections;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

/// ## JolkObjectTest
///
/// Validates the core protocol defined in `jolk.lang.Object`, which serves as the
/// root for all objects in the Jolk ecosystem. These tests ensure that fundamental
/// behaviors like identity, equivalence, and flow control are correctly implemented.
///
public class JolkObjectTest extends JolcTestBase {
    
    @Test
    void testIdentityOperators() {
        String source = "class TestIdentity {}";
        Value meta = eval(source);
        Value x = meta.invokeMember("new");
        Value y = meta.invokeMember("new");

        assertTrue(x.invokeMember("==", x).asBoolean(), "An object must be identical to itself.");
        assertFalse(x.invokeMember("==", y).asBoolean(), "Two distinct objects should not be identical.");
        assertTrue(x.invokeMember("!=", y).asBoolean(), "Two distinct objects should be non-identical.");
    }

    @Test
    void testDefaultEquivalenceIsIdentity() {
        String source = "class TestEquiv {}";
        Value meta = eval(source);
        Value x = meta.invokeMember("new");
        Value y = meta.invokeMember("new");

        // By default, equivalence (~~) should fall back to identity (==).
        assertTrue(x.invokeMember("~~", x).asBoolean(), "An object must be equivalent to itself.");
        assertFalse(x.invokeMember("~~", y).asBoolean(), "Two distinct objects should not be equivalent by default.");

        // Non-equivalence (!~) should be the logical opposite.
        assertFalse(x.invokeMember("!~", x).asBoolean(), "An object must not be non-equivalent to itself.");
        assertTrue(x.invokeMember("!~", y).asBoolean(), "Two distinct objects should be non-equivalent by default.");
    }

    @Test
    void testFlowControlMessagesOnObject() {
        // The #ifPresent block should execute for a valid object.
        String source = "class FlowControlTest {}";
        Value meta = eval(source);
        Value obj = meta.invokeMember("new");
        
        boolean[] executed = {false};
        Consumer<Object> action = (o) -> { executed[0] = true; };

        // #ifPresent takes a closure (executable) and passes 'self'
        obj.invokeMember("ifPresent", action);
        assertTrue(executed[0], "The #ifPresent block should execute on a non-null object.");

        // #ifEmpty takes a closure but should NOT execute it
        executed[0] = false;
        obj.invokeMember("ifEmpty", action);
        assertFalse(executed[0], "The #ifEmpty block should not execute on a non-null object.");
    }

    @Test
    void testFlowControlReturnValues() {
        String source = "class FlowReturnTest {}";
        Value meta = eval(source);
        Value obj = meta.invokeMember("new");

        // #ifPresent returns the result of the closure
        Function<Object, String> action = (o) -> "Executed";
        Value result = obj.invokeMember("ifPresent", action);
        assertEquals("Executed", result.asString(), "ifPresent should return the result of the action.");

        // #ifEmpty returns 'self' (the object)
        Value resultEmpty = obj.invokeMember("ifEmpty", action);
        assertEquals(obj, resultEmpty, "ifEmpty should return self for non-null objects.");
    }

    @Test
    void testHash() {
        String source = "class HashTest {}";
        Value meta = eval(source);
        Value instance = meta.invokeMember("new");
        Value hash = instance.invokeMember("hash");
        
        assertTrue(hash.isNumber(), "Hash code should be a number.");
        // We can't guarantee non-zero, but typically it is.
    }

    @Test
    void testHashConsistency() {
        String source = "class HashConsistencyTest {}";
        Value meta = eval(source);
        Value obj = meta.invokeMember("new");

        long h1 = obj.invokeMember("hash").asLong();
        long h2 = obj.invokeMember("hash").asLong();

        assertEquals(h1, h2, "Hash code must be consistent for the same object.");
    }

    @Test
    void testToString() {
        String source = "class StringTest {}";
        Value meta = eval(source);
        Value instance = meta.invokeMember("new");
        Value str = instance.invokeMember("toString");
        
        assertTrue(str.isString(), "toString should return a string.");
        assertEquals("instance of StringTest", str.asString());
    }

    @Test
    void testIsPresent() {
        String source = "class PresenceTest {}";
        Value meta = eval(source);
        Value instance = meta.invokeMember("new");
        
        assertTrue(instance.invokeMember("isPresent").asBoolean(), "A JolkObject should be present.");
    }

    @Test
    void testIsEmpty() {
        String source = "class EmptyTest {}";
        Value meta = eval(source);
        Value instance = meta.invokeMember("new");
        
        assertFalse(instance.invokeMember("isEmpty").asBoolean(), "A JolkObject should not be empty.");
    }

    @Test
    void testClassAccessor() {
        String source = "class ClassAccessTest {}";
        Value meta = eval(source);
        Value instance = meta.invokeMember("new");
        Value clazz = instance.invokeMember("class");
        
        assertTrue(clazz.isMetaObject(), "The result of #class should be a meta object.");
        assertEquals("ClassAccessTest", clazz.getMetaSimpleName());
    }

    @Test
    void testInstanceOf() {
        String source = "class InstanceTest {}";
        Value meta = eval(source);
        Value instance = meta.invokeMember("new");
        
        Value match = instance.invokeMember("instanceOf", meta);
        assertTrue(match.toString().contains("Match("), "Should return a successful Match containing the instance");
        
        Value otherMeta = eval("class OtherType {}");
        Value noMatch = instance.invokeMember("instanceOf", otherMeta);
        assertEquals("Match.empty", noMatch.toString(), "Should return Match.empty for unrelated types");
    }

    @Test
    void testMetaObjectInterop() {
        String source = "class MetaTest {}";
        Value meta = eval(source);
        Value instance = meta.invokeMember("new");

        assertFalse(instance.getMetaObject().isNull(), "Instance should report having a meta-object.");
        assertEquals(meta, instance.getMetaObject(), "The meta-object should be the JolkMetaClass.");
    }

    @Test
    void testGetMembersComposition() throws Exception {
        // Define a class with one field and one method
        Map<String, Object> fields = Collections.singletonMap("data", "Long");
        Map<String, Object> methods = Collections.singletonMap("perform", JolkNothing.INSTANCE);
        JolkMetaClass meta = new JolkMetaClass("CompTest", null, JolkFinality.OPEN, JolkVisibility.PUBLIC, JolkArchetype.CLASS, methods, fields, Collections.emptyMap());
        
        JolkObject obj = new JolkObject(meta);
        InteropLibrary interop = InteropLibrary.getUncached();
        
        Object members = interop.getMembers(obj);
        assertTrue(interop.hasArrayElements(members));
        
        boolean foundField = false;
        boolean foundMethod = false;
        boolean foundIntrinsic = false;

        long size = interop.getArraySize(members);
        for (long i = 0; i < size; i++) {
            String name = (String) interop.readArrayElement(members, i);
            if ("data".equals(name)) foundField = true;
            if ("perform".equals(name)) foundMethod = true;
            if ("hash".equals(name)) foundIntrinsic = true;
        }

        assertTrue(foundField, "Should include instance fields");
        assertTrue(foundMethod, "Should include instance methods");
        assertTrue(foundIntrinsic, "Should include intrinsic protocol members");
    }

    @Test
    void testConstructorFallbackToDefaults() {
        Map<String, Object> fields = Collections.singletonMap("val", "Long");
        JolkMetaClass meta = new JolkMetaClass("CtorTest", null, JolkFinality.OPEN, JolkVisibility.PUBLIC, JolkArchetype.CLASS, Collections.emptyMap(), fields, Collections.emptyMap());
        
        // Provide wrong number of arguments (arity mismatch for canonical #new)
        // JolkObject constructor should fall back to defaults
        JolkObject obj = new JolkObject(meta, new Object[]{10L, 20L}); 
        assertEquals(0L, obj.getFieldValue(0), "Should fall back to default field values on arity mismatch");
    }

    @Test
    void testResultRestitution() throws Exception {
        eval("");
        context.enter();
        try {
            var lang = tolk.language.JolkLanguage.getLanguage();
            // Create a method that returns raw Java null
            JolkClosure nullReturner = new JolkClosure(new JolkClosureTest.TestRootNode(lang, args -> null).getCallTarget());
            Map<String, Object> members = Collections.singletonMap("getNull", nullReturner);
            JolkMetaClass meta = new JolkMetaClass("RestitutionTest", JolkFinality.OPEN, JolkVisibility.PUBLIC, JolkArchetype.CLASS, members);
            
            JolkObject obj = new JolkObject(meta);
            Object result = InteropLibrary.getUncached().invokeMember(obj, "getNull");
            
            assertEquals(JolkNothing.INSTANCE, result, "Raw null results from methods must be lifted to JolkNothing.");
        } finally {
            context.leave();
        }
    }

    @Test
    void testIntrinsicArityChecks() {
        String source = "class ArityTest {}";
        Value meta = eval(source);
        Value obj = meta.invokeMember("new");
        Value other = meta.invokeMember("new");
        // ==
        assertThrows(Exception.class, () -> obj.invokeMember("=="));
        assertThrows(Exception.class, () -> obj.invokeMember("==", other, other));
        // !=
        assertThrows(Exception.class, () -> obj.invokeMember("!="));
        assertThrows(Exception.class, () -> obj.invokeMember("!=", other, other));
        // ~~
        assertThrows(Exception.class, () -> obj.invokeMember("~~"));
        assertThrows(Exception.class, () -> obj.invokeMember("~~", other, other));
        // !~
        assertThrows(Exception.class, () -> obj.invokeMember("!~"));
        assertThrows(Exception.class, () -> obj.invokeMember("!~", other, other));
        // ??
        assertThrows(Exception.class, () -> obj.invokeMember("??"));
        assertThrows(Exception.class, () -> obj.invokeMember("??", other, other));
        // hash
        assertThrows(Exception.class, () -> obj.invokeMember("hash", 1));
        // toString
        assertThrows(Exception.class, () -> obj.invokeMember("toString", 1));
        // ifPresent
        assertThrows(Exception.class, () -> obj.invokeMember("ifPresent"));
        assertThrows(Exception.class, () -> obj.invokeMember("ifPresent", other, other));
        // ifEmpty
        assertThrows(Exception.class, () -> obj.invokeMember("ifEmpty"));
        // isPresent
        assertThrows(Exception.class, () -> obj.invokeMember("isPresent", 1));
        // isEmpty
        assertThrows(Exception.class, () -> obj.invokeMember("isEmpty", 1));
        // class
        assertThrows(Exception.class, () -> obj.invokeMember("class", 1));
        // instanceOf
        assertThrows(Exception.class, () -> obj.invokeMember("instanceOf"));
    }

    @Test
    void testOverriddenEquivalence() {
        // Define a class that overrides the equivalence operator '~~'.
        String source = """
            class MyClass {
                Boolean ~~(Object other) {
                    (self == other) ? [ ^true ];
                    ^ false
                }
            }
        """;
        Value meta = eval(source);
        // Logic tests moved to JolkObjectTest or similar once parser supports statements
        Value x = meta.invokeMember("new");
        Value y = meta.invokeMember("new");

        // By default, equivalence (~~) should fall back to identity (==).
        assertTrue(x.invokeMember("~~", x).asBoolean(), "An object must be equivalent to itself.");
        assertFalse(x.invokeMember("~~", y).asBoolean(), "Two distinct objects should not be equivalent by default.");
    }

    @Test
    void testOverriddenEquivalence_2() {
        // Define a class that overrides the equivalence operator '~~'.
        String source = """
            class Point {
                Long x;
                Long y;

                Boolean ~~(Object other) {
                    (self == other) ? [ ^true ];
                    other #instanceOf(Point) #ifPresent [ p ->
                        ^ (self #x == p #x) && (self #y == p #y)
                    ];
                    ^ false
                }
            }
        """;
        Value meta = eval(source);
        // Logic tests moved to JolkObjectTest or similar once parser supports statements
        Value x = meta.invokeMember("new", 0L, 1L);
        assertEquals(0L, x.invokeMember("x").asLong());
        assertEquals(1L, x.invokeMember("y").asLong());

        Value y = meta.invokeMember("new", 1L, 0L);
        assertEquals(1L, y.invokeMember("x").asLong());
        assertEquals(0L, y.invokeMember("y").asLong());

        // By default, equivalence (~~) should fall back to identity (==).
        assertTrue(x.invokeMember("~~", x).asBoolean(), "An object must be equivalent to itself.");
        assertFalse(x.invokeMember("~~", y).asBoolean(), "Two distinct objects should not be equivalent by default.");
    }

    @Test
    void testPresence() {
        // Define a class that overrides the equivalence operator '~~'.
        String source = """
            class MyClass {
                Long val() { 42 #ifPresent [ x -> ^ x ]; ^ 0 }
                Long val2() { null #ifPresent [ x -> ^ x ]; ^ 0 }
                Long val3() { 42 #ifEmpty [ ^ 42 ]; ^ 0 }
                Long val4() { null #ifEmpty [ ^ 42 ]; ^ 0 }
                Long val5() { ^ 42 #isPresent ? 42 : ^ 0 }
                Long val6() { ^ null #isPresent ? 42 : ^ 0 }
                Long val7() { ^ 42 #isEmpty ? 42 : ^ 0 }
                Long val8() { ^ null #isEmpty ? 42 : ^ 0 }
            }
        """;
        Value meta = eval(source);
        // Logic tests moved to JolkObjectTest or similar once parser supports statements
        Value x = meta.invokeMember("new");

        assertEquals(42L, x.invokeMember("val").asLong());
        assertEquals(0L, x.invokeMember("val2").asLong());
        assertEquals(0L, x.invokeMember("val3").asLong());
        assertEquals(42L, x.invokeMember("val4").asLong());
        assertEquals(42L, x.invokeMember("val5").asLong());
        assertEquals(0L, x.invokeMember("val6").asLong());
        assertEquals(0L, x.invokeMember("val7").asLong());
        assertEquals(42L, x.invokeMember("val8").asLong());
    }

    /**
     * ### testAsProjection
     * 
     * Verifies the '#as' projection (Safe Casting). 
     * It should return a Match identity.
     */
    @Test
    @Disabled("Pending implemetation of asProjection")
    void testAsProjection() {
        String source = "class TargetType { Long id = 42; }";
        Value meta = eval(source);
        Value instance = meta.invokeMember("new");

        Value match = instance.invokeMember("as", meta);
        assertTrue(match.invokeMember("isPresent").asBoolean());
        assertEquals(42L, match.invokeMember("get").invokeMember("id").asLong());

        Value noMatch = instance.invokeMember("as", eval("class WrongType {}"));
        assertFalse(noMatch.invokeMember("isPresent").asBoolean());
    }

    /**
     * ### testProject
     * 
     * Verifies mass-assignment via the '#project' message using a Map.
     */
    @Test
    @Disabled("Pending implemetation of asProjection")
    void testProject() {
        String source = "class Projectable { String name; Long age; }";
        Value instance = eval(source).invokeMember("new");
        
        // In a real Jolk environment, this would be a Jolk Map #( ... )
        // For the test, we simulate the interop call.
        java.util.Map<String, Object> data = java.util.Map.of("name", "Jolk", "age", 1L);
        instance.invokeMember("project", data);

        assertEquals("Jolk", instance.invokeMember("name").asString());
        assertEquals(1L, instance.invokeMember("age").asLong());
    }

    @Test
    void testNothingAdheresToObjectProtocol() {
        Value nothing = eval("class Wrapper { Object get() { ^ null } }").invokeMember("new").invokeMember("get");
        
        assertEquals("null", nothing.invokeMember("toString").asString());
        assertTrue(nothing.invokeMember("hash").isNumber());
        assertTrue(nothing.invokeMember("isEmpty").asBoolean());
    }
}
