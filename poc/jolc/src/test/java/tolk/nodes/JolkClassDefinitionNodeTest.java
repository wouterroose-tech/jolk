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

import java.lang.reflect.Field;
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

    /**
     * Verifies that methods and members are correctly transformed into executable
     * closures and attached to instances created from the meta-class.
     */
    @Test
    void testExecuteGenericWithMembers() throws UnsupportedMessageException, ArityException, UnknownIdentifierException, UnsupportedTypeException {
        String className = "MemberClass";

        JolkClassDefinitionNode node = new JolkClassDefinitionNode(
            className,
            JolkFinality.FINAL,
            JolkVisibility.PUBLIC,
            JolkArchetype.CLASS,
            Map.of("testMethod", new JolkMethodNode("testMethod", new JolkLiteralNode("success"), new String[0], false, 1)),
            new HashMap<>(),
            new HashMap<>()
        );

        Object result = node.executeGeneric(null);

        assertTrue(result instanceof JolkMetaClass);

        // Verify intrinsic 'new' is also present (handled by JolkMetaClass)
        assertTrue(InteropLibrary.getUncached().isMemberInvocable(result, "new"));

        // Verify that the instance created from the meta class has the member
        Object instance = InteropLibrary.getUncached().invokeMember(result, "new");

        InteropLibrary interop = InteropLibrary.getUncached();
        assertTrue(interop.isMemberInvocable(instance, "testMethod"), "Instance should have invocable method 'testMethod'");
        
        Object methodResult = interop.invokeMember(instance, "testMethod");
        assertEquals("success", methodResult, "Invoking the synthesized method should return the body's value.");
    }

    /**
     * Verifies that instance fields with initializers are evaluated during the 
     * class definition phase to create the template for new instances.
     */
    @Test
    void testInstanceFieldInitializers() throws UnsupportedMessageException, ArityException, UnknownIdentifierException, UnsupportedTypeException {
        Map<String, Object> instanceFields = new HashMap<>();
        // field x = 42
        instanceFields.put("x", new JolkFieldNode("x", new JolkLiteralNode(42L)));

        JolkClassDefinitionNode node = new JolkClassDefinitionNode(
            "Point", JolkFinality.OPEN, JolkVisibility.PUBLIC, JolkArchetype.CLASS,
            new HashMap<>(), instanceFields, new HashMap<>()
        );

        Object metaClass = node.executeGeneric(null);
        Object instance = InteropLibrary.getUncached().invokeMember(metaClass, "new");
        
        assertEquals(42L, InteropLibrary.getUncached().invokeMember(instance, "x"));
    }

    /**
     * Verifies that meta-methods (static methods) are correctly attached to the 
     * class object itself.
     */
    @Test
    void testMetaMethodDefinition() throws UnsupportedMessageException, ArityException, UnknownIdentifierException, UnsupportedTypeException {
        Map<String, Object> metaMembers = new HashMap<>();
        metaMembers.put("classInfo", new JolkMethodNode("classInfo", new JolkLiteralNode("v1"), new String[0], false));

        JolkClassDefinitionNode node = new JolkClassDefinitionNode(
            "MetaTest", JolkFinality.OPEN, JolkVisibility.PUBLIC, JolkArchetype.CLASS,
            new HashMap<>(), new HashMap<>(), metaMembers
        );

        Object metaClass = node.executeGeneric(null);
        Object result = InteropLibrary.getUncached().invokeMember(metaClass, "classInfo");
        
        assertEquals("v1", result, "Meta methods should be invocable on the class object.");
    }

    /**
     * Verifies that the archetype, visibility, and finality properties are 
     * correctly propagated to the [JolkMetaClass].
     */
    @Test
    void testArchetypeMetadata() {
        JolkClassDefinitionNode node = new JolkClassDefinitionNode(
            "MyRecord", JolkFinality.FINAL, JolkVisibility.PUBLIC, JolkArchetype.RECORD
        );

        JolkMetaClass result = (JolkMetaClass) node.executeGeneric(null);
        
        assertEquals(JolkArchetype.RECORD, getInternalField(result, "archetype"), "Archetype should be preserved.");
        assertEquals(JolkVisibility.PUBLIC, getInternalField(result, "visibility"), "Visibility should be preserved.");
        assertEquals(JolkFinality.FINAL, getInternalField(result, "finality"), "Finality should be preserved.");
    }

    /**
     * ### getInternalField
     *
     * Helper to access private fields in `JolkMetaClass` for testing purposes.
     * This bypasses the current lack of public getters in the runtime classes.
     *
     * @param instance The object instance to inspect.
     * @param fieldName The name of the field to retrieve.
     * @return The value of the field.
     */
    private Object getInternalField(Object instance, String fieldName) {
        try {
            Field field = instance.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(instance);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(
                String.format("Could not access field '%s' in %s", fieldName, instance.getClass().getSimpleName()), 
                e
            );
        }
    }

    ///
    /// Verifies that meta-members defined as FieldNodes (constants) are evaluated
    /// and their results stored immediately when the meta-class is defined.
    ///
    @Test
    void testMetaConstantEvaluation() throws UnsupportedMessageException, UnknownIdentifierException, ArityException, UnsupportedTypeException {
        Map<String, Object> metaMembers = new HashMap<>();
        metaMembers.put("VERSION", new JolkFieldNode("VERSION", new JolkLiteralNode(1L)));

        JolkClassDefinitionNode node = new JolkClassDefinitionNode(
            "Config", JolkFinality.FINAL, JolkVisibility.PUBLIC, JolkArchetype.CLASS,
            new HashMap<>(), new HashMap<>(), metaMembers
        );

        Object result = node.executeGeneric(null);
        @SuppressWarnings("unchecked")
        Map<String, Object> members = (Map<String, Object>) getInternalField(result, "metaMembers");
        assertEquals(1L, members.get("VERSION"), "Meta constants should be evaluated and stored during class definition.");
    }
}
