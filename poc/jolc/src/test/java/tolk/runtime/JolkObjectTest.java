package tolk.runtime;

import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import tolk.JolcTestBase;
import java.util.function.Consumer;

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
    void testHash() {
        String source = "class HashTest {}";
        Value meta = eval(source);
        Value instance = meta.invokeMember("new");
        Value hash = instance.invokeMember("hash");
        
        assertTrue(hash.isNumber(), "Hash code should be a number.");
        // We can't guarantee non-zero, but typically it is.
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
    @Disabled("Pending implementation of Map support and project intrinsic")
    void testProject() {}

    @Test
    @Disabled("Pending complete method body parsing") 
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
        // Logic tests moved to JolkObjectTest or similar once parser supports statements
        Value x = meta.invokeMember("new");
        Value y = meta.invokeMember("new");

        // By default, equivalence (~~) should fall back to identity (==).
        assertTrue(x.invokeMember("~~", x).asBoolean(), "An object must be equivalent to itself.");
        assertFalse(x.invokeMember("~~", y).asBoolean(), "Two distinct objects should not be equivalent by default.");
    }
}
