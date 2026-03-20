package tolk.runtime;

import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import tolk.JolcTestBase;

import static org.junit.jupiter.api.Assertions.*;

/// ## JolkNothingTest
///
/// Verifies the behavior of Jolk's `null` identity, which is a first-class singleton
/// instance of the `Nothing` type. This test ensures that `null` correctly participates
/// in message passing, adheres to the Null Object Pattern, and supports flow control.
///
public class JolkNothingTest extends JolcTestBase {

    private final JolkNothing nothing = JolkNothing.INSTANCE;

    @Test
    void testIsNull() {
        assertTrue(nothing.isNull(), "JolkNothing should identify as null");
    }

    @Test
    void testToDisplayString() {
        assertEquals("null", nothing.toDisplayString(false));
    }

    @Test
    void testHasMembers() {
        assertTrue(nothing.hasMembers());
    }

    @Test
    void testIsMemberInvocable() {
        assertTrue(nothing.isMemberInvocable("isEmpty"));
        assertTrue(nothing.isMemberInvocable("ifEmpty"));
        assertFalse(nothing.isMemberInvocable("randomMethod"));
    }

    @Test
    void testInvokeHash() throws Exception {
        Object result = nothing.invokeMember("hash", new Object[]{});
        assertEquals(0, result);
    }

    @Test
    void testInvokeToString() throws Exception {
        Object result = nothing.invokeMember("toString", new Object[]{});
        assertEquals("null", result);
    }

    @Test
    void testInvokeIsPresent() throws Exception {
        Object result = nothing.invokeMember("isPresent", new Object[]{});
        assertEquals(false, result);
    }

    @Test
    void testInvokeIsEmpty() throws Exception {
        Object result = nothing.invokeMember("isEmpty", new Object[]{});
        assertEquals(true, result);
    }

    @Test
    void testInvokeIfPresent() throws Exception {
        boolean[] executed = {false};
        Consumer<Object> action = (o) -> { executed[0] = true; };
        Object result = nothing.invokeMember("ifPresent", new Object[]{action});
        
        assertSame(nothing, result, "Should return self for chaining");
        assertFalse(executed[0], "Action should NOT be executed for Nothing");
    }

    @Test
    void testInvokeIfEmpty() throws Exception {
        boolean[] executed = {false};
        Object action = new TestExecutable(() -> { executed[0] = true; });
        Object result = nothing.invokeMember("ifEmpty", new Object[]{action});
        
        assertTrue(executed[0], "Action SHOULD be executed for Nothing");
    }

    @Test
    void testInvokeProject() throws Exception {
        // Project is ignored, just verifying it returns self and doesn't crash
        Object result = nothing.invokeMember("project", new Object[]{ "someMap" });
        assertSame(nothing, result);
    }

    @Test
    void testInvokeClass() throws Exception {
        Object result = nothing.invokeMember("class", new Object[]{});
        
        assertTrue(result instanceof JolkMetaClass, "Nothing #class should return a MetaClass");
        JolkMetaClass meta = (JolkMetaClass) result;
        assertEquals("Nothing", meta.getMetaSimpleName());
    }

    @Test
    void testInvokeNewThrowsException() throws Exception {
        JolkMetaClass nothingClass = (JolkMetaClass) nothing.invokeMember("class", new Object[]{});
        assertThrows(RuntimeException.class, () -> {
            nothingClass.invokeMember("new", new Object[]{});
        }, "Invoking #new on Nothing should throw an exception");
    }

    @ExportLibrary(InteropLibrary.class)
    public static class TestExecutable implements TruffleObject {
        private final Runnable runnable;

        TestExecutable(Runnable runnable) {
            this.runnable = runnable;
        }

        @ExportMessage
        boolean isExecutable() {
            return true;
        }

        @ExportMessage
        Object execute(Object[] arguments) {
            runnable.run();
            return JolkNothing.INSTANCE;
        }
    }
}
