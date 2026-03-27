package tolk.nodes;

import org.junit.jupiter.api.Test;
import tolk.runtime.JolkNothing;
import static org.junit.jupiter.api.Assertions.*;

///
/// Verifies the behavior of the [JolkSelfNode].
///
public class JolkSelfNodeTest {

    ///
    /// Tests that the self node correctly retrieves the receiver 
    /// (the first argument in the frame).
    ///
    @Test
    void testExecuteWithReceiver() {
        Object receiver = new Object();
        JolkSelfNode node = new JolkSelfNode();
        
        JolkRootNode root = new JolkRootNode(null, node);
        Object result = root.getCallTarget().call(receiver);
        
        assertSame(receiver, result, "Self node should return the receiver from frame arguments.");
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
