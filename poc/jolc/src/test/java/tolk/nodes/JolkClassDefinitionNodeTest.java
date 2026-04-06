package tolk.nodes;

import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.Test;
import tolk.JolcTestBase;
import tolk.language.JolkLanguage;
import tolk.runtime.JolkArchetype;
import tolk.runtime.JolkFinality;
import tolk.runtime.JolkMetaClass;
import tolk.runtime.JolkVisibility;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class JolkClassDefinitionNodeTest extends JolcTestBase {

    private Object lastRawResult;

    @Test
    void testExecuteGenericReturnsMetaClass() {
        String className = "MyTestClass";
        JolkClassDefinitionNode node = new JolkClassDefinitionNode(
            className,
            JolkFinality.OPEN,
            JolkVisibility.PUBLIC,
            JolkArchetype.CLASS
        );

        Value result = execute(node);

        assertNotNull(result);
        assertTrue(unwrap(result, JolkMetaClass.class) instanceof JolkMetaClass, "Result must be an internal JolkMetaClass.");
        assertTrue(isMetaObject(result), "Value must be identified as a meta-object.");
        
        assertEquals(className, getMetaSimpleName(result));
        assertEquals(className, getMetaQualifiedName(result));
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
            null,
            JolkFinality.FINAL,
            JolkVisibility.PUBLIC,
            JolkArchetype.CLASS,
            Map.of("testMethod", List.of(new JolkMethodNode("testMethod", new JolkLiteralNode("success"), new String[0], false, 1))),
            new HashMap<String, JolkFieldNode>(),
            new HashMap<String, List<JolkNode>>()
        );

        Value result = execute(node);

        assertTrue(unwrap(result, JolkMetaClass.class) instanceof JolkMetaClass);

        // Verify intrinsic 'new' is also present (handled by JolkMetaClass)
        assertTrue(canInvokeMember(result, "new"), "Meta-class must support #new");

        // Verify that the instance created from the meta class has the member
        Value instance = invokeMember(result, "new");
        assertTrue(canInvokeMember(instance, "testMethod"), "Instance should have invocable method 'testMethod'");
        
        assertEquals("success", invokeMember(instance, "testMethod").asString(), "Invoking the synthesized method should return the body's value.");
    }

    /**
     * Verifies that instance fields with initializers are evaluated during the 
     * class definition phase to create the template for new instances.
     */
    @Test
    void testInstanceFieldInitializers() throws UnsupportedMessageException, ArityException, UnknownIdentifierException, UnsupportedTypeException {
        Map<String, JolkFieldNode> instanceFields = new HashMap<>();
        // field x = 42
        instanceFields.put("x", new JolkFieldNode("x", new JolkLiteralNode(42L)));

        JolkClassDefinitionNode node = new JolkClassDefinitionNode(
            "Point", 
            null, 
            JolkFinality.OPEN, JolkVisibility.PUBLIC, JolkArchetype.CLASS,
            new HashMap<String, List<JolkMethodNode>>(), instanceFields, new HashMap<String, List<JolkNode>>()
        );

        Value metaClass = execute(node);
        Value instance = invokeMember(metaClass, "new");
        
        assertEquals(42L, invokeMember(instance, "x").asLong());
    }

    /**
     * Verifies that meta-methods (static methods) are correctly attached to the 
     * class object itself.
     */
    @Test
    void testMetaMethodDefinition() throws UnsupportedMessageException, ArityException, UnknownIdentifierException, UnsupportedTypeException {
        Map<String, List<JolkNode>> metaMembers = new HashMap<>();
        metaMembers.put("classInfo", List.of(new JolkMethodNode("classInfo", new JolkLiteralNode("v1"), new String[0], false)));

        JolkClassDefinitionNode node = new JolkClassDefinitionNode(
            "MetaTest", 
            null, 
            JolkFinality.OPEN, JolkVisibility.PUBLIC, JolkArchetype.CLASS,
            new HashMap<String, List<JolkMethodNode>>(), new HashMap<String, JolkFieldNode>(), metaMembers
        );

        Value metaClass = execute(node);
        Value result = invokeMember(metaClass, "classInfo");
        assertEquals("v1", result.asString(), "Meta methods should be invocable on the class object.");
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

        Value result = execute(node);
        JolkMetaClass meta = unwrap(result, JolkMetaClass.class);
        
        assertEquals(JolkArchetype.RECORD, getInternalField(meta, "archetype"), "Archetype should be preserved.");
        assertEquals(JolkVisibility.PUBLIC, getInternalField(meta, "visibility"), "Visibility should be preserved.");
        assertEquals(JolkFinality.FINAL, getInternalField(meta, "finality"), "Finality should be preserved.");
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
        Map<String, List<JolkNode>> metaMembers = new HashMap<>();
        metaMembers.put("VERSION", List.of(new JolkFieldNode("VERSION", new JolkLiteralNode(1L))));

        JolkClassDefinitionNode node = new JolkClassDefinitionNode(
            "Config", 
            null, 
            JolkFinality.FINAL, JolkVisibility.PUBLIC, JolkArchetype.CLASS,
            new HashMap<String, List<JolkMethodNode>>(), new HashMap<String, JolkFieldNode>(), metaMembers
        );

        Value result = execute(node);
        assertEquals(1L, invokeMember(result, "VERSION").asLong(), "Meta constants should be evaluated and accessible via synthesized accessors.");
    }

    /**
     * Verifies that multiple methods with the same name but different arities
     * are correctly resolved by the synthesised [JolkArityDispatchNode].
     */
    @Test
    void testArityOverloading() throws UnsupportedMessageException, ArityException, UnknownIdentifierException, UnsupportedTypeException {
        List<JolkMethodNode> methods = new ArrayList<>();
        // val() -> returns 0
        methods.add(new JolkMethodNode("val", new JolkLiteralNode(0L), new String[0], false, 1));
        // val(x) -> returns x
        methods.add(new JolkMethodNode("val", new JolkReadArgumentNode(1, 0), new String[]{"x"}, false, 2));

        JolkClassDefinitionNode node = new JolkClassDefinitionNode(
            "OverloadTest", 
            null, 
            JolkFinality.OPEN, JolkVisibility.PUBLIC, JolkArchetype.CLASS,
            Map.of("val", methods), new HashMap<>(), new HashMap<>()
        );

        Value metaClass = execute(node);
        Value instance = invokeMember(metaClass, "new");
        
        assertEquals(0L, invokeMember(instance, "val").asLong(), "0-arg call should hit first overload.");
        assertEquals(42L, invokeMember(instance, "val", 42L).asLong(), "1-arg call should hit second overload.");
    }

    /**
     * Verifies that fields without initializers have their type names passed
     * as hints during the structural preamble.
     */
    @Test
    @SuppressWarnings("unchecked")
    void testFieldTypeHintsInPreamble() {
        Map<String, JolkFieldNode> fields = new HashMap<>();
        fields.put("count", new JolkFieldNode("count", "Long", new JolkEmptyNode()));
        fields.put("active", new JolkFieldNode("active", "Boolean", new JolkEmptyNode()));

        JolkClassDefinitionNode node = new JolkClassDefinitionNode(
            "HintTest", 
            null, 
            JolkFinality.OPEN, JolkVisibility.PUBLIC, JolkArchetype.CLASS,
            new HashMap<>(), fields, new HashMap<>()
        );

        Value result = execute(node);
        JolkMetaClass metaClass = unwrap(result, JolkMetaClass.class);
        
        // Access internal instanceFields map via reflection to verify hints
        Map<String, Object> hints = (Map<String, Object>) getInternalField(metaClass, "instanceFields");
        assertEquals("Long", hints.get("count"), "Preamble should contain the type name hint for uninitialized fields.");
        assertEquals("Boolean", hints.get("active"), "Preamble should contain the type name hint for uninitialized fields.");
    }

    private boolean isMetaObject(Value v) {
        context.enter();
        try { return v.isMetaObject(); } finally { context.leave(); }
    }

    private String getMetaSimpleName(Value v) {
        context.enter();
        try { return v.getMetaSimpleName(); } finally { context.leave(); }
    }

    private String getMetaQualifiedName(Value v) {
        context.enter();
        try { return v.getMetaQualifiedName(); } finally { context.leave(); }
    }

    private boolean canInvokeMember(Value v, String member) {
        context.enter();
        try { return v.canInvokeMember(member); } finally { context.leave(); }
    }

    private Value invokeMember(Value v, String member, Object... args) {
        context.enter();
        try { return v.invokeMember(member, args); } finally { context.leave(); }
    }

    /**
     * ### unwrap
     * 
     * Safely converts a Polyglot [Value] to a specific Java type while ensuring 
     * the Truffle context is properly entered. This prevents AssertionError 
     * in HostToGuestRootNode during internal type mapping.
     * 
     * @param v The polyglot value to convert.
     * @param clazz The target Java class.
     * @return The unwrapped Java instance.
     */
    private <T> T unwrap(Value v, Class<T> clazz) {
        // In unit tests, we want to access the internal guest object directly.
        // Since Value.as(Object.class) returns a proxy for guest objects, we 
        // prioritize the raw result captured during the last 'execute' call.
        if (lastRawResult != null && clazz.isInstance(lastRawResult)) {
            return clazz.cast(lastRawResult);
        }

        context.enter();
        try {
            // For internal runtime objects like JolkMetaClass, v.as(clazz) can trigger 
            // specialized Interop checks that fail with an AssertionError in 
            // HostToGuestRootNode. Retrieving it as a raw Object first bypasses this.
            Object raw = v.as(Object.class);
            if (clazz.isInstance(raw)) return clazz.cast(raw);
            return v.as(clazz);
        } finally {
            context.leave();
        }
    }

    /**
     * ### execute
     * 
     * Helper method to execute a [JolkNode] within a valid Truffle execution 
     * context. It ensures the language is initialized and the node is 
     * properly adopted by a [JolkRootNode].
     * 
     * @param node The node to execute.
     * @return The result of the evaluation.
     */
    private Value execute(JolkNode node) {
        eval(""); // Initialize context
        context.enter();
        try {
            JolkLanguage lang = com.oracle.truffle.api.TruffleLanguage.LanguageReference.create(JolkLanguage.class).get(null);
            JolkRootNode root = new JolkRootNode(lang, node);
            this.lastRawResult = root.getCallTarget().call();
            return context.asValue(lastRawResult);
        } finally {
            context.leave();
        }
    }
}
