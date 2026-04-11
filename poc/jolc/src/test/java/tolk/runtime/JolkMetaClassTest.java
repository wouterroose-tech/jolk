package tolk.runtime;

import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
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
    void testIsMetaInstanceHierarchy() throws UnsupportedMessageException {
        JolkMetaClass parent = new JolkMetaClass("Parent", null, JolkFinality.OPEN, JolkVisibility.PUBLIC, JolkArchetype.CLASS, Collections.emptyMap(), Collections.emptyMap());
        JolkMetaClass child = new JolkMetaClass("Child", parent, JolkFinality.OPEN, JolkVisibility.PUBLIC, JolkArchetype.CLASS, Collections.emptyMap(), Collections.emptyMap());
        
        JolkObject instance = new JolkObject(child);

        assertTrue(child.isMetaInstance(instance), "Instance should match its class");
        assertTrue(parent.isMetaInstance(instance), "Instance should match its superclass");
        assertFalse(metaClass.isMetaInstance(instance), "Instance should not match unrelated class");
        assertFalse(metaClass.isMetaInstance("string"), "String should not be a meta instance");
    }

    @Test
    void testIsMetaInstancePrimitives() throws UnsupportedMessageException {
        // Mocking intrinsic types by name as per implementation logic
        JolkMetaClass longClass = new JolkMetaClass("Long", JolkFinality.OPEN, JolkVisibility.PUBLIC, JolkArchetype.CLASS, Collections.emptyMap());
        JolkMetaClass boolClass = new JolkMetaClass("Boolean", JolkFinality.OPEN, JolkVisibility.PUBLIC, JolkArchetype.CLASS, Collections.emptyMap());
        JolkMetaClass objClass = new JolkMetaClass("Object", JolkFinality.OPEN, JolkVisibility.PUBLIC, JolkArchetype.CLASS, Collections.emptyMap());
        JolkMetaClass otherClass = new JolkMetaClass("Other", JolkFinality.OPEN, JolkVisibility.PUBLIC, JolkArchetype.CLASS, Collections.emptyMap());

        assertTrue(longClass.isMetaInstance(42), "Long should be instance of Long");
        assertTrue(boolClass.isMetaInstance(true), "Boolean should be instance of Boolean");
        assertTrue(objClass.isMetaInstance(42), "Long should be instance of Object");
        assertTrue(objClass.isMetaInstance(true), "Boolean should be instance of Object");
        assertFalse(otherClass.isMetaInstance(42), "Long should not be instance of Other");
        assertFalse(otherClass.isMetaInstance(true), "Boolean should not be instance of Other");
    }

    @Test
    void testIsMetaInstanceString() throws UnsupportedMessageException {
        JolkMetaClass stringClass = new JolkMetaClass("String", JolkFinality.FINAL, JolkVisibility.PUBLIC, JolkArchetype.CLASS, Collections.emptyMap());
        assertTrue(stringClass.isMetaInstance("hello"), "Java String should be instance of Jolk String archetype");
        assertFalse(stringClass.isMetaInstance(123L), "Long should not be instance of String");
    }

    @Test
    void testIsMetaInstanceNothing() throws UnsupportedMessageException {
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
        assertEquals(24, members.getArraySize());

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
        DynamicObjectLibrary objLib = DynamicObjectLibrary.getUncached();
        assertTrue(metaClass.isMemberInvocable("new", objLib));
        assertFalse(metaClass.isMemberInvocable("randomMethod", objLib));
    }

    @Test
    void testInvokeNew() throws UnknownIdentifierException, ArityException, UnsupportedTypeException, UnsupportedMessageException {
        Object result = metaClass.callMetaMember("new", new Object[]{}); // This uses the correct callMetaMember
        assertNotNull(result, "The result of #new should not be null");
        assertTrue(result instanceof JolkObject, "The result should be an instance of JolkObject");
    }

    @Test
    void testInvokeSuperclass() throws Exception {
        JolkMetaClass parent = new JolkMetaClass("Parent", null, JolkFinality.OPEN, JolkVisibility.PUBLIC, JolkArchetype.CLASS, Collections.emptyMap(), Collections.emptyMap());
        JolkMetaClass child = new JolkMetaClass("Child", parent, JolkFinality.OPEN, JolkVisibility.PUBLIC, JolkArchetype.CLASS, Collections.emptyMap(), Collections.emptyMap());

        assertEquals(parent, child.callMetaMember("superclass", new Object[]{}), "Should return parent meta class");
        assertEquals(JolkNothing.INSTANCE, parent.callMetaMember("superclass", new Object[]{}), "Root class superclass should be Nothing");
    }

    @Test
    void testInvokeIntrinsicArity() {
        // #name takes 0 args
        assertThrows(ArityException.class, () -> metaClass.callMetaMember("name", new Object[]{"extra"}));
        
        // #superclass takes 0 args
        assertThrows(ArityException.class, () -> metaClass.callMetaMember("superclass", new Object[]{"extra"}));
        
        // #isInstance takes 1 arg
        assertThrows(ArityException.class, () -> metaClass.callMetaMember("isInstance", new Object[]{}));
    }

    @Test
    void testCanonicalNewWithFields() throws Exception {
        Map<String, Object> fields = Collections.singletonMap("val", null);
        // Constructor: name, superclass, finality, visibility, archetype, instanceMembers, instanceFields, metaMembers
        JolkMetaClass containerClass = new JolkMetaClass("Container", null, JolkFinality.OPEN, JolkVisibility.PUBLIC, JolkArchetype.CLASS, Collections.emptyMap(), fields, Collections.emptyMap());

        Object instanceObj = containerClass.callMetaMember("new", new Object[]{"data"});
        assertTrue(instanceObj instanceof JolkObject, "Result should be a JolkObject");
        JolkObject instance = (JolkObject) instanceObj;

        // Verify field value via interop message send (executes doShapeRead in dispatch)
        Object result = InteropLibrary.getUncached().invokeMember(instance, "val");
        assertEquals("data", result, "Canonical constructor should initialize field");
    }

    @Test
    void testDefaultValueInitialization() {
        Map<String, Object> fields = new java.util.LinkedHashMap<>();
        fields.put("id", null);             // Should default to 0L
        fields.put("active", "Boolean");    // Hint: Boolean -> false
        fields.put("name", "String");       // Hint: String -> ""
        fields.put("other", "OtherClass");  // Unknown -> Nothing

        JolkMetaClass meta = new JolkMetaClass("HintTest", null, JolkFinality.OPEN, JolkVisibility.PUBLIC, JolkArchetype.CLASS, Collections.emptyMap(), fields, Collections.emptyMap());
        Object[] defaults = meta.getDefaultFieldValues();

        assertEquals(0L, defaults[0], "id should default to 0L");
        assertEquals(false, defaults[1], "Boolean hint should default to false");
        assertEquals("", defaults[2], "String hint should default to empty string");
        assertEquals(JolkNothing.INSTANCE, defaults[3], "Unrecognized hint should default to Nothing");
    }

    @Test
    void testMetaFieldStorage() throws Exception {
        Map<String, Object> metaFields = Collections.singletonMap("count", "Long");
        // constructor handles registering accessors in metaMembers
        JolkMetaClass meta = new JolkMetaClass("StaticTest", null, JolkFinality.OPEN, JolkVisibility.PUBLIC, JolkArchetype.CLASS, 
                                              Collections.emptyMap(), Collections.emptyMap(), new java.util.HashMap<>(), metaFields);
        
        InteropLibrary interop = InteropLibrary.getUncached();
        
        // Initial default (from hint "Long")
        assertEquals(0L, interop.invokeMember(meta, "count"), "Meta field should initialize from hint");

        // Update via synthesized accessor
        // Jolk Protocol: meta accessors return self on write
        Object result = interop.invokeMember(meta, "count", 42L);
        assertEquals(meta, result);
        
        assertEquals(42L, interop.invokeMember(meta, "count"), "Meta field should store updated value");
    }

    @Test
    void testInvokeMetaMember() throws Exception {
        AtomicBoolean ran = new AtomicBoolean(false);
        JolkNothingTest.TestExecutable executable = new JolkNothingTest.TestExecutable(() -> ran.set(true));
        Map<String, Object> metaMembers = Collections.singletonMap("customMeta", executable);
        JolkMetaClass meta = new JolkMetaClass("MetaWithMethod", JolkFinality.OPEN, JolkVisibility.PUBLIC, JolkArchetype.CLASS, Collections.emptyMap(), metaMembers);
        
        meta.callMetaMember("customMeta", new Object[]{});
        assertTrue(ran.get(), "Custom meta member should be invocable");
    }

    @Test
    void testInvokeNewWithArgumentsThrowsArityException() {
        assertThrows(ArityException.class, () -> {
            metaClass.callMetaMember("new", new Object[]{"unexpectedArg"});
        });
    }

    @Test
    void testInvokeUnknownMemberThrowsException() {
        assertThrows(UnknownIdentifierException.class, () -> {
            metaClass.callMetaMember("unknown", new Object[]{});
        });
    }

       @Test
    void testReadMemberThrowsException() {
        DynamicObjectLibrary objLib = DynamicObjectLibrary.getUncached();
        // 'new' is invocable, not readable as a direct member.
        assertThrows(UnknownIdentifierException.class, () -> metaClass.readMember("new", objLib));
    }
    
    @Test
    void testReadMetaConstant() throws UnknownIdentifierException {
        DynamicObjectLibrary objLib = DynamicObjectLibrary.getUncached();
        // VERSION is a meta-field
        Object value = metaClass.readMember("VERSION", objLib);
        assertEquals(1, value);
    }

    @Test
    void testIsMetaMemberReadable() {
        DynamicObjectLibrary objLib = DynamicObjectLibrary.getUncached();
        assertTrue(metaClass.isMemberReadable("VERSION", objLib));
        assertFalse(metaClass.isMemberReadable("new", objLib));
    }

    @Test
    void testLookupInstanceFieldAccessor() {
        // In the Shape-based model, fields are not stored as accessor objects in the registry.
        Map<String, Object> fields = Collections.singletonMap("myField", null);
        // Use the full constructor to inject fields
        JolkMetaClass fieldClass = new JolkMetaClass("FieldClass", null, JolkFinality.OPEN, JolkVisibility.PUBLIC, JolkArchetype.CLASS, Collections.emptyMap(), fields, Collections.emptyMap());
        
        assertNull(fieldClass.lookupInstanceMember("myField"), "Fields are no longer represented by accessor objects in the registry.");
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
        
        Object instanceObj = meta.callMetaMember("new", new Object[]{10});
        JolkObject instance = (JolkObject) instanceObj;
        
        InteropLibrary interop = InteropLibrary.getUncached();
        
        // Getter Pattern: #score
        assertEquals(10, interop.invokeMember(instance, "score"));
        
        // Setter Pattern: #score(20) -> returns instance (Self-Return Contract)
        Object setRes = interop.invokeMember(instance, "score", 20);
        assertEquals(instance, setRes);
        
        // Check new value
        assertEquals(20, interop.invokeMember(instance, "score"));
    }
}
