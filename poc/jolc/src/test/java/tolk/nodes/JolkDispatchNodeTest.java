package tolk.nodes;

import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.frame.VirtualFrame;
import java.util.concurrent.atomic.AtomicReference;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import org.graalvm.polyglot.Value;
import tolk.language.JolkLanguage;
import tolk.runtime.JolkStringExtension;

import org.junit.jupiter.api.Test;
import tolk.JolcTestBase;
import tolk.runtime.JolkClosure;
import tolk.runtime.JolkNothing;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ## JolkDispatchNodeTest
 *
 * Verifies the behavior of the {@link JolkDispatchNode}, which is responsible for
 * polymorphic message dispatch in Jolk, including intrinsic handling and
 * Identity Restitution.
 */
public class JolkDispatchNodeTest extends JolcTestBase {

    /**
     * Tests that {@link JolkNothing} correctly absorbs messages and returns itself.
     * This verifies the `doNothing` specialization.
     */
    @Test
    void testNothingSilentAbsorption() throws UnsupportedMessageException, UnknownIdentifierException, UnsupportedTypeException {
        eval(""); // Initialize context
        context.enter();
        try {
            JolkDispatchNode dispatchNode = JolkDispatchNodeGen.create();
            adopt(dispatchNode);
            Object result = dispatchNode.execute(null, null, JolkNothing.INSTANCE, "someArbitraryMessage", new Object[0]);
            assertEquals(JolkNothing.INSTANCE, result, "JolkNothing should absorb any message and return itself.");
        } finally {
            context.leave();
        }
    }

    /**
     * A simple wrapper node to allow JolkDispatchNode to be adopted by a JolkRootNode.
     * JolkRootNode expects a JolkNode as its body, but JolkDispatchNode directly extends Node.
     */
    private static class DispatchNodeWrapper extends JolkNode {
        @Child private JolkDispatchNode dispatchNode;

        DispatchNodeWrapper(JolkDispatchNode dispatchNode) {
            this.dispatchNode = dispatchNode;
        }

        @Override
        public Object executeGeneric(VirtualFrame frame) {
            // Use the first argument passed to the CallTarget as the receiver for dispatch.
            return dispatchNode.execute(frame, this, frame.getArguments()[0], "get", new Object[0]);
        }
    }

    private void adopt(JolkDispatchNode dispatchNode) {
        JolkLanguage lang = com.oracle.truffle.api.TruffleLanguage.LanguageReference.create(JolkLanguage.class).get(null);
        DispatchNodeWrapper wrapper = new DispatchNodeWrapper(dispatchNode);
        new JolkRootNode(lang, wrapper).getCallTarget(); // Calling getCallTarget() triggers adoption
    }

    /**
     * Tests that raw JVM `null` values returned from interop calls are
     * "lifted" to {@link JolkNothing.INSTANCE} (Identity Restitution).
     * This specifically targets the `if (result == null)` check in `doDispatch`.
     */
    @Test
    void testIdentityRestitutionForNull() throws UnsupportedMessageException, UnknownIdentifierException, UnsupportedTypeException {
        eval(""); // Ensure the language and context are initialized
        JolkDispatchNode dispatchNode = JolkDispatchNodeGen.create();

        context.enter();
        try {
            // Resolve the language reference while the context is entered.
            JolkLanguage lang = com.oracle.truffle.api.TruffleLanguage.LanguageReference.create(JolkLanguage.class).get(null);

            // We create a RootNode to establish a proper execution boundary.
            JolkRootNode root = new JolkRootNode(lang, new DispatchNodeWrapper(dispatchNode));

            // IMPORTANT: Use context.asValue(target).execute(...) instead of target.call(...)
            // This ensures the AtomicReference is properly wrapped as a guest-visible HostObject.
            // We wrap the CallTarget in a JolkClosure to make it executable via the Polyglot API.
            JolkClosure closure = new JolkClosure(root.getCallTarget());
            Value executable = context.asValue(closure);
            Value result = executable.execute(new AtomicReference<>(null));
            
            // Verify that the result is JolkNothing by checking its Jolk class.
            assertEquals("Nothing", result.invokeMember("class").getMetaSimpleName(), 
                "Raw JVM null from host must be lifted to the Jolk 'Nothing' identity.");
        } finally {
            context.leave();
        }
    }

    /**
     * Tests that intrinsic messages to raw Java `Long` are routed to `JolkLong.LONG_TYPE`.
     */
    @Test
    void testLongIntrinsicRouting() throws UnsupportedMessageException, UnknownIdentifierException, UnsupportedTypeException {
        eval("");
        context.enter();
        try {
            JolkDispatchNode dispatchNode = JolkDispatchNodeGen.create();
            adopt(dispatchNode);
            Long receiver = 42L;

            // Test an intrinsic like "hash"
            Object result = dispatchNode.execute(null, null, receiver, "hash", new Object[0]);
            assertEquals(42L, result, "Long intrinsic 'hash' should be handled correctly.");

            // Test an intrinsic like "isPresent"
            result = dispatchNode.execute(null, null, receiver, "isPresent", new Object[0]);
            assertEquals(true, result, "Long intrinsic 'isPresent' should return true.");

            // Test a non-intrinsic (should fall through to interop or JolkLong members)
            result = dispatchNode.execute(null, null, receiver, "+", new Object[]{10L});
            assertEquals(52L, result, "Long '+' operator should be handled by JolkLong members.");
        } finally {
            context.leave();
        }
    }

    /**
     * Tests that intrinsic messages to raw Java `Boolean` are routed to `JolkBoolean.BOOLEAN_TYPE`.
     */
    @Test
    void testBooleanIntrinsicRouting() throws UnsupportedMessageException, UnknownIdentifierException, UnsupportedTypeException {
        eval("");
        context.enter();
        try {
            JolkDispatchNode dispatchNode = JolkDispatchNodeGen.create();
            adopt(dispatchNode);
            Boolean receiver = true;

            // Test an intrinsic like "isPresent"
            Object result = dispatchNode.execute(null, null, receiver, "isPresent", new Object[0]);
            assertEquals(true, result, "Boolean intrinsic 'isPresent' should return true.");

            // Test an intrinsic like "isEmpty"
            result = dispatchNode.execute(null, null, receiver, "isEmpty", new Object[0]);
            assertEquals(false, result, "Boolean intrinsic 'isEmpty' should return false.");

            // Test a non-intrinsic (should fall through to interop or JolkBoolean members)
            result = dispatchNode.execute(null, null, receiver, "!", new Object[0]);
            assertEquals(false, result, "Boolean '!' operator should be handled by JolkBoolean members.");
        } finally {
            context.leave();
        }
    }

    /**
     * Tests that messages to raw Java {@link String} are routed to {@link JolkStringExtension#STRING_TYPE}.
     */
    @Test
    void testStringRouting() {
        eval("");
        context.enter();
        try {
            JolkDispatchNode dispatchNode = JolkDispatchNodeGen.create();
            adopt(dispatchNode);
            String receiver = "hello";

            // Jolk-native extension: #matches
            Object matches = dispatchNode.execute(null, null, receiver, "matches", new Object[]{"h.*o"});
            assertEquals(true, matches, "JolkString 'matches' method should work correctly.");

            // Host fallback: #length
            Object len = dispatchNode.execute(null, null, receiver, "length", new Object[0]);
            assertEquals(5L, len);

            // Host fallback: #toUpperCase
            Object upper = dispatchNode.execute(null, null, receiver, "toUpperCase", new Object[0]);
            assertEquals("HELLO", upper);
        } finally {
            context.leave();
        }
    }

    /**
     * Tests the Meta-Object Interceptor which allows sending #new to Java classes.
     */
    @Test
    void testMetaObjectNew() {
        eval("");
        context.enter();
        try {
            JolkDispatchNode dispatchNode = JolkDispatchNodeGen.create();
            adopt(dispatchNode);
            Class<?> receiver = java.lang.StringBuilder.class;

            // Jolk Unified Messaging: Class #new(...) maps to instantiation
            Object rawInstance = dispatchNode.execute(null, null, receiver, "new", new Object[]{"init"});
            Value instance = context.asValue(rawInstance); // Wrap in Polyglot Value
            assertTrue(instance.isHostObject(), "Result should be a host object.");
            assertEquals("init", instance.asHostObject().toString()); // Unwrap to StringBuilder for assertions
        } finally {
            context.leave();
        }
    }

    /**
     * Verifies Identity Congruence for intrinsic types (matching by value, not reference).
     */
    @Test
    void testIdentityCongruence() {
        eval("");
        context.enter();
        try {
            JolkDispatchNode dispatchNode = JolkDispatchNodeGen.create();
            adopt(dispatchNode);
            // Create Longs outside the JVM cache (-128 to 127)
            Long a = Long.valueOf(1000);
            Long b = Long.valueOf(1000);

            assertNotSame(a, b);
            Object isEqual = dispatchNode.execute(null, null, a, "==", new Object[]{b});
            assertEquals(true, isEqual, "Large Longs must match by value in the Jolk Identity protocol.");
            
            Object isEquivalent = dispatchNode.execute(null, null, "str", "~~", new Object[]{new String("str")});
            assertEquals(true, isEquivalent, "Strings must match by value equivalence.");
        } finally {
            context.leave();
        }
    }

    /**
     * Verifies the ternary logic and null-coalescing intrinsic dispatch.
     */
    @Test
    void testComplexIntrinsics() {
        eval("");
        context.enter();
        try {
            JolkDispatchNode dispatchNode = JolkDispatchNodeGen.create();
            adopt(dispatchNode);
            
            // ?? (Null-coalesce)
            JolkClosure fallback = new JolkClosure(new JolkRootNode(null, new JolkLiteralNode("fallback"), "test", false).getCallTarget());
            Object res1 = dispatchNode.execute(null, null, JolkNothing.INSTANCE, "??", new Object[]{fallback});
            assertEquals("fallback", res1);

            Object res2 = dispatchNode.execute(null, null, "present", "??", new Object[]{fallback});
            assertEquals("present", res2);

            // ? : (Ternary)
            JolkClosure thenBranch = new JolkClosure(new JolkRootNode(null, new JolkLiteralNode(1L), "test", false).getCallTarget());
            JolkClosure elseBranch = new JolkClosure(new JolkRootNode(null, new JolkLiteralNode(2L), "test", false).getCallTarget());
            
            Object branchT = dispatchNode.execute(null, null, true, "? :", new Object[]{thenBranch, elseBranch});
            assertEquals(1L, branchT);
            
            Object branchF = dispatchNode.execute(null, null, false, "? :", new Object[]{thenBranch, elseBranch});
            assertEquals(2L, branchF);
        } finally {
            context.leave();
        }
    }

    /**
     * Tests that `isObjectIntrinsic` correctly identifies all core object protocol messages.
     */
    @Test
    void testIsObjectIntrinsic() {
        assertTrue(JolkDispatchNode.isObjectIntrinsic("=="));
        assertTrue(JolkDispatchNode.isObjectIntrinsic("!="));
        assertTrue(JolkDispatchNode.isObjectIntrinsic("~~"));
        assertTrue(JolkDispatchNode.isObjectIntrinsic("!~"));
        assertTrue(JolkDispatchNode.isObjectIntrinsic("??"));
        assertTrue(JolkDispatchNode.isObjectIntrinsic("hash"));
        assertTrue(JolkDispatchNode.isObjectIntrinsic("toString"));
        assertTrue(JolkDispatchNode.isObjectIntrinsic("class"));
        assertTrue(JolkDispatchNode.isObjectIntrinsic("instanceOf"));
        assertTrue(JolkDispatchNode.isObjectIntrinsic("isPresent"));
        assertTrue(JolkDispatchNode.isObjectIntrinsic("isEmpty"));
        assertFalse(JolkDispatchNode.isObjectIntrinsic("someOtherMessage"));
    }

    @Test
    void testNullResultRestitution() {
        eval("");
        context.enter();
        try {
            JolkDispatchNode dispatchNode = JolkDispatchNodeGen.create();
            adopt(dispatchNode);
            // Verify raw nulls from members are restituted to Nothing
            Object result = dispatchNode.execute(null, null, JolkNothing.INSTANCE, "hash", new Object[0]);
            assertEquals(0L, result);

            Object str = dispatchNode.execute(null, null, JolkNothing.INSTANCE, "toString", new Object[0]);
            assertEquals("null", str); // JolkNothing.toString() returns the string "null"
        } finally {
            context.leave();
        }
    }

    /**
     * Tests automatic conversion of JolkClosures to Java functional interfaces.
     */
    @Test
    void testClosureToFunctionalInterfaceConversion() {
        eval("");
        context.enter();
        try {
            JolkDispatchNode dispatchNode = JolkDispatchNodeGen.create();
            
            // Adopt the dispatch node like in other tests
            JolkLanguage lang = com.oracle.truffle.api.TruffleLanguage.LanguageReference.create(JolkLanguage.class).get(null);
            DispatchNodeWrapper wrapper = new DispatchNodeWrapper(dispatchNode);
            new JolkRootNode(lang, wrapper).getCallTarget(); // Trigger adoption
            
            // Create test closures that will be converted to functional interfaces
            JolkClosure predicateClosure = createPredicateClosure((Object x) -> (Long) x > 5L);
            JolkClosure functionClosure = createFunctionClosure((Object x) -> (Long) x * 2L);
            JolkClosure consumerClosure = createConsumerClosure((Object x) -> { /* no-op */ });
            
            // Create a list using Java directly
            java.util.ArrayList<Long> list = new java.util.ArrayList<>();
            list.add(1L);
            list.add(2L);
            list.add(3L);
            list.add(6L);
            list.add(7L);
            list.add(8L);
            
            // Wrap in Polyglot Value
            Value listValue = context.asValue(list);
            
            // Test a simple method first
            Value toString = listValue.invokeMember("toString");
            assertTrue(toString.isString(), "toString() should work");
            
            // Wrap closures in Polyglot Values
            Value predicateValue = context.asValue(predicateClosure);
            Value functionValue = context.asValue(functionClosure);
            Value consumerValue = context.asValue(consumerClosure);
            
            // Test stream methods with functional interfaces
            Value stream = listValue.invokeMember("stream");
            
            // Test Predicate conversion with filter()
            Value filteredStream = stream.invokeMember("filter", predicateValue);
            Value resultList = filteredStream.invokeMember("toList");
            assertTrue(resultList.isHostObject(), "Predicate conversion should work for filter");
            java.util.List<?> filteredList = (java.util.List<?>) resultList.asHostObject();
            assertEquals(java.util.Arrays.asList(6L, 7L, 8L), filteredList);
            
            // Test Function conversion with map()
            stream = listValue.invokeMember("stream");
            Value mappedStream = stream.invokeMember("map", functionValue);
            resultList = mappedStream.invokeMember("toList");
            assertTrue(resultList.isHostObject(), "Function conversion should work for map");
            java.util.List<?> mappedList = (java.util.List<?>) resultList.asHostObject();
            assertEquals(java.util.Arrays.asList(2L, 4L, 6L, 12L, 14L, 16L), mappedList);
            
            // Test Consumer conversion with forEach()
            stream = listValue.invokeMember("stream");
            stream.invokeMember("forEach", consumerValue);
            // Consumer returns void, just verify no exception was thrown
            
        } finally {
            context.leave();
        }
    }

    /**
     * Helper class with methods that accept functional interfaces to test conversion.
     */
    public static class FunctionalInterfaceTestHelper {
        public boolean testPredicate(java.util.function.Predicate<Object> predicate, Object value) {
            return predicate.test(value);
        }
        
        public Object testFunction(java.util.function.Function<Object, Object> function, Object value) {
            return function.apply(value);
        }
        
        public String testConsumer(java.util.function.Consumer<Object> consumer, Object value) {
            consumer.accept(value);
            return "consumed";
        }
        
        public Object testSupplier(java.util.function.Supplier<Object> supplier) {
            return supplier.get();
        }
    }

    /**
     * Helper methods to create JolkClosures for testing.
     */
    private JolkClosure createPredicateClosure(java.util.function.Predicate<Object> predicate) {
        return new JolkClosure(null) {
            @Override
            public Object execute(Object[] arguments) {
                return predicate.test(arguments[0]);
            }
        };
    }
    
    private JolkClosure createFunctionClosure(java.util.function.Function<Object, Object> function) {
        return new JolkClosure(null) {
            @Override
            public Object execute(Object[] arguments) {
                return function.apply(arguments[0]);
            }
        };
    }
    
    private JolkClosure createConsumerClosure(java.util.function.Consumer<Object> consumer) {
        return new JolkClosure(null) {
            @Override
            public Object execute(Object[] arguments) {
                consumer.accept(arguments[0]);
                return JolkNothing.INSTANCE; // Return non-null value
            }
        };
    }
    
    @SuppressWarnings("unused")
    private JolkClosure createSupplierClosure(java.util.function.Supplier<Object> supplier) {
        return new JolkClosure(null) {
            @Override
            public Object execute(Object[] arguments) {
                return supplier.get();
            }
        };
    }
}