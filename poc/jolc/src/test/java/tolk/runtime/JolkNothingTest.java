package tolk.runtime;

import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.Test;
import tolk.JolcTestBase;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ## JolkNothingTest
 *
 * Validates the behavior of the 'Nothing' identity (null) in the Jolk ecosystem.
 * This test class covers both internal runtime behavior and guest-level 
 * message passing through source evaluation.
 */
public class JolkNothingTest extends JolcTestBase {

    private Value evalClass(String className, String source) {
        Value result = eval(source);
        assertFalse(result.isNull());
        assertTrue(result.isMetaObject());
        assertEquals(className, result.getMetaQualifiedName());
        return result;
    }

    // --- Source-Based Behavioral Tests ---

    @Test
    void testNullIsASingletonIdentity() {
        String source = "class NullTest { Object run() { ^ null } }";
        Value meta = evalClass("NullTest", source);
        Value nullValue = meta.invokeMember("new").invokeMember("run");
        // JolkNothing is a first-class object that supports messaging, so it is not a polyglot null.
        assertEquals("null", nullValue.toString());
    }

    @Test
    void testIfPresentFlow() {
        String source = """
            class IfEmptyTest {
                Object run() {
                    null #ifPresent [ ^1 ];
                    ^ null
                }
                Object runEmpty() {
                    null #ifEmpty [ ^1 ];
                    ^ null
                }
            }
        """;
        Value meta = evalClass("IfEmptyTest", source);
        Value instance = meta.invokeMember("new");

        // #ifPresent should not execute its closure for a null receiver.
        Value ifPresentResult = instance.invokeMember("run");
        assertEquals("null", ifPresentResult.toString());

        // #ifEmpty should execute its closure for a null receiver.
        Value ifEmptyResult = instance.invokeMember("runEmpty");
        assertFalse(ifEmptyResult.isNull());
        assertEquals(1L, ifEmptyResult.asLong());
    }

    @Test
    void testSafeNavigation() {
        // Sending an arbitrary message to 'null' should not cause a crash.
        // It should absorb the message and return 'null' itself, enabling fluid chains.
        String source = """
            class SilentTest {
                Object run() {
                    ^ null #someRandomMessage #anotherMessage
                }
            }
            """;
        Value instance = eval(source).invokeMember("new");
        Value result = instance.invokeMember("run");
        assertEquals("null", result.toString(), "Chaining messages on null should result in null (Silent Absorption).");
    }

    /**
     * Verifies that raw JVM `null` values returned from host (Java) interop calls
     * are correctly "lifted" to the Jolk `null` identity (Identity Restitution).
     */
    @Test
    void testHostNullRestitution() {
        String source = """
            class HostNullTest {
                Object callGet(Object host) {
                    ^ host #get
                }
            }
            """;
        Value meta = evalClass("HostNullTest", source);
        Value instance = meta.invokeMember("new");

        // Pass a Java host object that returns a raw null from its 'get' method.
        java.util.concurrent.atomic.AtomicReference<Object> hostRef = new java.util.concurrent.atomic.AtomicReference<>(null);
        Value result = instance.invokeMember("callGet", hostRef);

        assertNotNull(result, "Result wrapper should not be null.");
        assertEquals("null", result.toString(), "The restituted identity should have the string representation 'null'.");
        // Verify the archetype identity
        assertEquals("Nothing", result.invokeMember("class").getMetaSimpleName(), "Raw JVM null from host must be lifted to the Jolk 'Nothing' identity.");
    }

    // --- Runtime Internal Tests ---

    @Test
    void testInvokeIfEmpty() throws Exception {
        eval(""); // Initialize context and language
        context.enter();
        try {
        Object nothing = JolkNothing.INSTANCE;
        AtomicBoolean executed = new AtomicBoolean(false);
        TestExecutable action = new TestExecutable(() -> executed.set(true));

        // Execute: null #ifEmpty [ executed.set(true) ]
        InteropLibrary.getUncached().invokeMember(nothing, "ifEmpty", action);

        assertTrue(executed.get(), "Action should be executed for Nothing");
        } finally {
            context.leave();
        }
    }

    @Test
    void testInvokeIfPresent() throws Exception {
        eval("");
        context.enter();
        try {
        Object nothing = JolkNothing.INSTANCE;
        AtomicBoolean executed = new AtomicBoolean(false);
        TestExecutable action = new TestExecutable(() -> executed.set(true));

        InteropLibrary.getUncached().invokeMember(nothing, "ifPresent", action);

        assertFalse(executed.get(), "Action should NOT be executed for Nothing");
        } finally {
            context.leave();
        }
    }

    @Test
    void testEquivalence() throws Exception {
        Object nothing = JolkNothing.INSTANCE;
        JolkMetaClass meta = new JolkMetaClass("TestObject", JolkFinality.OPEN, JolkVisibility.PUBLIC, JolkArchetype.CLASS, Collections.emptyMap());
        Object other = new JolkObject(meta);
        
        assertTrue((Boolean) InteropLibrary.getUncached().invokeMember(nothing, "~~", nothing));
        assertFalse((Boolean) InteropLibrary.getUncached().invokeMember(nothing, "~~", other));
    }

    @Test
    void testNonEquivalence() throws Exception {
        Object nothing = JolkNothing.INSTANCE;
        JolkMetaClass meta = new JolkMetaClass("TestObject", JolkFinality.OPEN, JolkVisibility.PUBLIC, JolkArchetype.CLASS, Collections.emptyMap());
        Object other = new JolkObject(meta);

        assertFalse((Boolean) InteropLibrary.getUncached().invokeMember(nothing, "!~", nothing));
        assertTrue((Boolean) InteropLibrary.getUncached().invokeMember(nothing, "!~", other));
    }

    @Test
    void testHash() throws Exception {
        Object nothing = JolkNothing.INSTANCE;
        assertEquals(0L, InteropLibrary.getUncached().invokeMember(nothing, "hash"));
    }

    @Test
    void testToString() throws Exception {
        Object nothing = JolkNothing.INSTANCE;
        assertEquals("null", InteropLibrary.getUncached().invokeMember(nothing, "toString"));
    }

    @Test
    void testIsPresent() throws Exception {
        Object nothing = JolkNothing.INSTANCE;
        assertFalse((Boolean) InteropLibrary.getUncached().invokeMember(nothing, "isPresent"));
    }

    @Test
    void testIsEmpty() throws Exception {
        Object nothing = JolkNothing.INSTANCE;
        assertTrue((Boolean) InteropLibrary.getUncached().invokeMember(nothing, "isEmpty"));
    }

    @Test
    void testProject() throws Exception {
        Object nothing = JolkNothing.INSTANCE;
        // The argument to project is ignored by Nothing, but arity must be 1
        Object result = InteropLibrary.getUncached().invokeMember(nothing, "project", nothing);
        assertEquals(nothing, result, "Project on Nothing should return Nothing (silent absorption)");
    }
    
    /**
     * ### testSilentAbsorption
     * 
     * Verifies the "Neutral Response" model: any unknown message sent to Nothing 
     * should return Nothing itself.
     */
    @Test
    void testSilentAbsorption() throws Exception {
        Object nothing = JolkNothing.INSTANCE;
        Object result = InteropLibrary.getUncached().invokeMember(nothing, "someRandomMessageThatDoesNotExist");
        assertEquals(nothing, result, "Nothing should absorb unknown messages and return itself.");
    }

    @Test
    void testClass() throws Exception {
        Object nothing = JolkNothing.INSTANCE;
        Object type = InteropLibrary.getUncached().invokeMember(nothing, "class");
        assertEquals(JolkNothing.NOTHING_TYPE, type);
    }

    @Test
    void testInstanceOf() throws Exception {
        Object nothing = JolkNothing.INSTANCE;
        
        // Test against Nothing type
        Object match = InteropLibrary.getUncached().invokeMember(nothing, "instanceOf", JolkNothing.NOTHING_TYPE);
        assertTrue((Boolean) InteropLibrary.getUncached().invokeMember(match, "isPresent"));
        
        // Test against an unrelated type
        Object noMatch = InteropLibrary.getUncached().invokeMember(nothing, "instanceOf", JolkLongExtension.LONG_TYPE);
        assertFalse((Boolean) InteropLibrary.getUncached().invokeMember(noMatch, "isPresent"));
    }

    @ExportLibrary(InteropLibrary.class)
    public static class TestExecutable implements TruffleObject {
        private final Runnable runnable;

        public TestExecutable(Runnable runnable) {
            this.runnable = runnable;
        }

        @ExportMessage
        public boolean isExecutable() {
            return true;
        }

        @ExportMessage
        public Object execute(Object[] arguments) {
            runnable.run();
            return JolkNothing.INSTANCE;
        }
    }
}