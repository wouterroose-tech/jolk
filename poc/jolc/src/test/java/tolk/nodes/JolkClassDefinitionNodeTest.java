package tolk.nodes;

import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import org.junit.jupiter.api.Test;
import tolk.runtime.JolkArchetype;
import tolk.runtime.JolkFinality;
import tolk.runtime.JolkMetaClass;
import tolk.runtime.JolkVisibility;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class JolkClassDefinitionNodeTest {

    @Test
    void testExecuteGenericReturnsMetaClass() {
        String className = "MyTestClass";
        JolkClassDefinitionNode node = new JolkClassDefinitionNode(
            className,
            JolkFinality.OPEN,
            JolkVisibility.PUBLIC,
            JolkArchetype.CLASS
        );

        Object result = node.executeGeneric(null);

        assertNotNull(result);
        assertTrue(result instanceof JolkMetaClass);
        assertTrue(InteropLibrary.getUncached().isMetaObject(result));
        
        try {
            assertEquals(className, InteropLibrary.getUncached().getMetaSimpleName(result));
            assertEquals(className, InteropLibrary.getUncached().getMetaQualifiedName(result));
        } catch (UnsupportedMessageException e) {
            fail("Should support meta name messages");
        }
    }

    @Test
    void testExecuteGenericWithMembers() {
        String className = "MemberClass";
        Map<String, Object> members = new HashMap<>();
        String memberName = "testMember";
        members.put(memberName, new JolkMemberNode(memberName));

        JolkClassDefinitionNode node = new JolkClassDefinitionNode(
            className,
            JolkFinality.FINAL,
            JolkVisibility.PUBLIC,
            JolkArchetype.CLASS,
            members
        );

        Object result = node.executeGeneric(null);

        assertTrue(result instanceof JolkMetaClass);
        assertTrue(InteropLibrary.getUncached().isMemberReadable(result, memberName));
        // Verify intrinsic 'new' is also present (handled by JolkMetaClass)
        assertTrue(InteropLibrary.getUncached().isMemberInvocable(result, "new"));
    }
}
