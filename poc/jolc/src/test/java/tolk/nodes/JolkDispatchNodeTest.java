package tolk.nodes;

import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import tolk.JolcTestBase;
import tolk.runtime.JolkNothing;

import java.util.HashMap;
import java.util.Map;
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
        JolkDispatchNode dispatchNode = JolkDispatchNodeGen.create();
        Object result = dispatchNode.executeDispatch(JolkNothing.INSTANCE, "someArbitraryMessage", new Object[0]);
        assertEquals(JolkNothing.INSTANCE, result, "JolkNothing should absorb any message and return itself.");
    }

    /**
     * Tests that raw JVM `null` values returned from interop calls are
     * "lifted" to {@link JolkNothing.INSTANCE} (Identity Restitution).
     * This specifically targets the `if (result == null)` check in `doDispatch`.
     */
    @Test
    @Disabled("TODO reactivate when interop is finalized")
    void testIdentityRestitutionForNull() throws UnsupportedMessageException, UnknownIdentifierException, UnsupportedTypeException {
        JolkDispatchNode dispatchNode = JolkDispatchNodeGen.create();
        // Use a standard Java Map (Host Object) to test raw null restitution.
        // Host objects are allowed to return raw nulls via Interop, which 
        // Jolk must then lift to Nothing.
        Map<String, Object> hostMap = new HashMap<>();
        // We wrap the host object in a Polyglot Value to ensure it is treated as a 
        // Host Object with member support by the InteropLibrary.
        Object wrappedMap = context.asValue(hostMap);

        Object result = dispatchNode.executeDispatch(wrappedMap, "get", new Object[]{"missingKey"});
        assertEquals(JolkNothing.INSTANCE, result, "Raw JVM null should be restituted to JolkNothing.INSTANCE.");
    }

    /**
     * Tests that intrinsic messages to raw Java `Long` are routed to `JolkLong.LONG_TYPE`.
     */
    @Test
    void testLongIntrinsicRouting() throws UnsupportedMessageException, UnknownIdentifierException, UnsupportedTypeException {
        JolkDispatchNode dispatchNode = JolkDispatchNodeGen.create();
        Long receiver = 42L;

        // Test an intrinsic like "hash"
        Object result = dispatchNode.executeDispatch(receiver, "hash", new Object[0]);
        assertEquals(42L, result, "Long intrinsic 'hash' should be handled correctly.");

        // Test an intrinsic like "isPresent"
        result = dispatchNode.executeDispatch(receiver, "isPresent", new Object[0]);
        assertEquals(true, result, "Long intrinsic 'isPresent' should return true.");

        // Test a non-intrinsic (should fall through to interop or JolkLong members)
        // For this test, we'll assume JolkLong has a '+' operator
        result = dispatchNode.executeDispatch(receiver, "+", new Object[]{10L});
        assertEquals(52L, result, "Long '+' operator should be handled by JolkLong members.");
    }

    /**
     * Tests that intrinsic messages to raw Java `Boolean` are routed to `JolkBoolean.BOOLEAN_TYPE`.
     */
    @Test
    void testBooleanIntrinsicRouting() throws UnsupportedMessageException, UnknownIdentifierException, UnsupportedTypeException {
        JolkDispatchNode dispatchNode = JolkDispatchNodeGen.create();
        Boolean receiver = true;

        // Test an intrinsic like "isPresent"
        Object result = dispatchNode.executeDispatch(receiver, "isPresent", new Object[0]);
        assertEquals(true, result, "Boolean intrinsic 'isPresent' should return true.");

        // Test an intrinsic like "isEmpty"
        result = dispatchNode.executeDispatch(receiver, "isEmpty", new Object[0]);
        assertEquals(false, result, "Boolean intrinsic 'isEmpty' should return false.");

        // Test a non-intrinsic (should fall through to interop or JolkBoolean members)
        // For this test, we'll assume JolkBoolean has a '!' operator
        result = dispatchNode.executeDispatch(receiver, "!", new Object[0]);
        assertEquals(false, result, "Boolean '!' operator should be handled by JolkBoolean members.");
    }

    /**
     * Tests that `isObjectIntrinsic` correctly identifies all core object protocol messages.
     */
    @Test
    void testIsObjectIntrinsic() {
        JolkDispatchNode dispatchNode = JolkDispatchNodeGen.create(); // Need an instance to call non-static method
        assertTrue(dispatchNode.isObjectIntrinsic("=="));
        assertTrue(dispatchNode.isObjectIntrinsic("!="));
        assertTrue(dispatchNode.isObjectIntrinsic("~~"));
        assertTrue(dispatchNode.isObjectIntrinsic("!~"));
        assertTrue(dispatchNode.isObjectIntrinsic("??"));
        assertTrue(dispatchNode.isObjectIntrinsic("hash"));
        assertTrue(dispatchNode.isObjectIntrinsic("toString"));
        assertTrue(dispatchNode.isObjectIntrinsic("class"));
        assertTrue(dispatchNode.isObjectIntrinsic("instanceOf"));
        assertTrue(dispatchNode.isObjectIntrinsic("isPresent"));
        assertTrue(dispatchNode.isObjectIntrinsic("isEmpty"));
        assertFalse(dispatchNode.isObjectIntrinsic("someOtherMessage"));
    }

    @Test
    void testNullResultRestitution() {
        JolkDispatchNode dispatchNode = JolkDispatchNodeGen.create();
        // Verify raw nulls from members are restituted to Nothing
        Object result = dispatchNode.executeDispatch(JolkNothing.INSTANCE, "hash", new Object[0]);
        assertEquals(0L, result);

        Object str = dispatchNode.executeDispatch(JolkNothing.INSTANCE, "toString", new Object[0]);
        assertEquals("null", str);
    }
}