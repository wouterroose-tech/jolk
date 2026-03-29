package tolk.runtime;

import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

public class JolkNothingTest {

    @Test
    void testInvokeIfEmpty() throws Exception {
        Object nothing = JolkNothing.INSTANCE;
        AtomicBoolean executed = new AtomicBoolean(false);
        TestExecutable action = new TestExecutable(() -> executed.set(true));

        // Execute: null #ifEmpty [ executed.set(true) ]
        InteropLibrary.getUncached().invokeMember(nothing, "ifEmpty", action);

        assertTrue(executed.get(), "Action should be executed for Nothing");
    }

    @Test
    void testInvokeIfPresent() throws Exception {
        Object nothing = JolkNothing.INSTANCE;
        AtomicBoolean executed = new AtomicBoolean(false);
        TestExecutable action = new TestExecutable(() -> executed.set(true));

        InteropLibrary.getUncached().invokeMember(nothing, "ifPresent", action);

        assertFalse(executed.get(), "Action should NOT be executed for Nothing");
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
        Object noMatch = InteropLibrary.getUncached().invokeMember(nothing, "instanceOf", JolkLong.LONG_TYPE);
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