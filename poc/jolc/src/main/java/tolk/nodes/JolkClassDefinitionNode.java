package tolk.nodes;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeInfo;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
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
    private final Map<String, List<JolkMethodNode>> instanceMethods;
    private final Map<String, JolkFieldNode> instanceFields;
    private final Map<String, List<JolkNode>> metaMembers;

    /**
     * ### JolkClassDefinitionNode
     *
     * Primary constructor for defining a Jolk type with its members.
     *
     * @param className The name of the class.
     * @param finality The finality (OPEN, FINAL, etc).
     * @param visibility The visibility (PUBLIC, PRIVATE, etc).
     * @param archetype The archetype (CLASS, RECORD, etc).
     * @param instanceMethods Map of instance methods.
     * @param instanceFields Map of instance fields.
     * @param metaMembers Map of meta-members (static methods/fields).
     */
    @SuppressWarnings("unchecked")
    public JolkClassDefinitionNode(String className, JolkFinality finality, JolkVisibility visibility, JolkArchetype archetype, Map<String, ?> instanceMethods, Map<String, ?> instanceFields, Map<String, ?> metaMembers) {
        this.className = className;
        this.finality = finality;
        this.visibility = visibility;
        this.archetype = archetype;
        this.instanceMethods = (Map<String, List<JolkMethodNode>>) (Object) instanceMethods;
        this.instanceFields = (Map<String, JolkFieldNode>) (Object) instanceFields;
        this.metaMembers = (Map<String, List<JolkNode>>) (Object) metaMembers;
    }

    /**
     * ### JolkClassDefinitionNode
     *
     * Convenience constructor for unit tests that do not yet pass the {@link JolkLanguage} 
     * context. The language will be resolved dynamically during execution.
     */
    public JolkClassDefinitionNode(String className, JolkFinality finality, JolkVisibility visibility, JolkArchetype archetype) {
        this(className, finality, visibility, archetype, Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap());
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        JolkLanguage lang = JolkLanguage.getLanguage(this);
        JolkContext context = lang.getContextReference().get(this);

        Map<String, Object> runtimeMembers = new LinkedHashMap<>();
        Map<String, Object> runtimeInstanceFields = new LinkedHashMap<>();
        Map<String, Object> runtimeMetaMembers = new LinkedHashMap<>();
        Map<String, Object> runtimeMetaFields = new LinkedHashMap<>();

        // Jolk Structural Preamble: Populate keys and hints before MetaClass construction.
        // This ensures the MetaClass calculates the correct 'totalFieldCount' and arity 
        // for the canonical #new message, as well as accurate member visibility.
        for (String key : instanceMethods.keySet()) runtimeMembers.put(key, JolkNothing.INSTANCE);

        for (Map.Entry<String, List<JolkNode>> entry : metaMembers.entrySet()) {
            for (JolkNode node : entry.getValue()) {
                if (node instanceof JolkMethodNode) {
                    runtimeMetaMembers.put(entry.getKey(), JolkNothing.INSTANCE);
                } else if (node instanceof JolkFieldNode) {
                    runtimeMetaFields.put(entry.getKey(), JolkNothing.INSTANCE);
                }
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
        
        context.registerClass(newMetaClass);

        for (Map.Entry<String, List<JolkMethodNode>> entry : instanceMethods.entrySet()) {
            List<JolkMethodNode> methods = entry.getValue();
            // Create a dispatcher if multiple arities exist for the same name
            runtimeMembers.put(entry.getKey(), createMethodClosure(lang, methods));
        }

        for (Map.Entry<String, JolkFieldNode> entry : instanceFields.entrySet()) {
            JolkFieldNode fieldNode = entry.getValue();
            if (!(fieldNode.getInitializer() instanceof JolkEmptyNode)) {
                // Instance field initializers: evaluate the initializer to get the template value.
                // Note: For the PoC, we evaluate at class-definition time to ensure slots contain values.
                JolkRootNode root = new JolkRootNode(lang, fieldNode.getInitializer(), fieldNode.getName());
                runtimeInstanceFields.put(entry.getKey(), lift(root.getCallTarget().call()));
            } else {
                // If no initializer, the type hint was already passed.
                runtimeInstanceFields.put(entry.getKey(), fieldNode.getTypeName());
            }
        }

        for (Map.Entry<String, List<JolkNode>> entry : metaMembers.entrySet()) {
            String name = entry.getKey();
            List<JolkNode> nodes = entry.getValue();
            List<JolkMethodNode> methods = new java.util.ArrayList<>();
            
            for (JolkNode node : nodes) {
                if (node instanceof JolkMethodNode m) methods.add(m);
                else if (node instanceof JolkFieldNode field) {
                    JolkRootNode root = new JolkRootNode(lang, field.getInitializer(), field.getName());
                    Object initialValue = lift(root.getCallTarget().call());
                    newMetaClass.setMetaFieldValue(name, initialValue);
                    // Note: In case of field/method collision, we store the field accessor separately
                    // so the dispatcher can include it.
                    runtimeMetaMembers.put(name, newMetaClass.getMetaAccessor(name, field.isStable()));
                }
            }
            
            if (!methods.isEmpty()) {
                runtimeMetaMembers.put(name, createMethodClosure(lang, methods));
            }
        }

        // Jolk Lifecycle: Synchronize structural templates after hydration.
        newMetaClass.initializeDefaultValues();

        return newMetaClass;
    }

    private JolkClosure createMethodClosure(JolkLanguage lang, List<JolkMethodNode> methods) {
        if (methods.size() == 1) {
            JolkMethodNode method = methods.get(0); 
            FrameDescriptor.Builder builder = FrameDescriptor.newBuilder();
            builder.addSlots(method.getFrameSlots(), FrameSlotKind.Object);
            JolkRootNode root = new JolkRootNode(lang, builder.build(), method.getBody(), method.getName(), true);
            return new JolkClosure(root.getCallTarget());
        }

        // Jolk Arity Dispatch: Support overloading by selecting the implementation 
        // that matches the number of arguments provided at runtime.
        int maxSlots = 0;
        for (JolkMethodNode m : methods) maxSlots = Math.max(maxSlots, m.getFrameSlots());
        
        FrameDescriptor.Builder builder = FrameDescriptor.newBuilder();
        builder.addSlots(maxSlots, FrameSlotKind.Object);
        
        JolkNode dispatcherBody = new JolkArityDispatchNode(methods);
        JolkRootNode root = new JolkRootNode(lang, builder.build(), dispatcherBody, methods.get(0).getName(), true);
        return new JolkClosure(root.getCallTarget());
    }

    private static final class JolkArityDispatchNode extends JolkNode {
        @Children private final JolkNode[] methodBodies;
        private final int[] arities;

        JolkArityDispatchNode(List<JolkMethodNode> methods) {
            this.methodBodies = new JolkNode[methods.size()];
            this.arities = new int[methods.size()];
            for (int i = 0; i < methods.size(); i++) {
                methodBodies[i] = methods.get(i).getBody();
                arities[i] = methods.get(i).getParameters().length;
            }
        }

        @Override
        public Object executeGeneric(VirtualFrame frame) {
            int callArity = frame.getArguments().length - 1; // Exclude 'self'
            for (int i = 0; i < arities.length; i++) {
                if (arities[i] == callArity) {
                    return lift(methodBodies[i].executeGeneric(frame));
                }
            }
            throw new RuntimeException("No method found for arity " + callArity);
        }
    }
}