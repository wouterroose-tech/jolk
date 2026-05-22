package tolk.nodes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.Test;
import com.oracle.truffle.api.TruffleLanguage;
import tolk.JolcTestBase;
import tolk.language.JolkLanguage;

/// # JolkMethodReferenceDispatchNodeTest
///
/// Verifies the **Dual-Mode Reference** protocol implemented in 
/// [JolkMethodReferenceDispatchNode].
///
/// These tests ensure that method references (##) correctly differentiate 
/// between bound instance calls, static meta-object calls, and unbound 
/// instance calls on types based on the runtime heuristic.
public class JolkMethodReferenceDispatchNodeTest extends JolcTestBase {

    /**
     * ### testBoundInstanceReference
     * 
     * Verifies a bound instance reference (e.g. `self ##doRun`).
     * The receiver is fixed at reification time.
     */
    @Test
    void testBoundInstanceReference() {
        eval("");
        context.enter();
        try {
            // Define a class with an instance method
            Value meta = eval("class Test { doRun() { ^ \"called\" } }");
            assertNotNull(meta, "Class definition evaluation failed to return the meta-object. Verify JolkVisitor name resolution.");
            Value instance = meta.invokeMember("new");

            // Reify reference: instance ##doRun
            JolkLanguage lang = TruffleLanguage.LanguageReference.create(JolkLanguage.class).get(null);
            // Impedance Resolution: Extract the raw guest JolkObject to avoid Polyglot proxy interference.
            Object guestInstance = JolkNode.unwrap(instance.as(Object.class));
            JolkNode receiverNode = new JolkLiteralNode(guestInstance);
            JolkMethodReferenceDispatchNode dispatch = new JolkMethodReferenceDispatchNode("doRun", receiverNode);
            
            // Execute as if from a closure call: [env]
            JolkRootNode root = new JolkRootNode(lang, dispatch, "test", false);
            Object result = root.getCallTarget().call(new Object[]{null}); 
            
            assertEquals("called", result.toString());
        } finally {
            context.leave();
        }
    }

    /**
     * ### testUnboundTypeReference
     * 
     * Verifies an unbound instance reference on a type (e.g. `String ##toUpperCase`).
     * The heuristic should pivot to unbound mode because the meta-object 
     * does not have a static method matching the selector.
     */

    /**
     * ### testUnboundTypeReference
     * 
     * Verifies reification of `String ##toUpperCase`. The runtime must detect 
     * this as an instance method and pivot to Unbound Mode.
     */
    @Test
    void testUnboundTypeReference() {
        Value closure = eval("class Test { meta Object get() { ^ String ##toUpperCase } }").invokeMember("get");
        assertEquals("HELLO", closure.execute("hello").toString());
    }

    /**
     * ### testStaticTypeReference
     * 
     * Verifies reification of `String ##valueOf`. The runtime must detect 
     * this as a static method and remain in Bound Mode.
     */
    @Test
    void testStaticTypeReference() {
        Value closure = eval("class Test { meta Object get() { ^ String ##valueOf } }").invokeMember("get");
        assertEquals("123", closure.execute(123L).toString());
    }

    /**
     * ### testArgumentForwarding
     * 
     * Verifies that additional closure arguments are correctly shifted to the 
     * dispatch call (e.g. `receiver #matches(regex)`).
     */
    @Test
    void testArgumentForwarding() {
        Value closure = eval("class Test { meta Object get() { ^ String ##matches } }").invokeMember("get");
        assertEquals(true, closure.execute("12345", "\\d+").asBoolean());
    }
}
