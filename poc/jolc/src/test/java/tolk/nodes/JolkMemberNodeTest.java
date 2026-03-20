package tolk.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import org.junit.jupiter.api.Test;

import tolk.runtime.JolkNothing;

import static org.junit.jupiter.api.Assertions.*;

class JolkMemberNodeTest {

    @Test
    void testExecuteGeneric() {
        JolkMemberNode node = new JolkMemberNode("test");
        // executeGeneric should return the node itself to act as a TruffleObject
        assertSame(node, node.executeGeneric(null));
    }

    @Test
    void testToString() {
        JolkMemberNode node = new JolkMemberNode("myMember");
        assertEquals("Member(myMember)", node.toString());
    }

    @Test
    void testIsExecutable() {
        JolkMemberNode node = new JolkMemberNode("test");
        assertTrue(node.isExecutable());
    }

    @Test
    void testExecuteWithNoBody() {
        JolkMemberNode node = new JolkMemberNode("empty");
        // Direct call to package-private method
        Object result = node.execute(new Object[]{});
        assertSame(JolkNothing.INSTANCE, result, "Should return JolkNothing when no body is present");
    }

    @Test
    void testExecuteWithBody() {
        JolkNode body = new JolkNode() {
            @Override
            public Object executeGeneric(VirtualFrame frame) {
                return "success";
            }
        };
        JolkMemberNode node = new JolkMemberNode("method", body);
        
        Object result = node.execute(new Object[]{});
        assertEquals("success", result);
    }

    @Test
    void testExecuteWithNullReturningBody() {
        JolkNode body = new JolkNode() {
            @Override
            public Object executeGeneric(VirtualFrame frame) {
                return null;
            }
        };
        JolkMemberNode node = new JolkMemberNode("voidMethod", body);
        
        Object result = node.execute(new Object[]{});
        assertSame(JolkNothing.INSTANCE, result, "Should return JolkNothing when body returns null");
    }
}