package tolk.runtime;

import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.InvalidArrayIndexException;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;

import java.util.HashSet;
import java.util.Map;
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
        Map<String, Object> metaMembers = Collections.singletonMap("VERSION", 1);
        metaClass = new JolkMetaClass("MyClass", JolkFinality.OPEN, JolkVisibility.PUBLIC, JolkArchetype.CLASS, Collections.emptyMap(), metaMembers);
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
    void testIsMetaInstanceHierarchy() {
        JolkMetaClass parent = new JolkMetaClass("Parent", null, JolkFinality.OPEN, JolkVisibility.PUBLIC, JolkArchetype.CLASS, Collections.emptyMap(), Collections.emptyMap());
        JolkMetaClass child = new JolkMetaClass("Child", parent, JolkFinality.OPEN, JolkVisibility.PUBLIC, JolkArchetype.CLASS, Collections.emptyMap(), Collections.emptyMap());
        
        JolkObject instance = new JolkObject(child);

        assertTrue(child.isMetaInstance(instance), "Instance should match its class");
        assertTrue(parent.isMetaInstance(instance), "Instance should match its superclass");
        assertFalse(metaClass.isMetaInstance(instance), "Instance should not match unrelated class");
        assertFalse(metaClass.isMetaInstance("string"), "String should not be a meta instance");
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
        assertEquals(5, members.getArraySize());

        Set<String> names = new HashSet<>();
        for (long i = 0; i < members.getArraySize(); i++) {
            names.add((String) members.readArrayElement(i));
        }
        assertTrue(names.contains("new"));
        assertTrue(names.contains("name"));
        assertTrue(names.contains("superclass"));
        assertTrue(names.contains("isInstance"));
        assertTrue(names.contains("VERSION"));
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
        assertThrows(UnknownIdentifierException.class, () -> {
            metaClass.readMember("new");
        });
    }

    @Test
    void testReadMetaConstant() throws UnknownIdentifierException {
        Object value = metaClass.readMember("VERSION");
        assertEquals(1, value);
    }

    @Test
    void testIsMetaMemberReadable() {
        assertTrue(metaClass.isMemberReadable("VERSION"));
        assertFalse(metaClass.isMemberReadable("new"));
    }

    @Test
    void testLookupInstanceFieldAccessor() {
        // Verify the Virtual Field Strategy
        Map<String, Object> fields = Collections.singletonMap("myField", null);
        // Use the full constructor to inject fields
        JolkMetaClass fieldClass = new JolkMetaClass("FieldClass", null, JolkFinality.OPEN, JolkVisibility.PUBLIC, JolkArchetype.CLASS, Collections.emptyMap(), fields, Collections.emptyMap());
        
        assertTrue(fieldClass.hasInstanceMember("myField"), "Should report field as an instance member");
        Object accessor = fieldClass.lookupInstanceMember("myField");
        assertNotNull(accessor, "Should return a synthesized accessor");
        assertTrue(accessor instanceof JolkMetaClass.JolkSyntheticAccessor, "Accessor should be of the correct synthetic type");
    }
}
