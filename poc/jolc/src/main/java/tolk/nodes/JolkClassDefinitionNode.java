package tolk.nodes;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeInfo;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import tolk.language.JolkLanguage;
import tolk.language.JolkContext;
import tolk.runtime.JolkMetaClass;
import tolk.runtime.JolkNothing;
import tolk.runtime.JolkFinality;
import tolk.runtime.JolkVisibility;
import tolk.runtime.JolkArchetype;
import tolk.runtime.JolkClosure;

/// An AST node that represents the definition of a Jolk type.
///
/// This node captures the core structural properties of a type—such as its name,
/// archetype, visibility, and finality—during parsing. When executed at runtime, it
/// acts as a factory, instantiating a [JolkMetaClass] meta-object with these
/// properties. This meta-object then becomes the first-class representation of the
/// type within the Jolk interpreter.
/// 
@NodeInfo(language = "Jolk", description = "The node for defining a Jolk class.")
public class JolkClassDefinitionNode extends JolkExpressionNode {

    private final String className;
    private final JolkFinality finality;
    private final JolkVisibility visibility;
    private final JolkArchetype archetype;
    private final Map<String, JolkMethodNode> instanceMethods;
    private final Map<String, JolkFieldNode> instanceFields;
    private final Map<String, JolkNode> metaMembers;
    private final JolkLanguage language;

    public JolkClassDefinitionNode(JolkLanguage language, String className, JolkFinality finality, JolkVisibility visibility, JolkArchetype archetype, Map<String, JolkMethodNode> instanceMethods, Map<String, JolkFieldNode> instanceFields, Map<String, JolkNode> metaMembers) {
        this.language = language;
        this.className = className;
        this.finality = finality;
        this.visibility = visibility;
        this.archetype = archetype;
        this.instanceMethods = instanceMethods;
        this.instanceFields = instanceFields;
        this.metaMembers = metaMembers;
    }

    /**
     * ### JolkClassDefinitionNode
     * 
     * Convenience constructor for unit tests that provides member maps but omits the 
     * {@link JolkLanguage} context. The language will be resolved dynamically during execution.
     */
    @SuppressWarnings("unchecked")
    public JolkClassDefinitionNode(String className, JolkFinality finality, JolkVisibility visibility, JolkArchetype archetype, Map<String, ?> instanceMethods, Map<String, ?> instanceFields, Map<String, ?> metaMembers) {
        this(null, className, finality, visibility, archetype, (Map<String, JolkMethodNode>) instanceMethods, (Map<String, JolkFieldNode>) instanceFields, (Map<String, JolkNode>) metaMembers);
    }

    public JolkClassDefinitionNode(JolkLanguage language, String className, JolkFinality finality, JolkVisibility visibility, JolkArchetype archetype) {
        this(language, className, finality, visibility, archetype, Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap());
    }

    /**
     * ### JolkClassDefinitionNode
     * 
     * Convenience constructor for unit tests that do not yet pass the {@link JolkLanguage} 
     * context. The language will be resolved dynamically during execution.
     */
    public JolkClassDefinitionNode(String className, JolkFinality finality, JolkVisibility visibility, JolkArchetype archetype) {
        this(null, className, finality, visibility, archetype, Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap());
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        JolkLanguage lang = this.language;
        if (lang == null && getRootNode() != null) {
            lang = getRootNode().getLanguage(JolkLanguage.class);
        }

        Map<String, Object> runtimeMembers = new LinkedHashMap<>();
        Map<String, Object> runtimeInstanceFields = new LinkedHashMap<>();
        Map<String, Object> runtimeMetaMembers = new LinkedHashMap<>();
        Map<String, Object> runtimeMetaFields = new LinkedHashMap<>();

        // Jolk Structural Preamble: Populate keys and hints before MetaClass construction.
        // This ensures the MetaClass calculates the correct 'totalFieldCount' and arity 
        // for the canonical #new message, as well as accurate member visibility.
        for (String key : instanceMethods.keySet()) runtimeMembers.put(key, JolkNothing.INSTANCE);

        for (Map.Entry<String, JolkNode> entry : metaMembers.entrySet()) {
            if (entry.getValue() instanceof JolkMethodNode) {
                runtimeMetaMembers.put(entry.getKey(), JolkNothing.INSTANCE);
            } else if (entry.getValue() instanceof JolkFieldNode) {
                runtimeMetaFields.put(entry.getKey(), JolkNothing.INSTANCE);
            }
        }

        for (Map.Entry<String, JolkFieldNode> entry : instanceFields.entrySet()) {
            JolkFieldNode fieldNode = entry.getValue();
            // If the field has no initializer, pass its type name as a hint.
            // Passing hints to the constructor allows JolkMetaClass to establish correct
            // default values (like 0L or false) in its internal state template.
            runtimeInstanceFields.put(entry.getKey(), (fieldNode.getInitializer() instanceof JolkEmptyNode) ? fieldNode.getTypeName() : JolkNothing.INSTANCE);
        }

        // Jolk Lifecycle Protocol: Instantiate and Register the Identity BEFORE 
        // populating members (Hydration). This allows methods created during hydration to 
        // resolve the class name via the registry.
        JolkMetaClass newMetaClass = new JolkMetaClass(className, null, finality, visibility, archetype, runtimeMembers, runtimeInstanceFields, runtimeMetaMembers, runtimeMetaFields);

        if (lang != null) {
            JolkContext context = lang.getContextReference().get(this);
            context.registerClass(newMetaClass);
        }

        for (Map.Entry<String, JolkMethodNode> entry : instanceMethods.entrySet()) {
            if (entry.getValue() instanceof JolkMethodNode method) {
                FrameDescriptor.Builder builder = FrameDescriptor.newBuilder();
                builder.addSlots(method.getFrameSlots(), FrameSlotKind.Object);
                
                JolkRootNode root = new JolkRootNode(lang, builder.build(), method.getBody(), method.getName(), true);
                runtimeMembers.put(entry.getKey(), new JolkClosure(root.getCallTarget()));
            }
        }

        for (Map.Entry<String, JolkFieldNode> entry : instanceFields.entrySet()) {
            JolkFieldNode fieldNode = entry.getValue();
            if (!(fieldNode.getInitializer() instanceof JolkEmptyNode)) {
                // Instance field initializers: evaluate the initializer to get the template value.
                // Note: For the PoC, we evaluate at class-definition time to ensure slots contain values.
                JolkRootNode root = new JolkRootNode(lang, fieldNode.getInitializer(), fieldNode.getName());
                runtimeInstanceFields.put(entry.getKey(), root.getCallTarget().call());
            } else {
                // If no initializer, the type hint was already passed.
                runtimeInstanceFields.put(entry.getKey(), fieldNode.getTypeName());
            }
        }

        for (Map.Entry<String, JolkNode> entry : metaMembers.entrySet()) {
            String name = entry.getKey();
            JolkNode value = entry.getValue();
            if (value instanceof JolkMethodNode method) {
                FrameDescriptor.Builder builder = FrameDescriptor.newBuilder();
                builder.addSlots(method.getFrameSlots(), FrameSlotKind.Object);

                JolkRootNode root = new JolkRootNode(lang, builder.build(), method.getBody(), method.getName(), true);
                runtimeMetaMembers.put(entry.getKey(), new JolkClosure(root.getCallTarget()));
            } else if (value instanceof JolkFieldNode field) {
                // Meta fields/constants: evaluate initializer immediately and synthesize accessor
                JolkRootNode root = new JolkRootNode(lang, field.getInitializer(), field.getName());
                Object initialValue = root.getCallTarget().call();
                newMetaClass.setMetaFieldValue(name, initialValue);
                runtimeMetaMembers.put(name, newMetaClass.getMetaAccessor(name, field.isStable()));
            }
        }

        // Jolk Lifecycle: Synchronize structural templates after hydration.
        newMetaClass.initializeDefaultValues();

        return newMetaClass;
    }
}