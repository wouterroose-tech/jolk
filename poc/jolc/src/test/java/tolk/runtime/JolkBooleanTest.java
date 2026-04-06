package tolk.runtime;

import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import org.junit.jupiter.api.Test;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.*;

/**
 * ## JolkBooleanTest
 *
 * Verifies the intrinsic behavior of the Jolk `Boolean` type.
 * This includes logical operations, branching messages used for control flow,
 * and the standard object protocol.
 */
public class JolkBooleanTest {

    private Object getOperation(String opName) {
        Object op = JolkBooleanExtension.BOOLEAN_TYPE.lookupInstanceMember(opName);
        assertNotNull(op, "Operation " + opName + " should be defined in JolkBoolean.BOOLEAN_TYPE");
        return op;
    }

    private Object execute(Object op, Object... args) throws Exception {
        return InteropLibrary.getUncached().execute(op, args);
    }

    @Test
    void testLogicAnd() throws Exception {
        Object op = getOperation("&&");
        assertEquals(true, execute(op, true, true));
        assertEquals(false, execute(op, true, false));
        assertEquals(false, execute(op, false, true));
        assertEquals(false, execute(op, false, false));
    }

    @Test
    void testIdentityAndEquivalence() throws Exception {
        assertTrue((Boolean) execute(getOperation("=="), true, true));
        assertFalse((Boolean) execute(getOperation("=="), true, false));
        assertTrue((Boolean) execute(getOperation("!="), true, false));

        assertTrue((Boolean) execute(getOperation("~~"), true, true));
        assertTrue((Boolean) execute(getOperation("!~"), true, false));
    }

    @Test
    void testLogicOr() throws Exception {
        Object op = getOperation("||");
        assertEquals(true, execute(op, true, true));
        assertEquals(true, execute(op, true, false));
        assertEquals(true, execute(op, false, true));
        assertEquals(false, execute(op, false, false));
    }

    @Test
    void testLogicNot() throws Exception {
        Object op = getOperation("!");
        assertEquals(false, execute(op, true));
        assertEquals(true, execute(op, false));
    }

    @Test
    void testIfTrue() throws Exception {
        Object op = getOperation("?");
        AtomicBoolean executed = new AtomicBoolean(false);
        BooleanTestExecutable action = new BooleanTestExecutable(() -> executed.set(true));

        // true ? [ action ]
        Object resTrue = execute(op, true, action);
        assertTrue(executed.get(), "Action should execute for true");
        assertEquals(true, resTrue);

        executed.set(false);
        // false ? [ action ]
        Object resFalse = execute(op, false, action);
        assertFalse(executed.get(), "Action should NOT execute for false");
        assertEquals(false, resFalse);
    }

    @Test
    void testIfFalse() throws Exception {
        Object op = getOperation("?!");
        AtomicBoolean executed = new AtomicBoolean(false);
        BooleanTestExecutable action = new BooleanTestExecutable(() -> executed.set(true));

        // false ?! [ action ]
        Object resFalse = execute(op, false, action);
        assertTrue(executed.get(), "Action should execute for false");
        assertEquals(false, resFalse);

        executed.set(false);
        // true ?! [ action ]
        Object resTrue = execute(op, true, action);
        assertFalse(executed.get(), "Action should NOT execute for true");
        assertEquals(true, resTrue);
    }

    @Test
    void testCombinedTernary() throws Exception {
        Object op = getOperation("? :");
        AtomicBoolean thenExecuted = new AtomicBoolean(false);
        AtomicBoolean elseExecuted = new AtomicBoolean(false);
        
        BooleanTestExecutable thenAction = new BooleanTestExecutable(() -> thenExecuted.set(true));
        BooleanTestExecutable elseAction = new BooleanTestExecutable(() -> elseExecuted.set(true));

        // true ? [ then ] : [ else ]
        execute(op, true, thenAction, elseAction);
        assertTrue(thenExecuted.get());
        assertFalse(elseExecuted.get());
        // Note: In Jolk, ternary returns the result of the executed block.
    }

    @Test
    void testElse() throws Exception {
        Object op = getOperation(":");
        AtomicBoolean executed = new AtomicBoolean(false);
        BooleanTestExecutable action = new BooleanTestExecutable(() -> executed.set(true));

        // false : [ action ]
        Object resFalse = execute(op, false, action);
        assertTrue(executed.get(), "Action should execute for false (else branch)");
        assertEquals(false, resFalse);

        executed.set(false);
        // true : [ action ]
        Object resTrue = execute(op, true, action);
        assertFalse(executed.get(), "Action should NOT execute for true (else branch)");
        assertEquals(true, resTrue);
    }

    @Test
    void testObjectProtocol() throws Exception {
        // toString
        assertEquals("true", execute(getOperation("toString"), true));
        assertEquals("false", execute(getOperation("toString"), false));

        // Hash
        assertEquals((long) Boolean.TRUE.hashCode(), execute(getOperation("hash"), true));

        // Presence
        assertTrue((Boolean) execute(getOperation("isPresent"), true));
        assertFalse((Boolean) execute(getOperation("isEmpty"), false));

        // Class
        assertEquals(JolkBooleanExtension.BOOLEAN_TYPE, execute(getOperation("class"), true));
    }

    @Test
    void testPresenceLogic() throws Exception {
        AtomicBoolean executed = new AtomicBoolean(false);
        BooleanTestExecutable action = new BooleanTestExecutable(() -> executed.set(true));

        // ifPresent: Booleans are present identities, so action should execute
        Object resPresent = execute(getOperation("ifPresent"), true, action);
        assertTrue(executed.get(), "ifPresent should execute for Boolean");
        // JolkObject.ifPresent returns the result of the action
        assertEquals(JolkNothing.INSTANCE, resPresent);

        executed.set(false);

        // ifEmpty: Booleans are NOT empty, so action should NOT execute
        Object resEmpty = execute(getOperation("ifEmpty"), true, action);
        assertFalse(executed.get(), "ifEmpty should NOT execute for Boolean");
        // JolkObject.ifEmpty returns self (the receiver)
        assertEquals(true, resEmpty);
    }

    @Test
    void testInstanceOf() throws Exception {
        Object op = getOperation("instanceOf");
        Object match = execute(op, true, JolkBooleanExtension.BOOLEAN_TYPE);
        assertTrue((Boolean) InteropLibrary.getUncached().invokeMember(match, "isPresent"));
    }

    @ExportLibrary(InteropLibrary.class)
    public static class BooleanTestExecutable implements TruffleObject {
        private final Runnable runnable;

        public BooleanTestExecutable(Runnable runnable) {
            this.runnable = runnable;
        }

        @ExportMessage
        public boolean isExecutable() { return true; }

        @ExportMessage
        public Object execute(Object[] arguments) {
            runnable.run();
            return JolkNothing.INSTANCE;
        }
    }
}
