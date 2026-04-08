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
            Object result = dispatchNode.execute(null, JolkNothing.INSTANCE, "someArbitraryMessage", new Object[0]);
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
            return dispatchNode.execute(frame, frame.getArguments()[0], "get", new Object[0]);
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
            Long receiver = 42L;

            // Test an intrinsic like "hash"
            Object result = dispatchNode.execute(null, receiver, "hash", new Object[0]);
            assertEquals(42L, result, "Long intrinsic 'hash' should be handled correctly.");

            // Test an intrinsic like "isPresent"
            result = dispatchNode.execute(null, receiver, "isPresent", new Object[0]);
            assertEquals(true, result, "Long intrinsic 'isPresent' should return true.");

            // Test a non-intrinsic (should fall through to interop or JolkLong members)
            result = dispatchNode.execute(null, receiver, "+", new Object[]{10L});
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
            Boolean receiver = true;

            // Test an intrinsic like "isPresent"
            Object result = dispatchNode.execute(null, receiver, "isPresent", new Object[0]);
            assertEquals(true, result, "Boolean intrinsic 'isPresent' should return true.");

            // Test an intrinsic like "isEmpty"
            result = dispatchNode.execute(null, receiver, "isEmpty", new Object[0]);
            assertEquals(false, result, "Boolean intrinsic 'isEmpty' should return false.");

            // Test a non-intrinsic (should fall through to interop or JolkBoolean members)
            result = dispatchNode.execute(null, receiver, "!", new Object[0]);
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
            String receiver = "hello";

            // Jolk-native extension: #matches
            Object matches = dispatchNode.execute(null, receiver, "matches", new Object[]{"h.*o"});
            assertEquals(true, matches, "JolkString 'matches' method should work correctly.");

            // Host fallback: #length
            Object len = dispatchNode.execute(null, receiver, "length", new Object[0]);
            assertEquals(5L, len);

            // Host fallback: #toUpperCase
            Object upper = dispatchNode.execute(null, receiver, "toUpperCase", new Object[0]);
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
            Class<?> receiver = java.lang.StringBuilder.class;

            // Jolk Unified Messaging: Class #new(...) maps to instantiation
            Object rawInstance = dispatchNode.execute(null, receiver, "new", new Object[]{"init"});
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
            // Create Longs outside the JVM cache (-128 to 127)
            Long a = Long.valueOf(1000);
            Long b = Long.valueOf(1000);

            assertNotSame(a, b);
            Object isEqual = dispatchNode.execute(null, a, "==", new Object[]{b});
            assertEquals(true, isEqual, "Large Longs must match by value in the Jolk Identity protocol.");
            
            Object isEquivalent = dispatchNode.execute(null, "str", "~~", new Object[]{new String("str")});
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
            
            // ?? (Null-coalesce)
            JolkClosure fallback = new JolkClosure(new JolkRootNode(null, new JolkLiteralNode("fallback"), "test", false).getCallTarget());
            Object res1 = dispatchNode.execute(null, JolkNothing.INSTANCE, "??", new Object[]{fallback});
            assertEquals("fallback", res1);

            Object res2 = dispatchNode.execute(null, "present", "??", new Object[]{fallback});
            assertEquals("present", res2);

            // ? : (Ternary)
            JolkClosure thenBranch = new JolkClosure(new JolkRootNode(null, new JolkLiteralNode(1L), "test", false).getCallTarget());
            JolkClosure elseBranch = new JolkClosure(new JolkRootNode(null, new JolkLiteralNode(2L), "test", false).getCallTarget());
            
            Object branchT = dispatchNode.execute(null, true, "? :", new Object[]{thenBranch, elseBranch});
            assertEquals(1L, branchT);
            
            Object branchF = dispatchNode.execute(null, false, "? :", new Object[]{thenBranch, elseBranch});
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
            // Verify raw nulls from members are restituted to Nothing
            Object result = dispatchNode.execute(null, JolkNothing.INSTANCE, "hash", new Object[0]);
            assertEquals(0L, result);

            Object str = dispatchNode.execute(null, JolkNothing.INSTANCE, "toString", new Object[0]);
            assertEquals("null", str); // JolkNothing.toString() returns the string "null"
        } finally {
            context.leave();
        }
    }
}