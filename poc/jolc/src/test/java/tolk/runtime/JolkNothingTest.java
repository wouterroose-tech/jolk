package tolk.runtime;

import org.graalvm.polyglot.HostAccess;
import org.junit.jupiter.api.Disabled;
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
        MockClosure action = new MockClosure();
        Object result = nothing.invokeMember("ifPresent", new Object[]{action});
        
        assertSame(nothing, result, "Should return self for chaining");
        assertFalse(action.executed, "Action should NOT be executed for Nothing");
    }

    @Test
    @Disabled("This is a placeholder for future functionality to execute the closure for Nothing")
    void testInvokeIfEmpty() throws Exception {
        MockClosure action = new MockClosure();
        Object result = nothing.invokeMember("ifEmpty", new Object[]{action});
        
        assertSame(nothing, result, "Should return self for chaining");
        assertTrue(action.executed, "Action SHOULD be executed for Nothing");
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

    // Helper class to verify closure execution
    // TODO: create a proper Jolk closure object rather than a Java Runnable
    static class MockClosure implements Runnable {
        boolean executed = false;

        @HostAccess.Export
        @Override
        public void run() {
            executed = true;
        }
    }
}
