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
    void testDispatchToNothingError() {
        // isEmpty takes 0 arguments, passing 1 should fail
        TestDispatchNode node = new TestDispatchNode(JolkNothing.INSTANCE, "isEmpty", new Object[]{"extra"});
        RuntimeException ex = assertThrows(RuntimeException.class, () -> execute(node));
        assertTrue(ex.getMessage().contains("Message dispatch failed"), "Should catch ArityException for Nothing");
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
    void testDispatchToLong() {
        // Test protocol message
        TestDispatchNode node1 = new TestDispatchNode(42L, "toString", new Object[]{});
        assertEquals("42", execute(node1), "Long #toString should return string representation");

        // Test arithmetic message
        TestDispatchNode node2 = new TestDispatchNode(10L, "+", new Object[]{20L});
        assertEquals(30L, execute(node2), "Long #+ should perform addition");
    }

    @Test
    void testDispatchToLongError() {
        // Passing wrong type to '+' to trigger UnsupportedTypeException inside LongAdd
        TestDispatchNode node = new TestDispatchNode(10L, "+", new Object[]{"not a number"});
        RuntimeException ex = assertThrows(RuntimeException.class, () -> execute(node));
        assertTrue(ex.getMessage().contains("Error executing #+ on Long"), "Should wrap intrinsic execution errors");
    }

    @Test
    void testDispatchToBoolean() {
        // Test protocol message
        TestDispatchNode node1 = new TestDispatchNode(true, "toString", new Object[]{});
        assertEquals("true", execute(node1), "Boolean #toString should return string representation");

        // Test logic message
        TestDispatchNode node2 = new TestDispatchNode(true, "&&", new Object[]{false});
        assertEquals(false, execute(node2), "Boolean #&& should perform logical AND");
    }

    @Test
    void testDispatchToBooleanError() {
        // Passing wrong type to '&&' to trigger UnsupportedTypeException inside BooleanAnd
        TestDispatchNode node = new TestDispatchNode(true, "&&", new Object[]{"not a boolean"});
        RuntimeException ex = assertThrows(RuntimeException.class, () -> execute(node));
        assertTrue(ex.getMessage().contains("Error executing #&& on Boolean"), "Should wrap intrinsic execution errors");
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
