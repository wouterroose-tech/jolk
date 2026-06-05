package tolk.nodes;

import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeInfo;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

import tolk.language.JolkLanguage;
import tolk.language.JolkContext;
import tolk.runtime.JolkMetaClass;
import tolk.runtime.JolkNothing;
import tolk.runtime.JolkFinality;
import tolk.runtime.JolkLazyValue;
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
    private final String superclassName;
    private final JolkFinality finality;
    private final JolkVisibility visibility;
    private final JolkArchetype archetype;
    private final Map<String, List<JolkMethodNode>> instanceMethods;
    private final Map<String, JolkFieldNode> instanceFields;
    private final Map<String, List<JolkNode>> metaMembers;
    private final List<String> enumConstants;

    /// Stores visibility restrictions for synthesised field getters 
    /// established via signature-only method declarations.
    private final Map<String, JolkVisibility> getterOverrides;

    /// Stores visibility restrictions for synthesised field setters 
    /// established via signature-only method declarations.
    private final Map<String, JolkVisibility> setterOverrides;

    /**
     * ### JolkClassDefinitionNode
     *
     * Primary constructor for defining a Jolk type with its members.
     *
     * @param className The name of the class.
     * @param superclassName The name of the superclass (may be null).
     * @param finality The finality (OPEN, FINAL, etc).
     * @param visibility The visibility (PUBLIC, PRIVATE, etc).
     * @param archetype The archetype (CLASS, RECORD, etc).
     * @param instanceMethods Map of instance methods.
     * @param instanceFields Map of instance fields.
     * @param metaMembers Map of meta-members (static methods/fields).
     * @param enumConstants List of enum constant names (for ENUM archetype).
     * @param getterOverrides Visibility overrides for synthesised getters.
     * @param setterOverrides Visibility overrides for synthesised setters.
     */
    public JolkClassDefinitionNode(String className, 
                                   String superclassName, 
                                   JolkFinality finality, 
                                   JolkVisibility visibility, 
                                   JolkArchetype archetype, 
                                   Map<String, List<JolkMethodNode>> instanceMethods, 
                                   Map<String, JolkFieldNode> instanceFields, 
                                   Map<String, List<JolkNode>> metaMembers, 
                                   List<String> enumConstants,
                                   Map<String, JolkVisibility> getterOverrides,
                                   Map<String, JolkVisibility> setterOverrides) {
        this.className = className;
        this.superclassName = superclassName;
        this.finality = finality;
        this.visibility = visibility;
        this.archetype = archetype;
        this.instanceMethods = instanceMethods;
        this.instanceFields = instanceFields;
        this.metaMembers = metaMembers;
        this.enumConstants = enumConstants != null ? enumConstants : new ArrayList<>();
        this.getterOverrides = getterOverrides != null ? getterOverrides : Collections.emptyMap();
        this.setterOverrides = setterOverrides != null ? setterOverrides : Collections.emptyMap();
    }

    /**
     * ### JolkClassDefinitionNode
     *
     * Convenience constructor for unit tests that do not yet pass the {@link JolkLanguage} 
     * context. The language will be resolved dynamically during execution.
     */
    public JolkClassDefinitionNode(String className, JolkFinality finality, JolkVisibility visibility, JolkArchetype archetype) {
        this(className, null, finality, visibility, archetype, Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(), Collections.emptyList(), null, null);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        JolkLanguage lang = JolkLanguage.getLanguage(this);
        JolkContext context = lang.getContextReference().get(this);

        Map<String, Object> runtimeMembers = new LinkedHashMap<>();
        Map<String, Object> runtimeInstanceFields = new LinkedHashMap<>();
        Map<String, Object> runtimeMetaMembers = new LinkedHashMap<>();
        Map<String, Object> runtimeMetaFields = new LinkedHashMap<>();
        Set<String> stableFields = new HashSet<>();

        JolkMetaClass superMetaClass = null;
        if (superclassName != null) { // Robust Superclass Resolution
            String currentPackage = className.contains(".") ? className.substring(0, className.lastIndexOf('.')) : "";
            Object resolved = resolveClassOrProjection(superclassName, currentPackage, context);
            
            if (resolved instanceof JolkMetaClass jmc) {
                // Identity Linkage: The guest meta-object is already hydrated.
                superMetaClass = jmc;
            } else if (resolved instanceof String fqn) {
                // Path Linkage: Use the resolved FQN string to prevent short-name collision.
                superMetaClass = context.getOrCreateClass(fqn);
            } else {
                superMetaClass = context.getOrCreateClass(superclassName);
            }
            
            // Validation Guard: If getOrCreateClass returns null, it means the name
            // resolved to a HostObject (Java Class). We allow this to proceed, 
            // but only throw if the name is truly undefined in both registries.
            if (superMetaClass == null && context.getDefinedClass(superclassName) == null) {
                throw new RuntimeException("Superclass not found or cannot be extended: " + superclassName);
            }
        }

        // Jolk Structural Preamble: Populate keys and hints before MetaClass construction.
        // This ensures the MetaClass calculates the correct 'totalFieldCount' and arity 
        // for the canonical #new message, as well as accurate member visibility.
        for (String key : instanceMethods.keySet()) runtimeMembers.put(key, JolkNothing.INSTANCE);

        // Jolk Structural Preamble: Populate instance fields and hints.
        for (Map.Entry<String, JolkFieldNode> entry : instanceFields.entrySet()) {
            JolkFieldNode fieldNode = entry.getValue();
            String fieldName = entry.getKey();
            // Literal Folding: If the initializer is a literal, evaluate it early to stabilize the defaults.
            Object hint = (fieldNode.getInitializer() instanceof JolkLiteralNode literal) 
                ? lift(literal.executeGeneric(null)) 
                : resolveTypeHint(fieldNode.getTypeName(), context);
            runtimeInstanceFields.put(fieldName, hint);
            if (fieldNode.isStable()) {
                stableFields.add(fieldName);
            }
        }

        // Jolk Structural Preamble: Populate meta-members to establish the handshake surface.
        for (Map.Entry<String, List<JolkNode>> entry : metaMembers.entrySet()) {
            for (JolkNode node : entry.getValue()) {
                if (node instanceof JolkMethodNode) {
                    runtimeMetaMembers.put(entry.getKey(), JolkNothing.INSTANCE);
                } else if (node instanceof JolkFieldNode metaField) {
                    Object hint;
                    if (metaField.isLazy()) {
                        hint = new JolkLazyValue(metaField.getInitializer(), metaField.getName(), lang);
                    } else if (metaField.getInitializer() instanceof JolkLiteralNode literal) {
                        // Literal Folding for constants
                        hint = lift(literal.executeGeneric(null));
                    } else if (!(metaField.getInitializer() instanceof JolkEmptyNode)) {
                        hint = JolkNothing.INSTANCE; // Eager complex initializer, evaluated later
                    } else {
                        hint = resolveTypeHint(metaField.getTypeName(), context);
                    }
                    runtimeMetaFields.put(entry.getKey(), hint);
                if (metaField.isStable()) {
                    stableFields.add(entry.getKey());
                }
                }
            }
        }

        // Jolk Lifecycle Protocol: Instantiate the Identity now that the field map is stable.
        // We pass null for the hostClass substrate identity, as this is a guest-defined type.
        // Enum constants are registered post-instantiation to support circular identity resolution 
        // within the meta-registry.
        JolkMetaClass newMetaClass = new JolkMetaClass(className, superMetaClass, finality, visibility, archetype, runtimeMembers, runtimeInstanceFields, runtimeMetaMembers, runtimeMetaFields, stableFields, null, getterOverrides, setterOverrides);

        // Jolk Lifecycle Protocol: Register ALL methods (instance and meta) before evaluating 
        // any field initializers. This ensures that circular references or self-instantiation 
        // (#new) during bootstrapping can resolve the behavioral protocol correctly.
        for (Map.Entry<String, List<JolkMethodNode>> entry : instanceMethods.entrySet()) {
            JolkClosure closure = createMethodClosure(lang, entry.getValue());
            for (JolkMethodNode m : entry.getValue()) bindSuperNodes(m.getBody(), newMetaClass);
            newMetaClass.registerInstanceMethod(entry.getKey(), closure);
        }

        for (Map.Entry<String, List<JolkNode>> entry : metaMembers.entrySet()) {
            List<JolkMethodNode> methods = new ArrayList<>();
            for (JolkNode node : entry.getValue()) if (node instanceof JolkMethodNode m) methods.add(m);
            if (!methods.isEmpty()) {
                JolkClosure closure = createMethodClosure(lang, methods);
                for (JolkMethodNode m : methods) bindSuperNodes(m.getBody(), newMetaClass);
                newMetaClass.registerMetaMethod(entry.getKey(), closure);
            }
        }

        // Jolk Lifecycle Protocol: Publish the identity to the context so it can be resolved 
        // by the JolkRootNodes evaluating the initializers below.
        context.registerClass(newMetaClass);

        // Jolk Evaluation Protocol: Now evaluate initializers.
        for (Map.Entry<String, JolkFieldNode> entry : instanceFields.entrySet()) {
            JolkFieldNode fieldNode = entry.getValue();
            if (!(fieldNode.getInitializer() instanceof JolkEmptyNode)) {
                JolkRootNode root = new JolkRootNode(lang, fieldNode.getInitializer(), fieldNode.getName());
                // Evaluation Protocol: We unwrap the evaluated result to ensure the MetaClass 
                // defaults array stores substrate identities (Long, ArrayList) rather than 
                // guest proxies, preventing Identity Inversion during instantiation.
                // We use the explicit context to perform the identity preparation to 
                // stabilize the metaboundary during the bootstrapping phase.
                Object evaluatedValue = unwrap(context.env.asGuestValue(lift(root.getCallTarget().call())));
                String fieldName = entry.getKey();
                runtimeInstanceFields.put(fieldName, evaluatedValue);
            }
        }

        // Register enum constants for ENUM archetype
        if (archetype == JolkArchetype.ENUM) {
            for (String constantName : enumConstants) {
                newMetaClass.registerEnumConstant(constantName);
            }
        }

        for (Map.Entry<String, List<JolkNode>> entry : metaMembers.entrySet()) {
            String name = entry.getKey();
            List<JolkNode> nodes = entry.getValue();
            
            for (JolkNode node : nodes) {
                if (node instanceof JolkFieldNode field) {
                    if (field.isLazy()) {
                        JolkLazyValue lazyVal = new JolkLazyValue(field.getInitializer(), name, lang);
                        newMetaClass.setMetaFieldValue(name, lazyVal);
                        runtimeMetaFields.put(name, lazyVal);
                    } else if (!(field.getInitializer() instanceof JolkEmptyNode)) {
                        JolkRootNode root = new JolkRootNode(lang, field.getInitializer(), field.getName());
                        Object initialValue = unwrap(context.env.asGuestValue(lift(root.getCallTarget().call())));
                        // Update both the storage slot and the map hint for initializeDefaultValues
                        newMetaClass.setMetaFieldValue(name, initialValue);
                        runtimeMetaFields.put(name, initialValue);
                    } else {
                        // Identity initialization for meta-fields without initializers
                        Object defaultValue = resolveTypeHint(field.getTypeName(), context);
                        newMetaClass.setMetaFieldValue(name, defaultValue);
                        runtimeMetaFields.put(name, defaultValue);
                    }
                }
            }
        }

        // Synchronize defaults one last time to capture evaluated field initializers.
        newMetaClass.initializeDefaultValues();

        return newMetaClass;
    }

    /**
     * Resolves the type identity for field defaulting hints.
     * Recognizes intrinsic archetypes that require specific zero-value initialization.
     */
    private Object resolveTypeHint(String typeName, JolkContext context) {
        if ("Long".equals(typeName) || "Int".equals(typeName)) return 0L;
        if ("Double".equals(typeName)) return 0.0;
        if ("Boolean".equals(typeName)) return false;
        if ("String".equals(typeName)) return com.oracle.truffle.api.strings.TruffleString.fromJavaStringUncached("", com.oracle.truffle.api.strings.TruffleString.Encoding.UTF_16);
        if ("Number".equals(typeName)) return 0L;
        if ("Decimal".equals(typeName)) return java.math.BigDecimal.ZERO;

        // Collection Archetypes: Return host identities to maintain Shim-less Integration.
        // We strip generic parameters to resolve the base archetype identity.
        String baseType = typeName.contains("<") ? typeName.substring(0, typeName.indexOf("<")).trim() : typeName;
        if ("Array".equals(baseType) || "List".equals(baseType) || "ArrayList".equals(baseType)) return new java.util.ArrayList<>();
        if ("Map".equals(baseType) || "HashMap".equals(baseType)) return new java.util.LinkedHashMap<>();

        String currentPackage = className.contains(".") ? className.substring(0, className.lastIndexOf('.')) : "";
        Object resolved = resolveClassOrProjection(baseType, currentPackage, context);

        if (resolved instanceof JolkMetaClass jmc) {
            // Identity Resolution: Found an existing guest meta-object.
            return jmc;
        } else if (resolved instanceof String fqn) {
            // Impedance Resolution: Use the FQN to stabilize the type hint.
            return context.getOrCreateClass(fqn);
        }
        
        // Impedance Resolution: Ensure that placeholders for unresolved types 
        // are retrieved through the context to maintain Identity Congruence.
        return context.getOrCreateClass(baseType);
    }

    /// Helper method to resolve a type name (short or FQN) against defined classes and projections.
    private Object resolveClassOrProjection(String typeName, String currentPackage, JolkContext context) {
        // Priority 1: Qualified Identifier
        if (typeName.contains(".")) {
            Object type = context.getDefinedClass(typeName);
            if (type != null) return type;
        }

        // Priority 2: Package-Local Sibling
        if (!currentPackage.isEmpty()) {
            Object local = context.getDefinedClass(currentPackage + "." + typeName);
            if (local != null) return local;
        }

        // Priority 3: Meta-Projection (Expansion/Using)
        Object projected = context.lookupProjection(typeName);
        if (projected != null) return projected;

        // Priority 4: Global Registry / Host Fallback
        return context.getDefinedClass(typeName);
    }

    /**
     * Traverses the method AST to bind all `super` call sites to the current class identity.
     */
    private void bindSuperNodes(JolkNode node, JolkMetaClass holderClass) {
        if (node == null) return;
        if (node instanceof JolkSuperMessageSendNode superNode) {
            superNode.setHolderClass(holderClass);
        }
        for (com.oracle.truffle.api.nodes.Node child : node.getChildren()) {
            if (child instanceof JolkNode jolkChild) {
                bindSuperNodes(jolkChild, holderClass);
            }
        }
    }

    private JolkClosure createMethodClosure(JolkLanguage lang, List<JolkMethodNode> methods) {
        if (methods.size() == 1) {
            JolkMethodNode method = methods.get(0); 
            FrameDescriptor.Builder builder = FrameDescriptor.newBuilder();
            // Allow type specialization for local variables
            builder.addSlots(method.getFrameSlots(), FrameSlotKind.Illegal);
            
            // PERFORMANCE: If there is only one method implementation, bypass the 
            // ArityDispatchNode entirely. This removes a loop and an array 
            // access from every recursive call.
            JolkNode dispatcherBody = method.getBody();
            
            JolkRootNode root = new JolkRootNode(lang, builder.build(), dispatcherBody, method.getName(), method.hasNL());
            return new JolkClosure(root.getCallTarget());
        }

        // Jolk Arity Dispatch: Support overloading by selecting the implementation 
        // that matches the number of arguments provided at runtime.
        int maxSlots = 0;
        boolean hasAnyNL = false;
        for (JolkMethodNode m : methods) {
            maxSlots = Math.max(maxSlots, m.getFrameSlots());
            if (m.hasNL()) hasAnyNL = true;
        }
        
        FrameDescriptor.Builder builder = FrameDescriptor.newBuilder();
        builder.addSlots(maxSlots, FrameSlotKind.Object);
        
        JolkNode dispatcherBody = new JolkArityDispatchNode(methods);
        JolkRootNode root = new JolkRootNode(lang, builder.build(), dispatcherBody, methods.get(0).getName(), hasAnyNL);
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
        @ExplodeLoop
        public Object executeGeneric(VirtualFrame frame) {
            // Aligned Jolk Method Layout: [Self, ...Args]
            int callArity = frame.getArguments().length - 1; 
            for (int i = 0; i < arities.length; i++) {
                if (arities[i] == callArity) {
                    return methodBodies[i].executeGeneric(frame);
                }
            }
            throw new RuntimeException("No method found for arity " + callArity);
        }
    }
}