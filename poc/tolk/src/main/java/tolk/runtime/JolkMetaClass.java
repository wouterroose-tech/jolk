package tolk.runtime;

import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Exclusive;
import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;

import tolk.nodes.JolkDispatchNode;
import tolk.nodes.JolkNode;
import tolk.nodes.JolkReturnException;

import java.util.HashSet;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import java.util.Collections;
import java.util.Set;

/// # JolkMetaClass (Meta-Object Descriptor)
/// 
/// Represents a jolk type (a meta-object) at runtime. It acts as the first-class identity 
/// for classes, records, and enums.
/// 
/// As the primary engine for dual-stratum resolution, it manages the consolidated
/// flattened registry. It facilitates virtual static dispatch, enabling class-level
/// behaviors to participate in a rigorous inheritance model.
///
/// By enforcing the metaboundary during the late flattening process, this class
/// ensures that internal state is shielded by a lexical fence. This architecture
/// renders intrusive reflection a semantic impossibility; there are no guest-level
/// primitives capable of bypassing the defined protocol to interrogate the object's 
/// structural black box.
///
/// Every Type is treated as a live Identity within the **JoMoo** continuum.
/// 
/// @author Wouter Roose
@ExportLibrary(InteropLibrary.class)
public class JolkMetaClass extends DynamicObject {

    public String name;
    protected JolkMetaClass superclass;
    private final JolkFinality finality;
    private final JolkVisibility visibility;
    private final JolkArchetype archetype;
    // Instance members (fields and methods) for instances of this class.
    private final Map<String, Object> instanceMembers;
    // Instance fields (Sovereign Coordinate Map).
    protected Map<String, Object> instanceFields;
    // Map field names to their array index for O(1) access
    protected Map<String, Integer> fieldIndices;
    // The Truffle Shape defining the memory layout for instances
    private Shape instanceShape;
    protected Class<?> hostClass; // The underlying Java class this JolkMetaClass represents
    // Total number of fields including hierarchy
    protected int totalFieldCount;
    // Default field values
    protected Object[] defaultFieldValues; // Initialized in syncDefaultValues
    // Meta members (methods and field accessors).
    private final Map<String, Object> metaMembers;
    private final Map<String, Object> metaFields;
    private final Map<String, JolkVisibility> getterOverrides;
    private final Map<String, JolkVisibility> setterOverrides;
    @CompilationFinal protected final Set<String> stableFields;

    private static final Shape ROOT_META_SHAPE = Shape.newBuilder().build();

    // Optimized consolidated registries
    protected Map<String, Object> instanceRegistry;
    protected Map<String, Object> metaRegistry;
    protected JolkMemberNames cachedInstanceMemberNames;
    protected JolkMemberNames cachedMetaMemberNames;
    protected String[] flattenedFieldNames;
    protected boolean hydrated = false;
    // Enum constants cache for ENUM archetype
    private final Map<String, JolkEnumConstant> enumConstants = new HashMap<>();

    public JolkMetaClass(String name, JolkFinality finality, JolkVisibility visibility, JolkArchetype archetype) {
        this(name, null, finality, visibility, archetype, new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), Collections.emptySet(), null, null, null);
    }

    public JolkMetaClass(String name, JolkFinality finality, JolkVisibility visibility, JolkArchetype archetype, Map<String, Object> instanceMembers) {
        this(name, null, finality, visibility, archetype, instanceMembers, new HashMap<>(), new HashMap<>(), new HashMap<>(), Collections.emptySet(), null, null, null);
    }

    public JolkMetaClass(String name, JolkFinality finality, JolkVisibility visibility, JolkArchetype archetype, Map<String, Object> instanceMembers, Map<String, Object> metaMembers) {
        this(name, null, finality, visibility, archetype, instanceMembers, new HashMap<>(), metaMembers, new HashMap<>(), Collections.emptySet(), null, null, null);
    }

    public JolkMetaClass(String name, JolkMetaClass superclass, JolkFinality finality, JolkVisibility visibility, JolkArchetype archetype) {
        this(name, superclass, finality, visibility, archetype, new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), Collections.emptySet(), null, null, null);
    }

    public JolkMetaClass(String name, JolkMetaClass superclass, JolkFinality finality, JolkVisibility visibility, JolkArchetype archetype, Map<String, Object> instanceMembers, Map<String, Object> metaMembers) {
        this(name, superclass, finality, visibility, archetype, instanceMembers, new HashMap<>(), metaMembers, new HashMap<>(), Collections.emptySet(), null, null, null);
    }

    public JolkMetaClass(String name, JolkMetaClass superclass, JolkFinality finality, JolkVisibility visibility, JolkArchetype archetype, Class<?> hostClass) {
        this(name, superclass, finality, visibility, archetype, new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), Collections.emptySet(), hostClass, null, null);
    }

    // Convenience constructor for unit tests providing instance field templates
    public JolkMetaClass(String name, JolkMetaClass superclass, JolkFinality finality, JolkVisibility visibility, JolkArchetype archetype, Map<String, Object> instanceMembers, Map<String, Object> instanceFields, Map<String, Object> metaMembers) {
        this(name, superclass, finality, visibility, archetype, instanceMembers, instanceFields, metaMembers, new HashMap<>(), Collections.emptySet(), null, null, null);
    }

    /**
     * ### JolkMetaClass
     * 
     * The primary constructor for the Meta-Object Descriptor.
     * 
     * @param name The nominal identity of the type.
     * @param superclass The parent identity in the meta-stratum.
     * @param finality Structural constraints (FINAL, OPEN).
     * @param visibility Access level of the identity.
     * @param archetype The structural template (CLASS, RECORD, etc).
     * @param instanceMembers Consolidated instance-level logic.
     * @param instanceFields Sovereign coordinate map for instance state.
     * @param metaMembers Consolidated type-level logic.
     * @param metaFields Type-level state (constants/statics).
     * @param stableFields Fields marked for immutability.
     * @param hostClass The Java substrate class for shim-less integration.
     * @param getterOverrides Visibility overrides for synthesized getters.
     * @param setterOverrides Visibility overrides for synthesized setters.
     */
    public JolkMetaClass(String name, 
                        JolkMetaClass superclass, 
                        JolkFinality finality, 
                        JolkVisibility visibility, 
                        JolkArchetype archetype, 
                        Map<String, Object> instanceMembers, 
                        Map<String, Object> instanceFields, 
                        Map<String, Object> metaMembers, 
                        Map<String, Object> metaFields, 
                        Set<String> stableFields, 
                        Class<?> hostClass,
                        Map<String, JolkVisibility> getterOverrides,
                        Map<String, JolkVisibility> setterOverrides) {
        super(ROOT_META_SHAPE);
        this.name = name;
        this.superclass = superclass;
        this.finality = finality;
        this.visibility = visibility;
        this.archetype = archetype;
        this.instanceShape = Shape.newBuilder().build(); // Bootstrap anchor to prevent NPE in JolkObject
        this.instanceMembers = instanceMembers != null ? instanceMembers : new HashMap<>();
        this.instanceFields = instanceFields; // Reference retention for definition protocol
        this.fieldIndices = new HashMap<>();
        this.metaFields = metaFields != null ? metaFields : new HashMap<>();
        this.hostClass = hostClass;
        this.metaMembers = metaMembers != null ? metaMembers : new HashMap<>();
        this.stableFields = stableFields != null ? Collections.unmodifiableSet(new HashSet<>(stableFields)) : Collections.emptySet();
        this.instanceRegistry = new HashMap<>(); // Prevents NPE during circular bootstrapping
        this.metaRegistry = new HashMap<>();     // Prevents NPE during circular bootstrapping
        this.getterOverrides = getterOverrides != null ? getterOverrides : Collections.emptyMap();
        this.setterOverrides = setterOverrides != null ? setterOverrides : Collections.emptyMap();
    }

    public Class<?> getHostClass() {
        return hostClass;
    }

    /// Performs **Late Flattening**.
    ///
    /// Registries and field layouts are calculated on-demand. This solves forward
    /// references because a subclass can be created before its superclass is fully
    /// defined, as long as both are defined before the first message is sent.
    protected synchronized void ensureHydrated() {
        if (hydrated) return;

        // Jolk Hydration Guard: Do not hydrate placeholders directly.
        if (this instanceof JolkMetaClassPlaceholder) return;

        // Structural Dependency Guard: If the superclass is a placeholder 
        // that hasn't been reified yet, we cannot flatten this registry.
        // Returning early without setting 'hydrated = true' allows 
        // hydration to be retried on the next message send.
        if (superclass != null) {
            superclass.ensureHydrated();
            if (!superclass.isHydrated()) return;
        }

        // 1. Calculate Instance Field Layout
        int superCount = (superclass != null) ? superclass.getFieldCount() : 0;
        this.flattenedFieldNames = new String[superCount + instanceFields.size()];
        
        // Structural Continuity: inherit the shape from the superclass if available.
        if (superclass != null && superclass.instanceShape != null) {
            this.instanceShape = superclass.instanceShape;
        } else if (this.instanceShape == null) {
            this.instanceShape = Shape.newBuilder().build();
        }
        
        if (superclass != null) {
            System.arraycopy(superclass.getFlattenedFieldNames(), 0, this.flattenedFieldNames, 0, superCount);
        }

        int currentIndex = superCount;
        for (String fieldName : instanceFields.keySet()) {
            this.fieldIndices.put(fieldName, currentIndex++);
            this.flattenedFieldNames[currentIndex - 1] = fieldName;
        }
        this.totalFieldCount = currentIndex;
        this.defaultFieldValues = new Object[totalFieldCount];

        // 2. Calculate Meta Field Layout
        // Meta-stratum collapse: fields are now stored directly in DynamicObject slots.
        // Inheritance and defaults are handled during the synchronization phase.

        // 3. Consolidate Flattened Registries (Industrial O(1) Lookup)
        this.instanceRegistry = new HashMap<>();
        this.metaRegistry = new HashMap<>();
        if (superclass != null) {
            // Defensive check: superclass registries might be null if superclass is a placeholder
            // that hasn't been fully hydrated yet
            if (superclass.instanceRegistry != null) {
                this.instanceRegistry.putAll(superclass.instanceRegistry);
            }
            if (superclass.metaRegistry != null) {
                this.metaRegistry.putAll(superclass.metaRegistry);
            }
        }
        this.instanceRegistry.putAll(this.instanceMembers);
        this.metaRegistry.putAll(this.metaMembers);

        this.hydrated = true;
        syncDefaultValues(); // Now safe to call as it's not a placeholder
        refreshInstanceMemberCache();
        refreshMetaMemberCache();
    }

    /**
     * Returns true if this class has completed its structural flattening.
     * Used by subclasses to determine if they can safely inherit layout.
     */
    public boolean isHydrated() {
        if (!hydrated) ensureHydrated();
        return hydrated;
    }

    /**
     * Returns the structural archetype of this type (CLASS, RECORD, etc).
     */
    public JolkArchetype getArchetype() {
        return archetype;
    }

    /**
     * Returns true if the named field is marked as stable or constant.
     * Records implicitly treat all fields as stable.
     */
    public boolean isFieldStable(String fieldName) {
        if (archetype == JolkArchetype.RECORD) return true;
        if (stableFields != null && stableFields.contains(fieldName)) return true;
        return superclass != null && superclass.isFieldStable(fieldName);
    }

    public JolkVisibility getAccessorVisibility(String fieldName, boolean isSetter) {
        JolkVisibility override = isSetter ? setterOverrides.get(fieldName) : getterOverrides.get(fieldName);
        if (override != null) return override;
        return superclass != null ? superclass.getAccessorVisibility(fieldName, isSetter) : JolkVisibility.PUBLIC;
    }

    /**
     * Returns true if this class identity is stable and ready for hydration.
     * Placeholders are unready until their actual definition is published.
     */
    public boolean isReady() {
        return true;
    }

    /// ### registerInstanceMethod
    ///
    /// Registers a Java-implemented method for instances of this type.
    public void registerInstanceMethod(String name, Object method) {
        this.instanceMembers.put(name, method);
        if (!hydrated) return;
        // Ensure the consolidated registry and interop caches stay in sync
        this.instanceRegistry.put(name, method);
        refreshInstanceMemberCache();
    }

    /// ### registerMetaMethod
    ///
    /// Registers a Java-implemented method for the type itself (the MetaClass) and refreshes caches.
    public void registerMetaMethod(String name, Object method) {
        this.metaMembers.put(name, method);
        if (!hydrated) return;
        this.metaRegistry.put(name, method);
        refreshMetaMemberCache();
    }

    /// ### registerEnumConstant
    ///
    /// Registers an enum constant for ENUM archetype types.
    public void registerEnumConstant(String name) {
        if (archetype != JolkArchetype.ENUM) {
            throw new IllegalStateException("Cannot register enum constants on non-enum type: " + this.name);
        }
        int ordinal = enumConstants.size();
        JolkEnumConstant constant = new JolkEnumConstant(this, name, ordinal);
        enumConstants.put(name, constant);
        // Also register as meta constant so it can be accessed as Type#CONSTANT
        metaMembers.put(name, constant);
        if (hydrated) {
            metaRegistry.put(name, constant);
            refreshMetaMemberCache();
        }
    }

    /// ### getJolkMetaClass
    /// 
    /// Returns the meta-identity of this object. In the Jolk Meta-Object Protocol,
    /// classes (MetaClasses) act as their own meta-identities.
    /// 
    /// @return This MetaClass instance.
    public JolkMetaClass getJolkMetaClass() {
        return this; // In the PoC, classes are their own meta-identities.
    }

    public JolkMetaClass getSuperclass() {
        return superclass;
    }

    public JolkFinality getFinality() {
        return finality;
    }

    public JolkVisibility getVisibility() {
        return visibility;
    }

    public Object getMetaFieldValue(int index) {
        throw new UnsupportedOperationException("Index-based meta field access is deprecated. Use name-based property access.");
    }

    public void setMetaFieldValue(int index, Object value) {
        throw new UnsupportedOperationException("Index-based meta field access is deprecated. Use name-based property access.");
    }

    public void setMetaFieldValue(String name, Object value) {
        ensureHydrated();
        DynamicObjectLibrary.getUncached().put(this, name, value);
        refreshMetaMemberCache(); // Discovery Integrity: Ensure new constants are visible
    }

    /// ### initializeDefaultValues
    ///
    /// Public entry point used by the definition node to synchronize structural
    /// templates. In the Late Flattening model, this triggers full hydration
    /// to ensure indices and registries are prepared for use.
    public void initializeDefaultValues() {
        ensureHydrated();
        syncDefaultValues();
    }

    private void syncDefaultValues() {
        DynamicObjectLibrary metaLib = DynamicObjectLibrary.getUncached();
        if (superclass != null) {
            // Ensure superclass is hydrated so its default arrays are allocated.
            superclass.ensureHydrated();
            if (superclass.defaultFieldValues != null) {
                System.arraycopy(superclass.defaultFieldValues, 0, this.defaultFieldValues, 0, superclass.totalFieldCount);
            }
            // Synchronize inherited meta-fields into this meta-object's slots
            for (String key : superclass.getMetaPropertyKeys()) {
                metaLib.put(this, key, metaLib.getOrDefault(superclass, key, JolkNothing.INSTANCE));
            }
        }
        for (Map.Entry<String, Object> entry : instanceFields.entrySet()) {
            Integer idx = fieldIndices.get(entry.getKey());
            if (idx != null) {
                Object val = entry.getValue();
                
                // Jolk Defaulting Protocol: Distinguish between Type Sentinels (Hints) 
                // and Realized Values. In this model, Meta-Objects are Hints, 
                // and Strings are literal data.
                if (val instanceof JolkMetaClass || val instanceof Class<?> || val == JolkNothing.INSTANCE || val == null) {
                    String hint = resolveTypeHint(val);
                    if (isLongHint(hint) || (val == null && entry.getKey().equalsIgnoreCase("id"))) {
                        this.defaultFieldValues[idx] = 0L;
                    } else if (isBooleanHint(hint)) {
                        this.defaultFieldValues[idx] = false;
                    } else if (isStringHint(hint)) {
                        this.defaultFieldValues[idx] = "";
                    } else {
                        // Uninitialized user-defined types default to Nothing.
                        this.defaultFieldValues[idx] = JolkNothing.INSTANCE;
                    }
                } else {
                    // Realized Value: The initializer result is preserved exactly.
                    this.defaultFieldValues[idx] = val;
                }
            }
        }

        // Meta-Level Primary Initializer Expansion
        for (Map.Entry<String, Object> entry : metaFields.entrySet()) {
            // Only initialize if not already set by superclass or explicit value
            if (!metaLib.containsKey(this, entry.getKey())) {
                Object val = entry.getValue();
                Object resolvedVal;
                if (val instanceof JolkMetaClass || val instanceof Class<?> || val == JolkNothing.INSTANCE || val == null) {
                    String hint = resolveTypeHint(val);
                    if (isLongHint(hint) || ((val == null || val == JolkNothing.INSTANCE) && entry.getKey().equalsIgnoreCase("id"))) {
                        resolvedVal = 0L;
                    } else if (isBooleanHint(hint)) {
                        resolvedVal = false;
                    } else if (isStringHint(hint)) {
                        resolvedVal = "";
                    } else {
                        resolvedVal = JolkNothing.INSTANCE;
                    }
                } else {
                    resolvedVal = val == null ? JolkNothing.INSTANCE : val;
                }
                metaLib.put(this, entry.getKey(), resolvedVal);
            }
        }
    }

    private static String resolveTypeHint(Object val) {
        if (val instanceof JolkMetaClass mc) return mc.name;
        if (val instanceof String s) return s;
        if (val instanceof Class<?> c) return c.getSimpleName();
        return null;
    }

    public static boolean isLongHint(String hint) {
        if (hint == null) return false;
        return hint.equalsIgnoreCase("Long") 
            || hint.equalsIgnoreCase("Int")
            || hint.equalsIgnoreCase("jolk.lang.Long");
    }

    public static boolean isDecimalHint(String hint) {
        if (hint == null) return false;
        return hint.equalsIgnoreCase("Decimal") 
            || hint.equalsIgnoreCase("jolk.lang.Decimal");
    }

    public static boolean isDoubleHint(String hint) {
        if (hint == null) return false;
        return hint.equalsIgnoreCase("Double") 
            || hint.equalsIgnoreCase("jolk.lang.Double");
    }

    public static boolean isBooleanHint(String hint) {
        if (hint == null) return false;
        return hint.equalsIgnoreCase("Boolean") 
            || hint.equalsIgnoreCase("jolk.lang.Boolean");
    }
    
    public static boolean isNumberHint(String hint) {
        if (hint == null) return false;
        return hint.equalsIgnoreCase("Number")
            || hint.equalsIgnoreCase("jolk.lang.Number");
    }

    public static boolean isStringHint(String hint) {
        if (hint == null) return false;
        return hint.equalsIgnoreCase("String") || hint.equalsIgnoreCase("jolk.lang.String");
    }

    @ExportMessage
    public boolean hasMetaObject() {
        return true;
    }

    @ExportMessage
    public Object getMetaObject() {
        return this;
    }

    @ExportMessage
    public boolean isMetaObject() {
        return true;
    }

    @ExportMessage
    public Object getMetaQualifiedName() {
        return name;
    }

    @ExportMessage
    @TruffleBoundary
    public Object getMetaSimpleName() {
        int lastDot = name.lastIndexOf('.');
        return lastDot == -1 ? name : name.substring(lastDot + 1);
    }

    @ExportMessage
    public boolean isMetaInstance(Object instance) {
        InteropLibrary interop = InteropLibrary.getUncached();

        // 1. Handle Jolk's reified null (`Nothing`)
        if (instance == JolkNothing.INSTANCE) {
            // Nothing is an instance of its own type and also of Object.
            if (this == JolkNothing.NOTHING_TYPE) return true;
        }

        // 2. Handle Jolk's intrinsic numeric archetypes.
        // We use InteropLibrary to handle both raw primitives and polyglot-wrapped values.
        if (interop.isNumber(instance)) {
            if (isNumberHint(this.name)) return true;
            if (isLongHint(this.name)) return true;
            if (isDoubleHint(this.name)) return true;
            if (isDecimalHint(this.name)) return true;
        }

        // 3. Handle Jolk's intrinsic `Boolean` archetype.
        if (interop.isBoolean(instance)) {
            // Primitives match their specialized archetypes.
            if (isBooleanHint(this.name)) return true;
        }

        // 4. Handle Jolk's intrinsic `String` archetype.
        if (instance instanceof String) {
            if (isStringHint(this.name)) return true;
        }

        // 5. Hierarchy Walk: Support any identity that provides a Jolk Meta-Object.
        try {
            Object meta = interop.hasMetaObject(instance) ? interop.getMetaObject(instance) : null;
            if (meta instanceof JolkMetaClass current) {
                while (current != null) {
                    if (current == this) return true;
                    current = current.superclass;
                }
            }
        } catch (UnsupportedMessageException e) {
            // Fall through
        }

        // 6. Root Object Fallback: JolkObjects, Nothing, and primitives are instances of Object.
        // Note: During message dispatch, specialized MetaClasses (Boolean/Long) must be 
        // registered before the root Object archetype to avoid shadowing specialized members.
        if (this.superclass == null && "Object".equals(this.name)) {
            return instance instanceof JolkObject || instance == JolkNothing.INSTANCE ||
                   interop.isNumber(instance) || interop.isBoolean(instance) || instance instanceof String;
        }

        // 7. For the PoC, other host objects are not considered instances of Jolk types.
        if (this.superclass == null && "Number".equals(this.name)) {
            return interop.isNumber(instance) ||
                   interop.isNumber(instance) || interop.isBoolean(instance) || instance instanceof String;
        }

        // 7. For the PoC, other host objects are not considered instances of Jolk types.
        return false;
    }

    /// ### isMemberReadable
    ///
    /// Returns true if the identifier exists in the meta-member map.
    @ExportMessage
    public boolean isMemberReadable(String member,
                                    @Exclusive @CachedLibrary("this") DynamicObjectLibrary objLib) {
        ensureHydrated();
        // 1. Check DynamicObject properties (for meta fields directly stored there)
        if (objLib.containsKey(this, member)) return true;
        
        // 2. Check metaRegistry for non-executable members (constants/fields)
        if (metaRegistry != null && metaRegistry.containsKey(member)) {
            Object memberObj = metaRegistry.get(member);
            // A member is readable if it's not a method (executable) or if it's an enum constant
            return !InteropLibrary.getUncached().isExecutable(memberObj) || memberObj instanceof JolkEnumConstant;
        }

        // For ENUM types, check enumConstants directly as a fallback
        if (archetype == JolkArchetype.ENUM && enumConstants.containsKey(member)) return true;

        // Fallback for host statics (Fields)
        if (hostClass != null) {
            Object meta = lookupMetaMember(member);
            return meta != null && !InteropLibrary.getUncached().isExecutable(meta);
        }
        return false;
    }

    /// ### readMember
    ///
    /// Retrieves the internal substrate object (Closure or Accessor) for a meta-member.
    @ExportMessage
    public Object readMember(String member,
                             @Exclusive @CachedLibrary("this") DynamicObjectLibrary objLib) throws UnknownIdentifierException {
        ensureHydrated();
        Object val = (metaRegistry != null) ? metaRegistry.get(member) : null;
        if (val == null) {
            val = objLib.getOrDefault(this, member, null);
        }
        if (val == null && archetype == JolkArchetype.ENUM) {
            // Fallback for enum constants in case they aren't in metaRegistry
            val = enumConstants.get(member);
        }
        if (val == null && hostClass != null) {
            // Attempt to read static field from host class
            try {
                val = JolkDispatchNode.dispatchHostMember(hostClass, member, new Object[0]);
                if (InteropLibrary.getUncached().isExecutable(val)) val = null; // Methods are not readable
            } catch (UnknownIdentifierException e) { /* continue */ }
        }
        if (val == null) {
            throw UnknownIdentifierException.create(member);
        }
        return JolkNode.interopLift(val);
    }

    @ExportMessage
    public boolean hasMembers() {
        return true;
    }

    @ExportMessage
    public Object getMembers(boolean includeInternal) throws UnsupportedMessageException {
        ensureHydrated();
        // This returns the members of the META-OBJECT, not the instance.
        return cachedMetaMemberNames;
    }

    @ExportMessage
    public boolean isMemberInvocable(String member,
                                     @Exclusive @CachedLibrary("this") DynamicObjectLibrary objLib) {
        ensureHydrated();
        // 1. Check metaRegistry for executable members (methods)
        if (metaRegistry != null && metaRegistry.containsKey(member)) {
            Object memberObj = metaRegistry.get(member);
            if (InteropLibrary.getUncached().isExecutable(memberObj)) {
                return true;
            }
        }
        // 2. Check DynamicObject properties (meta fields are not invocable directly)
        // if (objLib.containsKey(this, member)) return true; // Removed, fields are not invocable
        
        // 3. Local Meta-Intrinsics (Identity properties should not delegate)
        if (switch (member) {
            case "new", "name", "qualifiedName", "superclass", "isInstance" -> true;
            default -> false;
        }) return true;

        // 4. Object Intrinsic Protocol (e.g., "toString", "hash")
        if (isObjectIntrinsic(member)) return true;

        // 5. Host Class Static Methods
        if (hostClass != null) { // This path is for static methods on the host class.
            Object meta = lookupMetaMember(member);
            return meta != null && InteropLibrary.getUncached().isExecutable(meta);
        }

        return false;
    }

    /// ### callMetaMember
    ///
    /// Convenience method for calling meta-level messages from Java code or unit tests.
    @TruffleBoundary
    public Object callMetaMember(String member) throws UnknownIdentifierException, ArityException, UnsupportedTypeException, UnsupportedMessageException {
        return callMetaMember(member, new Object[]{});
    }
    
    @TruffleBoundary
    public Object callMetaMember(String member, Object[] arguments) throws UnknownIdentifierException, ArityException, UnsupportedTypeException, UnsupportedMessageException {
        return invokeMember(member, arguments, InteropLibrary.getUncached(), member, lookupMetaMember(member), DynamicObjectLibrary.getUncached());
    }

    @TruffleBoundary
    static Object doLookupMeta(JolkMetaClass receiver, String member) {
        return receiver.lookupMetaMember(member);
    }

    @ExportMessage
    public Object invokeMember(String member, Object[] arguments,
                        @CachedLibrary(limit = "3") InteropLibrary interop,
                        @Cached(value = "member", allowUncached = true, neverDefault = false) String cachedMember,
                        @Cached(value = "doLookupMeta(this, member)", allowUncached = true, neverDefault = false) Object cachedValue, // This is for methods/enum constants
                        @Exclusive @CachedLibrary("this") DynamicObjectLibrary objLib) throws UnknownIdentifierException, ArityException, UnsupportedTypeException, UnsupportedMessageException {
        ensureHydrated();
        
        if (member.equals(cachedMember) && cachedValue != null) {
            Object memberObj = cachedValue;
            // Enum constants are returned directly, not executed
            if (memberObj instanceof JolkEnumConstant) {
                if (arguments.length != 0) throw ArityException.create(0, 0, arguments.length);
                return memberObj;
            }

            // Static Property Read: If the lookup found a non-executable host member
            // (like a static field), return it directly for unary message sends.
            if (!interop.isExecutable(memberObj)) {
                if (arguments.length != 0) throw ArityException.create(0, 0, arguments.length);
                return JolkBuiltinMethod.lift(memberObj);
            }

            // Jolk Protocol: Every method call must include the receiver as the first argument.
            Object[] metaArguments = new Object[arguments.length + 1];
            metaArguments[0] = this;
            if (arguments.length > 0) System.arraycopy(arguments, 0, metaArguments, 1, arguments.length);
            try {
                return JolkNode.interopLift(InteropLibrary.getUncached(memberObj).execute(memberObj, metaArguments));
            } catch (JolkReturnException e) {
                throw e; // Preserve signal for non-local returns
            }
        }

        // Fallback for non-cached members
        if (metaRegistry != null && metaRegistry.containsKey(member)) {
            Object memberObj = metaRegistry.get(member);
            if (memberObj instanceof JolkEnumConstant) {
                if (arguments.length != 0) throw ArityException.create(0, 0, arguments.length);
                return memberObj;
            }
            if (!interop.isExecutable(memberObj)) {
                if (arguments.length != 0) throw ArityException.create(0, 0, arguments.length);
                return JolkBuiltinMethod.lift(memberObj);
            }
            Object[] metaArguments = new Object[arguments.length + 1];
            metaArguments[0] = this;
            if (arguments.length > 0) System.arraycopy(arguments, 0, metaArguments, 1, arguments.length);
            try {
                return JolkNode.interopLift(InteropLibrary.getUncached(memberObj).execute(memberObj, metaArguments));
            } catch (JolkReturnException e) {
                throw e;
            }
        }

        // Fallback for non-cached host members
        if (hostClass != null) {
            Object hostMember = lookupMetaMember(member);
            if (hostMember != null) {
                if (!interop.isExecutable(hostMember)) {
                    if (arguments.length != 0) throw ArityException.create(0, 0, arguments.length);
                    return JolkBuiltinMethod.lift(hostMember);
                }
                Object[] metaArguments = new Object[arguments.length + 1];
                metaArguments[0] = this;
                if (arguments.length > 0) System.arraycopy(arguments, 0, metaArguments, 1, arguments.length);
                return JolkBuiltinMethod.lift(InteropLibrary.getUncached(hostMember).execute(hostMember, metaArguments));
            }
        }

        // 2. Property Projection for Meta-Fields (The "Collapse")
        if (objLib.containsKey(this, member)) {
            if (arguments.length == 0) {
                // Getter Pattern: Type #field
                Object value = objLib.getOrDefault(this, member, JolkNothing.INSTANCE);
                if (value instanceof JolkLazyValue lazyValue) {
                    return lazyValue.get(this); // Trigger lazy evaluation with the MetaClass as receiver
                }
                return JolkNode.interopLift(value);
            } else if (arguments.length == 1) {
                // Immutability Enforcement: Meta constants are non-assignable.
                if (isFieldStable(member)) {
                    throw UnsupportedMessageException.create();
                }
                // Setter Pattern: Type #field(val) -> Returns Self (Fluent Contract)
                objLib.put(this, member, JolkNode.unwrap(arguments[0]));
                return this;
            }
            throw ArityException.create(0, 1, arguments.length);
        }

        // 2. Local Meta-Intrinsics: Identity properties specific to this class
        switch (member) {
            case "new":
                // If this class extends a Host Class, we MUST use host instantiation 
                // to ensure the identity contains the substrate state (e.g. for Exceptions).
                if (hostClass != null) {
                    return JolkNode.interopLift(JolkDispatchNode.dispatchObjectIntrinsic(this, "new", arguments, interop, tolk.language.JolkLanguage.getContext()));
                }
                // For enums, new is not allowed - enum constants are accessed directly
                if (archetype == JolkArchetype.ENUM) {
                    throw UnsupportedMessageException.create();
                }
                // Canonical #new logic: Only applies if no explicit meta-method named 'new' exists.
                // This allows the variadic 'meta Map new(Object...)' to take precedence.
                // This is the intrinsic allocation for JolkMetaClass.
                if (arguments.length == 0) return JolkNode.lift(new JolkObject(this));
                
                // The arity check for canonical constructor
                if (arguments.length == totalFieldCount) {
                    return JolkNode.lift(new JolkObject(this, arguments));
                }
                throw ArityException.create(totalFieldCount, totalFieldCount, arguments.length);
            case "name":
                if (arguments.length != 0) throw ArityException.create(0, 0, arguments.length);
                return getMetaSimpleName();
            case "qualifiedName":
                if (arguments.length != 0) throw ArityException.create(0, 0, arguments.length);
                return name;
            case "superclass":
                if (arguments.length != 0) throw ArityException.create(0, 0, arguments.length);
                return superclass != null ? superclass : JolkNothing.INSTANCE;
            case "isInstance":
                if (arguments.length != 1) throw ArityException.create(1, 1, arguments.length);
                return isMetaInstance(arguments[0]);
            case "metaProtocol":
                if (arguments.length != 0) throw ArityException.create(0, 0, arguments.length);
                return JolkNode.interopLift(getMembers(true));
            case "instanceProtocol":
                if (arguments.length != 0) throw ArityException.create(0, 0, arguments.length);
                return JolkNode.interopLift(getInstanceMemberNames());
            case "project":
                return JolkNode.interopLift(JolkDispatchNode.dispatchObjectIntrinsic(this, member, arguments, interop, tolk.language.JolkLanguage.getContext()));
        }
        // 2. Jolk Object Protocol: Classes are polite JoMoos
        Object intrinsicResult = JolkDispatchNode.dispatchObjectIntrinsic(this, member, arguments, interop, tolk.language.JolkLanguage.getContext());
        if (intrinsicResult != null) {
            return JolkNode.interopLift(intrinsicResult);
        }

        throw UnknownIdentifierException.create(member);
    }

    @ExportMessage.Ignore
    public static Object invokeSuperMember(JolkMetaClass receiver, JolkMetaClass startClass, String member, Object[] arguments,
                             InteropLibrary interop) throws UnknownIdentifierException, ArityException, UnsupportedTypeException, UnsupportedMessageException {
        if (startClass == null) {
            throw new IllegalArgumentException("Cannot invoke super member from root object");
        }
        startClass.ensureHydrated();
        Object memberObj = isObjectIntrinsic(member) ? null : startClass.lookupMetaMember(member);
        if (memberObj != null) {
            // Enum constants are returned directly, not executed
            if (memberObj instanceof JolkEnumConstant) {
                if (arguments.length != 0) throw ArityException.create(0, 0, arguments.length);
                return memberObj;
            }
            Object[] metaArguments = new Object[arguments.length + 1];
            metaArguments[0] = receiver;
            if (arguments.length > 0) System.arraycopy(arguments, 0, metaArguments, 1, arguments.length);
            try {
                return interop.execute(memberObj, metaArguments);
            } catch (JolkReturnException e) {
                throw e;
            }
        }

        switch (member) {
            case "new":
                // Arity 0: Default birth
                if (arguments.length == 0) return new JolkObject(receiver);
                // Arity N: Canonical birth matching field layout
                if (arguments.length == receiver.totalFieldCount) {
                    return new JolkObject(receiver, arguments);
                }
                throw ArityException.create(0, receiver.totalFieldCount, arguments.length);
            case "name":
                if (arguments.length != 0) throw ArityException.create(0, 0, arguments.length);
                return receiver.getMetaSimpleName();
            case "qualifiedName":
                if (arguments.length != 0) throw ArityException.create(0, 0, arguments.length);
                return receiver.name;
            case "superclass":
                if (arguments.length != 0) throw ArityException.create(0, 0, arguments.length);
                return receiver.superclass != null ? receiver.superclass : JolkNothing.INSTANCE;
            case "isInstance":
                if (arguments.length != 1) throw ArityException.create(1, 1, arguments.length);
                return receiver.isMetaInstance(arguments[0]);
        }

        Object intrinsicResult = JolkIntrinsicProtocol.dispatchObjectIntrinsic(receiver, member, arguments, interop);
        if (intrinsicResult != null) {
            return intrinsicResult;
        }

        throw UnknownIdentifierException.create(member);
    }

    /// @param member The selector name to check.
    /// @return true if the selector is a Jolk intrinsic.
    public static boolean isObjectIntrinsic(String member) {
        return "case".equals(member) || "project".equals(member) || "respondsTo".equals(member) || 
               "metaProtocol".equals(member) || "instanceProtocol".equals(member) || "message".equals(member) ||
               "stateProjection".equals(member) || JolkIntrinsicProtocol.isObjectIntrinsic(member);
    }
    
    
    // --- Instance member lookup (for JolkObject) ---
    
    /**
     * ## hasInstanceMember
     *
     * Checks if a member with the given name exists on instances of this class.
     * This is used by `JolkObject` to determine if it can respond to an
     * instance-level message.
     *
     * @param name The name of the member.
     * @return `true` if an instance member with that name exists.
     */
    public boolean hasInstanceMember(String name) {
        ensureHydrated();
        return instanceRegistry != null && instanceRegistry.containsKey(name);
    }

    /**
     * ## hasMetaMember
     *
     * Checks if a member with the given name exists on the MetaClass itself.
     * This is the meta-level counterpart to `hasInstanceMember`.
     *
     * @param name The name of the member.
     * @return `true` if the MetaClass responds to this selector.
     */
    public boolean hasMetaMember(String name) {
        ensureHydrated();
        if (metaRegistry != null && metaRegistry.containsKey(name)) return true;
        if (DynamicObjectLibrary.getUncached().containsKey(this, name)) return true;
        if (switch (name) {
            case "new", "name", "qualifiedName", "superclass", "isInstance" -> true;
            default -> false;
        }) return true;
        if (isObjectIntrinsic(name)) return true;
        return hostClass != null && lookupMetaMember(name) != null;
    }

    /**
     * ## lookupInstanceMember
     *
     * Looks up an instance member (method or field accessor) by name.
     * This is O(1) and non-recursive due to hierarchy flattening.
     */
    public Object lookupInstanceMember(String name) {
        ensureHydrated();
        return (instanceRegistry != null) ? instanceRegistry.get(name) : null;
    }

    /**
     * ## lookupMetaMember
     *
     * Looks up a meta-level member (e.g., a built-in factory method like #new) 
     * by name. This is used by the dispatch system to resolve messages 
     * sent to the MetaClass itself.
     *
     * @param name The name of the meta-member to look up.
     * @return The meta-member object, or null if not found.
     */
    public Object lookupMetaMember(String name) {
        ensureHydrated();
        Object member = (metaRegistry != null) ? metaRegistry.get(name) : null;
        if (member != null) {
            return member;
        }
        // Fallback: Check for static members on the host Java class
        if (hostClass != null) {
            try {
                // Use JolkDispatchNode's logic to read static members from the hostClass
                // This handles both static fields and static methods.
                return JolkDispatchNode.dispatchHostMember(hostClass, name, new Object[0]);
            } catch (UnknownIdentifierException e) {
                // Not found as a host member, return null
                return null;
            } catch (Exception e) {
                throw new RuntimeException("Error looking up meta member " + name + " on host class " + hostClass.getName(), e);
            }
        }
        return null;
    }

    /**
     * ## getInstanceMemberKeys
     *
     * @return A set of all instance member names.
     */
    public Set<String> getInstanceMemberKeys() {
        ensureHydrated();
        return instanceRegistry.keySet();
    }

    /**
     * Returns the pre-calculated Interop member collection for instances.
     */
    public JolkMemberNames getInstanceMemberNames() {
        ensureHydrated();
        return cachedInstanceMemberNames != null ? cachedInstanceMemberNames : new JolkMemberNames(new String[0]);
    }

    /**
     * Returns the number of meta-properties currently stored in this meta-object.
     */
    public int getMetaFieldCount() {
        ensureHydrated();
        return DynamicObjectLibrary.getUncached().getKeyArray(this).length;
    }

    public Set<String> getMetaPropertyKeys() {
        ensureHydrated();
        DynamicObjectLibrary lib = DynamicObjectLibrary.getUncached();
        Object[] keyArray = lib.getKeyArray(this);
        Set<String> keys = new HashSet<>(keyArray.length);
        for (Object key : keyArray) {
            if (key instanceof String s) keys.add(s);
        }
        return keys;
    }

    private void refreshInstanceMemberCache() {
        if (!hydrated) return; // Only refresh if hydrated
        Set<String> instanceKeys = new HashSet<>(instanceRegistry.keySet());
        instanceKeys.addAll(JolkIntrinsicProtocol.INTRINSIC_MEMBERS);
        if (flattenedFieldNames != null) {
            instanceKeys.addAll(Arrays.asList(flattenedFieldNames));
        }
        this.cachedInstanceMemberNames = new JolkMemberNames(instanceKeys.toArray(new String[0]));
    }

    private void refreshMetaMemberCache() {
        if (!hydrated) return; // Only refresh if hydrated
        Set<String> metaKeys = new HashSet<>(this.metaRegistry.keySet());

        // Add Meta-Properties from DynamicObject slots
        metaKeys.addAll(getMetaPropertyKeys());

        // Standard Meta-Object Protocol members
        metaKeys.add("new");
        metaKeys.add("name");
        metaKeys.add("qualifiedName");
        metaKeys.add("superclass");
        metaKeys.add("isInstance");
        metaKeys.add("metaProtocol");
        metaKeys.add("instanceProtocol");
        metaKeys.addAll(JolkIntrinsicProtocol.INTRINSIC_MEMBERS);
        this.cachedMetaMemberNames = new JolkMemberNames(metaKeys.toArray(new String[0]));
    }

    public int getFieldCount() {
        ensureHydrated();
        return totalFieldCount;
    }

    public int getFieldIndex(String name) {
        ensureHydrated();
        Integer index = fieldIndices.get(name);
        if (index != null) return index;
        if (superclass != null) return superclass.getFieldIndex(name);
        return -1;
    }

    Object[] getDefaultFieldValues() {
        ensureHydrated();
        return defaultFieldValues != null ? defaultFieldValues : new Object[0];
    }

    public Shape getInstanceShape() {
        ensureHydrated();
        return instanceShape;
    }

    /**
     * Returns the full set of instance field names across the hierarchy in layout order.
     *
     * @return An array of flattened field names.
     */
    public String[] getFlattenedFieldNames() {
        ensureHydrated();
        return flattenedFieldNames != null ? flattenedFieldNames : new String[0];
    }

    /**
     * Returns an array of instance field names defined for this meta-class.
     *
     * @return An array of field names.
     */
    public String[] getInstanceFieldNames() {
        ensureHydrated();
        return instanceFields.keySet().toArray(new String[0]);
    }

    /**
     * ### JolkMetaClassPlaceholder
     * 
     * A specialized JolkMetaClass that acts as a forward-reference proxy. 
     * It allows classes to be defined that extend types not yet encountered 
     * by the parser. When the actual type is defined, the placeholder 
     * synchronizes its internal state with the real class identity.
     */
    public static class JolkMetaClassPlaceholder extends JolkMetaClass {
        private JolkMetaClass actualClass;

        public JolkMetaClassPlaceholder(String name) {
            // Initialize with minimal skeleton state. 
            // Fields and registries will be populated during updatePlaceholder.
            super(name, null, JolkFinality.OPEN, JolkVisibility.PUBLIC, JolkArchetype.CLASS,
                  new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), Collections.emptySet(), null, null, null); // Pass null for hostClass
            this.hydrated = false;
            // Initialize empty registries for defensive access during hydration
            this.instanceRegistry = new HashMap<>();
            this.metaRegistry = new HashMap<>();
        }

        /**
         * Updates this placeholder with the actual JolkMetaClass data.
         * This method is called when the actual definition of the forward-referenced class is processed.
         */
        @TruffleBoundary
        public synchronized void updatePlaceholder(JolkMetaClass actualClass) {
            if (this.actualClass != null) {
                throw new IllegalStateException("JolkMetaClassPlaceholder for " + name + " already updated.");
            }
            // Ensure the actual class is fully hydrated before copying its state.
            actualClass.ensureHydrated();
            this.actualClass = actualClass;
            // Identity and Hierarchy synchronization
            this.hostClass = actualClass.hostClass; // Synchronize hostClass
            this.name = actualClass.name;
            this.superclass = actualClass.superclass;
            // Structural state synchronization
            this.totalFieldCount = actualClass.totalFieldCount;
            this.defaultFieldValues = actualClass.defaultFieldValues;
            this.flattenedFieldNames = actualClass.flattenedFieldNames;
            // Synchronize meta-properties
            DynamicObjectLibrary metaLib = DynamicObjectLibrary.getUncached();
            for (String key : actualClass.getMetaPropertyKeys()) {
                metaLib.put(this, key, metaLib.getOrDefault(actualClass, key, JolkNothing.INSTANCE));
            }
            // Registry and Cache synchronization
            this.instanceRegistry = actualClass.instanceRegistry;
            this.metaRegistry = actualClass.metaRegistry;
            this.cachedInstanceMemberNames = actualClass.cachedInstanceMemberNames;
            this.cachedMetaMemberNames = actualClass.cachedMetaMemberNames;
            this.hydrated = true;
        }

        @Override
        public boolean isHydrated() {
            return actualClass != null && actualClass.isHydrated();
        }

        @Override
        public boolean isReady() {
            return actualClass != null;
        }

        @Override
        public boolean isFieldStable(String fieldName) {
            return actualClass != null ? actualClass.isFieldStable(fieldName) : super.isFieldStable(fieldName);
        }

        @Override
        public int getFieldIndex(String name) {
            return actualClass != null ? actualClass.getFieldIndex(name) : super.getFieldIndex(name);
        }

        @Override
        public int getFieldCount() {
            return actualClass != null ? actualClass.getFieldCount() : super.getFieldCount();
        }

        @Override
        public String[] getFlattenedFieldNames() {
            return actualClass != null ? actualClass.getFlattenedFieldNames() : super.getFlattenedFieldNames();
        }

        @Override
        public String[] getInstanceFieldNames() {
            return actualClass != null ? actualClass.getInstanceFieldNames() : super.getInstanceFieldNames();
        }

        @Override
        public Object[] getDefaultFieldValues() {
            return actualClass != null ? actualClass.getDefaultFieldValues() : super.getDefaultFieldValues();
        }

        @Override
        public boolean hasInstanceMember(String name) {
            return actualClass != null ? actualClass.hasInstanceMember(name) : super.hasInstanceMember(name);
        }

        @Override
        public Object lookupInstanceMember(String name) {
            return actualClass != null ? actualClass.lookupInstanceMember(name) : super.lookupInstanceMember(name);
        }

        @Override
        public Object lookupMetaMember(String name) {
            return actualClass != null ? actualClass.lookupMetaMember(name) : super.lookupMetaMember(name);
        }

        @Override
        public JolkMemberNames getInstanceMemberNames() {
            return actualClass != null ? actualClass.getInstanceMemberNames() : super.getInstanceMemberNames();
        }

        @Override
        public Set<String> getMetaPropertyKeys() {
            return actualClass != null ? actualClass.getMetaPropertyKeys() : super.getMetaPropertyKeys();
        }

        @Override
        public JolkMetaClass getSuperclass() {
            return actualClass != null ? actualClass.getSuperclass() : super.getSuperclass();
        }

        @Override
        public Shape getInstanceShape() {
            return actualClass != null ? actualClass.getInstanceShape() : super.getInstanceShape();
        }

        @Override
        protected synchronized void ensureHydrated() {
            if (actualClass != null) {
                actualClass.ensureHydrated();
            }
        }

        @Override
        public JolkMetaClass getJolkMetaClass() {
            return actualClass != null ? actualClass.getJolkMetaClass() : this;
        }
    }
}
