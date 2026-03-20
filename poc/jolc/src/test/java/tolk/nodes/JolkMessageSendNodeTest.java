package tolk.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import org.junit.jupiter.api.Test;
import tolk.runtime.JolkArchetype;
import tolk.runtime.JolkFinality;
import tolk.runtime.JolkMetaClass;
import tolk.runtime.JolkNothing;
import tolk.runtime.JolkObject;
import tolk.runtime.JolkVisibility;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JolkMessageSendNodeTest {

    @Test
    void testMessageToNothing() {
        // Setup: Receiver is JolkNothing.INSTANCE, Selector is "isEmpty"
        JolkNode receiver = new ValueNode(JolkNothing.INSTANCE);
        JolkNode[] args = new JolkNode[]{};
        JolkMessageSendNode node = new JolkMessageSendNode(receiver, "isEmpty", args);

        Object result = execute(node);
        
        assertEquals(true, result, "Nothing #isEmpty should be true");
    }

    @Test
    void testMessageToObject() {
        // Setup JolkObject
        JolkMetaClass meta = new JolkMetaClass("Test", JolkFinality.OPEN, JolkVisibility.PUBLIC, JolkArchetype.CLASS, Collections.emptyMap());
        JolkObject obj = new JolkObject(meta);

        JolkNode receiver = new ValueNode(obj);
        JolkNode[] args = new JolkNode[]{};
        JolkMessageSendNode node = new JolkMessageSendNode(receiver, "toString", args);

        Object result = execute(node);
        
        assertEquals("instance of Test", result);
    }
    
    @Test
    void testMessageWithArguments() {
        // Using JolkObject #== (identity) which takes one argument
        JolkMetaClass meta = new JolkMetaClass("Test", JolkFinality.OPEN, JolkVisibility.PUBLIC, JolkArchetype.CLASS, Collections.emptyMap());
        JolkObject obj = new JolkObject(meta);

        JolkNode receiver = new ValueNode(obj);
        JolkNode arg1 = new ValueNode(obj); // compare to self
        JolkMessageSendNode node = new JolkMessageSendNode(receiver, "==", new JolkNode[]{arg1});

        Object result = execute(node);
        
        assertEquals(true, result);
    }

    private Object execute(JolkNode node) {
        // Wrap in RootNode to support Truffle DSL caching and adoption
        JolkRootNode root = new JolkRootNode(null, node);
        return root.getCallTarget().call();
    }

    // Helper node for constant values
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