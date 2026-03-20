package tolk.runtime;

import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.InvalidArrayIndexException;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;

import java.util.HashSet;
import java.util.Set;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/// ## JolkMetaClassTest
///
/// Tests for the JolkMetaClass implementation,
/// verifying that it correctly handles meta-level messaging.
///
public class JolkMetaClassTest {

    private JolkMetaClass metaClass;

    @BeforeEach
    void setUp() {
        metaClass = new JolkMetaClass("MyClass", JolkFinality.OPEN, JolkVisibility.PUBLIC, JolkArchetype.CLASS, Collections.emptyMap());
    }

    @Test
    void testIsMetaObject() {
        assertTrue(metaClass.isMetaObject(), "Should identify as a meta object");
    }

    @Test
    void testMetaName() {
        assertEquals("MyClass", metaClass.getMetaQualifiedName());
        assertEquals("MyClass", metaClass.getMetaSimpleName());
    }

    @Test
    void testIsMetaInstance() {
        assertFalse(metaClass.isMetaInstance("someInstance"), "Instance checking is not yet implemented");
    }

    @Test
    void testHasMembers() throws UnsupportedMessageException {
        assertTrue(metaClass.hasMembers());
    }

    @Test
    void testGetMembers() throws UnsupportedMessageException, InvalidArrayIndexException {
        Object membersObj = metaClass.getMembers(true);
        assertTrue(membersObj instanceof JolkMemberNames);
        JolkMemberNames members = (JolkMemberNames) membersObj;

        assertTrue(members.hasArrayElements());
        assertEquals(4, members.getArraySize());

        Set<String> names = new HashSet<>();
        for (long i = 0; i < members.getArraySize(); i++) {
            names.add((String) members.readArrayElement(i));
        }
        assertTrue(names.contains("new"));
        assertTrue(names.contains("name"));
        assertTrue(names.contains("superclass"));
        assertTrue(names.contains("isInstance"));
    }

    @Test
    void testIsMemberInvocable() {
        assertTrue(metaClass.isMemberInvocable("new"));
        assertFalse(metaClass.isMemberInvocable("randomMethod"));
    }

    @Test
    void testInvokeNew() throws UnknownIdentifierException, ArityException, UnsupportedTypeException, UnsupportedMessageException {
        Object result = metaClass.invokeMember("new", new Object[]{});
        assertNotNull(result, "The result of #new should not be null");
        assertTrue(result instanceof JolkObject, "The result should be an instance of JolkObject");
    }

    @Test
    void testInvokeNewWithArgumentsThrowsArityException() {
        assertThrows(ArityException.class, () -> {
            metaClass.invokeMember("new", new Object[]{"unexpectedArg"});
        });
    }

    @Test
    void testInvokeUnknownMemberThrowsException() {
        assertThrows(UnknownIdentifierException.class, () -> {
            metaClass.invokeMember("unknown", new Object[]{});
        });
    }

    @Test
    void testReadMemberThrowsException() {
        // JolkMetaClass currently does not expose readable properties (only invocable "new")
        assertThrows(UnknownIdentifierException.class, () -> {
            metaClass.readMember("new");
        });
    }
}
