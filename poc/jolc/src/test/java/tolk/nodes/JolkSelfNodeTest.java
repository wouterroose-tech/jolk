package tolk.nodes;

import org.junit.jupiter.api.Test;
import tolk.JolcTestBase;
import tolk.language.JolkLanguage;
import tolk.runtime.JolkNothing;
import static org.junit.jupiter.api.Assertions.*;

///
/// Verifies the behavior of the [JolkSelfNode].
///
public class JolkSelfNodeTest extends JolcTestBase {

    ///
    /// Tests that the self node correctly retrieves the receiver 
    /// (the first argument in the frame).
    ///
    @Test
    void testExecuteWithReceiver() {
        eval(""); // Initialize context and language
        context.enter();
        try {
            JolkLanguage lang = com.oracle.truffle.api.TruffleLanguage.LanguageReference.create(JolkLanguage.class).get(null);
            Object receiver = new Object();
            JolkSelfNode node = new JolkSelfNode();
            
            JolkRootNode root = new JolkRootNode(lang, node);
            Object result = root.getCallTarget().call(receiver);
            
            // Identity Restitution: result is a lifted HostObject, so we must unwrap to compare
            assertSame(receiver, JolkNode.unwrap(result), "Self node should return the receiver from frame arguments.");
        } finally {
            context.leave();
        }
    }

    ///
    /// Tests that the self node returns JolkNothing when executed without a valid frame context.
    ///
    @Test
    void testExecuteWithoutFrame() {
        JolkSelfNode node = new JolkSelfNode();
        assertSame(JolkNothing.INSTANCE, node.executeGeneric(null));
    }
}
