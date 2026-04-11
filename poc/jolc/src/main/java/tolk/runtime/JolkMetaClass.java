package tolk.runtime;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import tolk.nodes.JolkReturnException;

import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/// # JolkMetaClass (Meta-Object Descriptor)
/// 
/// Represents a Jolk Type (a meta-object) at runtime.
///
/// This is the Java implementation of the `MetaClass` concept defined in Jolk.
/// It acts as the first-class identity for Classes, Records, and Enums. As a
/// meta-object, it is responsible for handling meta-level messages such as
/// `#new` (instance creation) and type introspection (`#name`, `#superclass`).
/// 
/// It also serves as a container for the definitions of members (fields and methods)
/// that belong to instances of this type. However, it does not execute instance-level
/// messages itself; that is the responsibility of {@link JolkObject}.
/// 
///  @author Wouter Roose

@ExportLibrary(InteropLibrary.class)
public class JolkMetaClass implements TruffleObject {

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
    // Cache synthetic accessors to prevent allocation on every lookup
    private final Map<String, JolkSyntheticAccessor> accessorCache;
    // Total number of fields including hierarchy
    protected int totalFieldCount;
    // Default field values
    protected Object[] defaultFieldValues;
    // Meta members (methods and field accessors).
    private final Map<String, Object> metaMembers;
    private final Map<String, Object> metaFields;
    // Meta-level field storage
    protected int totalMetaFieldCount;
    private final Map<String, Integer> metaFieldIndices;
    protected Object[] metaFieldValues;
    private final Map<String, JolkMetaFieldAccessor> metaAccessorCache;

    // Optimized consolidated registries
    protected Map<String, Object> instanceRegistry;
    protected Map<String, Object> metaRegistry;
    protected JolkMemberNames cachedInstanceMemberNames;
    protected JolkMemberNames cachedMetaMemberNames;
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
        this.name = name;
        this.superclass = superclass;
        this.finality = finality;
        this.visibility = visibility;
        this.archetype = archetype;
        this.instanceMembers = instanceMembers;
        this.instanceFields = instanceFields;
        this.metaFields = metaFields; // Maintain reference to allow deferred hydration
        this.fieldIndices = new HashMap<>();
        this.accessorCache = new HashMap<>();
        this.metaMembers = metaMembers;
        this.metaFieldIndices = new HashMap<>();
        this.metaAccessorCache = new HashMap<>();
    }

    /**
     * Performs **Late Flattening**.
     * 
     * Registries and field layouts are calculated on-demand. This solves forward 
     * references because a subclass can be created before its superclass is fully 
     * defined, as long as both are defined before the first message is sent.
     */
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
        int currentIndex = (superclass != null) ? superclass.getFieldCount() : 0;
        for (String fieldName : instanceFields.keySet()) {
            fieldIndices.put(fieldName, currentIndex);
            accessorCache.put(fieldName, new JolkSyntheticAccessor(fieldName, currentIndex));
            currentIndex++;
        }
        this.totalFieldCount = currentIndex;
        this.defaultFieldValues = new Object[totalFieldCount];

        // 2. Calculate Meta Field Layout
        int currentMetaIndex = (superclass != null) ? superclass.getMetaFieldCount() : 0;
        for (String fieldName : metaFields.keySet()) {
            metaFieldIndices.put(fieldName, currentMetaIndex++);
            if (this.metaMembers.get(fieldName) == null || this.metaMembers.get(fieldName) == JolkNothing.INSTANCE) {
                this.metaMembers.put(fieldName, getMetaAccessor(fieldName, false));
            }
        }
        this.totalMetaFieldCount = currentMetaIndex;
        this.metaFieldValues = new Object[totalMetaFieldCount];

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
        for (String fieldName : this.instanceFields.keySet()) this.instanceRegistry.put(fieldName, accessorCache.get(fieldName));
        this.instanceRegistry.putAll(this.instanceMembers);
        this.metaRegistry.putAll(this.metaMembers);

        this.hydrated = true;
        syncDefaultValues(); // Now safe to call as it's not a placeholder
        refreshInstanceMemberCache();
        refreshMetaMemberCache();
    }

    /**
     * ### registerInstanceMethod
     * 
     * Registers a Java-implemented method for instances of this type.
     */
    public void registerInstanceMethod(String name, Object method) {
        this.instanceMembers.put(name, method);
        if (!hydrated) return;
        // Ensure the consolidated registry and interop caches stay in sync
        this.instanceRegistry.put(name, method);
        refreshInstanceMemberCache();
    }

    /**
     * ### registerMetaMethod
     * 
     * Registers a Java-implemented method for the type itself (the MetaClass) and refreshes caches.
     */
    public void registerMetaMethod(String name, Object method) {
        this.metaMembers.put(name, method);
        if (!hydrated) return;
        this.metaRegistry.put(name, method);
        refreshMetaMemberCache();
    }

    /**
     * ### registerEnumConstant
     *
     * Registers an enum constant for ENUM archetype types.
     */
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

    /**
     * ### getJolkMetaClass
     * 
     * Returns the meta-identity of this object. In the Jolk Meta-Object Protocol,
     * classes (MetaClasses) act as their own meta-identities.
     * 
     * @return This MetaClass instance.
     */
    public JolkMetaClass getJolkMetaClass() {
        return this; // In the PoC, classes are their own meta-identities.
    }

    public JolkMetaClass getSuperclass() {
        return superclass;
    }

    public Object getMetaFieldValue(int index) {
        ensureHydrated();
        return metaFieldValues[index];
    }

    public void setMetaFieldValue(int index, Object value) {
        ensureHydrated();
        metaFieldValues[index] = value;
        if (!hydrated) return; // If still not hydrated (e.g., placeholder), do nothing.
    }

    public void setMetaFieldValue(String name, Object value) {
        ensureHydrated();
        Integer idx = metaFieldIndices.get(name);
        if (idx != null) metaFieldValues[idx] = value;
    }

    public Object getMetaAccessor(String name, boolean isStable) {
        JolkMetaFieldAccessor accessor = metaAccessorCache.get(name);
        if (accessor == null || accessor.isStable != isStable) {
            accessor = new JolkMetaFieldAccessor(name, metaFieldIndices.get(name), isStable);
            metaAccessorCache.put(name, accessor);
        }
        return accessor;
    }

    /**
     * ### initializeDefaultValues
     * 
     * Re-scans the instanceFields map to establish the template values for new 
     * instances. This is called after the hydration phase in the definition node.
     */
    /**
     * ### initializeDefaultValues
     * 
     * Public entry point used by the definition node to synchronize structural 
     * templates. In the Late Flattening model, this triggers full hydration 
     * to ensure indices and registries are prepared for use.
     */
    public void initializeDefaultValues() {
        ensureHydrated();
    }

    private void syncDefaultValues() {
        if (superclass != null) {
            // Ensure superclass is hydrated so its default arrays are allocated.
            superclass.ensureHydrated();
            if (superclass.defaultFieldValues != null) {
                System.arraycopy(superclass.defaultFieldValues, 0, this.defaultFieldValues, 0, superclass.totalFieldCount);
            }
            // Synchronize inherited meta-fields
            if (superclass.metaFieldValues != null) {
                System.arraycopy(superclass.metaFieldValues, 0, this.metaFieldValues, 0, superclass.totalMetaFieldCount);
            }
        }
        for (Map.Entry<String, Object> entry : instanceFields.entrySet()) {
            Integer idx = fieldIndices.get(entry.getKey());
            if (idx != null) {
                Object val = entry.getValue();
                // Robust Hint Detection: Resolve the type name from MetaClasses, Strings, or Host Classes.
                String hint = resolveTypeHint(val);

                if (isLongHint(hint) || ((val == null || val == JolkNothing.INSTANCE) && entry.getKey().equalsIgnoreCase("id"))) {
                    this.defaultFieldValues[idx] = 0L;
                } else if (isBooleanHint(hint)) {
                    this.defaultFieldValues[idx] = false;
                } else if (isStringHint(hint)) {
                    this.defaultFieldValues[idx] = "";
                } else if (val instanceof JolkMetaClass || val == null || val instanceof String) {
                    // Other classes (including sentinels for non-intrinsics) or explicit nulls default to Nothing.
                    this.defaultFieldValues[idx] = JolkNothing.INSTANCE;
                } else {
                    this.defaultFieldValues[idx] = val;
                }
            }
        }

        // Meta-Level Primary Initializer Expansion
        for (Map.Entry<String, Object> entry : metaFields.entrySet()) {
            Integer idx = metaFieldIndices.get(entry.getKey());
            if (idx != null) {
                Object val = entry.getValue();

                // If the value is a JolkMetaClass or a String, it's a type hint,
                // so we apply default initialization based on the hint.
                // Otherwise, it's a concrete initializer value.
                if (val instanceof JolkMetaClass || val instanceof String) {
                    String hint = resolveTypeHint(val);
                    if (isLongHint(hint) || ((val == null || val == JolkNothing.INSTANCE) && entry.getKey().equalsIgnoreCase("id"))) {
                        this.metaFieldValues[idx] = 0L;
                    } else if (isBooleanHint(hint)) {
                        this.metaFieldValues[idx] = false;
                    } else if (isStringHint(hint)) {
                        this.metaFieldValues[idx] = "";
                    } else {
                        this.metaFieldValues[idx] = JolkNothing.INSTANCE;
                    }
                } else {
                    this.metaFieldValues[idx] = val == null ? JolkNothing.INSTANCE : val; // Use the provided concrete value
                }
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

    /**
     * ### isMemberReadable
     * 
     * Returns true if the identifier exists in the meta-member map.
     */
    @ExportMessage
    public boolean isMemberReadable(String member) {
        ensureHydrated();
        if (metaRegistry.containsKey(member)) return true;
        // For ENUM types, check enumConstants directly as a fallback
        if (archetype == JolkArchetype.ENUM && enumConstants.containsKey(member)) return true;
        return false;
    }

    /**
     * ### readMember
     * 
     * Retrieves the internal substrate object (Closure or Accessor) for a meta-member.
     */
    @ExportMessage
    public Object readMember(String member) throws UnknownIdentifierException {
        ensureHydrated();
        Object val = metaRegistry.get(member);
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
    public boolean isMemberInvocable(String member) {
        ensureHydrated();
        // 1. Consolidated Meta-Members (Methods and Field Accessors)
        if (metaRegistry.containsKey(member)) return true;
        
        // 2. Local Meta-Intrinsics (Identity properties should not delegate)
        if (switch (member) {
            case "new", "name", "superclass", "isInstance" -> true;
            default -> false;
        }) return true;

        if (isObjectIntrinsic(member)) return true;
        return false;
    }

    /**
     * ### callMetaMember
     * 
     * Convenience method for calling meta-level messages from Java code or unit tests.
     */
    @TruffleBoundary
    public Object callMetaMember(String member, Object[] arguments) throws UnknownIdentifierException, ArityException, UnsupportedTypeException, UnsupportedMessageException {
        return invokeMember(member, arguments, InteropLibrary.getUncached(), member, lookupMetaMember(member));
    }

    @TruffleBoundary
    static Object doLookupMeta(JolkMetaClass receiver, String member) {
        return receiver.lookupMetaMember(member);
    }

    @ExportMessage
    public Object invokeMember(String member, Object[] arguments,
                        @CachedLibrary(limit = "3") InteropLibrary interop,
                        @Cached(value = "member", allowUncached = true) String cachedMember,
                        @Cached(value = "doLookupMeta(this, member)", allowUncached = true) Object cachedValue) throws UnknownIdentifierException, ArityException, UnsupportedTypeException, UnsupportedMessageException {
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

    public int getMetaFieldCount() {
        ensureHydrated();
        return totalMetaFieldCount;
    }

    private void refreshInstanceMemberCache() {
        if (!hydrated) return; // Only refresh if hydrated
        Set<String> instanceKeys = new HashSet<>(instanceRegistry.keySet());
        instanceKeys.addAll(JolkIntrinsicProtocol.INTRINSIC_MEMBERS);
        this.cachedInstanceMemberNames = new JolkMemberNames(instanceKeys.toArray(new String[0]));
    }

    private void refreshMetaMemberCache() {
        if (!hydrated) return; // Only refresh if hydrated
        Set<String> metaKeys = new HashSet<>(this.metaRegistry.keySet());
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

    ///
    /// ## JolkSyntheticAccessor
    ///
    /// Implements the "Virtual Field Strategy" by acting as a dynamic accessor node.
    /// It handles both read (getter) and write (setter) operations based on the
    /// number of arguments provided, supporting the Jolk fluent setter pattern.
    ///
    @ExportLibrary(InteropLibrary.class)
    public static final class JolkSyntheticAccessor implements TruffleObject {
        private final String fieldName;
        private final int index;

        public JolkSyntheticAccessor(String fieldName, int index) {
            this.fieldName = fieldName;
            this.index = index;
        }

        @ExportMessage
        public boolean isExecutable() {
            return true;
        }
    
        @ExportMessage
        public Object execute(Object[] arguments,
                       @Cached(value = "create()", inline = false) ConditionProfile lengthProfile) throws ArityException, UnsupportedTypeException {
            
            if (arguments.length < 1) throw ArityException.create(1, 2, arguments.length);
            
            // Argument 0 is the receiver (JolkObject)
            if (!(arguments[0] instanceof JolkObject receiver)) {
                throw UnsupportedTypeException.create(arguments, "Receiver must be a JolkObject");
            }

            if (lengthProfile.profile(arguments.length == 1)) {
                // Getter: #x -> returns value
                Object result = receiver.getFieldValue(index);
                // Identity Restitution Protocol: Ensure no raw JVM null or Interop null leaks.
                return (result == null || (result != JolkNothing.INSTANCE && InteropLibrary.getUncached().isNull(result))) ? JolkNothing.INSTANCE : result;
            } else if (lengthProfile.profile(arguments.length >= 2)) {
                // Setter: #x(value) -> returns Self (for fluent chaining)
                Object value = arguments[1];
                receiver.setFieldValue(index, value);
                return receiver;
            }
            throw ArityException.create(1, 2, arguments.length);
        }
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
            this.totalMetaFieldCount = actualClass.totalMetaFieldCount;
            this.metaFieldValues = actualClass.metaFieldValues;
            
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
