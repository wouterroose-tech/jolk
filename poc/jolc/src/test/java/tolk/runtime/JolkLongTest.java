package tolk.runtime;

import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;

import tolk.JolcTestBase;

import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

/// ## JolkLongTest
///
/// Verifies the intrinsic behavior of the Jolk `Long` type.
/// This includes arithmetic operations, comparisons, and flow control methods like `#repeat`.
///
public class JolkLongTest extends JolcTestBase {

    private Object getOperation(String opName) {
        Object op = JolkLongExtension.LONG_TYPE.lookupInstanceMember(opName);
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
    void testTimesWithIndex() throws Exception {
        Object op = getOperation("times");
        List<Long> indices = new ArrayList<>();
        IndexedIntTestExecutable action = new IndexedIntTestExecutable(args -> {
            if (args.length > 0 && args[0] instanceof Long) indices.add((Long) args[0]);
        });

        execute(op, 3L, action);
        assertEquals(3, indices.size());
        assertEquals(0L, indices.get(0));
        assertEquals(1L, indices.get(1));
        assertEquals(2L, indices.get(2));
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
        // We expect the closure result (e.g. 42) to be returned
        IntTestExecutable action = new IntTestExecutable(() -> 42L);

        // Execute: 5 #ifPresent [ ... ]
        Object result = execute(op, 5L, action);

        assertEquals(42L, result, "ifPresent should return the result of the closure");
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
        assertEquals(JolkLongExtension.LONG_TYPE, result);
    }

    @Test
    void testInstanceOf() throws Exception {
        Object op = getOperation("instanceOf");

        // Test against Int type
        Object match = execute(op, 42L, JolkLongExtension.LONG_TYPE);
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
        private final java.util.function.Supplier<Object> supplier;

        public IntTestExecutable(java.util.function.Supplier<Object> supplier) {
            this.supplier = supplier;
        }

        @ExportMessage
        public boolean isExecutable() {
            return true;
        }

        @ExportMessage
        public Object execute(Object[] arguments) {
            return supplier.get();
        }
    }

    @ExportLibrary(InteropLibrary.class)
    public static class IndexedIntTestExecutable implements TruffleObject {
        private final java.util.function.Consumer<Object[]> consumer;

        public IndexedIntTestExecutable(java.util.function.Consumer<Object[]> consumer) {
            this.consumer = consumer;
        }

        @ExportMessage
        public boolean isExecutable() {
            return true;
        }

        @ExportMessage
        public Object execute(Object[] arguments) {
            consumer.accept(arguments);
            return JolkNothing.INSTANCE;
        }
    }

    @Test
    void testLongField() {
        // Verify that #new(arg) initializes the field 'val'
        String source = "class Container { Long val; }";
        Value meta = eval(source);
        
        Value instance = meta.invokeMember("new");
        assertEquals(0L, instance.invokeMember("val").asLong(), "Uninitialized Long fields should default to 0L.");
        instance.invokeMember("val", 1L);
        assertEquals(1L, instance.invokeMember("val").asLong(), "The getter should return the long value stored in the Object field.");

        // Canonical #new
        instance = meta.invokeMember("new", 42L);
        assertEquals(42L, instance.invokeMember("val").asLong(), "Canonical #new should initialize fields in order.");
    }

    /// Verify that Long fields can be initialized with expressions and that they evaluate correctly.
     @Test
     void testLongExpression() {
        String source = "class ExprTest { Long run() { ^ 40 + 1 ** 2  * -2 * -1 } }";
        Value result = eval(source).invokeMember("new").invokeMember("run");
        assertEquals(42L, result.asLong(), "The expression should evaluate to the correct long value.");
     }

    @Test
    void testArithmetic() {
        String source = """
            class MathTest {
                Long add(Long a, Long b) { ^ a + b }
                Long sub(Long a, Long b) { ^ a - b }
                Long mul(Long a, Long b) { ^ a * b }
                Long div(Long a, Long b) { ^ a / b }
                Long mod(Long a, Long b) { ^ a % b }
                Long pow(Long a, Long b) { ^ a ** b }
            }""";;
        Value instance = eval(source).invokeMember("new");
        assertEquals(30L, instance.invokeMember("add", 10L, 20L).asLong());
        assertEquals(10L, instance.invokeMember("sub", 30L, 20L).asLong());
        assertEquals(200L, instance.invokeMember("mul", 10L, 20L).asLong());
        assertEquals(5L, instance.invokeMember("div", 10L, 2L).asLong());
        assertEquals(1L, instance.invokeMember("mod", 10L, 3L).asLong());
        assertEquals(8L, instance.invokeMember("pow", 2L, 3L).asLong());
    }

    @Test
    void testComparison() {
        String source = """
            class CompTest {
                Boolean gt(Long a, Long b) { ^ a > b }
                Boolean lt(Long a, Long b) { ^ a < b }
                Boolean ge(Long a, Long b) { ^ a >= b }
                Boolean le(Long a, Long b) { ^ a <= b }
                Boolean eq(Long a, Long b) { ^ a == b }
                Boolean ne(Long a, Long b) { ^ a != b }
            }""";
        Value instance = eval(source).invokeMember("new");
        assertTrue(instance.invokeMember("gt", 10L, 5L).asBoolean());
        assertTrue(instance.invokeMember("lt", 5L, 10L).asBoolean());
        assertTrue(instance.invokeMember("ge", 10L, 10L).asBoolean());
        assertTrue(instance.invokeMember("le", 10L, 10L).asBoolean());
        assertTrue(instance.invokeMember("eq", 10L, 10L).asBoolean());
        assertTrue(instance.invokeMember("ne", 10L, 5L).asBoolean());
    }

    @Test
    void testObjectProtocol() {
        String source = "class ProtoTest { " +
                "String str(Long i) { ^ i #toString } " +
                "Long h(Long i) { ^ i #hash } " +
                "Boolean eq(Long a, Long b) { ^ a ~~ b } " +
                "Boolean ne(Long a, Long b) { ^ a !~ b } " +
                "Boolean present(Long i) { ^ i #isPresent } " +
                "Boolean empty(Long i) { ^ i #isEmpty } }";
        Value instance = eval(source).invokeMember("new");
        assertEquals("42", instance.invokeMember("str", 42L).asString());
        assertEquals(42L, instance.invokeMember("h", 42L).asLong());
        assertTrue(instance.invokeMember("eq", 42L, 42L).asBoolean());
        assertFalse(instance.invokeMember("ne", 42L, 42L).asBoolean());
        assertTrue(instance.invokeMember("present", 42L).asBoolean());
        assertFalse(instance.invokeMember("empty", 42L).asBoolean());
    }

    @Test
    void testLongFieldWithDefault() {
        String source = "class Container { Long long = 42; Int int = 42;}";
        Value meta = eval(source);
        Value instance = meta.invokeMember("new");  
        assertEquals(42L, instance.invokeMember("long").asLong());
        assertEquals(42L, instance.invokeMember("int").asLong());
    }

    @Test
    void testMinMax() {
        String source = """
            class MinMaxTest {
            Long getMax() { ^ 9223372036854775807 }
            Long getMin() { ^ -9223372036854775808 }
            Long getMaxConst() { ^ Long #MAX }
            Long getMinConst() { ^ Long #MIN }
            Long wrap() { ^ 9223372036854775807 + 1 } }
            """;
        Value instance = eval(source).invokeMember("new");
        assertEquals(Long.MAX_VALUE, instance.invokeMember("getMax").asLong());
        assertEquals(Long.MIN_VALUE, instance.invokeMember("getMin").asLong());
        assertEquals(Long.MAX_VALUE, instance.invokeMember("getMaxConst").asLong());
        assertEquals(Long.MIN_VALUE, instance.invokeMember("getMinConst").asLong());
        assertEquals(Long.MIN_VALUE, instance.invokeMember("wrap").asLong(), "Long overflow should wrap around.");
    }

    @Test
    void testExpression() {
        String source = """
            class MinMaxTest {
            Long val() { ^ 42 }
            Long val2() { ^ 40 + 2 }
            Long val3() { ^ true ? 42 : 0 }
            Long val4() { ^ false ? 42 : 0 }
            Long val5() { ^ null ?? 42 }
            Long val6() { ^ (2 * 1) + (20 * 2)}
            Long val6() { ^ (2 ** 2) + (19 * 2)}
            Int int() { ^ 42 }
            }""";
        Value meta = eval(source);
        Value instance = meta.invokeMember("new");  
        assertEquals(42L, instance.invokeMember("val").asLong());
        assertEquals(42L, instance.invokeMember("val2").asLong());
        assertEquals(42L, instance.invokeMember("val3").asLong());
        assertEquals(0L, instance.invokeMember("val4").asLong());
        assertEquals(42L, instance.invokeMember("val5").asLong());
        assertEquals(42L, instance.invokeMember("val6").asLong());
        assertEquals(42L, instance.invokeMember("val6").asLong());
        assertEquals(42L, instance.invokeMember("int").asLong());
    }
}
