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
import com.oracle.truffle.api.profiles.ConditionProfile;

import tolk.nodes.JolkReturnException;

import java.util.HashSet;
import java.util.HashMap;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/// # JolkMetaClass
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
@ExportLibrary(InteropLibrary.class)
public final class JolkMetaClass implements TruffleObject {

    public final String name;
    private final JolkMetaClass superclass;
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
    private final int totalFieldCount;
    // Default field values
    private final Object[] defaultFieldValues;
    // Meta members (methods and field accessors).
    private final Map<String, Object> metaMembers;
    // Meta-level field storage
    private final Map<String, Integer> metaFieldIndices;
    private final Object[] metaFieldValues;
    private final Map<String, JolkMetaFieldAccessor> metaAccessorCache;

    public JolkMetaClass(String name, JolkFinality finality, JolkVisibility visibility, JolkArchetype archetype, Map<String, Object> instanceMembers) {
        this(name, null, finality, visibility, archetype, instanceMembers, Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap());
    }

    public JolkMetaClass(String name, JolkFinality finality, JolkVisibility visibility, JolkArchetype archetype, Map<String, Object> instanceMembers, Map<String, Object> metaMembers) {
        this(name, null, finality, visibility, archetype, instanceMembers, Collections.emptyMap(), metaMembers, Collections.emptyMap());
    }

    public JolkMetaClass(String name, JolkMetaClass superclass, JolkFinality finality, JolkVisibility visibility, JolkArchetype archetype, Map<String, Object> instanceMembers, Map<String, Object> metaMembers) {
        this(name, superclass, finality, visibility, archetype, instanceMembers, Collections.emptyMap(), metaMembers, Collections.emptyMap());
    }

    /**
     * Convenience constructor for unit tests providing instance field templates.
     */
    public JolkMetaClass(String name, JolkMetaClass superclass, JolkFinality finality, JolkVisibility visibility, JolkArchetype archetype, Map<String, Object> instanceMembers, Map<String, Object> instanceFields, Map<String, Object> metaMembers) {
        this(name, superclass, finality, visibility, archetype, instanceMembers, instanceFields, metaMembers, Collections.emptyMap());
    }

    public JolkMetaClass(String name, JolkMetaClass superclass, JolkFinality finality, JolkVisibility visibility, JolkArchetype archetype, Map<String, Object> instanceMembers, Map<String, Object> instanceFields, Map<String, Object> metaMembers, Map<String, Object> metaFields) {
        this.name = name;
        this.superclass = superclass;
        this.finality = finality;
        this.visibility = visibility;
        this.archetype = archetype;
        this.instanceMembers = instanceMembers;
        this.instanceFields = instanceFields;
        this.fieldIndices = new HashMap<>();
        this.accessorCache = new HashMap<>();
        
        // Calculate field layout: Start after superclass fields to support inheritance
        int currentIndex = (superclass != null) ? superclass.getFieldCount() : 0;
        for (String fieldName : instanceFields.keySet()) {
            fieldIndices.put(fieldName, currentIndex);
            accessorCache.put(fieldName, new JolkSyntheticAccessor(fieldName, currentIndex));
            currentIndex++;
        }
        this.totalFieldCount = currentIndex;
        
        this.defaultFieldValues = new Object[totalFieldCount];
        this.metaMembers = metaMembers;
        this.metaFieldIndices = new HashMap<>();
        this.metaAccessorCache = new HashMap<>();
        
        int mIdx = 0;
        for (String fieldName : metaFields.keySet()) {
            metaFieldIndices.put(fieldName, mIdx++);
        }
        this.metaFieldValues = new Object[mIdx];

        initializeDefaultValues();
    }
    public Object getMetaFieldValue(int index) {
        return metaFieldValues[index];
    }

    public void setMetaFieldValue(int index, Object value) {
        metaFieldValues[index] = value;
    }

    public void setMetaFieldValue(String name, Object value) {
        Integer idx = metaFieldIndices.get(name);
        if (idx != null) metaFieldValues[idx] = value;
    }

    public Object getMetaAccessor(String name, boolean isStable) {
        return metaAccessorCache.computeIfAbsent(name, k -> new JolkMetaFieldAccessor(k, metaFieldIndices.get(k), isStable));
    }

    @ExportLibrary(InteropLibrary.class)
    public static final class JolkMetaFieldAccessor implements TruffleObject {
        private final String fieldName;
        private final int index;
        private final boolean isStable;

        public JolkMetaFieldAccessor(String fieldName, int index, boolean isStable) {
            this.fieldName = fieldName;
            this.index = index;
            this.isStable = isStable;
        }

        @ExportMessage
        public boolean isExecutable() { return true; }

        @ExportMessage
        public Object execute(Object[] arguments) throws ArityException, UnsupportedTypeException {
            if (arguments.length < 1) throw ArityException.create(1, 2, arguments.length);
            
            if (!(arguments[0] instanceof JolkMetaClass receiver)) {
                throw UnsupportedTypeException.create(arguments, "Receiver must be a JolkMetaClass");
            }

            if (arguments.length == 1) {
                Object result = receiver.getMetaFieldValue(index);
                return result == null ? JolkNothing.INSTANCE : result;
            } else {
                if (isStable) {
                    throw UnsupportedTypeException.create(arguments, "Cannot modify stable/constant meta field: " + fieldName);
                }
                Object value = arguments[1];
                receiver.setMetaFieldValue(index, value);
                return receiver;
            }
        }
    }

    /**
     * ### initializeDefaultValues
     * 
     * Re-scans the instanceFields map to establish the template values for new 
     * instances. This is called after the hydration phase in the definition node.
     */
    public void initializeDefaultValues() {
        if (superclass != null) {
            System.arraycopy(superclass.defaultFieldValues, 0, this.defaultFieldValues, 0, superclass.totalFieldCount);
        }
        for (Map.Entry<String, Object> entry : instanceFields.entrySet()) {
            Integer idx = fieldIndices.get(entry.getKey());
            if (idx != null) {
                Object val = entry.getValue();
                // Robust Hint Detection: Resolve the type name from MetaClasses, Strings, or Host Classes.
                String hint = resolveTypeHint(val);

                if (isLongHint(hint) || (val == null && entry.getKey().equalsIgnoreCase("id"))) {
                    this.defaultFieldValues[idx] = 0L;
                } else if (isBooleanHint(hint)) {
                    this.defaultFieldValues[idx] = false;
                } else if (val instanceof JolkMetaClass || val == null || val instanceof String) {
                    // Other classes (including sentinels for non-intrinsics) or explicit nulls default to Nothing.
                    this.defaultFieldValues[idx] = JolkNothing.INSTANCE;
                } else {
                    this.defaultFieldValues[idx] = val;
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

        // 4. Handle standard JolkObjects by walking their class hierarchy.
        if (instance instanceof JolkObject jolkObject) {
            JolkMetaClass current = jolkObject.getJolkMetaClass();
            while (current != null) {
                if (current == this) return true;
                current = current.superclass;
            }
        }

        // 5. Root Object Fallback: JolkObjects, Nothing, and primitives are instances of Object.
        // Note: During message dispatch, specialized MetaClasses (Boolean/Long) must be 
        // registered before the root Object archetype to avoid shadowing specialized members.
        if (this.superclass == null && "Object".equals(this.name)) {
            return instance instanceof JolkObject || instance == JolkNothing.INSTANCE || 
                   interop.isNumber(instance) || interop.isBoolean(instance);
        }

        // 6. For the PoC, other Java objects (e.g., String) are not considered
        // instances of any Jolk type to ensure runtime safety.
        return false;
    }

    @ExportMessage
    public boolean hasMembers() {
        return true;
    }

    @ExportMessage
    public Object getMembers(boolean includeInternal) throws UnsupportedMessageException {
        // This returns the members of the META-OBJECT, not the instance.
        Set<String> keys = new HashSet<>(metaMembers.keySet());
        keys.add("new");
        keys.add("name");
        keys.add("superclass");
        keys.add("isInstance");
        return new JolkMemberNames(keys.toArray(new String[0]));
    }

    @ExportMessage
    public boolean isMemberReadable(String member) {
        return metaMembers.containsKey(member);
    }

    @ExportMessage
    public boolean isMemberInvocable(String member) {
        // This checks if a message can be sent TO THE META-OBJECT itself.
        String name = member;
        return metaMembers.containsKey(name) || switch (name) {
            case "new", "name", "superclass", "isInstance" -> true;
            default -> false;
        };
    }

    @ExportMessage
    public Object invokeMember(String member, Object[] arguments) throws UnknownIdentifierException, ArityException, UnsupportedTypeException, UnsupportedMessageException {
        String name = member;
        if (metaMembers.containsKey(name)) {
            Object memberObj = metaMembers.get(name);
            // Jolk Protocol: Every method call must include the receiver as the first argument.
            Object[] metaArguments = new Object[arguments.length + 1];
            metaArguments[0] = this;
            if (arguments.length > 0) System.arraycopy(arguments, 0, metaArguments, 1, arguments.length);
            try {
                return InteropLibrary.getUncached().execute(memberObj, metaArguments);
            } catch (JolkReturnException e) {
                throw e; // Preserve signal for non-local returns
            }
        }
        switch (name) {
            case "new":
                if (arguments.length != 0) {
                    // Support Canonical #new if arguments match field count
                    if (arguments.length == totalFieldCount) {
                        return new JolkObject(this, arguments);
                    }
                    throw ArityException.create(totalFieldCount, totalFieldCount, arguments.length);
                }
                return new JolkObject(this);
            case "name":
                if (arguments.length != 0) throw ArityException.create(0, 0, arguments.length);
                return name;
            case "superclass":
                if (arguments.length != 0) throw ArityException.create(0, 0, arguments.length);
                return superclass != null ? superclass : JolkNothing.INSTANCE;
            case "isInstance":
                if (arguments.length != 1) throw ArityException.create(1, 1, arguments.length);
                return isMetaInstance(arguments[0]);
            default:
                throw UnknownIdentifierException.create(member);
        }
    }

    @ExportMessage
    public Object readMember(String member) throws UnknownIdentifierException {
        String name = member;
        if (metaMembers.containsKey(name)) {
            return metaMembers.get(name);
        }
        throw UnknownIdentifierException.create(member);
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
        String sanitized = name;
        return instanceMembers.containsKey(sanitized) || instanceFields.containsKey(sanitized);
    }

    /**
     * ## lookupInstanceMember
     *
     * Looks up an instance member (e.g., a `JolkClosure`) by name.
     */
    public Object lookupInstanceMember(String name) {
        String sanitized = name;
        Object member = instanceMembers.get(sanitized);
        if (member != null) {
            // If the parser erroneously puts the field definition in instanceMembers,
            // we detect the duplicate and prefer the synthetic accessor.
            if (instanceFields.containsKey(sanitized) && instanceFields.get(sanitized) == member) {
                return accessorCache.get(sanitized);
            }
            return member;
        }
        // Virtual Field Strategy: Hydrate a specialized node if the field exists.
        if (instanceFields.containsKey(sanitized)) {
            return accessorCache.get(sanitized);
        }
        // Recurse up the hierarchy
        if (superclass != null) {
            return superclass.lookupInstanceMember(sanitized);
        }
        return null;
    }

    /**
     * ## getInstanceMemberKeys
     *
     * @return A set of all instance member names.
     */
    public Set<String> getInstanceMemberKeys() {
        Set<String> keys = new HashSet<>(instanceMembers.keySet());
        keys.addAll(instanceFields.keySet());
        return keys;
    }

    public int getFieldCount() {
        return totalFieldCount;
    }

    public int getFieldIndex(String name) {
        Integer index = fieldIndices.get(name);
        if (index != null) return index;
        if (superclass != null) return superclass.getFieldIndex(name);
        return -1;
    }

    Object[] getDefaultFieldValues() {
        return defaultFieldValues;
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
                       @Cached("createBinaryProfile()") ConditionProfile lengthProfile) throws ArityException, UnsupportedTypeException {
            
            if (arguments.length < 1) throw ArityException.create(1, 2, arguments.length);
            
            // Argument 0 is the receiver (JolkObject)
            if (!(arguments[0] instanceof JolkObject receiver)) {
                throw UnsupportedTypeException.create(arguments, "Receiver must be a JolkObject");
            }

            if (lengthProfile.profile(arguments.length == 1)) {
                // Getter: #x -> returns value
                Object result = receiver.getFieldValue(index);
                return result == null ? JolkNothing.INSTANCE : result;
            } else if (lengthProfile.profile(arguments.length >= 2)) {
                // Setter: #x(value) -> returns Self (for fluent chaining)
                Object value = arguments[1];
                receiver.setFieldValue(index, value);
                return receiver;
            }
            throw ArityException.create(1, 2, arguments.length);
        }
    }
}
