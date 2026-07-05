package tolk.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import org.junit.jupiter.api.Test;
import tolk.runtime.JolkArchetype;
import tolk.runtime.JolkFinality;
import tolk.runtime.JolkMetaClass;
import tolk.runtime.JolkNothing;
import tolk.runtime.JolkObject;
import tolk.runtime.JolkVisibility;
import tolk.JolcTestBase;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * ## JolkMessageSendNodeTest
 *
 * Verifies the behavior of the {@link JolkMessageSendNode}, which is responsible for
 * initiating message dispatches to a receiver object.
 */
class JolkMessageSendNodeTest extends JolcTestBase {

    @Test
    void testMessageToNothing() {
        eval(""); // Initialize context
        context.enter();
        try {
            // Setup: Receiver is JolkNothing.INSTANCE, Selector is "isEmpty"
            JolkNode receiver = new ValueNode(JolkNothing.INSTANCE);
            JolkNode[] args = new JolkNode[]{};
        JolkNode node = JolkMessageSendNodeGen.create("isEmpty", args, receiver);

            Object result = execute(node);
            assertEquals(true, result, "Nothing #isEmpty should be true");
        } finally {
            context.leave();
        }
    }

    @Test
    void testMessageToObject() {
        eval(""); // Initialize context
        context.enter();
        try {
            // Setup JolkObject
            JolkMetaClass meta = new JolkMetaClass("Test", JolkFinality.OPEN, JolkVisibility.PUBLIC, JolkArchetype.CLASS, Collections.emptyMap());
            JolkObject obj = new JolkObject(meta);

            JolkNode receiver = new ValueNode(obj);
            JolkNode[] args = new JolkNode[]{};
        JolkNode node = JolkMessageSendNodeGen.create("toString", args, receiver);

            Object result = execute(node);
            assertEquals("instance of Test", result.toString());
        } finally {
            context.leave();
        }
    }
    
    @Test
    void testMessageWithArguments() {
        // Using JolkObject #== (identity) which takes one argument
        JolkMetaClass meta = new JolkMetaClass("Test", JolkFinality.OPEN, JolkVisibility.PUBLIC, JolkArchetype.CLASS, Collections.emptyMap());
        JolkObject obj = new JolkObject(meta);
        eval(""); // Initialize context
        context.enter();
        try {
            JolkNode receiver = new ValueNode(obj);
            JolkNode arg1 = new ValueNode(obj); // compare to self
            JolkNode node = JolkMessageSendNodeGen.create("==", new JolkNode[]{arg1}, receiver);

            Object result = execute(node);
            assertEquals(true, result);
        } finally {
            context.leave();
        }
    }

    // Helper method to execute a JolkNode within a proper Truffle context,
    // wrapping it in a JolkRootNode and calling its CallTarget.
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