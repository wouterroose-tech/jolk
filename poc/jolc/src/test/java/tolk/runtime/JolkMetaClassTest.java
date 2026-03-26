package tolk.runtime;

import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.InvalidArrayIndexException;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;
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
    void testIsMetaInstancePrimitives() {
        // Mocking intrinsic types by name as per implementation logic
        JolkMetaClass longClass = new JolkMetaClass("Long", JolkFinality.OPEN, JolkVisibility.PUBLIC, JolkArchetype.CLASS, Collections.emptyMap());
        JolkMetaClass objClass = new JolkMetaClass("Object", JolkFinality.OPEN, JolkVisibility.PUBLIC, JolkArchetype.CLASS, Collections.emptyMap());
        JolkMetaClass otherClass = new JolkMetaClass("Other", JolkFinality.OPEN, JolkVisibility.PUBLIC, JolkArchetype.CLASS, Collections.emptyMap());

        assertTrue(longClass.isMetaInstance(42), "Long should be instance of Long");
        assertTrue(objClass.isMetaInstance(42), "Long should be instance of Object");
        assertFalse(otherClass.isMetaInstance(42), "Long should not be instance of Other");
    }

    @Test
    void testIsMetaInstanceNothing() {
        JolkMetaClass objClass = new JolkMetaClass("Object", JolkFinality.OPEN, JolkVisibility.PUBLIC, JolkArchetype.CLASS, Collections.emptyMap());
        // JolkNothing.INSTANCE is an instance of Nothing type and Object (by name)
        assertTrue(objClass.isMetaInstance(JolkNothing.INSTANCE), "Nothing should be instance of Object");
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
    void testInvokeSuperclass() throws Exception {
        JolkMetaClass parent = new JolkMetaClass("Parent", null, JolkFinality.OPEN, JolkVisibility.PUBLIC, JolkArchetype.CLASS, Collections.emptyMap(), Collections.emptyMap());
        JolkMetaClass child = new JolkMetaClass("Child", parent, JolkFinality.OPEN, JolkVisibility.PUBLIC, JolkArchetype.CLASS, Collections.emptyMap(), Collections.emptyMap());

        assertEquals(parent, child.invokeMember("superclass", new Object[]{}), "Should return parent meta class");
        assertEquals(JolkNothing.INSTANCE, parent.invokeMember("superclass", new Object[]{}), "Root class superclass should be Nothing");
    }

    @Test
    void testCanonicalNewWithFields() throws Exception {
        Map<String, Object> fields = Collections.singletonMap("val", null);
        // Constructor: name, superclass, finality, visibility, archetype, instanceMembers, instanceFields, metaMembers
        JolkMetaClass containerClass = new JolkMetaClass("Container", null, JolkFinality.OPEN, JolkVisibility.PUBLIC, JolkArchetype.CLASS, Collections.emptyMap(), fields, Collections.emptyMap());

        Object instanceObj = containerClass.invokeMember("new", new Object[]{"data"});
        assertTrue(instanceObj instanceof JolkObject, "Result should be a JolkObject");
        JolkObject instance = (JolkObject) instanceObj;

        // Verify field value via synthesized accessor
        Object accessorObj = containerClass.lookupInstanceMember("val");
        assertTrue(accessorObj instanceof JolkMetaClass.JolkSyntheticAccessor);
        
        Object result = InteropLibrary.getUncached().execute(accessorObj, new Object[]{instance});
        assertEquals("data", result, "Canonical constructor should initialize field");
    }

    @Test
    void testInvokeMetaMember() throws Exception {
        AtomicBoolean ran = new AtomicBoolean(false);
        JolkNothingTest.TestExecutable executable = new JolkNothingTest.TestExecutable(() -> ran.set(true));
        Map<String, Object> metaMembers = Collections.singletonMap("customMeta", executable);
        JolkMetaClass meta = new JolkMetaClass("MetaWithMethod", JolkFinality.OPEN, JolkVisibility.PUBLIC, JolkArchetype.CLASS, Collections.emptyMap(), metaMembers);
        
        meta.invokeMember("customMeta", new Object[]{});
        assertTrue(ran.get(), "Custom meta member should be invocable");
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

    @Test
    void testInstanceMemberLookupInheritance() {
        Object method = new Object(); // Dummy member
        Map<String, Object> parentMembers = Collections.singletonMap("inherited", method);
        JolkMetaClass parent = new JolkMetaClass("Parent", null, JolkFinality.OPEN, JolkVisibility.PUBLIC, JolkArchetype.CLASS, parentMembers, Collections.emptyMap());
        JolkMetaClass child = new JolkMetaClass("Child", parent, JolkFinality.OPEN, JolkVisibility.PUBLIC, JolkArchetype.CLASS, Collections.emptyMap(), Collections.emptyMap());
        
        assertEquals(method, child.lookupInstanceMember("inherited"), "Should find member in superclass");
        assertNull(child.lookupInstanceMember("nonexistent"), "Should return null for missing member");
    }

    @Test
    void testSynthesizedAccessorExecution() throws Exception {
        Map<String, Object> fields = Collections.singletonMap("score", null);
        JolkMetaClass meta = new JolkMetaClass("Score", null, JolkFinality.OPEN, JolkVisibility.PUBLIC, JolkArchetype.CLASS, Collections.emptyMap(), fields, Collections.emptyMap());
        
        Object instanceObj = meta.invokeMember("new", new Object[]{10});
        JolkObject instance = (JolkObject) instanceObj;
        
        Object accessor = meta.lookupInstanceMember("score");
        InteropLibrary interop = InteropLibrary.getUncached();
        
        // Getter
        assertEquals(10, interop.execute(accessor, instance));
        
        // Setter (Fluent: returns instance)
        Object setRes = interop.execute(accessor, instance, 20);
        assertEquals(instance, setRes);
        
        // Check new value
        assertEquals(20, interop.execute(accessor, instance));
    }
}
