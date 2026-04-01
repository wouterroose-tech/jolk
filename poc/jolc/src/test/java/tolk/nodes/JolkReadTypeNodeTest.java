package tolk.nodes;

import static org.junit.jupiter.api.Assertions.*;

import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import tolk.JolcTestBase;
import tolk.language.JolkLanguage;
import tolk.runtime.JolkArchetype;
import tolk.runtime.JolkFinality;
import tolk.runtime.JolkMetaClass;
import tolk.runtime.JolkNothing;
import tolk.runtime.JolkVisibility;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * ## JolkReadTypeNodeTest
 * 
 * Verifies the resolution logic of the {@link JolkReadTypeNode}.
 * 
 * This test ensures that uppercase identifiers are resolved with the correct 
 * priority: first as global types in the registry, and second as meta-members 
 * (constants/fields) via a fallback receiver.
 */
public class JolkReadTypeNodeTest extends JolcTestBase {

    private Object lastRawResult;

    /**
     * Verifies that the node correctly resolves a globally registered class name.
     */
    @Test
    void testResolveGlobalType() throws UnsupportedMessageException {
        String typeName = "GlobalType";
        
        // Register the class via the Jolk engine. This ensures the MetaClass 
        // is properly registered in the JolkContext with full language support.
        eval("class " + typeName + " {}");

        JolkReadTypeNode node = new JolkReadTypeNode(typeName);
        Value result = execute(node);

        assertNotNull(result);
        assertTrue(unwrap(result, JolkMetaClass.class) instanceof JolkMetaClass, "The result should be a JolkMetaClass instance.");
        
        // Use Interop to verify the identity/name, avoiding Polyglot wrapper mismatches.
        assertEquals(typeName, getMetaSimpleName(result),
            "The node should resolve a globally registered class by name.");
    }

    /**
     * Verifies the fallback mechanism where an identifier is treated as a 
     * message send to a meta-receiver (e.g. for internal constants like FORTY_TWO).
     */
    @Test
    @Disabled("activate once the fallback logic is implemented in JolkReadTypeNode")
    void testResolveMetaConstantFallback() {
        String constantName = "FORTY_TWO";
        long expectedValue = 42L;

        // In Jolk, constants represent meta-state. We populate the metaFields 
        // map (arg 9) to ensure the node resolves the identifier via the 
        // interop.readMember() protocol.
        Map<String, Object> metaFields = new HashMap<>();
        metaFields.put(constantName, expectedValue);
        
        JolkMetaClass metaReceiverObj = new JolkMetaClass(
            "Constants", null, JolkFinality.FINAL, JolkVisibility.PUBLIC, JolkArchetype.CLASS, 
            Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(), metaFields
        );

        // Setup the node with a receiver that returns our meta-object
        JolkNode receiverNode = new JolkLiteralNode(metaReceiverObj);
        JolkReadTypeNode node = new JolkReadTypeNode(constantName, receiverNode);

        Value result = execute(node);

        assertEquals(expectedValue, asLong(result), "The node should fall back to reading a meta-constant if no global type matches.");
    }

    /**
     * Verifies that if no type is found and the fallback fails (or is absent), 
     * the node evaluates to JolkNothing.
     */
    @Test
    void testResolveUnknownReturnsNothing() {
        JolkReadTypeNode node = new JolkReadTypeNode("NonExistentType");
        Value result = execute(node);

        assertTrue(isNull(result), "Unknown types should resolve to JolkNothing.");
    }

    /**
     * Verifies that the node returns JolkNothing if the meta-receiver itself 
     * evaluates to Nothing.
     */
    @Test
    void testResolveWithNothingReceiver() {
        JolkReadTypeNode node = new JolkReadTypeNode("SOME_CONSTANT", new JolkLiteralNode(JolkNothing.INSTANCE));
        Value result = execute(node);

        assertTrue(isNull(result));
    }

    private String getMetaSimpleName(Value v) {
        context.enter();
        try {
            return v.getMetaSimpleName();
        } finally {
            context.leave();
        }
    }

    private long asLong(Value v) {
        // Prioritize the raw result captured during execution to bypass Polyglot 
        // proxying and potential boolean coercion of JolkNothing.
        if (lastRawResult instanceof Number n) return n.longValue();
        
        context.enter();
        try {
            if (v.fitsInLong()) {
                return v.asLong();
            }
            throw new AssertionError(String.format(
                "Expected a numeric result, but got: %s (Raw: %s)", v, lastRawResult
            ));
        } finally {
            context.leave();
        }
    }

    /**
     * ### isNull
     * 
     * Safely checks if a [Value] represents the JolkNothing identity.
     * 
     * @param v The value to check.
     * @return true if the value is null or JolkNothing.
     */
    private boolean isNull(Value v) {
        // For JolkNothing, we rely on the raw object identity for correctness.
        // The Polyglot Value.isNull() might return false if JolkNothing doesn't
        // explicitly export the isNull interop message, or if the context is not entered.
        // By checking lastRawResult directly, we bypass these potential issues.
        return lastRawResult == JolkNothing.INSTANCE || lastRawResult == null;
    }

    private <T> T unwrap(Value v, Class<T> clazz) {
        // Prioritize the raw result to bypass Polyglot proxying of internal types
        if (lastRawResult != null && clazz.isInstance(lastRawResult)) {
            return clazz.cast(lastRawResult);
        }
        context.enter();
        try {
            Object raw = v.as(Object.class);
            if (clazz.isInstance(raw)) return clazz.cast(raw);
            return v.as(clazz);
        } finally {
            context.leave();
        }
    }

    private Value execute(JolkNode node) {
        // Evaluate an empty string to ensure the context is initialized and entered.
        eval("");
        
        context.enter();
        try {
            JolkLanguage lang = com.oracle.truffle.api.TruffleLanguage.LanguageReference.create(JolkLanguage.class).get(null);
            JolkRootNode root = new JolkRootNode(lang, node);
            this.lastRawResult = root.getCallTarget().call();
            return context.asValue(lastRawResult);
        } finally {
            context.leave();
        }
    }
}
