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
    private final Map<String, Object> instanceMembers;
    private final Map<String, Object> instanceFields;
    private final Map<String, Object> metaMembers;
    private final JolkLanguage language;

    public JolkClassDefinitionNode(JolkLanguage language, String className, JolkFinality finality, JolkVisibility visibility, JolkArchetype archetype, Map<String, Object> instanceMembers, Map<String, Object> instanceFields, Map<String, Object> metaMembers) {
        this.language = language;
        this.className = className;
        this.finality = finality;
        this.visibility = visibility;
        this.archetype = archetype;
        this.instanceMembers = instanceMembers;
        this.instanceFields = instanceFields;
        this.metaMembers = metaMembers;
    }

    /**
     * ### JolkClassDefinitionNode
     * 
     * Convenience constructor for unit tests that provides member maps but omits the 
     * {@link JolkLanguage} context. The language will be resolved dynamically during execution.
     */
    public JolkClassDefinitionNode(String className, JolkFinality finality, JolkVisibility visibility, JolkArchetype archetype, Map<String, Object> instanceMembers, Map<String, Object> instanceFields, Map<String, Object> metaMembers) {
        this(null, className, finality, visibility, archetype, instanceMembers, instanceFields, metaMembers);
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

        // Jolk Structural Preamble: Populate keys and hints before MetaClass construction.
        // This ensures the MetaClass calculates the correct 'totalFieldCount' and arity 
        // for the canonical #new message, as well as accurate member visibility.
        for (String key : instanceMembers.keySet()) runtimeMembers.put(key, JolkNothing.INSTANCE);
        for (String key : metaMembers.keySet()) runtimeMetaMembers.put(key, JolkNothing.INSTANCE);
        
        for (Map.Entry<String, Object> entry : instanceFields.entrySet()) {
            Object val = entry.getValue();
            // If the value is a String, it's a type hint from the visitor (e.g. "Long").
            // Passing hints to the constructor allows JolkMetaClass to establish correct
            // default values (like 0L or false) in its internal state template.
            runtimeInstanceFields.put(entry.getKey(), (val instanceof String) ? val : JolkNothing.INSTANCE);
        }

        // Jolk Lifecycle Protocol: Instantiate and Register the Identity BEFORE 
        // populating members (Hydration). This allows methods created during hydration to 
        // resolve the class name via the registry.
        JolkMetaClass newMetaClass = new JolkMetaClass(className, null, finality, visibility, archetype, runtimeMembers, runtimeInstanceFields, runtimeMetaMembers);

        if (lang != null) {
            JolkContext context = lang.getContextReference().get(this);
            context.registerClass(newMetaClass);
        }

        for (Map.Entry<String, Object> entry : instanceMembers.entrySet()) {
            if (entry.getValue() instanceof JolkMethodNode method) {
                FrameDescriptor.Builder builder = FrameDescriptor.newBuilder();
                builder.addSlots(method.getFrameSlots(), FrameSlotKind.Object);
                
                JolkRootNode root = new JolkRootNode(lang, builder.build(), method.getBody(), method.getName(), true);
                runtimeMembers.put(entry.getKey(), new JolkClosure(root.getCallTarget()));
            } else if (entry.getValue() instanceof JolkMemberNode member) {
                JolkRootNode root = new JolkRootNode(lang, member.getBody(), member.getName());
                runtimeMembers.put(entry.getKey(), new JolkClosure(root.getCallTarget()));
            } else {
                runtimeMembers.put(entry.getKey(), entry.getValue());
            }
        }

        for (Map.Entry<String, Object> entry : instanceFields.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof JolkFieldNode fieldNode) {
                // Instance field initializers: evaluate the initializer to get the template value.
                // Note: For the PoC, we evaluate at class-definition time to ensure slots contain values.
                JolkRootNode root = new JolkRootNode(lang, fieldNode.getInitializer(), fieldNode.getName());
                runtimeInstanceFields.put(entry.getKey(), root.getCallTarget().call());
            } else {
                runtimeInstanceFields.put(entry.getKey(), value);
            }
        }

        for (Map.Entry<String, Object> entry : metaMembers.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof JolkMethodNode method) {
                FrameDescriptor.Builder builder = FrameDescriptor.newBuilder();
                builder.addSlots(method.getFrameSlots(), FrameSlotKind.Object);

                JolkRootNode root = new JolkRootNode(lang, builder.build(), method.getBody(), method.getName(), true);
                runtimeMetaMembers.put(entry.getKey(), new JolkClosure(root.getCallTarget()));
            } else if (value instanceof JolkFieldNode field) {
                // Constants: evaluate initializer immediately
                JolkRootNode root = new JolkRootNode(lang, field.getInitializer(), field.getName());
                runtimeMetaMembers.put(entry.getKey(), root.getCallTarget().call());
            } else if (value instanceof JolkMemberNode member) {
                JolkRootNode root = new JolkRootNode(lang, member.getBody(), member.getName());
                if (member.isState()) {
                    // It's a constant or static field: evaluate the initializer immediately
                    runtimeMetaMembers.put(entry.getKey(), root.getCallTarget().call());
                } else {
                    // It's a meta method: wrap in closure
                    runtimeMetaMembers.put(entry.getKey(), new JolkClosure(root.getCallTarget()));
                }
            } else {
                runtimeMetaMembers.put(entry.getKey(), entry.getValue());
            }
        }

        // Jolk Lifecycle: Synchronize structural templates after hydration.
        newMetaClass.initializeDefaultValues();

        return newMetaClass;
    }
}