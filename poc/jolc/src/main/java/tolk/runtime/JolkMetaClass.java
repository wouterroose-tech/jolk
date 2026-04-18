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
import tolk.nodes.JolkReturnException;

import java.util.HashSet;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/// # JolkMetaClass (Meta-Object Descriptor)
/// 
/// Represents a Jolk Type (a meta-object) at runtime. It acts as the first-class identity 
/// for Classes, Records, and Enums.
/// 
/// As the primary engine for **Dual-Stratum Resolution**, it manages the **Consolidated 
/// Flattened Registry**. It facilitates **Virtual Static Dispatch**, enabling class-level 
/// behaviors to participate in a rigorous inheritance model, effectively treating every 
/// Type as a live Identity within the **JoMoo** continuum.
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
    private final Map<String, Object> instanceFields;
    // Map field names to their array index for O(1) access
    private final Map<String, Integer> fieldIndices;
    // The Truffle Shape defining the memory layout for instances
    private Shape instanceShape;
    // Total number of fields including hierarchy
    protected int totalFieldCount;
    // Default field values
    protected Object[] defaultFieldValues;
    // Meta members (methods and field accessors).
    private final Map<String, Object> metaMembers;
    private final Map<String, Object> metaFields;

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

    public JolkMetaClass(String name, JolkFinality finality, JolkVisibility visibility, JolkArchetype archetype, Map<String, Object> instanceMembers) {
        this(name, null, finality, visibility, archetype, instanceMembers, new HashMap<>(), new HashMap<>(), new HashMap<>());
    }

    public JolkMetaClass(String name, JolkFinality finality, JolkVisibility visibility, JolkArchetype archetype, Map<String, Object> instanceMembers, Map<String, Object> metaMembers) {
        this(name, null, finality, visibility, archetype, instanceMembers, new HashMap<>(), metaMembers, new HashMap<>());
    }

    public JolkMetaClass(String name, JolkMetaClass superclass, JolkFinality finality, JolkVisibility visibility, JolkArchetype archetype, Map<String, Object> instanceMembers, Map<String, Object> metaMembers) {
        this(name, superclass, finality, visibility, archetype, instanceMembers, new HashMap<>(), metaMembers, new HashMap<>());
    }

    // Convenience constructor for unit tests providing instance field templates
    public JolkMetaClass(String name, JolkMetaClass superclass, JolkFinality finality, JolkVisibility visibility, JolkArchetype archetype, Map<String, Object> instanceMembers, Map<String, Object> instanceFields, Map<String, Object> metaMembers) {
        this(name, superclass, finality, visibility, archetype, instanceMembers, instanceFields, metaMembers, new HashMap<>());
    }

    public JolkMetaClass(String name, JolkMetaClass superclass, JolkFinality finality, JolkVisibility visibility, JolkArchetype archetype, Map<String, Object> instanceMembers, Map<String, Object> instanceFields, Map<String, Object> metaMembers, Map<String, Object> metaFields) {
        super(ROOT_META_SHAPE);
        this.name = name;
        this.superclass = superclass;
        this.finality = finality;
        this.visibility = visibility;
        this.archetype = archetype;
        this.instanceMembers = instanceMembers;
        this.instanceFields = new LinkedHashMap<>(instanceFields); // Preserve declaration order
        this.fieldIndices = new HashMap<>();
        this.metaFields = metaFields; // Maintain reference to allow deferred hydration
        this.metaMembers = metaMembers;
    }

    /// Performs **Late Flattening**.
    ///
    /// Registries and field layouts are calculated on-demand. This solves forward
    /// references because a subclass can be created before its superclass is fully
    /// defined, as long as both are defined before the first message is sent.
    protected synchronized void ensureHydrated() {
        if (hydrated) return;
        // If this is a placeholder, its hydration will be handled by updatePlaceholder.
        if (this instanceof JolkMetaClassPlaceholder) {
            return;
        }
        // Ensure superclass is fully hydrated before accessing its members or registries.
        if (superclass != null) {
            superclass.ensureHydrated();
        }

        // 1. Calculate Instance Field Layout
        int superCount = (superclass != null) ? superclass.getFieldCount() : 0;
        this.flattenedFieldNames = new String[superCount + instanceFields.size()];
        if (this.instanceShape == null) {
            this.instanceShape = (superclass != null) ? superclass.instanceShape : Shape.newBuilder().build();
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

    public static boolean isBooleanHint(String hint) {
        if (hint == null) return false;
        return hint.equalsIgnoreCase("Boolean") 
            || hint.equalsIgnoreCase("jolk.lang.Boolean");
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
    public Object getMetaSimpleName() {
        return name;
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
            if (isLongHint(this.name)) return true;
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
        return false;
    }

    /// ### isMemberReadable
    ///
    /// Returns true if the identifier exists in the meta-member map.
    @ExportMessage
    public boolean isMemberReadable(String member,
                                    @Exclusive @CachedLibrary("this") DynamicObjectLibrary objLib) {
        ensureHydrated();
        if (metaRegistry.containsKey(member)) return true;
        if (objLib.containsKey(this, member)) return true;
        // For ENUM types, check enumConstants directly as a fallback
        if (archetype == JolkArchetype.ENUM && enumConstants.containsKey(member)) return true;
        return false;
    }

    /// ### readMember
    ///
    /// Retrieves the internal substrate object (Closure or Accessor) for a meta-member.
    @ExportMessage
    public Object readMember(String member,
                             @Exclusive @CachedLibrary("this") DynamicObjectLibrary objLib) throws UnknownIdentifierException {
        ensureHydrated();
        Object val = metaRegistry.get(member);
        if (val == null) {
            val = objLib.getOrDefault(this, member, null);
        }
        if (val == null && archetype == JolkArchetype.ENUM) {
            // Fallback for enum constants in case they aren't in metaRegistry
            val = enumConstants.get(member);
        }
        if (val == null) {
            throw UnknownIdentifierException.create(member);
        }
        return val;
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
        // 1. Consolidated Meta-Members (Methods and Field Accessors)
        if (metaRegistry.containsKey(member)) return true;
        if (objLib.containsKey(this, member)) return true;
        
        // 2. Local Meta-Intrinsics (Identity properties should not delegate)
        if (switch (member) {
            case "new", "name", "superclass", "isInstance" -> true;
            default -> false;
        }) return true;

        if (isObjectIntrinsic(member)) return true;
        return false;
    }

    /// ### callMetaMember
    ///
    /// Convenience method for calling meta-level messages from Java code or unit tests.
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
            // Jolk Protocol: Every method call must include the receiver as the first argument.
            Object[] metaArguments = new Object[arguments.length + 1];
            metaArguments[0] = this;
            if (arguments.length > 0) System.arraycopy(arguments, 0, metaArguments, 1, arguments.length);
            try {
                return interop.execute(memberObj, metaArguments);
            } catch (JolkReturnException e) {
                throw e; // Preserve signal for non-local returns
            }
        }

        // Fallback for non-cached members
        if (metaRegistry.containsKey(member)) {
            Object memberObj = metaRegistry.get(member);
            if (memberObj instanceof JolkEnumConstant) {
                if (arguments.length != 0) throw ArityException.create(0, 0, arguments.length);
                return memberObj;
            }
            Object[] metaArguments = new Object[arguments.length + 1];
            metaArguments[0] = this;
            if (arguments.length > 0) System.arraycopy(arguments, 0, metaArguments, 1, arguments.length);
            try {
                return interop.execute(memberObj, metaArguments);
            } catch (JolkReturnException e) {
                throw e;
            }
        }

        // 2. Property Projection for Meta-Fields (The "Collapse")
        if (objLib.containsKey(this, member)) {
            if (arguments.length == 0) {
                // Getter Pattern: Type #field
                return objLib.getOrDefault(this, member, JolkNothing.INSTANCE);
            } else if (arguments.length == 1) {
                // Setter Pattern: Type #field(val) -> Returns Self (Fluent Contract)
                objLib.put(this, member, arguments[0]);
                return this;
            }
            throw ArityException.create(0, 1, arguments.length);
        }

        // 2. Local Meta-Intrinsics: Identity properties specific to this class
        switch (member) {
            case "new":
                // For enums, new is not allowed - enum constants are accessed directly
                if (archetype == JolkArchetype.ENUM) {
                    throw UnsupportedMessageException.create();
                }
                // Canonical #new logic: Only applies if no explicit meta-method named 'new' exists.
                // This allows the variadic 'meta Map new(Object...)' to take precedence.
                if (arguments.length == 0) return new JolkObject(this);
                
                if (arguments.length == totalFieldCount) {
                    return new JolkObject(this, arguments);
                }
                throw ArityException.create(totalFieldCount, totalFieldCount, arguments.length);
            case "name":
                if (arguments.length != 0) throw ArityException.create(0, 0, arguments.length);
                return name;
            case "superclass":
                if (arguments.length != 0) throw ArityException.create(0, 0, arguments.length);
                return superclass != null ? superclass : JolkNothing.INSTANCE;
            case "isInstance":
                if (arguments.length != 1) throw ArityException.create(1, 1, arguments.length);
                return isMetaInstance(arguments[0]);
        }

        // 2. Jolk Object Protocol: Classes are polite JoMoos
        Object intrinsicResult = JolkIntrinsicProtocol.dispatchObjectIntrinsic(this, member, arguments, interop);
        if (intrinsicResult != null) {
            return intrinsicResult;
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

    public static boolean isObjectIntrinsic(String member) {
        return JolkIntrinsicProtocol.isObjectIntrinsic(member);
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
        return instanceRegistry.containsKey(name);
    }

    /**
     * ## lookupInstanceMember
     *
     * Looks up an instance member (method or field accessor) by name.
     * This is O(1) and non-recursive due to hierarchy flattening.
     */
    public Object lookupInstanceMember(String name) {
        ensureHydrated();
        return instanceRegistry.get(name);
    }

    /**
     * ## lookupMetaMember
     *
     * Looks up a meta-level member (e.g., a built-in factory method like #new) 
     * by name. This is used by the dispatch system to resolve messages 
     * sent to the MetaClass itself.
     */
    public Object lookupMetaMember(String name) {
        ensureHydrated();
        return metaRegistry.get(name);
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
        metaKeys.add("superclass");
        metaKeys.add("isInstance");
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
                  new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>());
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
        protected synchronized void ensureHydrated() {
            // No-op: The placeholder's state is strictly managed via updatePlaceholder.
        }

        @Override
        public JolkMetaClass getJolkMetaClass() {
            return actualClass != null ? actualClass.getJolkMetaClass() : this;
        }
    }
}
