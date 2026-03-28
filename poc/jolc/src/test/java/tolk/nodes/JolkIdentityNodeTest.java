package tolk.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class JolkIdentityNodeTest {

    @Test
    void testIdentityTrue() {
        Long obj = 1000L;
        JolkIdentityNode node = new JolkIdentityNode(new ValueNode(obj), new ValueNode(obj), false);
        // Since JolkIdentityNode doesn't use the frame, passing null is safe for this unit test
        assertTrue((Boolean) node.executeGeneric(null), "Identical objects should return true for ==");
    }

    @Test
    void testIdentityFalse() {
        JolkIdentityNode node = new JolkIdentityNode(new ValueNode(1000L), new ValueNode(2000L), false);
        assertFalse((Boolean) node.executeGeneric(null), "Distinct objects should return false for ==");
    }

    @Test
    void testNonIdentityTrue() {
        JolkIdentityNode node = new JolkIdentityNode(new ValueNode(1000L), new ValueNode(2000L), true);
        assertTrue((Boolean) node.executeGeneric(null), "Distinct objects should return true for !=");
    }

    @Test
    void testNonIdentityFalse() {
        Long obj = 1000L;
        JolkIdentityNode node = new JolkIdentityNode(new ValueNode(obj), new ValueNode(obj), true);
        assertFalse((Boolean) node.executeGeneric(null), "Identical objects should return false for !=");
    }

    // Simple helper node to provide constant values for testing
    static class ValueNode extends JolkNode {
        private final Object value;

        ValueNode(Object value) {
            this.value = value;
        }

        @Override
        public Object executeGeneric(VirtualFrame frame) {
            return value;
        }
    }
}
