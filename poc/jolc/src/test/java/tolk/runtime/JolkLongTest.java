package tolk.runtime;

import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

/// ## JolkLongTest
///
/// Verifies the intrinsic behavior of the Jolk `Long` type.
/// This includes arithmetic operations, comparisons, and flow control methods like `#repeat`.
///
public class JolkLongTest {

    private Object getOperation(String opName) {
        Object op = JolkLong.LONG_TYPE.lookupInstanceMember(opName);
        assertNotNull(op, "Operation " + opName + " should be defined in JolkLong.LONG_TYPE");
        return op;
    }

    private Object execute(Object op, Object... args) throws Exception {
        return InteropLibrary.getUncached().execute(op, args);
    }

    @Test
    void testAdd() throws Exception {
        Object op = getOperation("+");
        // We test both Integer and Long literals here to verify the asLong coercion logic
        assertEquals(30L, execute(op, 10, 20L), "Mixed Integer and Long should work");
        assertEquals(30L, execute(op, 10L, 20), "Mixed Integer and Long should work");
        assertEquals(30L, execute(op, 10L, 20L), "Pure Long should work");
        assertEquals(30L, execute(op, 10, 20), "Pure Integer should work");
        assertEquals(-5L, execute(op, 5, -10), "Mixed Integer and Long should work");
        assertEquals(-5L, execute(op, 5, -10L), "Mixed Integer and Long should work");
        assertEquals(-5L, execute(op, 5L, -10), "Mixed Integer and Long should work");
        assertEquals(-5L, execute(op, 5L, -10L), "Mixed Integer and Long should work");
    }

    @Test
    void testSubtract() throws Exception {
        Object op = getOperation("-");
        assertEquals(10L, execute(op, 30L, 20L));
        assertEquals(15L, execute(op, 5L, -10L));
    }

    @Test
    void testNegate() throws Exception {
        Object op = getOperation("-");
        assertEquals(-5L, execute(op, 5L));
        assertEquals(10L, execute(op, -10L));
    }

    @Test
    void testMultiply() throws Exception {
        Object op = getOperation("*");
        assertEquals(60L, execute(op, 3L, 20L));
        assertEquals(-50L, execute(op, 5L, -10L));
    }

    @Test
    void testDivide() throws Exception {
        Object op = getOperation("/");
        assertEquals(5L, execute(op, 10L, 2L));
        assertEquals(0L, execute(op, 1L, 2L)); // Long division
    }

    @Test
    void testModulo() throws Exception {
        Object op = getOperation("%");
        assertEquals(1L, execute(op, 10L, 3L));
        assertEquals(0L, execute(op, 10L, 5L));
    }

    @Test
    void testPower() throws Exception {
        Object op = getOperation("**");
        assertEquals(8L, execute(op, 2L, 3L));
        assertEquals(1L, execute(op, 10L, 0L));
    }

    @Test
    void testEquals() throws Exception {
        Object op = getOperation("==");
        assertEquals(true, execute(op, 10L, 10L));
        assertEquals(false, execute(op, 10L, 5L));
    }

    @Test
    void testNotEquals() throws Exception {
        Object op = getOperation("!=");
        assertEquals(false, execute(op, 10L, 10L));
        assertEquals(true, execute(op, 10L, 5L));
    }

    @Test
    void testGreaterThan() throws Exception {
        Object op = getOperation(">");
        assertEquals(true, execute(op, 10L, 5L));
        assertEquals(false, execute(op, 5L, 10L));
        assertEquals(false, execute(op, 5L, 5L));
    }

    @Test
    void testLessThan() throws Exception {
        Object op = getOperation("<");
        assertEquals(false, execute(op, 10L, 5L));
        assertEquals(true, execute(op, 5L, 10L));
        assertEquals(false, execute(op, 5L, 5L));
    }

    @Test
    void testGreaterOrEqual() throws Exception {
        Object op = getOperation(">=");
        assertEquals(true, execute(op, 10L, 5L));
        assertEquals(false, execute(op, 5L, 10L));
        assertEquals(true, execute(op, 5L, 5L));
    }

    @Test
    void testLessOrEqual() throws Exception {
        Object op = getOperation("<=");
        assertEquals(false, execute(op, 10L, 5L));
        assertEquals(true, execute(op, 5L, 10L));
        assertEquals(true, execute(op, 5L, 5L));
    }

    @Test
    void testTimes() throws Exception {
        Object op = getOperation("times");
        AtomicLong counter = new AtomicLong(0);
        IntTestExecutable action = new IntTestExecutable(counter::incrementAndGet);

        // Execute: 5 #times [ ... ]
        // Arguments: [5, action]
        Object result = execute(op, 5L, action);
        assertEquals(5L, result, "Times should return the count (self)");
        assertEquals(5L, counter.get(), "Action should be executed 5 times");

        // Verify coercion: Integer should also work
        execute(op, 2, action);
        assertEquals(7L, counter.get(), "Action should be executed 2 more times (Total 7)");
    }

    @Test
    void testToString() throws Exception {
        Object op = getOperation("toString");
        assertEquals("42", execute(op, 42L));
    }

    @Test
    void testHash() throws Exception {
        Object op = getOperation("hash");
        assertEquals(42L, execute(op, 42L));
    }

    @Test
    void testEquivalence() throws Exception {
        Object op = getOperation("~~");
        assertEquals(true, execute(op, 10L, 10L));
        assertEquals(false, execute(op, 10L, 5L));
    }

    @Test
    void testNonEquivalence() throws Exception {
        Object op = getOperation("!~");
        assertEquals(false, execute(op, 10L, 10L));
        assertEquals(true, execute(op, 10L, 5L));
    }

    @Test
    void testIfPresent() throws Exception {
        Object op = getOperation("ifPresent");
        AtomicLong counter = new AtomicLong(0);
        IntTestExecutable action = new IntTestExecutable(counter::incrementAndGet);

        // Execute: 5 #ifPresent [ ... ]
        Object result = execute(op, 5L, action);

        assertEquals(5L, result, "ifPresent should return self");
        assertEquals(1, counter.get(), "Action should be executed");
    }

    @Test
    void testIfEmpty() throws Exception {
        Object op = getOperation("ifEmpty");
        AtomicLong counter = new AtomicLong(0);
        IntTestExecutable action = new IntTestExecutable(counter::incrementAndGet);

        // Execute: 5 #ifEmpty [ ... ]
        Object result = execute(op, 5L, action);

        assertEquals(5L, result, "ifEmpty should return self");
        assertEquals(0, counter.get(), "Action should NOT be executed");
    }

    @Test
    void testIsPresent() throws Exception {
        Object op = getOperation("isPresent");
        assertEquals(true, execute(op, 5L));
    }

    @Test
    void testIsEmpty() throws Exception {
        Object op = getOperation("isEmpty");
        assertEquals(false, execute(op, 5L));
    }

    @Test
    void testClassAccessor() throws Exception {
        Object op = getOperation("class");
        Object result = execute(op, 42L);
        assertEquals(JolkLong.LONG_TYPE, result);
    }

    @Test
    void testInstanceOf() throws Exception {
        Object op = getOperation("instanceOf");

        // Test against Int type
        Object match = execute(op, 42L, JolkLong.LONG_TYPE);
        assertTrue((Boolean) InteropLibrary.getUncached().invokeMember(match, "isPresent"));

        // Test against an unrelated type (using Nothing type as dummy unrelated)
        Object noMatch = execute(op, 42L, JolkNothing.NOTHING_TYPE);
        assertFalse((Boolean) InteropLibrary.getUncached().invokeMember(noMatch, "isPresent"));
    }

    @Test
    void testMinMaxValues() throws Exception {
        Object add = getOperation("+");
        Object sub = getOperation("-");
        Object negate = getOperation("-");
        Object gt = getOperation(">");

        // Arithmetic at boundaries
        assertEquals(Long.MAX_VALUE - 1L, execute(sub, Long.MAX_VALUE, 1L));
        assertEquals(Long.MIN_VALUE + 1L, execute(add, Long.MIN_VALUE, 1L));
        assertEquals(-1L, execute(add, Long.MAX_VALUE, Long.MIN_VALUE));

        // Comparisons
        assertEquals(true, execute(gt, Long.MAX_VALUE, Long.MIN_VALUE));
        assertEquals(false, execute(gt, Long.MIN_VALUE, Long.MAX_VALUE));

        // Negation behavior (Standard Java overflow for MIN_VALUE)
        assertEquals(Long.MIN_VALUE, execute(negate, Long.MIN_VALUE));
        assertEquals(-Long.MAX_VALUE, execute(negate, Long.MAX_VALUE));
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
