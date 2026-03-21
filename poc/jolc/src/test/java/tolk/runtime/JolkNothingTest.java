package tolk.runtime;

import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import java.util.function.Consumer;
import org.graalvm.polyglot.Value;
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

    
    @Test
    void testIsNothingIdentity() {
        Value nothing = eval("null");
        // A behavioral check is more robust than a string comparison or an instance check on a proxy.
        // In Jolk, only the Nothing identity returns false for #isPresent.
        assertFalse(nothing.invokeMember("isPresent").asBoolean(), "eval(\"null\") should return the Nothing identity, which is not present.");
        assertTrue(nothing.invokeMember("isEmpty").asBoolean(), "eval(\"null\") should return the Nothing identity, which is empty.");
    }

    @Test
    void testToDisplayString() {
        assertEquals("null", eval("null").toString());
    }

    @Test
    void testHasMembers() {
        assertTrue(eval("null").hasMembers());
    }

    @Test
    void testIsMemberInvocable() {
        Value nothing = eval("null");
        assertTrue(nothing.canInvokeMember("isEmpty"));
        assertTrue(nothing.canInvokeMember("ifEmpty"));
        assertTrue(nothing.canInvokeMember("randomMethod"), "JolkNothing should silently absorb unknown messages");
    }

    @Test
    void testEquivalenceOperators() throws Exception {
        // Get another object to compare against
        Value otherObject = eval("class Other{};").invokeMember("new");
        Value nothing = eval("null");

        // Test equivalence (~~)
        assertTrue(nothing.invokeMember("~~", nothing).asBoolean(), "Nothing should be equivalent to itself.");
        assertFalse(nothing.invokeMember("~~", otherObject).asBoolean(), "Nothing should not be equivalent to another object.");

        // Test non-equivalence (!~)
        assertFalse(nothing.invokeMember("!~", nothing).asBoolean(), "Nothing should not be non-equivalent to itself.");
        assertTrue(nothing.invokeMember("!~", otherObject).asBoolean(), "Nothing should be non-equivalent to another object.");
    }

    @Test
    void testInvokeHash() throws Exception {
        Value result = eval("null").invokeMember("hash");
        assertEquals(0, result.asInt());
    }

    @Test
    void testInvokeToString() throws Exception {
        Value result = eval("null").invokeMember("toString");
        assertEquals("null", result.asString());
    }

    @Test
    void testInvokeIsPresent() throws Exception {
        Value result = eval("null").invokeMember("isPresent");
        assertFalse(result.asBoolean());
    }

    @Test
    void testInvokeIsEmpty() throws Exception {
        Value result = eval("null").invokeMember("isEmpty");
        assertTrue(result.asBoolean());
    }

    @Test
    void testInvokeIfPresent() throws Exception {
        boolean[] executed = {false};
        Consumer<Object> action = (o) -> { executed[0] = true; };
        Value nothing = eval("null");
        Value result = nothing.invokeMember("ifPresent", action);
        
        assertEquals(nothing, result, "Should return self for chaining");
        assertFalse(executed[0], "Action should NOT be executed for Nothing");
    }

    @Test
    void testInvokeIfEmpty() throws Exception {
        boolean[] executed = {false};
        Object action = new TestExecutable(() -> { executed[0] = true; });
        eval("null").invokeMember("ifEmpty", action);
        
        assertTrue(executed[0], "Action SHOULD be executed for Nothing");
    }

    @Test
    void testInvokeProject() throws Exception {
        Value nothing = eval("null");
        // Project is ignored, just verifying it returns self and doesn't crash
        Value result = nothing.invokeMember("project", "someMap");
        assertEquals(nothing, result);
    }

    @Test
    void testInvokeClass() throws Exception {
        Value result = eval("null").invokeMember("class");
        
        assertTrue(result.isMetaObject(), "Nothing #class should return a MetaClass");
        assertEquals("Nothing", result.getMetaSimpleName());
    }

    @Test
    void testInstanceOfObject() throws Exception {
        Value objectMeta = eval("class Object {}"); // Access the intrinsic Object class
        Value nothingValue = eval("null");

        // JolkNothing should match against Object
        Value match = nothingValue.invokeMember("instanceOf", objectMeta);
        assertTrue(match.toString().contains("Match("), "Nothing should be an instance of Object");
    }

    @Test
    void testInvokeNewThrowsException() throws Exception {
        Value nothingClass = eval("null").invokeMember("class");
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
