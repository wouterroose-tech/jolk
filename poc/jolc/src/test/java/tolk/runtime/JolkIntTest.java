package tolk.runtime;

import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/// ## JolkIntTest
///
/// Verifies the intrinsic behavior of the Jolk `Int` type.
/// This includes arithmetic operations, comparisons, and flow control methods like `#repeat`.
///
public class JolkIntTest {

    private Object getOperation(String opName) {
        Object op = JolkInt.INT_TYPE.lookupInstanceMember(opName);
        assertNotNull(op, "Operation " + opName + " should be defined in JolkInt.INT_TYPE");
        return op;
    }

    private Object execute(Object op, Object... args) throws Exception {
        return InteropLibrary.getUncached().execute(op, args);
    }

    @Test
    void testAdd() throws Exception {
        Object op = getOperation("+");
        assertEquals(30, execute(op, 10, 20));
        assertEquals(-5, execute(op, 5, -10));
    }

    @Test
    void testSubtract() throws Exception {
        Object op = getOperation("-");
        assertEquals(10, execute(op, 30, 20));
        assertEquals(15, execute(op, 5, -10));
    }

    @Test
    void testNegate() throws Exception {
        Object op = getOperation("-");
        assertEquals(-5, execute(op, 5));
        assertEquals(10, execute(op, -10));
    }

    @Test
    void testMultiply() throws Exception {
        Object op = getOperation("*");
        assertEquals(60, execute(op, 3, 20));
        assertEquals(-50, execute(op, 5, -10));
    }

    @Test
    void testDivide() throws Exception {
        Object op = getOperation("/");
        assertEquals(5, execute(op, 10, 2));
        assertEquals(0, execute(op, 1, 2)); // Integer division
    }

    @Test
    void testModulo() throws Exception {
        Object op = getOperation("%");
        assertEquals(1, execute(op, 10, 3));
        assertEquals(0, execute(op, 10, 5));
    }

    @Test
    void testPower() throws Exception {
        Object op = getOperation("**");
        assertEquals(8, execute(op, 2, 3));
        assertEquals(1, execute(op, 10, 0));
    }

    @Test
    void testEquals() throws Exception {
        Object op = getOperation("==");
        assertEquals(true, execute(op, 10, 10));
        assertEquals(false, execute(op, 10, 5));
    }

    @Test
    void testNotEquals() throws Exception {
        Object op = getOperation("!=");
        assertEquals(false, execute(op, 10, 10));
        assertEquals(true, execute(op, 10, 5));
    }

    @Test
    void testGreaterThan() throws Exception {
        Object op = getOperation(">");
        assertEquals(true, execute(op, 10, 5));
        assertEquals(false, execute(op, 5, 10));
        assertEquals(false, execute(op, 5, 5));
    }

    @Test
    void testLessThan() throws Exception {
        Object op = getOperation("<");
        assertEquals(false, execute(op, 10, 5));
        assertEquals(true, execute(op, 5, 10));
        assertEquals(false, execute(op, 5, 5));
    }

    @Test
    void testGreaterOrEqual() throws Exception {
        Object op = getOperation(">=");
        assertEquals(true, execute(op, 10, 5));
        assertEquals(false, execute(op, 5, 10));
        assertEquals(true, execute(op, 5, 5));
    }

    @Test
    void testLessOrEqual() throws Exception {
        Object op = getOperation("<=");
        assertEquals(false, execute(op, 10, 5));
        assertEquals(true, execute(op, 5, 10));
        assertEquals(true, execute(op, 5, 5));
    }

    @Test
    void testTimes() throws Exception {
        Object op = getOperation("times");
        AtomicInteger counter = new AtomicInteger(0);
        IntTestExecutable action = new IntTestExecutable(counter::incrementAndGet);

        // Execute: 5 #times [ ... ]
        // Arguments: [5, action]
        Object result = execute(op, 5, action);

        assertEquals(5, result, "Times should return the count (self)");
        assertEquals(5, counter.get(), "Action should be executed 5 times");
    }

    @Test
    void testToString() throws Exception {
        Object op = getOperation("toString");
        assertEquals("42", execute(op, 42));
    }

    @Test
    void testHash() throws Exception {
        Object op = getOperation("hash");
        assertEquals(42, execute(op, 42));
    }

    @Test
    void testEquivalence() throws Exception {
        Object op = getOperation("~~");
        assertEquals(true, execute(op, 10, 10));
        assertEquals(false, execute(op, 10, 5));
    }

    @Test
    void testNonEquivalence() throws Exception {
        Object op = getOperation("!~");
        assertEquals(false, execute(op, 10, 10));
        assertEquals(true, execute(op, 10, 5));
    }

    @Test
    void testIfPresent() throws Exception {
        Object op = getOperation("ifPresent");
        AtomicInteger counter = new AtomicInteger(0);
        IntTestExecutable action = new IntTestExecutable(counter::incrementAndGet);

        // Execute: 5 #ifPresent [ ... ]
        Object result = execute(op, 5, action);

        assertEquals(5, result, "ifPresent should return self");
        assertEquals(1, counter.get(), "Action should be executed");
    }

    @Test
    void testIfEmpty() throws Exception {
        Object op = getOperation("ifEmpty");
        AtomicInteger counter = new AtomicInteger(0);
        IntTestExecutable action = new IntTestExecutable(counter::incrementAndGet);

        // Execute: 5 #ifEmpty [ ... ]
        Object result = execute(op, 5, action);

        assertEquals(5, result, "ifEmpty should return self");
        assertEquals(0, counter.get(), "Action should NOT be executed");
    }

    @Test
    void testIsPresent() throws Exception {
        Object op = getOperation("isPresent");
        assertEquals(true, execute(op, 5));
    }

    @Test
    void testIsEmpty() throws Exception {
        Object op = getOperation("isEmpty");
        assertEquals(false, execute(op, 5));
    }

    @Test
    void testClassAccessor() throws Exception {
        Object op = getOperation("class");
        Object result = execute(op, 42);
        assertEquals(JolkInt.INT_TYPE, result);
    }

    @Test
    void testInstanceOf() throws Exception {
        Object op = getOperation("instanceOf");

        // Test against Int type
        Object match = execute(op, 42, JolkInt.INT_TYPE);
        assertTrue((Boolean) InteropLibrary.getUncached().invokeMember(match, "isPresent"));

        // Test against an unrelated type (using Nothing type as dummy unrelated)
        Object noMatch = execute(op, 42, JolkNothing.NOTHING_TYPE);
        assertFalse((Boolean) InteropLibrary.getUncached().invokeMember(noMatch, "isPresent"));
    }

    @Test
    void testArityMismatch() {
        Object op = getOperation("+");
        assertThrows(ArityException.class, () -> execute(op, 1)); // Missing argument
        assertThrows(ArityException.class, () -> execute(op, 1, 2, 3)); // Too many arguments
    }

    @Test
    void testTypeMismatch() {
        Object op = getOperation("+");
        assertThrows(UnsupportedTypeException.class, () -> execute(op, 1, "string"));
    }

    @ExportLibrary(InteropLibrary.class)
    public static class IntTestExecutable implements TruffleObject {
        private final Runnable runnable;

        public IntTestExecutable(Runnable runnable) {
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
