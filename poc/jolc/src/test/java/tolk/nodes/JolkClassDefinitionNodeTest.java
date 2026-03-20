package tolk.nodes;

import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
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
    void testExecuteGenericWithMembers() throws UnsupportedMessageException, ArityException, UnknownIdentifierException, UnsupportedTypeException {
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

        // Verify intrinsic 'new' is also present (handled by JolkMetaClass)
        assertTrue(InteropLibrary.getUncached().isMemberInvocable(result, "new"));

        // Verify that the instance created from the meta class has the member
        Object instance = InteropLibrary.getUncached().invokeMember(result, "new");
        // The `hasMember` method exists on the polyglot `Value` API, but not directly on
        // InteropLibrary. The polyglot API implements this check by testing if the member
        // is either readable or invocable. We replicate that logic here.
        InteropLibrary interop = InteropLibrary.getUncached();
        assertTrue(interop.isMemberInvocable(instance, memberName) || interop.isMemberReadable(instance, memberName), "Instance should have member '" + memberName + "'");
    }
}
