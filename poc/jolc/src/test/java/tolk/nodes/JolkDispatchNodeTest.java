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

import static org.junit.jupiter.api.Assertions.*;

public class JolkDispatchNodeTest {

    @Test
    void testDispatchToNothing() {
        TestDispatchNode node = new TestDispatchNode(JolkNothing.INSTANCE, "isEmpty", new Object[]{});
        Object result = execute(node);
        assertEquals(true, result, "Nothing #isEmpty should be true");
    }

    @Test
    void testDispatchToObject() {
        JolkMetaClass meta = new JolkMetaClass("Test", JolkFinality.OPEN, JolkVisibility.PUBLIC, JolkArchetype.CLASS, Collections.emptyMap());
        JolkObject obj = new JolkObject(meta);
        TestDispatchNode node = new TestDispatchNode(obj, "toString", new Object[]{});
        
        Object result = execute(node);
        assertEquals("instance of Test", result);
    }

    @Test
    void testDispatchUnknownSelector() {
        JolkMetaClass meta = new JolkMetaClass("Test", JolkFinality.OPEN, JolkVisibility.PUBLIC, JolkArchetype.CLASS, Collections.emptyMap());
        JolkObject obj = new JolkObject(meta);
        TestDispatchNode node = new TestDispatchNode(obj, "unknown", new Object[]{});
        
        RuntimeException ex = assertThrows(RuntimeException.class, () -> execute(node));
        assertTrue(ex.getMessage().contains("Message dispatch failed"));
    }

    @Test
    void testDispatchArityError() {
        JolkMetaClass meta = new JolkMetaClass("Test", JolkFinality.OPEN, JolkVisibility.PUBLIC, JolkArchetype.CLASS, Collections.emptyMap());
        JolkObject obj = new JolkObject(meta);
        // hash takes 0 arguments, passing 1 should fail
        TestDispatchNode node = new TestDispatchNode(obj, "hash", new Object[]{"extra"});
        
        RuntimeException ex = assertThrows(RuntimeException.class, () -> execute(node));
        assertTrue(ex.getMessage().contains("Message dispatch failed"));
    }

    private Object execute(JolkNode node) {
        // Wrap in RootNode to support Truffle DSL caching and adoption
        JolkRootNode root = new JolkRootNode(null, node);
        return root.getCallTarget().call();
    }

    // Helper node to exercise JolkDispatchNode within a Truffle context
    static class TestDispatchNode extends JolkNode {
        @Child JolkDispatchNode dispatchNode = JolkDispatchNodeGen.create();
        private final Object receiver;
        private final String selector;
        private final Object[] args;

        TestDispatchNode(Object receiver, String selector, Object[] args) {
            this.receiver = receiver;
            this.selector = selector;
            this.args = args;
        }

        @Override
        public Object executeGeneric(VirtualFrame frame) {
            return dispatchNode.executeDispatch(receiver, selector, args);
        }
    }
}
